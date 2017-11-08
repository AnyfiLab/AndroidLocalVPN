package me.joowon.androidlocalvpn.util;

import java.net.InetAddress;

import me.joowon.androidlocalvpn.header.l3.IPHeader;
import me.joowon.androidlocalvpn.header.l3.IPv4Header;
import me.joowon.androidlocalvpn.header.l4.TCPHeader;
import me.joowon.androidlocalvpn.header.l4.UDPHeader;
import me.joowon.androidlocalvpn.packet.IPPacket;

/**
 * Created by joowon on 2017. 10. 30..
 */

public class IPPacketUtil {
    public static void swapSrcAndDst(IPPacket ipPacket) {
        if (ipPacket.ipVersion == IPHeader.IPVersion.Version4) {
            IPv4Header ipv4Header = (IPv4Header) ipPacket.ipHeader;

            InetAddress newSrcAddr = ipv4Header.destinationAddress;
            ipv4Header.destinationAddress = ipv4Header.sourceAddress;
            ipv4Header.sourceAddress = newSrcAddr;

            if (ipPacket.isTCP()) {
                TCPHeader tcpHeader = (TCPHeader) ipPacket.l4Header;

                int newSrcPort = tcpHeader.destPort;
                tcpHeader.destPort = tcpHeader.srcPort;
                tcpHeader.srcPort = newSrcPort;
            } else if (ipPacket.isUDP()) {
                UDPHeader udpHeader = (UDPHeader) ipPacket.l4Header;

                int newSrcPort = udpHeader.destPort;
                udpHeader.destPort = udpHeader.srcPort;
                udpHeader.srcPort = newSrcPort;
            }
        } else if (ipPacket.ipVersion == IPHeader.IPVersion.Version6) {
            // TODO : Support IPv6
        }
    }
}
