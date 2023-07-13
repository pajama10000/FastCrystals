// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.placeholders.handlers;

import org.bukkit.entity.Player;

public interface ExtensionHandler
{
    String onRequest(final Player p0, final String p1);
    
    String getPrefix();
}
