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
public abstract class ShortIterator extends ImmutableIterator<Short> {
    public static final ShortIterator EMPTY = new ShortIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public short nextShort() {
            throw new NoSuchElementException();
        }
    };

    public static ShortIterator empty() {
        return EMPTY;
    }

    public static ShortIterator of(final short[] a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    public static ShortIterator of(final short[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new ShortIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
            }

            @Override
            public short[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public ShortList toList() {
                return ShortList.of(N.copyOfRange(a, cursor, toIndex));
            }
        };
    }

    /**
     * 
     * @Deprecated use <code>nextShort()</code> instead.
     */
    @Deprecated
    @Override
    public Short next() {
        return nextShort();
    }

    public abstract short nextShort();

    public short[] toArray() {
        return toList().trimToSize().array();
    }

    public ShortList toList() {
        final ShortList list = new ShortList();

        while (hasNext()) {
            list.add(nextShort());
        }

        return list;
    }

    public <E extends Exception> void forEachRemaining(Try.ShortConsumer<E> action) throws E {
        N.requireNonNull(action);

        while (hasNext()) {
            action.accept(nextShort());
        }
    }
}
