package me.joowon.androidlocalvpn.util;

import me.joowon.androidlocalvpn.packet.IPPacket;

/**
 * Created by joowon on 2017. 11. 1..
 */

public interface PacketInfo {
    boolean isMatch(IPPacket packet) throws HeaderInfo.UnmatchedException;
}
