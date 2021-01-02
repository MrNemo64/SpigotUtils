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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.nemo_64.spigot.spigotutils.xseries.ReflectionUtils;

/**
 * Utility class for marking block using shulkers and NMS
 * 
 * @version 3.0
 * @author MrNemo64
 */
public class LocationMarker {

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

	// Moving shulkers - Not working yet
	private static Constructor<?> packetPlayOutEntityTeleportConstructor;
	private static Field packetPlayOutEntityTeleportShulkerIdField;
	private static Field packetPlayOutEntityTeleportXField;
	private static Field packetPlayOutEntityTeleportYField;
	private static Field packetPlayOutEntityTeleportZField;
	private static Field packetPlayOutEntityTeleportYawField;
	private static Field packetPlayOutEntityTeleportPitchField;
	private static Field packetPlayOutEntityTeleportOnGroundField;

	private LocationMarker() {}

	/**
	 * Marks all the given locations to the player in the thread that is called
	 * 
	 * @see #markLocations(Player, ChatColor, Location...)
	 * @param players   The player
	 * @param color     The color of the shulkers
	 * @param locations The locations to mark
	 * @return The markers
	 */
	public static MarkersDataManager markLocationsSync(Player[] players, ChatColor color, Location... locations) {
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
		List<MarkerData> markers = new ArrayList<MarkerData>(locations.length);

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
					markers.add(new MarkerData((Integer) idO, ((UUID) uuidO).toString(), location.clone()));
				}
			} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException |
							InstantiationException e) {
				e.printStackTrace();
			}
		}

		Object[] packets = packetsToSend.toArray();
		for (Player player : players)
			ReflectionUtils.sendPacketSync(player, packets);
		MarkersDataManager shulkerMarker = new MarkersDataManager(markers.toArray(new MarkerData[0]), players,
				MarkerColor.of(color));

		// Color
		if (markerColor != MarkerColor.NONE && markerColor != MarkerColor.WHITE)
			changeColorSync(players, markerColor, shulkerMarker.getMarkersUniqueIdAsArray());

		return shulkerMarker;
	}

	/**
	 * Marks all the given locations to the player asynchronously since packets are
	 * thread safe. This is achived by callig
	 * {@link LocationMarker#markLocationsSync(Player, ChatColor, Location...)} in a
	 * {@link CompletableFuture}. <br>
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
	 * @return The async thread handling the packet with the markers
	 */
	public static CompletableFuture<MarkersDataManager> markLocations(Player[] player, ChatColor color,
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
	 * @return If the packets were sent
	 */
	public static boolean unmarkLocationsSync(Player[] players, int... ids) {
		if (packetPlayOutEntityDestroyConstructor == null) {
			try {
				loadReflectionEntityDestroy();
			} catch(NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				return false;
			}
		}
		try {
			Object packetPlayOutEntityDestroy = packetPlayOutEntityDestroyConstructor.newInstance(ids);
			for (Player player : players)
				ReflectionUtils.sendPacketSync(player, packetPlayOutEntityDestroy);
			return true;
		} catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Unmarks all the given locations to the player asynchronously since packets
	 * are thread safe. This is achived by callig
	 * {@link LocationMarker#unmarkLocationsSync(Player, int...)} in a completable
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
	 * @return The async thread handling the packet with the success of the process
	 */
	public static CompletableFuture<Boolean> unmarkLocations(Player[] player, int... ids) {
		return CompletableFuture.supplyAsync(() -> {
			return unmarkLocationsSync(player, ids);
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
	 * @return The success of the process
	 */
	public static boolean changeColorSync(Player[] players, MarkerColor color, String... shulkers) {
		Object packet = createTeamPacket(color, createRandomTeamName(TEAM_NAME_SIZE), shulkers);
		if (packet == null)
			return false;
		for (Player player : players)
			ReflectionUtils.sendPacketSync(player, packet);
		return true;
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
	 * @return The async thread handling the packet with the success of the process
	 */
	public static CompletableFuture<Boolean> changeColor(Player[] players, MarkerColor color, String... shulkers) {
		return CompletableFuture.supplyAsync(() -> {
			return changeColorSync(players, color, shulkers);
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
	 * @return The success of the process
	 */
	// Temporaly protected
	protected static boolean moveMarkerSync(Player[] players, int id, double x, double y, double z) {
		Object packet = createTeleportPacket(id, x, y, z);
		if (packet == null)
			return false;
		for (Player player : players)
			ReflectionUtils.sendPacketSync(player, packet);
		return true;
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
	 * @return The async thread handling the packet with the success of the process
	 */
	// Temporaly protected
	protected static CompletableFuture<Boolean> moveMarker(Player[] players, int id, double x, double y, double z) {
		return CompletableFuture.supplyAsync(() -> {
			return moveMarkerSync(players, id, x, y, z);
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
	 * Class to store data about several markers
	 * 
	 * @author MrNemo64
	 * @version 2.0
	 */
	public static class MarkersDataManager {

		private final List<MarkerData> markers;
		private final Set<UUID> players;
		private MarkerColor color;

		public MarkersDataManager(List<MarkerData> markers, Set<UUID> players, MarkerColor color) {
			this.markers = markers;
			this.players = players;
			this.color = color;
		}

		public MarkersDataManager(MarkerData[] markers, UUID[] players, MarkerColor color) {
			this(Arrays.asList(markers), new HashSet<UUID>(Arrays.asList(players)), color);
		}

		public MarkersDataManager(MarkerData[] markers, Player[] players, MarkerColor color) {
			this(Arrays.asList(markers), new HashSet<UUID>(players.length), color);
			for (Player player : players)
				this.players.add(player.getUniqueId());
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
		 * @return The async thread handling the packet with the success of the process
		 *         where the index of the boolean indicates the success of the location
		 *         with the same index
		 */
		// Temporaly protected
		protected CompletableFuture<Boolean[]> moveMarkers(Location... newLocations) {
			return CompletableFuture.supplyAsync(() -> {
				return moveMarkersSync(newLocations);
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
		 * @return The success of the process where the index of the boolean indicates
		 *         the success of the location with the same index
		 */
		// Temporaly protected
		protected Boolean[] moveMarkersSync(Location... newLocations) {
			if (newLocations.length > markers.size())
				throw new IllegalArgumentException("Too many locations where provided");
			Boolean[] b = new Boolean[newLocations.length];
			for (int i = 0; i < newLocations.length; i++) {
				b[i] = LocationMarker.moveMarkerSync(getOnlinePlayersAsArray(false), markers.get(i).getId(),
						newLocations[i].getX(), newLocations[i].getY(), newLocations[i].getZ());
				if (b[i]) // only upate the position of the marker if the packet was sent successfully
					markers.get(i).setPos(newLocations[i]);
			}
			return b;
		}

		/**
		 * Changes the color of all the markers handled by this
		 * {@link MarkersDataManager} object asynchronously since packets are thread
		 * safe. This is achived by calling the
		 * {@link LocationMarker#changeColor(Player, MarkerColor, String...)} method in
		 * a {@link CompletableFuture}<br>
		 * If used in the
		 * {@link CompletableFuture#thenAccept(java.util.function.Consumer)},
		 * {@link #changeColorSync(ChatColor)} should probably be used instead to avoid
		 * creating too many threads. Since the thenAccept method stil runs in the
		 * thread created by the completable future all the code will be runned async so
		 * there is no need to run it async again
		 * 
		 * @param newColor
		 * @return The async thread handling the packet with the success of the process
		 */
		public CompletableFuture<Boolean> changeColor(ChatColor newColor) {
			return LocationMarker.changeColor(getOnlinePlayersAsArray(false), MarkerColor.of(newColor),
					getMarkersUniqueIdAsArray());
		}

		/**
		 * Changes the color of all the colors handled by this
		 * {@link MarkersDataManager} object in the threat that is called. This is
		 * achived by calling the
		 * {@link LocationMarker#changeColorSync(Player, MarkerColor, String...)} method
		 * 
		 * @see #changeColor(ChatColor)
		 * @param newColor The new color
		 * @return The success of the process
		 */
		public boolean changeColorSync(ChatColor newColor) {
			return LocationMarker.changeColorSync(getOnlinePlayersAsArray(false), MarkerColor.of(newColor),
					getMarkersUniqueIdAsArray());
		}

		/**
		 * Unmarks all the marked locations handled by this {@link MarkersDataManager}
		 * object asynchronously since packets are thread safe. This is achived by
		 * calling the {@link LocationMarker#unmarkLocations(Player, int...)}<br>
		 * If used in the
		 * {@link CompletableFuture#thenAccept(java.util.function.Consumer)},
		 * {@link #unmarkSync()} should probably be used instead to avoid creating too
		 * many threads. Since the thenAccept method stil runs in the thread created by
		 * the completable future all the code will be runned async so there is no need
		 * to run it async again
		 * 
		 * @see #unmarkSync()
		 * @return The async thread handling the packet with the success of the process
		 */
		public CompletableFuture<Boolean> unmark() {
			return LocationMarker.unmarkLocations(getOnlinePlayersAsArray(false), getMarkersIdAsArray());
		}

		/**
		 * Unmarks all the marked locations handled by this {@link MarkersDataManager}
		 * object. This is achived by calling the
		 * {@link LocationMarker#unmarkLocationsSync(Player, int...)} method
		 * 
		 * @see #unmark()
		 * @return The success of the process
		 */
		public boolean unmarkSync() {
			return LocationMarker.unmarkLocationsSync(getOnlinePlayersAsArray(false), getMarkersIdAsArray());
		}

		/**
		 * Removes the given players from the list
		 * 
		 * @param unmark  If true, the markers will be unmarked for the players
		 * @param players The players to remove
		 * @return The success of the process
		 */
		public boolean removePlayersSync(boolean unmark, Player... players) {
			for (Player player : players)
				this.players.remove(player.getUniqueId());
			if (unmark)
				return LocationMarker.unmarkLocationsSync(players, getMarkersIdAsArray());
			return true;
		}

		/**
		 * Removes the given players from the list
		 * 
		 * @param unmark  If true, the markers will be unmarked for the players
		 * @param players The players to remove
		 * @return The success of the process
		 */
		public boolean removePlayersSync(boolean unmark, UUID... players) {
			Player[] ps = new Player[players.length];
			for (int i = 0; i < players.length; i++)
				ps[i] = Bukkit.getPlayer(players[i]);
			return removePlayersSync(unmark, ps);
		}

		/**
		 * Removes the given players from the list
		 * 
		 * @param unmark  If true, the markers will be unmarked for the players
		 * @param players The players to remove
		 * @return The async thread handling the process with the success of it
		 */
		public CompletableFuture<Boolean> removePlayers(boolean unmark, Player... players) {
			for (Player player : players)
				this.players.remove(player.getUniqueId());
			if (unmark)
				return LocationMarker.unmarkLocations(players, getMarkersIdAsArray());
			return CompletableFuture.supplyAsync(() -> true);
		}

		/**
		 * Removes the given players from the list
		 * 
		 * @param unmark  If true, the markers will be unmarked for the players
		 * @param players The players to remove
		 * @return The async thread handling the process with the success of it
		 */
		public CompletableFuture<Boolean> removePlayers(boolean unmark, UUID... players) {
			Player[] ps = new Player[players.length];
			for (int i = 0; i < players.length; i++)
				ps[i] = Bukkit.getPlayer(players[i]);
			return removePlayers(unmark, ps);
		}

		/**
		 * Removes offline players from the list
		 */
		public void removeOfflinePlayers() {
			Iterator<UUID> iterator = players.iterator();
			UUID uuid;
			while (iterator.hasNext()) {
				uuid = iterator.next();
				if (uuid != null)
					if (Bukkit.getPlayer(uuid) == null) // If is offline
						iterator.remove();
			}
		}

		/**
		 * Adds all the given players to the list and shows them the markers
		 * 
		 * @return The async thread handling the packet with the markers
		 */
		public CompletableFuture<MarkersDataManager> addPlayers(Player... players) {
			List<UUID> uuids = new ArrayList<UUID>(players.length);
			for (Player player : players)
				uuids.add(player.getUniqueId());
			this.players.addAll(uuids);
			return LocationMarker.markLocations(players, getColor().getChatColor(), getLocations());
		}

		/**
		 * Adds all the given players to the list and shows them the markers
		 * 
		 * @return The async thread handling the packet with the markers
		 */
		public CompletableFuture<MarkersDataManager> addPlayers(List<Player> players) {
			return addPlayers(players.toArray(new Player[0]));
		}

		/**
		 * Adds all the given players to the list and shows them the markers
		 * 
		 * @return The markers
		 */
		public MarkersDataManager addPlayersSync(Player... players) {
			List<UUID> uuids = new ArrayList<UUID>(players.length);
			for (Player player : players)
				uuids.add(player.getUniqueId());
			this.players.addAll(uuids);
			return LocationMarker.markLocationsSync(players, getColor().getChatColor(), getLocations());
		}

		/**
		 * Adds all the given players to the list and shows them the markers
		 * 
		 * @return The markers
		 */
		public MarkersDataManager addPlayersSync(List<Player> players) {
			return addPlayersSync(players.toArray(new Player[0]));
		}

		/**
		 * Gets all the uuids of the shulkers as a string
		 */
		public List<String> getMarkersUniqueId() {
			List<String> uuids = new ArrayList<String>(markers.size());
			markers.forEach((marker) -> uuids.add(marker.getUniqueId()));
			return uuids;
		}

		/**
		 * Gets all the ids of the shulkers as an integer
		 */
		public List<Integer> getMarkersId() {
			List<Integer> ids = new ArrayList<Integer>(markers.size());
			markers.forEach((marker) -> ids.add(marker.getId()));
			return ids;
		}

		/**
		 * Gets all the {@link MarkerData} handled by this object
		 */
		public List<MarkerData> getMarkers() {
			return new ArrayList<MarkerData>(markers);
		}

		/**
		 * Gets all the players that see this {@link MarkersDataManager}
		 */
		public List<UUID> getPlayers() {
			return new ArrayList<UUID>(players);
		}

		/**
		 * Gets all the players that are online
		 * 
		 * @param removeOffline If true, all the online players get removed from the
		 *                      list of players
		 */
		public List<Player> getOnlinePlayers(boolean removeOffline) {
			if (removeOffline)
				removeOfflinePlayers();
			List<Player> players = new ArrayList<Player>(this.players.size());
			for (UUID uuid : this.players) {
				Player player = Bukkit.getPlayer(uuid);
				if (players != null)
					players.add(player);
			}
			return players;
		}

		/**
		 * Gets all the uuids of the shulkers as a string
		 */
		public String[] getMarkersUniqueIdAsArray() {
			return getMarkersUniqueId().toArray(new String[0]);
		}

		/**
		 * Gets all the ids of the shulkers as an integer
		 */
		public int[] getMarkersIdAsArray() {
			return markers.stream().mapToInt((marker) -> marker.getId()).toArray();
		}

		/**
		 * Gets all the {@link MarkerData} handled by this object
		 */
		public MarkerData[] getMarkersAsArray() {
			return getMarkers().toArray(new MarkerData[0]);
		}

		/**
		 * Gets all the players that see this {@link MarkersDataManager}
		 */
		public UUID[] getPlayersAsArray() {
			return getPlayers().toArray(new UUID[0]);
		}

		/**
		 * Gets all the players that are online
		 * 
		 * @param removeOffline If true, all the online players get removed from the
		 *                      list of players
		 */
		public Player[] getOnlinePlayersAsArray(boolean removeOffline) {
			return getOnlinePlayers(removeOffline).toArray(new Player[0]);
		}

		/**
		 * Gets the color of the markers. May not be acurate if the color is changed
		 * without using the manager
		 */

		public MarkerColor getColor() {
			return color;
		}

		/**
		 * Sets the color of the markers. This does not update automaticly the colors of
		 * the markers, {@link #changeColor(ChatColor)} or
		 * {@link #changeColorSync(ChatColor)} should be used
		 */
		public void setColor(MarkerColor color) {
			this.color = color;
		}

		/**
		 * Gets the locations of all the shulkers
		 */
		public Location[] getLocations() {
			Location[] l = new Location[markers.size()];
			for (int i = 0; i < markers.size(); i++)
				l[i] = markers.get(i).getPos();
			return l;
		}

		@Override
		public String toString() {
			return "MarkersDataManager{players=" + getPlayers().toString() + ", markers=" + markers.toString() + "}";
		}

	}

	/**
	 * Class to store data about a shulker marker
	 * 
	 * @author MrNemo64
	 * @version 1.0
	 */
	public static class MarkerData {

		final Integer id;
		final String uuid;
		private Location pos;

		public MarkerData(Integer id, String uuid, Location pos) {
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
		 * {@link LocationMarker#moveMarker(Player[], int, double, double, double)} or
		 * {@link LocationMarker#moveMarkerSync(Player[], int, double, double, double)}
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
		 * Gets the chat color of this color
		 */
		public ChatColor getChatColor() {
			return color;
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