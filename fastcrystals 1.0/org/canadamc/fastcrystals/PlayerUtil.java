// 
// Created by pajama
// 

package org.canadamc.fastcrystals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.bukkit.entity.Player;

public class PlayerUtil
{
    public static int getPing(final Player player) {
        try {
            final Method getHandle = player.getClass().getMethod("getHandle", (Class[])new Class[0]);
            final Object handle = getHandle.invoke(player, new Object[0]);
            final Field ping = handle.getClass().getField("ping");
            return ping.getInt(handle);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException ex2) {
            final ReflectiveOperationException ex;
            final ReflectiveOperationException e = ex;
            e.printStackTrace();
            return 0;
        }
    }
}