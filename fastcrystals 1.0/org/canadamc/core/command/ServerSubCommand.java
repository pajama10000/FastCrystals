// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.command;

import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public abstract class ServerSubCommand
{
    public final ServerCommand parent;
    public final String label;
    public final String description;
    public final String usage;
    public final List<String> aliases;
    public List<String> tabCompleteArgs;
    
    public ServerSubCommand(final ServerCommand parent, final String label, final String description, final String usage, final List<String> aliases) {
        this.tabCompleteArgs = new ArrayList<String>();
        this.parent = parent;
        this.label = label;
        this.description = description;
        this.usage = usage;
        this.aliases = aliases;
        parent.registerSub(this);
    }
    
    public abstract void onCommand();
    
    public void addTabCompleteArgs(final String... args) {
        this.tabCompleteArgs.addAll(Arrays.asList(args));
    }
    
    protected void say(final boolean withPrefix, final String message) {
        this.parent.say(withPrefix, message);
    }
    
    protected void say(final String message) {
        this.parent.say(message);
    }
}
