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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Try;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface Consumer<T> extends java.util.function.Consumer<T>, Try.Consumer<T, RuntimeException> {

    @Override
    void accept(T t);

    public static <T> Consumer<T> of(final Consumer<T> consumer) {
        N.requireNonNull(consumer);

        return consumer;
    }

    public static <T, R> Consumer<T> create(final Function<T, R> func) {
        N.requireNonNull(func);

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                func.apply(t);
            }
        };
    }
}
