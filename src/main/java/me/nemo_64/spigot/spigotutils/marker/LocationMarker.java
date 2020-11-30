/*
MIT License

Copyright (c) 2020 MrNemo64

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package me.nemo_64.spigot.spigotutils.marker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import me.nemo_64.spigot.spigotutils.xseries.ReflectionUtils;

/**
 * Utility class for marking block using shulkers and NMS
 * 
 * @version 1.0
 * @author MrNemo64
 */
public class LocationMarker {

	private static Constructor<?> entityShulkerConstructor;
	private static Constructor<?> packetPlayOutSpawnEntityLivingConstructor;
	private static Constructor<?> packetPlayOutEntityMetadataConstructor;
	private static Constructor<?> packetPlayOutEntityDestroyConstructor;
	private static Class<?> craftWorldClass;
	private static Method worldGetHandleMethod;
	private static Method entityShulkerSetLocationMethod;
	private static Method entityShulkerSetInvisibleMethod;
	private static Method entityShulkerSetGlowingMethod;
	private static Method entityShulkerGetIdMethod;
	private static Method entityShulkerGetDataWatcherMethod;
	private static Field entityTypesShulkerField;

	static {
		try {
			Class<?> entityLivingClass = ReflectionUtils.getNMSClass("EntityLiving");
			Class<?> packetPlayOutSpawnEntityLivingClass = ReflectionUtils.getNMSClass("PacketPlayOutSpawnEntityLiving");
			if (packetPlayOutSpawnEntityLivingClass != null) {
				packetPlayOutSpawnEntityLivingConstructor = packetPlayOutSpawnEntityLivingClass
						.getConstructor(entityLivingClass);
			}

			Class<?> packetPlayOutEntityMetadataClass = ReflectionUtils.getNMSClass("PacketPlayOutEntityMetadata");
			if (packetPlayOutEntityMetadataClass != null) {
				Class<?> dataWatcherClass = ReflectionUtils.getNMSClass("DataWatcher");
				if (dataWatcherClass != null)
					packetPlayOutEntityMetadataConstructor = packetPlayOutEntityMetadataClass.getConstructor(int.class,
							dataWatcherClass, boolean.class);
			}

			Class<?> packetPlayOutEntityDestroyClass = ReflectionUtils.getNMSClass("PacketPlayOutEntityDestroy");
			if (packetPlayOutEntityDestroyClass != null)
				packetPlayOutEntityDestroyConstructor = packetPlayOutEntityDestroyClass.getConstructor(int[].class);

			craftWorldClass = ReflectionUtils.getCraftClass("CraftWorld");
			if (craftWorldClass != null) {
				worldGetHandleMethod = craftWorldClass.getMethod("getHandle");
			}

			Class<?> entityShulkerClass = ReflectionUtils.getNMSClass("EntityShulker");
			if (entityShulkerClass != null) {
				entityShulkerSetLocationMethod = entityShulkerClass.getMethod("setLocation", double.class, double.class,
						double.class, float.class, float.class);
				entityShulkerSetInvisibleMethod = entityShulkerClass.getMethod("setInvisible", boolean.class);
				entityShulkerSetGlowingMethod = entityShulkerClass.getMethod("h", boolean.class);
				entityShulkerGetIdMethod = entityShulkerClass.getMethod("getId");
				entityShulkerGetDataWatcherMethod = entityShulkerClass.getMethod("getDataWatcher");
			}

			Class<?> nmsWorldClass = ReflectionUtils.getNMSClass("World");
			if (entityShulkerClass != null && nmsWorldClass != null) {
				if (ReflectionUtils.VERSION_NUMBER >= 14) { // 1.14+
					Class<?> entityTypesClass = ReflectionUtils.getNMSClass("EntityTypes");
					if (entityTypesClass != null) {
						entityTypesShulkerField = entityTypesClass.getField("SHULKER");
						entityShulkerConstructor = entityShulkerClass.getConstructor(entityTypesClass, nmsWorldClass);
					}
				} else {
					entityShulkerConstructor = entityShulkerClass.getConstructor(nmsWorldClass);
				}
			}
		} catch(NoSuchMethodException e) {
			e.printStackTrace();
		} catch(SecurityException e) {
			e.printStackTrace();
		} catch(NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Marks a block to a given player
	 * 
	 * @param player The player
	 * @param block  The block
	 * @return The id of the shulker marking the block, null if no shulker was
	 *         spawned
	 */
	public static Integer markBlock(Player player, Block block) {
		Location location = block.getLocation();
		double x = location.getBlockX() + 0.5;
		double y = location.getBlockY();
		double z = location.getBlockZ() + 0.5;
		return markLocation(player, new Location(location.getWorld(), x, y, z));
	}

	/**
	 * Marks all the given locations to a player
	 * 
	 * @param player    The player
	 * @param locations The locations
	 * @return An array with all the ids of the spawned shulkers
	 */
	public static Integer[] markLocations(Player player, Location... locations) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Location l : locations) {
			Integer id = markLocation(player, l);
			if (id != null)
				ids.add(id);
		}
		return ids.toArray(new Integer[ids.size()]);
	}

