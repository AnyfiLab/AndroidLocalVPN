package me.joowon.androidlocalvpn.util;

import java.nio.ByteBuffer;

/**
 * Created by joowon on 2017. 11. 1..
 */

public class HexUtil {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static byte[] toBytes(String hex) {
        hex = hex.replace(" ", "").replace("\n", "");
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static String fromBytes(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String fromByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) return "";
        char[] hexChars = new char[byteBuffer.position() * 2];
        for (int j = 0; j < byteBuffer.position(); j++) {
            int v = byteBuffer.get(j) & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
