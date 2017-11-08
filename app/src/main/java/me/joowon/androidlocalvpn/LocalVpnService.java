package me.joowon.androidlocalvpn;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.joowon.androidlocalvpn.packet.IPPacket;
import me.joowon.androidlocalvpn.processor.TCPInputProcessor;
import me.joowon.androidlocalvpn.processor.TCPOutputProcessor;
import me.joowon.androidlocalvpn.processor.UDPInputProcessor;
import me.joowon.androidlocalvpn.processor.UDPOutputProcessor;
import me.joowon.androidlocalvpn.util.ByteBufferPool;
import me.joowon.androidlocalvpn.util.HexUtil;

/**
 * VpnService which implement local VPN.
 * <p>
 * LocalVpnService contains 3 queue for processing packets.
 * {@code devToNetUDPQueue} : UDP packets read from device are inserted by {@link VpnRunnable},
 * then process the packets at {@link UDPOutputProcessor}.
 * {@code devToNetTCPQueue} : TCP packets read from device are inserted by {@link VpnRunnable},
 * then process the packets at {@link TCPOutputProcessor}.
 * {@code netToDevQueue} : UDP and TCP packets read from network are inserted
 * by {@link TCPInputProcessor} and {@link UDPInputProcessor},
 * then process the packets at {@link VpnRunnable}.
 * <p>
 * LocalVpnService uses 5 threads for processing packets.
 * {@link UDPInputProcessor} : Process UDP input packets (Network -> Device)
 * {@link UDPOutputProcessor} : Process UDP output packets (Device -> Network)
 * {@link TCPInputProcessor} : Process TCP input packets (Network -> Device)
 * {@link TCPOutputProcessor} : Process TCP output packets (Device -> Network_
 * {@link VpnRunnable} : Doing the VPN I/O operations. Read packets from device, and write to device.
 * <p>
 * Created by joowon
 */
public class LocalVpnService extends VpnService {
    private static String TAG = LocalVpnService.class.getSimpleName();

    private static final String VPN_CONFIG_ADDRESS = "10.0.0.1";
    private static final int VPN_CONFIG_ADDRESS_PREFIX = 24;
    private static final String VPN_CONFIG_ROUTE = "0.0.0.0";     // Intercept everything
    private static final String VPN_CONFIG_DNS = "8.8.8.8";
    private static final int VPN_CONFIG_MTU = 8000;

    private static boolean isRunning;


    private ParcelFileDescriptor vpnFileDescriptor;

    private ConcurrentLinkedQueue<IPPacket> devToNetUDPQueue;     // device to network UDP Queue
    private ConcurrentLinkedQueue<IPPacket> devToNetTCPQueue;     // device to network TCP Queue
    private ConcurrentLinkedQueue<ByteBuffer> netToDevQueue;    // network to device queue
    private ExecutorService threadPool;

    private Selector tcpSelector;
    private Selector udpSelector;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        setupVpn();

        try {
            tcpSelector = Selector.open();
            udpSelector = Selector.open();

            devToNetUDPQueue = new ConcurrentLinkedQueue<>();
            devToNetTCPQueue = new ConcurrentLinkedQueue<>();
            netToDevQueue = new ConcurrentLinkedQueue<>();

            threadPool = Executors.newFixedThreadPool(5);
            threadPool.submit(new UDPInputProcessor(netToDevQueue, udpSelector));
            threadPool.submit(new UDPOutputProcessor(devToNetUDPQueue, udpSelector, this));
            threadPool.submit(new TCPInputProcessor(netToDevQueue, tcpSelector));
            threadPool.submit(new TCPOutputProcessor(devToNetTCPQueue, netToDevQueue, tcpSelector, this));
            threadPool.submit(new VpnRunnable(vpnFileDescriptor.getFileDescriptor(),
                    devToNetTCPQueue, devToNetUDPQueue, netToDevQueue));

            Log.i(TAG, "Started");
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while starting service", e);
            clear();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        clear();
    }

    public static boolean isRunning() {
        return isRunning;
    }

    private void setupVpn() {
        if (vpnFileDescriptor != null)
            return;

        Builder builder = new Builder();
        builder.addAddress(VPN_CONFIG_ADDRESS, VPN_CONFIG_ADDRESS_PREFIX);
        builder.addRoute(VPN_CONFIG_ROUTE, 0);
        builder.setSession(getString(R.string.app_name));
        builder.addDnsServer(VPN_CONFIG_DNS);
        builder.setMtu(VPN_CONFIG_MTU);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                builder.addDisallowedApplication(getApplication().getPackageName());
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        vpnFileDescriptor = builder.establish();
    }

