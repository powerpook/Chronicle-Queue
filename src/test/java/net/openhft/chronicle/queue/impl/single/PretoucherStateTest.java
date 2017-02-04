/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.queue.impl.single;

import net.openhft.chronicle.bytes.MappedBytes;
import org.junit.Test;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 28/11/16.
 */
public class PretoucherStateTest {
    @Test
    public void pretouch() throws Exception {
        long[] pos = {0};
        final StringBuilder record = new StringBuilder();
        PretoucherState ps = new DummyPretoucherState(() -> pos[0] += 4096, 64 << 10, record, () -> false);
        ps.pretouch(null);
        ps.pretouch(null);
        assertEquals("debug none - Reset pretoucher to pos 4096 as the underlying MappedBytes changed.\n" +
                "touchPage 4096\n" +
                "touchPage 8192\n" +
                "touchPage 12288\n" +
                "touchPage 16384\n" +
                "touchPage 20480\n" +
                "touchPage 24576\n" +
                "touchPage 28672\n" +
                "touchPage 32768\n" +
                "touchPage 36864\n" +
                "touchPage 40960\n" +
                "touchPage 45056\n" +
                "touchPage 49152\n" +
                "touchPage 53248\n" +
                "touchPage 57344\n" +
                "touchPage 61440\n" +
                "touchPage 65536\n" +
                "touchPage 69632\n" +
                "debug pretouch for only 0 of 17\n" +
                "debug none: Advanced 4 KB between pretouch() and 4 KB while mapping of 64 KB.\n", record.toString());
    }

    @Test
    public void pretouchLeap() throws Exception {
        long[] pos = {1 << 20};
        final StringBuilder record = new StringBuilder();
        PretoucherState ps = new DummyPretoucherState(() -> pos[0] += 4096, 16 << 10, record, () -> true);
        ps.pretouch(null); // reset();
        for (int i = 0; i < 4; i++)
            ps.pretouch(null);
        ps.pretouch(null);
        assertEquals("debug none - Reset pretoucher to pos 1052672 as the underlying MappedBytes changed.\n" +
                "touchPage 1052672\n" +
                "touchPage 1056768\n" +
                "touchPage 1060864\n" +
                "touchPage 1064960\n" +
                "touchPage 1069056\n" +
                "debug none: Advanced 4 KB between pretouch() and 4 KB while mapping of 16 KB.\n" +
                "touchPage 1073152\n" +
                "touchPage 1077248\n" +
                "debug none: Advanced 8 KB between pretouch() and 4 KB while mapping of 16 KB.\n" +
                "touchPage 1081344\n" +
                "touchPage 1085440\n" +
                "debug none: Advanced 8 KB between pretouch() and 4 KB while mapping of 16 KB.\n" +
                "touchPage 1089536\n" +
                "touchPage 1093632\n" +
                "debug none: Advanced 8 KB between pretouch() and 4 KB while mapping of 16 KB.\n" +
                "touchPage 1097728\n" +
                "touchPage 1101824\n" +
                "debug none: Advanced 8 KB between pretouch() and 4 KB while mapping of 16 KB.\n", record.toString());
    }

    @Test
    public void pretouchLongBreak() throws Exception {
        long[] pos = {0};
        final StringBuilder record = new StringBuilder();
        PretoucherState ps = new DummyPretoucherState(() -> pos[0] += 1024, 16 << 10, record, () -> true);
        for (int i = 0; i <= 20; i++) {
            record.append("pos: " + pos[0] + ", i:" + i + "\n");
            ps.pretouch(null);
        }
        assertEquals("pos: 0, i:0\n" +
                "debug none - Reset pretoucher to pos 1024 as the underlying MappedBytes changed.\n" +
                "pos: 1024, i:1\n" +
                "touchPage 0\n" +
                "touchPage 4096\n" +
                "touchPage 8192\n" +
                "touchPage 12288\n" +
                "touchPage 16384\n" +
                "debug none: Advanced 1 KB between pretouch() and 1 KB while mapping of 16 KB.\n" +
                "pos: 3072, i:2\n" +
                "pos: 4096, i:3\n" +
                "touchPage 20480\n" +
                "debug none: Advanced 3 KB between pretouch() and 1 KB while mapping of 16 KB.\n" +
                "pos: 6144, i:4\n" +
                "pos: 7168, i:5\n" +
                "pos: 8192, i:6\n" +
                "touchPage 24576\n" +
                "debug none: Advanced 4 KB between pretouch() and 1 KB while mapping of 16 KB.\n" +
                "pos: 10240, i:7\n" +
                "pos: 11264, i:8\n" +
                "pos: 12288, i:9\n" +
                "touchPage 28672\n" +
                "debug none: Advanced 4 KB between pretouch() and 1 KB while mapping of 16 KB.\n" +
                "pos: 14336, i:10\n" +
                "pos: 15360, i:11\n" +
                "pos: 16384, i:12\n" +
                "touchPage 32768\n" +
                "debug none: Advanced 4 KB between pretouch() and 1 KB while mapping of 16 KB.\n" +
                "pos: 18432, i:13\n" +
                "pos: 19456, i:14\n" +
                "pos: 20480, i:15\n" +
                "touchPage 36864\n" +
                "debug none: Advanced 4 KB between pretouch() and 1 KB while mapping of 16 KB.\n" +
                "pos: 22528, i:16\n" +
                "pos: 23552, i:17\n" +
                "pos: 24576, i:18\n" +
                "touchPage 40960\n" +
                "debug none: Advanced 4 KB between pretouch() and 1 KB while mapping of 16 KB.\n" +
                "pos: 26624, i:19\n" +
                "pos: 27648, i:20\n", record.toString());
    }

    class DummyPretoucherState extends PretoucherState {
        private final BooleanSupplier touched;
        StringBuilder record;

        DummyPretoucherState(LongSupplier posSupplier, int headRoom, StringBuilder record, BooleanSupplier touched) {
            super(posSupplier, headRoom);
            this.record = record;
            this.touched = touched;
        }

        @Override
        protected void debug(String message) {
            record.append("debug ").append(message).append("\n");
        }

        @Override
        protected boolean touchPage(MappedBytes bytes, long offset) {
            record.append("touchPage ").append(offset).append("\n");
            return touched.getAsBoolean();
        }
    }

}