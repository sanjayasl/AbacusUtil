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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharMatrix;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableChar;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Optional;
import com.landawn.abacus.util.OptionalChar;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharTriFunction;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjCharConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToCharFunction;

/**
 * This class is a sequential, stateful and immutable stream implementation.
 *
 * @since 0.8
 * 
 * @author Haiyang Li
 */
abstract class AbstractCharStream extends CharStream {

    AbstractCharStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public CharStream flattMap(final CharFunction<char[]> mapper) {
        return flatMap(new CharFunction<CharStream>() {
            @Override
            public CharStream apply(char t) {
                return CharStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public CharStream remove(final long n, final CharConsumer action) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0) {
            return this;
        }

        if (this.isParallel()) {
            final AtomicLong cnt = new AtomicLong(n);

            return removeWhile(new CharPredicate() {
                @Override
                public boolean test(char value) {
                    return cnt.getAndDecrement() > 0;
                }
            }, action);
        } else {
            final MutableLong cnt = MutableLong.of(n);

            return removeWhile(new CharPredicate() {
                @Override
                public boolean test(char value) {
                    return cnt.getAndDecrement() > 0;
                }
            }, action);
        }
    }

    @Override
    public CharStream removeIf(final CharPredicate predicate) {
        N.requireNonNull(predicate);

        return filter(new CharPredicate() {
            @Override
            public boolean test(char value) {
                return predicate.test(value) == false;
            }
        });
    }

    @Override
    public CharStream removeIf(final CharPredicate predicate, final CharConsumer action) {
        N.requireNonNull(predicate);
        N.requireNonNull(predicate);

        return filter(new CharPredicate() {
            @Override
            public boolean test(char value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return false;
                }

                return true;
            }
        });
    }

    @Override
    public CharStream removeWhile(final CharPredicate predicate, final CharConsumer action) {
        N.requireNonNull(predicate);
        N.requireNonNull(action);

        return dropWhile(new CharPredicate() {
            @Override
            public boolean test(char value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public CharStream step(final long step) {
        N.checkArgument(step > 0, "'step' can't be 0 or negative: %s", step);

        if (step == 1) {
            return this;
        }

        final long skip = step - 1;
        final CharIteratorEx iter = this.iteratorEx();

        final CharIterator charIterator = new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                final char next = iter.nextChar();
                iter.skip(skip);
                return next;
            }
        };

        return newStream(charIterator, sorted);
    }

    @Override
    public Stream<CharStream> split(final int size) {
        return splitToList(size).map(new Function<CharList, CharStream>() {
            @Override
            public CharStream apply(CharList t) {
                return new ArrayCharStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<CharStream> split(final CharPredicate predicate) {
        return splitToList(predicate).map(new Function<CharList, CharStream>() {
            @Override
            public CharStream apply(CharList t) {
                return new ArrayCharStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<CharList> splitToList(final CharPredicate predicate) {
        final BiPredicate<Character, Object> predicate2 = new BiPredicate<Character, Object>() {

            @Override
            public boolean test(Character t, Object u) {
                return predicate.test(t);
            }
        };

        return splitToList(null, predicate2, null);
    }

    @Override
    public <U> Stream<CharStream> split(final U seed, final BiPredicate<? super Character, ? super U> predicate, final Consumer<? super U> seedUpdate) {
        return splitToList(seed, predicate, seedUpdate).map(new Function<CharList, CharStream>() {
            @Override
            public CharStream apply(CharList t) {
                return new ArrayCharStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<CharStream> sliding(final int windowSize, final int increment) {
        return slidingToList(windowSize, increment).map(new Function<CharList, CharStream>() {
            @Override
            public CharStream apply(CharList t) {
                return new ArrayCharStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public CharStream collapse(final CharBiPredicate collapsible, final CharBiFunction<Character> mergeFunction) {
        final CharIteratorEx iter = iteratorEx();

        return this.newStream(new CharIteratorEx() {
            private boolean hasNext = false;
            private char next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public char nextChar() {
                char res = hasNext ? next : (next = iter.nextChar());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextChar()))) {
                        res = mergeFunction.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public CharStream scan(final CharBiFunction<Character> accumulator) {
        final CharIteratorEx iter = iteratorEx();

        return this.newStream(new CharIteratorEx() {
            private char res = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.nextChar());
                } else {
                    return (res = accumulator.apply(res, iter.nextChar()));
                }
            }
        }, false);
    }

    @Override
    public CharStream scan(final char seed, final CharBiFunction<Character> accumulator) {
        final CharIteratorEx iter = iteratorEx();

        return this.newStream(new CharIteratorEx() {
            private char res = seed;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                return (res = accumulator.apply(res, iter.nextChar()));
            }
        }, false);
    }

    @Override
    public CharStream reverseSorted() {
        return sorted().reversed();
    }

    @Override
    public <K, U> Map<K, U> toMap(CharFunction<? extends K> keyExtractor, CharFunction<? extends U> valueMapper) {
        final Supplier<Map<K, U>> mapFactory = Fn.Suppliers.ofMap();

        return toMap(keyExtractor, valueMapper, mapFactory);
    }

    @Override
    public <K, U, M extends Map<K, U>> M toMap(CharFunction<? extends K> keyExtractor, CharFunction<? extends U> valueMapper, Supplier<M> mapFactory) {
        final BinaryOperator<U> mergeFunction = Fn.throwingMerger();

        return toMap(keyExtractor, valueMapper, mergeFunction, mapFactory);
    }

    @Override
    public <K, U> Map<K, U> toMap(CharFunction<? extends K> keyExtractor, CharFunction<? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        final Supplier<Map<K, U>> mapFactory = Fn.Suppliers.ofMap();

        return toMap(keyExtractor, valueMapper, mergeFunction, mapFactory);
    }

    @Override
    public <K, A, D> Map<K, D> toMap(CharFunction<? extends K> classifier, Collector<Character, A, D> downstream) {
        final Supplier<Map<K, D>> mapFactory = Fn.Suppliers.ofMap();

        return toMap(classifier, downstream, mapFactory);
    }

    @Override
    public CharMatrix toMatrix() {
        return CharMatrix.of(toArray());
    }

    @Override
    public CharStream distinct() {
        final Set<Object> set = new HashSet<>();

        return newStream(this.sequential().filter(new CharPredicate() {
            @Override
            public boolean test(char value) {
                return set.add(value);
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public OptionalChar first() {
        final CharIterator iter = this.iteratorEx();

        return iter.hasNext() ? OptionalChar.of(iter.nextChar()) : OptionalChar.empty();
    }

    @Override
    public OptionalChar last() {
        final CharIterator iter = this.iteratorEx();

        if (iter.hasNext() == false) {
            return OptionalChar.empty();
        }

        char next = iter.nextChar();

        while (iter.hasNext()) {
            next = iter.nextChar();
        }

        return OptionalChar.of(next);
    }

    @Override
    public <E extends Exception> OptionalChar findAny(final Try.CharPredicate<E> predicate) throws E {
        return findFirst(predicate);
    }

    @Override
    public <E extends Exception, E2 extends Exception> OptionalChar findFirstOrLast(Try.CharPredicate<E> predicateForFirst,
            Try.CharPredicate<E> predicateForLast) throws E, E2 {
        final CharIteratorEx iter = iteratorEx();
        MutableChar last = null;
        char next = 0;

        while (iter.hasNext()) {
            next = iter.nextChar();

            if (predicateForFirst.test(next)) {
                return OptionalChar.of(next);
            } else if (predicateForLast.test(next)) {
                if (last == null) {
                    last = MutableChar.of(next);
                } else {
                    last.setValue(next);
                }
            }
        }

        return last == null ? OptionalChar.empty() : OptionalChar.of(last.value());
    }

    @Override
    public CharStream intersection(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new CharPredicate() {
            @Override
            public boolean test(char value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public CharStream difference(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new CharPredicate() {
            @Override
            public boolean test(char value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public CharStream symmetricDifference(final Collection<Character> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new CharPredicate() {
            @Override
            public boolean test(char value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).append(Stream.of(c).filter(new Predicate<Character>() {
            @Override
            public boolean test(Character value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).mapToChar(ToCharFunction.UNBOX)).iteratorEx(), false);
    }

    @Override
    public Stream<CharStream> splitAt(final int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        final CharIterator iter = this.iteratorEx();
        final CharList list = new CharList();

        while (list.size() < n && iter.hasNext()) {
            list.add(iter.nextChar());
        }

        final CharStream[] a = { new ArrayCharStream(list.array(), 0, list.size(), sorted, null), new IteratorCharStream(iter, sorted, null) };

        return this.newStream(a, false, null);
    }

    @Override
    public Stream<CharStream> splitBy(CharPredicate where) {
        N.requireNonNull(where);

        final CharIterator iter = this.iteratorEx();
        final CharList list = new CharList();
        char next = 0;
        CharStream s = null;

        while (iter.hasNext()) {
            next = iter.nextChar();

            if (where.test(next)) {
                list.add(next);
            } else {
                s = CharStream.of(next);

                break;
            }
        }

        final CharStream[] a = { new ArrayCharStream(list.array(), 0, list.size(), sorted, null), new IteratorCharStream(iter, sorted, null) };

        if (s != null) {
            if (sorted) {
                a[1] = new IteratorCharStream(a[1].prepend(s).iteratorEx(), sorted, null);
            } else {
                a[1] = a[1].prepend(s);
            }
        }

        return this.newStream(a, false, null);
    }

    @Override
    public CharStream reversed() {
        final char[] tmp = toArray();

        return newStream(new CharIteratorEx() {
            private int cursor = tmp.length;

            @Override
            public boolean hasNext() {
                return cursor > 0;
            }

            @Override
            public char nextChar() {
                if (cursor <= 0) {
                    throw new NoSuchElementException();
                }

                return tmp[--cursor];
            }

            @Override
            public long count() {
                return cursor;
            }

            @Override
            public void skip(long n) {
                cursor = n < cursor ? cursor - (int) n : 0;
            }

            @Override
            public char[] toArray() {
                final char[] a = new char[cursor];

                for (int i = 0, len = tmp.length; i < len; i++) {
                    a[i] = tmp[cursor - i - 1];
                }

                return a;
            }
        }, false);
    }

    @Override
    public CharStream shuffled() {
        final char[] a = toArray();

        N.shuffle(a);

        return newStream(a, false);
    }

    @Override
    public CharStream shuffled(final Random rnd) {
        final char[] a = toArray();

        N.shuffle(a, rnd);

        return newStream(a, false);
    }

    @Override
    public CharStream rotated(int distance) {
        final char[] a = toArray();

        N.rotate(a, distance);

        return newStream(a, false);
    }

    @Override
    public Optional<Map<Percentage, Character>> percentiles() {
        final char[] a = sorted().toArray();

        if (a.length == 0) {
            return Optional.empty();
        }

        return Optional.of(N.percentiles(a));
    }

    @Override
    public Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> summarizze() {
        final char[] a = sorted().toArray();

        if (N.isNullOrEmpty(a)) {
            return Pair.of(new CharSummaryStatistics(), Optional.<Map<Percentage, Character>> empty());
        } else {
            return Pair.of(new CharSummaryStatistics(a.length, sum(a), a[0], a[a.length - 1]), Optional.of(N.percentiles(a)));
        }
    }

    @Override
    public String join(final CharSequence delimiter) {
        return join(delimiter, "", "");
    }

    @Override
    public String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        final Joiner joiner = Joiner.with(delimiter, prefix, suffix).reuseStringBuilder(true);
        final CharIteratorEx iter = this.iteratorEx();

        while (iter.hasNext()) {
            joiner.append(iter.nextChar());
        }

        return joiner.toString();
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjCharConsumer<R> accumulator) {
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public Pair<OptionalChar, CharStream> headAndTail() {
        return Pair.of(head(), tail());
    }

    @Override
    public Pair<CharStream, OptionalChar> headAndTaill() {
        return Pair.of(headd(), taill());
    }

    @Override
    public Stream<IndexedChar> indexed() {
        final MutableLong idx = MutableLong.of(0);

        return newStream(this.sequential().mapToObj(new CharFunction<IndexedChar>() {
            @Override
            public IndexedChar apply(char t) {
                return IndexedChar.of(t, idx.getAndIncrement());
            }
        }).iterator(), true, INDEXED_CHAR_COMPARATOR);
    }

    @Override
    public CharStream append(CharStream stream) {
        return CharStream.concat(this, stream);
    }

    @Override
    public CharStream prepend(CharStream stream) {
        return CharStream.concat(stream, this);
    }

    @Override
    public CharStream merge(CharStream b, CharBiFunction<Nth> nextSelector) {
        return CharStream.merge(this, b, nextSelector);
    }

    @Override
    public CharStream zipWith(CharStream b, CharBiFunction<Character> zipFunction) {
        return CharStream.zip(this, b, zipFunction);
    }

    @Override
    public CharStream zipWith(CharStream b, CharStream c, CharTriFunction<Character> zipFunction) {
        return CharStream.zip(this, b, c, zipFunction);
    }

    @Override
    public CharStream zipWith(CharStream b, char valueForNoneA, char valueForNoneB, CharBiFunction<Character> zipFunction) {
        return CharStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public CharStream zipWith(CharStream b, CharStream c, char valueForNoneA, char valueForNoneB, char valueForNoneC, CharTriFunction<Character> zipFunction) {
        return CharStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public CharStream cached() {
        return this.newStream(toArray(), sorted);
    }
}
