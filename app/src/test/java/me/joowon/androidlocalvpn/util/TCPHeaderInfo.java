package me.joowon.androidlocalvpn.util;

import me.joowon.androidlocalvpn.header.Header;
import me.joowon.androidlocalvpn.header.l4.TCPHeader;

/**
 * Created by joowon on 2017. 11. 1..
 */

public class TCPHeaderInfo extends HeaderInfo {
    public int srcPort;
    public int destPort;

    public long seqNum;
    public long ackNum;

    public byte dataOffset;
    public byte reserved;
    public byte ns;
    public byte flags;
    public int window;

    public int checksum;
    public int urgentPointer;

    public byte[] optionsAndPadding;

    @Override
    public boolean isMatch(Header header) throws UnmatchedException {
        TCPHeader tcpHeader = (TCPHeader) header;

        return checkMatch("srcPort", srcPort, tcpHeader.srcPort) &&
                checkMatch("srcPort", srcPort, tcpHeader.srcPort) &&
                checkMatch("destPort", destPort, tcpHeader.destPort) &&
                checkMatch("seqNum", seqNum, tcpHeader.seqNum) &&
                checkMatch("ackNum", ackNum, tcpHeader.ackNum) &&
                checkMatch("dataOffset", dataOffset, tcpHeader.dataOffset) &&
                checkMatch("reserved", reserved, tcpHeader.reserved) &&
                checkMatch("flags", flags, tcpHeader.flags) &&
                checkMatch("window", window, tcpHeader.window) &&
                checkMatch("checksum", checksum, tcpHeader.checksum) &&
                checkMatch("urgentPointer", urgentPointer, tcpHeader.urgentPointer) &&
                checkMatch("optionsAndPadding", optionsAndPadding, tcpHeader.optionsAndPadding);
    }
}
