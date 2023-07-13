// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.collections.expiringmap;

public enum ExpirationPolicy
{
    ACCESSED, 
    CREATED;
    
    private static /* synthetic */ ExpirationPolicy[] $values() {
        return new ExpirationPolicy[] { ExpirationPolicy.ACCESSED, ExpirationPolicy.CREATED };
    }
    
    static {
        $VALUES = $values();
    }
}
