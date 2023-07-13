// 
// Decompiled by Procyon v0.5.36
// 

package org.canadamc.fastcrystals.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.canadamc.fastcrystals.PlayerUtil;
import org.bukkit.Material;
import org.canadamc.fastcrystals.FastCrystals;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.Listener;

public class CrystalPlaceListener implements Listener
{
    @EventHandler
    public void onCrystalPlace(final PlayerInteractEvent event) {
        final Player p = event.getPlayer();
        if (!FastCrystals.enabled.get(p.getUniqueId())) {
            return;
        }
        if (event.getClickedBlock() != null && p.getInventory().getItemInMainHand().getType() == Material.END_CRYSTAL && (event.getClickedBlock().getType() == Material.OBSIDIAN || event.getClickedBlock().getType() == Material.BEDROCK) && PlayerUtil.getPing(p) > FastCrystals.config.getInt("ping")) {
            final Location loc = event.getClickedBlock().getLocation().add(0.5, 2.0, 0.5);
            if (loc.getWorld().getEntities().stream().noneMatch(e -> e.getLocation().equals((Object)loc))) {
                loc.getWorld().spawnEntity(loc, EntityType.ENDER_CRYSTAL);
                if (p.getGameMode() == GameMode.SURVIVAL) {
                    p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount() - 1);
                }
            }
        }
    }
}
