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

import java.util.UUID;

import ness.mongo.BSONEncodingTransformers.BsonUuidTransformer;

import org.junit.Before;

public class TestBsonUuidTransformer extends AbstractTestBSONTransformers<UUID, String>
{
    public TestBsonUuidTransformer()
    {
        super(String.class);
    }

    @Before
    public void setUp()
    {
        testTransformer = new BsonUuidTransformer();
        testObject = UUID.randomUUID();
        resultValue = testObject.toString();
    }
}
