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

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;
import org.skife.config.TimeSpan;

import com.mongodb.MongoURI;

/**
 * Configure an Mongo writer for a given object type to an Mongo table.
 */
public abstract class MongoWriterConfig
{
    /**
     * Enable / disable the writer.
     */
    @Config({"ness.mongo.writer.${writername}.enabled","ness.mongo.writer.enabled"})
    @Default("false")
    public boolean isEnabled()
    {
        return false;
    }

    /**
     * Length of the internal queue to buffer bursts from JMS into Mongo. A longer queue increases
     * the risk of losing events if the service crashes before they could be stuffed away.
     * A shorter queue with higher timeout (see below) will slow the writer.
     */
    @Config({"ness.mongo.writer.${writername}.queue-length","ness.mongo.writer.queue-length"})
    @Default("1000")
    public int getQueueLength()
    {
        return 1000;
    }

    /**
     * Time that the writer thread sleeps (and accumulates new events).
     */
    @Config({"ness.mongo.writer.${writername}.ticker-time","ness.mongo.writer.ticker-time"})
    @Default("100ms")
    public TimeSpan getTickerTime()
    {
        return new TimeSpan("100ms");
    }


    /**
     * Maximum amount of time that is waited to enqueue an object into the write queue.
     *
     * Default is to wait until there is space in the queue.
     */
    @Config({"ness.mongo.writer.${writername}.enqueue-timeout","ness.mongo.writer.enqueue-timeout"})
    @DefaultNull
    public TimeSpan getEnqueueTimeout()
    {
        return null;
    }

    /**
     * Cooloff time after failing to enqueue an event.
     */
    @Config({"ness.mongo.writer.${writername}.failure-cooloff-time","ness.mongo.writer.failure-cooloff-time"})
    @Default("1s")
    public TimeSpan getFailureCooloffTime()
    {
        return new TimeSpan("1s");
    }

    /**
     * The Mongo DB Server to connect to.
     */
    @Config({"ness.mongo.writer.${writername}.uri","ness.mongo.writer.uri"})
    public abstract MongoURI getMongoUri();

    /**
     * Name of the collection to write into.
     */
    @Config({"ness.mongo.writer.${writername}.collection-name","ness.mongo.writer.collection-name"})
    public abstract String getCollectionName();
}
