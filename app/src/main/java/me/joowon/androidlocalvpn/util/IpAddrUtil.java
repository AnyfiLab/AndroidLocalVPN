package me.joowon.androidlocalvpn.util;

import java.net.InetAddress;

/**
 * Created by joowon on 2017. 10. 30..
 */

public class IpAddrUtil {
    public static int convertInetAddrToInt(InetAddress inetAddress) {
        int result = 0;
        for (byte b : inetAddress.getAddress()) {
            result = result << 8 | (b & 0xFF);
        }
        return result;
    }
}
