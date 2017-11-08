package me.joowon.androidlocalvpn.util;

import me.joowon.androidlocalvpn.header.Header;

/**
 * Created by joowon on 2017. 11. 1..
 */

public abstract class HeaderInfo {
    public abstract boolean isMatch(Header header) throws UnmatchedException;

    protected <V> boolean checkMatch(String param, V expected, V actual) throws UnmatchedException {
        if (!isEqual(expected, actual))
            throw new UnmatchedException(param, expected, actual);
        return true;
    }

    private <V> boolean isEqual(V v1, V v2) {
        if (v1 instanceof byte[]) {
            for (int i = 0; i < ((byte[]) v1).length; ++i) {
                if (((byte[]) v1)[i] == ((byte[]) v2)[i]) continue;
                return false;
            }
            return true;
        }
        if (v1 == null || v2 == null) {
            return v1 == null && v2 == null;
        }
        return v1.equals(v2);
    }

    public class UnmatchedException extends Exception {
        public <V> UnmatchedException(String param, V expected, V actual) {
            super(param + "(expected: " + expected + ", actual: " + actual + ")");
        }
    }
}
