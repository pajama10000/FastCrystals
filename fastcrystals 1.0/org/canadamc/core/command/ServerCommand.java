// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.command;

import xyz.fxcilities.core.logging.Chat;
import java.util.Iterator;
import org.bukkit.entity.Player;
import java.util.Collection;
import java.util.Arrays;
import java.util.Collections;
import xyz.fxcilities.core.Core;
import java.util.ArrayList;
import java.util.UUID;
import xyz.fxcilities.core.collections.expiringmap.ExpiringMap;
import java.util.concurrent.TimeUnit;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

public abstract class ServerCommand extends BukkitCommand
{
    private final boolean playerOnly;
    protected CommandSender sender;
    protected String[] args;
    public List<ServerSubCommand> subCommands;
    public List<String> tabCompleteArgs;
    private long cooldownDuration;
    private TimeUnit cooldownTimeUnit;
    protected ExpiringMap<UUID, Long> cooldownMap;
    
    public ServerCommand(final String label, final String description, final String usage, final boolean playerOnly, final List<String> aliases) {
        super(label, description, usage, (List)aliases);
        this.subCommands = new ArrayList<ServerSubCommand>();
        this.tabCompleteArgs = new ArrayList<String>();
        this.cooldownDuration = 30L;
        this.cooldownTimeUnit = TimeUnit.SECONDS;
        this.cooldownMap = ExpiringMap.builder().expiration(this.cooldownDuration, this.cooldownTimeUnit).build();
        this.playerOnly = playerOnly;
        Core.getInstance().commands.add(this);
    }
    
    public ServerCommand(final String label, final String description, final String usage, final boolean playerOnly) {
        this(label, description, usage, playerOnly, Collections.emptyList());
    }
    
    public ServerCommand(final String label, final boolean playerOnly) {
        this(label, "", invokedynamic(makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;, label), playerOnly, Collections.emptyList());
    }
    
    public ServerCommand(final String label) {
        this(label, false);
    }
    
    public void registerSub(final ServerSubCommand subCommand) {
        this.subCommands.add(subCommand);
    }
    
    public abstract void onCommand();
    
    public void setCooldownDuration(final long duration, final TimeUnit timeUnit) {
        this.cooldownDuration = duration;
        this.cooldownTimeUnit = timeUnit;
        this.cooldownMap.setExpiration(this.cooldownDuration, this.cooldownTimeUnit);
    }
    
    public void addTabCompleteArgs(final String... args) {
        this.tabCompleteArgs.addAll(Arrays.asList(args));
    }
    
    private String addPrefix(final String message) {
        return message.replace("{PREFIX}", Core.getInstance().getPrefix());
    }
    
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        this.populate(sender, args);
        if (this.playerOnly && !(sender instanceof Player)) {
            return this.returnSay(false, this.addPrefix(Core.instance.notAPlayerMessage));
        }
        if (this.cooldownDuration > 0L && this.isPlayer()) {
            final Player player = (Player)this.sender;
            final long lastCommandRun = this.cooldownMap.getOrDefault(player.getUniqueId(), 0L);
            final long difference = this.cooldownTimeUnit.convert(System.currentTimeMillis() - lastCommandRun, TimeUnit.MILLISECONDS);
            if (difference <= this.cooldownDuration) {
                final String remainingTime = invokedynamic(makeConcatWithConstants:(JLjava/lang/String;)Ljava/lang/String;, this.cooldownDuration - difference, this.formattedTimeUnit(this.cooldownTimeUnit));
                return this.returnSay(false, this.addPrefix(Core.instance.onCooldownMessage).replace("{TIME}", remainingTime));
            }
            this.cooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
        }
        if (args.length >= 1) {
            for (final ServerSubCommand subCommand : this.subCommands) {
                if (subCommand.label.equalsIgnoreCase(args[0]) || subCommand.aliases.contains(args[0].toLowerCase())) {
                    subCommand.onCommand();
                    return true;
                }
            }
        }
        this.onCommand();
        return true;
    }
    
    public List<String> tabComplete(final CommandSender sender, final String alias, final String[] args) throws IllegalArgumentException {
        if (!(sender instanceof Player)) {
            return null;
        }
        this.populate(sender, args);
        if (args.length == 1) {
            final List<String> tabComplete = new ArrayList<String>();
            for (final ServerSubCommand subCommand : this.subCommands) {
                if (subCommand.label.startsWith(args[0]) || subCommand.aliases.contains(args[0])) {
                    tabComplete.add(subCommand.label);
                }
                if (subCommand.label.equalsIgnoreCase(args[0])) {
                    tabComplete.addAll(subCommand.tabCompleteArgs);
                }
            }
            tabComplete.add(this.getLabel());
            tabComplete.addAll(this.tabCompleteArgs);
            return tabComplete;
        }
        return Collections.emptyList();
    }
    
    protected void say(final boolean withPrefix, final String message) {
        Chat.say(withPrefix, this.sender, message);
    }
    
    protected void say(final String message) {
        this.say(true, message);
    }
    
    protected final boolean isPlayer() {
        return this.sender instanceof Player;
    }
    
    public CommandSender getSender() {
        return this.sender;
    }
    
    private boolean returnSay(final boolean withPrefix, final String message) {
        this.say(withPrefix, message);
        return true;
    }
    
    private void populate(final CommandSender sender, final String[] args) {
        this.sender = sender;
        this.args = args;
    }
    
    private String formattedTimeUnit(final TimeUnit unit) {
        switch (unit) {
            case HOURS:
            case DAYS:
            case MINUTES:
            case SECONDS: {
                return unit.toString().substring(0, 1).toLowerCase();
            }
            case MILLISECONDS: {
                return "ms";
            }
            case MICROSECONDS: {
                return "micros";
            }
            case NANOSECONDS: {
                return "ns";
            }
            default: {
                return "";
            }
        }
    }
}
