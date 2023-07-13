// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.logging;

import java.util.logging.Level;
import org.bukkit.plugin.Plugin;
import java.util.logging.Logger;

public class BukkitLoggerOverride extends Logger
{
    public BukkitLoggerOverride(final Plugin context) {
        super(context.getClass().getCanonicalName(), null);
        this.setParent(context.getServer().getLogger());
        this.setLevel(Level.ALL);
    }
}
