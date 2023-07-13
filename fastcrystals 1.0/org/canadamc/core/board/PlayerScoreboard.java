// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.board;

import java.util.Iterator;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.Bukkit;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class PlayerScoreboard
{
    public static final int MAX_LINES = 16;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final List<String> modifies;
    
    public PlayerScoreboard(final String title) {
        this.modifies = new ArrayList<String>(16);
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        (this.objective = this.scoreboard.registerNewObjective(title, "dummy")).setDisplayName(title);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
    
    public void setTitle(final String title) {
        this.objective.setDisplayName(title);
    }
    
    private String getLineCoded(final String line) {
        String result;
        for (result = line; this.modifies.contains(result); result = invokedynamic(makeConcatWithConstants:(Ljava/lang/String;Lorg/bukkit/ChatColor;)Ljava/lang/String;, result, ChatColor.RESET)) {}
        return result.substring(0, Math.min(40, result.length()));
    }
    
    public void addLine(final String line) {
        if (this.modifies.size() > 16) {
            throw new IndexOutOfBoundsException("You cannot add more than 16 lines.");
        }
        final String modified = this.getLineCoded(line);
        this.modifies.add(modified);
        this.objective.getScore(modified).setScore(-this.modifies.size());
    }
    
    public void addBlankSpace() {
        this.addLine(" ");
    }
    
    public void setLine(final int index, final String line) {
        if (index < 0 || index >= 16) {
            throw new IndexOutOfBoundsException("The index cannot be negative or higher than 15.");
        }
        final String oldModified = this.modifies.get(index);
        this.scoreboard.resetScores(oldModified);
        final String modified = this.getLineCoded(line);
        this.modifies.set(index, modified);
        this.objective.getScore(modified).setScore(-(index + 1));
    }
    
    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }
    
    @Override
    public String toString() {
        String out = "";
        final int i = 0;
        for (final String string : this.modifies) {
            out = invokedynamic(makeConcatWithConstants:(Ljava/lang/String;ILjava/lang/String;)Ljava/lang/String;, out, -(i + 1), string);
        }
        return out;
    }
}
