package me.nemo_64.spigot.spigotutils.marker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.nemo_64.spigot.spigotutils.xseries.ReflectionUtils;

/*
 * TODO:
 * 	Test unmarking system
 * 	Test marking system
 * 	Test color changing system
 * 	Create moving system
 */

/**
 * Utility class for marking block using shulkers and NMS
 * 
 * @version 3.0
 * @author MrNemo64
 */
public class LocationMarker2 {

	// Spawning
	private static Constructor<?> packetPlayOutSpawnEntityLivingConstructor;
	private static Constructor<?> packetPlayOutEntityMetadataConstructor;

	// Creating shulker
	private static Constructor<?> entityShulkerConstructor;
	private static Class<?> entityShulkerClass;
	private static Class<?> craftWorldClass;
	private static Method worldGetHandleMethod;
	private static Method entityShulkerSetLocationMethod;
	private static Method entityShulkerSetInvisibleMethod;
	private static Method entityShulkerSetGlowingMethod;
	private static Method entityShulkerGetIdMethod;
	private static Method entityShulkerGetDataWatcherMethod;
	private static Method entityShulkerGetUuidMethod;
	private static Object entityTypesShulkerObject;

	// Coloring shulker
	private static Constructor<?> packetPlayOutScoreboardTeamConstructor;
	private static Field packetPlayOutScoreboardTeamCreateField;
	private static Field packetPlayOutScoreboardTeamOptionsField;
	private static Field packetPlayOutScoreboardTeamEntitiesField;
	private static Field packetPlayOutScoreboardTeamNameField;
	private static Field packetPlayOutScoreboardTeamColorField;

	// Deleting shulker
	private static Constructor<?> packetPlayOutEntityDestroyConstructor;

	/**
	 * Marks all the given locations to the player in the thread that is called
	 * 
	 * @see #markLocations(Player, ChatColor, Location...)
	 * @param player    The player
	 * @param color     The color of the shulkers
	 * @param locations The locations to mark
	 */
	public static SeveralShulkerMarker markLocationsSync(Player player, ChatColor color, Location... locations) {
		if (entityShulkerGetUuidMethod == null || entityShulkerGetIdMethod == null) {
			try {
				loadReflectionShulker();
			} catch(NoSuchMethodException | SecurityException | NoSuchFieldException | IllegalArgumentException |
							IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		}
		if (packetPlayOutSpawnEntityLivingConstructor == null || packetPlayOutEntityMetadataConstructor == null) {
			try {
				loadReflectionSpawnEntityes();
			} catch(NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				return null;
			}
		}

		MarkerColor markerColor = MarkerColor.of(color);
		// For every spawned shulker we need 2 packets: to spawn and the data watcher
		List<Object> packetsToSend = new ArrayList<Object>(locations.length * 2);
		List<ShulkerMarker> markers = new ArrayList<ShulkerMarker>(locations.length);

		// Create all the shulkers
		for (Location location : locations) {
			if (location == null)
				continue;
			Object shulker = createShulkerAt(location);
			if (shulker == null)
				continue;
			Object datawatcher = getDataWatcherOfShulker(shulker);
			if (datawatcher == null)
				continue;
			try {
				Object uuidO = entityShulkerGetUuidMethod.invoke(shulker);
				Object idO = entityShulkerGetIdMethod.invoke(shulker);
				if (uuidO != null && uuidO instanceof String && idO != null && idO instanceof Integer) {
					packetsToSend.add(packetPlayOutSpawnEntityLivingConstructor.newInstance(shulker));
					packetsToSend.add(packetPlayOutEntityMetadataConstructor.newInstance(idO, datawatcher, true));
					markers.add(new ShulkerMarker((Integer) idO, (String) uuidO, player, false));
				}
			} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException |
							InstantiationException e) {
				e.printStackTrace();
			}
		}

		ReflectionUtils.sendPacketSync(player, packetsToSend.toArray());
		SeveralShulkerMarker shulkerMarker = new SeveralShulkerMarker(player, createRandomTeamName(5), markers);
		shulkerMarker.setSpawned(true);

		// Color
		if (markerColor != MarkerColor.NONE && markerColor != MarkerColor.WHITE)
			changeColorSync(player, shulkerMarker.teamName, markerColor, shulkerMarker.getShulkersUniqueId());

		return shulkerMarker;
	}

	/**
	 * Marks all the given locations to the player asynchronously since packets are
	 * thread safe. This is achived by callig
	 * {@link LocationMarker2#markLocationsSync(Player, ChatColor, Location...)} in
	 * a completable future.
	 * 
	 * @see #markLocationsSync(Player, ChatColor, Location...)
	 * @param player The player
	 * @param ids    The ids of the shulkers that mark the locations to be removed
	 * @return The async thread handling the packet
	 */
	public static CompletableFuture<SeveralShulkerMarker> markLocations(Player player, ChatColor color,
			Location... locations) {
		return CompletableFuture.supplyAsync(() -> {
			return markLocationsSync(player, color, locations);
		}).exceptionally(ex -> {
			ex.printStackTrace();
			return null;
		});
	}

