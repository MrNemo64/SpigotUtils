/*
 * MIT License
 * 
 * Copyright (c) 2020 MrNemo64
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
*/

package me.nemo_64.spigot.spigotutils.marker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import me.nemo_64.spigot.spigotutils.xseries.ReflectionUtils;

/**
 * Utility class for marking block using shulkers and NMS
 * 
 * @version 2.1.1
 * @author MrNemo64
 */
public class LocationMarker {

	private static Constructor<?> entityShulkerConstructor;
	private static Constructor<?> packetPlayOutSpawnEntityLivingConstructor;
	private static Constructor<?> packetPlayOutEntityMetadataConstructor;
	private static Constructor<?> packetPlayOutEntityDestroyConstructor;
	private static Class<?> entityShulkerClass;
	private static Class<?> craftWorldClass;
	private static Method worldGetHandleMethod;
	private static Method entityShulkerSetLocationMethod;
	private static Method entityShulkerSetInvisibleMethod;
	private static Method entityShulkerSetGlowingMethod;
	private static Method entityShulkerGetIdMethod;
	private static Method entityShulkerGetDataWatcherMethod;
	private static Method entityShulkerGetUuidMethod;
	private static Field packetPlayOutScoreboardTeamNameField;
	private static Field packetPlayOutScoreboardTeamColorField;
	private static Object entityTypesShulkerObject;
	private static Object teamPacketObject;
	private static Collection<String> entities;

