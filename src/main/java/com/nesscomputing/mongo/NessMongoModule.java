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

import java.util.UUID;


import org.joda.time.DateTime;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.nesscomputing.mongo.BSONEncodingTransformers.BsonReadableInstantTransformer;
import com.nesscomputing.mongo.BSONEncodingTransformers.BsonUuidTransformer;

/**
 * Basic mongo glue.
 */
public class NessMongoModule extends AbstractModule
{
    @Override
    public void configure()
    {
        BSONTransformerBinder.bindEncodingTransformer(binder(), UUID.class).to(BsonUuidTransformer.class).in(Scopes.SINGLETON);
        BSONTransformerBinder.bindEncodingTransformer(binder(), DateTime.class).to(BsonReadableInstantTransformer.class).in(Scopes.SINGLETON);

        bind(BSONTransformerBinder.class).asEagerSingleton();
    }
}
