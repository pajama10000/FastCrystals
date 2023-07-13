// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.collections.expiringmap.internal;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory
{
    private final AtomicInteger threadNumber;
    private final String nameFormat;
    
    public NamedThreadFactory(final String nameFormat) {
        this.threadNumber = new AtomicInteger(1);
        this.nameFormat = nameFormat;
    }
    
    @Override
    public Thread newThread(final Runnable r) {
        final Thread thread = new Thread(r, String.format(this.nameFormat, this.threadNumber.getAndIncrement()));
        thread.setDaemon(true);
        return thread;
    }
}
