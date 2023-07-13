// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.collections.expiringmap;

import java.util.concurrent.TimeUnit;

public final class ExpiringValue<V>
{
    private static final long UNSET_DURATION = -1L;
    private final V value;
    private final ExpirationPolicy expirationPolicy;
    private final long duration;
    private final TimeUnit timeUnit;
    
    public ExpiringValue(final V value) {
        this(value, -1L, null, null);
    }
    
    public ExpiringValue(final V value, final ExpirationPolicy expirationPolicy) {
        this(value, -1L, null, expirationPolicy);
    }
    
    public ExpiringValue(final V value, final long duration, final TimeUnit timeUnit) {
        this(value, duration, timeUnit, null);
        if (timeUnit == null) {
            throw new NullPointerException();
        }
    }
    
    public ExpiringValue(final V value, final ExpirationPolicy expirationPolicy, final long duration, final TimeUnit timeUnit) {
        this(value, duration, timeUnit, expirationPolicy);
        if (timeUnit == null) {
            throw new NullPointerException();
        }
    }
    
    private ExpiringValue(final V value, final long duration, final TimeUnit timeUnit, final ExpirationPolicy expirationPolicy) {
        this.value = value;
        this.expirationPolicy = expirationPolicy;
        this.duration = duration;
        this.timeUnit = timeUnit;
    }
    
    public V getValue() {
        return this.value;
    }
    
    public ExpirationPolicy getExpirationPolicy() {
        return this.expirationPolicy;
    }
    
    public long getDuration() {
        return this.duration;
    }
    
    public TimeUnit getTimeUnit() {
        return this.timeUnit;
    }
    
    @Override
    public int hashCode() {
        return (this.value != null) ? this.value.hashCode() : 0;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final ExpiringValue<?> that = (ExpiringValue<?>)o;
        if (this.value != null) {
            if (!this.value.equals(that.value)) {
                return false;
            }
        }
        else if (that.value != null) {
            return false;
        }
        if (this.expirationPolicy == that.expirationPolicy && this.duration == that.duration && this.timeUnit == that.timeUnit) {
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return invokedynamic(makeConcatWithConstants:(Ljava/lang/Object;Lxyz/fxcilities/core/collections/expiringmap/ExpirationPolicy;JLjava/util/concurrent/TimeUnit;)Ljava/lang/String;, this.value, this.expirationPolicy, this.duration, this.timeUnit);
    }
}
