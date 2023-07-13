// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.actionbar;

import net.md_5.bungee.api.chat.TextComponent;
import xyz.fxcilities.core.logging.Chat;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.entity.Player;

public class PlayerActionBar
{
    private final Player player;
    private String content;
    
    public PlayerActionBar(final Player player) {
        this.player = player;
    }
    
    public void setBar(final String content) {
        this.content = content;
    }
    
    public void sendBar() {
        this.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Chat.format(this.content)));
    }
    
    public Player getPlayer() {
        return this.player;
    }
}
