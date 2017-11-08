package me.joowon.androidlocalvpn;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import me.joowon.androidlocalvpn.header.l3.IPv4Header;
import me.joowon.androidlocalvpn.packet.IPPacket;
import me.joowon.androidlocalvpn.util.HeaderInfo;
import me.joowon.androidlocalvpn.util.HexUtil;
import me.joowon.androidlocalvpn.util.IPv4HeaderInfo;
import me.joowon.androidlocalvpn.util.IPv4TCPPacketInfo;
import me.joowon.androidlocalvpn.util.PacketValidator;
import me.joowon.androidlocalvpn.util.TCPHeaderInfo;

import static org.junit.Assert.assertTrue;

/**
 * Created by joowon on 2017. 11. 1..
 */

public class PacketHeaderTest {

    @Test
    public void testIPv4TCPHeader() throws UnknownHostException, HeaderInfo.UnmatchedException {
        PacketValidator packetValidator = new PacketValidator("45 00 00 28 ca 1b 00 00 40 06 4f 02 c0 a8 00 0d d8 3a c8 c2 c1 62 01 bb c5 37 91 b8 39 4b fa 8a 50 10 10 00 f0 3d 00 00");

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
        tcpHeader.seqNum = 3308753336L;
        tcpHeader.ackNum = 961280650L;
        tcpHeader.dataOffset = 20;
        tcpHeader.reserved = 0;
        tcpHeader.ns = 0;
        tcpHeader.flags = 0x010;
        tcpHeader.window = 4096;
        tcpHeader.checksum = 0xf03d;
        tcpHeader.urgentPointer = 0;
        tcpHeader.optionsAndPadding = null;

        assertTrue(packetValidator.validate(
                new IPv4TCPPacketInfo(ipv4Header, tcpHeader)
        ));
    }

    @Test
    public void testB() throws UnknownHostException {
        while (true) {
            ByteBuffer buffer = ByteBuffer.wrap(HexUtil.toBytes("45 00 00 28 ca 1b 00 00 40 06 4f 02 c0 a8 00 0d d8 3a c8 c2 c1 62 01 bb c5 37 91 b8 39 4b fa 8a 50 10 10 00 f0 3d 00 00"));
            IPPacket ipPacket = new IPPacket(buffer);
            if (ipPacket.backingBuffer.position() == 0) {
                System.out.println(ipPacket.backingBuffer.position() + " " + ipPacket.backingBuffer.limit());
            }
        }
    }
}
