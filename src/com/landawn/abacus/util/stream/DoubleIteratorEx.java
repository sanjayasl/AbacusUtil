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

package com.landawn.abacus.util.stream;

import static com.landawn.abacus.util.stream.StreamBase.checkFromToIndex;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.N;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public abstract class DoubleIteratorEx extends DoubleIterator implements IteratorEx<Double> {
    public static final DoubleIteratorEx EMPTY = new DoubleIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public double nextDouble() {
            throw new NoSuchElementException();
        }

        @Override
        public void skip(long n) {
            // Do nothing.
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public double[] toArray() {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        @Override
        public void close() {
            // Do nothing.
        }
    };

    public static DoubleIteratorEx empty() {
        return EMPTY;
    }

    public static DoubleIteratorEx of(final double[] a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    public static DoubleIteratorEx of(final double[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new DoubleIteratorEx() {
            int cursor = fromIndex;

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
            public void skip(long n) {
                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor;
            }

            @Override
            public double[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public DoubleList toList() {
                return DoubleList.of(N.copyOfRange(a, cursor, toIndex));
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    public static DoubleIteratorEx of(final DoubleIterator iter) {
        if (iter instanceof DoubleIteratorEx) {
            return ((DoubleIteratorEx) iter);
        }

        return new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                return iter.nextDouble();
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    public static DoubleIteratorEx from(final Iterator<Double> iter) {
        if (iter instanceof ObjIteratorEx) {
            final ObjIteratorEx<Double> iteratorEx = ((ObjIteratorEx<Double>) iter);

            return new DoubleIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public double nextDouble() {
                    return iteratorEx.next();
                }

                @Override
                public void skip(long n) {
                    iteratorEx.skip(n);
                }

                @Override
                public long count() {
                    return iteratorEx.count();
                }

                @Override
                public void close() {
                    iteratorEx.close();
                }
            };
        } else {
            return new DoubleIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public double nextDouble() {
                    return iter.next();
                }

                @Override
                public void close() {
                    // Do nothing.
                }
            };
        }
    }

    @Override
    public void skip(long n) {
        while (n > 0 && hasNext()) {
            nextDouble();
            n--;
        }
    }

    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextDouble();
            result++;
        }

        return result;
    }

    @Override
    public void close() {
        // Do nothing.
    }
}
