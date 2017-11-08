package me.joowon.androidlocalvpn.packet;

import java.nio.ByteBuffer;

import me.joowon.androidlocalvpn.header.l4.TCPHeader;
import me.joowon.androidlocalvpn.tcp.TCB;
import me.joowon.androidlocalvpn.util.HexUtil;

/**
 * Created by joowon on 2017. 10. 30..
 */

public class TCPPacket extends L4Packet {
    public final TCB tcb;
    public TCPHeader tcpHeader;

    public IPPacket ipPacket;

    public TCPPacket(TCB tcb, IPPacket ipPacket) {
        this.tcb = tcb;
        this.ipPacket = ipPacket;
        this.tcpHeader = (TCPHeader) ipPacket.l4Header;
    }

    @Override
    public boolean isTCP() {
        return true;
    }

    @Override
    public boolean isUDP() {
        return false;
    }

    public void updateBuffer(ByteBuffer buffer, byte flags, long seqNum, long ackNum, int payloadSize) {
        ipPacket.backingBuffer = buffer;
        ipPacket.fillHeader(ipPacket.backingBuffer);

        tcpHeader.flags = flags;
        tcpHeader.seqNum = seqNum;
        tcpHeader.ackNum = ackNum;
        tcpHeader.dataOffset = TCPHeader.TCP_HEADER_SIZE;

        ipPacket.changePayloadSize(payloadSize);

        ipPacket.fillHeader(buffer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TCPPacket tcpPacket = (TCPPacket) o;

        if (tcb != null ? !tcb.equals(tcpPacket.tcb) : tcpPacket.tcb != null) return false;
        if (tcpHeader != null ? !tcpHeader.equals(tcpPacket.tcpHeader) : tcpPacket.tcpHeader != null)
            return false;
        return ipPacket != null ? ipPacket.equals(tcpPacket.ipPacket) : tcpPacket.ipPacket == null;
    }

    @Override
    public int hashCode() {
        int result = tcb != null ? tcb.hashCode() : 0;
        result = 31 * result + (tcpHeader != null ? tcpHeader.hashCode() : 0);
        result = 31 * result + (ipPacket != null ? ipPacket.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        String flag = "";
        if (tcpHeader.isSYN()) {
            flag += "SYN";
            if (tcpHeader.isACK()) {
                flag += "|ACK";
            }
        } else if (tcpHeader.isRST()) {
            flag += "RST";
        } else if (tcpHeader.isFIN()) {
            flag += "FIN";
        } else if (tcpHeader.isPSH()) {
            flag += "PSH";
            if (tcpHeader.isACK()) {
                flag += "|ACK";
            }
        } else if (tcpHeader.isACK()) {
            flag += "ACK";
        }

        return String.format("(18%s %4s) %7s [%d, %d] %s",
                tcb.key.dstIp.toString(), tcb.key.dstPort,
                flag, tcpHeader.seqNum, tcpHeader.ackNum,
                HexUtil.fromByteBuffer(ipPacket.backingBuffer));
    }
}
