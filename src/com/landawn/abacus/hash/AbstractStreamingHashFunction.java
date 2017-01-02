/*
 * Copyright (C) 2011 The Guava Authors
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

package com.landawn.abacus.hash;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.BiConsumer;

/**
 * Note: It's copied from Google Guava under Apache License 2.0
 * 
 * Skeleton implementation of {@link HashFunction}. Provides default implementations which invokes
 * the appropriate method on {@link #newHasher()}, then return the result of {@link Hasher#hash}.
 *
 * <p>Invocations of {@link #newHasher(int)} also delegate to {@linkplain #newHasher()}, ignoring
 * the expected input size parameter.
 *
 * @author Kevin Bourrillion
 */
abstract class AbstractStreamingHashFunction implements HashFunction {
    @Override
    public <T> HashCode hash(T instance, BiConsumer<? super T, ? super Hasher> funnel) {
        return newHasher().put(instance, funnel).hash();
    }

    @Override
    public HashCode hash(CharSequence input) {
        return newHasher().put(input).hash();
    }

    @Override
    public HashCode hash(CharSequence input, Charset charset) {
        return newHasher().put(input, charset).hash();
    }

    //    @Override
    //    public HashCode hash(boolean input) {
    //        return hash(input == false ? 0 : 1);
    //    }

    @Override
    public HashCode hash(int input) {
        return newHasher().put(input).hash();
    }

    @Override
    public HashCode hash(long input) {
        return newHasher().put(input).hash();
    }

    //    @Override
    //    public HashCode hash(float input) {
    //        return newHasher().put(input).hash();
    //    }
    //
    //    @Override
    //    public HashCode hash(double input) {
    //        return newHasher().put(input).hash();
    //    }

    @Override
    public HashCode hash(byte[] input) {
        return newHasher().put(input).hash();
    }

    @Override
    public HashCode hash(byte[] input, int off, int len) {
        return newHasher().put(input, off, len).hash();
    }

    @Override
    public Hasher newHasher(int expectedInputSize) {
        N.checkArgument(expectedInputSize >= 0);
        return newHasher();
    }

    /**
     * A convenience base class for implementors of {@code Hasher}; handles accumulating data until an
     * entire "chunk" (of implementation-dependent length) is ready to be hashed.
     *
     * @author Kevin Bourrillion
     * @author Dimitris Andreou
     */
    // TODO(kevinb): this class still needs some design-and-document-for-inheritance love

    protected static abstract class AbstractStreamingHasher extends AbstractHasher {
        /** Buffer via which we pass data to the hash algorithm (the implementor) */
        private final ByteBuffer buffer;

        /** Number of bytes to be filled before process() invocation(s). */
        private final int bufferSize;

        /** Number of bytes processed per process() invocation. */
        private final int chunkSize;

        /**
         * Constructor for use by subclasses. This hasher instance will process chunks of the specified
         * size.
         *
         * @param chunkSize the number of bytes available per {@link #process(ByteBuffer)} invocation;
         *     must be at least 4
         */
        protected AbstractStreamingHasher(int chunkSize) {
            this(chunkSize, chunkSize);
        }

        /**
         * Constructor for use by subclasses. This hasher instance will process chunks of the specified
         * size, using an internal buffer of {@code bufferSize} size, which must be a multiple of
         * {@code chunkSize}.
         *
         * @param chunkSize the number of bytes available per {@link #process(ByteBuffer)} invocation;
         *     must be at least 4
         * @param bufferSize the size of the internal buffer. Must be a multiple of chunkSize
         */
        protected AbstractStreamingHasher(int chunkSize, int bufferSize) {
            // TODO(kevinb): check more preconditions (as bufferSize >= chunkSize) if this is ever public
            N.checkArgument(bufferSize % chunkSize == 0);

            // TODO(user): benchmark performance difference with longer buffer
            // always space for a single primitive
            this.buffer = ByteBuffer.allocate(bufferSize + 7).order(ByteOrder.LITTLE_ENDIAN);
            this.bufferSize = bufferSize;
            this.chunkSize = chunkSize;
        }

