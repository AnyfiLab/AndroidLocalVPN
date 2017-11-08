package me.joowon.androidlocalvpn.processor;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.joowon.androidlocalvpn.packet.UDPPacket;
import me.joowon.androidlocalvpn.util.ByteBufferPool;

/**
 * Created by joowon on 2017. 10. 25..
 */

public class UDPInputProcessor extends PacketProcessor {
    private static final String TAG = UDPInputProcessor.class.getSimpleName();

    private Selector selector;
    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;

    public UDPInputProcessor(ConcurrentLinkedQueue<ByteBuffer> outputQueue, Selector selector) {
        this.outputQueue = outputQueue;
        this.selector = selector;
    }

    @Override
    public void run() {
        Log.i(TAG, "Started");

        try {
            while (!Thread.interrupted()) {
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    Thread.sleep(10);
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIt = keys.iterator();

                while (keyIt.hasNext() && !Thread.interrupted()) {
                    SelectionKey key = keyIt.next();
                    if (key.isValid() && key.isReadable()) {
                        keyIt.remove();

                        UDPPacket referencePacket = (UDPPacket) key.attachment();
                        ByteBuffer receiveBuffer = ByteBufferPool.acquire();
                        // Leave space for the header
                        int headerLength = referencePacket.ipPacket.getHeaderLength();
                        receiveBuffer.position(headerLength);

                        DatagramChannel inputChannel = (DatagramChannel) key.channel();
                        int readBytes = inputChannel.read(receiveBuffer);

                        referencePacket.updateBuffer(receiveBuffer, readBytes);
                        receiveBuffer.position(headerLength + readBytes);

                        outputQueue.offer(receiveBuffer);
                    }
                }
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Stopped");
        } catch (IOException e) {
            Log.w(TAG, "Error occurred while processing UDP Input", e);
        } catch (Exception e) {
            Log.e(TAG, "Bug!!!", e);
        }
    }
}
