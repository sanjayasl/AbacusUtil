/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.landawn.abacus.util.stream;

import java.io.File;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.DataSet;
import com.landawn.abacus.DirtyMarker;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.CompletableFuture;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LineIterator;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.Matrix;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Nullable;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Optional;
import com.landawn.abacus.util.OptionalDouble;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RowIterator;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteNFunction;
import com.landawn.abacus.util.function.ByteTriFunction;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharNFunction;
import com.landawn.abacus.util.function.CharTriFunction;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleNFunction;
import com.landawn.abacus.util.function.DoubleTriFunction;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatNFunction;
import com.landawn.abacus.util.function.FloatTriFunction;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntNFunction;
import com.landawn.abacus.util.function.IntTriFunction;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongNFunction;
import com.landawn.abacus.util.function.LongTriFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortNFunction;
import com.landawn.abacus.util.function.ShortTriFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.UnaryOperator;
import com.landawn.abacus.util.stream.ObjIteratorEx.QueuedIterator;

/**
 * Note: It's copied from OpenJDK at: http://hg.openjdk.java.net/jdk8u/hs-dev/jdk,
 * And including methods copied from StreamEx: https://github.com/amaembo/streamex under Apache License, version 2.0.
 * <br />
 * 
 * <pre>
 * It's designed to service your purposes: Do what you want and returns what you expected. Try the best to adopt the the design principles of Java 8 Stream APIs, but won't has to obey every rule.
 * </pre>
 * 
 * A sequence of elements supporting sequential and parallel aggregate
 * operations.  The following example illustrates an aggregate operation using
 * {@link Stream} and {@link IntStream}:
 *
 * <pre>{@code
 *     int sum = widgets.stream()
 *                      .filter(w -> w.getColor() == RED)
 *                      .mapToInt(w -> w.getWeight())
 *                      .sum();
 * }</pre>
 *
 * In this example, {@code widgets} is a {@code Collection<Widget>}.  We create
 * a stream of {@code Widget} objects via {@link Collection#stream Collection.stream()},
 * filter it to produce a stream containing only the red widgets, and then
 * transform it into a stream of {@code int} values representing the weight of
 * each red widget. Then this stream is summed to produce a total weight.
 *
 * <p>In addition to {@code Stream}, which is a stream of object references,
 * there are primitive specializations for {@link IntStream}, {@link LongStream},
 * and {@link DoubleStream}, all of which are referred to as "streams" and
 * conform to the characteristics and restrictions described here.
 *
 * <p>To perform a computation, stream
 * <a href="package-summary.html#StreamOps">operations</a> are composed into a
 * <em>stream pipeline</em>.  A stream pipeline consists of a source (which
 * might be an array, a collection, a generator function, an I/O channel,
 * etc), zero or more <em>intermediate operations</em> (which transform a
 * stream into another stream, such as {@link Stream#filter(Predicate)}), and a
 * <em>terminal operation</em> (which produces a result or side-effect, such
 * as {@link Stream#count()} or {@link Stream#forEach(Consumer)}).
 * Streams are lazy; computation on the source data is only performed when the
 * terminal operation is initiated, and source elements are consumed only
 * as needed.
 *
 * <p>Collections and streams, while bearing some superficial similarities,
 * have different goals.  Collections are primarily concerned with the efficient
 * management of, and access to, their elements.  By contrast, streams do not
 * provide a means to directly access or manipulate their elements, and are
 * instead concerned with declaratively describing their source and the
 * computational operations which will be performed in aggregate on that source.
 * However, if the provided stream operations do not offer the desired
 * functionality, the {@link #iterator()} and {@link #spliterator()} operations
 * can be used to perform a controlled traversal.
 *
 * <p>A stream pipeline, like the "widgets" example above, can be viewed as
 * a <em>query</em> on the stream source.  Unless the source was explicitly
 * designed for concurrent modification (such as a {@link ConcurrentHashMap}),
 * unpredictable or erroneous behavior may result from modifying the stream
 * source while it is being queried.
 *
 * <p>Most stream operations accept parameters that describe user-specified
 * behavior, such as the lambda expression {@code w -> w.getWeight()} passed to
 * {@code mapToInt} in the example above.  To preserve correct behavior,
 * these <em>behavioral parameters</em>:
 * <ul>
 * <li>must be <a href="package-summary.html#NonInterference">non-interfering</a>
 * (they do not modify the stream source); and</li>
 * <li>in most cases must be <a href="package-summary.html#Statelessness">stateless</a>
 * (their result should not depend on any state that might change during execution
 * of the stream pipeline).</li>
 * </ul>
 *
 * <p>Such parameters are always instances of a
 * <a href="../function/package-summary.html">functional interface</a> such
 * as {@link java.util.function.Function}, and are often lambda expressions or
 * method references.  Unless otherwise specified these parameters must be
 * <em>non-null</em>.
 *
 * <p>A stream should be operated on (invoking an intermediate or terminal stream
 * operation) only once.  This rules out, for example, "forked" streams, where
 * the same source feeds two or more pipelines, or multiple traversals of the
 * same stream.  A stream implementation may throw {@link IllegalStateException}
 * if it detects that the stream is being reused. However, since some stream
 * operations may return their receiver rather than a new stream object, it may
 * not be possible to detect reuse in all cases.
 *
 * <p>Streams have a {@link #close()} method and implement {@link AutoCloseable},
 * but nearly all stream instances do not actually need to be closed after use.
 * Generally, only streams whose source is an IO channel (such as those returned
 * by {@link Files#lines(Path, Charset)}) will require closing.  Most streams
 * are backed by collections, arrays, or generating functions, which require no
 * special resource management.  (If a stream does require closing, it can be
 * declared as a resource in a {@code try}-with-resources statement.)
 *
 * <p>Stream pipelines may execute either sequentially or in
 * <a href="package-summary.html#Parallelism">parallel</a>.  This
 * execution mode is a property of the stream.  Streams are created
 * with an initial choice of sequential or parallel execution.  (For example,
 * {@link Collection#stream() Collection.stream()} creates a sequential stream,
 * and {@link Collection#parallelStream() Collection.parallelStream()} creates
 * a parallel one.)  This choice of execution mode may be modified by the
 * {@link #sequential()} or {@link #parallel()} methods, and may be queried with
 * the {@link #isParallel()} method.
 *
 * @param <T> the type of the stream elements
 * @since 1.8
 * @see IntStream
 * @see LongStream
 * @see DoubleStream
 * @see <a href="package-summary.html">java.util.stream</a>
 */
public abstract class Stream<T> extends StreamBase<T, Object[], Predicate<? super T>, Consumer<? super T>, List<T>, Nullable<T>, Indexed<T>, Stream<T>> {

    @SuppressWarnings("rawtypes")
    private static final Stream EMPTY = new ArrayStream(N.EMPTY_OBJECT_ARRAY, true, OBJECT_COMPARATOR, null);

    Stream(final boolean sorted, final Comparator<? super T> cmp, final Collection<Runnable> closeHandlers) {
        super(sorted, cmp, closeHandlers);
    }

    /**
     * 
     * @param seed initial value to check if the value match the condition.
     * @param predicate
     * @return
     */
    public abstract <U> Stream<T> filter(final U seed, final BiPredicate<? super T, ? super U> predicate);

    /**
     * 
     * @param seed initial value to check if the value match the condition.
     * @param predicate
     * @return
     */
    public abstract <U> Stream<T> takeWhile(final U seed, final BiPredicate<? super T, ? super U> predicate);

    /**
     * 
     * @param seed initial value to check if the value match the condition.
     * @param predicate
     * @return
     */
    public abstract <U> Stream<T> dropWhile(final U seed, final BiPredicate<? super T, ? super U> predicate);

    public abstract <U> Stream<T> removeIf(final U seed, final BiPredicate<? super T, ? super U> predicate);

    public abstract <U> Stream<T> removeIf(final U seed, final BiPredicate<? super T, ? super U> predicate, final Consumer<? super T> consumer);

    /**
     * Returns a stream consisting of the remaining elements of this stream
     * after removing and consuming until the specified <code>predicate</code> return false.
     * If there is no more elements then an empty stream will be returned.
     * 
     * @param seed
     * @param predicate
     * @param consumer
     * @return {@link #dropWhile(Object, BiPredicate)}
     */
    public abstract <U> Stream<T> removeWhile(final U seed, final BiPredicate<? super T, ? super U> predicate, final Consumer<? super T> consumer);

    /**
     * Returns a stream consisting of the elements in this stream which are
     * instances of given class.
     * 
     * @param targetType
     * @return
     */
    public <U> Stream<U> select(Class<U> targetType) {
        return (Stream<U>) filter(Fn.instanceOf(targetType));
    }

    /**
     * Returns a stream consisting of the results of applying the given
     * function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param <R> The element type of the new stream
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    public abstract <U, R> Stream<R> map(U seed, BiFunction<? super T, ? super U, ? extends R> mapper);

    //    public abstract <R> Stream<R> biMap(BiFunction<? super T, ? super T, ? extends R> mapper);
    //
    //    /**
    //     * Returns a stream consisting of the results of applying the given function
    //     * to the every two adjacent elements of this stream.
    //     * 
    //     * <pre>
    //     * <code>
    //     * Stream.of("a", "b", "c", "d", "e").biMap((i, j) -> i + "-" + j).println();
    //     * // print out: [a-b, c-d, e-null]
    //     * </code>
    //     * </pre>
    //     * 
    //     * @param mapper
    //     * @param ignoreNotPaired flag to identify if need to ignore the last element when the total length of the stream is odd number. Default value is false
    //     * @return
    //     */
    //    public abstract <R> Stream<R> biMap(BiFunction<? super T, ? super T, ? extends R> mapper, boolean ignoreNotPaired);
    //
    //    public abstract <R> Stream<R> triMap(TriFunction<? super T, ? super T, ? super T, ? extends R> mapper);
    //
    //    /**
    //     * Returns a stream consisting of the results of applying the given function
    //     * to the every three adjacent elements of this stream.
    //     * 
    //     * <pre>
    //     * <code>
    //     * Stream.of("a", "b", "c", "d", "e").triMap((i, j, k) -> i + "-" + j + "-" + k).println();
    //     * // print out: [a-b-c, d-e-null]
    //     * </code>
    //     * </pre>
    //     * 
    //     * @param mapper
    //     * @param ignoreNotPaired  flag to identify if need to ignore the last one or two elements when the total length of the stream is not multiple of 3. Default value is false
    //     * @return
    //     */
    //    public abstract <R> Stream<R> triMap(TriFunction<? super T, ? super T, ? super T, ? extends R> mapper, boolean ignoreNotPaired);

    public abstract <R> Stream<R> slidingMap(BiFunction<? super T, ? super T, R> mapper);

    /**
     * Slide with <code>windowSize = 2</code> and the specified <code>increment</code>, then <code>map</code> by the specified <code>mapper</code>.
     * 
     * @param mapper
     * @param increment
     * @return
     */
    public abstract <R> Stream<R> slidingMap(BiFunction<? super T, ? super T, R> mapper, int increment);

    public abstract <R> Stream<R> slidingMap(BiFunction<? super T, ? super T, R> mapper, int increment, boolean ignoreNotPaired);

    public abstract <R> Stream<R> slidingMap(TriFunction<? super T, ? super T, ? super T, R> mapper);

    /**
     * Slide with <code>windowSize = 3</code> and the specified <code>increment</code>, then <code>map</code> by the specified <code>mapper</code>.
     * 
     * @param mapper
     * @param increment
     * @return
     */
    public abstract <R> Stream<R> slidingMap(TriFunction<? super T, ? super T, ? super T, R> mapper, int increment);

    public abstract <R> Stream<R> slidingMap(TriFunction<? super T, ? super T, ? super T, R> mapper, int increment, boolean ignoreNotPaired);

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     * 
     * <br />
     * 
     * Returns a stream consisting of results of applying the given function to
     * the ranges created from the source elements.
     * 
     * <pre>
     * <code>
     * Stream.of("a", "ab", "ac", "b", "c", "cb").rangeMap((a, b) -> b.startsWith(a), (a, b) -> a + "->" + b).toList(); // a->ac, b->b, c->cb
     * </code>
     * </pre>
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     * 
     * @param <U> the type of the resulting elements
     * @param sameRange a non-interfering, stateless predicate to apply to
     *        the leftmost and next elements which returns true for elements
     *        which belong to the same range.
     * @param mapper a non-interfering, stateless function to apply to the
     *        range borders and produce the resulting element. If value was
     *        not merged to the interval, then mapper will receive the same
     *        value twice, otherwise it will receive the leftmost and the
     *        rightmost values which were merged to the range.
     * @return the new stream
     * @see #collapse(BiPredicate, BinaryOperator)
     */
    public abstract <U> Stream<U> rangeMap(final BiPredicate<? super T, ? super T> sameRange, final BiFunction<? super T, ? super T, ? extends U> mapper);

    public abstract Stream<T> mapFirst(Function<? super T, ? extends T> mapperForFirst);

    public abstract <R> Stream<R> mapFirstOrElse(Function<? super T, ? extends R> mapperForFirst, Function<? super T, ? extends R> mapperForElse);

    public abstract Stream<T> mapLast(Function<? super T, ? extends T> mapperForLast);

    public abstract <R> Stream<R> mapLastOrElse(Function<? super T, ? extends R> mapperForLast, Function<? super T, ? extends R> mapperForElse);

    public abstract CharStream mapToChar(ToCharFunction<? super T> mapper);

    public abstract ByteStream mapToByte(ToByteFunction<? super T> mapper);

    public abstract ShortStream mapToShort(ToShortFunction<? super T> mapper);

    /**
     * Returns an {@code IntStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">
     *     intermediate operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract IntStream mapToInt(ToIntFunction<? super T> mapper);

    /**
     * Returns a {@code LongStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract LongStream mapToLong(ToLongFunction<? super T> mapper);

    public abstract FloatStream mapToFloat(ToFloatFunction<? super T> mapper);

    /**
     * Returns a {@code DoubleStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

    // public abstract <K, V> EntryStream<K, V> mapToEntry();

    public abstract <K, V> EntryStream<K, V> mapToEntry(Function<? super T, ? extends Map.Entry<K, V>> mapper);

    public abstract <K, V> EntryStream<K, V> mapToEntry(Function<? super T, K> keyMapper, Function<? super T, V> valueMapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @apiNote
     * The {@code flatMap()} operation has the effect of applying a one-to-many
     * transformation to the elements of the stream, and then flattening the
     * resulting elements into a new stream.
     *
     * <p><b>Examples.</b>
     *
     * <p>If {@code orders} is a stream of purchase orders, and each purchase
     * order contains a collection of line items, then the following produces a
     * stream containing all the line items in all the orders:
     * <pre>{@code
     *     orders.flatMap(order -> order.getLineItems().stream())...
     * }</pre>
     *
     * <p>If {@code path} is the path to a file, then the following produces a
     * stream of the {@code words} contained in that file:
     * <pre>{@code
     *     Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8);
     *     Stream<String> words = lines.flatMap(line -> Stream.of(line.split(" +")));
     * }</pre>
     * The {@code mapper} function passed to {@code flatMap} splits a line,
     * using a simple regular expression, into an array of words, and then
     * creates a stream of words from that array.
     *
     * @param <R> The element type of the new stream
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     * @return the new stream
     */
    public abstract <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    public abstract <U, R> Stream<R> flatMap(U seed, BiFunction<? super T, ? super U, ? extends Stream<? extends R>> mapper);

    public abstract <R> Stream<R> flattMap(Function<? super T, ? extends Collection<? extends R>> mapper);

    public abstract <U, R> Stream<R> flattMap(U seed, BiFunction<? super T, ? super U, ? extends Collection<? extends R>> mapper);

    public abstract <R> Stream<R> flatMapp(Function<? super T, R[]> mapper);

    public abstract <U, R> Stream<R> flatMapp(U seed, BiFunction<? super T, ? super U, R[]> mapper);

    public abstract CharStream flatMapToChar(Function<? super T, ? extends CharStream> mapper);

    public abstract ByteStream flatMapToByte(Function<? super T, ? extends ByteStream> mapper);

    public abstract ShortStream flatMapToShort(Function<? super T, ? extends ShortStream> mapper);

