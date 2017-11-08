package me.joowon.androidlocalvpn.util;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by joowon on 2017. 10. 23..
 */

public class ByteBufferPool {
    private static final int BUFFER_SIZE = 16834;
    private static ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();

    public static ByteBuffer acquire() {
        ByteBuffer buffer = pool.poll();
        if (buffer == null)
            buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        return buffer;
    }

    public static void release(ByteBuffer buffer) {
        buffer.clear();
        pool.offer(buffer);
    }

    public static void clear() {
        pool.clear();
    }
}