	static {
		try {
			// Load the necessary fields to create the team package.
			// The team package is used to change the color of the shulker
			Class<?> packetPlayOutScoreboardTeamClass = ReflectionUtils.getNMSClass("PacketPlayOutScoreboardTeam");
			if (packetPlayOutScoreboardTeamClass != null) {
				// We need this field to set a name for the team
				packetPlayOutScoreboardTeamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
				packetPlayOutScoreboardTeamNameField.setAccessible(true);
				// We need this field to set the color of the team
				packetPlayOutScoreboardTeamColorField = packetPlayOutScoreboardTeamClass.getDeclaredField("g");
				packetPlayOutScoreboardTeamColorField.setAccessible(true);
			}

			// We need this class to get the packet to spawn the shulkers
			Class<?> entityLivingClass = ReflectionUtils.getNMSClass("EntityLiving");
			// The packet that spawns an entity client-side
			Class<?> packetPlayOutSpawnEntityLivingClass = ReflectionUtils.getNMSClass("PacketPlayOutSpawnEntityLiving");
			if (packetPlayOutSpawnEntityLivingClass != null && entityLivingClass != null) {
				packetPlayOutSpawnEntityLivingConstructor = packetPlayOutSpawnEntityLivingClass
						.getConstructor(entityLivingClass);
			}

			// All spawned entityes need a data watcher, we need to provide the data watcher
			// when spawning an entity
			Class<?> packetPlayOutEntityMetadataClass = ReflectionUtils.getNMSClass("PacketPlayOutEntityMetadata");
			if (packetPlayOutEntityMetadataClass != null) {
				Class<?> dataWatcherClass = ReflectionUtils.getNMSClass("DataWatcher");
				if (dataWatcherClass != null)
					packetPlayOutEntityMetadataConstructor = packetPlayOutEntityMetadataClass.getConstructor(int.class,
							dataWatcherClass, boolean.class);
			}

			// We use this package to remove entityes
			Class<?> packetPlayOutEntityDestroyClass = ReflectionUtils.getNMSClass("PacketPlayOutEntityDestroy");
			if (packetPlayOutEntityDestroyClass != null)
				packetPlayOutEntityDestroyConstructor = packetPlayOutEntityDestroyClass.getConstructor(int[].class);

			// Need the CraftWorld to cast worlds to craftworlds
			craftWorldClass = ReflectionUtils.getCraftClass("CraftWorld");
			if (craftWorldClass != null) {
				// Need the getHandle to get the nms version of the world
				worldGetHandleMethod = craftWorldClass.getMethod("getHandle");
			}

			// The shulker class
			entityShulkerClass = ReflectionUtils.getNMSClass("EntityShulker");
			if (entityShulkerClass != null) {
				// Some necesary methods of the shulker class
				entityShulkerGetUuidMethod = entityShulkerClass.getMethod("getUniqueID");
				entityShulkerSetLocationMethod = entityShulkerClass.getMethod("setLocation", double.class, double.class,
						double.class, float.class, float.class);
				entityShulkerSetInvisibleMethod = entityShulkerClass.getMethod("setInvisible", boolean.class);
				entityShulkerSetGlowingMethod = entityShulkerClass.getMethod("h", boolean.class); // To set the shulker glowing
				entityShulkerGetIdMethod = entityShulkerClass.getMethod("getId");
				entityShulkerGetDataWatcherMethod = entityShulkerClass.getMethod("getDataWatcher");
			}

			// NMS version of the world class
			Class<?> nmsWorldClass = ReflectionUtils.getNMSClass("World");
			// The constructor of the entityes changes between versions
			if (entityShulkerClass != null && nmsWorldClass != null) {
				if (ReflectionUtils.VERSION_NUMBER >= 14) { // 1.14+
					Class<?> entityTypesClass = ReflectionUtils.getNMSClass("EntityTypes");
					if (entityTypesClass != null) {
						entityShulkerConstructor = entityShulkerClass.getConstructor(entityTypesClass, nmsWorldClass);
						Field entityTypesShulkerField = entityTypesClass.getField("SHULKER");
						if (entityTypesShulkerField != null)
							entityTypesShulkerObject = entityTypesShulkerField.get(null);
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
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
		} catch(IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Marks a block to a given player
	 * 
	 * @param player The player
	 * @param block  The block
	 * @return A {@link ShulkerMarker} with the data of the shulker
	 */
	public static ShulkerMarker markBlock(Player player, Block block) {
		return markBlock(player, block, null);
	}

	/**
	 * Marks a block to a given player
	 * 
	 * @param player The player
	 * @param block  The block
	 * @param color  The color
	 * @return A {@link ShulkerMarker} with the data of the shulker
	 */
	public static ShulkerMarker markBlock(Player player, Block block, ChatColor color) {
		return markLocation(player, block.getLocation().add(0.5, 0, 0.5), color);
	}

	/**
	 * Marks all the given locations to a player
	 * 
	 * @param player    The player
	 * @param color     The color
	 * @param locations The locations
	 * @return A list of {@link ShulkerMarker} with the data of the shulkers
	 */
	public static List<ShulkerMarker> markLocations(Player player, ChatColor color, Location... locations) {
		List<ShulkerMarker> ids = new ArrayList<ShulkerMarker>(locations.length);
		for (Location l : locations)
			ids.add(markLocation(player, l, color));
		return ids;
	}

	/**
	 * Marks all the given locations to a player
	 * 
	 * @param player    The player
	 * @param locations The locations
	 * @return A list of {@link ShulkerMarker} with the data of the shulkers
	 */
	public static List<ShulkerMarker> markLocations(Player player, Location... locations) {
		return markLocations(player, null, locations);
	}

	/**
	 * Marks the given location to a player
	 * 
	 * @param player   The player
	 * @param location The location
	 * @param color    The color
	 * @return A {@link ShulkerMarker} with the data of the shulker
	 */
	@Nonnull
	public static ShulkerMarker markLocation(Player player, Location location, ChatColor color) {
		if (entityShulkerSetLocationMethod == null || entityShulkerSetInvisibleMethod == null
				|| entityShulkerSetGlowingMethod == null || entityShulkerGetIdMethod == null
				|| entityShulkerGetDataWatcherMethod == null || packetPlayOutSpawnEntityLivingConstructor == null
				|| packetPlayOutEntityMetadataConstructor == null)
			return null;
		String uuid = null;
		Integer id = null;
		ShulkerMarker markerShulker = new ShulkerMarker(player);
		Object shulker = createShulker(location.getWorld());
		if (shulker == null)
			return markerShulker;
		Object idO = null;
		Object uuidO = null;
		try {
			entityShulkerSetLocationMethod.invoke(shulker, location.getX(), location.getY(), location.getZ(), 0f, 0f);
			entityShulkerSetInvisibleMethod.invoke(shulker, true);
			entityShulkerSetGlowingMethod.invoke(shulker, true);
			idO = entityShulkerGetIdMethod.invoke(shulker);
			uuidO = entityShulkerGetUuidMethod.invoke(shulker);
			Object dataWatcher = entityShulkerGetDataWatcherMethod.invoke(shulker);
			if (dataWatcher == null)
				return markerShulker;

			Object packetPlayOutSpawnEntityLiving = packetPlayOutSpawnEntityLivingConstructor.newInstance(shulker);
			Object packetPlayOutEntityMetadata = packetPlayOutEntityMetadataConstructor.newInstance(idO, dataWatcher, true);
			ReflectionUtils.sendPacket(player, packetPlayOutSpawnEntityLiving, packetPlayOutEntityMetadata);
			if (color != null) {
				Object teamPackage = createTeamPacket(color, shulker);
				if (teamPackage != null)
					ReflectionUtils.sendPacket(player, teamPackage);
			}
		} catch(IllegalAccessException e) {
			e.printStackTrace();
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
		} catch(InvocationTargetException e) {
			e.printStackTrace();
		} catch(InstantiationException e) {
			e.printStackTrace();
		}
		if (idO != null && idO instanceof Integer && uuidO != null && uuidO instanceof UUID) {
			id = (Integer) idO;
			uuid = ((UUID) uuidO).toString();
			markerShulker.setSpawned(true);
		}
		markerShulker.setId(id);
		markerShulker.setUuid(uuid);
		return markerShulker;
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

	/**
	 * Updates the color of the given shulkers to the given players.
	 * 
	 * @param newColor The new color
	 * @param player   The player
	 * @param shulkers The uuid of the shulkers to update
	 */
	public static void changeColor(ChatColor newColor, Player player, String... shulkers) {
		if (teamPacketObject == null)
			teamPacketObject = createTeamPacket(null, new Object[] {});
		try {
			packetPlayOutScoreboardTeamNameField.set(teamPacketObject, createRandomTeamName(5)); // Team name, random to avoid
																																														// conflict
			Object colorId = MarkerColor.of(newColor).getEnumChatFormat();
			packetPlayOutScoreboardTeamColorField.set(teamPacketObject,
					colorId != null ? colorId : MarkerColor.WHITE.getEnumChatFormat()); // Set the color

			entities.clear();
			for (String str : shulkers) {
				try {
					UUID.fromString(str);
					entities.add(str);
				} catch(Exception e) {}
			}
			ReflectionUtils.sendPacket(player, teamPacketObject);
		} catch(IllegalArgumentException | IllegalAccessException e1) {
			e1.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private static Object createTeamPacket(ChatColor color, Object... shulkers) {
		if (shulkers == null)
			return null;
		try {
			if (teamPacketObject == null) {
				Class<?> packetPlayOutScoreboardTeamClass = ReflectionUtils.getNMSClass("PacketPlayOutScoreboardTeam");
				if (packetPlayOutScoreboardTeamClass == null)
					return null;

				Constructor<?> packetPlayOutScoreboardTeamConstructor = packetPlayOutScoreboardTeamClass.getConstructor();
				if (packetPlayOutScoreboardTeamConstructor == null)
					return null;

				Field packetPlayOutScoreboardTeamCreateField = packetPlayOutScoreboardTeamClass.getDeclaredField("i");
				if (packetPlayOutScoreboardTeamCreateField == null)
					return null;
				packetPlayOutScoreboardTeamCreateField.setAccessible(true);

				Field packetPlayOutScoreboardTeamOptionsField = packetPlayOutScoreboardTeamClass.getDeclaredField("j");
				if (packetPlayOutScoreboardTeamOptionsField == null)
					return null;
				packetPlayOutScoreboardTeamOptionsField.setAccessible(true);
				Field packetPlayOutScoreboardTeamEntitiesField = packetPlayOutScoreboardTeamClass.getDeclaredField("h");
				if (packetPlayOutScoreboardTeamEntitiesField == null)
					return null;
				packetPlayOutScoreboardTeamEntitiesField.setAccessible(true);

				teamPacketObject = packetPlayOutScoreboardTeamConstructor.newInstance();
				packetPlayOutScoreboardTeamCreateField.set(teamPacketObject, 0); // Create a team
				packetPlayOutScoreboardTeamOptionsField.set(teamPacketObject, 0); // idk it just works
				Object entitiesObj = packetPlayOutScoreboardTeamEntitiesField.get(teamPacketObject);
				if (!(entitiesObj instanceof Collection<?>))
					return null;
				entities = (Collection<String>) entitiesObj;
			}
			packetPlayOutScoreboardTeamNameField.set(teamPacketObject, createRandomTeamName(5)); // Team name. Random to avoid
																																														// conflict
			Object colorId = MarkerColor.of(color).getEnumChatFormat();
			packetPlayOutScoreboardTeamColorField.set(teamPacketObject,
					colorId != null ? colorId : MarkerColor.WHITE.getEnumChatFormat()); // Set the color

			entities.clear();
			for (Object obj : shulkers) {
				if (entityShulkerClass.isInstance(obj)) {
					Object uuid = entityShulkerGetUuidMethod.invoke(obj);
					if (uuid instanceof UUID) {
						entities.add(((UUID) uuid).toString());
					}
				}
			}
			return teamPacketObject;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static String createRandomTeamName(int size) {
		byte[] array = new byte[size];
		new Random().nextBytes(array);
		return new String(array, Charset.forName("UTF-8"));
	}

	private static Object createShulker(World world) {
		if (entityShulkerConstructor == null || worldGetHandleMethod == null)
			return null;
		if (ReflectionUtils.VERSION_NUMBER >= 14 && entityTypesShulkerObject == null)
			return null;
		Object w = worldToCraftWorld(world);
		if (w == null)
			return null;
		try {
			if (ReflectionUtils.VERSION_NUMBER >= 14) {
				return entityShulkerConstructor.newInstance(entityTypesShulkerObject, worldGetHandleMethod.invoke(w));
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

	/**
	 * Class to store data about a shulker marker
	 * 
	 * @author MrNemo64
	 * @version 1.0
	 */
	public static class ShulkerMarker {

		private Integer id;
		private String uuid;
		private Player player;
		private boolean spawned;

		protected ShulkerMarker(Player player) {
			this.spawned = false;
			this.player = player;
			this.id = null;
			this.uuid = null;
		}

		/**
		 * Changes the color of this shulker. This method will only run if
		 * {@link #wasSpawned()} returns true and {@link #getUniqueId()} does not return
		 * null. To change the color the
		 * {@link LocationMarker#changeColor(ChatColor, Player, String...)} is used. If
		 * several shulkers are going to be updated to the same player with the same
		 * color using {@link LocationMarker#changeColor(ChatColor, Player, String...)}
		 * is recomended for faster updating
		 * 
		 * @param color The new color of the shulker
		 */
		public void changeColor(ChatColor color) {
			if (wasSpawned() && getUniqueId() != null)
				LocationMarker.changeColor(color, getPlayer(), getUniqueId());
		}

		/**
		 * Deletes this shulker marker. This method will only run if
		 * {@link #wasSpawned()} returns true and {@link #getId()} does not return null.
		 * If several shulkers are going to be deleted to the same player
		 * {@link LocationMarker#unmarkLocation(Player, int...)} is recomended for
		 * faster unmarking
		 */
		public void delete() {
			if (wasSpawned() && getId() == null)
				return;
			setSpawned(false);
			LocationMarker.unmarkLocation(getPlayer(), getId());
		}

		/**
		 * Gets the player that sees this shulker marker
		 * 
		 * @return The player
		 */
		public Player getPlayer() {
			return player;
		}

		protected void setPlayer(Player player) {
			this.player = player;
		}

		/**
		 * Retuns true if the shulker was spawned
		 * 
		 * @return True if the shulker was spawned
		 */
		public boolean wasSpawned() {
			return spawned;
		}

		protected void setSpawned(boolean spawned) {
			this.spawned = spawned;
		}

		/**
		 * Gets the id of the shulker. Used to delete the shulker
		 * 
		 * @return The id
		 */
		public Integer getId() {
			return id;
		}

		protected void setId(Integer id) {
			this.id = id;
		}

		/**
		 * Gets the uuid of the shulker. Used to change the color of the shulker
		 * 
		 * @return
		 */
		public String getUniqueId() {
			return uuid;
		}

		protected void setUuid(String uuid) {
			this.uuid = uuid;
		}

		@Override
		public String toString() {
			return "ShulkerMarker{id=" + getId() + ", uuid=" + getUniqueId() + ", spawned=" + wasSpawned() + ", player="
					+ player.getUniqueId().toString() + "}";
		}

	}

	/**
	 * Enum to store color data
	 * 
	 * @author MrNemo64
	 * @version 1.0
	 *
	 */
	public static enum MarkerColor {

		BLACK(0, "BLACK", ChatColor.BLACK),
		DARK_BLUE(1, "DARK_BLUE", ChatColor.DARK_BLUE),
		DARK_GREEN(2, "DARK_GREEN", ChatColor.DARK_GREEN),
		DARK_AQUA(3, "DARK_AQUA", ChatColor.DARK_AQUA),
		DARK_RED(4, "DARK_RED", ChatColor.DARK_RED),
		DARK_PURPLE(5, "DARK_PURPLE", ChatColor.DARK_PURPLE),
		GOLD(6, "GOLD", ChatColor.GOLD),
		GRAY(7, "GRAY", ChatColor.GRAY),
		DARK_GRAY(8, "DARK_GRAY", ChatColor.DARK_GRAY),
		BLUE(9, "BLUE", ChatColor.BLUE),
		GREEN(10, "GREEN", ChatColor.GREEN),
		AQUA(11, "AQUA", ChatColor.AQUA),
		RED(12, "RED", ChatColor.RED),
		PURPLE(13, "LIGHT_PURPLE", ChatColor.LIGHT_PURPLE),
		YELLOW(14, "YELLOW", ChatColor.YELLOW),
		WHITE(15, "WHITE", ChatColor.WHITE),
		NONE(-1, null, null);

		private int id;
		private ChatColor color;
		private Object enumChatFormat;

		private MarkerColor(int id, String enumChatFormatName, ChatColor color) {
			this.id = id;
			this.color = color;

			if (enumChatFormatName == null || color == null)
				return;
			try {
				Class<?> c = ReflectionUtils.getNMSClass("EnumChatFormat");
				if (c == null)
					return;
				Method nameMethod = c.getMethod("name");
				if (nameMethod == null)
					return;
				Object[] objects = c.getEnumConstants();
				if (objects == null || objects.length == 0)
					return;
				for (Object obj : objects) {
					Object entryNameO = nameMethod.invoke(obj);
					if (enumChatFormatName.equals(entryNameO)) {
						this.enumChatFormat = obj;
						return;
					}
				}
			} catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException |
							InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Gets the EnumChatFormat for this color
		 * 
		 * @return The EnumChatFormat
		 */
		public Object getEnumChatFormat() {
			return enumChatFormat;
		}

		/**
		 * Gets a random color of {@link ChatColor}
		 * 
		 * @return A random colro of {@link ChatColor}
		 */
		public static ChatColor random() {
			int id = new Random().nextInt(16);
			return MarkerColor.of(id).color;
		}

		/**
		 * Gets the package id for this color
		 * 
		 * @return The package id
		 */
		public int getPackageId() {
			return id;
		}

		/**
		 * Gets a color based on its package id
		 * 
		 * @param id The package id of the color
		 * @return The {@link MarkerColor} with the given package id, {@link #NONE} if
		 *         no color has that id
		 */
		public static MarkerColor of(int id) {
			for (MarkerColor mc : values())
				if (mc.id == id)
					return mc;
			return NONE;
		}

		/**
		 * Gets a color based on its {@link ChatColor}
		 * 
		 * @param color The {@link ChatColor} of the color
		 * @return The {@link MarkerColor} with the given {@link ChatColor},
		 *         {@link #NONE} if no color has that {@link ChatColor}
		 */
		public static MarkerColor of(ChatColor color) {
			if (color == null)
				return NONE;
			for (MarkerColor mc : values())
				if (mc.color != null)
					if (mc.color == color)
						return mc;
			return NONE;
		}

	}

}