	/**
	 * Marks the given location to a player
	 * 
	 * @param player   The player
	 * @param location The location
	 * @return The id of the spawned shulker, null if no shulker was spawned
	 */
	public static Integer markLocation(Player player, Location location) {
		if (entityShulkerSetLocationMethod == null || entityShulkerSetInvisibleMethod == null
				|| entityShulkerSetGlowingMethod == null || entityShulkerGetIdMethod == null
				|| entityShulkerGetDataWatcherMethod == null || packetPlayOutSpawnEntityLivingConstructor == null
				|| packetPlayOutEntityMetadataConstructor == null)
			return null;
		Integer id = null;
		Object shulker = createShulker(location.getWorld());
		Object idO = null;
		try {
			entityShulkerSetLocationMethod.invoke(shulker, location.getX(), location.getY(), location.getZ(), 0f, 0f);
			entityShulkerSetInvisibleMethod.invoke(shulker, true);
			entityShulkerSetGlowingMethod.invoke(shulker, true);
			idO = entityShulkerGetIdMethod.invoke(shulker);
			Object dataWatcher = entityShulkerGetDataWatcherMethod.invoke(shulker);
			if (dataWatcher == null)
				return null;

			Object packetPlayOutSpawnEntityLiving = packetPlayOutSpawnEntityLivingConstructor.newInstance(shulker);
			Object packetPlayOutEntityMetadata = packetPlayOutEntityMetadataConstructor.newInstance(idO, dataWatcher, true);
			ReflectionUtils.sendPacket(player, packetPlayOutSpawnEntityLiving, packetPlayOutEntityMetadata);
		} catch(IllegalAccessException e) {
			e.printStackTrace();
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
		} catch(InvocationTargetException e) {
			e.printStackTrace();
		} catch(InstantiationException e) {
			e.printStackTrace();
		}
		if (idO != null && idO instanceof Integer)
			id = (Integer) idO;
		return id;
	}

	/**
	 * Unmarks all the given ids to a player
	 * 
	 * @param player The player
	 * @param id     The ids of the shulkers that are marking the locations
	 * @return True if the packet to unmark the location was sent
	 */
	public static boolean unmarkLocation(Player player, int... id) {
		if (packetPlayOutEntityDestroyConstructor == null)
			return false;
		try {
			Object packetPlayOutEntityDestroy = packetPlayOutEntityDestroyConstructor.newInstance(id);
			ReflectionUtils.sendPacket(player, packetPlayOutEntityDestroy);
			return true;
		} catch(InstantiationException e) {
			e.printStackTrace();
		} catch(IllegalAccessException e) {
			e.printStackTrace();
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
		} catch(InvocationTargetException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static Object createShulker(World world) {
		if (entityShulkerConstructor == null || worldGetHandleMethod == null)
			return null;
		if (ReflectionUtils.VERSION_NUMBER >= 14 && entityTypesShulkerField == null)
			return null;
		Object w = worldToCraftWorld(world);
		if (w == null)
			return null;
		try {
			if (ReflectionUtils.VERSION_NUMBER >= 14) {
				return entityShulkerConstructor.newInstance(entityTypesShulkerField.get(null), worldGetHandleMethod.invoke(w));
			} else {
				return entityShulkerConstructor.newInstance(worldGetHandleMethod.invoke(w));
			}
		} catch(InstantiationException e) {
			e.printStackTrace();
		} catch(IllegalAccessException e) {
			e.printStackTrace();
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
		} catch(InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Object worldToCraftWorld(World world) {
		if (craftWorldClass == null)
			return null;
		try {
			return craftWorldClass.cast(world);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
