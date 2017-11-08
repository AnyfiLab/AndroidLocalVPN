package me.joowon.androidlocalvpn.util;

import me.joowon.androidlocalvpn.packet.IPPacket;

/**
 * Created by joowon on 2017. 11. 1..
 */

public class IPv4TCPPacketInfo implements PacketInfo {
    private IPv4HeaderInfo ipv4HeaderInfo;
    private TCPHeaderInfo tcpHeaderInfo;

    public IPv4TCPPacketInfo(IPv4HeaderInfo ipv4HeaderInfo, TCPHeaderInfo tcpHeaderInfo) {
        this.ipv4HeaderInfo = ipv4HeaderInfo;
        this.tcpHeaderInfo = tcpHeaderInfo;
    }

    @Override
    public boolean isMatch(IPPacket packet) throws HeaderInfo.UnmatchedException {
        return ipv4HeaderInfo.isMatch(packet.ipHeader) &&
                tcpHeaderInfo.isMatch(packet.l4Header);
    }
}
