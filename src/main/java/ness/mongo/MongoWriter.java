/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ness.mongo;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.skife.config.TimeSpan;
import org.weakref.jmx.Managed;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.nesscomputing.logging.Log;

/**
 * Write objects into a Mongo collection. Do local buffering and bursting into the collection.
 */
public class MongoWriter implements Runnable
{
    private static final Function<Callable<DBObject>, DBObject> CALLABLE_FUNCTION = new Function<Callable<DBObject>, DBObject>() {
        @Override
        public DBObject apply(@Nullable final Callable<DBObject> callable) {
            try {
                return callable == null ? null : callable.call();
            }
            catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    };

    private static final Log LOG = Log.findLog();

    /** Queue of pending writes.  */
    private final LinkedBlockingQueue<Callable<DBObject>> writeQueue;

    private AtomicBoolean taskRunning = new AtomicBoolean(true);

    /** Holds a reference to the Mongo event collection that this writer uses. */
    private final AtomicReference<DBCollection> dbCollection = new AtomicReference<DBCollection>(null);

    private final AtomicLong cooloffTime = new AtomicLong(-1L);

    private final AtomicReference<Thread> writerThread = new AtomicReference<Thread>(null);

    private final MongoWriterConfig mongoWriterConfig;
    private final String collectionName;

    private final TimeSpan enqueueTimeout;

    private final AtomicLong opsEnqueued = new AtomicLong(0L);
    private final AtomicLong opsEnqTimeout = new AtomicLong(0L);
    private final AtomicLong opsEnqCooloff = new AtomicLong(0L);
    private final AtomicLong opsDequeued = new AtomicLong(0L);
    private final AtomicLong opsSent = new AtomicLong(0L);
    private final AtomicLong opsLost = new AtomicLong(0L);
    private final AtomicInteger longestBurst = new AtomicInteger(0);


    MongoWriter(final MongoWriterConfig mongoWriterConfig)
    {
        this.mongoWriterConfig = mongoWriterConfig;
        this.collectionName = mongoWriterConfig.getCollectionName();

        this.writeQueue = new LinkedBlockingQueue<Callable<DBObject>>(mongoWriterConfig.getQueueLength());
        this.enqueueTimeout = mongoWriterConfig.getEnqueueTimeout();
    }

    synchronized void start()
    {
        if (mongoWriterConfig.isEnabled()) {
            try {
                Preconditions.checkState(writerThread.get() == null, "already started, boldly refusing to start twice!");
                Preconditions.checkState(dbCollection.get() == null, "Already have a collection object, something went very wrong!");

                LOG.info("Starting Mongo Writer for collection %s.", collectionName);

                final DBCollection collection = mongoWriterConfig.getMongoUri().connectDB().getCollection(collectionName);
                dbCollection.set(collection);

                final Thread thread = new Thread(this, String.format("mongo-%s-writer", collectionName));
                writerThread.set(thread);
                thread.start();
            }
            catch (UnknownHostException uhe) {
                LOG.errorDebug(uhe, "Could not connect to mongo URI %s", mongoWriterConfig.getMongoUri());
            }
        }
    }

    synchronized void stop()
    {
        final Thread thread = writerThread.getAndSet(null);
        if (thread != null) {
            LOG.info("Stopping Mongo Writer for collection %s.", collectionName);
            try {
                taskRunning.set(false);
                thread.interrupt();
                thread.join(500L);
            }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            final DBCollection collection = dbCollection.getAndSet(null);
            if (collection != null) {
                final Mongo mongo = collection.getDB().getMongo();
                mongo.close();
            }
        } else {
            LOG.debug("Never started, ignoring stop()");
        }
    }

    /**
     * Write a new DBObject into the collection.
     */
    public boolean write(final DBObject dbObject)
    {
        return write(new Callable<DBObject>() {
            @Override
            public DBObject call() {
                return dbObject;
            }
        });
    }

    /**
     * Write a new DBObject into the collection. The Callable hands off the actual
     * conversion work from the caller thread to the writer thread.
     */
    public boolean write(final Callable<DBObject> callable)
    {
        if (!mongoWriterConfig.isEnabled()) {
            return false;
        }

        Preconditions.checkState(taskRunning.get(), "Attempt to enqueue while the writer is shut down!");

        if (callable == null) {
            return false;
        }

        final long cooloffTime = this.cooloffTime.get();

        if (cooloffTime > 0 && System.nanoTime() < cooloffTime) {
            opsEnqCooloff.incrementAndGet();
            LOG.trace("Cooling off from enqueue failure");
            return false;
        }

        try {
            if (enqueueTimeout == null) {
                writeQueue.put(callable);
                opsEnqueued.incrementAndGet();
                this.cooloffTime.set(-1L);
                return true;
            }
            else {
                if (writeQueue.offer(callable, enqueueTimeout.getPeriod(), enqueueTimeout.getUnit())) {
                    opsEnqueued.incrementAndGet();
                    this.cooloffTime.set(-1L);
                    return true;
                }
                opsEnqTimeout.incrementAndGet();
            }
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        LOG.warn("Could not offer object to queue, sleeping for %s sec!", mongoWriterConfig.getFailureCooloffTime());
        this.cooloffTime.compareAndSet(-1L, System.nanoTime() + mongoWriterConfig.getFailureCooloffTime().getMillis() * 1000000L);

        return false;
    }

    private void flushToMongo(final List<Callable<DBObject>> dbObjects)
    {
        LOG.trace("Starting write of %d elements...", dbObjects.size());

        final DBCollection collection = dbCollection.get();
        if (collection != null) {
            final WriteResult writeResult = collection.insert(Lists.transform(dbObjects, CALLABLE_FUNCTION), WriteConcern.NORMAL);
            final CommandResult cmdResult = writeResult.getLastError();
            if (cmdResult.ok()) {
                opsSent.addAndGet(dbObjects.size());
            }
            else {
                LOG.warn("Command returned %s", cmdResult.getErrorMessage());
                opsLost.addAndGet(dbObjects.size());
            }
            LOG.trace("Wrote %d put ops to Mongo dbCollection %s.", dbObjects.size(), collectionName);
        } else {
            LOG.warn("dbCollection is null, probably shutting down!");
        }
    }

    @Override
    public void run()
    {
        final TimeSpan tickerTime  = mongoWriterConfig.getTickerTime();

        LOG.info("Mongo writer for %s starting (ticker: %s) ...", collectionName, tickerTime);
        try {
            while (taskRunning.get()) {
                final Callable<DBObject> dbObject = writeQueue.poll();
                if (dbObject != null) {
                    final List<Callable<DBObject>> dbObjects = Lists.newArrayList();
                    dbObjects.add(dbObject);
                    writeQueue.drainTo(dbObjects);
                    final int size = dbObjects.size();
                    opsDequeued.addAndGet(size);
                    if (size > longestBurst.get()) {
                        longestBurst.set(size);
                    }
                    flushToMongo(dbObjects);
                }
                Thread.sleep(tickerTime.getMillis());
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOG.info("Exiting");
    }

    @Managed
    public long getOpsEnqueued()
    {
        return opsEnqueued.get();
    }

    @Managed
    public long getOpsEnqTimeout()
    {
        return opsEnqTimeout.get();
    }

    @Managed
    public long getOpsEnqCooloff()
    {
        return opsEnqCooloff.get();
    }

    @Managed
    public long getOpsDequeued()
    {
        return opsDequeued.get();
    }

    @Managed
    public long getOpsSent()
    {
        return opsSent.get();
    }

    @Managed
    public long getOpsLost()
    {
        return opsLost.get();
    }

    @Managed
    public int getQueueLength()
    {
        return writeQueue.size();
    }

    @Managed
    public int getLongestBurst()
    {
        return longestBurst.get();
    }
}
