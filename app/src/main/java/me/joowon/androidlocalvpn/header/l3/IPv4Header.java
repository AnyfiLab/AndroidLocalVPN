package me.joowon.androidlocalvpn.header.l3;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import me.joowon.androidlocalvpn.util.BitUtils;
import me.joowon.androidlocalvpn.util.ByteBufferPool;

/**
 * Created by joowon on 2017. 10. 23..
 */

public class IPv4Header extends IPHeader {
    public static final int IPv4_HEADER_SIZE = 20;

    public byte version;
    public byte IHL;
    public short typeOfService;
    public int totalLength;

    public int identification;
    public byte flags;
    public short fragmentOffset;

    public short TTL;
    private short protocolNum;
    public TransportProtocol protocol;
    public int headerChecksum;

    public InetAddress sourceAddress;
    public InetAddress destinationAddress;

    public int optionsAndPadding;

    public enum TransportProtocol {
        TCP(6),
        UDP(17),
        Other(0xFF);

        private int protocolNumber;

        TransportProtocol(int protocolNumber) {
            this.protocolNumber = protocolNumber;
        }

        private static TransportProtocol numberToEnum(int protocolNumber) {
            if (protocolNumber == 6)
                return TCP;
            else if (protocolNumber == 17)
                return UDP;
            else
                return Other;
        }

        public int getNumber() {
            return this.protocolNumber;
        }
    }

    public IPv4Header(ByteBuffer buffer) throws UnknownHostException {
        buffer.position(0);

        byte versionAndIHL = buffer.get();
        this.version = (byte) (versionAndIHL >> 4);
        this.IHL = (byte) ((versionAndIHL & 0b1111) << 2);

        this.typeOfService = BitUtils.getUnsignedByte(buffer.get());
        this.totalLength = BitUtils.getUnsignedShort(buffer.getShort());

        this.identification = BitUtils.getUnsignedShort(buffer.getShort());
        short flagsAndFragmentOffset = buffer.getShort();
        this.flags = (byte) (flagsAndFragmentOffset >> 13 & 0b111);
        this.fragmentOffset = (short) (flagsAndFragmentOffset & 0x1FFF);

        this.TTL = BitUtils.getUnsignedByte(buffer.get());
        this.protocolNum = BitUtils.getUnsignedByte(buffer.get());
        this.protocol = TransportProtocol.numberToEnum(protocolNum);
        this.headerChecksum = BitUtils.getUnsignedShort(buffer.getShort());

        byte[] addressBytes = new byte[4];
        buffer.get(addressBytes, 0, 4);
        this.sourceAddress = InetAddress.getByAddress(addressBytes);

        buffer.get(addressBytes, 0, 4);
        this.destinationAddress = InetAddress.getByAddress(addressBytes);
    }

    @Override
    public void fillHeader(ByteBuffer buffer) {
        buffer.position(0);

        buffer.put((byte) (this.version << 4 | (this.IHL >> 2)));
        buffer.put((byte) this.typeOfService);
        buffer.putShort((short) this.totalLength);

        buffer.putShort((short) identification);
        short flagsAndFragmentOffset = (short) (fragmentOffset | (flags << 13));
        buffer.putShort(flagsAndFragmentOffset);

        buffer.put((byte) this.TTL);
        buffer.put((byte) this.protocol.getNumber());
        buffer.putShort((short) this.headerChecksum);

        buffer.put(this.sourceAddress.getAddress());
        buffer.put(this.destinationAddress.getAddress());
    }

    @Override
    public int getLength() {
        return IPv4_HEADER_SIZE;
    }

    @Override
    public void updateChecksum(ByteBuffer buffer) {
        fillHeader(buffer);
        buffer.position(0);

        // Clear previous checksum
        buffer.putShort(10, (short) 0);

        int ipLength = IHL;
        int sum = 0;
        while (ipLength > 0) {
            sum += BitUtils.getUnsignedShort(buffer.getShort());
            ipLength -= 2;
        }
        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;
        headerChecksum = (short) sum;
    }

    @Override
    public InetAddress getSrcInetAddress() {
        return sourceAddress;
    }

    @Override
    public InetAddress getDstInetAddress() {
        return destinationAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IPv4Header that = (IPv4Header) o;

        if (version != that.version) return false;
        if (IHL != that.IHL) return false;
        if (typeOfService != that.typeOfService) return false;
        if (totalLength != that.totalLength) return false;
        if (identification != that.identification) return false;
        if (flags != that.flags) return false;
        if (fragmentOffset != that.fragmentOffset) return false;
        if (TTL != that.TTL) return false;
        if (protocolNum != that.protocolNum) return false;
        if (headerChecksum != that.headerChecksum) return false;
        if (optionsAndPadding != that.optionsAndPadding) return false;
        if (protocol != that.protocol) return false;
        if (sourceAddress != null ? !sourceAddress.equals(that.sourceAddress) : that.sourceAddress != null)
            return false;
        return destinationAddress != null ? destinationAddress.equals(that.destinationAddress) : that.destinationAddress == null;
    }

    @Override
    public int hashCode() {
        int result = (int) version;
        result = 31 * result + (int) IHL;
        result = 31 * result + (int) typeOfService;
        result = 31 * result + totalLength;
        result = 31 * result + identification;
        result = 31 * result + (int) flags;
        result = 31 * result + (int) fragmentOffset;
        result = 31 * result + (int) TTL;
        result = 31 * result + (int) protocolNum;
        result = 31 * result + (protocol != null ? protocol.hashCode() : 0);
        result = 31 * result + headerChecksum;
        result = 31 * result + (sourceAddress != null ? sourceAddress.hashCode() : 0);
        result = 31 * result + (destinationAddress != null ? destinationAddress.hashCode() : 0);
        result = 31 * result + optionsAndPadding;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("IPv4Header{");
        sb.append("version=").append(version).append(", ");
        sb.append("IHL=").append(IHL).append(", ");
        sb.append("typeOfService=").append(typeOfService).append(", ");
        sb.append("totalLength=").append(totalLength).append(", ");
        sb.append("identification=").append(identification).append(", ");
        sb.append("flags=").append(flags).append(", ");
        sb.append("fragmentOffset=").append(fragmentOffset).append(", ");
        sb.append("TTL=").append(TTL).append(", ");
        sb.append("protocol=").append(protocolNum).append(":").append(protocol).append(", ");
        sb.append("headerChecksum=").append(headerChecksum).append(", ");
        sb.append("sourceAddress=").append(sourceAddress.getHostAddress()).append(", ");
        sb.append("destinationAddress=").append(destinationAddress.getHostAddress()).append(", ");
        sb.append('}');
        return sb.toString();
    }
}
