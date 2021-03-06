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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nullable;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 * This class is a sequential, stateful and immutable stream implementation.
 *
 * @param <T>
 * @since 0.8
 * 
 * @author Haiyang Li
 */
class ArrayStream<T> extends AbstractStream<T> {
    final T[] elements;
    final int fromIndex;
    final int toIndex;

    ArrayStream(final T[] values) {
        this(values, 0, values.length);
    }

    ArrayStream(final T[] values, final Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    ArrayStream(final T[] values, final boolean sorted, final Comparator<? super T> comparator, final Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, sorted, comparator, closeHandlers);
    }

    ArrayStream(final T[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    ArrayStream(final T[] values, final int fromIndex, final int toIndex, final Collection<Runnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, null, closeHandlers);
    }

    ArrayStream(final T[] values, final int fromIndex, final int toIndex, final boolean sorted, Comparator<? super T> comparator,
            final Collection<Runnable> closeHandlers) {
        super(sorted, comparator, closeHandlers);

        checkFromToIndex(fromIndex, toIndex, values.length);

        this.elements = values;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    @Override
    public Stream<T> filter(final Predicate<? super T> predicate) {
        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private boolean hasNext = false;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                if (hasNext == false && cursor < toIndex) {
                    do {
                        if (predicate.test(elements[cursor])) {
                            hasNext = true;
                            break;
                        }
                    } while (++cursor < toIndex);
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted, cmp, closeHandlers);
    }

    @Override
    public Stream<T> takeWhile(final Predicate<? super T> predicate) {
        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private boolean hasMore = true;
            private boolean hasNext = false;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                if (hasNext == false && hasMore && cursor < toIndex) {
                    if (predicate.test(elements[cursor])) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted, cmp, closeHandlers);
    }

    @Override
    public Stream<T> dropWhile(final Predicate<? super T> predicate) {
        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private boolean hasNext = false;
            private int cursor = fromIndex;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (hasNext == false && cursor < toIndex) {
                    if (dropped == false) {
                        do {
                            if (predicate.test(elements[cursor]) == false) {
                                hasNext = true;
                                break;
                            }
                        } while (++cursor < toIndex);

                        dropped = true;
                    } else {
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted, cmp, closeHandlers);
    }

    @Override
    public <R> Stream<R> map(final Function<? super T, ? extends R> mapper) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.apply(elements[cursor++]);
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = (A) mapper.apply(elements[cursor++]);
                }

                return a;
            }
        }, closeHandlers);
    }

    //    @Override
    //    public <R> Stream<R> biMap(final BiFunction<? super T, ? super T, ? extends R> mapper, final boolean ignoreNotPaired) {
    //        return new IteratorStream<>(new ObjIteratorEx<R>() {
    //            private final int atLeast = ignoreNotPaired ? 2 : 1;
    //            private int cursor = fromIndex;
    //
    //            @Override
    //            public boolean hasNext() {
    //                return toIndex - cursor >= atLeast;
    //            }
    //
    //            @Override
    //            public R next() {
    //                if (toIndex - cursor < atLeast) {
    //                    throw new NoSuchElementException();
    //                }
    //
    //                return mapper.apply(elements[cursor++], cursor == toIndex ? null : elements[cursor++]);
    //            }
    //
    //            //            @Override
    //            //            public long count() {
    //            //                return (toIndex - cursor) / 2 + (ignoreNotPaired || (toIndex - cursor) % 2 == 0 ? 0 : 1);
    //            //            }
    //            //
    //            //            @Override
    //            //            public void skip(long n) {
    //            //                cursor = n < count() ? cursor + (int) n * 2 : toIndex;
    //            //            }
    //
    //            @Override
    //            public <A> A[] toArray(A[] a) {
    //                final int len = (int) count();
    //                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);
    //
    //                for (int i = 0, len2 = (toIndex - cursor) / 2; i < len2; i++) {
    //                    a[i] = (A) mapper.apply(elements[cursor++], elements[cursor++]);
    //                }
    //
    //                if (cursor < toIndex) {
    //                    a[len - 1] = (A) mapper.apply(elements[cursor++], null);
    //                }
    //
    //                return a;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    @Override
    //    public <R> Stream<R> triMap(final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper, final boolean ignoreNotPaired) {
    //        return new IteratorStream<>(new ObjIteratorEx<R>() {
    //            private final int atLeast = ignoreNotPaired ? 3 : 1;
    //            private int cursor = fromIndex;
    //
    //            @Override
    //            public boolean hasNext() {
    //                return toIndex - cursor >= atLeast;
    //            }
    //
    //            @Override
    //            public R next() {
    //                if (toIndex - cursor < atLeast) {
    //                    throw new NoSuchElementException();
    //                }
    //
    //                return mapper.apply(elements[cursor++], cursor == toIndex ? null : elements[cursor++], cursor == toIndex ? null : elements[cursor++]);
    //            }
    //
    //            //            @Override
    //            //            public long count() {
    //            //                return (toIndex - cursor) / 3 + (ignoreNotPaired || (toIndex - cursor) % 3 == 0 ? 0 : 1);
    //            //            }
    //            //
    //            //            @Override
    //            //            public void skip(long n) {
    //            //                cursor = n < count() ? cursor + (int) n * 3 : toIndex;
    //            //            }
    //
    //            @Override
    //            public <A> A[] toArray(A[] a) {
    //                final int len = (int) count();
    //                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);
    //
    //                for (int i = 0, len2 = (toIndex - cursor) / 3; i < len2; i++) {
    //                    a[i] = (A) mapper.apply(elements[cursor++], elements[cursor++], elements[cursor++]);
    //                }
    //
    //                if (cursor < toIndex) {
    //                    a[len - 1] = (A) mapper.apply(elements[cursor++], cursor == toIndex ? null : elements[cursor++], null);
    //                }
    //
    //                return a;
    //            }
    //        }, closeHandlers);
    //    }

    @Override
    public <R> Stream<R> slidingMap(final BiFunction<? super T, ? super T, R> mapper, final int increment, final boolean ignoreNotPaired) {
        final int windowSize = 2;

        N.checkArgument(windowSize > 0 && increment > 0, "'windowSize'=%s and 'increment'=%s must not be less than 1", windowSize, increment);

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return ignoreNotPaired ? toIndex - cursor >= windowSize : cursor < toIndex;
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final R result = mapper.apply(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            //            @Override
            //            public long count() {
            //                if (toIndex - cursor == 0) {
            //                    return 0;
            //                } else if (toIndex - cursor <= windowSize) {
            //                    return 1;
            //                } else {
            //                    final long len = (toIndex - cursor) - windowSize;
            //                    return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
            //                }
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                if (n > 0) {
            //                    if (n >= count()) {
            //                        cursor = toIndex;
            //                    } else {
            //                        cursor += n * increment;
            //                    }
            //                }
            //            }

        }, closeHandlers);
    }

    @Override
    public <R> Stream<R> slidingMap(final TriFunction<? super T, ? super T, ? super T, R> mapper, final int increment, final boolean ignoreNotPaired) {
        final int windowSize = 3;

        N.checkArgument(windowSize > 0 && increment > 0, "'windowSize'=%s and 'increment'=%s must not be less than 1", windowSize, increment);

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return ignoreNotPaired ? toIndex - cursor >= windowSize : cursor < toIndex;
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final R result = mapper.apply(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null,
                        cursor < toIndex - 2 ? elements[cursor + 2] : null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            //            @Override
            //            public long count() {
            //                if (toIndex - cursor == 0) {
            //                    return 0;
            //                } else if (toIndex - cursor <= windowSize) {
            //                    return 1;
            //                } else {
            //                    final long len = (toIndex - cursor) - windowSize;
            //                    return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
            //                }
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                if (n > 0) {
            //                    if (n >= count()) {
            //                        cursor = toIndex;
            //                    } else {
            //                        cursor += n * increment;
            //                    }
            //                }
            //            }

        }, closeHandlers);
    }

    @Override
    public Stream<T> mapFirst(final Function<? super T, ? extends T> mapperForFirst) {
        N.requireNonNull(mapperForFirst);

        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public T next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                if (cursor == fromIndex) {
                    return mapperForFirst.apply(elements[cursor++]);
                } else {
                    return elements[cursor++];
                }
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public long count() {
                if (hasNext()) {
                    next();
                    return toIndex - cursor + 1;
                }

                return 0;
            }

            @Override
            public void skip(long n) {
                if (hasNext()) {
                    next();
                    n -= 1;
                    cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
                }
            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    if (cursor == fromIndex) {
                        a[i] = (A) mapperForFirst.apply(elements[cursor++]);
                    } else {
                        a[i] = (A) elements[cursor++];
                    }
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public <R> Stream<R> mapFirstOrElse(final Function<? super T, ? extends R> mapperForFirst, final Function<? super T, ? extends R> mapperForElse) {
        N.requireNonNull(mapperForFirst);
        N.requireNonNull(mapperForElse);

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                if (cursor == fromIndex) {
                    return mapperForFirst.apply(elements[cursor++]);
                } else {
                    return mapperForElse.apply(elements[cursor++]);
                }
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    if (cursor == fromIndex) {
                        a[i] = (A) mapperForFirst.apply(elements[cursor++]);
                    } else {
                        a[i] = (A) mapperForElse.apply(elements[cursor++]);
                    }
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public Stream<T> mapLast(final Function<? super T, ? extends T> mapperForLast) {
        N.requireNonNull(mapperForLast);

        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private int last = toIndex - 1;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public T next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                if (cursor == last) {
                    return mapperForLast.apply(elements[cursor++]);
                } else {
                    return elements[cursor++];
                }
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    if (cursor == last) {
                        a[i] = (A) mapperForLast.apply(elements[cursor++]);
                    } else {
                        a[i] = (A) elements[cursor++];
                    }
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public <R> Stream<R> mapLastOrElse(final Function<? super T, ? extends R> mapperForLast, final Function<? super T, ? extends R> mapperForElse) {
        N.requireNonNull(mapperForLast);
        N.requireNonNull(mapperForElse);

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            private int last = toIndex - 1;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                if (cursor == last) {
                    return mapperForLast.apply(elements[cursor++]);
                } else {
                    return mapperForElse.apply(elements[cursor++]);
                }
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    if (cursor == last) {
                        a[i] = (A) mapperForLast.apply(elements[cursor++]);
                    } else {
                        a[i] = (A) mapperForElse.apply(elements[cursor++]);
                    }
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public CharStream mapToChar(final ToCharFunction<? super T> mapper) {
        return new IteratorCharStream(new CharIteratorEx() {
            int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsChar(elements[cursor++]);
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public char[] toArray() {
                final char[] a = new char[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsChar(elements[cursor++]);
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public ByteStream mapToByte(final ToByteFunction<? super T> mapper) {
        return new IteratorByteStream(new ByteIteratorEx() {
            int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsByte(elements[cursor++]);
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public byte[] toArray() {
                final byte[] a = new byte[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsByte(elements[cursor++]);
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public ShortStream mapToShort(final ToShortFunction<? super T> mapper) {
        return new IteratorShortStream(new ShortIteratorEx() {
            int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsShort(elements[cursor++]);
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public short[] toArray() {
                final short[] a = new short[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsShort(elements[cursor++]);
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public IntStream mapToInt(final ToIntFunction<? super T> mapper) {
        return new IteratorIntStream(new IntIteratorEx() {
            int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public int nextInt() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsInt(elements[cursor++]);
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public int[] toArray() {
                final int[] a = new int[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsInt(elements[cursor++]);
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public LongStream mapToLong(final ToLongFunction<? super T> mapper) {
        return new IteratorLongStream(new LongIteratorEx() {
            int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsLong(elements[cursor++]);
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public long[] toArray() {
                final long[] a = new long[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsLong(elements[cursor++]);
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public FloatStream mapToFloat(final ToFloatFunction<? super T> mapper) {
        return new IteratorFloatStream(new FloatIteratorEx() {
            int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsFloat(elements[cursor++]);
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public float[] toArray() {
                final float[] a = new float[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsFloat(elements[cursor++]);
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) {
        return new IteratorDoubleStream(new DoubleIteratorEx() {
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

                return mapper.applyAsDouble(elements[cursor++]);
            }

            //            @Override
            //            public long count() {
            //                return toIndex - cursor;
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //            }

            @Override
            public double[] toArray() {
                final double[] a = new double[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsDouble(elements[cursor++]);
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) {
        final ObjIteratorEx<R> iter = new ObjIteratorEx<R>() {
            private int cursor = fromIndex;
            private Iterator<? extends R> cur = null;
            private Stream<? extends R> s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Set<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public R next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Set<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalLinkedHashSet<Runnable>(1)
                : new LocalLinkedHashSet<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorStream<>(iter, newCloseHandlers);
    }

    @Override
    public CharStream flatMapToChar(final Function<? super T, ? extends CharStream> mapper) {
        final CharIteratorEx iter = new CharIteratorEx() {
            private int cursor = fromIndex;
            private CharIterator cur = null;
            private CharStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Set<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public char nextChar() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextChar();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Set<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalLinkedHashSet<Runnable>(1)
                : new LocalLinkedHashSet<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorCharStream(iter, newCloseHandlers);
    }

    @Override
    public ByteStream flatMapToByte(final Function<? super T, ? extends ByteStream> mapper) {
        final ByteIteratorEx iter = new ByteIteratorEx() {
            private int cursor = fromIndex;
            private ByteIterator cur = null;
            private ByteStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Set<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public byte nextByte() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextByte();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Set<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalLinkedHashSet<Runnable>(1)
                : new LocalLinkedHashSet<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorByteStream(iter, newCloseHandlers);
    }

    @Override
    public ShortStream flatMapToShort(final Function<? super T, ? extends ShortStream> mapper) {
        final ShortIteratorEx iter = new ShortIteratorEx() {
            private int cursor = fromIndex;
            private ShortIterator cur = null;
            private ShortStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Set<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public short nextShort() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextShort();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Set<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalLinkedHashSet<Runnable>(1)
                : new LocalLinkedHashSet<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorShortStream(iter, newCloseHandlers);
    }

    @Override
    public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> mapper) {
        final IntIteratorEx iter = new IntIteratorEx() {
            private int cursor = fromIndex;
            private IntIterator cur = null;
            private IntStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Set<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public int nextInt() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextInt();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Set<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalLinkedHashSet<Runnable>(1)
                : new LocalLinkedHashSet<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorIntStream(iter, newCloseHandlers);
    }

    @Override
    public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> mapper) {
        final LongIteratorEx iter = new LongIteratorEx() {
            private int cursor = fromIndex;
            private LongIterator cur = null;
            private LongStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Set<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public long nextLong() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextLong();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Set<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalLinkedHashSet<Runnable>(1)
                : new LocalLinkedHashSet<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorLongStream(iter, newCloseHandlers);
    }

    @Override
    public FloatStream flatMapToFloat(final Function<? super T, ? extends FloatStream> mapper) {
        final FloatIteratorEx iter = new FloatIteratorEx() {
            private int cursor = fromIndex;
            private FloatIterator cur = null;
            private FloatStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Set<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public float nextFloat() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextFloat();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Set<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalLinkedHashSet<Runnable>(1)
                : new LocalLinkedHashSet<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorFloatStream(iter, newCloseHandlers);
    }

    @Override
    public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> mapper) {
        final DoubleIteratorEx iter = new DoubleIteratorEx() {
            private int cursor = fromIndex;
            private DoubleIterator cur = null;
            private DoubleStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Set<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public double nextDouble() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextDouble();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Set<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalLinkedHashSet<Runnable>(1)
                : new LocalLinkedHashSet<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorDoubleStream(iter, newCloseHandlers);
    }

    @Override
    public Stream<Stream<T>> split(final int size) {
        N.checkArgument(size > 0, "'size' must be bigger than 0. Can't be: %s", size);

        return new IteratorStream<>(new ObjIteratorEx<Stream<T>>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return new ArrayStream<>(elements, cursor, (cursor = size < toIndex - cursor ? cursor + size : toIndex), sorted, cmp, null);
            }

            @Override
            public long count() {
                final long len = toIndex - cursor;
                return len % size == 0 ? len / size : len / size + 1;
            }

            @Override
            public void skip(long n) {
                final long len = toIndex - cursor;
                cursor = n <= len / size ? cursor + (int) n * size : toIndex;
            }
        }, closeHandlers);
    }

    @Override
    public Stream<List<T>> splitToList(final int size) {
        N.checkArgument(size > 0, "'size' must be bigger than 0. Can't be: %s", size);

        return new IteratorStream<>(new ObjIteratorEx<List<T>>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public List<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return N.asList(N.copyOfRange(elements, cursor, (cursor = size < toIndex - cursor ? cursor + size : toIndex)));
            }

            @Override
            public long count() {
                final long len = toIndex - cursor;
                return len % size == 0 ? len / size : len / size + 1;
            }

            @Override
            public void skip(long n) {
                final long len = toIndex - cursor;
                cursor = n <= len / size ? cursor + (int) n * size : toIndex;
            }
        }, closeHandlers);
    }

    @Override
    public Stream<Set<T>> splitToSet(final int size) {
        N.checkArgument(size > 0, "'size' must be bigger than 0. Can't be: %s", size);

        return new IteratorStream<>(new ObjIteratorEx<Set<T>>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Set<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final Set<T> result = new HashSet<>(N.min(9, toIndex - cursor > size ? size : toIndex - cursor));

                for (int i = cursor, to = (cursor = size < toIndex - cursor ? cursor + size : toIndex); i < to; i++) {
                    result.add(elements[i]);
                }

                return result;
            }

            @Override
            public long count() {
                final long len = toIndex - cursor;
                return len % size == 0 ? len / size : len / size + 1;
            }

            @Override
            public void skip(long n) {
                final long len = toIndex - cursor;
                cursor = n <= len / size ? cursor + (int) n * size : toIndex;
            }
        }, closeHandlers);
    }

    @Override
    public Stream<Stream<T>> split(final Predicate<? super T> predicate) {
        return new IteratorStream<>(new ObjIteratorEx<Stream<T>>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final int from = cursor;

                while (cursor < toIndex) {
                    if (from == cursor) {
                        preCondition = predicate.test(elements[from]);
                        cursor++;
                    } else if (predicate.test(elements[cursor]) == preCondition) {
                        cursor++;
                    } else {

                        break;
                    }
                }

                return new ArrayStream<>(elements, from, cursor, sorted, cmp, null);
            }
        }, closeHandlers);
    }

    @Override
    public Stream<List<T>> splitToList(final Predicate<? super T> predicate) {
        return new IteratorStream<>(new ObjIteratorEx<List<T>>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public List<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final List<T> result = new ArrayList<>();

                while (cursor < toIndex) {
                    if (result.size() == 0) {
                        preCondition = predicate.test(elements[cursor]);
                        result.add(elements[cursor]);
                        cursor++;
                    } else if (predicate.test(elements[cursor]) == preCondition) {
                        result.add(elements[cursor]);
                        cursor++;
                    } else {

                        break;
                    }
                }

                return result;
            }

        }, closeHandlers);
    }

    @Override
    public <U> Stream<Stream<T>> split(final U seed, final BiPredicate<? super T, ? super U> predicate, final Consumer<? super U> seedUpdate) {
        return new IteratorStream<>(new ObjIteratorEx<Stream<T>>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final int from = cursor;

                while (cursor < toIndex) {
                    if (from == cursor) {
                        preCondition = predicate.test(elements[from], seed);
                        cursor++;
                    } else if (predicate.test(elements[cursor], seed) == preCondition) {
                        cursor++;
                    } else {
                        if (seedUpdate != null) {
                            seedUpdate.accept(seed);
                        }

                        break;
                    }
                }

                return new ArrayStream<>(elements, from, cursor, sorted, cmp, null);
            }
        }, closeHandlers);
    }

    @Override
    public <U> Stream<List<T>> splitToList(final U seed, final BiPredicate<? super T, ? super U> predicate, final Consumer<? super U> seedUpdate) {
        return new IteratorStream<>(new ObjIteratorEx<List<T>>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public List<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final List<T> result = new ArrayList<>();

                while (cursor < toIndex) {
                    if (result.size() == 0) {
                        preCondition = predicate.test(elements[cursor], seed);
                        result.add(elements[cursor]);
                        cursor++;
                    } else if (predicate.test(elements[cursor], seed) == preCondition) {
                        result.add(elements[cursor]);
                        cursor++;
                    } else {
                        if (seedUpdate != null) {
                            seedUpdate.accept(seed);
                        }

                        break;
                    }
                }

                return result;
            }

        }, closeHandlers);
    }

    @Override
    public <U> Stream<Set<T>> splitToSet(final U seed, final BiPredicate<? super T, ? super U> predicate, final Consumer<? super U> seedUpdate) {
        return new IteratorStream<>(new ObjIteratorEx<Set<T>>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Set<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final Set<T> result = new HashSet<>();

                while (cursor < toIndex) {
                    if (result.size() == 0) {
                        preCondition = predicate.test(elements[cursor], seed);
                        result.add(elements[cursor]);
                        cursor++;
                    } else if (predicate.test(elements[cursor], seed) == preCondition) {
                        result.add(elements[cursor]);
                        cursor++;
                    } else {
                        if (seedUpdate != null) {
                            seedUpdate.accept(seed);
                        }

                        break;
                    }
                }

                return result;
            }

        }, closeHandlers);
    }

    @Override
    public Stream<Stream<T>> splitAt(final int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        final Stream<T>[] a = new Stream[2];
        final int middleIndex = n < toIndex - fromIndex ? fromIndex + n : toIndex;
        a[0] = middleIndex == fromIndex ? (Stream<T>) Stream.empty() : new ArrayStream<>(elements, fromIndex, middleIndex, sorted, cmp, null);
        a[1] = middleIndex == toIndex ? (Stream<T>) Stream.empty() : new ArrayStream<>(elements, middleIndex, toIndex, sorted, cmp, null);

        return new ArrayStream<>(a, closeHandlers);
    }

    @Override
    public Stream<Stream<T>> splitBy(Predicate<? super T> where) {
        N.requireNonNull(where);

        int n = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            if (where.test(elements[i])) {
                n++;
            } else {
                break;
            }
        }

        return splitAt(n);
    }

    @Override
    public Stream<Stream<T>> sliding(final int windowSize, final int increment) {
        N.checkArgument(windowSize > 0 && increment > 0, "'windowSize'=%s and 'increment'=%s must not be less than 1", windowSize, increment);

        return new IteratorStream<>(new ObjIteratorEx<Stream<T>>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final Stream<T> result = new ArrayStream<>(elements, cursor, windowSize < toIndex - cursor ? cursor + windowSize : toIndex, sorted, cmp, null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            @Override
            public long count() {
                if (toIndex - cursor == 0) {
                    return 0;
                } else if (toIndex - cursor <= windowSize) {
                    return 1;
                } else {
                    final long len = (toIndex - cursor) - windowSize;
                    return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
                }
            }

            @Override
            public void skip(long n) {
                if (n > 0) {
                    if (n >= count()) {
                        cursor = toIndex;
                    } else {
                        cursor += n * increment;
                    }
                }
            }
        }, closeHandlers);
    }

    @Override
    public Stream<List<T>> slidingToList(final int windowSize, final int increment) {
        N.checkArgument(windowSize > 0 && increment > 0, "'windowSize'=%s and 'increment'=%s must not be less than 1", windowSize, increment);

        return new IteratorStream<>(new ObjIteratorEx<List<T>>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public List<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final List<T> result = N.asList(N.copyOfRange(elements, cursor, windowSize < toIndex - cursor ? cursor + windowSize : toIndex));

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            @Override
            public long count() {
                if (toIndex - cursor == 0) {
                    return 0;
                } else if (toIndex - cursor <= windowSize) {
                    return 1;
                } else {
                    final long len = (toIndex - cursor) - windowSize;
                    return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
                }
            }

            @Override
            public void skip(long n) {
                if (n > 0) {
                    if (n >= count()) {
                        cursor = toIndex;
                    } else {
                        cursor += n * increment;
                    }
                }
            }

        }, closeHandlers);
    }

    @Override
    public Stream<T> top(int n) {
        return top(n, OBJECT_COMPARATOR);
    }

    @Override
    public Stream<T> top(int n, Comparator<? super T> comparator) {
        N.checkArgument(n > 0, "'n' must be bigger than 0");

        if (n >= toIndex - fromIndex) {
            return this;
        } else if (sorted && isSameComparator(comparator, cmp)) {
            return new ArrayStream<>(elements, toIndex - n, toIndex, sorted, cmp, closeHandlers);
        } else {
            return new ArrayStream<>(N.top(elements, fromIndex, toIndex, n, comparator), sorted, cmp, closeHandlers);
        }
    }

    @Override
    public Stream<T> sorted() {
        return sorted(OBJECT_COMPARATOR);
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        if (sorted && isSameComparator(comparator, cmp)) {
            return this;
        }

        final T[] a = N.copyOfRange(elements, fromIndex, toIndex);
        N.sort(a, comparator);
        return new ArrayStream<>(a, true, comparator, closeHandlers);
    }

    @Override
    public Stream<T> peek(final Consumer<? super T> action) {
        return new IteratorStream<>(new ObjIteratorEx<T>() {
            int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public T next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = (A) elements[cursor++];
                }

                return a;
            }
        }, sorted, cmp, closeHandlers);
    }

    @Override
    public Stream<T> limit(long maxSize) {
        N.checkArgument(maxSize >= 0, "'maxSizse' can't be negative: %s", maxSize);

        if (maxSize >= toIndex - fromIndex) {
            return this;
        }

        return new ArrayStream<>(elements, fromIndex, (int) (fromIndex + maxSize), sorted, cmp, closeHandlers);
    }

    @Override
    public Stream<T> skip(long n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0) {
            return this;
        }

        if (n >= toIndex - fromIndex) {
            return new ArrayStream<>(elements, toIndex, toIndex, sorted, cmp, closeHandlers);
        } else {
            return new ArrayStream<>(elements, (int) (fromIndex + n), toIndex, sorted, cmp, closeHandlers);
        }
    }

    @Override
    public <E extends Exception> void forEach(Try.Consumer<? super T, E> action) throws E {
        for (int i = fromIndex; i < toIndex; i++) {
            action.accept(elements[i]);
        }
    }

    @Override
    public <R, E extends Exception, E2 extends Exception> R forEach(R seed, Try.BiFunction<R, ? super T, R, E> accumulator,
            Try.BiPredicate<? super R, ? super T, E2> conditionToBreak) throws E, E2 {
        R result = seed;

        for (int i = fromIndex; i < toIndex; i++) {
            result = accumulator.apply(result, elements[i]);

            if (conditionToBreak.test(result, elements[i])) {
                break;
            }
        }

        return result;
    }

    @Override
    public <E extends Exception> void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action, final int increment) throws E {
        final int windowSize = 2;

        N.checkArgument(windowSize > 0 && increment > 0, "'windowSize'=%s and 'increment'=%s must not be less than 1", windowSize, increment);

        int cursor = fromIndex;

        while (cursor < toIndex) {
            action.accept(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null);

            cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;
        }
    }

    @Override
    public <E extends Exception> void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action, final int increment) throws E {
        final int windowSize = 3;

        N.checkArgument(windowSize > 0 && increment > 0, "'windowSize'=%s and 'increment'=%s must not be less than 1", windowSize, increment);

        int cursor = fromIndex;

        while (cursor < toIndex) {
            action.accept(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null, cursor < toIndex - 2 ? elements[cursor + 2] : null);

            cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;
        }
    }

    @Override
    public Object[] toArray() {
        return N.copyOfRange(elements, fromIndex, toIndex);
    }

    <A> A[] toArray(A[] a) {
        if (a.length < (toIndex - fromIndex)) {
            a = N.newArray(a.getClass().getComponentType(), toIndex - fromIndex);
        }

        N.copy(elements, fromIndex, a, 0, toIndex - fromIndex);

        return a;
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return toArray(generator.apply(toIndex - fromIndex));
    }

    @Override
    public List<T> toList() {
        // return N.asList(N.copyOfRange(elements, fromIndex, toIndex));

        if (fromIndex == 0 && toIndex == elements.length && elements.length > 9) {
            return new ArrayList<>(Arrays.asList(elements));
        }

        final List<T> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public <R extends List<T>> R toList(Supplier<R> supplier) {
        final R result = supplier.get();

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public Set<T> toSet() {
        final Set<T> result = new HashSet<>(N.min(9, N.initHashCapacity(toIndex - fromIndex)));

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public <R extends Set<T>> R toSet(Supplier<R> supplier) {
        final R result = supplier.get();

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public Multiset<T> toMultiset() {
        final Multiset<T> result = new Multiset<>(N.min(9, N.initHashCapacity(toIndex - fromIndex)));

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public Multiset<T> toMultiset(Supplier<? extends Multiset<T>> supplier) {
        final Multiset<T> result = supplier.get();

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public LongMultiset<T> toLongMultiset() {
        final LongMultiset<T> result = new LongMultiset<>(N.min(9, N.initHashCapacity(toIndex - fromIndex)));

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public LongMultiset<T> toLongMultiset(Supplier<? extends LongMultiset<T>> supplier) {
        final LongMultiset<T> result = supplier.get();

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public <K, U, M extends Map<K, U>> M toMap(Function<? super T, ? extends K> keyExtractor, Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction, Supplier<M> mapFactory) {
        final M result = mapFactory.get();

        for (int i = fromIndex; i < toIndex; i++) {
            Collectors.merge(result, keyExtractor.apply(elements[i]), valueMapper.apply(elements[i]), mergeFunction);
        }

        return result;
    }

    @Override
    public <K, A, D, M extends Map<K, D>> M toMap(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream,
            final Supplier<M> mapFactory) {
        final M result = mapFactory.get();
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        final Map<K, A> intermediate = (Map<K, A>) result;
        K key = null;
        A v = null;

        for (int i = fromIndex; i < toIndex; i++) {
            key = N.requireNonNull(classifier.apply(elements[i]), "element cannot be mapped to a null key");

            if ((v = intermediate.get(key)) == null) {
                if ((v = downstreamSupplier.get()) != null) {
                    intermediate.put(key, v);
                }
            }

            downstreamAccumulator.accept(v, elements[i]);
        }

        final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
            @Override
            public A apply(K k, A v) {
                return (A) downstream.finisher().apply(v);
            }
        };

        Collectors.replaceAll(intermediate, function);

        return result;
    }

    @Override
    public <K, U, A, D, M extends Map<K, D>> M toMap(final Function<? super T, ? extends K> classifier, final Function<? super T, ? extends U> valueMapper,
            final Collector<? super U, A, D> downstream, final Supplier<M> mapFactory) {
        final M result = mapFactory.get();
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
        final Map<K, A> intermediate = (Map<K, A>) result;
        K key = null;
        A v = null;

        for (int i = fromIndex; i < toIndex; i++) {
            key = N.requireNonNull(classifier.apply(elements[i]), "element cannot be mapped to a null key");

            if ((v = intermediate.get(key)) == null) {
                if ((v = downstreamSupplier.get()) != null) {
                    intermediate.put(key, v);
                }
            }

            downstreamAccumulator.accept(v, valueMapper.apply(elements[i]));
        }

        final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
            @Override
            public A apply(K k, A v) {
                return (A) downstream.finisher().apply(v);
            }
        };

        Collectors.replaceAll(intermediate, function);

        return result;
    }

    @Override
    public <K, U, V extends Collection<U>, M extends Multimap<K, U, V>> M toMultimap(Function<? super T, ? extends K> keyExtractor,
            Function<? super T, ? extends U> valueMapper, Supplier<M> mapFactory) {
        final M result = mapFactory.get();

        for (int i = fromIndex; i < toIndex; i++) {
            result.put(keyExtractor.apply(elements[i]), valueMapper.apply(elements[i]));
        }

        return result;
    }

    @Override
    public Nullable<T> first() {
        if (fromIndex == toIndex) {
            return Nullable.empty();
        }

        return Nullable.of(elements[fromIndex]);
    }

    @Override
    public Nullable<T> last() {
        if (fromIndex == toIndex) {
            return Nullable.empty();
        }

        return Nullable.of(elements[toIndex - 1]);
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        T result = identity;

        for (int i = fromIndex; i < toIndex; i++) {
            result = accumulator.apply(result, elements[i]);
        }

        return result;
    }

    @Override
    public Nullable<T> reduce(BinaryOperator<T> accumulator) {
        if (fromIndex == toIndex) {
            return Nullable.empty();
        }

        T result = elements[fromIndex];

        for (int i = fromIndex + 1; i < toIndex; i++) {
            result = accumulator.apply(result, elements[i]);
        }

        return Nullable.of(result);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        U result = identity;

        for (int i = fromIndex; i < toIndex; i++) {
            result = accumulator.apply(result, elements[i]);
        }

        return result;
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        final R result = supplier.get();

        for (int i = fromIndex; i < toIndex; i++) {
            accumulator.accept(result, elements[i]);
        }

        return result;
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        final A container = collector.supplier().get();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();

        for (int i = fromIndex; i < toIndex; i++) {
            accumulator.accept(container, elements[i]);
        }

        return collector.finisher().apply(container);
    }

    @Override
    public Nullable<T> head() {
        return fromIndex == toIndex ? Nullable.<T> empty() : Nullable.of(elements[fromIndex]);
    }

    @Override
    public Stream<T> tail() {
        if (fromIndex == toIndex) {
            return this;
        }

        return new ArrayStream<>(elements, fromIndex + 1, toIndex, sorted, cmp, closeHandlers);
    }

    @Override
    public Stream<T> headd() {
        if (fromIndex == toIndex) {
            return this;
        }

        return new ArrayStream<>(elements, fromIndex, toIndex - 1, sorted, cmp, closeHandlers);
    }

    @Override
    public Nullable<T> taill() {
        return fromIndex == toIndex ? Nullable.<T> empty() : Nullable.of(elements[toIndex - 1]);
    }

    @Override
    public Stream<T> last(final int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (toIndex - fromIndex <= n) {
            return this;
        }

        return new ArrayStream<>(elements, toIndex - n, toIndex, sorted, cmp, closeHandlers);
    }

    @Override
    public Stream<T> skipLast(int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0) {
            return this;
        }

        return new ArrayStream<>(elements, fromIndex, N.max(fromIndex, toIndex - n), sorted, cmp, closeHandlers);
    }

    @Override
    public Nullable<T> min(Comparator<? super T> comparator) {
        if (fromIndex == toIndex) {
            return Nullable.empty();
        } else if (sorted && isSameComparator(cmp, comparator)) {
            return Nullable.of(elements[fromIndex]);
        }

        return Nullable.of(N.min(elements, fromIndex, toIndex, comparator));
    }

    @Override
    public Nullable<T> max(Comparator<? super T> comparator) {
        if (fromIndex == toIndex) {
            return Nullable.empty();
        } else if (sorted && isSameComparator(cmp, comparator)) {
            return Nullable.of(elements[toIndex - 1]);
        }

        return Nullable.of(N.max(elements, fromIndex, toIndex, comparator));
    }

    @Override
    public Nullable<T> kthLargest(int k, Comparator<? super T> comparator) {
        N.checkArgument(k > 0, "'k' must be bigger than 0");

        if (k > toIndex - fromIndex) {
            return Nullable.empty();
        } else if (sorted && isSameComparator(cmp, comparator)) {
            return Nullable.of(elements[toIndex - k]);
        }

        return Nullable.of(N.kthLargest(elements, fromIndex, toIndex, k, comparator));
    }

    @Override
    public long count() {
        return toIndex - fromIndex;
    }

    @Override
    public Stream<T> reversed() {
        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private int cursor = toIndex;

            @Override
            public boolean hasNext() {
                return cursor > fromIndex;
            }

            @Override
            public T next() {
                if (cursor <= fromIndex) {
                    throw new NoSuchElementException();
                }

                return elements[--cursor];
            }

            @Override
            public long count() {
                return cursor - fromIndex;
            }

            @Override
            public void skip(long n) {
                cursor = n < cursor - fromIndex ? cursor - (int) n : fromIndex;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= cursor - fromIndex ? a : (A[]) N.newArray(a.getClass().getComponentType(), cursor - fromIndex);

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = (A) elements[cursor - i - 1];
                }

                return a;
            }
        }, closeHandlers);
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Try.Predicate<? super T, E> predicate) throws E {
        for (int i = fromIndex; i < toIndex; i++) {
            if (predicate.test(elements[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public <E extends Exception> boolean allMatch(final Try.Predicate<? super T, E> predicate) throws E {
        for (int i = fromIndex; i < toIndex; i++) {
            if (predicate.test(elements[i]) == false) {
                return false;
            }
        }

        return true;
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Try.Predicate<? super T, E> predicate) throws E {
        for (int i = fromIndex; i < toIndex; i++) {
            if (predicate.test(elements[i])) {
                return false;
            }
        }

        return true;
    }

    @Override
    public <E extends Exception> Nullable<T> findFirst(final Try.Predicate<? super T, E> predicate) throws E {
        for (int i = fromIndex; i < toIndex; i++) {
            if (predicate.test(elements[i])) {
                return Nullable.of(elements[i]);
            }
        }

        return (Nullable<T>) Nullable.empty();
    }

    @Override
    public <E extends Exception> Nullable<T> findLast(final Try.Predicate<? super T, E> predicate) throws E {
        for (int i = toIndex - 1; i >= fromIndex; i--) {
            if (predicate.test(elements[i])) {
                return Nullable.of(elements[i]);
            }
        }

        return (Nullable<T>) Nullable.empty();
    }

    @Override
    public Stream<T> cached() {
        return this;
    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterator asynchronously.
     * 
     * @param stream
     * @param queueSize Default value is 8
     * @return
     */
    @Override
    public Stream<T> queued(int queueSize) {
        return this;
    }

    @Override
    ObjIteratorEx<T> iteratorEx() {
        return ObjIteratorEx.of(elements, fromIndex, toIndex);
    }

    @Override
    public Stream<T> parallel(int maxThreadNum, BaseStream.Splitor splitor) {
        return new ParallelArrayStream<>(elements, fromIndex, toIndex, sorted, cmp, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        final Set<Runnable> newCloseHandlers = new LocalLinkedHashSet<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        newCloseHandlers.add(closeHandler);

        return new ArrayStream<>(elements, fromIndex, toIndex, sorted, cmp, newCloseHandlers);
    }
}
