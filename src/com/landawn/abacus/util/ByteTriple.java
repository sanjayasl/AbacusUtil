package com.landawn.abacus.util;

import com.landawn.abacus.util.stream.Stream;

public class ByteTriple {
    public final byte _1;
    public final byte _2;
    public final byte _3;

    ByteTriple() {
        this((byte) 0, (byte) 0, (byte) 0);
    }

    ByteTriple(byte _1, byte _2, byte _3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    public static ByteTriple of(byte _1, byte _2, byte _3) {
        return new ByteTriple(_1, _2, _3);
    }

    public static ByteTriple from(final byte[] a) {
        if (N.isNullOrEmpty(a)) {
            return new ByteTriple();
        } else if (a.length == 1) {
            return new ByteTriple(a[0], (byte) 0, (byte) 0);
        } else if (a.length == 2) {
            return new ByteTriple(a[0], a[1], (byte) 0);
        } else {
            return new ByteTriple(a[0], a[1], a[2]);
        }
    }

    public byte min() {
        return N.min(_1, _2, _3);
    }

    public byte max() {
        return N.max(_1, _2, _3);
    }

    public byte median() {
        return N.median(_1, _2, _3);
    }

    public int sum() {
        return _1 + _2 + _3;
    }

    public double average() {
        return (0d + _1 + _2 + _3) / 3;
    }

    public ByteTriple reversed() {
        return new ByteTriple(_3, _2, _1);
    }

    public byte[] toArray() {
        return new byte[] { _1, _2, _3 };
    }

    public ByteList toList() {
        return ByteList.of(_1, _2, _3);
    }

    public <E extends Exception> void forEach(Try.ByteConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
        comsumer.accept(this._3);
    }

    public <E extends Exception> void accept(Try.Consumer<ByteTriple, E> action) throws E {
        action.accept(this);
    }

    public <U, E extends Exception> U map(Try.Function<ByteTriple, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    public <E extends Exception> Optional<ByteTriple> filter(final Try.Predicate<ByteTriple, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<ByteTriple> empty();
    }

    public Stream<ByteTriple> stream() {
        return Stream.of(this);
    }

    @Override
    public int hashCode() {
        return (31 * (31 * _1 + this._2)) + _3;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ByteTriple)) {
            return false;
        } else {
            ByteTriple other = (ByteTriple) obj;
            return this._1 == other._1 && this._2 == other._2 && this._3 == other._3;
        }
    }

    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + ", " + this._3 + "]";
    }
}
