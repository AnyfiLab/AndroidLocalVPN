package me.joowon.androidlocalvpn.packet;

/**
 * Created by joowon on 2017. 10. 30..
 */

public abstract class L4Packet implements Packet {
    public abstract boolean isTCP();

    public abstract boolean isUDP();
}
