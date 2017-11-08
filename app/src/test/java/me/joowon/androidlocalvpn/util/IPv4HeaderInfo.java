package me.joowon.androidlocalvpn.util;

import java.net.InetAddress;

import me.joowon.androidlocalvpn.header.Header;
import me.joowon.androidlocalvpn.header.l3.IPv4Header;

/**
 * Created by joowon on 2017. 11. 1..
 */

public class IPv4HeaderInfo extends HeaderInfo {
    public byte version;
    public byte IHL;
    public short typeOfService;
    public int totalLength;

    public int identification;
    public byte flags;
    public short fragmentOffset;

    public short TTL;
    public IPv4Header.TransportProtocol protocol;
    public int headerChecksum;

    public InetAddress sourceAddress;
    public InetAddress destinationAddress;

    public int optionsAndPadding;

    @Override
    public boolean isMatch(Header header) throws UnmatchedException {
        IPv4Header ipv4Header = (IPv4Header) header;

        return checkMatch("version", version, ipv4Header.version) &&
                checkMatch("IHL", IHL, ipv4Header.IHL) &&
                checkMatch("typeOfService", typeOfService, ipv4Header.typeOfService) &&
                checkMatch("totalLength", totalLength, ipv4Header.totalLength) &&
                checkMatch("identification", identification, ipv4Header.identification) &&
                checkMatch("flags", flags, ipv4Header.flags) &&
                checkMatch("fragmentOffset", fragmentOffset, ipv4Header.fragmentOffset) &&
                checkMatch("TTL", TTL, ipv4Header.TTL) &&
                checkMatch("protocol", protocol, ipv4Header.protocol) &&
                checkMatch("headerChecksum", headerChecksum, ipv4Header.headerChecksum) &&
                checkMatch("sourceAddress", sourceAddress, ipv4Header.sourceAddress) &&
                checkMatch("destinationAddress", destinationAddress, ipv4Header.destinationAddress) &&
                checkMatch("optionsAndPadding", optionsAndPadding, ipv4Header.optionsAndPadding);

    }
}
