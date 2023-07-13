// 
// Decompiled by Procyon v0.5.36
// 

package org.canadamc.fastcrystals.commands;

import org.canadamc.fastcrystals.FastCrystals;
import org.bukkit.entity.Player;
import java.util.concurrent.TimeUnit;
import org.canadamc.core.command.ServerCommand;

public class CrystalCommand extends ServerCommand
{
    public CrystalCommand(final String label) {
        super(label, "Toggle fastcrystaling for yourself", invokedynamic(makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;, label), true);
        this.setCooldownDuration(0L, TimeUnit.SECONDS);
        this.addTabCompleteArgs("on", "off");
    }
    
    @Override
    public void onCommand() {
        final Player p = (Player)this.sender;
        if (this.args.length == 1) {
            if (this.args[0].equalsIgnoreCase("off")) {
                FastCrystals.enabled.put(p.getUniqueId(), false);
            }
            else if (this.args[0].equalsIgnoreCase("on")) {
                FastCrystals.enabled.put(p.getUniqueId(), true);
            }
        }
        else {
            FastCrystals.enabled.put(p.getUniqueId(), !FastCrystals.enabled.get(p.getUniqueId()));
        }
        this.sendMessage(p);
    }
    
    private void sendMessage(final Player p) {
        final String key = FastCrystals.enabled.get(p.getUniqueId()) ? "enable_message" : "disabled_message";
        p.sendMessage(FastCrystals.messages.getString(key));
    }
}
