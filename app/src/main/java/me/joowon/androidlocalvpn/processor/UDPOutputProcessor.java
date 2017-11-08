package me.joowon.androidlocalvpn.processor;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.joowon.androidlocalvpn.LocalVpnService;
import me.joowon.androidlocalvpn.header.l4.UDPHeader;
import me.joowon.androidlocalvpn.packet.IPPacket;
import me.joowon.androidlocalvpn.packet.UDPPacket;
import me.joowon.androidlocalvpn.util.ByteBufferPool;
import me.joowon.androidlocalvpn.util.IPPacketUtil;
import me.joowon.androidlocalvpn.util.LRUCache;

/**
 * Created by joowon on 2017. 10. 25..
 */

public class UDPOutputProcessor extends PacketProcessor {
    private static final String TAG = UDPOutputProcessor.class.getSimpleName();

    private LocalVpnService vpnService;
    private ConcurrentLinkedQueue<IPPacket> inputQueue;
    private Selector selector;

    private static final int MAX_CACHE_SIZE = 50;
    private LRUCache<UDPChannelKey, DatagramChannel> channelCache =
            new LRUCache<>(MAX_CACHE_SIZE, new LRUCache.CleanupCallback<UDPChannelKey, DatagramChannel>() {
                @Override
                public void onCleanup(Map.Entry<UDPChannelKey, DatagramChannel> eldest) {
                    closeChannel(eldest.getValue());
                }
            });

    public UDPOutputProcessor(ConcurrentLinkedQueue<IPPacket> inputQueue, Selector selector, LocalVpnService vpnService) {
        this.inputQueue = inputQueue;
        this.selector = selector;
        this.vpnService = vpnService;
    }

    @Override
    public void run() {
        Log.i(TAG, "Started");

        try {
            Thread currentThread = Thread.currentThread();
            while (true) {
                IPPacket packet;
                do {
                    packet = inputQueue.poll();
                    if (packet != null)
                        break;
                    Thread.sleep(10);
                } while (!currentThread.isInterrupted());
                assert packet != null;

                if (currentThread.isInterrupted())
                    break;

                InetAddress destAddr = packet.ipHeader.getDstInetAddress();

                UDPHeader udpHeader = (UDPHeader) packet.l4Header;
                int dstPort = udpHeader.destPort;
                int srcPort = udpHeader.srcPort;

                UDPChannelKey key = new UDPChannelKey(destAddr, srcPort, dstPort);
                DatagramChannel outputChannel = channelCache.get(key);
                if (outputChannel == null) {
                    outputChannel = DatagramChannel.open();
                    try {
                        outputChannel.connect(new InetSocketAddress(destAddr, dstPort));
                    } catch (IOException e) {
                        Log.e(TAG, "Connection error: " + key, e);

                        closeChannel(outputChannel);
                        ByteBufferPool.release(packet.backingBuffer);
                        continue;
                    }
                    outputChannel.configureBlocking(false);
                    IPPacketUtil.swapSrcAndDst(packet);

                    selector.wakeup();
                    outputChannel.register(selector, SelectionKey.OP_READ, new UDPPacket(packet));

                    vpnService.protect(outputChannel.socket());

                    channelCache.put(key, outputChannel);
                }

                try {
                    ByteBuffer payloadBuffer = packet.backingBuffer;
                    payloadBuffer.position(packet.getHeaderLength());
                    while (payloadBuffer.hasRemaining())
                        outputChannel.write(payloadBuffer);
                } catch (IOException e) {
                    Log.e(TAG, "Network write error: " + key, e);

                    channelCache.remove(key);
                    closeChannel(outputChannel);
                }

                ByteBufferPool.release(packet.backingBuffer);
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Stopped");
        } catch (IOException e) {
            Log.w(TAG, "Error occurred while processing UDP Ouput", e);
        } catch (Exception e) {
            Log.e(TAG, "Bug!!", e);
        } finally {
            closeAllChannel();
        }
    }

    private void closeAllChannel() {
        Iterator<Map.Entry<UDPChannelKey, DatagramChannel>> it = channelCache.entrySet().iterator();
        while (it.hasNext()) {
            closeChannel(it.next().getValue());
            it.remove();
        }
    }

    private void closeChannel(DatagramChannel channel) {
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }

    private class UDPChannelKey {
        public InetAddress addr;
        public int srcPort;
        public int dstPort;

        public UDPChannelKey(InetAddress addr, int srcPort, int dstPort) {
            this.addr = addr;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UDPChannelKey that = (UDPChannelKey) o;

            if (srcPort != that.srcPort) return false;
            if (dstPort != that.dstPort) return false;
            return addr != null ? addr.equals(that.addr) : that.addr == null;
        }

        @Override
        public int hashCode() {
            int result = addr != null ? addr.hashCode() : 0;
            result = 31 * result + srcPort;
            result = 31 * result + dstPort;
            return result;
        }

        @Override
        public String toString() {
            return "UDPChannelKey{" +
                    "addr=" + addr.getHostAddress() +
                    ", srcPort=" + srcPort +
                    ", dstPort=" + dstPort +
                    '}';
        }
    }
}
