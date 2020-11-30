package me.nemo_64.spigot.spigotutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class YamlUtils {

	private YamlUtils() {}

	public static Map<String, Object> set(Map<String, Object> map, String key, Location pos) {
		Map<String, Object> loc = set(null, "x", pos.getX());
		set(loc, "y", pos.getY());
		set(loc, "z", pos.getZ());
		set(loc, "world", pos.getWorld().getName());
		return set(map, key, loc);
	}

	public static Location getLocation(Map<String, Object> map, String key) {
		if (map.get(key) == null || !(map.get(key) instanceof Map<?, ?>))
			return null;
		Map<?, ?> loc = (Map<?, ?>) map.get(key);
		double x = loc.containsKey("x") && loc.get("x") instanceof Double ? (Double) loc.get("x") : 0.0;
		double y = loc.containsKey("y") && loc.get("y") instanceof Double ? (Double) loc.get("y") : 0.0;
		double z = loc.containsKey("z") && loc.get("z") instanceof Double ? (Double) loc.get("z") : 0.0;
		World w = Bukkit
				.getWorld(loc.containsKey("world") && loc.get("world") instanceof String ? (String) loc.get("world") : "world");
		return new Location(w, x, y, z);
	}

	@Nonnull
	public static Map<String, Object> set(Map<String, Object> map, String key, Object val) {
		if (map == null)
			map = createMap();
		if (key != null && val != null) {
			if (val instanceof List<?>) {
				if (!((List<?>) val).isEmpty())
					map.put(key, val);
			} else {
				map.put(key, val);
			}
		}
		return map;
	}

	@Nonnull
	public static <T> List<T> getList(Map<String, Object> map, String key, Class<T> clazz) {
		List<T> list = new ArrayList<T>();
		Object obj = map.get(key);
		if (obj == null || !(obj instanceof List<?>) || ((List<?>) obj).isEmpty())
			return list;
		List<?> l = (List<?>) obj;
		for (Object element : l) {
			if (clazz.isInstance(element))
				list.add(clazz.cast(obj));
		}
		return list;
	}

	public static <T> T get(Map<String, Object> map, String key, T def) {
		return get(map, key, def, def.getClass());
	}

	public static <T> T get(Map<String, Object> map, String key, Class<?> clazz) {
		return get(map, key, null, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Map<String, Object> map, String key, T def, Class<?> clazz) {
		Object obj = map.get(key);
		if (obj == null || !clazz.isInstance(obj))
			return def;
		return (T) clazz.cast(obj);
	}

	public static Map<String, Object> createMap() {
		return new HashMap<String, Object>();
	}

}