	/**
	 * Unmarks all the given locations to the player in the thread that is called
	 * 
	 * @see #unmarkLocations(Player, int...)
	 * @param player The player
	 * @param ids    The ids of the shulkers that mark the locations to be removed
	 */
	public static void unmarkLocationsSync(Player player, int... ids) {
		if (packetPlayOutEntityDestroyConstructor == null) {
			try {
				loadReflectionEntityDestroy();
			} catch(NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				return;
			}
		}
		try {
			Object packetPlayOutEntityDestroy = packetPlayOutEntityDestroyConstructor.newInstance(ids);
			ReflectionUtils.sendPacketSync(player, packetPlayOutEntityDestroy);
		} catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Unmarks all the given locations to the player asynchronously since packets
	 * are thread safe. This is achived by callig
	 * {@link LocationMarker2#unmarkLocationsSync(Player, int...)} in a completable
	 * future.
	 * 
	 * @see #unmarkLocationsSync(Player, int...)
	 * @param player The player
	 * @param ids    The ids of the shulkers that mark the locations to be removed
	 * @return The async thread handling the packet
	 */
	public static CompletableFuture<Void> unmarkLocations(Player player, int... ids) {
		return CompletableFuture.runAsync(() -> {
			unmarkLocationsSync(player, ids);
		}).exceptionally((ex) -> {
			ex.printStackTrace();
			return null;
		});
	}

	/**
	 * Changes the color of all the given shulkers in the thread that is called
	 * 
	 * @see #changeColor(Player, String, MarkerColor, String...)
	 * @param player   The player that will see the change
	 * @param teamName The name of the team. Can be null
	 * @param color    The new color
	 * @param shulkers The uuids of the shulkers as string
	 */
	public static void changeColorSync(Player player, String teamName, MarkerColor color, String... shulkers) {
		Object packet = createTeamPacket(color, teamName == null ? createRandomTeamName(5) : teamName, shulkers);
		if (packet != null)
			ReflectionUtils.sendPacketSync(player, packet);
	}

	/**
	 * Changes the color of all the given shulkers asynchronously since packets are
	 * thread safe. This is achived by callig
	 * {@link #changeColorSync(Player, String, MarkerColor, String...)} in a
	 * completable future.
	 * 
	 * @see #changeColorSync(Player, String, MarkerColor, String...)
	 * @param player   The player that will see the change
	 * @param teamName The name of the team. Can be null
	 * @param color    The new color
	 * @param shulkers The uuids of the shulkers as string
	 */
	public static CompletableFuture<Void> changeColor(Player player, String teamName, MarkerColor color,
			String... shulkers) {
		return CompletableFuture.runAsync(() -> {
			changeColorSync(player, teamName, color, shulkers);
		}).exceptionally((ex) -> {
			ex.printStackTrace();
			return null;
		});
	}

	private static void loadReflectionEntityDestroy() throws NoSuchMethodException, SecurityException {
		// We use this package to remove entityes
		Class<?> packetPlayOutEntityDestroyClass = ReflectionUtils.getNMSClass("PacketPlayOutEntityDestroy");
		if (packetPlayOutEntityDestroyClass != null)
			packetPlayOutEntityDestroyConstructor = packetPlayOutEntityDestroyClass.getConstructor(int[].class);
	}

	private static void loadReflectionShulker() throws NoSuchMethodException, SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {
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
					loadReflectionEntityTypesShulker();
				}
			} else {
				entityShulkerConstructor = entityShulkerClass.getConstructor(nmsWorldClass);
			}
		}

