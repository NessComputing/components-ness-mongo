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

import java.util.Map;

import org.bson.BSON;
import org.bson.Transformer;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * Bind new Transformers into the BSON converter used by Mongo. This is not the most optimal conversion from/to BSON but
 * it gets us off the ground and leaves space to improve later.
 */
public final class BSONTransformerBinder
{
    public static final String ENCODING_NAME = "_bson_encoding";
    public static final Named ENCODING_NAMED = Names.named(ENCODING_NAME);
    public static final String DECODING_NAME = "_bson_decoding";
    public static final Named DECODING_NAMED = Names.named(DECODING_NAME);

    @Inject(optional=true)
    void injectBSONEncodingTransformers(@Named(ENCODING_NAME) final Map<Class<?>, Transformer> encodingTransformers)
    {
        Preconditions.checkNotNull(encodingTransformers, "transformers can not be null!");

        for (Map.Entry<Class<?>, Transformer> entry : encodingTransformers.entrySet()) {
            BSON.addEncodingHook(entry.getKey(), entry.getValue());
        }
    }

    @Inject(optional=true)
    void injectBSONDecodingTransformers(@Named(DECODING_NAME) final Map<Class<?>, Transformer> decodingTransformers)
    {
        Preconditions.checkNotNull(decodingTransformers, "transformers can not be null!");

        for (Map.Entry<Class<?>, Transformer> entry : decodingTransformers.entrySet()) {
            BSON.addDecodingHook(entry.getKey(), entry.getValue());
        }
    }

    public static LinkedBindingBuilder<Transformer> bindEncodingTransformer(final Binder binder, final Class<?> clazz)
    {
        final MapBinder<Class<?>, Transformer> transformerBinder = MapBinder.newMapBinder(binder, new TypeLiteral<Class<?>>() {}, new TypeLiteral<Transformer>() {}, ENCODING_NAMED);
        return transformerBinder.addBinding(clazz);
    }

    public static LinkedBindingBuilder<Transformer> bindDecodingTransformer(final Binder binder, final Class<?> clazz)
    {
        final MapBinder<Class<?>, Transformer> transformerBinder = MapBinder.newMapBinder(binder, new TypeLiteral<Class<?>>() {}, new TypeLiteral<Transformer>() {}, DECODING_NAMED);
        return transformerBinder.addBinding(clazz);
    }
}
