// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core;

import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Checks
{
    public static <T> T nonNull(@Nullable final T obj, @Nonnull final String name) {
        if (obj == null) {
            throw new IllegalArgumentException(invokedynamic(makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;, name));
        }
        return obj;
    }
    
    public static void check(final boolean failed, final String name) {
        if (failed) {
            throw new RuntimeException(name);
        }
    }
    
    public static void state(final boolean expression, final String errorMessageFormat, final Object... args) {
        if (!expression) {
            throw new IllegalStateException(String.format(errorMessageFormat, args));
        }
    }
    
    public static void element(final Object element, final Object key) {
        if (element == null) {
            throw new NoSuchElementException(key.toString());
        }
    }
}
