package com.landawn.abacus.util.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.OptionalByte;
import com.landawn.abacus.util.OptionalDouble;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteToIntFunction;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.function.ObjByteConsumer;
import com.landawn.abacus.util.function.Supplier;

/**
 * This class is a sequential, stateful and immutable stream implementation.
 *
 */
final class ByteStreamImpl extends ByteStream {
    private final byte[] elements;
    private final int fromIndex;
    private final int toIndex;
    private final boolean sorted;
    private final Set<Runnable> closeHandlers;

    ByteStreamImpl(byte[] values) {
        this(values, null);
    }

    ByteStreamImpl(byte[] values, Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    ByteStreamImpl(byte[] values, boolean sorted, Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, sorted, closeHandlers);
    }

    ByteStreamImpl(byte[] values, int fromIndex, int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    ByteStreamImpl(byte[] values, int fromIndex, int toIndex, Collection<Runnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, closeHandlers);
    }

    ByteStreamImpl(byte[] values, int fromIndex, int toIndex, boolean sorted, Collection<Runnable> closeHandlers) {
        if (fromIndex < 0 || toIndex < fromIndex || toIndex > values.length) {
            throw new IllegalArgumentException("Invalid fromIndex(" + fromIndex + ") or toIndex(" + toIndex + ")");
        }

        this.elements = values;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.sorted = sorted;
        this.closeHandlers = N.isNullOrEmpty(closeHandlers) ? null : new LinkedHashSet<>(closeHandlers);
    }

    @Override
    public ByteStream filter(BytePredicate predicate) {
        return filter(predicate, Integer.MAX_VALUE);
    }

    @Override
    public ByteStream filter(final BytePredicate predicate, final int max) {
        return new ByteStreamImpl(N.filter(elements, fromIndex, toIndex, predicate, max), sorted, closeHandlers);
    }

    @Override
    public ByteStream takeWhile(BytePredicate predicate) {
        return takeWhile(predicate, Integer.MAX_VALUE);
    }

    @Override
    public ByteStream takeWhile(BytePredicate predicate, int max) {
        final ByteList list = ByteList.of(new byte[N.min(9, max, (toIndex - fromIndex))], 0);

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (predicate.test(elements[i])) {
                list.add(elements[i]);
                cnt++;
            } else {
                break;
            }
        }

