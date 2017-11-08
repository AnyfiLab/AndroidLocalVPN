package me.joowon.androidlocalvpn.util;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import me.joowon.androidlocalvpn.packet.IPPacket;

/**
 * Created by joowon on 2017. 11. 1..
 */

public class PacketValidator {
    private IPPacket packet;

    public PacketValidator(String hex) {
        try {
            this.packet = new IPPacket(ByteBuffer.wrap(HexUtil.toBytes(hex)));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public PacketValidator(ByteBuffer buffer) {
        try {
            this.packet = new IPPacket(buffer);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public boolean validate(PacketInfo info) throws HeaderInfo.UnmatchedException {
        return info.isMatch(packet);
    }
}
