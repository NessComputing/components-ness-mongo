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

import java.util.Date;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;

import com.nesscomputing.mongo.BSONEncodingTransformers.BsonReadableInstantTransformer;

public class TestBsonReadableInstantTransformer extends AbstractTestBSONTransformers<DateTime, Date>
{
    public TestBsonReadableInstantTransformer()
    {
        super(Date.class);
    }

    @Before
    public void setUp()
    {
        testTransformer = new BsonReadableInstantTransformer();
        testObject = new DateTime().withZone(DateTimeZone.UTC);
        resultValue = testObject.toDate();
    }
}