        return new ByteStreamImpl(list.trimToSize().array(), sorted, closeHandlers);
    }

    @Override
    public ByteStream dropWhile(BytePredicate predicate) {
        return dropWhile(predicate, Integer.MAX_VALUE);
    }

    @Override
    public ByteStream dropWhile(BytePredicate predicate, int max) {
        int index = fromIndex;
        while (index < toIndex && predicate.test(elements[index])) {
            index++;
        }

        final ByteList list = ByteList.of(new byte[N.min(9, max, (toIndex - index))], 0);
        int cnt = 0;

        while (index < toIndex && cnt < max) {
            list.add(elements[index]);
            index++;
            cnt++;
        }

        return new ByteStreamImpl(list.trimToSize().array(), sorted, closeHandlers);
    }

    @Override
    public ByteStream map(ByteUnaryOperator mapper) {
        final byte[] a = new byte[toIndex - fromIndex];

        for (int i = fromIndex, j = 0; i < toIndex; i++, j++) {
            a[j] = mapper.applyAsByte(elements[i]);
        }

        return new ByteStreamImpl(a, closeHandlers);
    }

    @Override
    public IntStream mapToInt(ByteToIntFunction mapper) {
        final int[] a = new int[toIndex - fromIndex];

        for (int i = fromIndex, j = 0; i < toIndex; i++, j++) {
            a[j] = mapper.applyAsInt(elements[i]);
        }

        return new IntStreamImpl(a, closeHandlers);
    }

    @Override
    public <U> Stream<U> mapToObj(final ByteFunction<? extends U> mapper) {
        //        final Object[] a = new Object[toIndex - fromIndex];
        //
        //        for (int i = fromIndex, j = 0; i < toIndex; i++, j++) {
        //            a[j] = mapper.apply(elements[i]);
        //        }
        //
        //        return new ArrayStream<U>((U[]) a, closeHandlers);

        return new IteratorStream<U>(new Iterator<U>() {
            int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public U next() {
                return mapper.apply(elements[cursor++]);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }, closeHandlers);
    }

    @Override
    public ByteStream flatMap(ByteFunction<? extends ByteStream> mapper) {
        final List<byte[]> listOfArray = new ArrayList<byte[]>();

        int lengthOfAll = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            final byte[] tmp = mapper.apply(elements[i]).toArray();
            lengthOfAll += tmp.length;
            listOfArray.add(tmp);
        }

        final byte[] arrayOfAll = new byte[lengthOfAll];
        int from = 0;
        for (byte[] tmp : listOfArray) {
            N.copy(tmp, 0, arrayOfAll, from, tmp.length);
            from += tmp.length;
        }

        return new ByteStreamImpl(arrayOfAll, closeHandlers);
    }

    @Override
    public IntStream flatMapToInt(ByteFunction<? extends IntStream> mapper) {
        final List<int[]> listOfArray = new ArrayList<int[]>();

        int lengthOfAll = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            final int[] tmp = mapper.apply(elements[i]).toArray();
            lengthOfAll += tmp.length;
            listOfArray.add(tmp);
        }

        final int[] arrayOfAll = new int[lengthOfAll];
        int from = 0;
        for (int[] tmp : listOfArray) {
            N.copy(tmp, 0, arrayOfAll, from, tmp.length);
            from += tmp.length;
        }

        return new IntStreamImpl(arrayOfAll, closeHandlers);
    }

    @Override
    public <T> Stream<T> flatMapToObj(final ByteFunction<? extends Stream<T>> mapper) {
        //        final List<Object[]> listOfArray = new ArrayList<Object[]>();
        //        int lengthOfAll = 0;
        //
        //        for (int i = fromIndex; i < toIndex; i++) {
        //            final Object[] tmp = mapper.apply(elements[i]).toArray();
        //            lengthOfAll += tmp.length;
        //            listOfArray.add(tmp);
        //        }
        //
        //        final Object[] arrayOfAll = new Object[lengthOfAll];
        //        int from = 0;
        //
        //        for (Object[] tmp : listOfArray) {
        //            N.copy(tmp, 0, arrayOfAll, from, tmp.length);
        //            from += tmp.length;
        //        }
        //
        //        return new ArrayStream<T>((T[]) arrayOfAll, closeHandlers);

        return new IteratorStream<T>(new Iterator<T>() {
            private int cursor = fromIndex;
            private Iterator<? extends T> cur = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    cur = mapper.apply(elements[cursor++]).iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                return cur.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        }, closeHandlers);
    }

    @Override
    public ByteStream distinct() {
        return new ByteStreamImpl(N.removeDuplicates(elements, fromIndex, toIndex, sorted), sorted, closeHandlers);
    }

    @Override
    public ByteStream sorted() {
        if (sorted) {
            return new ByteStreamImpl(elements, fromIndex, toIndex, sorted, closeHandlers);
        }

        final byte[] a = N.copyOfRange(elements, fromIndex, toIndex);
        N.sort(a);
        return new ByteStreamImpl(a, true, closeHandlers);
    }

    @Override
    public ByteStream peek(ByteConsumer action) {
        for (int i = fromIndex; i < toIndex; i++) {
            action.accept(elements[i]);
        }

        // return new ByteStreamImpl(values, fromIndex, toIndex, sorted, closeHandlers);
        return this;
    }

    @Override
    public ByteStream limit(long maxSize) {
        if (maxSize >= toIndex - fromIndex) {
            return new ByteStreamImpl(elements, fromIndex, toIndex, sorted, closeHandlers);
        } else {
            return new ByteStreamImpl(elements, fromIndex, (int) (fromIndex + maxSize), sorted, closeHandlers);
        }
    }

    @Override
    public ByteStream skip(long n) {
        if (n >= toIndex - fromIndex) {
            return new ByteStreamImpl(N.EMPTY_BYTE_ARRAY, sorted, closeHandlers);
        } else {
            return new ByteStreamImpl(elements, (int) (fromIndex + n), toIndex, sorted, closeHandlers);
        }
    }

    @Override
    public void forEach(ByteConsumer action) {
        for (int i = fromIndex; i < toIndex; i++) {
            action.accept(elements[i]);
        }
    }

    @Override
    public byte[] toArray() {
        return N.copyOfRange(elements, fromIndex, toIndex);
    }

    @Override
    public ByteList toByteList() {
        return ByteList.of(N.copyOfRange(elements, fromIndex, toIndex));
    }

    @Override
    public byte reduce(byte identity, ByteBinaryOperator op) {
        byte result = identity;

        for (int i = fromIndex; i < toIndex; i++) {
            result = op.applyAsByte(result, elements[i]);
        }

        return result;
    }

    @Override
    public OptionalByte reduce(ByteBinaryOperator op) {
        if (count() == 0) {
            return OptionalByte.empty();
        }

        byte result = elements[fromIndex];

        for (int i = fromIndex + 1; i < toIndex; i++) {
            result = op.applyAsByte(result, elements[i]);
        }

        return OptionalByte.of(result);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjByteConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        final R result = supplier.get();

        for (int i = fromIndex; i < toIndex; i++) {
            accumulator.accept(result, elements[i]);
        }

        return result;
    }

    @Override
    public long sum() {
        return N.sum(elements, fromIndex, toIndex).longValue();
    }

    @Override
    public OptionalByte min() {
        if (count() == 0) {
            return OptionalByte.empty();
        }

        return OptionalByte.of(N.min(elements, fromIndex, toIndex));
    }

    @Override
    public OptionalByte max() {
        if (count() == 0) {
            return OptionalByte.empty();
        }

        return OptionalByte.of(N.max(elements, fromIndex, toIndex));
    }

    @Override
    public long count() {
        return toIndex - fromIndex;
    }

    @Override
    public OptionalDouble average() {
        if (count() == 0) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.avg(elements, fromIndex, toIndex).doubleValue());
    }

    @Override
    public boolean anyMatch(BytePredicate predicate) {
        for (int i = fromIndex; i < toIndex; i++) {
            if (predicate.test(elements[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean allMatch(BytePredicate predicate) {
        for (int i = fromIndex; i < toIndex; i++) {
            if (predicate.test(elements[i]) == false) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean noneMatch(BytePredicate predicate) {
        for (int i = fromIndex; i < toIndex; i++) {
            if (predicate.test(elements[i])) {
                return false;
            }
        }

        return true;
    }

    @Override
    public OptionalByte findFirst() {
        return count() == 0 ? OptionalByte.empty() : OptionalByte.of(elements[fromIndex]);
    }

    @Override
    public OptionalByte findAny() {
        return count() == 0 ? OptionalByte.empty() : OptionalByte.of(elements[fromIndex]);
    }

    @Override
    public IntStream asIntStream() {
        final int[] a = new int[toIndex - fromIndex];

        for (int i = fromIndex, j = 0; i < toIndex; i++, j++) {
            a[j] = elements[i];
        }

        return new IntStreamImpl(a, sorted, closeHandlers);
    }

    @Override
    public Stream<Byte> boxed() {
        return new ArrayStream<Byte>(Array.box(elements, fromIndex, toIndex), closeHandlers);
    }

    @Override
    public Iterator<Byte> iterator() {
        return new ByteIterator(elements, fromIndex, toIndex);
    }

    @Override
    public ByteStream onClose(Runnable closeHandler) {
        final List<Runnable> closeHandlerList = new ArrayList<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        if (N.notNullOrEmpty(this.closeHandlers)) {
            closeHandlerList.addAll(this.closeHandlers);
        }

        closeHandlerList.add(closeHandler);

        return new ByteStreamImpl(elements, fromIndex, toIndex, closeHandlerList);
    }

    @Override
    public void close() {
        if (N.notNullOrEmpty(closeHandlers)) {
            RuntimeException ex = null;

            for (Runnable closeHandler : closeHandlers) {
                try {
                    closeHandler.run();
                } catch (RuntimeException e) {
                    if (ex == null) {
                        ex = e;
                    } else {
                        ex.addSuppressed(e);
                    }
                }
            }

            if (ex != null) {
                throw ex;
            }
        }
    }

    static class ByteIterator extends ImmutableIterator<Byte> {
        private final byte[] values;
        private final int toIndex;
        private int cursor;

        ByteIterator(byte[] array, int fromIndex, int toIndex) {
            this.values = array;
            this.toIndex = toIndex;
            this.cursor = fromIndex;
        }

        @Override
        public boolean hasNext() {
            return cursor < toIndex;
        }

        @Override
        public Byte next() {
            return values[cursor++];
        }
    }
}