		// Need the CraftWorld to cast worlds to craftworlds
		craftWorldClass = ReflectionUtils.getCraftClass("CraftWorld");
		if (craftWorldClass != null) {
			// Need the getHandle to get the nms version of the world
			worldGetHandleMethod = craftWorldClass.getMethod("getHandle");
		}
	}

	private static void loadReflectionEntityTypesShulker()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Class<?> entityTypesClass = ReflectionUtils.getNMSClass("EntityTypes");
		Field entityTypesShulkerField = entityTypesClass.getField("SHULKER");
		if (entityTypesShulkerField != null)
			entityTypesShulkerObject = entityTypesShulkerField.get(null);
	}

	private static void loadReflectionSpawnEntityes() throws NoSuchMethodException, SecurityException {
		// We need this class to get the packet to spawn the shulkers
		Class<?> entityLivingClass = ReflectionUtils.getNMSClass("EntityLiving");
		// The packet that spawns an entity client-side
		Class<?> packetPlayOutSpawnEntityLivingClass = ReflectionUtils.getNMSClass("PacketPlayOutSpawnEntityLiving");
		if (packetPlayOutSpawnEntityLivingClass != null && entityLivingClass != null) {
			packetPlayOutSpawnEntityLivingConstructor = packetPlayOutSpawnEntityLivingClass.getConstructor(entityLivingClass);
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

	}

	private static void loadReflectionCreateTeam() throws NoSuchFieldException, SecurityException, NoSuchMethodException {
		Class<?> packetPlayOutScoreboardTeamClass = ReflectionUtils.getNMSClass("PacketPlayOutScoreboardTeam");
		if (packetPlayOutScoreboardTeamClass == null)
			return;

		packetPlayOutScoreboardTeamConstructor = packetPlayOutScoreboardTeamClass.getConstructor();

		packetPlayOutScoreboardTeamCreateField = packetPlayOutScoreboardTeamClass.getDeclaredField("i");
		if (packetPlayOutScoreboardTeamCreateField != null)
			packetPlayOutScoreboardTeamCreateField.setAccessible(true);

		packetPlayOutScoreboardTeamOptionsField = packetPlayOutScoreboardTeamClass.getDeclaredField("j");
		if (packetPlayOutScoreboardTeamOptionsField != null)
			packetPlayOutScoreboardTeamOptionsField.setAccessible(true);

		packetPlayOutScoreboardTeamEntitiesField = packetPlayOutScoreboardTeamClass.getDeclaredField("h");
		if (packetPlayOutScoreboardTeamEntitiesField != null)
			packetPlayOutScoreboardTeamEntitiesField.setAccessible(true);

		packetPlayOutScoreboardTeamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
		if (packetPlayOutScoreboardTeamNameField != null)
			packetPlayOutScoreboardTeamNameField.setAccessible(true);

		packetPlayOutScoreboardTeamColorField = packetPlayOutScoreboardTeamClass.getDeclaredField("g");
		if (packetPlayOutScoreboardTeamColorField != null)
			packetPlayOutScoreboardTeamColorField.setAccessible(true);
	}

	private static Object createShulkerAt(Location location) {
		if (entityShulkerConstructor == null || worldGetHandleMethod == null || entityShulkerSetLocationMethod == null
				|| entityShulkerSetInvisibleMethod == null || entityShulkerSetGlowingMethod == null) {
			try {
				loadReflectionShulker();
			} catch(NoSuchMethodException | SecurityException | NoSuchFieldException | IllegalArgumentException |
							IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		}
		if (ReflectionUtils.VERSION_NUMBER >= 14 && entityTypesShulkerObject == null) {
			try {
				loadReflectionEntityTypesShulker();
			} catch(NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		}
		Object w = worldToCraftWorld(location.getWorld());
		if (w == null)
			return null;
		try {
			Object shulker;
			if (ReflectionUtils.VERSION_NUMBER >= 14) {
				shulker = entityShulkerConstructor.newInstance(entityTypesShulkerObject, worldGetHandleMethod.invoke(w));
			} else {
				shulker = entityShulkerConstructor.newInstance(worldGetHandleMethod.invoke(w));
			}
			if (shulker == null)
				return null;
			entityShulkerSetLocationMethod.invoke(shulker, location.getX(), location.getY(), location.getZ(), 0f, 0f);
			entityShulkerSetInvisibleMethod.invoke(shulker, true);
			entityShulkerSetGlowingMethod.invoke(shulker, true);
			return shulker;
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

	private static Object getDataWatcherOfShulker(Object shulker) {
		if (entityShulkerGetDataWatcherMethod == null) {
			try {
				loadReflectionShulker();
			} catch(NoSuchMethodException | SecurityException | NoSuchFieldException | IllegalArgumentException |
							IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		}
		try {
			return entityShulkerGetDataWatcherMethod.invoke(shulker);
		} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static Object createTeamPacket(MarkerColor color, String teamName, String... shulkers) {
		if (shulkers == null || shulkers.length == 0)
			return null;
		if (packetPlayOutScoreboardTeamConstructor == null || packetPlayOutScoreboardTeamCreateField == null
				|| packetPlayOutScoreboardTeamOptionsField == null || packetPlayOutScoreboardTeamEntitiesField == null
				|| packetPlayOutScoreboardTeamNameField == null || packetPlayOutScoreboardTeamColorField == null) {
			try {
				loadReflectionCreateTeam();
			} catch(NoSuchFieldException | SecurityException | NoSuchMethodException e) {
				e.printStackTrace();
				return null;
			}
		}
		try {
			Object teamPacketObject = packetPlayOutScoreboardTeamConstructor.newInstance();
			packetPlayOutScoreboardTeamCreateField.set(teamPacketObject, 0); // Create a team
			packetPlayOutScoreboardTeamOptionsField.set(teamPacketObject, 0); // idk it just works
			packetPlayOutScoreboardTeamEntitiesField.set(teamPacketObject, (Collection<String>) Arrays.asList(shulkers));
			packetPlayOutScoreboardTeamNameField.set(teamPacketObject, teamName); // Set the team name
			packetPlayOutScoreboardTeamColorField.set(teamPacketObject, // Set the color
					color != null ? color.getEnumChatFormat() : MarkerColor.WHITE.getEnumChatFormat());
			return teamPacketObject;
		} catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
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

	private static String createRandomTeamName(int size) {
		byte[] array = new byte[size];
		new Random().nextBytes(array);
		return new String(array, Charset.forName("UTF-8"));
	}

	/**
	 * Class to store data about several shulker markers
	 * 
	 * @author MrNemo64
	 * @version 1.0
	 */
	public static class SeveralShulkerMarker implements Iterable<ShulkerMarker> {

		private ShulkerMarker[] shulkers;
		private String teamName;
		private Player player;
		private boolean spawned;

		public SeveralShulkerMarker(Player player, String teamName, ShulkerMarker... markers) {
			this.player = player;
			this.teamName = teamName;
			this.shulkers = markers;
			this.spawned = false;
		}

		public SeveralShulkerMarker(Player player, String teamName, List<ShulkerMarker> markers) {
			this(player, teamName, markers.toArray(new ShulkerMarker[0]));
		}

		/**
		 * Changes the color of all the locations handled by this
		 * {@link SeveralShulkerMarker} object. This is achived by calling the
		 * {@link LocationMarker2#changeColor(Player, String, MarkerColor, String...)}
		 * method
		 * 
		 * @param newColor
		 * @return The async thread handling the packet
		 */
		public CompletableFuture<Void> changeColor(ChatColor newColor) {
			return LocationMarker2.changeColor(player, teamName, MarkerColor.of(newColor), getShulkersUniqueId());
		}

		/**
		 * Changes the color of all the locations handled by this
		 * {@link SeveralShulkerMarker} object. This is achived by calling the
		 * {@link LocationMarker2#changeColorSync(Player, String, MarkerColor, String...)}
		 * method
		 * 
		 * @see #changeColor(ChatColor)
		 * @param newColor
		 */
		public void changeColorSync(ChatColor newColor) {
			LocationMarker2.changeColorSync(player, teamName, MarkerColor.of(newColor), getShulkersUniqueId());
		}

		/**
		 * Unmarks all the marked locations handled by this {@link SeveralShulkerMarker}
		 * object. This is achived by calling the
		 * {@link LocationMarker2#unmarkLocations(Player, int...)} method
		 * 
		 * @see #unmarkSync()
		 * @return The async thread handling the packet
		 */
		public CompletableFuture<Void> unmark() {
			return LocationMarker2.unmarkLocations(player, getShulkersId());
		}

		/**
		 * Unmarks all the marked locations handled by this {@link SeveralShulkerMarker}
		 * object. This is achived by calling the
		 * {@link LocationMarker2#unmarkLocationsSync(Player, int...)} method
		 * 
		 * @see #unmark()
		 */
		public void unmarkSync() {
			LocationMarker2.unmarkLocationsSync(player, getShulkersId());
		}

		@Override
		public Iterator<ShulkerMarker> iterator() {
			return Arrays.asList(shulkers).iterator();
		}

		/**
		 * Gets all the uuids of the shulkers as a string
		 */
		public String[] getShulkersUniqueId() {
			List<String> uuids = new ArrayList<String>(shulkers.length);
			for (ShulkerMarker shulkerMarker : shulkers)
				uuids.add(shulkerMarker.uuid);
			return uuids.toArray(new String[0]);
		}

		/**
		 * Gets all the ids of the shulkers as an integer
		 */
		public int[] getShulkersId() {
			List<Integer> ids = new ArrayList<Integer>(shulkers.length);
			for (ShulkerMarker shulkerMarker : shulkers)
				ids.add(shulkerMarker.id);
			return ids.stream().mapToInt(i -> i).toArray();
		}

		public String getTeamName() {
			return teamName;
		}

		protected void setTeamName(String teamName) {
			this.teamName = teamName;
		}

		protected void setSpawned(boolean spawned) {
			this.spawned = spawned;
			for (ShulkerMarker marker : shulkers)
				marker.setSpawned(spawned);
		}

		public boolean areSpawned() {
			return spawned;
		}

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

		public ShulkerMarker(Integer id, String uuid, Player player, boolean spawned) {
			super();
			this.id = id;
			this.uuid = uuid;
			this.player = player;
			this.spawned = spawned;
		}

		protected ShulkerMarker(Player player) {
			this(null, null, player, false);
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
			LocationMarker2.unmarkLocations(getPlayer(), getId());
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