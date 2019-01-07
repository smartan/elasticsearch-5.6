/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.transport.netty3;

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.bytes.AbstractBytesReferenceTestCase;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.ReleasableBytesStreamOutput;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.io.IOException;

public class ChannelBufferBytesReferenceTests extends AbstractBytesReferenceTestCase {

    @Override
    protected BytesReference newBytesReference(int length) throws IOException {
        return newBytesReferenceWithOffsetOfZero(length);
    }

    @Override
    protected BytesReference newBytesReferenceWithOffsetOfZero(int length) throws IOException {
        ReleasableBytesStreamOutput out = new ReleasableBytesStreamOutput(length, bigarrays);
        for (int i = 0; i < length; i++) {
            out.writeByte((byte) random().nextInt(1 << 8));
        }
        assertEquals(out.size(), length);
        BytesReference ref = out.bytes();
        assertEquals(ref.length(), length);
        BytesRef bytesRef = ref.toBytesRef();
        final ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(bytesRef.bytes, bytesRef.offset, bytesRef.length);
        return Netty3Utils.toBytesReference(channelBuffer);
    }

    public void testSliceOnAdvancedBuffer() throws IOException {
        BytesReference bytesReference = newBytesReference(randomIntBetween(10, 3 * PAGE_SIZE));
        BytesRef bytesRef = bytesReference.toBytesRef();
        ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(bytesRef.bytes, bytesRef.offset,
            bytesRef.length);
        int numBytesToRead = randomIntBetween(1, 5);
        for (int i = 0; i < numBytesToRead; i++) {
            channelBuffer.readByte();
        }
        BytesReference other = Netty3Utils.toBytesReference(channelBuffer);
        BytesReference slice = bytesReference.slice(numBytesToRead, bytesReference.length() - numBytesToRead);
        assertEquals(other, slice);
        assertEquals(other.slice(3, 1), slice.slice(3, 1));
    }

    public void testImmutable() throws IOException {
        BytesReference bytesReference = newBytesReference(randomIntBetween(10, 3 * PAGE_SIZE));
        BytesRef bytesRef = BytesRef.deepCopyOf(bytesReference.toBytesRef());
        ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(bytesRef.bytes, bytesRef.offset,
            bytesRef.length);
        ChannelBufferBytesReference channelBufferBytesReference = new ChannelBufferBytesReference(channelBuffer, bytesRef.length);
        assertEquals(channelBufferBytesReference, bytesReference);
        channelBuffer.readInt(); // this advances the index of the channel buffer
        assertEquals(channelBufferBytesReference, bytesReference);
        assertEquals(bytesRef, channelBufferBytesReference.toBytesRef());

        BytesRef unicodeBytes = new BytesRef(randomUnicodeOfCodepointLength(100));
        channelBuffer = ChannelBuffers.wrappedBuffer(unicodeBytes.bytes, unicodeBytes.offset, unicodeBytes.length);
        channelBufferBytesReference = new ChannelBufferBytesReference(channelBuffer, unicodeBytes.length);
        String utf8ToString = channelBufferBytesReference.utf8ToString();
        channelBuffer.readInt(); // this advances the index of the channel buffer
        assertEquals(utf8ToString, channelBufferBytesReference.utf8ToString());
    }
}
