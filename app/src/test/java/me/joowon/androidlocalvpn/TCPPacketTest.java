package me.joowon.androidlocalvpn;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import me.joowon.androidlocalvpn.header.l3.IPv4Header;
import me.joowon.androidlocalvpn.header.l4.TCPHeader;
import me.joowon.androidlocalvpn.packet.IPPacket;
import me.joowon.androidlocalvpn.packet.TCPPacket;
import me.joowon.androidlocalvpn.util.ByteBufferPool;
import me.joowon.androidlocalvpn.util.HeaderInfo;
import me.joowon.androidlocalvpn.util.HexUtil;
import me.joowon.androidlocalvpn.util.IPv4HeaderInfo;
import me.joowon.androidlocalvpn.util.IPv4TCPPacketInfo;
import me.joowon.androidlocalvpn.util.PacketValidator;
import me.joowon.androidlocalvpn.util.TCPHeaderInfo;

import static junit.framework.Assert.assertTrue;
import static me.joowon.androidlocalvpn.util.ByteBufferPool.acquire;

/**
 * Created by joowon on 2017. 11. 2..
 */

public class TCPPacketTest {
    @Test
    public void testUpdateBuffer() throws UnknownHostException, HeaderInfo.UnmatchedException {
        String hex = "45 00 00 28 ca 1b 00 00 40 06 4f 02 c0 a8 00 0d d8 3a c8 c2 c1 62 01 bb c5 37 91 b8 39 4b fa 8a 50 10 10 00 f0 3d 00 00";
        IPPacket ipPacket = new IPPacket(ByteBuffer.wrap(HexUtil.toBytes(hex)));
        TCPPacket tcpPacket = new TCPPacket(null, ipPacket);

        int seqNum = 111;
        int ackNum = 888;
        ByteBuffer buffer = ByteBufferPool.acquire();
        tcpPacket.updateBuffer(buffer, (byte) (TCPHeader.SYN | TCPHeader.ACK),
                seqNum, ackNum, 0);

        System.out.println(HexUtil.fromByteBuffer(buffer));

        PacketValidator packetValidator = new PacketValidator(buffer);
        IPv4HeaderInfo ipv4Header = new IPv4HeaderInfo();
        ipv4Header.version = 4;
        ipv4Header.IHL = 20;
        ipv4Header.typeOfService = 0x00;
        ipv4Header.totalLength = 40;
        ipv4Header.identification = 0xca1b;
        ipv4Header.flags = 0x00;
        ipv4Header.fragmentOffset = 0;
        ipv4Header.TTL = 64;
        ipv4Header.protocol = IPv4Header.TransportProtocol.TCP;
        ipv4Header.headerChecksum = 0x4f02;
        ipv4Header.sourceAddress = InetAddress.getByName("192.168.0.13");
        ipv4Header.destinationAddress = InetAddress.getByName("216.58.200.194");
        ipv4Header.optionsAndPadding = 0;

        TCPHeaderInfo tcpHeader = new TCPHeaderInfo();
        tcpHeader.srcPort = 49506;
        tcpHeader.destPort = 443;
        tcpHeader.seqNum = seqNum;
        tcpHeader.ackNum = ackNum;
        tcpHeader.dataOffset = 20;
        tcpHeader.reserved = 0;
        tcpHeader.ns = 0;
        tcpHeader.flags = (byte) (TCPHeader.SYN | TCPHeader.ACK);   // Changed
        tcpHeader.window = 4096;
        tcpHeader.checksum = 0x771b;    // Changed
        tcpHeader.urgentPointer = 0;
        tcpHeader.optionsAndPadding = null;

        Assert.assertTrue(packetValidator.validate(
                new IPv4TCPPacketInfo(ipv4Header, tcpHeader)
        ));
    }
}
