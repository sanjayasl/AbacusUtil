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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.landawn.abacus.util.CompletableFuture;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IndexedShort;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Nullable;
import com.landawn.abacus.util.OptionalDouble;
import com.landawn.abacus.util.OptionalShort;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjShortConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortToIntFunction;
import com.landawn.abacus.util.function.ShortTriFunction;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToShortFunction;

/**
 * This class is a sequential, stateful and immutable stream implementation.
 *
 * @since 0.8
 * 
 * @author Haiyang Li
 */
final class ParallelArrayShortStream extends ArrayShortStream {
    private final int maxThreadNum;
    private final Splitor splitor;
    private volatile ArrayShortStream sequential;
    private volatile Stream<Short> boxed;

    ParallelArrayShortStream(short[] values, final int fromIndex, final int toIndex, final boolean sorted, int maxThreadNum, Splitor splitor,
            final Collection<Runnable> closeHandlers) {
        super(values, fromIndex, toIndex, sorted, closeHandlers);

        this.maxThreadNum = checkMaxThreadNum(maxThreadNum);
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
    }

    @Override
    public ShortStream filter(final ShortPredicate predicate) {
        if (maxThreadNum <= 1) {
            return new ParallelIteratorShortStream(sequential().filter(predicate).iteratorEx(), sorted, maxThreadNum, splitor, closeHandlers);
        }

        final Stream<Short> stream = boxed().filter(new Predicate<Short>() {
            @Override
            public boolean test(Short value) {
                return predicate.test(value);
            }
        });

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream takeWhile(final ShortPredicate predicate) {
        if (maxThreadNum <= 1) {
            return new ParallelIteratorShortStream(sequential().takeWhile(predicate).iteratorEx(), sorted, maxThreadNum, splitor, closeHandlers);
        }

        final Stream<Short> stream = boxed().takeWhile(new Predicate<Short>() {
            @Override
            public boolean test(Short value) {
                return predicate.test(value);
            }
        });

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream dropWhile(final ShortPredicate predicate) {
        if (maxThreadNum <= 1) {
            return new ParallelIteratorShortStream(sequential().dropWhile(predicate).iteratorEx(), sorted, maxThreadNum, splitor, closeHandlers);
        }

        final Stream<Short> stream = boxed().dropWhile(new Predicate<Short>() {
            @Override
            public boolean test(Short value) {
                return predicate.test(value);
            }
        });

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream map(final ShortUnaryOperator mapper) {
        if (maxThreadNum <= 1) {
            return new ParallelIteratorShortStream(sequential().map(mapper).iteratorEx(), false, maxThreadNum, splitor, closeHandlers);
        }

        final ShortStream stream = boxed().mapToShort(new ToShortFunction<Short>() {
            @Override
            public short applyAsShort(Short value) {
                return mapper.applyAsShort(value);
            }
        });

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public IntStream mapToInt(final ShortToIntFunction mapper) {
        if (maxThreadNum <= 1) {
            return new ParallelIteratorIntStream(sequential().mapToInt(mapper).iteratorEx(), false, maxThreadNum, splitor, closeHandlers);
        }

        final IntStream stream = boxed().mapToInt(new ToIntFunction<Short>() {
            @Override
            public int applyAsInt(Short value) {
                return mapper.applyAsInt(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public <U> Stream<U> mapToObj(final ShortFunction<? extends U> mapper) {
        if (maxThreadNum <= 1) {
            return new ParallelIteratorStream<U>(sequential().mapToObj(mapper).iterator(), false, null, maxThreadNum, splitor, closeHandlers);
        }

        return boxed().map(new Function<Short, U>() {
            @Override
            public U apply(Short value) {
                return mapper.apply(value);
            }
        });
    }

    @Override
    public ShortStream flatMap(final ShortFunction<? extends ShortStream> mapper) {
        if (maxThreadNum <= 1) {
            return new ParallelIteratorShortStream(sequential().flatMap(mapper), false, maxThreadNum, splitor, null);
        }

        final ShortStream stream = boxed().flatMapToShort(new Function<Short, ShortStream>() {
            @Override
            public ShortStream apply(Short value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, null);
    }

    @Override
    public IntStream flatMapToInt(final ShortFunction<? extends IntStream> mapper) {
        if (maxThreadNum <= 1) {
            return new ParallelIteratorIntStream(sequential().flatMapToInt(mapper), false, maxThreadNum, splitor, null);
        }

        final IntStream stream = boxed().flatMapToInt(new Function<Short, IntStream>() {
            @Override
            public IntStream apply(Short value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, null);
    }

    @Override
    public <T> Stream<T> flatMapToObj(final ShortFunction<? extends Stream<T>> mapper) {
        if (maxThreadNum <= 1) {
            return new ParallelIteratorStream<>(sequential().flatMapToObj(mapper), false, null, maxThreadNum, splitor, null);
        }

        return boxed().flatMap(new Function<Short, Stream<T>>() {
            @Override
            public Stream<T> apply(Short value) {
                return mapper.apply(value);
            }
        });
    }

    @Override
    public Stream<ShortStream> split(final int size) {
        return new ParallelIteratorStream<ShortStream>(sequential().split(size).iterator(), false, null, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public Stream<ShortList> splitToList(final int size) {
        return new ParallelIteratorStream<ShortList>(sequential().splitToList(size).iterator(), false, null, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public <U> Stream<ShortStream> split(final U seed, final BiPredicate<? super Short, ? super U> predicate, final Consumer<? super U> seedUpdate) {
        return new ParallelIteratorStream<ShortStream>(sequential().split(seed, predicate, seedUpdate).iterator(), false, null, maxThreadNum, splitor,
                closeHandlers);
    }

    @Override
    public <U> Stream<ShortList> splitToList(final U seed, final BiPredicate<? super Short, ? super U> predicate, final Consumer<? super U> seedUpdate) {
        return new ParallelIteratorStream<ShortList>(sequential().splitToList(seed, predicate, seedUpdate).iterator(), false, null, maxThreadNum, splitor,
                closeHandlers);
    }

    @Override
    public Stream<ShortStream> splitAt(final int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        final ShortStream[] a = new ShortStream[2];
        final int middleIndex = n < toIndex - fromIndex ? fromIndex + n : toIndex;
        a[0] = middleIndex == fromIndex ? ShortStream.empty() : new ArrayShortStream(elements, fromIndex, middleIndex, sorted, null);
        a[1] = middleIndex == toIndex ? ShortStream.empty() : new ArrayShortStream(elements, middleIndex, toIndex, sorted, null);

        return new ParallelArrayStream<>(a, 0, a.length, false, null, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public Stream<ShortStream> splitBy(final ShortPredicate where) {
        N.requireNonNull(where);

        final Nullable<IndexedShort> first = indexed().findFirst(new Predicate<IndexedShort>() {
            @Override
            public boolean test(IndexedShort indexed) {
                return !where.test(indexed.value());
            }
        });

        return splitAt(first.isPresent() ? (int) first.get().index() : toIndex - fromIndex);
    }

    @Override
    public Stream<ShortStream> sliding(final int windowSize, final int increment) {
        return new ParallelIteratorStream<ShortStream>(sequential().sliding(windowSize, increment).iterator(), false, null, maxThreadNum, splitor,
                closeHandlers);
    }

    @Override
    public Stream<ShortList> slidingToList(final int windowSize, final int increment) {
        return new ParallelIteratorStream<ShortList>(sequential().slidingToList(windowSize, increment).iterator(), false, null, maxThreadNum, splitor,
                closeHandlers);
    }

    @Override
    public ShortStream top(int n) {
        return top(n, SHORT_COMPARATOR);
    }

    @Override
    public ShortStream top(int n, Comparator<? super Short> comparator) {
        N.checkArgument(n > 0, "'n' must be bigger than 0");

        if (n >= toIndex - fromIndex) {
            return this;
        } else if (sorted && isSameComparator(comparator, SHORT_COMPARATOR)) {
            return new ParallelArrayShortStream(elements, toIndex - n, toIndex, sorted, maxThreadNum, splitor, closeHandlers);
        } else {
            final short[] a = N.top(elements, fromIndex, toIndex, n, comparator);
            return new ParallelArrayShortStream(a, 0, a.length, sorted, maxThreadNum, splitor, closeHandlers);
        }
    }

    @Override
    public ShortStream sorted() {
        if (sorted) {
            return this;
        }

        final short[] a = N.copyOfRange(elements, fromIndex, toIndex);
        N.parallelSort(a);
        return new ParallelArrayShortStream(a, 0, a.length, true, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream peek(final ShortConsumer action) {
        if (maxThreadNum <= 1) {
            return new ParallelIteratorShortStream(sequential().peek(action).iteratorEx(), false, maxThreadNum, splitor, closeHandlers);
        }

        final ShortStream stream = boxed().peek(new Consumer<Short>() {
            @Override
            public void accept(Short t) {
                action.accept(t);
            }
        }).sequential().mapToShort(ToShortFunction.UNBOX);

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream limit(long maxSize) {
        N.checkArgument(maxSize >= 0, "'maxSizse' can't be negative: %s", maxSize);

        if (maxSize >= toIndex - fromIndex) {
            return this;
        }

        return new ParallelArrayShortStream(elements, fromIndex, (int) (fromIndex + maxSize), sorted, maxThreadNum, splitor, closeHandlers);

    }

    @Override
    public ShortStream skip(long n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0) {
            return this;
        }

        if (n >= toIndex - fromIndex) {
            return new ParallelArrayShortStream(elements, toIndex, toIndex, sorted, maxThreadNum, splitor, closeHandlers);
        } else {
            return new ParallelArrayShortStream(elements, (int) (fromIndex + n), toIndex, sorted, maxThreadNum, splitor, closeHandlers);
        }
    }

    @Override
    public <E extends Exception> void forEach(final Try.ShortConsumer<E> action) throws E {
        if (maxThreadNum <= 1) {
            sequential().forEach(action);
            return;
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                action.accept(elements[cursor++]);
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        short next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                action.accept(next);
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        complette(futureList, eHolder, (E) null);
    }

    @Override
    public short[] toArray() {
        return N.copyOfRange(elements, fromIndex, toIndex);
    }

    @Override
    public ShortList toShortList() {
        return ShortList.of(N.copyOfRange(elements, fromIndex, toIndex));
    }

    @Override
    public List<Short> toList() {
        final List<Short> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public <R extends List<Short>> R toList(Supplier<R> supplier) {
        final R result = supplier.get();

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public Set<Short> toSet() {
        final Set<Short> result = new HashSet<>(N.min(9, N.initHashCapacity(toIndex - fromIndex)));

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public <R extends Set<Short>> R toSet(Supplier<R> supplier) {
        final R result = supplier.get();

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public Multiset<Short> toMultiset() {
        final Multiset<Short> result = new Multiset<>(N.min(9, N.initHashCapacity(toIndex - fromIndex)));

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public Multiset<Short> toMultiset(Supplier<? extends Multiset<Short>> supplier) {
        final Multiset<Short> result = supplier.get();

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public LongMultiset<Short> toLongMultiset() {
        final LongMultiset<Short> result = new LongMultiset<>(N.min(9, N.initHashCapacity(toIndex - fromIndex)));

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public LongMultiset<Short> toLongMultiset(Supplier<? extends LongMultiset<Short>> supplier) {
        final LongMultiset<Short> result = supplier.get();

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]);
        }

        return result;
    }

    @Override
    public <K, U, M extends Map<K, U>> M toMap(final ShortFunction<? extends K> keyExtractor, final ShortFunction<? extends U> valueMapper,
            final BinaryOperator<U> mergeFunction, final Supplier<M> mapFactory) {
        if (maxThreadNum <= 1) {
            return sequential().toMap(keyExtractor, valueMapper, mergeFunction, mapFactory);
        }

        final Function<? super Short, ? extends K> keyExtractor2 = new Function<Short, K>() {
            @Override
            public K apply(Short value) {
                return keyExtractor.apply(value);
            }
        };

        final Function<? super Short, ? extends U> valueMapper2 = new Function<Short, U>() {
            @Override
            public U apply(Short value) {
                return valueMapper.apply(value);
            }
        };

        return boxed().toMap(keyExtractor2, valueMapper2, mergeFunction, mapFactory);
    }

    @Override
    public <K, A, D, M extends Map<K, D>> M toMap(final ShortFunction<? extends K> classifier, final Collector<Short, A, D> downstream,
            final Supplier<M> mapFactory) {
        if (maxThreadNum <= 1) {
            return sequential().toMap(classifier, downstream, mapFactory);
        }

        final Function<? super Short, ? extends K> classifier2 = new Function<Short, K>() {
            @Override
            public K apply(Short value) {
                return classifier.apply(value);
            }
        };

        return boxed().toMap(classifier2, downstream, mapFactory);
    }

    @Override
    public OptionalShort first() {
        return fromIndex < toIndex ? OptionalShort.of(elements[fromIndex]) : OptionalShort.empty();
    }

    @Override
    public OptionalShort last() {
        return fromIndex < toIndex ? OptionalShort.of(elements[toIndex - 1]) : OptionalShort.empty();
    }

    @Override
    public short reduce(final short identity, final ShortBinaryOperator op) {
        if (maxThreadNum <= 1) {
            return sequential().reduce(identity, op);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Short>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Callable<Short>() {
                    @Override
                    public Short call() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        short result = identity;

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                result = op.applyAsShort(result, elements[cursor++]);
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }

                        return result;
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Callable<Short>() {
                    @Override
                    public Short call() {
                        short result = identity;
                        short next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                result = op.applyAsShort(result, next);
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }

                        return result;
                    }
                }));
            }
        }

        if (eHolder.value() != null) {
            throw N.toRuntimeException(eHolder.value());
        }

        Short result = null;

        try {
            for (CompletableFuture<Short> future : futureList) {
                if (result == null) {
                    result = future.get();
                } else {
                    result = op.applyAsShort(result, future.get());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        }

        return result == null ? identity : result;
    }

    @Override
    public OptionalShort reduce(final ShortBinaryOperator accumulator) {
        if (maxThreadNum <= 1) {
            return sequential().reduce(accumulator);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Short>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Callable<Short>() {
                    @Override
                    public Short call() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        if (cursor >= to) {
                            return null;
                        }

                        short result = elements[cursor++];

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                result = accumulator.applyAsShort(result, elements[cursor++]);
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }

                        return result;
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Callable<Short>() {
                    @Override
                    public Short call() {
                        short result = 0;

                        synchronized (elements) {
                            if (cursor.intValue() < toIndex) {
                                result = elements[cursor.getAndIncrement()];
                            } else {
                                return null;
                            }
                        }

                        short next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                result = accumulator.applyAsShort(result, next);
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }

                        return result;
                    }
                }));
            }
        }

        if (eHolder.value() != null) {
            throw N.toRuntimeException(eHolder.value());
        }

        Short result = null;

        try {
            for (CompletableFuture<Short> future : futureList) {
                final Short tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (result == null) {
                    result = tmp;
                } else {
                    result = accumulator.applyAsShort(result, tmp);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        }

        return result == null ? OptionalShort.empty() : OptionalShort.of(result);
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjShortConsumer<R> accumulator, final BiConsumer<R, R> combiner) {
        if (maxThreadNum <= 1) {
            return sequential().collect(supplier, accumulator, combiner);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<R>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Callable<R>() {
                    @Override
                    public R call() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        final R container = supplier.get();

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                accumulator.accept(container, elements[cursor++]);
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }

                        return container;
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Callable<R>() {
                    @Override
                    public R call() {
                        final R container = supplier.get();
                        short next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                accumulator.accept(container, next);
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }

                        return container;
                    }
                }));
            }
        }

        if (eHolder.value() != null) {
            throw N.toRuntimeException(eHolder.value());
        }

        R container = (R) NONE;

        try {
            for (CompletableFuture<R> future : futureList) {
                if (container == NONE) {
                    container = future.get();
                } else {
                    combiner.accept(container, future.get());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        }

        return container == NONE ? supplier.get() : container;
    }

    @Override
    public ShortStream tail() {
        if (fromIndex == toIndex) {
            return this;
        }

        return new ParallelArrayShortStream(elements, fromIndex + 1, toIndex, sorted, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream headd() {
        if (fromIndex == toIndex) {
            return this;
        }

        return new ParallelArrayShortStream(elements, fromIndex, toIndex - 1, sorted, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public OptionalShort min() {
        if (fromIndex == toIndex) {
            return OptionalShort.empty();
        } else if (sorted) {
            return OptionalShort.of(elements[fromIndex]);
        } else if (maxThreadNum <= 1) {
            return OptionalShort.of(N.min(elements, fromIndex, toIndex));
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Short>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<Short>() {
                @Override
                public Short call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    return cursor >= to ? null : N.min(elements, cursor, to);
                }
            }));
        }

        Short candidate = null;

        try {
            for (CompletableFuture<Short> future : futureList) {
                final Short tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (candidate == null || tmp.shortValue() < candidate.shortValue()) {
                    candidate = tmp;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        }

        return candidate == null ? OptionalShort.empty() : OptionalShort.of(candidate);
    }

    @Override
    public OptionalShort max() {
        if (fromIndex == toIndex) {
            return OptionalShort.empty();
        } else if (sorted) {
            return OptionalShort.of(elements[toIndex - 1]);
        } else if (maxThreadNum <= 1) {
            return OptionalShort.of(N.max(elements, fromIndex, toIndex));
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Short>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<Short>() {
                @Override
                public Short call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    return cursor >= to ? null : N.max(elements, cursor, to);
                }
            }));
        }

        Short candidate = null;

        try {
            for (CompletableFuture<Short> future : futureList) {
                final Short tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (candidate == null || tmp.shortValue() > candidate.shortValue()) {
                    candidate = tmp;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        }

        return candidate == null ? OptionalShort.empty() : OptionalShort.of(candidate);
    }

    @Override
    public OptionalShort kthLargest(int k) {
        N.checkArgument(k > 0, "'k' must be bigger than 0");

        if (k > toIndex - fromIndex) {
            return OptionalShort.empty();
        } else if (sorted) {
            return OptionalShort.of(elements[toIndex - k]);
        }

        return OptionalShort.of(N.kthLargest(elements, fromIndex, toIndex, k));
    }

    @Override
    public long sum() {
        if (fromIndex == toIndex) {
            return 0L;
        } else if (maxThreadNum <= 1) {
            return sum(elements, fromIndex, toIndex);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Long>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<Long>() {
                @Override
                public Long call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    return cursor >= to ? null : sum(elements, cursor, to);
                }
            }));
        }

        long result = 0;

        try {
            for (CompletableFuture<Long> future : futureList) {
                final Long tmp = future.get();

                if (tmp == null) {
                    continue;
                } else {
                    result += tmp.longValue();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        }

        return result;
    }

    @Override
    public OptionalDouble average() {
        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(sum() / toIndex - fromIndex);
    }

    @Override
    public long count() {
        return toIndex - fromIndex;
    }

    @Override
    public ShortStream reversed() {
        return new ParallelIteratorShortStream(sequential().reversed().iteratorEx(), false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortSummaryStatistics summarize() {
        if (fromIndex == toIndex) {
            return new ShortSummaryStatistics();
        } else if (maxThreadNum <= 1) {
            return sequential().summarize();
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<ShortSummaryStatistics>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<ShortSummaryStatistics>() {
                @Override
                public ShortSummaryStatistics call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    final ShortSummaryStatistics result = new ShortSummaryStatistics();

                    for (int i = cursor; i < to; i++) {
                        result.accept(elements[i]);
                    }

                    return result;
                }
            }));
        }

        ShortSummaryStatistics result = null;

        try {
            for (CompletableFuture<ShortSummaryStatistics> future : futureList) {
                final ShortSummaryStatistics tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (result == null) {
                    result = tmp;
                } else {
                    result.combine(tmp);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        }

        return result;
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Try.ShortPredicate<E> predicate) throws E {
        if (maxThreadNum <= 1) {
            return sequential().anyMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(false);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        try {
                            while (cursor < to && result.isFalse() && eHolder.value() == null) {
                                if (predicate.test(elements[cursor++])) {
                                    result.setTrue();
                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        short next = 0;

                        try {
                            while (result.isFalse() && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(next)) {
                                    result.setTrue();
                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        complette(futureList, eHolder, (E) null);

        return result.value();
    }

    @Override
    public <E extends Exception> boolean allMatch(final Try.ShortPredicate<E> predicate) throws E {
        if (maxThreadNum <= 1) {
            return sequential().allMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        try {
                            while (cursor < to && result.isTrue() && eHolder.value() == null) {
                                if (predicate.test(elements[cursor++]) == false) {
                                    result.setFalse();
                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        short next = 0;

                        try {
                            while (result.isTrue() && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(next) == false) {
                                    result.setFalse();
                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        complette(futureList, eHolder, (E) null);

        return result.value();
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Try.ShortPredicate<E> predicate) throws E {
        if (maxThreadNum <= 1) {
            return sequential().noneMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        try {
                            while (cursor < to && result.isTrue() && eHolder.value() == null) {
                                if (predicate.test(elements[cursor++])) {
                                    result.setFalse();
                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        short next = 0;

                        try {
                            while (result.isTrue() && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(next)) {
                                    result.setFalse();
                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        complette(futureList, eHolder, (E) null);

        return result.value();
    }

    @Override
    public <E extends Exception> OptionalShort findFirst(final Try.ShortPredicate<E> predicate) throws E {
        if (maxThreadNum <= 1) {
            return sequential().findFirst(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Short>> resultHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                        final Pair<Integer, Short> pair = new Pair<>();

                        try {
                            while (cursor < to && (resultHolder.value() == null || cursor < resultHolder.value().left) && eHolder.value() == null) {
                                pair.left = cursor;
                                pair.right = elements[cursor++];

                                if (predicate.test(pair.right)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == null || pair.left < resultHolder.value().left) {
                                            resultHolder.setValue(pair.copy());
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final Pair<Integer, Short> pair = new Pair<>();

                        try {
                            while (resultHolder.value() == null && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        pair.left = cursor.intValue();
                                        pair.right = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(pair.right)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == null || pair.left < resultHolder.value().left) {
                                            resultHolder.setValue(pair.copy());
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        complette(futureList, eHolder, (E) null);

        return resultHolder.value() == null ? OptionalShort.empty() : OptionalShort.of(resultHolder.value().right);
    }

    @Override
    public <E extends Exception> OptionalShort findLast(final Try.ShortPredicate<E> predicate) throws E {
        if (maxThreadNum <= 1) {
            return sequential().findLast(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Short>> resultHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final int from = fromIndex + sliceIndex * sliceSize;
                        int cursor = toIndex - from > sliceSize ? from + sliceSize : toIndex;
                        final Pair<Integer, Short> pair = new Pair<>();

                        try {
                            while (cursor > from && (resultHolder.value() == null || cursor > resultHolder.value().left) && eHolder.value() == null) {
                                pair.left = cursor;
                                pair.right = elements[--cursor];

                                if (predicate.test(pair.right)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == null || pair.left > resultHolder.value().left) {
                                            resultHolder.setValue(pair.copy());
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(toIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final Pair<Integer, Short> pair = new Pair<>();

                        try {
                            while (resultHolder.value() == null && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() > fromIndex) {
                                        pair.left = cursor.intValue();
                                        pair.right = elements[cursor.decrementAndGet()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(pair.right)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == null || pair.left > resultHolder.value().left) {
                                            resultHolder.setValue(pair.copy());
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        complette(futureList, eHolder, (E) null);

        return resultHolder.value() == null ? OptionalShort.empty() : OptionalShort.of(resultHolder.value().right);
    }

    @Override
    public <E extends Exception> OptionalShort findAny(final Try.ShortPredicate<E> predicate) throws E {
        if (maxThreadNum <= 1) {
            return sequential().findAny(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<CompletableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Object> resultHolder = Holder.of(NONE);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                        short next = 0;

                        try {
                            while (cursor < to && resultHolder.value() == NONE && eHolder.value() == null) {
                                next = elements[cursor++];

                                if (predicate.test(next)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == NONE) {
                                            resultHolder.setValue(next);
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        short next = 0;

                        try {
                            while (resultHolder.value() == NONE && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(next)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == NONE) {
                                            resultHolder.setValue(next);
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        complette(futureList, eHolder, (E) null);

        return resultHolder.value() == NONE ? OptionalShort.empty() : OptionalShort.of((Short) resultHolder.value());
    }

    @Override
    public IntStream asIntStream() {
        return new ParallelIteratorIntStream(new IntIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public int nextInt() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return elements[cursor++];
            }

            @Override
            public long count() {
                return toIndex - cursor;
            }

            @Override
            public void skip(long n) {
                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public int[] toArray() {
                final int[] a = new int[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, sorted, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public Stream<Short> boxed() {
        Stream<Short> tmp = boxed;

        if (tmp == null) {
            tmp = new ParallelIteratorStream<Short>(iterator(), sorted, sorted ? SHORT_COMPARATOR : null, maxThreadNum, splitor, closeHandlers);
            boxed = tmp;
        }

        return tmp;
    }

    @Override
    public ShortStream cached() {
        return this;
    }

    @Override
    public ShortStream append(ShortStream stream) {
        return new ParallelIteratorShortStream(ShortStream.concat(this, stream), false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream prepend(ShortStream stream) {
        return new ParallelIteratorShortStream(ShortStream.concat(stream, this), false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream merge(final ShortStream b, final ShortBiFunction<Nth> nextSelector) {
        return new ParallelIteratorShortStream(ShortStream.merge(this, b, nextSelector), false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream zipWith(ShortStream b, ShortBiFunction<Short> zipFunction) {
        return new ParallelIteratorShortStream(ShortStream.zip(this, b, zipFunction), false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream zipWith(ShortStream b, ShortStream c, ShortTriFunction<Short> zipFunction) {
        return new ParallelIteratorShortStream(ShortStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream zipWith(ShortStream b, short valueForNoneA, short valueForNoneB, ShortBiFunction<Short> zipFunction) {
        return new ParallelIteratorShortStream(ShortStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitor,
                closeHandlers);
    }

    @Override
    public ShortStream zipWith(ShortStream b, ShortStream c, short valueForNoneA, short valueForNoneB, short valueForNoneC,
            ShortTriFunction<Short> zipFunction) {
        return new ParallelIteratorShortStream(ShortStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                splitor, closeHandlers);
    }

    @Override
    public boolean isParallel() {
        return true;
    }

    @Override
    public ShortStream sequential() {
        ArrayShortStream tmp = sequential;

        if (tmp == null) {
            tmp = new ArrayShortStream(elements, fromIndex, toIndex, sorted, closeHandlers);
            sequential = tmp;
        }

        return tmp;
    }

    @Override
    public ShortStream parallel(int maxThreadNum, Splitor splitor) {
        if (this.maxThreadNum == checkMaxThreadNum(maxThreadNum) && this.splitor == splitor) {
            return this;
        }

        return new ParallelArrayShortStream(elements, fromIndex, toIndex, sorted, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public int maxThreadNum() {
        return maxThreadNum;
    }

    @Override
    public ShortStream maxThreadNum(int maxThreadNum) {
        if (this.maxThreadNum == checkMaxThreadNum(maxThreadNum)) {
            return this;
        }

        return new ParallelArrayShortStream(elements, fromIndex, toIndex, sorted, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public BaseStream.Splitor splitor() {
        return splitor;
    }

    @Override
    public ShortStream splitor(BaseStream.Splitor splitor) {
        if (this.splitor == splitor) {
            return this;
        }

        return new ParallelArrayShortStream(elements, fromIndex, toIndex, sorted, maxThreadNum, splitor, closeHandlers);
    }

    @Override
    public ShortStream onClose(Runnable closeHandler) {
        final Set<Runnable> newCloseHandlers = new AbstractStream.LocalLinkedHashSet<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        newCloseHandlers.add(closeHandler);

        return new ParallelArrayShortStream(elements, fromIndex, toIndex, sorted, maxThreadNum, splitor, newCloseHandlers);
    }
}