        /**
         * Processes the available bytes of the buffer (at most {@code chunk} bytes).
         */
        protected abstract void process(ByteBuffer bb);

        /**
         * This is invoked for the last bytes of the input, which are not enough to fill a whole chunk.
         * The passed {@code ByteBuffer} is guaranteed to be non-empty.
         *
         * <p>This implementation simply pads with zeros and delegates to {@link #process(ByteBuffer)}.
         */
        protected void processRemaining(ByteBuffer bb) {
            bb.position(bb.limit()); // move at the end
            bb.limit(chunkSize + 7); // get ready to pad with longs
            while (bb.position() < chunkSize) {
                bb.putLong(0);
            }
            bb.limit(chunkSize);
            bb.flip();
            process(bb);
        }

        @Override
        public final Hasher put(byte[] bytes) {
            return put(bytes, 0, bytes.length);
        }

        @Override
        public final Hasher put(byte[] bytes, int off, int len) {
            return putBytes(ByteBuffer.wrap(bytes, off, len).order(ByteOrder.LITTLE_ENDIAN));
        }

        private Hasher putBytes(ByteBuffer readBuffer) {
            // If we have room for all of it, this is easy
            if (readBuffer.remaining() <= buffer.remaining()) {
                buffer.put(readBuffer);
                munchIfFull();
                return this;
            }

            // First add just enough to fill buffer size, and munch that
            int bytesToCopy = bufferSize - buffer.position();
            for (int i = 0; i < bytesToCopy; i++) {
                buffer.put(readBuffer.get());
            }
            munch(); // buffer becomes empty here, since chunkSize divides bufferSize

            // Now process directly from the rest of the input buffer
            while (readBuffer.remaining() >= chunkSize) {
                process(readBuffer);
            }

            // Finally stick the remainder back in our usual buffer
            buffer.put(readBuffer);
            return this;
        }

        @Override
        public final Hasher put(CharSequence charSequence) {
            for (int i = 0; i < charSequence.length(); i++) {
                put(charSequence.charAt(i));
            }
            return this;
        }

        /*
         * Note: hashString(CharSequence, Charset) is intentionally not overridden.
         *
         * While intuitively, using CharsetEncoder to encode the CharSequence directly to the buffer (or
         * even to an intermediate buffer) should be considerably more efficient than potentially
         * copying the CharSequence to a String and then calling getBytes(Charset) on that String, in
         * reality there are optimizations that make the getBytes(Charset) approach considerably faster,
         * at least for commonly used charsets like UTF-8.
         */

        @Override
        public final Hasher put(byte b) {
            buffer.put(b);
            munchIfFull();
            return this;
        }

        @Override
        public final Hasher put(short s) {
            buffer.putShort(s);
            munchIfFull();
            return this;
        }

        @Override
        public final Hasher put(char c) {
            buffer.putChar(c);
            munchIfFull();
            return this;
        }

        @Override
        public final Hasher put(int i) {
            buffer.putInt(i);
            munchIfFull();
            return this;
        }

        @Override
        public final Hasher put(long l) {
            buffer.putLong(l);
            munchIfFull();
            return this;
        }

        @Override
        public final <T> Hasher put(T instance, BiConsumer<? super T, ? super Hasher> funnel) {
            funnel.accept(instance, this);
            return this;
        }

        @Override
        public final HashCode hash() {
            munch();
            buffer.flip();
            if (buffer.remaining() > 0) {
                processRemaining(buffer);
            }
            return makeHash();
        }

        abstract HashCode makeHash();

        // Process pent-up data in chunks
        private void munchIfFull() {
            if (buffer.remaining() < 8) {
                // buffer is full; not enough room for a primitive. We have at least one full chunk.
                munch();
            }
        }

        private void munch() {
            buffer.flip();
            while (buffer.remaining() >= chunkSize) {
                // we could limit the buffer to ensure process() does not read more than
                // chunkSize number of bytes, but we trust the implementations
                process(buffer);
            }
            buffer.compact(); // preserve any remaining data that do not make a full chunk
        }
    }
}