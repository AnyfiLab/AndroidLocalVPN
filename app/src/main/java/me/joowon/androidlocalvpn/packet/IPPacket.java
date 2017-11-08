package me.joowon.androidlocalvpn.packet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import me.joowon.androidlocalvpn.header.l3.IPHeader;
import me.joowon.androidlocalvpn.header.l3.IPv4Header;
import me.joowon.androidlocalvpn.header.l4.L4Header;
import me.joowon.androidlocalvpn.header.l4.TCPHeader;
import me.joowon.androidlocalvpn.header.l4.UDPHeader;
import me.joowon.androidlocalvpn.util.ByteBufferPool;

/**
 * Created by joowon on 2017. 10. 23..
 */

public class IPPacket {
    public IPHeader.IPVersion ipVersion;

    public IPHeader ipHeader;
    public L4Header l4Header;
    public ByteBuffer backingBuffer;

    public IPPacket(ByteBuffer buffer) throws UnknownHostException {
        this.ipVersion = IPHeader.getVersion(buffer);
        if (ipVersion == IPHeader.IPVersion.Version4) {
            this.ipHeader = new IPv4Header(buffer);
            if (((IPv4Header) ipHeader).protocol == IPv4Header.TransportProtocol.TCP) {
                this.l4Header = new TCPHeader(buffer, ipHeader.getLength());
            } else if (((IPv4Header) ipHeader).protocol == IPv4Header.TransportProtocol.UDP) {
                this.l4Header = new UDPHeader(buffer, ipHeader.getLength());
            }
        } else if (ipVersion == IPHeader.IPVersion.Version6) {
            // TODO : support ipv6
        }

        this.backingBuffer = buffer;
    }

    public boolean isTCP() {
        return l4Header != null && l4Header instanceof TCPHeader;
    }

    public boolean isUDP() {
        return l4Header != null && l4Header instanceof UDPHeader;
    }

    public void fillHeader(ByteBuffer buffer) {
        ipHeader.fillHeader(buffer);
        l4Header.fillHeader(buffer);
    }

    public void changePayloadSize(int payloadSize) {
        l4Header.updatePayloadSize(ipHeader, payloadSize, backingBuffer);
        l4Header.fillHeader(backingBuffer);

        if (ipHeader instanceof IPv4Header) {
            short totalLength = IPv4Header.IPv4_HEADER_SIZE;
            if (l4Header instanceof TCPHeader)
                totalLength += TCPHeader.TCP_HEADER_SIZE;
            else if (l4Header instanceof UDPHeader)
                totalLength += UDPHeader.UDP_HEADER_SIZE;
            totalLength += payloadSize;

            ((IPv4Header) ipHeader).totalLength = totalLength;
            ipHeader.updateChecksum(backingBuffer);
        } else {
            // TODO : Support Ipv6
        }
    }

    public int getHeaderLength() {
        return ipHeader.getLength() + l4Header.getLength();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IPPacket ipPacket = (IPPacket) o;

        if (ipVersion != ipPacket.ipVersion) return false;
        if (ipHeader != null ? !ipHeader.equals(ipPacket.ipHeader) : ipPacket.ipHeader != null)
            return false;
        return l4Header != null ? l4Header.equals(ipPacket.l4Header) : ipPacket.l4Header == null;
    }

    @Override
    public int hashCode() {
        int result = ipVersion != null ? ipVersion.hashCode() : 0;
        result = 31 * result + (ipHeader != null ? ipHeader.hashCode() : 0);
        result = 31 * result + (l4Header != null ? l4Header.hashCode() : 0);
        return result;
    }
}
