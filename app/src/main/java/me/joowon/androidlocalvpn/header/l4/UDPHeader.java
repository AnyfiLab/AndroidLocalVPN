package me.joowon.androidlocalvpn.header.l4;

import java.nio.ByteBuffer;

import me.joowon.androidlocalvpn.header.l3.IPHeader;
import me.joowon.androidlocalvpn.util.BitUtils;

/**
 * Created by joowon on 2017. 10. 24..
 */

public class UDPHeader extends L4Header {
    public static final int UDP_HEADER_SIZE = 8;

    public int srcPort;
    public int destPort;

    public int length;
    public int checksum;

    private int startPosition;

    public UDPHeader(ByteBuffer buffer, int startPosition) {
        this.startPosition = startPosition;
        buffer.position(startPosition);

        this.srcPort = BitUtils.getUnsignedShort(buffer.getShort());
        this.destPort = BitUtils.getUnsignedShort(buffer.getShort());

        this.length = BitUtils.getUnsignedShort(buffer.getShort());
        this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
    }

    @Override
    public void fillHeader(ByteBuffer buffer) {
        buffer.position(startPosition);

        buffer.putShort((short) this.srcPort);
        buffer.putShort((short) this.destPort);

        buffer.putShort((short) this.length);
        buffer.putShort((short) this.checksum);
    }

    @Override
    public int getLength() {
        return UDP_HEADER_SIZE;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UDPHeader{");
        sb.append("srcPort=").append(srcPort).append(", ");
        sb.append("destPort=").append(destPort).append(", ");
        sb.append("length=").append(length).append(", ");
        sb.append("checksum=").append(checksum).append(", ");
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void updatePayloadSize(IPHeader ipHeader, int payloadSize, ByteBuffer referencePacketBuf) {
        fillHeader(referencePacketBuf);

        this.length = UDPHeader.UDP_HEADER_SIZE + payloadSize;
        this.checksum = 0;  // Disable UDP checksum validation
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UDPHeader udpHeader = (UDPHeader) o;

        if (srcPort != udpHeader.srcPort) return false;
        if (destPort != udpHeader.destPort) return false;
        if (length != udpHeader.length) return false;
        if (checksum != udpHeader.checksum) return false;
        return startPosition == udpHeader.startPosition;
    }

    @Override
    public int hashCode() {
        int result = srcPort;
        result = 31 * result + destPort;
        result = 31 * result + length;
        result = 31 * result + checksum;
        result = 31 * result + startPosition;
        return result;
    }
}
