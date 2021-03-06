/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.function;

import java.util.Objects;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Try;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface TriConsumer<A, B, C> extends Try.TriConsumer<A, B, C, RuntimeException> {

    @Override
    void accept(A a, B b, C c);

    default TriConsumer<A, B, C> andThen(TriConsumer<? super A, ? super B, ? super C> after) {
        Objects.requireNonNull(after);

        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }

    public static <A, B, C> TriConsumer<A, B, C> of(final TriConsumer<A, B, C> triConsumer) {
        N.requireNonNull(triConsumer);

        return triConsumer;
    }

    public static <A, B, C, R> TriConsumer<A, B, C> create(final TriFunction<A, B, C, R> func) {
        N.requireNonNull(func);

        return new TriConsumer<A, B, C>() {
            @Override
            public void accept(A a, B b, C c) {
                func.apply(a, b, c);
            }
        };
    }
}
