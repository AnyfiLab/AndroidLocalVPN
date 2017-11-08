package me.joowon.androidlocalvpn.tcp;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;

import me.joowon.androidlocalvpn.util.LRUCache;

/**
 * Created by joowon on 2017. 10. 26..
 */

public class TCB {
    public TCBKey key;

    public long seqNum, devSeqNum;
    public long ackNum, devAckNum;
    public TCBStatus status;

    public enum TCBStatus {
        SYN_SENT,
        SYN_RECEIVED,
        ESTABLISHED,
        CLOSE_WAIT,
        LAST_ACK,
    }

    public SocketChannel channel;
    public boolean waitingForNetData;
    public SelectionKey selectionKey;

    private static final int MAX_CACHE_SIZE = 50;
    private static final LRUCache<TCBKey, TCB> tcbCache =
            new LRUCache<>(MAX_CACHE_SIZE, new LRUCache.CleanupCallback<TCBKey, TCB>() {
                @Override
                public void onCleanup(Map.Entry<TCBKey, TCB> eldest) {
                    eldest.getValue().closeChannel();
                }
            });

    public static TCB getTCB(TCBKey key) {
        synchronized (tcbCache) {
            return tcbCache.get(key);
        }
    }

    public static void closeTCB(TCB tcb) {
        tcb.closeChannel();

        synchronized (tcbCache) {
            tcbCache.remove(tcb.key);
        }
    }

    public static void closeAllTCB() {
        synchronized (tcbCache) {
            Iterator<Map.Entry<TCBKey, TCB>> it = tcbCache.entrySet().iterator();
            while (it.hasNext()) {
                it.next().getValue().closeChannel();
                it.remove();
            }
        }
    }

    public static TCB newTCB(TCBKey key, long srcSeqNum, long dstSeqNum, long srcAckNum, long dstAckNum, SocketChannel channel) {
        TCB tcb = new TCB(key, srcSeqNum, dstSeqNum, srcAckNum, dstAckNum, channel);

        synchronized (tcbCache) {
            tcbCache.put(key, tcb);
        }

        return tcb;
    }


    private TCB(TCBKey key, long seqNum, long devSeqNum, long ackNum, long devAckNum, SocketChannel channel) {
        this.key = key;

        this.seqNum = seqNum;
        this.devSeqNum = devSeqNum;
        this.ackNum = ackNum;
        this.devAckNum = devAckNum;

        this.channel = channel;
    }

    private void closeChannel() {
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TCB tcb = (TCB) o;

        if (seqNum != tcb.seqNum) return false;
        if (devSeqNum != tcb.devSeqNum) return false;
        if (ackNum != tcb.ackNum) return false;
        if (devAckNum != tcb.devAckNum) return false;
        if (key != null ? !key.equals(tcb.key) : tcb.key != null) return false;
        return status == tcb.status;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (int) (seqNum ^ (seqNum >>> 32));
        result = 31 * result + (int) (devSeqNum ^ (devSeqNum >>> 32));
        result = 31 * result + (int) (ackNum ^ (ackNum >>> 32));
        result = 31 * result + (int) (devAckNum ^ (devAckNum >>> 32));
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TCB{" +
                "key=" + key +
                ", seqNum=" + seqNum +
                ", devSeqNum=" + devSeqNum +
                ", ackNum=" + ackNum +
                ", devAckNum=" + devAckNum +
                ", status=" + status +
                '}';
    }
}
