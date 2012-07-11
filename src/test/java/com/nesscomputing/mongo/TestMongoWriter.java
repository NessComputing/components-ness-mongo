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
package com.nesscomputing.mongo;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Test;
import org.skife.config.TimeSpan;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoURI;

public class TestMongoWriter
{
    private static final MongoWriterConfig MONGO_WRITER_CONFIG = new MongoWriterConfig() {
        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public TimeSpan getEnqueueTimeout() {
            return new TimeSpan("10ms");
        }

        @Override
        public MongoURI getMongoUri() {
            return null;
        }

        @Override
        public String getCollectionName() {
            return null;
        }
    };

    private static final Callable<DBObject> CALLABLE = new Callable<DBObject>() {
        @Override
        public DBObject call() {
            return new BasicDBObject();
        }
    };


    @Test
    public void testCoolOffWithQueueFlush() throws Exception
    {
        final List<Callable<DBObject>> flushList = Lists.newArrayList();

        final MongoWriter dummyWriter = new MongoWriter(MONGO_WRITER_CONFIG) {
            @Override
            protected void flushToMongo(final List<Callable<DBObject>> dbObjects)
            {
                flushList.addAll(dbObjects);
            }
        };

        int count = 0;

        while (dummyWriter.write(CALLABLE)) {
            count++;
        }

        // Make sure that the queue is full.
        Assert.assertEquals(MONGO_WRITER_CONFIG.getQueueLength(), count);

        Assert.assertEquals(0, flushList.size());

        // Run a single loop of the thread to flush the queue.
        dummyWriter.runLoop();
        Assert.assertEquals(MONGO_WRITER_CONFIG.getQueueLength(), flushList.size());

        // But writing should still fail.
        Assert.assertFalse(dummyWriter.write(CALLABLE));

        Thread.sleep(500L);

        // But writing should still fail.
        Assert.assertFalse(dummyWriter.write(CALLABLE));

        Thread.sleep(1000L);

        // Now it should work again.
        Assert.assertTrue(dummyWriter.write(CALLABLE));
    }

    @Test
    public void testCoolOffWithoutQueueFlush() throws Exception
    {
        final List<Callable<DBObject>> flushList = Lists.newArrayList();

        final MongoWriter dummyWriter = new MongoWriter(MONGO_WRITER_CONFIG) {
            @Override
            protected void flushToMongo(final List<Callable<DBObject>> dbObjects)
            {
                flushList.addAll(dbObjects);
            }
        };

        int count = 0;

        while (dummyWriter.write(CALLABLE)) {
            count++;
        }

        // Make sure that the queue is full.
        Assert.assertEquals(MONGO_WRITER_CONFIG.getQueueLength(), count);

        // Wait until "past cooloff time".
        Thread.sleep(1100L);

        // Writing should still fail, but it should log only every second..
        for (int i = 0; i < 30; i++) {
            Assert.assertFalse(dummyWriter.write(CALLABLE));
            Thread.sleep(100L);
        }
    }

}
