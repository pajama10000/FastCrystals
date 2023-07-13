// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.logging;

import java.util.Iterator;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import xyz.fxcilities.core.Core;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

public class Chat
{
    public static String format(final String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public static void say(final boolean showPrefix, final CommandSender sender, final String text) {
        final StringBuilder content = new StringBuilder();
        if (showPrefix) {
            content.append(Core.getInstance().getPrefix());
        }
        content.append(text);
        sender.sendMessage(format(content.toString()));
    }
    
    public static void say(final String permission, final boolean showPrefix, final String text) {
        final StringBuilder content = new StringBuilder();
        if (showPrefix) {
            content.append(Core.getInstance().getPrefix());
        }
        content.append(text);
        final String newText = format(content.toString());
        for (final Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(newText);
            }
        }
    }
    
    public static void say(final String permission, final String text) {
        say(permission, true, text);
    }
}