    private void clear() {
        devToNetUDPQueue = null;
        devToNetTCPQueue = null;
        netToDevQueue = null;

        closeCloseable(tcpSelector, udpSelector, vpnFileDescriptor);
    }


    /**
     * VPNRunnable for processing VPN I/O.
     * <p>
     * VPN Read Operation
     * Every time VpnRunnable reads a buffer from the device via VPN, it creates a IPPacket
     * then add to the {@code devToNetTCPQueue} or {@code devNetUDPQueue}.
     * <p>
     * VPN Write Operation
     * If {@code netToDevQueue} contains any buffers, VpnRunnable will process the buffer
     * by writing a buffer into the VPN.
     */
    private static class VpnRunnable implements Runnable {
        private static String TAG = VpnRunnable.class.getSimpleName();

        private FileDescriptor vpnFileDescriptor;

        private ConcurrentLinkedQueue<IPPacket> devToNetTcpQueue;
        private ConcurrentLinkedQueue<IPPacket> devToNetUdpQueue;
        private ConcurrentLinkedQueue<ByteBuffer> netToDevQueue;

        public VpnRunnable(FileDescriptor vpnFileDescriptor,
                           ConcurrentLinkedQueue<IPPacket> devToNetTcpQueue,
                           ConcurrentLinkedQueue<IPPacket> devToNetUdpQueue,
                           ConcurrentLinkedQueue<ByteBuffer> netToDevQueue) {
            this.vpnFileDescriptor = vpnFileDescriptor;
            this.devToNetTcpQueue = devToNetTcpQueue;
            this.devToNetUdpQueue = devToNetUdpQueue;
            this.netToDevQueue = netToDevQueue;
        }

        @Override
        public void run() {
            Log.i(TAG, "Start");

            FileChannel vpnInputChannel = new FileInputStream(vpnFileDescriptor).getChannel();
            FileChannel vpnOutputChannel = new FileOutputStream(vpnFileDescriptor).getChannel();

            try {
                ByteBuffer bufferToNet = null;
                boolean dataSent = true;
                boolean dataReceived;

                while (!Thread.interrupted()) {
                    if (dataSent) bufferToNet = ByteBufferPool.acquire();
                    else bufferToNet.clear();

                    /*
                     * VPN Read Operation
                     */
                    int readBytes = vpnInputChannel.read(bufferToNet);
                    if (readBytes > 0) {
                        dataSent = true;

                        bufferToNet.flip();
                        IPPacket packet = new IPPacket(bufferToNet);
                        if (packet.backingBuffer.position() == 0) {
                            System.out.println(HexUtil.fromByteBuffer(packet.backingBuffer));
                            System.out.println(HexUtil.fromByteBuffer(packet.backingBuffer));
                        }
                        if (packet.isTCP()) {
                            devToNetTcpQueue.offer(packet);
                        } else if (packet.isUDP()) {
                            devToNetUdpQueue.offer(packet);
                        } else {
                            Log.w(TAG, "Unsupported packet type");
                            dataSent = false;
                        }
                    } else {
                        dataSent = false;
                    }

                    /*
                     * VPN Write Operation
                     */
                    ByteBuffer bufferFromNet = netToDevQueue.poll();
                    if (bufferFromNet != null) {
                        bufferFromNet.flip();
                        while (bufferFromNet.hasRemaining())
                            vpnOutputChannel.write(bufferFromNet);
                        dataReceived = true;

                        ByteBufferPool.release(bufferFromNet);
                    } else {
                        dataReceived = false;
                    }

                    /*
                     * Battery Optimization
                     */
                    if (!dataSent && !dataReceived) {
                        Thread.sleep(10);
                    }
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "Stopped");
            } catch (IOException e) {
                Log.w(TAG, "Error occurred", e);
            } catch (Exception e) {
                Log.e(TAG, "Bug", e);
            } finally {
                closeCloseable(vpnInputChannel, vpnOutputChannel);

                Log.e(TAG, "terminated");
            }
        }
    }

    private static void closeCloseable(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }
}
