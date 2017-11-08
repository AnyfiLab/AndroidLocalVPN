package me.joowon.androidlocalvpn.header.l4;

import java.nio.ByteBuffer;
import java.util.Arrays;

import me.joowon.androidlocalvpn.header.l3.IPHeader;
import me.joowon.androidlocalvpn.header.l3.IPv4Header;
import me.joowon.androidlocalvpn.util.BitUtils;

/**
 * Created by joowon on 2017. 10. 24..
 */

public class TCPHeader extends L4Header {
    public static final int TCP_HEADER_SIZE = 20;

    public static final int FIN = 0x01;
    public static final int SYN = 0x02;
    public static final int RST = 0x04;
    public static final int PSH = 0x08;
    public static final int ACK = 0x10;
    public static final int URG = 0x20;

    public int srcPort;
    public int destPort;

    public long seqNum;
    public long ackNum;

    public byte dataOffset;
    public byte reserved;
    public byte ns;
    public byte flags;
    public int window;

    public int checksum;
    public int urgentPointer;

    public byte[] optionsAndPadding;

    private int startPosition;

    public TCPHeader(ByteBuffer buffer, int startPosition) {
        this.startPosition = startPosition;
        buffer.position(startPosition);

        this.srcPort = BitUtils.getUnsignedShort(buffer.getShort());
        this.destPort = BitUtils.getUnsignedShort(buffer.getShort());

        this.seqNum = BitUtils.getUnsignedInt(buffer.getInt());
        this.ackNum = BitUtils.getUnsignedInt(buffer.getInt());

        int dataOffsetReservedNs = buffer.get();
        this.dataOffset = (byte) ((dataOffsetReservedNs >> 4 & 0b1111) << 2);
        this.reserved = (byte) (dataOffsetReservedNs >> 1 & 0b111);
        this.ns = (byte) (dataOffsetReservedNs & 0b1);
        this.flags = buffer.get();
        this.window = BitUtils.getUnsignedShort(buffer.getShort());

        this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
        this.urgentPointer = BitUtils.getUnsignedShort(buffer.getShort());

        int optionsLength = this.dataOffset - TCP_HEADER_SIZE;
        if (optionsLength > 0) {
            optionsAndPadding = new byte[optionsLength];
            buffer.get(optionsAndPadding, 0, optionsLength);
        }
    }

    public boolean isFIN() {
        return (flags & FIN) == FIN;
    }

    public boolean isSYN() {
        return (flags & SYN) == SYN;
    }

    public boolean isRST() {
        return (flags & RST) == RST;
    }

    public boolean isPSH() {
        return (flags & PSH) == PSH;
    }

    public boolean isACK() {
        return (flags & ACK) == ACK;
    }

    public boolean isURG() {
        return (flags & URG) == URG;
    }

    @Override
    public void fillHeader(ByteBuffer buffer) {
        buffer.position(startPosition);

        buffer.putShort((short) srcPort);
        buffer.putShort((short) destPort);

        buffer.putInt((int) seqNum);
        buffer.putInt((int) ackNum);

        byte dataOffsetReservedNs = ns;
        dataOffsetReservedNs |= reserved << 1;
        dataOffsetReservedNs |= (dataOffset >> 2) << 4;
        buffer.put(dataOffsetReservedNs);
        buffer.put(flags);
        buffer.putShort((short) window);

        buffer.putShort((short) checksum);
        buffer.putShort((short) urgentPointer);
    }

    @Override
    public int getLength() {
        return TCP_HEADER_SIZE;
    }

    @Override
    public void updatePayloadSize(IPHeader ipHeader, int payloadSize, ByteBuffer referencePacketBuf) {
        fillHeader(referencePacketBuf);

        int sum = 0;
        int tcpLength = TCP_HEADER_SIZE + payloadSize;

        if (ipHeader instanceof IPv4Header) {
            // Calculate pseudo-header checksum
            IPv4Header ipv4Header = (IPv4Header) ipHeader;

            ByteBuffer buffer = ByteBuffer.wrap(ipv4Header.sourceAddress.getAddress());
            sum = BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());

            buffer = ByteBuffer.wrap(ipv4Header.destinationAddress.getAddress());
            sum += BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());

            sum += IPv4Header.TransportProtocol.TCP.getNumber() + tcpLength;

            buffer = referencePacketBuf.duplicate();
            buffer.putShort(IPv4Header.IPv4_HEADER_SIZE + 16, (short) 0); // Clear previous checksum

            // Calculate TCP segment checksum
            buffer.position(IPv4Header.IPv4_HEADER_SIZE);
            while (tcpLength > 1) {
                sum += BitUtils.getUnsignedShort(buffer.getShort());
                tcpLength -= 2;
            }
            if (tcpLength > 0)
                sum += BitUtils.getUnsignedByte(buffer.get()) << 8;

            while (sum >> 16 > 0)
                sum = (sum & 0xFFFF) + (sum >> 16);

            sum = ~sum;
            this.checksum = (short) sum;
            referencePacketBuf.putShort(IPv4Header.IPv4_HEADER_SIZE + 16, (short) sum);
        } else {
            // TODO : Support IPv6
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TCPHeader tcpHeader = (TCPHeader) o;

        if (srcPort != tcpHeader.srcPort) return false;
        if (destPort != tcpHeader.destPort) return false;
        if (seqNum != tcpHeader.seqNum) return false;
        if (ackNum != tcpHeader.ackNum) return false;
        if (dataOffset != tcpHeader.dataOffset) return false;
        if (reserved != tcpHeader.reserved) return false;
        if (ns != tcpHeader.ns) return false;
        if (flags != tcpHeader.flags) return false;
        if (window != tcpHeader.window) return false;
        if (checksum != tcpHeader.checksum) return false;
        if (urgentPointer != tcpHeader.urgentPointer) return false;
        if (startPosition != tcpHeader.startPosition) return false;
        return Arrays.equals(optionsAndPadding, tcpHeader.optionsAndPadding);
    }

    @Override
    public int hashCode() {
        int result = srcPort;
        result = 31 * result + destPort;
        result = 31 * result + (int) (seqNum ^ (seqNum >>> 32));
        result = 31 * result + (int) (ackNum ^ (ackNum >>> 32));
        result = 31 * result + (int) dataOffset;
        result = 31 * result + (int) reserved;
        result = 31 * result + (int) ns;
        result = 31 * result + (int) flags;
        result = 31 * result + window;
        result = 31 * result + checksum;
        result = 31 * result + urgentPointer;
        result = 31 * result + Arrays.hashCode(optionsAndPadding);
        result = 31 * result + startPosition;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TCPHeader{");
        sb.append("srcPort=").append(srcPort).append(", ");
        sb.append("destPort=").append(destPort).append(", ");
        sb.append("seqNum=").append(seqNum).append(", ");
        sb.append("ackNum=").append(ackNum).append(", ");
        sb.append("dataOffset=").append(dataOffset).append(", ");
        sb.append("reserved=").append(reserved).append(", ");
        sb.append("ns=").append(ns).append(", ");
        sb.append("window=").append(window).append(", ");
        sb.append("checksum=").append(checksum).append(", ");
        sb.append("flags");
        if (isFIN()) sb.append(" FIN");
        if (isSYN()) sb.append(" SYN");
        if (isRST()) sb.append(" RST");
        if (isPSH()) sb.append(" PSH");
        if (isACK()) sb.append(" ACK");
        if (isURG()) sb.append(" URG");
        sb.append("}");
        return sb.toString();
    }
}
