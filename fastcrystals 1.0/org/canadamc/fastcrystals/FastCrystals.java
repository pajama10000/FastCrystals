// 
// Created by pajama
// 

package org.canadamc.fastcrystals;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import org.canadamc.fastcrystals.listeners.CrystalPlaceListener;
import org.bukkit.Bukkit;
import org.canadamc.fastcrystals.commands.CrystalCommand;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.UUID;
import java.util.HashMap;
import org.canadamc.core.Core;

public final class FastCrystals extends Core
{
    public static HashMap<UUID, Boolean> enabled;
    public static FileConfiguration config;
    public static FileConfiguration messages;
    
    @Override
    public void onPluginEnable() {
        FastCrystals.config = this.loadConfig("settings.yml");
        FastCrystals.messages = this.loadConfig("messages.yml");
        this.setNotAPlayerMessage(FastCrystals.messages.getString("not_a_player_message"));
        new CrystalCommand(FastCrystals.config.getString("toggle_command"));
        Bukkit.getPluginManager().registerEvents((Listener)new CrystalPlaceListener(), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new CrystalPlaceListener(), (Plugin)this);
    }
    
    @Override
    public void onPluginDisable() {
    }
    
    @Override
    public String getPrefix() {
        return (FastCrystals.messages == null) ? "" : FastCrystals.messages.getString("prefix");
    }
    
    @Override
    public String getPluginVersion() {
        return "v0.1";
    }
    
    @Override
    public String getPluginName() {
        return "FastCrystals";
    }
    
    @Override
    public String[] getPluginAuthors() {
        return new String[] { "Fxcilities", "pajamaMC" };
    }
    
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        FastCrystals.enabled.put(event.getPlayer().getUniqueId(), true);
    }
    
    static {
        FastCrystals.enabled = new HashMap<UUID, Boolean>();
    }
}
