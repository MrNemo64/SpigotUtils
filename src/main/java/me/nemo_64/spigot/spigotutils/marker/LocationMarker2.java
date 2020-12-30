package me.nemo_64.spigot.spigotutils.marker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.nemo_64.spigot.spigotutils.xseries.ReflectionUtils;

/*
 * TODO:
 * 	Test moving system
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
	private static final int TEAM_NAME_SIZE = 5;

	// Deleting shulker
	private static Constructor<?> packetPlayOutEntityDestroyConstructor;

	// Moving shulkers
	private static Constructor<?> packetPlayOutEntityTeleportConstructor;
	private static Field packetPlayOutEntityTeleportShulkerIdField;
	private static Field packetPlayOutEntityTeleportXField;
	private static Field packetPlayOutEntityTeleportYField;
	private static Field packetPlayOutEntityTeleportZField;
	private static Field packetPlayOutEntityTeleportYawField;
	private static Field packetPlayOutEntityTeleportPitchField;
	private static Field packetPlayOutEntityTeleportOnGroundField;

	private LocationMarker2() {}

	/**
	 * Marks all the given locations to the player in the thread that is called
	 * 
	 * @see #markLocations(Player, ChatColor, Location...)
	 * @param players   The player
	 * @param color     The color of the shulkers
	 * @param locations The locations to mark
	 */
	public static SeveralShulkerMarker markLocationsSync(Player[] players, ChatColor color, Location... locations) {
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
				loadReflectionSpawn();
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
				if (uuidO != null && uuidO instanceof UUID && idO != null && idO instanceof Integer) {
					packetsToSend.add(packetPlayOutSpawnEntityLivingConstructor.newInstance(shulker));
					packetsToSend.add(packetPlayOutEntityMetadataConstructor.newInstance(idO, datawatcher, true));
					markers.add(new ShulkerMarker((Integer) idO, ((UUID) uuidO).toString(), location.clone()));
				}
			} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException |
							InstantiationException e) {
				e.printStackTrace();
			}
		}

		Object[] packets = packetsToSend.toArray();
		for (Player player : players)
			ReflectionUtils.sendPacketSync(player, packets);
		SeveralShulkerMarker shulkerMarker = new SeveralShulkerMarker(players, markers.toArray(new ShulkerMarker[0]));

		// Color
		if (markerColor != MarkerColor.NONE && markerColor != MarkerColor.WHITE)
			changeColorSync(players, markerColor, shulkerMarker.getShulkersUniqueId());

		return shulkerMarker;
	}

	/**
	 * Marks all the given locations to the player asynchronously since packets are
	 * thread safe. This is achived by callig
	 * {@link LocationMarker2#markLocationsSync(Player, ChatColor, Location...)} in
	 * a {@link CompletableFuture}. <br>
	 * If used in the
	 * {@link CompletableFuture#thenAccept(java.util.function.Consumer)},
	 * {@link #markLocationsSync(Player[], ChatColor, Location...)} should probably
	 * be used instead to avoid creating too many threads. Since the thenAccept
	 * method stil runs in the thread created by the completable future all the code
	 * will be runned async so there is no need to run it async again
	 * 
	 * @see #markLocationsSync(Player, ChatColor, Location...)
	 * @param player The player
	 * @param ids    The ids of the shulkers that mark the locations to be removed
	 * @return The async thread handling the packet
	 */
	public static CompletableFuture<SeveralShulkerMarker> markLocations(Player[] player, ChatColor color,
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
	 * @param players The player
	 * @param ids     The ids of the shulkers that mark the locations to be removed
	 */
	public static void unmarkLocationsSync(Player[] players, int... ids) {
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
			for (Player player : players)
				ReflectionUtils.sendPacketSync(player, packetPlayOutEntityDestroy);
		} catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Unmarks all the given locations to the player asynchronously since packets
	 * are thread safe. This is achived by callig
	 * {@link LocationMarker2#unmarkLocationsSync(Player, int...)} in a completable
	 * future.<br>
	 * If used in the
	 * {@link CompletableFuture#thenAccept(java.util.function.Consumer)},
	 * {@link #unmarkLocationsSync(Player[], int...)} should probably be used
	 * instead to avoid creating too many threads. Since the thenAccept method stil
	 * runs in the thread created by the completable future all the code will be
	 * runned async so there is no need to run it async again
	 * 
	 * @see #unmarkLocationsSync(Player, int...)
	 * @param player The player
	 * @param ids    The ids of the shulkers that mark the locations to be removed
	 * @return The async thread handling the packet
	 */
	public static CompletableFuture<Void> unmarkLocations(Player[] player, int... ids) {
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
	 * @see #changeColor(Player, MarkerColor, String...)
	 * @param players  The player that will see the change
	 * @param color    The new color
	 * @param shulkers The uuids of the shulkers as string
	 */
	public static void changeColorSync(Player[] players, MarkerColor color, String... shulkers) {
		Object packet = createTeamPacket(color, createRandomTeamName(TEAM_NAME_SIZE), shulkers);
		if (packet != null)
			for (Player player : players)
				ReflectionUtils.sendPacketSync(player, packet);
	}

	/**
	 * Changes the color of all the given shulkers asynchronously since packets are
	 * thread safe. This is achived by callig
	 * {@link #changeColorSync(Player, MarkerColor, String...)} in a completable
	 * future.<br>
	 * If used in the
	 * {@link CompletableFuture#thenAccept(java.util.function.Consumer)},
	 * {@link #changeColorSync(Player[], MarkerColor, String...)} should probably be
	 * used instead to avoid creating too many threads. Since the thenAccept method
	 * stil runs in the thread created by the completable future all the code will
	 * be runned async so there is no need to run it async again
	 * 
	 * @see #changeColorSync(Player, MarkerColor, String...)
	 * @param players  The player that will see the change
	 * @param color    The new color
	 * @param shulkers The uuids of the shulkers as string
	 * @return The async thread handling the packet
	 */
	public static CompletableFuture<Void> changeColor(Player[] players, MarkerColor color, String... shulkers) {
		return CompletableFuture.runAsync(() -> {
			changeColorSync(players, color, shulkers);
		}).exceptionally((ex) -> {
			ex.printStackTrace();
			return null;
		});
	}

	/**
	 * Changes the position of all the given shulker in the thread that is called
	 * 
	 * @see #moveMarker(Player[], int, double, double, double)
	 * @param players The player that will see the change
	 * @param id      The id of the shulker
	 * @param x       The new x
	 * @param y       The new y
	 * @param z       The new z
	 */
	public static void moveMarkerSync(Player[] players, int id, double x, double y, double z) {
		Object packet = createTeleportPacket(id, x, y, z);
		if (packet != null)
			for (Player player : players)
				ReflectionUtils.sendPacketSync(player, packet);
	}

	/**
	 * Changes the position of all the given shulker asynchronously since packets
	 * are thread safe. This is achived by callig
	 * {@link #moveMarkerSync(Player[], int, double, double, double)} in a
	 * {@link CompletableFuture}.<br>
	 * If used in the
	 * {@link CompletableFuture#thenAccept(java.util.function.Consumer)},
	 * {@link #moveMarkerSync(Player[], int, double, double, double)} should
	 * probably be used instead to avoid creating too many threads. Since the
	 * thenAccept method stil runs in the thread created by the completable future
	 * all the code will be runned async so there is no need to run it async again
	 * 
	 * @see #moveMarkerSync(Player[], int, double, double, double)
	 * @param players The player that will see the change
	 * @param id      The id of the shulker
	 * @param x       The new x
	 * @param y       The new y
	 * @param z       The new z
	 * @return The async thread handling the packet
	 */
	public static CompletableFuture<Void> moveMarker(Player[] players, int id, double x, double y, double z) {
		return CompletableFuture.runAsync(() -> {
			moveMarkerSync(players, id, x, y, z);
		}).exceptionally(ex -> {
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

	private static void loadReflectionSpawn() throws NoSuchMethodException, SecurityException {
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

		packetPlayOutScoreboardTeamCreateField = ReflectionUtils.getDeclaredField(packetPlayOutScoreboardTeamClass, "i");
		packetPlayOutScoreboardTeamOptionsField = ReflectionUtils.getDeclaredField(packetPlayOutScoreboardTeamClass, "j");
		packetPlayOutScoreboardTeamEntitiesField = ReflectionUtils.getDeclaredField(packetPlayOutScoreboardTeamClass, "h");
		packetPlayOutScoreboardTeamNameField = ReflectionUtils.getDeclaredField(packetPlayOutScoreboardTeamClass, "a");
		packetPlayOutScoreboardTeamColorField = ReflectionUtils.getDeclaredField(packetPlayOutScoreboardTeamClass, "g");
	}

	private static void loadReflectionTeleport() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
		Class<?> packetPlayOutEntityTeleportClass = ReflectionUtils.getNMSClass("PacketPlayOutEntityTeleport");
		if (packetPlayOutEntityTeleportClass == null)
			return;
		packetPlayOutEntityTeleportConstructor = packetPlayOutEntityTeleportClass.getConstructor();

		packetPlayOutEntityTeleportShulkerIdField = ReflectionUtils.getDeclaredField(packetPlayOutEntityTeleportClass, "a");
		packetPlayOutEntityTeleportXField = ReflectionUtils.getDeclaredField(packetPlayOutEntityTeleportClass, "b");
		packetPlayOutEntityTeleportYField = ReflectionUtils.getDeclaredField(packetPlayOutEntityTeleportClass, "c");
		packetPlayOutEntityTeleportZField = ReflectionUtils.getDeclaredField(packetPlayOutEntityTeleportClass, "d");
		packetPlayOutEntityTeleportYawField = ReflectionUtils.getDeclaredField(packetPlayOutEntityTeleportClass, "e");
		packetPlayOutEntityTeleportPitchField = ReflectionUtils.getDeclaredField(packetPlayOutEntityTeleportClass, "f");
		packetPlayOutEntityTeleportOnGroundField = ReflectionUtils.getDeclaredField(packetPlayOutEntityTeleportClass, "g");

		if (entityShulkerSetLocationMethod == null) {
			Class<?> entityShulkerClass = ReflectionUtils.getNMSClass("EntityShulker");
			entityShulkerSetLocationMethod = entityShulkerClass.getMethod("setLocation", double.class, double.class,
					double.class, float.class, float.class);
		}
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

	private static Object createTeleportPacket(int shulkerId, double x, double y, double z) {
		if (packetPlayOutEntityTeleportConstructor == null || packetPlayOutEntityTeleportShulkerIdField == null
				|| packetPlayOutEntityTeleportXField == null || packetPlayOutEntityTeleportYField == null
				|| packetPlayOutEntityTeleportZField == null || packetPlayOutEntityTeleportYawField == null
				|| packetPlayOutEntityTeleportPitchField == null || packetPlayOutEntityTeleportOnGroundField == null) {
			try {
				loadReflectionTeleport();
			} catch(NoSuchMethodException | SecurityException | NoSuchFieldException e) {
				e.printStackTrace();
				return null;
			}
		}
		try {
			Object packet = packetPlayOutEntityTeleportConstructor.newInstance();
			packetPlayOutEntityTeleportShulkerIdField.set(packet, shulkerId);
			packetPlayOutEntityTeleportXField.set(packet, x);
			packetPlayOutEntityTeleportYField.set(packet, y);
			packetPlayOutEntityTeleportZField.set(packet, z);
			packetPlayOutEntityTeleportYawField.set(packet, (byte) 0);
			packetPlayOutEntityTeleportPitchField.set(packet, (byte) 0);
			packetPlayOutEntityTeleportOnGroundField.set(packet, true);
			return packet;
		} catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
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
	public static class SeveralShulkerMarker {

		private final ShulkerMarker[] shulkers;
		private final Player[] players;

		public SeveralShulkerMarker(Player[] players, ShulkerMarker[] markers) {
			this.players = players;
			this.shulkers = markers;
		}

		/**
		 * Moves the markers to a new location asynchronously since packets are thread
		 * safe. This is done calling {@link #moveMarkersSync(Location[])} in a
		 * {@link CompletableFuture}. If the locations are less than the amount of
		 * markers handled by this object only the first markers are moved.<br>
		 * If used in the
		 * {@link CompletableFuture#thenAccept(java.util.function.Consumer)},
		 * {@link #moveMarkersSync(Location[])} should probably be used instead to avoid
		 * creating too many threads. Since the thenAccept method stil runs in the
		 * thread created by the completable future all the code will be runned async so
		 * there is no need to run it async again
		 * 
		 * @param newLocations The new locations
		 * @throws IllegalArgumentException if more locations than markers handles this
		 *                                  object are provided
		 */
		public CompletableFuture<Void> moveMarkers(Location[] newLocations) {
			return CompletableFuture.runAsync(() -> {
				moveMarkersSync(newLocations);
			}).exceptionally((ex) -> {
				ex.printStackTrace();
				return null;
			});
		}

		/**
		 * Moves the markers to a new location in the thread that is called. If the
		 * locations are less than the amount of markers handled by this object only the
		 * first markers are moved
		 * 
		 * @param newLocations The new locations
		 * @throws IllegalArgumentException if more locations than markers handles this
		 *                                  object are provided
		 */
		public void moveMarkersSync(Location[] newLocations) {
			if (newLocations.length > shulkers.length)
				throw new IllegalArgumentException("Too many locations where provided");
			for (int i = 0; i < newLocations.length; i++) {
				LocationMarker2.moveMarkerSync(getPlayers(), shulkers[i].getId(), newLocations[i].getX(),
						newLocations[i].getY(), newLocations[i].getZ());
			}
		}

		/**
		 * Changes the color of all the markers handled by this
		 * {@link SeveralShulkerMarker} object asynchronously since packets are thread
		 * safe. This is achived by calling the
		 * {@link LocationMarker2#changeColor(Player, MarkerColor, String...)} method in
		 * a {@link CompletableFuture}<br>
		 * If used in the
		 * {@link CompletableFuture#thenAccept(java.util.function.Consumer)},
		 * {@link #changeColorSync(ChatColor)} should probably be used instead to avoid
		 * creating too many threads. Since the thenAccept method stil runs in the
		 * thread created by the completable future all the code will be runned async so
		 * there is no need to run it async again
		 * 
		 * @param newColor
		 * @return The async thread handling the packet
		 */
		public CompletableFuture<Void> changeColor(ChatColor newColor) {
			return LocationMarker2.changeColor(players, MarkerColor.of(newColor), getShulkersUniqueId());
		}

		/**
		 * Changes the color of all the colors handled by this
		 * {@link SeveralShulkerMarker} object in the threat that is called. This is
		 * achived by calling the
		 * {@link LocationMarker2#changeColorSync(Player, MarkerColor, String...)}
		 * method
		 * 
		 * @see #changeColor(ChatColor)
		 * @param newColor
		 */
		public void changeColorSync(ChatColor newColor) {
			LocationMarker2.changeColorSync(players, MarkerColor.of(newColor), getShulkersUniqueId());
		}

		/**
		 * Unmarks all the marked locations handled by this {@link SeveralShulkerMarker}
		 * object asynchronously since packets are thread safe. This is achived by
		 * calling the {@link LocationMarker2#unmarkLocations(Player, int...)}<br>
		 * If used in the
		 * {@link CompletableFuture#thenAccept(java.util.function.Consumer)},
		 * {@link #unmarkSync()} should probably be used instead to avoid creating too
		 * many threads. Since the thenAccept method stil runs in the thread created by
		 * the completable future all the code will be runned async so there is no need
		 * to run it async again
		 * 
		 * @see #unmarkSync()
		 * @return The async thread handling the packet
		 */
		public CompletableFuture<Void> unmark() {
			return LocationMarker2.unmarkLocations(players, getShulkersId());
		}

		/**
		 * Unmarks all the marked locations handled by this {@link SeveralShulkerMarker}
		 * object. This is achived by calling the
		 * {@link LocationMarker2#unmarkLocationsSync(Player, int...)} method
		 * 
		 * @see #unmark()
		 */
		public void unmarkSync() {
			LocationMarker2.unmarkLocationsSync(players, getShulkersId());
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

		/**
		 * Gets all the {@link ShulkerMarker} handled by this object
		 */
		public ShulkerMarker[] getShulkers() {
			return shulkers;
		}

		/**
		 * Gets all the players that see this {@link SeveralShulkerMarker}
		 */
		public Player[] getPlayers() {
			return players;
		}

	}

	/**
	 * Class to store data about a shulker marker
	 * 
	 * @author MrNemo64
	 * @version 1.0
	 */
	public static class ShulkerMarker {

		private final Integer id;
		private final String uuid;
		private Location pos;

		public ShulkerMarker(Integer id, String uuid, Location pos) {
			this.id = id;
			this.uuid = uuid;
			this.pos = pos;
		}

		/**
		 * Gets the id of the shulker. Used to delete the shulker
		 * 
		 * @return The id
		 */
		public Integer getId() {
			return id;
		}

		/**
		 * Gets the uuid of the shulker. Used to change the color of the shulker
		 * 
		 * @return
		 */
		public String getUniqueId() {
			return uuid;
		}

		/**
		 * Gets the location of this marker. May not be acurate if the location is
		 * modified using
		 * {@link LocationMarker2#moveMarker(Player[], int, double, double, double)} or
		 * {@link LocationMarker2#moveMarkerSync(Player[], int, double, double, double)}
		 * without updating it with {@link #setPos(Location)}
		 */
		public Location getPos() {
			return pos;
		}

		/**
		 * Sets the location of this marker. Calling this method will not move the
		 * marker automaticly
		 */
		public void setPos(Location pos) {
			this.pos = pos;
		}

		@Override
		public String toString() {
			return "ShulkerMarker{id=" + getId() + ", uuid=" + getUniqueId() + ", pos=" + pos.toString() + "}";
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
		 * Gets a random color of {@link MarkerColor}
		 */
		public static MarkerColor random() {
			int id = new Random().nextInt(16);
			return MarkerColor.of(id);
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