/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.nfi.backend.libffi;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.nfi.backend.libffi.LibFFIType.CachedTypeInfo;

abstract class ClosureArgumentNode extends Node {

    public abstract Object execute(VirtualFrame frame);

    static final class ConstArgumentNode extends ClosureArgumentNode {

        private final Object value;

        ConstArgumentNode(Object value) {
            this.value = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return value;
        }
    }

    static final class GetArgumentNode extends ClosureArgumentNode {

        private final int index;

        GetArgumentNode(int index) {
            this.index = index;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return frame.getArguments()[index];
        }
    }

    @NodeChild(value = "argument", type = ClosureArgumentNode.class)
    abstract static class BufferClosureArgumentNode extends ClosureArgumentNode {

        final CachedTypeInfo type;

        BufferClosureArgumentNode(CachedTypeInfo type) {
            this.type = type;
        }

        @Specialization
        Object doBuffer(ByteBuffer arg,
                        @CachedLibrary("type") NativeArgumentLibrary nativeArguments) {
            NativeArgumentBuffer buffer = new NativeArgumentBuffer.Direct(arg, 0);
            return nativeArguments.deserialize(type, buffer);
        }
    }

    @NodeChild(value = "argument", type = ClosureArgumentNode.class)
    abstract static class ObjectClosureArgumentNode extends ClosureArgumentNode {

        @Specialization(guards = "arg == null")
        Object doNull(@SuppressWarnings("unused") Object arg) {
            return NativePointer.create(LibFFILanguage.get(this), 0);
        }

        @Fallback
        Object doObject(Object arg) {
            return arg;
        }
    }

    @NodeChild(value = "argument", type = ClosureArgumentNode.class)
    abstract static class StringClosureArgumentNode extends ClosureArgumentNode {

        @Specialization(guards = "arg == null")
        Object doNull(@SuppressWarnings("unused") Object arg) {
            return new NativeString(0);
        }

        @Fallback
        Object doString(Object arg) {
            return arg;
        }
    }

    static class InjectedClosureArgumentNode extends ClosureArgumentNode {

        @Override
        public Object execute(VirtualFrame frame) {
            return new NativePointer(0);
        }
    }
}
