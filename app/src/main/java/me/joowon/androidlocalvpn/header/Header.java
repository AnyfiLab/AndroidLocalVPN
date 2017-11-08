package me.joowon.androidlocalvpn.header;

import java.nio.ByteBuffer;

/**
 * Created by joowon on 2017. 10. 24..
 */

public interface Header {
    void fillHeader(ByteBuffer buffer);

    int getLength();
}
