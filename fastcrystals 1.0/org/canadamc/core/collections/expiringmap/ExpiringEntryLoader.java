// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.collections.expiringmap;

public interface ExpiringEntryLoader<K, V>
{
    ExpiringValue<V> load(final K p0);
}
