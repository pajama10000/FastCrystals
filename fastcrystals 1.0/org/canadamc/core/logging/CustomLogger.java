// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.logging;

import org.bukkit.plugin.Plugin;
import xyz.fxcilities.core.Core;

public class CustomLogger
{
    private BukkitLoggerOverride logger;
    private Core plugin;
    
    public CustomLogger(final Core plugin) {
        this.logger = new BukkitLoggerOverride((Plugin)plugin);
        this.plugin = plugin;
    }
    
    public void print(final boolean prefix, final String text) {
        final StringBuilder logRecord = new StringBuilder();
        if (prefix) {
            logRecord.append(this.plugin.getPrefix());
        }
        logRecord.append(text);
        this.logger.info(Chat.format(logRecord.toString()));
    }
    
    public void print(final String text) {
        this.print(false, text);
    }
    
    public Core getPlugin() {
        return this.plugin;
    }
}
