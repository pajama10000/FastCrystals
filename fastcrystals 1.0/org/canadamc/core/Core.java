// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core;

import java.io.InputStream;
import org.bukkit.configuration.Configuration;
import java.io.Reader;
import java.io.InputStreamReader;
import com.google.common.base.Charsets;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Iterator;
import java.lang.reflect.Field;
import xyz.fxcilities.core.placeholders.PAPIExpansion;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.Bukkit;
import xyz.fxcilities.core.command.ServerCommand;
import java.util.ArrayList;
import xyz.fxcilities.core.logging.CustomLogger;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class Core extends JavaPlugin implements Global
{
    public static CustomLogger console;
    public static Core instance;
    public String notAPlayerMessage;
    public String onCooldownMessage;
    public ArrayList<ServerCommand> commands;
    
    public Core() {
        this.notAPlayerMessage = "{PREFIX}&c&lYou must be a player to run this command!";
        this.onCooldownMessage = "{PREFIX}&cYou are on a cooldown! You may run this command again in &l{TIME}";
        this.commands = new ArrayList<ServerCommand>();
    }
    
    public void onEnable() {
        Core.console = new CustomLogger(this);
        (Core.instance = this).onPluginEnable();
        CommandMap commandMap;
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            commandMap = (CommandMap)bukkitCommandMap.get(Bukkit.getServer());
        }
        catch (NoSuchFieldException | SecurityException | IllegalAccessException ex2) {
            final Exception ex;
            final Exception e = ex;
            e.printStackTrace();
            return;
        }
        for (final ServerCommand command : this.commands) {
            commandMap.register(command.getLabel(), (Command)command);
            Core.console.print(true, invokedynamic(makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;, command.getLabel()));
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Core.console.print(true, "Found PlaceholderAPI, registering placeholders");
            for (final PAPIExpansion expansion : PAPIExpansion.expansions) {
                expansion.register();
            }
        }
    }
    
    public void onDisable() {
        this.onPluginDisable();
    }
    
    public abstract void onPluginEnable();
    
    public abstract void onPluginDisable();
    
    public abstract String getPrefix();
    
    public abstract String getPluginVersion();
    
    public abstract String getPluginName();
    
    public abstract String[] getPluginAuthors();
    
    public void setNotAPlayerMessage(final String message) {
        this.notAPlayerMessage = message;
    }
    
    public void setOnCooldownMessage(final String message) {
        this.onCooldownMessage = message;
    }
    
    public FileConfiguration loadConfig(final String fileName) {
        Checks.nonNull(fileName, "The fileName argument");
        final FileConfiguration config = (FileConfiguration)YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), fileName));
        this.saveResource(fileName, false);
        final InputStream stream = this.getResource(fileName);
        Checks.check(stream == null, "Failed to open a InputStream from the argument fileName");
        config.setDefaults((Configuration)YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(stream, Charsets.UTF_8)));
        config.options().copyDefaults(true);
        return config;
    }
    
    public static Core getInstance() {
        return Core.instance;
    }
}
