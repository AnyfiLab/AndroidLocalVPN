package me.joowon.androidlocalvpn.processor;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.joowon.androidlocalvpn.header.l4.TCPHeader;
import me.joowon.androidlocalvpn.packet.TCPPacket;
import me.joowon.androidlocalvpn.tcp.TCB;
import me.joowon.androidlocalvpn.util.ByteBufferPool;

/**
 * Created by joowon on 2017. 10. 25..
 */

public class TCPInputProcessor extends PacketProcessor {
    private static final String TAG = TCPInputProcessor.class.getSimpleName();

    private Selector selector;
    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;

    public TCPInputProcessor(ConcurrentLinkedQueue<ByteBuffer> outputQueue, Selector selector) {
        this.outputQueue = outputQueue;
        this.selector = selector;
    }

    @Override
    public void run() {
        Log.d(TAG, "Started");

        try {
            while (!Thread.interrupted()) {
                int readyChannels = selector.select();

                if (readyChannels == 0) {
                    Thread.sleep(10);
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();

                while (keyIterator.hasNext() && !Thread.interrupted()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid()) {
                        if (key.isConnectable())
                            processConnect(key, keyIterator);
                        else if (key.isReadable())
                            processInput(key, keyIterator);
                    }
                }
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Stopped");
        } catch (IOException e) {
            Log.w(TAG, "Error occurred while process TCP input", e);
        } catch (Exception e) {
            Log.e(TAG, "Bug!!!", e);
        } finally {
            Log.e(TAG, "TCPInputProcessor terminated!");
        }
    }

    private void processConnect(SelectionKey key, Iterator<SelectionKey> keyIterator) {
        TCPPacket packet = (TCPPacket) key.attachment();

        try {
            if (packet.tcb.channel.finishConnect()) {
                keyIterator.remove();
                packet.tcb.status = TCB.TCBStatus.SYN_RECEIVED;

                // TODO : Set MSS for receiving larger packets from the device
                ByteBuffer responseBuffer = ByteBufferPool.acquire();
                packet.updateBuffer(responseBuffer, (byte) (TCPHeader.SYN | TCPHeader.ACK),
                        packet.tcb.seqNum, packet.tcb.ackNum, 0);
                outputQueue.offer(responseBuffer);
                System.out.println("In  " + packet);

                packet.tcb.seqNum++;
                key.interestOps(SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            Log.e(TAG, "Connection error: " + packet.tcb.key, e);

            ByteBuffer responseBuffer = ByteBufferPool.acquire();
            packet.updateBuffer(responseBuffer, (byte) TCPHeader.RST, 0, packet.tcb.ackNum, 0);
            System.out.println("In  " + packet);
            outputQueue.offer(responseBuffer);

            TCB.closeTCB(packet.tcb);
        }
    }

    private void processInput(SelectionKey key, Iterator<SelectionKey> keyIterator) {
        keyIterator.remove();
        TCPPacket packet = (TCPPacket) key.attachment();

        ByteBuffer receiveBuffer = ByteBufferPool.acquire();
        // Leave space for the header
        int headerSize = packet.ipPacket.ipHeader.getLength() + TCPHeader.TCP_HEADER_SIZE;
        receiveBuffer.position(headerSize);

        synchronized (packet.tcb) {
            SocketChannel inputChannel = (SocketChannel) key.channel();
            int readBytes;
            try {
//                System.out.println("1 " + receiveBuffer.position() + " " + receiveBuffer.limit());
                readBytes = inputChannel.read(receiveBuffer);   // FIXME : Bad position 4384/40
//                System.out.println("2 " + receiveBuffer.position() + " " + receiveBuffer.limit());
            } catch (IOException e) {
                Log.e(TAG, "Network read error: " + packet.tcb.key, e);

                packet.updateBuffer(receiveBuffer, (byte) TCPHeader.RST, 0, packet.tcb.ackNum, 0);
                System.out.println("In  " + packet);
                outputQueue.offer(receiveBuffer);

                TCB.closeTCB(packet.tcb);
                return;
            }

            if (readBytes == -1) {
                // End of stream, stop waiting until we push more data
                key.interestOps(0);
                packet.tcb.waitingForNetData = false;

                if (packet.tcb.status != TCB.TCBStatus.CLOSE_WAIT) {
                    ByteBufferPool.release(receiveBuffer);
                    // TODO : FIN (FIN_WAIT state)
                    return;
                }

                packet.tcb.status = TCB.TCBStatus.LAST_ACK;
                packet.updateBuffer(receiveBuffer, (byte) TCPHeader.FIN,
                        packet.tcb.seqNum, packet.tcb.ackNum, 0);
                System.out.println("In  " + packet);
                packet.tcb.seqNum++;
            } else {
                // TODO : Check this - We should ideally be splitting segments by MTU/MSS, but this seems to work without
                packet.updateBuffer(receiveBuffer, (byte) (TCPHeader.PSH | TCPHeader.ACK),
                        packet.tcb.seqNum, packet.tcb.ackNum, readBytes);
                packet.tcb.seqNum += readBytes;
                receiveBuffer.position(headerSize + readBytes);

//                System.out.println("In  " + packet);
            }
        }
        outputQueue.offer(receiveBuffer);
    }
}
