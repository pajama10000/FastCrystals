// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.actionbar.animations;

import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.bukkit.plugin.Plugin;
import xyz.fxcilities.core.Core;
import xyz.fxcilities.core.actionbar.PlayerActionBar;
import org.bukkit.scheduler.BukkitRunnable;

public class ProgressBar extends BukkitRunnable
{
    private final PlayerActionBar actionBar;
    private final int maxBarTicks;
    private final int maxDisplayTicks;
    private int ticked;
    private int displayTicks;
    private final String begin;
    private final String middle;
    private final String end;
    
    public ProgressBar(final PlayerActionBar actionBar, final int maxBarTicks, final int maxDisplayTicks, final String begin, final String middle, final String end) {
        this.ticked = 0;
        this.displayTicks = 0;
        this.actionBar = actionBar;
        this.maxBarTicks = maxBarTicks;
        this.maxDisplayTicks = maxDisplayTicks;
        this.begin = begin;
        this.middle = middle;
        this.end = end;
        this.runTaskTimer((Plugin)Core.getInstance(), 0L, 1L);
    }
    
    public void run() {
        ++this.displayTicks;
        if (this.displayTicks >= this.maxDisplayTicks) {
            this.cancel();
            return;
        }
        if (this.ticked < this.maxBarTicks) {
            ++this.ticked;
        }
        final String bar = IntStream.range(0, this.ticked).mapToObj(i -> this.middle).collect((Collector<? super Object, ?, String>)Collectors.joining());
        this.actionBar.setBar(invokedynamic(makeConcatWithConstants:(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;, this.begin, bar, this.end));
        this.actionBar.sendBar();
    }
}
