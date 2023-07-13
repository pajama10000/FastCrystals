// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.placeholders;

import java.util.ArrayList;
import org.bukkit.entity.Player;
import xyz.fxcilities.core.Core;
import xyz.fxcilities.core.placeholders.handlers.ExtensionHandler;
import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PAPIExpansion extends PlaceholderExpansion
{
    public static List<PAPIExpansion> expansions;
    private final ExtensionHandler manager;
    
    public PAPIExpansion(final ExtensionHandler manager) {
        this.manager = manager;
        PAPIExpansion.expansions.add(this);
    }
    
    public String getIdentifier() {
        return this.manager.getPrefix();
    }
    
    public String getAuthor() {
        return String.join(", ", Core.getInstance().getDescription().getAuthors());
    }
    
    public String getVersion() {
        return Core.getInstance().getPluginVersion();
    }
    
    public String onPlaceholderRequest(final Player player, final String params) {
        return this.manager.onRequest(player, params);
    }
    
    static {
        PAPIExpansion.expansions = new ArrayList<PAPIExpansion>();
    }
}
