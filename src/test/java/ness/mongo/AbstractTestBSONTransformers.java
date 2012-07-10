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

import org.bson.Transformer;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestBSONTransformers<From, To>
{
    protected Transformer testTransformer = null;

    protected From testObject = null;
    protected Class<To> resultClass;

    protected To resultValue = null;

    protected AbstractTestBSONTransformers(Class<To> resultClass)
    {
        this.resultClass = resultClass;
    }

    @Test
    public void testNullOk()
    {
        Assert.assertNotNull(testTransformer);

        final Object result = testTransformer.transform(null);

        Assert.assertNull(result);
    }

    @Test
    public void testNormalConvert()
    {
        Assert.assertNotNull(testObject);
        Assert.assertNotNull(testTransformer);
        Assert.assertNotNull(resultClass);
        Assert.assertNotNull(resultValue);

        final Object result = testTransformer.transform(testObject);

        Assert.assertNotNull(result);
        Assert.assertEquals(resultClass, result.getClass());
        Assert.assertEquals(resultValue, result);
    }

    @Test(expected = IllegalStateException.class)
    public void testExplodeOnBadObject()
    {
        Assert.assertNotNull(testTransformer);
        testTransformer.transform(new FooObject() {});
    }



    public static interface FooObject
    {
    }
}




