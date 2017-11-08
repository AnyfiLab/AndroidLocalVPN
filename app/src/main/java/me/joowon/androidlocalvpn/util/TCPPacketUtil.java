package me.joowon.androidlocalvpn.util;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import me.joowon.androidlocalvpn.header.l4.TCPHeader;
import me.joowon.androidlocalvpn.packet.IPPacket;

/**
 * Created by joowon on 2017. 10. 26..
 */

public class TCPPacketUtil {
    public static ByteBuffer newTCPBufferFromPacket(
            IPPacket packet, byte flags, int seqNum, int ackNum, int payloadSize) {
        ByteBuffer newBuffer = ByteBufferPool.acquire();
        packet.fillHeader(newBuffer);

        try {
            IPPacket tempPacket = new IPPacket(newBuffer);
            TCPHeader tempTCPHeader = (TCPHeader) tempPacket.l4Header;
            tempTCPHeader.flags = flags;
            tempTCPHeader.seqNum = seqNum;
            tempTCPHeader.ackNum = ackNum;
            tempTCPHeader.fillHeader(newBuffer);
            tempPacket.changePayloadSize(payloadSize);

            return newBuffer;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }
}
