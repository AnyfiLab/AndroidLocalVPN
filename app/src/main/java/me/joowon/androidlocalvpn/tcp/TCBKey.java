package me.joowon.androidlocalvpn.tcp;

import java.net.InetAddress;

import me.joowon.androidlocalvpn.util.IpAddrUtil;

/**
 * Created by joowon on 2017. 10. 26..
 */

public class TCBKey {
    public int srcPort;
    public InetAddress dstIp;
    public int dstPort;

    public TCBKey(int srcPort, InetAddress dstIp, int dstPort) {
        this.srcPort = srcPort;
        this.dstIp = dstIp;
        this.dstPort = dstPort;
    }

    @Override
    public String toString() {
        return "TCBKey{" +
                "srcPort=" + srcPort +
                ", dstIp=" + dstIp +
                ", dstPort=" + dstPort +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TCBKey tcbKey = (TCBKey) o;

        return srcPort == tcbKey.srcPort &&
                dstIp.equals(tcbKey.dstIp) &&
                dstPort == tcbKey.dstPort;
    }

    @Override
    public int hashCode() {
        int result = (int) srcPort;
        result = 31 * result + dstIp.hashCode();
        result = 31 * result + (int) dstPort;
        return result;
    }
}
