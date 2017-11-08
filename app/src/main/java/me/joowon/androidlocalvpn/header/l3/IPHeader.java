package me.joowon.androidlocalvpn.header.l3;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import me.joowon.androidlocalvpn.header.Header;

/**
 * Created by joowon on 2017. 10. 25..
 */

public abstract class IPHeader implements Header {
    public enum IPVersion {
        Version4((byte) 4),
        Version6((byte) 6),
        Other((byte) -1);

        byte version;

        IPVersion(byte version) {
            this.version = version;
        }

        private static IPVersion numberToEnum(int version) {
            if (version == 4)
                return Version4;
            else if (version == 6)
                return Version6;
            else
                return Other;
        }
    }

    public static IPVersion getVersion(ByteBuffer buffer) {
        byte versionAndIHL = buffer.get(0);
        return IPVersion.numberToEnum(versionAndIHL >> 4);
    }

    public abstract void updateChecksum(ByteBuffer buffer);

    public abstract InetAddress getSrcInetAddress();

    public abstract InetAddress getDstInetAddress();
}
