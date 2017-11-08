package me.joowon.androidlocalvpn.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by joowon on 2017. 10. 26..
 */

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private int maxSize;
    private CleanupCallback<K, V> callback;

    public LRUCache(int maxSize, CleanupCallback<K, V> callback) {
        super(maxSize + 1, 1, true);

        this.maxSize = maxSize;
        this.callback = callback;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        if (size() > maxSize) {
            callback.onCleanup(eldest);
            return true;
        }
        return false;
    }

    public interface CleanupCallback<K, V> {
        void onCleanup(Entry<K, V> eldest);
    }
}
