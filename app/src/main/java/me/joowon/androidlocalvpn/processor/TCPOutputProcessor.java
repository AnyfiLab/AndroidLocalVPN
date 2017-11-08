package me.joowon.androidlocalvpn.processor;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.joowon.androidlocalvpn.LocalVpnService;
import me.joowon.androidlocalvpn.header.l4.TCPHeader;
import me.joowon.androidlocalvpn.packet.IPPacket;
import me.joowon.androidlocalvpn.packet.TCPPacket;
import me.joowon.androidlocalvpn.tcp.TCB;
import me.joowon.androidlocalvpn.tcp.TCBKey;
import me.joowon.androidlocalvpn.util.ByteBufferPool;
import me.joowon.androidlocalvpn.util.HexUtil;
import me.joowon.androidlocalvpn.util.IPPacketUtil;

/**
 * Created by joowon on 2017. 10. 25..
 */

public class TCPOutputProcessor extends PacketProcessor {
    private static final String TAG = TCPOutputProcessor.class.getSimpleName();

    private LocalVpnService vpnService;
    private ConcurrentLinkedQueue<IPPacket> inputQueue;
    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;
    private Selector selector;

    private Random random = new Random();

    public TCPOutputProcessor(ConcurrentLinkedQueue<IPPacket> inputQueue, ConcurrentLinkedQueue<ByteBuffer> outputQueue,
                              Selector selector, LocalVpnService vpnService) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.selector = selector;
        this.vpnService = vpnService;
    }

    @Override
    public void run() {
        Log.i(TAG, "Started");

        try {
            Thread currentThread = Thread.currentThread();
            while (true) {
                IPPacket currentPacket;
                do {
                    currentPacket = inputQueue.poll();
                    if (currentPacket != null)
                        break;
                    Thread.sleep(10);
                } while (!currentThread.isInterrupted());
                assert currentPacket != null;

                if (currentThread.isInterrupted())
                    break;

                ByteBuffer payloadBuffer = currentPacket.backingBuffer;
                currentPacket.backingBuffer = null;
                ByteBuffer responseBuffer = ByteBufferPool.acquire();

                InetAddress destAddr = currentPacket.ipHeader.getDstInetAddress();
                TCPHeader tcpHeader = (TCPHeader) currentPacket.l4Header;
                int dstPort = tcpHeader.destPort;
                int srcPort = tcpHeader.srcPort;

                TCBKey tcbKey = new TCBKey(srcPort, destAddr, dstPort);
                TCB tcb = TCB.getTCB(tcbKey);
                if (tcb == null) {
                    initializeConnection(tcbKey, currentPacket, tcpHeader, responseBuffer);
                } else {
                    TCPPacket tcpPacket = new TCPPacket(tcb, currentPacket);
//                    System.out.println("Out " + tcpPacket + " " + HexUtil.fromByteBuffer(payloadBuffer));

                    if (tcpHeader.isSYN())
                        processDuplicateSYN(tcpPacket, responseBuffer);
                    else if (tcpHeader.isRST())
                        processClean(tcpPacket, responseBuffer);
                    else if (tcpHeader.isFIN())
                        processFIN(tcpPacket, responseBuffer);
                    else if (tcpHeader.isACK())
                        processACK(tcpPacket, payloadBuffer, responseBuffer);
                }

                // Cleanup later
                if (responseBuffer.position() == 0)
                    ByteBufferPool.release(responseBuffer);
                ByteBufferPool.release(payloadBuffer);
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Stopped");
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while processing TCPOutput", e);
        } catch (Exception e) {
            Log.e(TAG, "Bug!", e);
        } finally {
            Log.e(TAG, "TCPOutputProcessor terminated!");

            TCB.closeAllTCB();
        }
    }

    private void initializeConnection(TCBKey tcbKey, IPPacket packet, TCPHeader tcpHeader, ByteBuffer responseBuffer) throws IOException {
        IPPacketUtil.swapSrcAndDst(packet);
        if (tcpHeader.isSYN()) {
            SocketChannel outputChannel = SocketChannel.open();
            outputChannel.configureBlocking(false);
            vpnService.protect(outputChannel.socket());

            TCB tcb = TCB.newTCB(tcbKey, random.nextInt(Short.MAX_VALUE + 1), tcpHeader.seqNum,
                    tcpHeader.seqNum + 1, tcpHeader.ackNum, outputChannel);
            TCPPacket tcpPacket = new TCPPacket(tcb, packet);
            System.out.println("Out " + tcpPacket);

            try {
                outputChannel.connect(new InetSocketAddress(tcbKey.dstIp, tcbKey.dstPort));
                if (outputChannel.finishConnect()) {
                    tcb.status = TCB.TCBStatus.SYN_RECEIVED;
                    // TODO : Set MSS for receiving larger packets from the device
                    tcpPacket.updateBuffer(responseBuffer, (byte) (TCPHeader.SYN | TCPHeader.ACK),
                            tcb.seqNum, tcb.ackNum, 0);
                    tcb.seqNum++;    // SYN counts as a byte
                } else {
                    tcb.status = TCB.TCBStatus.SYN_SENT;
                    selector.wakeup();
                    tcpPacket.tcb.selectionKey = outputChannel.register(selector, SelectionKey.OP_CONNECT, tcpPacket);
                    return;
                }
            } catch (IOException e) {
                Log.e(TAG, "Connection error: " + tcbKey, e);

                tcpPacket.updateBuffer(responseBuffer, (byte) TCPHeader.RST, 0, tcb.ackNum, 0);
                TCB.closeTCB(tcb);
            }
        } else {
            // FIXME BufferOverflowException
            new TCPPacket(null, packet).updateBuffer(responseBuffer, (byte) TCPHeader.RST,
                    0, tcpHeader.seqNum + 1, 0);
        }

        outputQueue.offer(responseBuffer);
    }

    private void processDuplicateSYN(TCPPacket tcpPacket, ByteBuffer responseBuffer) {
        synchronized (tcpPacket.tcb) {
            if (tcpPacket.tcb.status == TCB.TCBStatus.SYN_SENT) {
                tcpPacket.tcb.ackNum = tcpPacket.tcpHeader.seqNum + 1;
                return;
            }
        }
        sendRST(tcpPacket, 1, responseBuffer);
    }

    private void processFIN(TCPPacket tcpPacket, ByteBuffer responseBuffer) {
        synchronized (tcpPacket.tcb) {
            tcpPacket.tcb.seqNum = tcpPacket.tcpHeader.seqNum + 1;
            tcpPacket.tcb.devAckNum = tcpPacket.tcpHeader.ackNum;

            if (tcpPacket.tcb.waitingForNetData) {
                tcpPacket.tcb.status = TCB.TCBStatus.CLOSE_WAIT;
                tcpPacket.updateBuffer(responseBuffer, (byte) TCPHeader.ACK,
                        tcpPacket.tcb.seqNum, tcpPacket.tcb.ackNum, 0);
            } else {
                tcpPacket.tcb.status = TCB.TCBStatus.LAST_ACK;
                tcpPacket.updateBuffer(responseBuffer, (byte) (TCPHeader.FIN | TCPHeader.ACK),
                        tcpPacket.tcb.seqNum, tcpPacket.tcb.ackNum, 0);
                tcpPacket.tcb.seqNum++;
            }
        }

        outputQueue.offer(responseBuffer);
    }

    private void processACK(TCPPacket tcpPacket, ByteBuffer payloadBuffer, ByteBuffer responseBuffer) throws ClosedChannelException {
        // TODO : Check SEQ, ACK
        IPPacketUtil.swapSrcAndDst(tcpPacket.ipPacket);
        int payloadSize = payloadBuffer.limit() - payloadBuffer.position();

        synchronized (tcpPacket.tcb) {
            SocketChannel outputChannel = tcpPacket.tcb.channel;
            if (tcpPacket.tcb.status == TCB.TCBStatus.SYN_RECEIVED) {
                tcpPacket.tcb.status = TCB.TCBStatus.ESTABLISHED;

                selector.wakeup();
                tcpPacket.tcb.selectionKey = outputChannel.register(selector, SelectionKey.OP_READ, tcpPacket);
                tcpPacket.tcb.waitingForNetData = true;
            } else if (tcpPacket.tcb.status == TCB.TCBStatus.LAST_ACK) {
                processClean(tcpPacket, responseBuffer);
            }

            if (payloadSize == 0)   // if Empty ACK, ignore
                return;

            if (!tcpPacket.tcb.waitingForNetData) {
                selector.wakeup();
                tcpPacket.tcb.selectionKey.interestOps(SelectionKey.OP_READ);
                tcpPacket.tcb.waitingForNetData = true;
            }

            // Forward to remote server
            try {
                while (payloadBuffer.hasRemaining())
                    outputChannel.write(payloadBuffer);     // FIXME : Bad position 16834/40
            } catch (IOException e) {
                Log.e(TAG, "Network write error: " + tcpPacket.tcb.key, e);

                sendRST(tcpPacket, payloadSize, responseBuffer);
                return;
            }

            // TODO : We don't expect out-of-order packets, but verify
            tcpPacket.tcb.ackNum = tcpPacket.tcpHeader.seqNum + payloadSize;
            tcpPacket.tcb.devAckNum = tcpPacket.tcpHeader.ackNum;
            tcpPacket.updateBuffer(responseBuffer, (byte) TCPHeader.ACK, tcpPacket.tcb.seqNum, tcpPacket.tcb.ackNum, 0);    // FIXME : Bad position 20/16834
        }

//        System.out.println("IN  " + tcpPacket);
        outputQueue.offer(responseBuffer);
    }

    private void sendRST(TCPPacket tcpPacket, int prevPayloadSize, ByteBuffer buffer) {
        tcpPacket.updateBuffer(buffer, (byte) TCPHeader.RST, 0, tcpPacket.tcb.ackNum + prevPayloadSize, 0);
        outputQueue.offer(buffer);
        TCB.closeTCB(tcpPacket.tcb);
    }

    private void processClean(TCPPacket tcpPacket, ByteBuffer responseBuffer) {
        ByteBufferPool.release(responseBuffer);
        TCB.closeTCB(tcpPacket.tcb);
    }
}
