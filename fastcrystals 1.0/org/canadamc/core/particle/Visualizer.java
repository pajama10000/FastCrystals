// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.particle;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.bukkit.scheduler.BukkitRunnable;

public class Visualizer extends BukkitRunnable
{
    private final BoundingBox bb;
    private final ParticleBuilder particleBuilder;
    private final World world;
    
    public Visualizer(final BoundingBox bb, final ParticleBuilder particleBuilder, final World world) {
        this.bb = bb;
        this.world = world;
        this.particleBuilder = particleBuilder;
    }
    
    public void run() {
        for (final double[][] coords : new double[][][] { { { this.bb.getMinX(), this.bb.getMinY(), this.bb.getMinZ() }, { this.bb.getMaxX(), this.bb.getMinY(), this.bb.getMaxZ() } }, { { this.bb.getMaxX(), this.bb.getMaxY(), this.bb.getMaxZ() }, { this.bb.getMinX(), this.bb.getMaxY(), this.bb.getMinZ() } }, { { this.bb.getMaxX(), this.bb.getMaxY(), this.bb.getMinZ() }, { this.bb.getMinX(), this.bb.getMinY(), this.bb.getMinZ() } }, { { this.bb.getMinX(), this.bb.getMaxY(), this.bb.getMinZ() }, { this.bb.getMinX(), this.bb.getMinY(), this.bb.getMaxZ() } }, { { this.bb.getMinX(), this.bb.getMaxY(), this.bb.getMaxZ() }, { this.bb.getMaxX(), this.bb.getMinY(), this.bb.getMaxZ() } }, { { this.bb.getMaxX(), this.bb.getMaxY(), this.bb.getMinZ() }, { this.bb.getMaxX(), this.bb.getMinY(), this.bb.getMaxZ() } } }) {
            final double[] locOne = coords[0];
            final double[] locTwo = coords[1];
            this.wireframe(new BoundingBox(locOne[0], locOne[1], locOne[2], locTwo[0], locTwo[1], locTwo[2]));
        }
    }
    
    private void wireframe(final BoundingBox box) {
        for (double x = box.getMinX(); x <= box.getMaxX(); ++x) {
            for (double y = box.getMinY(); y <= box.getMaxY(); ++y) {
                for (double z = box.getMinZ(); z <= box.getMaxZ(); ++z) {
                    final Location loc = new Location(this.world, x, y, z);
                    if (this.particleBuilder.shouldUseData()) {
                        this.world.spawnParticle(this.particleBuilder.particle, loc, 0, this.particleBuilder.data);
                    }
                    else {
                        this.world.spawnParticle(this.particleBuilder.particle, loc, 0);
                    }
                }
            }
        }
    }
}