    /**
     * Returns an {@code IntStream} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element.  Each mapped
     * stream is {@link java.util.stream.Baseclose() closed} after its
     * contents have been placed into this stream.  (If a mapped stream is
     * {@code null} an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     * @return the new stream
     * @see #flatMap(Function)
     */
    public abstract IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper);

    /**
     * Returns an {@code LongStream} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element.  Each mapped
     * stream is {@link java.util.stream.Baseclose() closed} after its
     * contents have been placed into this stream.  (If a mapped stream is
     * {@code null} an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     * @return the new stream
     * @see #flatMap(Function)
     */
    public abstract LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper);

    public abstract FloatStream flatMapToFloat(Function<? super T, ? extends FloatStream> mapper);

    /**
     * Returns an {@code DoubleStream} consisting of the results of replacing
     * each element of this stream with the contents of a mapped stream produced
     * by applying the provided mapping function to each element.  Each mapped
     * stream is {@link java.util.stream.Baseclose() closed} after its
     * contents have placed been into this stream.  (If a mapped stream is
     * {@code null} an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     * @return the new stream
     * @see #flatMap(Function)
     */
    public abstract DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper);

    public abstract <K, V> EntryStream<K, V> flatMapToEntry(Function<? super T, ? extends Stream<? extends Map.Entry<K, V>>> mapper);

    public abstract <K> Stream<Map.Entry<K, List<T>>> groupBy(final Function<? super T, ? extends K> classifier);

    public abstract <K> Stream<Map.Entry<K, List<T>>> groupBy(final Function<? super T, ? extends K> classifier, final Supplier<Map<K, List<T>>> mapFactory);

    /**
     * 
     * @param classifier
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public abstract <K, U> Stream<Map.Entry<K, List<U>>> groupBy(Function<? super T, ? extends K> classifier, Function<? super T, ? extends U> valueMapper);

    /**
     * 
     * @param classifier
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public abstract <K, U> Stream<Map.Entry<K, List<U>>> groupBy(Function<? super T, ? extends K> classifier, Function<? super T, ? extends U> valueMapper,
            Supplier<Map<K, List<U>>> mapFactory);

    public abstract <K, A, D> Stream<Map.Entry<K, D>> groupBy(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream);

    public abstract <K, A, D> Stream<Map.Entry<K, D>> groupBy(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream,
            final Supplier<Map<K, D>> mapFactory);

    public abstract <K, U, A, D> Stream<Map.Entry<K, D>> groupBy(final Function<? super T, ? extends K> classifier,
            final Function<? super T, ? extends U> valueMapper, final Collector<? super U, A, D> downstream);

    public abstract <K, U, A, D> Stream<Map.Entry<K, D>> groupBy(final Function<? super T, ? extends K> classifier,
            final Function<? super T, ? extends U> valueMapper, final Collector<? super U, A, D> downstream, final Supplier<Map<K, D>> mapFactory);

    public abstract <K, U> Stream<Map.Entry<K, U>> groupBy(final Function<? super T, ? extends K> classifier,
            final Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction);

    public abstract <K, U> Stream<Map.Entry<K, U>> groupBy(final Function<? super T, ? extends K> classifier,
            final Function<? super T, ? extends U> valueMapper, final BinaryOperator<U> mergeFunction, final Supplier<Map<K, U>> mapFactory);

    /**
     * 
     * @param predicate
     * @return
     * @see Collectors#partitioningBy(Predicate)
     */
    public abstract Stream<Map.Entry<Boolean, List<T>>> partitionBy(final Predicate<? super T> predicate);

    /**
     * 
     * @param predicate
     * @param downstream
     * @return
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    public abstract <A, D> Stream<Map.Entry<Boolean, D>> partitionBy(final Predicate<? super T> predicate, final Collector<? super T, A, D> downstream);

    public abstract <K> EntryStream<K, List<T>> groupByToEntry(final Function<? super T, ? extends K> classifier);

    public abstract <K> EntryStream<K, List<T>> groupByToEntry(final Function<? super T, ? extends K> classifier, final Supplier<Map<K, List<T>>> mapFactory);

    /**
     * 
     * @param classifier
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public abstract <K, U> EntryStream<K, List<U>> groupByToEntry(Function<? super T, ? extends K> classifier, Function<? super T, ? extends U> valueMapper);

    /**
     * 
     * @param classifier
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public abstract <K, U> EntryStream<K, List<U>> groupByToEntry(Function<? super T, ? extends K> classifier, Function<? super T, ? extends U> valueMapper,
            Supplier<Map<K, List<U>>> mapFactory);

    public abstract <K, A, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream);

    public abstract <K, A, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream,
            final Supplier<Map<K, D>> mapFactory);

    public abstract <K, U, A, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> classifier,
            final Function<? super T, ? extends U> valueMapper, final Collector<? super U, A, D> downstream);

    public abstract <K, U, A, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> classifier,
            final Function<? super T, ? extends U> valueMapper, final Collector<? super U, A, D> downstream, final Supplier<Map<K, D>> mapFactory);

    public abstract <K, U> EntryStream<K, U> groupByToEntry(final Function<? super T, ? extends K> classifier,
            final Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction);

    public abstract <K, U> EntryStream<K, U> groupByToEntry(final Function<? super T, ? extends K> classifier,
            final Function<? super T, ? extends U> valueMapper, final BinaryOperator<U> mergeFunction, final Supplier<Map<K, U>> mapFactory);

    /**
     * 
     * @param predicate
     * @return
     * @see Collectors#partitioningBy(Predicate)
     */
    public abstract EntryStream<Boolean, List<T>> partitionByToEntry(final Predicate<? super T> predicate);

    /**
     * 
     * @param predicate
     * @param downstream
     * @return
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    public abstract <A, D> EntryStream<Boolean, D> partitionByToEntry(final Predicate<? super T> predicate, final Collector<? super T, A, D> downstream);

    /**
     * Returns Stream of Stream with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param size
     * @return
     */
    public abstract Stream<Set<T>> splitToSet(int size);

    /**
     * Split the stream by the specified predicate.
     * 
     * <pre>
     * <code>
     * // split the number sequence by window 5.
     * Stream.of(1, 2, 3, 5, 7, 9, 10, 11, 19).splitToSet(MutableInt.of(5), (e, b) -> e <= b.intValue(), b -> b.addAndGet(5)).forEach(N::println);
     * </code>
     * </pre>
     * 
     * This stream should be sorted by value which is used to verify the border.
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param seed
     * @param predicate
     * @param seedUpdate
     * @return
     */
    public abstract <U> Stream<Set<T>> splitToSet(final U seed, final BiPredicate<? super T, ? super U> predicate, final Consumer<? super U> seedUpdate);

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param collapsible
     * @param mergeFunction
     * @return
     */
    public abstract Stream<T> collapse(final BiPredicate<? super T, ? super T> collapsible, final BiFunction<? super T, ? super T, T> mergeFunction);

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param collapsible
     * @param collector
     * @return
     */
    public abstract <R, A> Stream<R> collapse(final BiPredicate<? super T, ? super T> collapsible, final Collector<? super T, A, R> collector);

    /**
     * Returns a {@code Stream} produced by iterative application of a accumulation function
     * to an initial element {@code seed} and next element of the current stream.
     * Produces a {@code Stream} consisting of {@code seed}, {@code acc(seed, value1)},
     * {@code acc(acc(seed, value1), value2)}, etc.
     *
     * <p>This is an intermediate operation.
     *
     * <p>Example:
     * <pre>
     * accumulator: (a, b) -&gt; a + b
     * stream: [1, 2, 3, 4, 5]
     * result: [1, 3, 6, 10, 15]
     * </pre>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param accumulator  the accumulation function
     * @return the new stream which has the extract same size as this stream.
     */
    public abstract Stream<T> scan(final BiFunction<? super T, ? super T, T> accumulator);

    /**
     * Returns a {@code Stream} produced by iterative application of a accumulation function
     * to an initial element {@code seed} and next element of the current stream.
     * Produces a {@code Stream} consisting of {@code seed}, {@code acc(seed, value1)},
     * {@code acc(acc(seed, value1), value2)}, etc.
     *
     * <p>This is an intermediate operation.
     *
     * <p>Example:
     * <pre>
     * seed:10
     * accumulator: (a, b) -&gt; a + b
     * stream: [1, 2, 3, 4, 5]
     * result: [11, 13, 16, 20, 25]
     * </pre>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param seed the initial value. it's only used once by <code>accumulator</code> to calculate the fist element in the returned stream. 
     * It will be ignored if this stream is empty and won't be the first element of the returned stream.
     * 
     * @param accumulator  the accumulation function
     * @return the new stream which has the extract same size as this stream.
     */
    public abstract <R> Stream<R> scan(final R seed, final BiFunction<? super R, ? super T, R> accumulator);

    /**
     * <code>Stream.of(1).intersperse(9) --> [1]</code>
     * <code>Stream.of(1, 2, 3).intersperse(9) --> [1, 9, 2, 9, 3]</code>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param delimiter
     * @return
     */
    public abstract Stream<T> intersperse(T delimiter);

    /**
     * Distinct by the value mapped from <code>keyExtractor</code>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param keyExtractor don't change value of the input parameter.
     * @return
     */
    public abstract Stream<T> distinctBy(Function<? super T, ?> keyExtractor);

    /**
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @return
     */
    public abstract Stream<T> top(int n);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @param comparator
     * @return
     */
    public abstract Stream<T> top(int n, Comparator<? super T> comparator);

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the provided {@code Comparator}.
     *
     * <p>For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">stateful
     * intermediate operation</a>.
     *
     * @param comparator a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                   <a href="package-summary.html#Statelessness">stateless</a>
     *                   {@code Comparator} to be used to compare stream elements
     * @return the new stream
     */
    public abstract Stream<T> sorted(Comparator<? super T> comparator);

    @SuppressWarnings("rawtypes")
    public abstract Stream<T> sortedBy(Function<? super T, ? extends Comparable> keyExtractor);

    public abstract Stream<T> sortedByInt(ToIntFunction<? super T> keyExtractor);

    public abstract Stream<T> sortedByLong(ToLongFunction<? super T> keyExtractor);

    public abstract Stream<T> sortedByDouble(ToDoubleFunction<? super T> keyExtractor);

    public abstract <E extends Exception> void forEach(Try.Consumer<? super T, E> action) throws E;

    /**
     * Execute <code>accumulator</code> on each element till <code>true</code> is returned by <code>conditionToBreak</code>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param seed
     * @param accumulator
     * @param conditionToBreak break if <code>true</code> is return.
     * @return
     */
    public abstract <R, E extends Exception, E2 extends Exception> R forEach(final R seed, Try.BiFunction<R, ? super T, R, E> accumulator,
            final Try.BiPredicate<? super R, ? super T, E2> conditionToBreak) throws E, E2;

    public abstract <E extends Exception> void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action) throws E;

    /**
     * Slide with <code>windowSize = 2</code> and the specified <code>increment</code>, then <code>consume</code> by the specified <code>mapper</code>.
     * 
     * @param mapper
     * @param increment
     * @return
     */
    public abstract <E extends Exception> void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action, final int increment) throws E;

    public abstract <E extends Exception> void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action) throws E;

    /**
     * Slide with <code>windowSize = 3</code> and the specified <code>increment</code>, then <code>consume</code> by the specified <code>mapper</code>.
     * 
     * @param mapper
     * @param increment
     * @return
     */
    public abstract <E extends Exception> void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action, final int increment) throws E;

    public abstract <E extends Exception> boolean anyMatch(Try.Predicate<? super T, E> predicate) throws E;

    public abstract <E extends Exception> boolean allMatch(Try.Predicate<? super T, E> predicate) throws E;

    public abstract <E extends Exception> boolean noneMatch(Try.Predicate<? super T, E> predicate) throws E;

    public abstract <E extends Exception> Nullable<T> findFirst(Try.Predicate<? super T, E> predicate) throws E;

    public abstract <E extends Exception> Nullable<T> findLast(Try.Predicate<? super T, E> predicate) throws E;

    public abstract <E extends Exception, E2 extends Exception> Nullable<T> findFirstOrLast(Try.Predicate<? super T, E> predicateForFirst,
            Try.Predicate<? super T, E2> predicateForLast) throws E, E2;

    public abstract <E extends Exception> Nullable<T> findAny(Try.Predicate<? super T, E> predicate) throws E;

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param seed
     * @param predicate
     * @return
     */
    public abstract <U, E extends Exception> Nullable<T> findFirst(final U seed, final Try.BiPredicate<? super T, ? super U, E> predicate) throws E;

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param seed
     * @param predicate
     * @return
     */
    public abstract <U, E extends Exception> Nullable<T> findLast(final U seed, final Try.BiPredicate<? super T, ? super U, E> predicate) throws E;

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param seed
     * @param predicate
     * @return
     */
    public abstract <U, E extends Exception> Nullable<T> findAny(final U seed, final Try.BiPredicate<? super T, ? super U, E> predicate) throws E;

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param seed
     * @param predicateForFirst
     * @param predicateForLast
     * @return
     */
    public abstract <U, E extends Exception, E2 extends Exception> Nullable<T> findFirstOrLast(final U seed,
            final Try.BiPredicate<? super T, ? super U, E> predicateForFirst, final Try.BiPredicate<? super T, ? super U, E2> predicateForLast) throws E, E2;

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param preFunc
     * @param predicateForFirst
     * @param predicateForLast
     * @return
     */
    public abstract <U, E extends Exception, E2 extends Exception> Nullable<T> findFirstOrLast(final Function<? super T, U> preFunc,
            final Try.BiPredicate<? super T, ? super U, E> predicateForFirst, final Try.BiPredicate<? super T, ? super U, E2> predicateForLast) throws E, E2;

    public abstract <U, E extends Exception> boolean anyMatch(final U seed, final Try.BiPredicate<? super T, ? super U, E> predicate) throws E;

    public abstract <U, E extends Exception> boolean allMatch(final U seed, final Try.BiPredicate<? super T, ? super U, E> predicate) throws E;

    public abstract <U, E extends Exception> boolean noneMatch(final U seed, final Try.BiPredicate<? super T, ? super U, E> predicate) throws E;

    public abstract boolean containsAll(T... a);

    public abstract boolean containsAll(Collection<? extends T> c);

    /**
     * Returns an array containing the elements of this stream, using the
     * provided {@code generator} function to allocate the returned array, as
     * well as any additional arrays that might be required for a partitioned
     * execution or for resizing.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote
     * The generator function takes an integer, which is the size of the
     * desired array, and produces an array of the desired size.  This can be
     * concisely expressed with an array constructor reference:
     * <pre>{@code
     *     Person[] men = people.stream()
     *                          .filter(p -> p.getGender() == MALE)
     *                          .toArray(Person[]::new);
     * }</pre>
     *
     * @param <A> the element type of the resulting array
     * @param generator a function which produces a new array of the desired
     *                  type and the provided length
     * @return an array containing the elements in this stream
     * @throws ArrayStoreException if the runtime type of the array returned
     * from the array generator is not a supertype of the runtime type of every
     * element in this stream
     */
    public abstract <A> A[] toArray(IntFunction<A[]> generator);

    public ImmutableList<T> toImmutableList() {
        return ImmutableList.of(toList());
    }

    public ImmutableSet<T> toImmutableSet() {
        return ImmutableSet.of(toSet());
    }

    /**
     * 
     * @param keyExtractor
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    public <K, U> ImmutableMap<K, U> toImmutableMap(Function<? super T, ? extends K> keyExtractor, Function<? super T, ? extends U> valueMapper) {
        return ImmutableMap.of(toMap(keyExtractor, valueMapper));
    }

    /**
     * 
     * @param keyExtractor
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    public <K, U> ImmutableMap<K, U> toImmutableMap(Function<? super T, ? extends K> keyExtractor, Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction) {
        return ImmutableMap.of(toMap(keyExtractor, valueMapper, mergeFunction));
    }

    /**
     * 
     * @param keyExtractor
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    public abstract <K, U> Map<K, U> toMap(Function<? super T, ? extends K> keyExtractor, Function<? super T, ? extends U> valueMapper);

    /**
     * 
     * @param keyExtractor
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    public abstract <K, U> Map<K, U> toMap(Function<? super T, ? extends K> keyExtractor, Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction);

    /**
     * 
     * @param keyExtractor
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, Supplier)
     */
    public abstract <K, U, M extends Map<K, U>> M toMap(Function<? super T, ? extends K> keyExtractor, Function<? super T, ? extends U> valueMapper,
            Supplier<M> mapFactory);

    /**
     * 
     * @param keyExtractor
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    public abstract <K, U, M extends Map<K, U>> M toMap(Function<? super T, ? extends K> keyExtractor, Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction, Supplier<M> mapFactory);

    /**
     * 
     * @param classifier
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    public abstract <K, A, D> Map<K, D> toMap(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream);

    /**
     * 
     * @param classifier
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    public abstract <K, A, D, M extends Map<K, D>> M toMap(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream,
            final Supplier<M> mapFactory);

    /**
     * 
     * @param classifier
     * @param valueMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    public abstract <K, U, A, D> Map<K, D> toMap(final Function<? super T, ? extends K> classifier, final Function<? super T, ? extends U> valueMapper,
            final Collector<? super U, A, D> downstream);

    /**
     * 
     * @param classifier
     * @param valueMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    public abstract <K, U, A, D, M extends Map<K, D>> M toMap(final Function<? super T, ? extends K> classifier,
            final Function<? super T, ? extends U> valueMapper, final Collector<? super U, A, D> downstream, final Supplier<M> mapFactory);

    /**
     * 
     * @param classifier
     * @return
     * @see Collectors#groupingBy(Function)
     */
    public abstract <K> Map<K, List<T>> groupTo(Function<? super T, ? extends K> classifier);

    /**
     * 
     * @param classifier
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Supplier)
     */
    public abstract <K, M extends Map<K, List<T>>> M groupTo(final Function<? super T, ? extends K> classifier, final Supplier<M> mapFactory);

    public abstract <K, U> Map<K, List<U>> groupTo(Function<? super T, ? extends K> keyExtractor, Function<? super T, ? extends U> valueMapper);

    /**
     * 
     * @param keyExtractor
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public abstract <K, U, M extends Map<K, List<U>>> M groupTo(Function<? super T, ? extends K> keyExtractor, Function<? super T, ? extends U> valueMapper,
            Supplier<M> mapFactory);

    /**
     * 
     * @param predicate
     * @return
     * @see Collectors#partitioningBy(Predicate)
     */
    public abstract Map<Boolean, List<T>> partitionTo(final Predicate<? super T> predicate);

    /**
     * 
     * @param predicate
     * @param downstream
     * @return
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    public abstract <A, D> Map<Boolean, D> partitionTo(final Predicate<? super T> predicate, final Collector<? super T, A, D> downstream);

    /**
     * 
     * @param keyExtractor
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public abstract <K> ListMultimap<K, T> toMultimap(Function<? super T, ? extends K> keyExtractor);

    /**
     * 
     * @param keyExtractor
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public abstract <K, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(Function<? super T, ? extends K> keyExtractor,
            Supplier<M> mapFactory);

    /**
     * 
     * @param keyExtractor
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public abstract <K, U> ListMultimap<K, U> toMultimap(Function<? super T, ? extends K> keyExtractor, Function<? super T, ? extends U> valueMapper);

    /**
     * 
     * @param keyExtractor
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public abstract <K, U, V extends Collection<U>, M extends Multimap<K, U, V>> M toMultimap(Function<? super T, ? extends K> keyExtractor,
            Function<? super T, ? extends U> valueMapper, Supplier<M> mapFactory);

    public abstract Matrix<T> toMatrix(Class<T> type);

    /**
     * 
     * @return
     */
    public abstract DataSet toDataSet();

    /**
     * @param isFirstHeader
     * @return
     */
    public abstract DataSet toDataSet(boolean isFirstHeader);

    /**
     * 
     * @param columnNames it can be null or empty if this is Map or entity stream.
     * @return
     */
    public abstract DataSet toDataSet(final List<String> columnNames);

    /**
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using the provided identity value and an
     * <a href="package-summary.html#Associativity">associative</a>
     * accumulation function, and returns the reduced value.  This is equivalent
     * to:
     * <pre>{@code
     *     T result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code identity} value must be an identity for the accumulator
     * function. This means that for all {@code t},
     * {@code accumulator.apply(identity, t)} is equal to {@code t}.
     * The {@code accumulator} function must be an
     * <a href="package-summary.html#Associativity">associative</a> function.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote Sum, min, max, average, and string concatenation are all special
     * cases of reduction. Summing a stream of numbers can be expressed as:
     *
     * <pre>{@code
     *     Integer sum = integers.reduce(0, (a, b) -> a+b);
     * }</pre>
     *
     * or:
     *
     * <pre>{@code
     *     Integer sum = integers.reduce(0, Integer::sum);
     * }</pre>
     *
     * <p>While this may seem a more roundabout way to perform an aggregation
     * compared to simply mutating a running total in a loop, reduction
     * operations parallelize more gracefully, without needing additional
     * synchronization and with greatly reduced risk of data races.
     *
     * @param identity the identity value for the accumulating function
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values
     * @return the result of the reduction
     */
    public abstract T reduce(T identity, BinaryOperator<T> accumulator);

    /**
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using an
     * <a href="package-summary.html#Associativity">associative</a> accumulation
     * function, and returns an {@code Optional} describing the reduced value,
     * if any. This is equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     T result = null;
     *     for (T element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.apply(result, element);
     *     }
     *     return foundAny ? Nullable.of(result) : Nullable.empty();
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code accumulator} function must be an
     * <a href="package-summary.html#Associativity">associative</a> function.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values
     * @return an {@link Optional} describing the result of the reduction
     * @see #reduce(Object, BinaryOperator)
     * @see #min(Comparator)
     * @see #max(Comparator)
     */
    public abstract Nullable<T> reduce(BinaryOperator<T> accumulator);

    /**
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using the provided identity, accumulation and
     * combining functions.  This is equivalent to:
     * <pre>{@code
     *     U result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code identity} value must be an identity for the zipFunction
     * function.  This means that for all {@code u}, {@code zipFunction(identity, u)}
     * is equal to {@code u}.  Additionally, the {@code zipFunction} function
     * must be compatible with the {@code accumulator} function; for all
     * {@code u} and {@code t}, the following must hold:
     * <pre>{@code
     *     zipFunction.apply(u, accumulator.apply(identity, t)) == accumulator.apply(u, t)
     * }</pre>
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote Many reductions using this form can be represented more simply
     * by an explicit combination of {@code map} and {@code reduce} operations.
     * The {@code accumulator} function acts as a fused mapper and accumulator,
     * which can sometimes be more efficient than separate mapping and reduction,
     * such as when knowing the previously reduced value allows you to avoid
     * some computation.
     *
     * @param <U> The type of the result
     * @param identity the identity value for the zipFunction function
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for incorporating an additional element into a result
     * @param combiner an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values, which must be
     *                    compatible with the accumulator function
     * @return the result of the reduction
     * @see #reduce(BinaryOperator)
     * @see #reduce(Object, BinaryOperator)
     */
    public abstract <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);

    /**
     * The result will be merged by: <code>a.addAll(b)</code> if result container is <code>Collection/Multiset/LongMultiset/IntList/CharList/...</code>, 
     * or <code>a.putAll(b)</code> if result container is <code>Map/Multimap/Sheet</code>, 
     * or <code>a.append(b)</code> if result container is <code>StringBuilder</code>, 
     * or <code>N.concat(a, b)</code> if result container is array: <code>boolean[]/char[]/int[]/.../Object[]</code> when it's necessary in Parallel Stream.
     * 
     * @param identity
     * @param accumulator
     * @return
     * @throws RuntimeException if the result container can't be merged by default when it's necessary in Parallel Stream.
     */
    public abstract <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator);

    /**
     * Performs a <a href="package-summary.html#MutableReduction">mutable
     * reduction</a> operation on the elements of this stream.  A mutable
     * reduction is one in which the reduced value is a mutable result container,
     * such as an {@code ArrayList}, and elements are incorporated by updating
     * the state of the result rather than by replacing the result.  This
     * produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (T element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     *
     * <p>Like {@link #reduce(Object, BinaryOperator)}, {@code collect} operations
     * can be parallelized without requiring additional synchronization.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote There are many existing classes in the JDK whose signatures are
     * well-suited for use with method references as arguments to {@code collect()}.
     * For example, the following will accumulate strings into an {@code ArrayList}:
     * <pre>{@code
     *     List<String> asList = stringStream.collect(ArrayList::new, ArrayList::add,
     *                                                ArrayList::addAll);
     * }</pre>
     *
     * <p>The following will take a stream of strings and concatenates them into a
     * single string:
     * <pre>{@code
     *     String concat = stringStream.collect(StringBuilder::new, StringBuilder::append,
     *                                          StringBuilder::append)
     *                                 .toString();
     * }</pre>
     *
     * @param <R> type of the result
     * @param supplier a function that creates a new result container. For a
     *                 parallel execution, this function may be called
     *                 multiple times and must return a fresh value each time.
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for incorporating an additional element into a result
     * @param combiner an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values, which must be
     *                    compatible with the accumulator function
     * @return the result of the reduction
     */
    public abstract <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);

    /**
     * The result will be merged by: <code>a.addAll(b)</code> if result container is <code>Collection/Multiset/LongMultiset/IntList/CharList/...</code>, 
     * or <code>a.putAll(b)</code> if result container is <code>Map/Multimap/Sheet</code>, 
     * or <code>a.append(b)</code> if result container is <code>StringBuilder</code> when it's necessary in Parallel Stream.
     * 
     * @param supplier
     * @param accumulator
     * @return
     * @throws RuntimeException if the result container can't be merged by default when it's necessary in Parallel Stream.
     */
    public abstract <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator);

    /**
     * Performs a <a href="package-summary.html#MutableReduction">mutable
     * reduction</a> operation on the elements of this stream using a
     * {@code Collector}.  A {@code Collector}
     * encapsulates the functions used as arguments to
     * {@link #collect(Supplier, BiConsumer, BiConsumer)}, allowing for reuse of
     * collection strategies and composition of collect operations such as
     * multiple-level grouping or partitioning.
     *
     * <p>If the stream is parallel, and the {@code Collector}
     * is {@link Collector.Characteristics#CONCURRENT concurrent}, and
     * either the stream is unordered or the collector is
     * {@link Collector.Characteristics#UNORDERED unordered},
     * then a concurrent reduction will be performed (see {@link Collector} for
     * details on concurrent reduction.)
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * <p>When executed in parallel, multiple intermediate results may be
     * instantiated, populated, and merged so as to maintain isolation of
     * mutable data structures.  Therefore, even when executed in parallel
     * with non-thread-safe data structures (such as {@code ArrayList}), no
     * additional synchronization is needed for a parallel reduction.
     *
     * @apiNote
     * The following will accumulate strings into an ArrayList:
     * <pre>{@code
     *     List<String> asList = stringStream.collect(Collectors.toList());
     * }</pre>
     *
     * <p>The following will classify {@code Person} objects by city:
     * <pre>{@code
     *     Map<String, List<Person>> peopleByCity
     *         = personStream.collect(Collectors.groupingBy(Person::getCity));
     * }</pre>
     *
     * <p>The following will classify {@code Person} objects by state and city,
     * cascading two {@code Collector}s together:
     * <pre>{@code
     *     Map<String, Map<String, List<Person>>> peopleByStateAndCity
     *         = personStream.collect(Collectors.groupingBy(Person::getState,
     *                                                      Collectors.groupingBy(Person::getCity)));
     * }</pre>
     *
     * @param <R> the type of the result
     * @param <A> the intermediate accumulation type of the {@code Collector}
     * @param collector the {@code Collector} describing the reduction
     * @return the result of the reduction
     * @see #collect(Supplier, BiConsumer, BiConsumer)
     * @see Collectors
     */
    public abstract <R, A> R collect(Collector<? super T, A, R> collector);

    public abstract <R, A> R collect(java.util.stream.Collector<? super T, A, R> collector);

    public abstract <R, A, RR> RR collectAndThen(Collector<? super T, A, R> downstream, Function<R, RR> finisher);

    public abstract <R, A, RR> RR collectAndThen(java.util.stream.Collector<? super T, A, R> downstream, Function<R, RR> finisher);

    public abstract <R> R toListAndThen(Function<? super List<T>, R> func);

    public abstract <R> R toSetAndThen(Function<? super Set<T>, R> func);

    /**
     * Head and tail should be used by pair. If only one is called, should use first() or skip(1) instead.
     * Don't call any other methods with this stream after head() or tail() is called. 
     * 
     * @return
     */
    public abstract Nullable<T> head();

    /**
     * Head and tail should be used by pair. If only one is called, should use first() or skip(1) instead.
     * Don't call any other methods with this stream after head() or tail() is called. 
     * 
     * @return
     */
    public abstract Stream<T> tail();

    /**
     * Head2 and tail2 should be used by pair. 
     * Don't call any other methods with this stream after headd() or taill() is called.
     * 
     * <br />
     * All elements will be loaded to memory.
     * 
     * @return
     */
    public abstract Stream<T> headd();

    /**
     * Head2 and tail2 should be used by pair. 
     * Don't call any other methods with this stream after headd() or taill() is called. 
     * 
     * <br />
     * All elements will be loaded to memory.
     * 
     * @return
     */
    public abstract Nullable<T> taill();

    public abstract Pair<Nullable<T>, Stream<T>> headAndTail();

    /**
     * 
     * <br />
     * All elements will be loaded to memory.
     * 
     * @return
     */
    public abstract Pair<Stream<T>, Nullable<T>> headAndTaill();

    /**
     * A queue with size up to <code>n</code> will be maintained to filter out the last <code>n</code> elements. 
     * It may cause <code>out of memory error</code> if <code>n</code> is big enough.
     *
     * @param n
     * @return
     */
    public abstract Stream<T> last(int n);

    /**
     * A queue with size up to <code>n</code> will be maintained to filter out the last <code>n</code> elements. 
     * It may cause <code>out of memory error</code> if <code>n</code> is big enough.
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @return
     */
    public abstract Stream<T> skipLast(int n);

    /**
     * Returns the minimum element of this stream according to the provided
     * {@code Comparator}.  This is a special case of a
     * <a href="package-summary.html#Reduction">reduction</a>.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal operation</a>.
     *
     * @param comparator a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                   <a href="package-summary.html#Statelessness">stateless</a>
     *                   {@code Comparator} to compare elements of this stream
     * @return an {@code Optional} describing the minimum element of this stream,
     * or an empty {@code Optional} if the stream is empty
     */
    public abstract Nullable<T> min(Comparator<? super T> comparator);

    @SuppressWarnings("rawtypes")
    public Nullable<T> minBy(final Function<? super T, ? extends Comparable> keyExtractor) {
        final Comparator<? super T> comparator = Fn.comparingBy(keyExtractor);

        return min(comparator);
    }

    /**
     * Returns the maximum element of this stream according to the provided
     * {@code Comparator}.  This is a special case of a
     * <a href="package-summary.html#Reduction">reduction</a>.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param comparator a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                   <a href="package-summary.html#Statelessness">stateless</a>
     *                   {@code Comparator} to compare elements of this stream
     * @return an {@code Optional} describing the maximum element of this stream,
     * or an empty {@code Optional} if the stream is empty
     */
    public abstract Nullable<T> max(Comparator<? super T> comparator);

    @SuppressWarnings("rawtypes")
    public Nullable<T> maxBy(final Function<? super T, ? extends Comparable> keyExtractor) {
        final Comparator<? super T> comparator = Fn.comparingBy(keyExtractor);

        return max(comparator);
    }

    /**
     * 
     * @param k
     * @param comparator
     * @return Nullable.empty() if there is no element or count less than k, otherwise the kth largest element.
     */
    public abstract Nullable<T> kthLargest(int k, Comparator<? super T> comparator);

    public abstract int sumInt(ToIntFunction<? super T> mapper);

    public abstract long sumLong(ToLongFunction<? super T> mapper);

    public abstract double sumDouble(ToDoubleFunction<? super T> mapper);

    public abstract OptionalDouble averageInt(ToIntFunction<? super T> mapper);

    public abstract OptionalDouble averageLong(ToLongFunction<? super T> mapper);

    public abstract OptionalDouble averageDouble(ToDoubleFunction<? super T> mapper);

    public abstract Optional<Map<Percentage, T>> percentiles(Comparator<? super T> comparator);

    public abstract Stream<List<T>> combinations();

    public abstract Stream<List<T>> combinations(int len);

    public abstract Stream<List<T>> permutations();

    public abstract Stream<List<T>> orderedPermutations();

    public abstract Stream<List<T>> orderedPermutations(Comparator<? super T> comparator);

    @SafeVarargs
    public final Stream<List<T>> cartesianProduct(Collection<? extends T>... cs) {
        return cartesianProduct(Arrays.asList(cs));
    }

    public abstract Stream<List<T>> cartesianProduct(Collection<? extends Collection<? extends T>> cs);

    /**
     * 
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    public abstract <U> Stream<Pair<T, U>> innerJoin(Collection<U> b, Function<? super T, ?> leftKeyMapper, Function<? super U, ?> rightKeyMapper);

    /**
     * 
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param predicate
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    public abstract <U> Stream<Pair<T, U>> innerJoin(Collection<U> b, BiPredicate<? super T, ? super U> predicate);

    /**
     * 
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    public abstract <U> Stream<Pair<T, U>> fullJoin(Collection<U> b, Function<? super T, ?> leftKeyMapper, Function<? super U, ?> rightKeyMapper);

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param predicate
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    public abstract <U> Stream<Pair<T, U>> fullJoin(Collection<U> b, BiPredicate<? super T, ? super U> predicate);

    /**
     * 
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    public abstract <U> Stream<Pair<T, U>> leftJoin(Collection<U> b, Function<? super T, ?> leftKeyMapper, Function<? super U, ?> rightKeyMapper);

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param predicate
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    public abstract <U> Stream<Pair<T, U>> leftJoin(Collection<U> b, BiPredicate<? super T, ? super U> predicate);

    /**
     * 
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    public abstract <U> Stream<Pair<T, U>> rightJoin(Collection<U> b, Function<? super T, ?> leftKeyMapper, Function<? super U, ?> rightKeyMapper);

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param predicate
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    public abstract <U> Stream<Pair<T, U>> rightJoin(Collection<U> b, BiPredicate<? super T, ? super U> predicate);

    public abstract boolean hasDuplicates();

    public abstract Stream<T> skipNull();

    /**
     * Intersect with the specified Collection by the values mapped by <code>mapper</code>.
     * 
     * @param mapper
     * @param c
     * @return
     * @see IntList#intersection(IntList)
     */
    public abstract Stream<T> intersection(Function<? super T, ?> mapper, Collection<?> c);

    /**
     * Except with the specified Collection by the values mapped by <code>mapper</code>.
     * 
     * @param mapper
     * @param c
     * @return
     * @see IntList#difference(IntList)
     */
    public abstract Stream<T> difference(Function<? super T, ?> mapper, Collection<?> c);

    @SafeVarargs
    public final Stream<T> append(T... a) {
        if (N.isNullOrEmpty(a)) {
            return this;
        }

        return append(Arrays.asList(a));
    }

    public abstract Stream<T> append(Collection<? extends T> c);

    @SafeVarargs
    public final Stream<T> prepend(T... a) {
        if (N.isNullOrEmpty(a)) {
            return this;
        }

        return prepend(Arrays.asList(a));
    }

    public abstract Stream<T> prepend(Collection<? extends T> c);

    /**
     * Returns a reusable stream which can be repeatedly used.
     * 
     * <br />
     * All elements will be loaded to memory.
     * 
     * @param generator
     * @return
     */
    public abstract Stream<T> cached(IntFunction<T[]> generator);

    public abstract Stream<T> queued();

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterator asynchronously.
     * 
     * @param queueSize Default value is 8
     * @return
     */
    public abstract Stream<T> queued(int queueSize);

    /**
     * 
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public abstract Stream<T> merge(final Stream<? extends T> b, final BiFunction<? super T, ? super T, Nth> nextSelector);

    public abstract <T2, R> Stream<R> zipWith(final Stream<T2> b, final BiFunction<? super T, ? super T2, R> zipFunction);

    public abstract <T2, T3, R> Stream<R> zipWith(final Stream<T2> b, final Stream<T3> c, final TriFunction<? super T, ? super T2, ? super T3, R> zipFunction);

    public abstract <T2, R> Stream<R> zipWith(final Stream<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final BiFunction<? super T, ? super T2, R> zipFunction);

    public abstract <T2, T3, R> Stream<R> zipWith(final Stream<T2> b, final Stream<T3> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final TriFunction<? super T, ? super T2, ? super T3, R> zipFunction);

    public abstract long persist(File file, Function<? super T, String> toLine);

    public abstract long persist(OutputStream os, Function<? super T, String> toLine);

    public abstract long persist(Writer writer, Function<? super T, String> toLine);

    public abstract long persist(final Connection conn, final String insertSQL, final int batchSize, final int batchInterval,
            final Try.BiConsumer<? super PreparedStatement, ? super T, SQLException> stmtSetter);

    public abstract long persist(final PreparedStatement stmt, final int batchSize, final int batchInterval,
            final Try.BiConsumer<? super PreparedStatement, ? super T, SQLException> stmtSetter);

    @Override
    public ObjIterator<T> iterator() {
        return iteratorEx();
    }

    abstract ObjIteratorEx<T> iteratorEx();

    @Override
    public <R> R __(Function<? super Stream<T>, R> transfer) {
        return transfer.apply(this);
    }

    /**
     * To reduce the memory footprint, Only one instance of <code>Map.Entry</code> is created, 
     * and the same entry instance is returned and set with different keys/values during iteration of the returned stream.
     * The elements only can be retrieved one by one, can't be modified or saved.
     * The returned Stream doesn't support the operations which require two or more elements at the same time: (e.g. sort/distinct/pairMap/slidingMap/sliding/split/toList/toSet/...).
     * , and can't be parallel stream.
     * Operations: filter/map/toMap/groupBy/groupTo/... are supported.
     * 
     * <br />
     * <code>ER</code> = <code>Entry Reusable</code>
     * 
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     */
    @Beta
    public abstract <K, V> EntryStream<K, V> mapToEntryER(Function<? super T, K> keyMapper, Function<? super T, V> valueMapper);

    public static <T> Stream<T> empty() {
        return EMPTY;
    }

    public static <T> Stream<T> just(final T a) {
        return of(N.asArray(a));
    }

    @SafeVarargs
    public static <T> Stream<T> of(final T... a) {
        return N.isNullOrEmpty(a) ? (Stream<T>) empty() : of(a, 0, a.length);
    }

    /**
     * Returns a sequential, stateful and immutable <code>Stream</code>.
     *
     * @param a
     * @param startIndex
     * @param endIndex
     * @return
     */
    public static <T> Stream<T> of(final T[] a, final int startIndex, final int endIndex) {
        return N.isNullOrEmpty(a) && (startIndex == 0 && endIndex == 0) ? (Stream<T>) empty() : new ArrayStream<>(a, startIndex, endIndex);
    }

    /**
     * Returns a sequential, stateful and immutable <code>Stream</code>.
     *
     * @param c
     * @return
     */
    public static <T> Stream<T> of(final Collection<? extends T> c) {
        return N.isNullOrEmpty(c) ? (Stream<T>) empty() : of(c, 0, c.size());
    }

    /**
     * Returns a sequential, stateful and immutable <code>Stream</code>.
     * 
     * @param c
     * @param startIndex
     * @param endIndex
     * @return
     */
    public static <T> Stream<T> of(final Collection<? extends T> c, int startIndex, int endIndex) {
        if (N.isNullOrEmpty(c) && (startIndex == 0 && endIndex == 0)) {
            return empty();
        }

        if (startIndex < 0 || endIndex < startIndex || endIndex > c.size()) {
            throw new IllegalArgumentException("startIndex(" + startIndex + ") or endIndex(" + endIndex + ") is invalid");
        }

        // return new CollectionStream<T>(c);
        // return new ArrayStream<T>((T[]) c.toArray()); // faster

        if (isListElementDataFieldGettable && listElementDataField != null && c instanceof ArrayList) {
            T[] array = null;

            try {
                array = (T[]) listElementDataField.get(c);
            } catch (Throwable e) {
                // ignore;
                isListElementDataFieldGettable = false;
            }

            if (array != null) {
                return of(array, startIndex, endIndex);
            }
        }

        if (startIndex == 0 && endIndex == c.size()) {
            // return (c.size() > 10 && (c.size() < 1000 || (c.size() < 100000 && c instanceof ArrayList))) ? streamOf((T[]) c.toArray()) : c.stream();
            return of(ObjIteratorEx.of(c));
        } else {
            return of(ObjIteratorEx.of(c), startIndex, endIndex);
        }
    }

    public static <K, V> Stream<Map.Entry<K, V>> of(final Map<K, V> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return of(map.entrySet());
    }

    /**
     * Returns a sequential, stateful and immutable <code>Stream</code>.
     *
     * @param iterator
     * @return
     */
    public static <T> Stream<T> of(final Iterator<? extends T> iterator) {
        if (iterator == null) {
            return empty();
        }

        if (iterator instanceof RowIterator) {
            return (Stream<T>) of((RowIterator) iterator);
        } else {
            return new IteratorStream<>(iterator);
        }
    }

    /**
     * Returns a sequential, stateful and immutable <code>Stream</code>.
     * 
     * @param c
     * @param startIndex
     * @param endIndex
     * @return
     */
    static <T> Stream<T> of(final Iterator<? extends T> iterator, int startIndex, int endIndex) {
        if (iterator == null && (startIndex == 0 && endIndex == 0)) {
            return empty();
        }

        if (startIndex < 0 || endIndex < startIndex) {
            throw new IllegalArgumentException("startIndex(" + startIndex + ") or endIndex(" + endIndex + ") is invalid");
        }

        final Stream<T> stream = of(iterator);
        return stream.skip(startIndex).limit(endIndex - startIndex);
    }

    public static <T> Stream<T> of(final java.util.stream.Stream<T> stream) {
        return of(new ObjIteratorEx<T>() {
            private Iterator<T> iter = null;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.next();
            }

            @Override
            public long count() {
                return iter == null ? stream.count() : super.count();
            }

            @Override
            public void skip(long n) {
                if (iter == null) {
                    iter = stream.skip(n).iterator();
                } else {
                    super.skip(n);
                }
            }

            @Override
            public Object[] toArray() {
                return iter == null ? stream.toArray() : super.toArray();
            }

            @Override
            public <A> A[] toArray(final A[] a) {
                return iter == null ? stream.toArray(new IntFunction<A[]>() {
                    @Override
                    public A[] apply(int value) {
                        return a;
                    }
                }) : super.toArray(a);
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                stream.close();
            }
        });
    }

    /**
     * It's user's responsibility to close the input <code>reader</code> after the stream is finished.
     * 
     * @param reader
     * @return
     */
    public static Stream<String> of(final Reader reader) {
        N.requireNonNull(reader);

        return of(new LineIterator(reader));
    }

    static Stream<String> of(final Reader reader, int startIndex, int endIndex) {
        N.requireNonNull(reader);

        return of(new LineIterator(reader), startIndex, endIndex);
    }

    /**
     * It's user's responsibility to close the input <code>rowIterator</code> after the stream is finished.
     * 
     * @param rowIterator
     * @return
     */
    public static Stream<Object[]> of(final RowIterator rowIterator) {
        N.requireNonNull(rowIterator);

        return new IteratorStream<>(new ObjIteratorEx<Object[]>() {
            @Override
            public boolean hasNext() {
                return rowIterator.hasNext();
            }

            @Override
            public Object[] next() {
                return rowIterator.next();
            }

            @Override
            public void skip(long n) {
                rowIterator.skip(n);
            }
        });
    }

    public static Try<Stream<Object[]>> of(final RowIterator rowIterator, final boolean closeRowIterator) {
        return closeRowIterator ? of(rowIterator).onClose(newCloseHandle(rowIterator)).tried() : of(rowIterator).tried();
    }

    /**
     * It's user's responsibility to close the input <code>rowIterator</code> after the stream is finished.
     * 
     * @param targetClass
     * @param rowIterator
     * @return
     */
    public static <T> Stream<T> of(final Class<T> targetClass, final RowIterator rowIterator) {
        N.requireNonNull(targetClass);
        N.requireNonNull(rowIterator);

        final Type<?> type = N.typeOf(targetClass);

        N.checkArgument(type.isMap() || type.isEntity(), "target class must be Map or entity with getter/setter methods");

        final int columnCount = rowIterator.getColumnCount();
        final String[] columnLabels = rowIterator.getColumnLabelList().toArray(new String[columnCount]);

        final boolean isMap = type.isMap();
        final boolean isDirtyMarker = N.isDirtyMarker(targetClass);

        return Stream.of(rowIterator).map(new Function<Object[], T>() {
            @SuppressWarnings("deprecation")
            @Override
            public T apply(Object[] a) {
                if (isMap) {
                    final Map<String, Object> m = (Map<String, Object>) N.newInstance(targetClass);

                    for (int i = 0; i < columnCount; i++) {
                        m.put(columnLabels[i], a[i]);
                    }

                    return (T) m;
                } else {
                    final Object entity = N.newInstance(targetClass);

                    for (int i = 0; i < columnCount; i++) {
                        if (columnLabels[i] == null) {
                            continue;
                        }

                        if (ClassUtil.setPropValue(entity, columnLabels[i], a[i], true) == false) {
                            columnLabels[i] = null;
                        }
                    }

                    if (isDirtyMarker) {
                        ((DirtyMarker) entity).markDirty(false);
                    }

                    return (T) entity;
                }
            }
        });
    }

    public static <T> Try<Stream<T>> of(final Class<T> targetClass, final RowIterator rowIterator, final boolean closeRowIterator) {
        return closeRowIterator ? of(targetClass, rowIterator).onClose(newCloseHandle(rowIterator)).tried() : of(targetClass, rowIterator).tried();
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param resultSet
     * @return
     */
    public static Stream<Object[]> of(final ResultSet resultSet) {
        return of(new RowIterator(resultSet, false, false));
    }

    public static Try<Stream<Object[]>> of(final ResultSet resultSet, final boolean closeResultSet) {
        return closeResultSet ? of(resultSet).onClose(newCloseHandle(resultSet)).tried() : of(resultSet).tried();
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param targetClass
     * @param resultSet
     * @return
     */
    public static <T> Stream<T> of(final Class<T> targetClass, final ResultSet resultSet) {
        return of(targetClass, new RowIterator(resultSet, false, false));
    }

    public static <T> Try<Stream<T>> of(final Class<T> targetClass, final ResultSet resultSet, final boolean closeResultSet) {
        return closeResultSet ? of(targetClass, resultSet).onClose(newCloseHandle(resultSet)).tried() : of(targetClass, resultSet).tried();
    }

    public static Try<Stream<String>> of(final File file) {
        return of(file, Charsets.DEFAULT);
    }

    public static Try<Stream<String>> of(final File file, final Charset charset) {
        final Reader reader = IOUtil.newBufferedReader(file, charset == null ? Charsets.DEFAULT : charset);

        return of(reader).onClose(newCloseHandle(reader)).tried();
    }

    public static Try<Stream<String>> of(final Path path) {
        return of(path, Charsets.DEFAULT);
    }

    public static Try<Stream<String>> of(final Path path, final Charset charset) {
        final Reader reader = IOUtil.newBufferedReader(path, charset == null ? Charsets.DEFAULT : charset);

        return of(reader).onClose(new Runnable() {
            private boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeQuietly(reader);
            }
        }).tried();
    }

    public static <T> Stream<T> ofNullable(T t) {
        return t == null ? Stream.<T> empty() : of(t);
    }

    public static Stream<Boolean> of(final boolean[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Boolean> of(final boolean[] a, final int fromIndex, final int toIndex) {
        Stream.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Boolean>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Boolean next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }
                return a[cursor++];
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Boolean.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Character> of(char[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Character> of(final char[] a, final int fromIndex, final int toIndex) {
        Stream.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Character>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Character next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Character.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Byte> of(byte[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Byte> of(final byte[] a, final int fromIndex, final int toIndex) {
        Stream.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Byte>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Byte next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Byte.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Short> of(short[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Short> of(final short[] a, final int fromIndex, final int toIndex) {
        Stream.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Short>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Short next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Short.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Integer> of(int[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Integer> of(final int[] a, final int fromIndex, final int toIndex) {
        Stream.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Integer>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Integer next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Integer.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Long> of(long[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Long> of(final long[] a, final int fromIndex, final int toIndex) {
        Stream.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Long>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Long next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Long.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Float> of(float[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Float> of(final float[] a, final int fromIndex, final int toIndex) {
        Stream.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Float>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Float next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Float.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Double> of(double[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Double> of(final double[] a, final int fromIndex, final int toIndex) {
        Stream.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Double>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Double next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Double.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    /**
     * Lazy evaluation.
     * @param supplier
     * @return
     */
    public static <T> Stream<T> of(final Supplier<Collection<? extends T>> supplier) {
        final Iterator<T> iter = new ObjIteratorEx<T>() {
            private Iterator<? extends T> iterator = null;

            @Override
            public boolean hasNext() {
                if (iterator == null) {
                    init();
                }

                return iterator.hasNext();
            }

            @Override
            public T next() {
                if (iterator == null) {
                    init();
                }

                return iterator.next();
            }

            private void init() {
                final Collection<? extends T> c = supplier.get();

                if (N.isNullOrEmpty(c)) {
                    iterator = ObjIterator.empty();
                } else {
                    iterator = c.iterator();
                }
            }
        };

        return of(iter);
    }

    public static <T> Stream<T> repeat(final T element, final long n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0) {
            return empty();
        }

        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public T next() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                return element;
            }

            @Override
            public void skip(long n) {
                cnt = n >= cnt ? 0 : cnt - (int) n;
            }

            @Override
            public long count() {
                return cnt;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= cnt ? a : N.copyOf(a, (int) cnt);

                for (int i = 0; i < cnt; i++) {
                    a[i] = (A) element;
                }

                cnt = 0;

                return a;
            }
        });
    }

    public static <T> Stream<T> iterate(final Supplier<Boolean> hasNext, final Supplier<? extends T> next) {
        N.requireNonNull(hasNext);
        N.requireNonNull(next);

        return of(new ObjIteratorEx<T>() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.get().booleanValue();
                }

                return hasNextVal;
            }

            @Override
            public T next() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return next.get();
            }
        });
    }

    /**
     * Returns a sequential ordered {@code Stream} produced by iterative
     * application of a function {@code f} to an initial element {@code seed},
     * producing a {@code Stream} consisting of {@code seed}, {@code f(seed)},
     * {@code f(f(seed))}, etc.
     *
     * <p>The first element (position {@code 0}) in the {@code Stream} will be
     * the provided {@code seed}.  For {@code n > 0}, the element at position
     * {@code n}, will be the result of applying the function {@code f} to the
     * element at position {@code n - 1}.
     * 
     * @param seed
     * @param hasNext
     * @param f
     * @return
     */
    public static <T> Stream<T> iterate(final T seed, final Supplier<Boolean> hasNext, final UnaryOperator<T> f) {
        N.requireNonNull(hasNext);
        N.requireNonNull(f);

        return of(new ObjIteratorEx<T>() {
            private T t = (T) NONE;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.get().booleanValue();
                }

                return hasNextVal;
            }

            @Override
            public T next() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return t = (t == NONE) ? seed : f.apply(t);
            }
        });
    }

    /**
     * 
     * @param seed
     * @param hasNext test if has next by hasNext.test(seed) for first time and hasNext.test(f.apply(previous)) for remaining.
     * @param f
     * @return
     */
    public static <T> Stream<T> iterate(final T seed, final Predicate<T> hasNext, final UnaryOperator<T> f) {
        N.requireNonNull(hasNext);
        N.requireNonNull(f);

        return of(new ObjIteratorEx<T>() {
            private T t = (T) NONE;
            private T cur = (T) NONE;
            private boolean hasMore = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false && hasMore) {
                    hasNextVal = hasNext.test((cur = (t == NONE ? seed : f.apply(t))));

                    if (hasNextVal == false) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public T next() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                t = cur;
                cur = (T) NONE;
                hasNextVal = false;
                return t;
            }
        });
    }

    public static <T> Stream<T> iterate(final T seed, final UnaryOperator<T> f) {
        N.requireNonNull(f);

        return of(new ObjIteratorEx<T>() {
            private T t = (T) NONE;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                return t = t == NONE ? seed : f.apply(t);
            }
        });
    }

    public static <T> Stream<T> generate(final Supplier<T> s) {
        N.requireNonNull(s);

        return of(new ObjIteratorEx<T>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                return s.get();
            }
        });
    }

    /**
     * 
     * @param intervalInMillis
     * @param s
     * @return
     */
    public static <T> Stream<T> interval(final long intervalInMillis, final Supplier<T> s) {
        return interval(0, intervalInMillis, s);
    }

    /**
     * 
     * @param delayInMillis
     * @param intervalInMillis
     * @param s
     * @return
     * @see java.util.concurrent.TimeUnit
     */
    public static <T> Stream<T> interval(final long delayInMillis, final long intervalInMillis, final Supplier<T> s) {
        return interval(delayInMillis, intervalInMillis, TimeUnit.MILLISECONDS, s);
    }

    /**
     * 
     * @param delay
     * @param interval
     * @param unit
     * @param s
     * @return
     */
    public static <T> Stream<T> interval(final long delay, final long interval, final TimeUnit unit, final Supplier<T> s) {
        N.requireNonNull(s);

        final LongIteratorEx timer = LongStream.interval(delay, interval, unit).iteratorEx();

        return of(new ObjIteratorEx<T>() {
            @Override
            public boolean hasNext() {
                return timer.hasNext();
            }

            @Override
            public T next() {
                timer.nextLong();
                return s.get();
            }
        });
    }

    public static <T> Stream<T> interval(final long intervalInMillis, final LongFunction<T> s) {
        return interval(0, intervalInMillis, s);
    }

    /**
     * 
     * @param delayInMillis
     * @param intervalInMillis
     * @param s
     * @return
     * @see java.util.concurrent.TimeUnit
     */
    public static <T> Stream<T> interval(final long delayInMillis, final long intervalInMillis, final LongFunction<T> s) {
        return interval(delayInMillis, intervalInMillis, TimeUnit.MILLISECONDS, s);
    }

    /**
     * 
     * @param delay
     * @param interval
     * @param unit
     * @param s
     * @return
     */
    public static <T> Stream<T> interval(final long delay, final long interval, final TimeUnit unit, final LongFunction<T> s) {
        N.requireNonNull(s);

        final LongIteratorEx timer = LongStream.interval(delay, interval, unit).iteratorEx();

        return of(new ObjIteratorEx<T>() {
            @Override
            public boolean hasNext() {
                return timer.hasNext();
            }

            @Override
            public T next() {
                return s.apply(timer.nextLong());
            }
        });
    }

    /**
     * Splits the provided text into an array, separator specified, preserving all tokens, including empty tokens created by adjacent separators.
     * 
     * @param str
     * @param delimiter
     * @return
     */
    public static Stream<String> split(final String str, final String delimiter) {
        return split(str, delimiter, false);
    }

    /**
     * Splits the provided text into an array, separator specified, preserving all tokens, including empty tokens created by adjacent separators.
     * 
     * @param str
     * @param delimiter
     * @param trim
     * @return
     */
    public static Stream<String> split(final String str, final String delimiter, final boolean trim) {
        if (delimiter.length() == 1) {
            return split(str, delimiter.charAt(0), trim);
        } else {
            return of(N.splitPreserveAllTokens(str, delimiter, trim));
        }
    }

    /**
     * Splits the provided text into an array, separator specified, preserving all tokens, including empty tokens created by adjacent separators.
     * 
     * @param str
     * @param delimiter
     * @return
     */
    public static Stream<String> split(final String str, final char delimiter) {
        return split(str, delimiter, false);
    }

    /**
     * Splits the provided text into an array, separator specified, preserving all tokens, including empty tokens created by adjacent separators.
     * 
     * @param str
     * @param delimiter
     * @param trim
     * @return
     */
    public static Stream<String> split(final String str, final char delimiter, final boolean trim) {
        if (str == null || str.length() == 0) {
            return of("");
        }

        return of(new ObjIterator<String>() {
            private final int len = str.length();
            private boolean isLastDelimiter = str.charAt(len - 1) == delimiter;
            private int prePos = 0;
            private int curPos = 0;

            @Override
            public boolean hasNext() {
                if (prePos == curPos) {
                    while (curPos < len && str.charAt(curPos) != delimiter) {
                        curPos++;
                    }
                }

                return prePos < len || isLastDelimiter;
            }

            @Override
            public String next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (prePos < len) {
                    String res = str.subSequence(prePos, curPos).toString();
                    curPos++;
                    prePos = curPos;

                    return trim ? res.trim() : res;
                } else {
                    isLastDelimiter = false;

                    return N.EMPTY_STRING;
                }
            }
        });
    }

    public static Stream<File> list(final File parentPath) {
        if (!parentPath.exists()) {
            return empty();
        }

        return of(parentPath.listFiles());
    }

    public static Stream<File> list(final File parentPath, final boolean recursively) {
        if (!parentPath.exists()) {
            return empty();
        } else if (recursively == false) {
            return of(parentPath.listFiles());
        }

        final ObjIterator<File> iter = new ObjIterator<File>() {
            private final Queue<File> paths = N.asLinkedList(parentPath);
            private File[] subFiles = null;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                if ((subFiles == null || cursor >= subFiles.length) && paths.size() > 0) {
                    cursor = 0;
                    subFiles = null;

                    while (paths.size() > 0) {
                        subFiles = paths.poll().listFiles();

                        if (N.notNullOrEmpty(subFiles)) {
                            break;
                        }
                    }
                }

                return subFiles != null && cursor < subFiles.length;
            }

            @Override
            public File next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (subFiles[cursor].isDirectory()) {
                    paths.offer(subFiles[cursor]);
                }

                return subFiles[cursor++];
            }
        };

        return of(iter);
    }

    public static <T> Stream<T> observe(final BlockingQueue<T> queue, final Duration duration) {
        final Iterator<T> iter = new ObjIterator<T>() {
            private final long endTime = N.currentMillis() + duration.toMillis();
            private T next = null;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    final long curTime = N.currentMillis();

                    if (curTime <= endTime) {
                        try {
                            next = queue.poll(endTime - curTime, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                return next != null;
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final T res = next;
                next = null;
                return res;
            }
        };

        return of(iter);
    }

    public static <T> Stream<T> observe(final BlockingQueue<T> queue, final Predicate<? super T> isLast, final long maxWaitIntervalInMillis) {
        N.checkArgument(maxWaitIntervalInMillis > 0, "'maxWaitIntervalInMillis' can't be %s. It must be positive");

        final Iterator<T> iter = new ObjIterator<T>() {
            private T next = null;
            private boolean isDone = false;

            @Override
            public boolean hasNext() {
                if (next == null && isDone == false) {
                    do {
                        try {
                            next = queue.poll(maxWaitIntervalInMillis, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        isDone = isLast.test(next);
                    } while (next != null && isDone == false);
                }

                return next != null;
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final T res = next;
                next = null;
                return res;
            }
        };

        return of(iter);
    }

    @SafeVarargs
    public static <T> Stream<T> concat(final T[]... a) {
        return N.isNullOrEmpty(a) ? (Stream<T>) empty() : new IteratorStream<>(new ObjIteratorEx<T>() {
            private final Iterator<T[]> iter = N.asList(a).iterator();
            private Iterator<T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = ObjIteratorEx.of(iter.next());
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        });
    }

    @SafeVarargs
    public static <T> Stream<T> concat(final Collection<? extends T>... a) {
        return N.isNullOrEmpty(a) ? (Stream<T>) empty() : new IteratorStream<>(new ObjIteratorEx<T>() {
            private final Iterator<Collection<? extends T>> iter = N.asList(a).iterator();
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next().iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        });
    }

    @SafeVarargs
    public static <T> Stream<T> concat(final Iterator<? extends T>... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return concatt(N.asList(a));
    }

    @SafeVarargs
    public static <T> Stream<T> concat(final Stream<? extends T>... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return concat(N.asList(a));
    }

    public static <T> Stream<T> concat(final Collection<? extends Stream<? extends T>> c) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(c.size());

        for (Stream<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return concatt(iterList).onClose(newCloseHandler(c));
    }

    public static <T> Stream<T> concatt(final Collection<? extends Iterator<? extends T>> c) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        }

        return of(new ObjIteratorEx<T>() {
            private final Iterator<? extends Iterator<? extends T>> iterators = c.iterator();
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iterators.hasNext()) {
                    cur = iterators.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        });
    }

    // NO NEED.
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @return
    //     */
    //    public static <T> Stream<T> parallelConcat(final T[]... a) {
    //        return parallelConcat(a, DEFAULT_READING_THREAD_NUM, calculateQueueSize(a.length));
    //    }
    //
    //    /**
    //     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
    //     * 
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, a.length)
    //     * @param queueSize Default value is N.min(128, a.length * 16)
    //     * @return
    //     */
    //    public static <T> Stream<T> parallelConcat(final T[][] a, final int readThreadNum, final int queueSize) {
    //        if (N.isNullOrEmpty(a)) {
    //            return empty();
    //        }
    //
    //        final Iterator<? extends T>[] iters = new Iterator[a.length];
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            iters[i] = ImmutableIterator.of(a[i]);
    //        }
    //
    //        return parallelConcat(iters, readThreadNum, queueSize);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @return
    //     */
    //    public static <T> Stream<T> parallelConcat(final Collection<? extends T>... a) {
    //        return parallelConcat(a, DEFAULT_READING_THREAD_NUM, calculateQueueSize(a.length));
    //    }
    //
    //    /**
    //     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
    //     * 
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, a.length)
    //     * @param queueSize Default value is N.min(128, a.length * 16)
    //     * @return
    //     */
    //    public static <T> Stream<T> parallelConcat(final Collection<? extends T>[] a, final int readThreadNum, final int queueSize) {
    //        if (N.isNullOrEmpty(a)) {
    //            return empty();
    //        }
    //
    //        final Iterator<? extends T>[] iters = new Iterator[a.length];
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            iters[i] = a[i].iterator();
    //        }
    //
    //        return parallelConcat(iters, readThreadNum, queueSize);
    //    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Stream<T> parallelConcat(final Iterator<? extends T>... a) {
        return parallelConcat(a, DEFAULT_READING_THREAD_NUM, calculateQueueSize(a.length));
    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
     * 
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, a.length)
     * @param queueSize Default value is N.min(128, a.length * 16)
     * @return
     */
    public static <T> Stream<T> parallelConcat(final Iterator<? extends T>[] a, final int readThreadNum, final int queueSize) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return parallelConcatt(N.asList(a), readThreadNum, queueSize);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Stream<T> parallelConcat(final Stream<? extends T>... a) {
        return parallelConcat(a, DEFAULT_READING_THREAD_NUM, calculateQueueSize(a.length));
    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
     * 
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, a.length)
     * @param queueSize Default value is N.min(128, a.length * 16)
     * @return
     */
    public static <T> Stream<T> parallelConcat(final Stream<? extends T>[] a, final int readThreadNum, final int queueSize) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return parallelConcat(N.asList(a), readThreadNum, queueSize);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @return
     */
    public static <T> Stream<T> parallelConcat(final Collection<? extends Stream<? extends T>> c) {
        return parallelConcat(c, DEFAULT_READING_THREAD_NUM);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param readThreadNum
     * @return
     */
    public static <T> Stream<T> parallelConcat(final Collection<? extends Stream<? extends T>> c, final int readThreadNum) {
        return parallelConcat(c, readThreadNum, calculateQueueSize(c.size()));
    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
     * 
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, c.size())
     * @param queueSize Default value is N.min(128, c.size() * 16)
     * @return
     */
    public static <T> Stream<T> parallelConcat(final Collection<? extends Stream<? extends T>> c, final int readThreadNum, final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(c.size());

        for (Stream<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return parallelConcatt(iterList, readThreadNum, queueSize).onClose(newCloseHandler(c));
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @return
     */
    public static <T> Stream<T> parallelConcatt(final Collection<? extends Iterator<? extends T>> c) {
        return parallelConcatt(c, DEFAULT_READING_THREAD_NUM);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param readThreadNum
     * @return
     */
    public static <T> Stream<T> parallelConcatt(final Collection<? extends Iterator<? extends T>> c, final int readThreadNum) {
        return parallelConcatt(c, readThreadNum, calculateQueueSize(c.size()));
    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
     * 
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, c.size())
     * @param queueSize Default value is N.min(128, c.size() * 16)
     * @return
     */
    public static <T> Stream<T> parallelConcatt(final Collection<? extends Iterator<? extends T>> c, final int readThreadNum, final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final AtomicInteger threadCounter = new AtomicInteger(c.size());
        final ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        final Iterator<? extends Iterator<? extends T>> iterators = c.iterator();
        final int threadNum = Math.min(c.size(), readThreadNum);

        for (int i = 0; i < threadNum; i++) {
            asyncExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (onGoing.value()) {
                            Iterator<? extends T> iter = null;

                            synchronized (iterators) {
                                if (iterators.hasNext()) {
                                    iter = iterators.next();
                                } else {
                                    break;
                                }
                            }

                            T next = null;

                            while (onGoing.value() && iter.hasNext()) {
                                next = iter.next();

                                if (next == null) {
                                    next = (T) NONE;
                                }

                                if (queue.offer(next) == false) {
                                    while (onGoing.value()) {
                                        if (queue.offer(next, 100, TimeUnit.MILLISECONDS)) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) {
                        setError(eHolder, e, onGoing);
                    } finally {
                        threadCounter.decrementAndGet();
                    }
                }
            });
        }

        return of(new QueuedIterator<T>(queueSize) {
            T next = null;

            @Override
            public boolean hasNext() {
                try {
                    if (next == null && (next = queue.poll()) == null) {
                        while (onGoing.value() && (threadCounter.get() > 0 || queue.size() > 0)) { // (queue.size() > 0 || counter.get() > 0) is wrong. has to check counter first
                            if ((next = queue.poll(1, TimeUnit.MILLISECONDS)) != null) {
                                break;
                            }
                        }
                    }
                } catch (Throwable e) {
                    setError(eHolder, e, onGoing);
                }

                if (eHolder.value() != null) {
                    throwError(eHolder, onGoing);
                }

                return next != null;
            }

            @Override
            public T next() {
                if (next == null && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                T result = next == NONE ? null : next;
                next = null;
                return result;
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final char[] a, final char[] b, final CharBiFunction<R> zipFunction) {
        return zip(CharIteratorEx.of(a), CharIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final char[] a, final char[] b, final char[] c, final CharTriFunction<R> zipFunction) {
        return zip(CharIteratorEx.of(a), CharIteratorEx.of(b), CharIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final CharIterator a, final CharIterator b, final CharBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextChar(), b.nextChar());
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final CharIterator a, final CharIterator b, final CharIterator c, final CharTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextChar(), b.nextChar(), c.nextChar());
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final CharStream a, final CharStream b, final CharBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final CharStream a, final CharStream b, final CharStream c, final CharTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends CharStream> c, final CharNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<CharIterator> iterList = new ArrayList<>(len);

        for (CharStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (CharIterator e : iterList) {
                    if (e.hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final char[] args = new char[len];
                int idx = 0;

                for (CharIterator e : iterList) {
                    args[idx++] = e.nextChar();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final char[] a, final char[] b, final char valueForNoneA, final char valueForNoneB, final CharBiFunction<R> zipFunction) {
        return zip(CharIteratorEx.of(a), CharIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final char[] a, final char[] b, final char[] c, final char valueForNoneA, final char valueForNoneB,
            final char valueForNoneC, final CharTriFunction<R> zipFunction) {
        return zip(CharIteratorEx.of(a), CharIteratorEx.of(b), CharIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final CharIterator a, final CharIterator b, final char valueForNoneA, final char valueForNoneB,
            final CharBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextChar() : valueForNoneA, b.hasNext() ? b.nextChar() : valueForNoneB);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final CharIterator a, final CharIterator b, final CharIterator c, final char valueForNoneA, final char valueForNoneB,
            final char valueForNoneC, final CharTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextChar() : valueForNoneA, b.hasNext() ? b.nextChar() : valueForNoneB,
                        c.hasNext() ? c.nextChar() : valueForNoneC);
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final CharStream a, final CharStream b, final char valueForNoneA, final char valueForNoneB,
            final CharBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final CharStream a, final CharStream b, final CharStream c, final char valueForNoneA, final char valueForNoneB,
            final char valueForNoneC, final CharTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until all of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends CharStream> c, final char[] valuesForNone, final CharNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<CharIterator> iterList = new ArrayList<>(len);

        for (CharStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (CharIterator e : iterList) {
                    if (e.hasNext()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final char[] args = new char[len];
                int idx = 0;
                boolean hasNext = false;

                for (CharIterator e : iterList) {
                    if (e.hasNext()) {
                        hasNext = true;
                        args[idx++] = e.nextChar();
                    } else {
                        args[idx] = valuesForNone[idx];
                        idx++;
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final byte[] a, final byte[] b, final ByteBiFunction<R> zipFunction) {
        return zip(ByteIteratorEx.of(a), ByteIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final byte[] a, final byte[] b, final byte[] c, final ByteTriFunction<R> zipFunction) {
        return zip(ByteIteratorEx.of(a), ByteIteratorEx.of(b), ByteIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final ByteIterator a, final ByteIterator b, final ByteBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextByte(), b.nextByte());
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final ByteIterator a, final ByteIterator b, final ByteIterator c, final ByteTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextByte(), b.nextByte(), c.nextByte());
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final ByteStream a, final ByteStream b, final ByteBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final ByteStream a, final ByteStream b, final ByteStream c, final ByteTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends ByteStream> c, final ByteNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<ByteIterator> iterList = new ArrayList<>(len);

        for (ByteStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (ByteIterator e : iterList) {
                    if (e.hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final byte[] args = new byte[len];
                int idx = 0;

                for (ByteIterator e : iterList) {
                    args[idx++] = e.nextByte();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final byte[] a, final byte[] b, final byte valueForNoneA, final byte valueForNoneB, final ByteBiFunction<R> zipFunction) {
        return zip(ByteIteratorEx.of(a), ByteIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final byte[] a, final byte[] b, final byte[] c, final byte valueForNoneA, final byte valueForNoneB,
            final byte valueForNoneC, final ByteTriFunction<R> zipFunction) {
        return zip(ByteIteratorEx.of(a), ByteIteratorEx.of(b), ByteIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final ByteIterator a, final ByteIterator b, final byte valueForNoneA, final byte valueForNoneB,
            final ByteBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextByte() : valueForNoneA, b.hasNext() ? b.nextByte() : valueForNoneB);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final ByteIterator a, final ByteIterator b, final ByteIterator c, final byte valueForNoneA, final byte valueForNoneB,
            final byte valueForNoneC, final ByteTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextByte() : valueForNoneA, b.hasNext() ? b.nextByte() : valueForNoneB,
                        c.hasNext() ? c.nextByte() : valueForNoneC);
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final ByteStream a, final ByteStream b, final byte valueForNoneA, final byte valueForNoneB,
            final ByteBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final ByteStream a, final ByteStream b, final ByteStream c, final byte valueForNoneA, final byte valueForNoneB,
            final byte valueForNoneC, final ByteTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until all of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends ByteStream> c, final byte[] valuesForNone, final ByteNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<ByteIterator> iterList = new ArrayList<>(len);

        for (ByteStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (ByteIterator e : iterList) {
                    if (e.hasNext()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final byte[] args = new byte[len];
                int idx = 0;
                boolean hasNext = false;

                for (ByteIterator e : iterList) {
                    if (e.hasNext()) {
                        hasNext = true;
                        args[idx++] = e.nextByte();
                    } else {
                        args[idx] = valuesForNone[idx];
                        idx++;
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final short[] a, final short[] b, final ShortBiFunction<R> zipFunction) {
        return zip(ShortIteratorEx.of(a), ShortIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final short[] a, final short[] b, final short[] c, final ShortTriFunction<R> zipFunction) {
        return zip(ShortIteratorEx.of(a), ShortIteratorEx.of(b), ShortIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final ShortIterator a, final ShortIterator b, final ShortBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextShort(), b.nextShort());
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final ShortIterator a, final ShortIterator b, final ShortIterator c, final ShortTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextShort(), b.nextShort(), c.nextShort());
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final ShortStream a, final ShortStream b, final ShortBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final ShortStream a, final ShortStream b, final ShortStream c, final ShortTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends ShortStream> c, final ShortNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<ShortIterator> iterList = new ArrayList<>(len);

        for (ShortStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (ShortIterator e : iterList) {
                    if (e.hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final short[] args = new short[len];
                int idx = 0;

                for (ShortIterator e : iterList) {
                    args[idx++] = e.nextShort();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final short[] a, final short[] b, final short valueForNoneA, final short valueForNoneB,
            final ShortBiFunction<R> zipFunction) {
        return zip(ShortIteratorEx.of(a), ShortIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final short[] a, final short[] b, final short[] c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTriFunction<R> zipFunction) {
        return zip(ShortIteratorEx.of(a), ShortIteratorEx.of(b), ShortIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final ShortIterator a, final ShortIterator b, final short valueForNoneA, final short valueForNoneB,
            final ShortBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextShort() : valueForNoneA, b.hasNext() ? b.nextShort() : valueForNoneB);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final ShortIterator a, final ShortIterator b, final ShortIterator c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextShort() : valueForNoneA, b.hasNext() ? b.nextShort() : valueForNoneB,
                        c.hasNext() ? c.nextShort() : valueForNoneC);
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final ShortStream a, final ShortStream b, final short valueForNoneA, final short valueForNoneB,
            final ShortBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final ShortStream a, final ShortStream b, final ShortStream c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until all of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends ShortStream> c, final short[] valuesForNone, final ShortNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<ShortIterator> iterList = new ArrayList<>(len);

        for (ShortStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (ShortIterator e : iterList) {
                    if (e.hasNext()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final short[] args = new short[len];
                int idx = 0;
                boolean hasNext = false;

                for (ShortIterator e : iterList) {
                    if (e.hasNext()) {
                        hasNext = true;
                        args[idx++] = e.nextShort();
                    } else {
                        args[idx] = valuesForNone[idx];
                        idx++;
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final int[] a, final int[] b, final IntBiFunction<R> zipFunction) {
        return zip(IntIteratorEx.of(a), IntIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final int[] a, final int[] b, final int[] c, final IntTriFunction<R> zipFunction) {
        return zip(IntIteratorEx.of(a), IntIteratorEx.of(b), IntIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final IntIterator a, final IntIterator b, final IntBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextInt(), b.nextInt());
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final IntIterator a, final IntIterator b, final IntIterator c, final IntTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextInt(), b.nextInt(), c.nextInt());
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final IntStream a, final IntStream b, final IntBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final IntStream a, final IntStream b, final IntStream c, final IntTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends IntStream> c, final IntNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<IntIterator> iterList = new ArrayList<>(len);

        for (IntStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (IntIterator e : iterList) {
                    if (e.hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final int[] args = new int[len];
                int idx = 0;

                for (IntIterator e : iterList) {
                    args[idx++] = e.nextInt();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final int[] a, final int[] b, final int valueForNoneA, final int valueForNoneB, final IntBiFunction<R> zipFunction) {
        return zip(IntIteratorEx.of(a), IntIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final int[] a, final int[] b, final int[] c, final int valueForNoneA, final int valueForNoneB, final int valueForNoneC,
            final IntTriFunction<R> zipFunction) {
        return zip(IntIteratorEx.of(a), IntIteratorEx.of(b), IntIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final IntIterator a, final IntIterator b, final int valueForNoneA, final int valueForNoneB,
            final IntBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextInt() : valueForNoneA, b.hasNext() ? b.nextInt() : valueForNoneB);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final IntIterator a, final IntIterator b, final IntIterator c, final int valueForNoneA, final int valueForNoneB,
            final int valueForNoneC, final IntTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextInt() : valueForNoneA, b.hasNext() ? b.nextInt() : valueForNoneB,
                        c.hasNext() ? c.nextInt() : valueForNoneC);
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final IntStream a, final IntStream b, final int valueForNoneA, final int valueForNoneB,
            final IntBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final IntStream a, final IntStream b, final IntStream c, final int valueForNoneA, final int valueForNoneB,
            final int valueForNoneC, final IntTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until all of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends IntStream> c, final int[] valuesForNone, final IntNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<IntIterator> iterList = new ArrayList<>(len);

        for (IntStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (IntIterator e : iterList) {
                    if (e.hasNext()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final int[] args = new int[len];
                int idx = 0;
                boolean hasNext = false;

                for (IntIterator e : iterList) {
                    if (e.hasNext()) {
                        hasNext = true;
                        args[idx++] = e.nextInt();
                    } else {
                        args[idx] = valuesForNone[idx];
                        idx++;
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final long[] a, final long[] b, final LongBiFunction<R> zipFunction) {
        return zip(LongIteratorEx.of(a), LongIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final long[] a, final long[] b, final long[] c, final LongTriFunction<R> zipFunction) {
        return zip(LongIteratorEx.of(a), LongIteratorEx.of(b), LongIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final LongIterator a, final LongIterator b, final LongBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextLong(), b.nextLong());
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final LongIterator a, final LongIterator b, final LongIterator c, final LongTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextLong(), b.nextLong(), c.nextLong());
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final LongStream a, final LongStream b, final LongBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final LongStream a, final LongStream b, final LongStream c, final LongTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends LongStream> c, final LongNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<LongIterator> iterList = new ArrayList<>(len);

        for (LongStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (LongIterator e : iterList) {
                    if (e.hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final long[] args = new long[len];
                int idx = 0;

                for (LongIterator e : iterList) {
                    args[idx++] = e.nextLong();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final long[] a, final long[] b, final long valueForNoneA, final long valueForNoneB, final LongBiFunction<R> zipFunction) {
        return zip(LongIteratorEx.of(a), LongIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final long[] a, final long[] b, final long[] c, final long valueForNoneA, final long valueForNoneB,
            final long valueForNoneC, final LongTriFunction<R> zipFunction) {
        return zip(LongIteratorEx.of(a), LongIteratorEx.of(b), LongIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final LongIterator a, final LongIterator b, final long valueForNoneA, final long valueForNoneB,
            final LongBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextLong() : valueForNoneA, b.hasNext() ? b.nextLong() : valueForNoneB);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final LongIterator a, final LongIterator b, final LongIterator c, final long valueForNoneA, final long valueForNoneB,
            final long valueForNoneC, final LongTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextLong() : valueForNoneA, b.hasNext() ? b.nextLong() : valueForNoneB,
                        c.hasNext() ? c.nextLong() : valueForNoneC);
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final LongStream a, final LongStream b, final long valueForNoneA, final long valueForNoneB,
            final LongBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final LongStream a, final LongStream b, final LongStream c, final long valueForNoneA, final long valueForNoneB,
            final long valueForNoneC, final LongTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until all of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends LongStream> c, final long[] valuesForNone, final LongNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<LongIterator> iterList = new ArrayList<>(len);

        for (LongStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (LongIterator e : iterList) {
                    if (e.hasNext()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final long[] args = new long[len];
                int idx = 0;
                boolean hasNext = false;

                for (LongIterator e : iterList) {
                    if (e.hasNext()) {
                        hasNext = true;
                        args[idx++] = e.nextLong();
                    } else {
                        args[idx] = valuesForNone[idx];
                        idx++;
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final float[] a, final float[] b, final FloatBiFunction<R> zipFunction) {
        return zip(FloatIteratorEx.of(a), FloatIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final float[] a, final float[] b, final float[] c, final FloatTriFunction<R> zipFunction) {
        return zip(FloatIteratorEx.of(a), FloatIteratorEx.of(b), FloatIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final FloatIterator a, final FloatIterator b, final FloatBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextFloat(), b.nextFloat());
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final FloatIterator a, final FloatIterator b, final FloatIterator c, final FloatTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextFloat(), b.nextFloat(), c.nextFloat());
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final FloatStream a, final FloatStream b, final FloatBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final FloatStream a, final FloatStream b, final FloatStream c, final FloatTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends FloatStream> c, final FloatNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<FloatIterator> iterList = new ArrayList<>(len);

        for (FloatStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (FloatIterator e : iterList) {
                    if (e.hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final float[] args = new float[len];
                int idx = 0;

                for (FloatIterator e : iterList) {
                    args[idx++] = e.nextFloat();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final float[] a, final float[] b, final float valueForNoneA, final float valueForNoneB,
            final FloatBiFunction<R> zipFunction) {
        return zip(FloatIteratorEx.of(a), FloatIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final float[] a, final float[] b, final float[] c, final float valueForNoneA, final float valueForNoneB,
            final float valueForNoneC, final FloatTriFunction<R> zipFunction) {
        return zip(FloatIteratorEx.of(a), FloatIteratorEx.of(b), FloatIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final FloatIterator a, final FloatIterator b, final float valueForNoneA, final float valueForNoneB,
            final FloatBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextFloat() : valueForNoneA, b.hasNext() ? b.nextFloat() : valueForNoneB);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final FloatIterator a, final FloatIterator b, final FloatIterator c, final float valueForNoneA, final float valueForNoneB,
            final float valueForNoneC, final FloatTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextFloat() : valueForNoneA, b.hasNext() ? b.nextFloat() : valueForNoneB,
                        c.hasNext() ? c.nextFloat() : valueForNoneC);
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final FloatStream a, final FloatStream b, final float valueForNoneA, final float valueForNoneB,
            final FloatBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final FloatStream a, final FloatStream b, final FloatStream c, final float valueForNoneA, final float valueForNoneB,
            final float valueForNoneC, final FloatTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until all of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends FloatStream> c, final float[] valuesForNone, final FloatNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<FloatIterator> iterList = new ArrayList<>(len);

        for (FloatStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (FloatIterator e : iterList) {
                    if (e.hasNext()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final float[] args = new float[len];
                int idx = 0;
                boolean hasNext = false;

                for (FloatIterator e : iterList) {
                    if (e.hasNext()) {
                        hasNext = true;
                        args[idx++] = e.nextFloat();
                    } else {
                        args[idx] = valuesForNone[idx];
                        idx++;
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final double[] a, final double[] b, final DoubleBiFunction<R> zipFunction) {
        return zip(DoubleIteratorEx.of(a), DoubleIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final double[] a, final double[] b, final double[] c, final DoubleTriFunction<R> zipFunction) {
        return zip(DoubleIteratorEx.of(a), DoubleIteratorEx.of(b), DoubleIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final DoubleIterator a, final DoubleIterator b, final DoubleBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextDouble(), b.nextDouble());
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final DoubleTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextDouble(), b.nextDouble(), c.nextDouble());
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final DoubleStream a, final DoubleStream b, final DoubleBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final DoubleStream a, final DoubleStream b, final DoubleStream c, final DoubleTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends DoubleStream> c, final DoubleNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<DoubleIterator> iterList = new ArrayList<>(len);

        for (DoubleStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (DoubleIterator e : iterList) {
                    if (e.hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final double[] args = new double[len];
                int idx = 0;

                for (DoubleIterator e : iterList) {
                    args[idx++] = e.nextDouble();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final double[] a, final double[] b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBiFunction<R> zipFunction) {
        return zip(DoubleIteratorEx.of(a), DoubleIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final double[] a, final double[] b, final double[] c, final double valueForNoneA, final double valueForNoneB,
            final double valueForNoneC, final DoubleTriFunction<R> zipFunction) {
        return zip(DoubleIteratorEx.of(a), DoubleIteratorEx.of(b), DoubleIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final DoubleIterator a, final DoubleIterator b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextDouble() : valueForNoneA, b.hasNext() ? b.nextDouble() : valueForNoneB);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final double valueForNoneA,
            final double valueForNoneB, final double valueForNoneC, final DoubleTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextDouble() : valueForNoneA, b.hasNext() ? b.nextDouble() : valueForNoneB,
                        c.hasNext() ? c.nextDouble() : valueForNoneC);
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final DoubleStream a, final DoubleStream b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBiFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <R> Stream<R> zip(final DoubleStream a, final DoubleStream b, final DoubleStream c, final double valueForNoneA, final double valueForNoneB,
            final double valueForNoneC, final DoubleTriFunction<R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until all of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends DoubleStream> c, final double[] valuesForNone, final DoubleNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<DoubleIterator> iterList = new ArrayList<>(len);

        for (DoubleStream e : c) {
            iterList.add(e.iteratorEx());
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (DoubleIterator e : iterList) {
                    if (e.hasNext()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final double[] args = new double[len];
                int idx = 0;
                boolean hasNext = false;

                for (DoubleIterator e : iterList) {
                    if (e.hasNext()) {
                        hasNext = true;
                        args[idx++] = e.nextDouble();
                    } else {
                        args[idx] = valuesForNone[idx];
                        idx++;
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, R> Stream<R> zip(final A[] a, final B[] b, final BiFunction<? super A, ? super B, R> zipFunction) {
        return zip(ObjIteratorEx.of(a), ObjIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, C, R> Stream<R> zip(final A[] a, final B[] b, final C[] c, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(ObjIteratorEx.of(a), ObjIteratorEx.of(b), ObjIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, R> Stream<R> zip(final Collection<? extends A> a, final Collection<? extends B> b,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, C, R> Stream<R> zip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), c.iterator(), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, R> Stream<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final BiFunction<? super A, ? super B, R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.next(), b.next());
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, C, R> Stream<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.next(), b.next(), c.next());
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, R> Stream<R> zip(final Stream<? extends A> a, final Stream<? extends B> b, final BiFunction<? super A, ? super B, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, C, R> Stream<R> zip(final Stream<? extends A> a, final Stream<? extends B> b, final Stream<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), c.iterator(), zipFunction).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> zip(final Collection<? extends Stream<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Stream<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return zipp(iterList, zipFunction).onClose(newCloseHandler(c));
    }

    public static <T, R> Stream<R> zipp(final Collection<? extends Iterator<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (Iterator<? extends T> e : c) {
                    if (e.hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final Object[] args = new Object[len];
                int idx = 0;

                for (Iterator<? extends T> e : c) {
                    args[idx++] = e.next();
                }

                return zipFunction.apply(Arrays.asList((T[]) args));
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return zip(ObjIteratorEx.of(a), ObjIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> Stream<R> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(ObjIteratorEx.of(a), ObjIteratorEx.of(b), ObjIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> zip(final Collection<? extends A> a, final Collection<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> Stream<R> zip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), c.iterator(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.next() : valueForNoneA, b.hasNext() ? b.next() : valueForNoneB);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> Stream<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.next() : valueForNoneA, b.hasNext() ? b.next() : valueForNoneB,
                        c.hasNext() ? c.next() : valueForNoneC);
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> zip(final Stream<? extends A> a, final Stream<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> Stream<R> zip(final Stream<? extends A> a, final Stream<? extends B> b, final Stream<? extends C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), c.iterator(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until all of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> zip(final Collection<? extends Stream<? extends T>> c, final Object[] valuesForNone,
            Function<? super List<? extends T>, R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Stream<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return zipp(iterList, valuesForNone, zipFunction).onClose(newCloseHandler(c));
    }

    /**
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> zipp(final Collection<? extends Iterator<? extends T>> c, final Object[] valuesForNone,
            final Function<? super List<? extends T>, R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (Iterator<? extends T> e : c) {
                    if (e.hasNext()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final Object[] args = new Object[len];
                int idx = 0;
                boolean hasNext = false;

                for (Iterator<? extends T> e : c) {
                    if (e.hasNext()) {
                        hasNext = true;
                        args[idx++] = e.next();
                    } else {
                        args[idx] = valuesForNone[idx];
                        idx++;
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(Arrays.asList((T[]) args));
            }
        });
    }

    // NO NEED.
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final A[] a, final B[] b, final BiFunction<? super A, ? super B, R> zipFunction) {
    //        return parallelZip(a, b, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 8
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final A[] a, final B[] b, final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
    //        return parallelZip(ImmutableIterator.of(a), ImmutableIterator.of(b), zipFunction, queueSize);
    //    }
    //
    //    public static <A, B, C, R> Stream<R> parallelZip(final A[] a, final B[] b, final C[] c, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
    //        return parallelZip(a, b, c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 8
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final A[] a, final B[] b, final C[] c, final TriFunction<? super A, ? super B, ? super C, R> zipFunction,
    //            final int queueSize) {
    //        return parallelZip(ImmutableIterator.of(a), ImmutableIterator.of(b), ImmutableIterator.of(c), zipFunction, queueSize);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b,
    //            final BiFunction<? super A, ? super B, R> zipFunction) {
    //        return parallelZip(a, b, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 8
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b,
    //            final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
    //        return parallelZip(a.iterator(), b.iterator(), zipFunction, queueSize);
    //    }
    //
    //    public static <A, B, C, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
    //            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
    //        return parallelZip(a, b, c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 8
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
    //            final TriFunction<? super A, ? super B, ? super C, R> zipFunction, final int queueSize) {
    //        return parallelZip(a.iterator(), b.iterator(), c.iterator(), zipFunction, queueSize);
    //    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return parallelZip(a, b, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
        final AtomicInteger threadCounterA = new AtomicInteger(1);
        final AtomicInteger threadCounterB = new AtomicInteger(1);
        final BlockingQueue<A> queueA = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<B> queueB = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        readToQueue(a, b, asyncExecutor, threadCounterA, threadCounterB, queueA, queueB, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            A nextA = null;
            B nextB = null;

            @Override
            public boolean hasNext() {
                if (nextA == null || nextB == null) {
                    try {
                        while (nextA == null && onGoing.value() && (threadCounterA.get() > 0 || queueA.size() > 0)) { // (threadCounterA.get() > 0 || queueA.size() > 0) is wrong. has to check counter first
                            nextA = queueA.poll(1, TimeUnit.MILLISECONDS);
                        }

                        if (nextA == null) {
                            onGoing.setFalse();

                            return false;
                        }

                        while (nextB == null && onGoing.value() && (threadCounterB.get() > 0 || queueB.size() > 0)) { // (threadCounterB.get() > 0 || queueB.size() > 0) is wrong. has to check counter first
                            nextB = queueB.poll(1, TimeUnit.MILLISECONDS);
                        }

                        if (nextB == null) {
                            onGoing.setFalse();

                            return false;
                        }
                    } catch (Throwable e) {
                        setError(eHolder, e, onGoing);
                    }

                    if (eHolder.value() != null) {
                        throwError(eHolder, onGoing);
                    }
                }

                return true;
            }

            @Override
            public R next() {
                if ((nextA == null || nextB == null) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                boolean isOK = false;

                try {
                    final R result = zipFunction.apply(nextA == NONE ? null : nextA, nextB == NONE ? null : nextB);
                    nextA = null;
                    nextB = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    public static <A, B, C, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return parallelZip(a, b, c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction, final int queueSize) {
        final AtomicInteger threadCounterA = new AtomicInteger(1);
        final AtomicInteger threadCounterB = new AtomicInteger(1);
        final AtomicInteger threadCounterC = new AtomicInteger(1);
        final BlockingQueue<A> queueA = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<B> queueB = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<C> queueC = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        readToQueue(a, b, c, asyncExecutor, threadCounterA, threadCounterB, threadCounterC, queueA, queueB, queueC, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            A nextA = null;
            B nextB = null;
            C nextC = null;

            @Override
            public boolean hasNext() {
                if (nextA == null || nextB == null || nextC == null) {
                    try {
                        while (nextA == null && onGoing.value() && (threadCounterA.get() > 0 || queueA.size() > 0)) { // (threadCounterA.get() > 0 || queueA.size() > 0) is wrong. has to check counter first
                            nextA = queueA.poll(1, TimeUnit.MILLISECONDS);
                        }

                        if (nextA == null) {
                            onGoing.setFalse();

                            return false;
                        }

                        while (nextB == null && onGoing.value() && (threadCounterB.get() > 0 || queueB.size() > 0)) { // (threadCounterB.get() > 0 || queueB.size() > 0) is wrong. has to check counter first
                            nextB = queueB.poll(1, TimeUnit.MILLISECONDS);
                        }

                        if (nextB == null) {
                            onGoing.setFalse();

                            return false;
                        }

                        while (nextC == null && onGoing.value() && (threadCounterC.get() > 0 || queueC.size() > 0)) { // (threadCounterC.get() > 0 || queueC.size() > 0) is wrong. has to check counter first
                            nextC = queueC.poll(1, TimeUnit.MILLISECONDS);
                        }

                        if (nextC == null) {
                            onGoing.setFalse();

                            return false;
                        }
                    } catch (Throwable e) {
                        setError(eHolder, e, onGoing);
                    }

                    if (eHolder.value() != null) {
                        throwError(eHolder, onGoing);
                    }
                }

                return true;
            }

            @Override
            public R next() {
                if ((nextA == null || nextB == null || nextC == null) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                boolean isOK = false;

                try {
                    final R result = zipFunction.apply(nextA == NONE ? null : nextA, nextB == NONE ? null : nextB, nextC == NONE ? null : nextC);
                    nextA = null;
                    nextB = null;
                    nextC = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final BiFunction<? super A, ? super B, R> zipFunction) {
        return parallelZip(a, b, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final BiFunction<? super A, ? super B, R> zipFunction,
            final int queueSize) {
        return parallelZip(a.iterator(), b.iterator(), zipFunction, queueSize).onClose(newCloseHandler(N.asList(a, b)));
    }

    public static <A, B, C, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final Stream<C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return parallelZip(a, b, c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final Stream<C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction, final int queueSize) {
        return parallelZip(a.iterator(), b.iterator(), c.iterator(), zipFunction, queueSize).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> parallelZip(final Collection<? extends Stream<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction) {
        return parallelZip(c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <T, R> Stream<R> parallelZip(final Collection<? extends Stream<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction,
            final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Stream<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return parallelZipp(iterList, zipFunction, queueSize).onClose(newCloseHandler(c));
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> parallelZipp(final Collection<? extends Iterator<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction) {
        return parallelZipp(c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <T, R> Stream<R> parallelZipp(final Collection<? extends Iterator<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction,
            final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final AtomicInteger[] counters = new AtomicInteger[len];
        final BlockingQueue<Object>[] queues = new ArrayBlockingQueue[len];
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        readToQueue(c, queueSize, asyncExecutor, counters, queues, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            Object[] next = null;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = new Object[len];

                    for (int i = 0; i < len; i++) {
                        try {
                            while (next[i] == null && onGoing.value() && (counters[i].get() > 0 || queues[i].size() > 0)) { // (counters[i].get() > 0 || queues[i].size() > 0) is wrong. has to check counter first
                                next[i] = queues[i].poll(1, TimeUnit.MILLISECONDS);
                            }

                            if (next[i] == null) {
                                onGoing.setFalse();

                                return false;
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e, onGoing);
                        }

                        if (eHolder.value() != null) {
                            throwError(eHolder, onGoing);
                        }
                    }

                } else {
                    for (int i = 0; i < len; i++) {
                        if (next[i] == null) {
                            return false;
                        }
                    }
                }

                return true;
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                for (int i = 0; i < len; i++) {
                    if (next[i] == NONE) {
                        next[i] = null;
                    }
                }

                boolean isOK = false;

                try {
                    R result = zipFunction.apply(Arrays.asList((T[]) next));
                    next = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    // NO NEED.
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
    //            final BiFunction<? super A, ? super B, R> zipFunction) {
    //        return parallelZip(a, b, valueForNoneA, valueForNoneB, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 8
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
    //            final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
    //        return parallelZip(ImmutableIterator.of(a), ImmutableIterator.of(b), valueForNoneA, valueForNoneB, zipFunction, queueSize);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param valueForNoneC
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
    //            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
    //        return parallelZip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param valueForNoneC
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 8
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
    //            final TriFunction<? super A, ? super B, ? super C, R> zipFunction, final int queueSize) {
    //        return parallelZip(ImmutableIterator.of(a), ImmutableIterator.of(b), ImmutableIterator.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, queueSize);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final A valueForNoneA,
    //            final B valueForNoneB, final BiFunction<? super A, ? super B, R> zipFunction) {
    //        return parallelZip(a, b, valueForNoneA, valueForNoneB, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 8
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final A valueForNoneA,
    //            final B valueForNoneB, final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
    //        return parallelZip(a.iterator(), b.iterator(), valueForNoneA, valueForNoneB, zipFunction, queueSize);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param valueForNoneC
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
    //            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
    //        return parallelZip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param valueForNoneC
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 8
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
    //            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction,
    //            final int queueSize) {
    //        return parallelZip(a.iterator(), b.iterator(), c.iterator(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, queueSize);
    //    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return parallelZip(a, b, valueForNoneA, valueForNoneB, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
        final AtomicInteger threadCounterA = new AtomicInteger(1);
        final AtomicInteger threadCounterB = new AtomicInteger(1);
        final BlockingQueue<A> queueA = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<B> queueB = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        readToQueue(a, b, asyncExecutor, threadCounterA, threadCounterB, queueA, queueB, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            A nextA = null;
            B nextB = null;

            @Override
            public boolean hasNext() {
                if (nextA == null && nextB == null) {
                    try {
                        while (nextA == null && onGoing.value() && (threadCounterA.get() > 0 || queueA.size() > 0)) { // (threadCounterA.get() > 0 || queueA.size() > 0) is wrong. has to check counter first
                            nextA = queueA.poll(1, TimeUnit.MILLISECONDS);
                        }

                        while (nextB == null && onGoing.value() && (threadCounterB.get() > 0 || queueB.size() > 0)) { // (threadCounterB.get() > 0 || queueB.size() > 0) is wrong. has to check counter first
                            nextB = queueB.poll(1, TimeUnit.MILLISECONDS);
                        }
                    } catch (Throwable e) {
                        setError(eHolder, e, onGoing);
                    }

                    if (eHolder.value() != null) {
                        throwError(eHolder, onGoing);
                    }
                }

                if (nextA != null || nextB != null) {
                    return true;
                } else {
                    onGoing.setFalse();
                    return false;
                }
            }

            @Override
            public R next() {
                if ((nextA == null && nextB == null) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                nextA = nextA == NONE ? null : (nextA == null ? valueForNoneA : nextA);
                nextB = nextB == NONE ? null : (nextB == null ? valueForNoneB : nextB);
                boolean isOK = false;

                try {
                    final R result = zipFunction.apply(nextA, nextB);
                    nextA = null;
                    nextB = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return parallelZip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction,
            final int queueSize) {
        final AtomicInteger threadCounterA = new AtomicInteger(1);
        final AtomicInteger threadCounterB = new AtomicInteger(1);
        final AtomicInteger threadCounterC = new AtomicInteger(1);
        final BlockingQueue<A> queueA = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<B> queueB = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<C> queueC = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        readToQueue(a, b, c, asyncExecutor, threadCounterA, threadCounterB, threadCounterC, queueA, queueB, queueC, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            A nextA = null;
            B nextB = null;
            C nextC = null;

            @Override
            public boolean hasNext() {
                if (nextA == null && nextB == null && nextC == null) {
                    try {
                        while (nextA == null && onGoing.value() && (threadCounterA.get() > 0 || queueA.size() > 0)) { // (threadCounterA.get() > 0 || queueA.size() > 0) is wrong. has to check counter first
                            nextA = queueA.poll(1, TimeUnit.MILLISECONDS);
                        }

                        while (nextB == null && onGoing.value() && (threadCounterB.get() > 0 || queueB.size() > 0)) { // (threadCounterB.get() > 0 || queueB.size() > 0) is wrong. has to check counter first
                            nextB = queueB.poll(1, TimeUnit.MILLISECONDS);
                        }

                        while (nextC == null && onGoing.value() && (threadCounterC.get() > 0 || queueC.size() > 0)) { // (threadCounterC.get() > 0 || queueC.size() > 0) is wrong. has to check counter first
                            nextC = queueC.poll(1, TimeUnit.MILLISECONDS);
                        }
                    } catch (Throwable e) {
                        setError(eHolder, e, onGoing);
                    }

                    if (eHolder.value() != null) {
                        throwError(eHolder, onGoing);
                    }
                }

                if (nextA != null || nextB != null || nextC != null) {
                    return true;
                } else {
                    onGoing.setFalse();

                    return false;
                }
            }

            @Override
            public R next() {
                if ((nextA == null && nextB == null && nextC == null) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                nextA = nextA == NONE ? null : (nextA == null ? valueForNoneA : nextA);
                nextB = nextB == NONE ? null : (nextB == null ? valueForNoneB : nextB);
                nextC = nextC == NONE ? null : (nextC == null ? valueForNoneC : nextC);
                boolean isOK = false;

                try {
                    final R result = zipFunction.apply(nextA, nextB, nextC);
                    nextA = null;
                    nextB = null;
                    nextC = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return parallelZip(a, b, valueForNoneA, valueForNoneB, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
        return parallelZip(a.iterator(), b.iterator(), valueForNoneA, valueForNoneB, zipFunction, queueSize).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final Stream<C> c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return parallelZip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final Stream<C> c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction, final int queueSize) {
        return parallelZip(a.iterator(), b.iterator(), c.iterator(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, queueSize)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param valuesForNone
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> parallelZip(final Collection<? extends Stream<? extends T>> c, final Object[] valuesForNone,
            Function<? super List<? extends T>, R> zipFunction) {
        return parallelZip(c, valuesForNone, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param valuesForNone
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <T, R> Stream<R> parallelZip(final Collection<? extends Stream<? extends T>> c, final Object[] valuesForNone,
            Function<? super List<? extends T>, R> zipFunction, final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Stream<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return parallelZipp(iterList, valuesForNone, zipFunction, queueSize).onClose(newCloseHandler(c));
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param valuesForNone
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> parallelZipp(final Collection<? extends Iterator<? extends T>> c, final Object[] valuesForNone,
            Function<? super List<? extends T>, R> zipFunction) {
        return parallelZipp(c, valuesForNone, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param valuesForNone
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 8
     * @return
     */
    public static <T, R> Stream<R> parallelZipp(final Collection<? extends Iterator<? extends T>> c, final Object[] valuesForNone,
            final Function<? super List<? extends T>, R> zipFunction, final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        if (c.size() != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final int len = c.size();
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);
        final AtomicInteger[] counters = new AtomicInteger[len];
        final BlockingQueue<Object>[] queues = new ArrayBlockingQueue[len];

        readToQueue(c, queueSize, asyncExecutor, counters, queues, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            Object[] next = null;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = new Object[len];

                    for (int i = 0; i < len; i++) {
                        try {
                            while (next[i] == null && onGoing.value() && (counters[i].get() > 0 || queues[i].size() > 0)) { // (counters[i].get() > 0 || queues[i].size() > 0) is wrong. has to check counter first
                                next[i] = queues[i].poll(1, TimeUnit.MILLISECONDS);
                            }
                        } catch (Throwable e) {
                            setError(eHolder, e, onGoing);
                        }

                        if (eHolder.value() != null) {
                            throwError(eHolder, onGoing);
                        }
                    }
                }

                for (int i = 0; i < len; i++) {
                    if (next[i] != null) {
                        return true;
                    }
                }

                onGoing.setFalse();
                return false;
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                for (int i = 0; i < len; i++) {
                    next[i] = next[i] == NONE ? null : (next[i] == null ? valuesForNone[i] : next[i]);
                }

                boolean isOK = false;
                try {
                    R result = zipFunction.apply(Arrays.asList((T[]) next));
                    next = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    //    /**
    //     * 
    //     * @param c
    //     * @param unzip the second parameter is an output parameter.
    //     * @return
    //     */
    //    public static <T, L, R> Pair<Stream<L>, Stream<R>> unzip(final Collection<? extends T> c, final BiConsumer<? super T, Pair<L, R>> unzip) {
    //        final Pair<List<L>, List<R>> p = Seq.unzip(c, unzip);
    //
    //        return Pair.of(Stream.of(p.left), Stream.of(p.right));
    //    }
    //
    //    /**
    //     * 
    //     * @param iter
    //     * @param unzip the second parameter is an output parameter.
    //     * @return
    //     */
    //    public static <T, L, R> Pair<Stream<L>, Stream<R>> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<L, R>> unzip) {
    //        final Pair<List<L>, List<R>> p = Iterators.unzip(iter, unzip);
    //
    //        return Pair.of(Stream.of(p.left), Stream.of(p.right));
    //    }
    //
    //    /**
    //     * 
    //     * @param c
    //     * @param unzip the second parameter is an output parameter.
    //     * @return
    //     */
    //    public static <T, L, M, R> Triple<Stream<L>, Stream<M>, Stream<R>> unzipp(final Collection<? extends T> c,
    //            final BiConsumer<? super T, Triple<L, M, R>> unzip) {
    //        final Triple<List<L>, List<M>, List<R>> p = Seq.unzipp(c, unzip);
    //
    //        return Triple.of(Stream.of(p.left), Stream.of(p.middle), Stream.of(p.right));
    //    }
    //
    //    /**
    //     * 
    //     * @param iter
    //     * @param unzip the second parameter is an output parameter.
    //     * @return
    //     */
    //    public static <T, L, M, R> Triple<Stream<L>, Stream<M>, Stream<R>> unzipp(final Iterator<? extends T> iter,
    //            final BiConsumer<? super T, Triple<L, M, R>> unzip) {
    //        final Triple<List<L>, List<M>, List<R>> p = Iterators.unzipp(iter, unzip);
    //
    //        return Triple.of(Stream.of(p.left), Stream.of(p.middle), Stream.of(p.right));
    //    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final T[] a, final T[] b, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        if (N.isNullOrEmpty(a)) {
            return of(b);
        } else if (N.isNullOrEmpty(b)) {
            return of(a);
        }

        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public T next() {
                if (cursorA < lenA) {
                    if (cursorB < lenB) {
                        if (nextSelector.apply(a[cursorA], b[cursorB]) == Nth.FIRST) {
                            return a[cursorA++];
                        } else {
                            return b[cursorB++];
                        }
                    } else {
                        return a[cursorA++];
                    }
                } else if (cursorB < lenB) {
                    return b[cursorB++];
                } else {
                    throw new NoSuchElementException();
                }
            }
        });
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final T[] a, final T[] b, final T[] c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iterator(), Stream.of(c).iterator(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Collection<? extends T> a, final Collection<? extends T> b,
            final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return merge(a.iterator(), b.iterator(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Collection<? extends T> a, final Collection<? extends T> b, final Collection<? extends T> c,
            final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return merge(a.iterator(), b.iterator(), c.iterator(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        if (a.hasNext() == false) {
            return of(b);
        } else if (b.hasNext() == false) {
            return of(a);
        }

        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private T nextA = null;
            private T nextB = null;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return hasNextA || hasNextB || a.hasNext() || b.hasNext();
            }

            @Override
            public T next() {
                if (hasNextA) {
                    if (b.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = b.next())) == Nth.FIRST) {
                            hasNextA = false;
                            hasNextB = true;
                            return nextA;
                        } else {
                            return nextB;
                        }
                    } else {
                        hasNextA = false;
                        return nextA;
                    }
                } else if (hasNextB) {
                    if (a.hasNext()) {
                        if (nextSelector.apply((nextA = a.next()), nextB) == Nth.FIRST) {
                            return nextA;
                        } else {
                            hasNextA = true;
                            hasNextB = false;
                            return nextB;
                        }
                    } else {
                        hasNextB = false;
                        return nextB;
                    }
                } else if (a.hasNext()) {
                    if (b.hasNext()) {
                        if (nextSelector.apply((nextA = a.next()), (nextB = b.next())) == Nth.FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return a.next();
                    }
                } else if (b.hasNext()) {
                    return b.next();
                } else {
                    throw new NoSuchElementException();
                }
            }
        });
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b, final Iterator<? extends T> c,
            final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iterator(), c, nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Stream<? extends T> a, final Stream<? extends T> b, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return merge(a.iterator(), b.iterator(), nextSelector).onClose(newCloseHandler(N.asList(a, b)));
    }

    public static <T> Stream<T> merge(final Stream<? extends T> a, final Stream<? extends T> b, final Stream<? extends T> c,
            final BiFunction<? super T, ? super T, Nth> nextSelector) {

        return merge(a.iterator(), b.iterator(), c.iterator(), nextSelector).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Collection<? extends Stream<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        N.requireNonNull(nextSelector);

        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return (Stream<T>) c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends Stream<? extends T>> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(c.size());

        for (Stream<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return mergge(iterList, nextSelector).onClose(newCloseHandler(c));
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> mergge(final Collection<? extends Iterator<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        N.requireNonNull(nextSelector);

        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return of(c.iterator().next());
        } else if (c.size() == 2) {
            final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
        Stream<T> result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result.iterator(), iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> parallelMerge(final Collection<? extends Stream<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return parallelMerge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @param maxThreadNum
     * @return
     */
    public static <T> Stream<T> parallelMerge(final Collection<? extends Stream<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector,
            final int maxThreadNum) {
        N.requireNonNull(nextSelector);
        checkMaxThreadNum(maxThreadNum);

        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return (Stream<T>) c.iterator().next();
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(c.size());

        for (Stream<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return parallelMergge(iterList, nextSelector, maxThreadNum).onClose(newCloseHandler(c));
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> parallelMergge(final Collection<? extends Iterator<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return parallelMergge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @param maxThreadNum
     * @return
     */
    public static <T> Stream<T> parallelMergge(final Collection<? extends Iterator<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector,
            final int maxThreadNum) {
        checkMaxThreadNum(maxThreadNum);

        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return of(c.iterator().next());
        } else if (c.size() == 2) {
            final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
            final Iterator<? extends T> a = iter.next();
            final Iterator<? extends T> b = iter.next();
            return merge(a instanceof QueuedIterator ? a : Stream.of(a).queued().iterator(), b instanceof QueuedIterator ? b : Stream.of(b).queued().iterator(),
                    nextSelector);
        }

        final Queue<Iterator<? extends T>> queue = N.newLinkedList(c);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt cnt = MutableInt.of(c.size());
        final List<CompletableFuture<Void>> futureList = new ArrayList<>(c.size() - 1);

        for (int i = 0, n = N.min(maxThreadNum, c.size() / 2 + 1); i < n; i++) {
            futureList.add(asyncExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Iterator<? extends T> a = null;
                    Iterator<? extends T> b = null;
                    Iterator<? extends T> c = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (queue) {
                                if (cnt.intValue() > 2 && queue.size() > 1) {
                                    a = queue.poll();
                                    b = queue.poll();

                                    cnt.decrement();
                                } else {
                                    break;
                                }
                            }

                            c = (Iterator<? extends T>) ObjIteratorEx.of(merge(a instanceof QueuedIterator ? a : Stream.of(a).queued().iterator(),
                                    b instanceof QueuedIterator ? b : Stream.of(b).queued().iterator(), nextSelector).toArray());

                            synchronized (queue) {
                                queue.offer(c);
                            }
                        }
                    } catch (Throwable e) {
                        setError(eHolder, e);
                    }
                }
            }));
        }

        complete(futureList, eHolder);

        // Should never happen.
        if (queue.size() != 2) {
            throw new AbacusException("Unknown error happened.");
        }

        final Iterator<? extends T> a = queue.poll();
        final Iterator<? extends T> b = queue.poll();

        return merge(a instanceof QueuedIterator ? a : Stream.of(a).queued().iterator(), b instanceof QueuedIterator ? b : Stream.of(b).queued().iterator(),
                nextSelector);
    }

    private static <B, A> void readToQueue(final Iterator<? extends A> a, final Iterator<? extends B> b, final AsyncExecutor asyncExecutor,
            final AtomicInteger threadCounterA, final AtomicInteger threadCounterB, final BlockingQueue<A> queueA, final BlockingQueue<B> queueB,
            final Holder<Throwable> eHolder, final MutableBoolean onGoing) {
        asyncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    A nextA = null;

                    while (onGoing.value() && a.hasNext()) {
                        nextA = a.next();

                        if (nextA == null) {
                            nextA = (A) NONE;
                        }

                        while (onGoing.value() && queueA.offer(nextA, 100, TimeUnit.MILLISECONDS) == false) {
                            // continue
                        }
                    }
                } catch (Throwable e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    threadCounterA.decrementAndGet();
                }
            }
        });

        asyncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    B nextB = null;

                    while (onGoing.value() && b.hasNext()) {
                        nextB = b.next();

                        if (nextB == null) {
                            nextB = (B) NONE;
                        }

                        while (onGoing.value() && queueB.offer(nextB, 100, TimeUnit.MILLISECONDS) == false) {
                            // continue
                        }
                    }
                } catch (Throwable e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    threadCounterB.decrementAndGet();
                }
            }
        });
    }

    private static <B, C, A> void readToQueue(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final AsyncExecutor asyncExecutor, final AtomicInteger threadCounterA, final AtomicInteger threadCounterB, final AtomicInteger threadCounterC,
            final BlockingQueue<A> queueA, final BlockingQueue<B> queueB, final BlockingQueue<C> queueC, final Holder<Throwable> eHolder,
            final MutableBoolean onGoing) {
        asyncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    A nextA = null;

                    while (onGoing.value() && a.hasNext()) {
                        nextA = a.next();

                        if (nextA == null) {
                            nextA = (A) NONE;
                        }

                        while (onGoing.value() && queueA.offer(nextA, 100, TimeUnit.MILLISECONDS) == false) {
                            // continue
                        }
                    }
                } catch (Throwable e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    threadCounterA.decrementAndGet();
                }
            }
        });

        asyncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    B nextB = null;

                    while (onGoing.value() && b.hasNext()) {
                        nextB = b.next();

                        if (nextB == null) {
                            nextB = (B) NONE;
                        }

                        while (onGoing.value() && queueB.offer(nextB, 100, TimeUnit.MILLISECONDS) == false) {
                            // continue
                        }
                    }
                } catch (Throwable e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    threadCounterB.decrementAndGet();
                }
            }
        });

        asyncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    C nextC = null;

                    while (onGoing.value() && c.hasNext()) {
                        nextC = c.next();

                        if (nextC == null) {
                            nextC = (C) NONE;
                        }

                        while (onGoing.value() && queueC.offer(nextC, 100, TimeUnit.MILLISECONDS) == false) {
                            // continue
                        }
                    }
                } catch (Throwable e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    threadCounterC.decrementAndGet();
                }
            }
        });
    }

    private static void readToQueue(final Collection<? extends Iterator<?>> c, final int queueSize, final AsyncExecutor asyncExecutor,
            final AtomicInteger[] counters, final BlockingQueue<Object>[] queues, final Holder<Throwable> eHolder, final MutableBoolean onGoing) {
        int idx = 0;

        for (Iterator<?> e : c) {
            counters[idx] = new AtomicInteger(1);
            queues[idx] = new ArrayBlockingQueue<>(queueSize);

            final Iterator<?> iter = e;
            final AtomicInteger count = counters[idx];
            final BlockingQueue<Object> queue = queues[idx];

            asyncExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Object next = null;

                        while (onGoing.value() && iter.hasNext()) {
                            next = iter.next();

                            if (next == null) {
                                next = NONE;
                            }

                            while (onGoing.value() && queue.offer(next, 100, TimeUnit.MILLISECONDS) == false) {
                                // continue
                            }
                        }
                    } catch (Throwable e) {
                        setError(eHolder, e, onGoing);
                    } finally {
                        count.decrementAndGet();
                    }
                }
            });

            idx++;
        }
    }

    public static abstract class StreamEx<T> extends Stream<T> {
        private StreamEx(boolean sorted, Comparator<? super T> cmp, Collection<Runnable> closeHandlers) {
            super(sorted, cmp, closeHandlers);
            // Factory class.
        }
    }

    /**
     * SOO = Sequential Only Operations.
     * These operations run sequentially only, even under parallel stream.
     * 
     * Mostly, if an operation is a single operation, or has to be executed in order,
     *  or there is no benefit to execute it in parallel, it will run sequentially, even under parallel stream.
     *
     */
    public static enum SOO {
        SPLIT, SPLIT_TO_LIST, SPLIT_TO_SET, SPLIT_AT, SLIDING, //
        INTERSECTION, DIFFERENCE, SYMMETRIC_DIFFERENCE, //
        REVERSED, SHUFFLED, ROTATED, DISTINCT, HAS_DUPLICATE, //
        APPEND, PREPEND, CACHED, INDEXED, SKIP, SKIP_LAST, LIMIT, STEP, //
        QUEUED, MERGE, ZIP_WITH, PERSIST_FILE_OUTPUT_STREAM_WRITE, //
        COMBINATIONS, PERMUTATIONS, ORDERED_PERMUTATIONS, DISTRIBUTION, //
        CARTESIAN_PRODUCT, INNER_JOIN, FULL_JOIN, LEFT_JOIN, RIGHT_JOIN, //
        COLLAPSE, RANGE_MAP, SCAN, INTERSPERSE, TOP, K_TH_LARGEST, FOR_EACH_WITH_RESULT, //
        COUNT, FIND_FIRST_OR_LAST, FIND_FIRST_AND_LAST, //
        LAST, HEAD, HEAD_2, TAIL, TAIL_2, HEAD_AND_TAIL, HEAD_AND_TAIL_2, //
        TO_ARRAY, TO_LIST, TO_SET, TO_MULTISET, TO_LONG_MULTISET, TO_MATRIX, TO_DATA_SET, //
        BOXED, ITERATOR, AS_INT_STREAM, AS_LONG_STREAM, AS_FLOAT_STREAM, AS_DOUBLE_STREAM, //
        PRINTLN, IS_PARALLEL, SEQUENTIAL, PARALLEL, MAX_THREAD_NUM, SPLITOR, TRIED, PERSIST_FILE, ON_CLOSE, CLOSE;
    }

    /**
     * PSO = Parallel supported Operations.
     * These operations run in parallel under parallel stream.
     * 
     * Mostly, if an operation can be executed in parallel and has benefit to execute it in parallel. it will run in parallel under parallel stream.
     * 
     * @author haiyangl
     *
     */
    public static enum PSO {
        MAP, BI_MAP, TRI_MAP, SLIDING_MAP, MAP_TO_ENTRY, MAP_TO_, MAP_FIRST, MAP_FIRST_, MAP_LAST, MAP_LAST_, RANGE_MAP, //
        FLAT_MAP, FLAT_MAP_TO_, FLAT_ARRAY, FLAT_COLLECION, //
        FILTER, TAKE_WHILE, DROP_WHILE, REMOVE, REMOVE_IF, REMOVE_WHILE, SKIP_NULL, //
        SPLIT_BY, SORTED, REVERSE_SORTED, DISTINCT_BY, JOIN, PEEK, //
        GROUP_BY, GROUP_BY_TO_ENTRY, GROUP_TO, TO_MAP, TO_MULTIMAP, //
        MIN, MAX, SUM_INT, SUM_LONG, SUM_DOUBLE, AVERAGE_INT, AVERAGE_LONG, AVERAGE_DOUBLE, SUMMARIZE_, //
        FOR_EACH, FOR_EACH_PAIR, FOR_EACH_TRIPLE, ANY_MATCH, ALL_MATCH, NONE_MATCH, FIND_FIRST, FIND_LAST, FIND_ANY, //
        REDUCE, COLLECT, PERSIST_DB;
    }

    /**
     * LAIO = Loading All Intermediate Operations.
     * 
     * Intermediate operations which will load or go through all the elements in the stream.
     * 
     * @author haiyangl
     *
     */
    public static enum LAIO {
        SORTED, SORTED_BY, REVERSE_SORTED, CACHED, REVERSED, SHUFFLED, ROTATED, //
        GROUP_BY, GROUP_BY_TO_ENTRY, //
        HEAD_2, TAIL_2, HEAD_AND_TAIL_2;
    }
}
