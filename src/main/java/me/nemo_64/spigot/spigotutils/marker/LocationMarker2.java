package me.nemo_64.spigot.spigotutils.marker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.nemo_64.spigot.spigotutils.xseries.ReflectionUtils;

/*
 * TODO:
 * 	Test unmarking system
 */

/**
 * Utility class for marking block using shulkers and NMS
 * 
 * @version 3.0
 * @author MrNemo64
 */
public class LocationMarker2 {

	private static Constructor<?> packetPlayOutEntityDestroyConstructor;

	/**
	 * Unmarks all the given locations to the player
	 * 
	 * @see #unmarkLocations(Player, int...)
	 * @param player The player
	 * @param ids    The ids of the shulkers that mark the locations to be removed
	 */
	public static void unmarkLocationsSync(Player player, int... ids) {
		if (packetPlayOutEntityDestroyConstructor == null) {
			try {
				loadPacketEntityDestroy();
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

	private static void loadPacketEntityDestroy() throws NoSuchMethodException, SecurityException {
		// We use this package to remove entityes
		Class<?> packetPlayOutEntityDestroyClass = ReflectionUtils.getNMSClass("PacketPlayOutEntityDestroy");
		if (packetPlayOutEntityDestroyClass != null)
			packetPlayOutEntityDestroyConstructor = packetPlayOutEntityDestroyClass.getConstructor(int[].class);
	}

	/**
	 * Class to store data about several shulker markers
	 * 
	 * @author MrNemo64
	 * @version 1.0
	 */
	public static class SeveralSulkerMarker implements Iterable<ShulkerMarker> {

		private ShulkerMarker[] shulkers;
		private Player player;

		public SeveralSulkerMarker(Player player, ShulkerMarker... markers) {
			this.player = player;
			this.shulkers = markers;
		}

		public SeveralSulkerMarker(Player player, List<ShulkerMarker> markers) {
			this(player, markers.toArray(new ShulkerMarker[0]));
		}

		/**
		 * Unmarks all the marked locations handled by this {@link SeveralSulkerMarker}
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
		 * Unmarks all the marked locations handled by this {@link SeveralSulkerMarker}
		 * object. This is achived by calling the
		 * {@link LocationMarker2#unmarkLocationsSync(Player, int...)} method
		 * 
		 * @see #unmark()
		 * @return The async thread handling the packet
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