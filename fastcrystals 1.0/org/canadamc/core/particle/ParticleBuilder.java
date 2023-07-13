// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.particle;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import org.bukkit.Particle;

public class ParticleBuilder
{
    public Particle particle;
    public Object data;
    
    public ParticleBuilder(@Nonnull final Particle particle, @Nullable final Object data) {
        this.particle = particle;
        this.data = data;
    }
    
    public ParticleBuilder(@Nonnull final Particle particle) {
        this(particle, null);
    }
    
    public boolean shouldUseData() {
        return this.data == null || this.particle.getDataType() != Void.class;
    }
}
