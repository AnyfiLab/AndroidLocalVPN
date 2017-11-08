package me.joowon.androidlocalvpn.header.l4;

import java.nio.ByteBuffer;

import me.joowon.androidlocalvpn.header.Header;
import me.joowon.androidlocalvpn.header.l3.IPHeader;

/**
 * Created by joowon on 2017. 10. 25..
 */

public abstract class L4Header implements Header {
    public abstract void updatePayloadSize(IPHeader ipHeader, int payloadSize, ByteBuffer referencePacketBuf);
}
