// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.collections.expiringmap;

public interface ExpirationListener<K, V>
{
    void expired(final K p0, final V p1);
}
