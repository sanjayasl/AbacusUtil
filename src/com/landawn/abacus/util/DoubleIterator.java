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

package com.landawn.abacus.util;

import java.util.NoSuchElementException;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public abstract class DoubleIterator extends ImmutableIterator<Double> {
    public static final DoubleIterator EMPTY = new DoubleIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public double nextDouble() {
            throw new NoSuchElementException();
        }
    };

    public static DoubleIterator empty() {
        return EMPTY;
    }

    public static DoubleIterator of(final double[] a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    public static DoubleIterator of(final double[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new DoubleIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
            }

            @Override
            public double[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public DoubleList toList() {
                return DoubleList.of(N.copyOfRange(a, cursor, toIndex));
            }
        };
    }

    /**
     * 
     * @Deprecated use <code>nextDouble()</code> instead.
     */
    @Deprecated
    @Override
    public Double next() {
        return nextDouble();
    }

    public abstract double nextDouble();

    public double[] toArray() {
        return toList().trimToSize().array();
    }

    public DoubleList toList() {
        final DoubleList list = new DoubleList();

        while (hasNext()) {
            list.add(nextDouble());
        }

        return list;
    }

    public <E extends Exception> void forEachRemaining(Try.DoubleConsumer<E> action) throws E {
        N.requireNonNull(action);

        while (hasNext()) {
            action.accept(nextDouble());
        }
    }
}
