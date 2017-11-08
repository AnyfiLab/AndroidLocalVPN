package me.joowon.androidlocalvpn.packet;

import java.nio.ByteBuffer;

import me.joowon.androidlocalvpn.header.l3.IPHeader;
import me.joowon.androidlocalvpn.header.l4.UDPHeader;

/**
 * Created by joowon on 2017. 11. 3..
 */

public class UDPPacket extends L4Packet {
    public UDPHeader udpHeader;
    public IPPacket ipPacket;

    public UDPPacket(IPPacket ipPacket) {
        this.ipPacket = ipPacket;
        this.udpHeader = (UDPHeader) ipPacket.l4Header;
    }

    public void updateBuffer(ByteBuffer buffer, int payloadSize) {
        ipPacket.fillHeader(buffer);
        ipPacket.backingBuffer = buffer;

        udpHeader.updatePayloadSize(ipPacket.ipHeader, payloadSize, buffer);
        ipPacket.changePayloadSize(payloadSize);

        ipPacket.fillHeader(buffer);
    }

    @Override
    public boolean isTCP() {
        return false;
    }

    @Override
    public boolean isUDP() {
        return true;
    }
}
