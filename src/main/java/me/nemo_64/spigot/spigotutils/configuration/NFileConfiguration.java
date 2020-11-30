package me.nemo_64.spigot.spigotutils.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.google.common.base.Charsets;

public class NFileConfiguration {

	private FileConfiguration newConfig = null;
	private File configFile = null;
	private Plugin plugin;
	private String name;

	public NFileConfiguration(Plugin plugin, String name) {
		this.name = name == null ? "config.yml" : name;
		this.plugin = plugin;
		setFile(new File(plugin.getDataFolder(), this.name));
		reloadConfig();
	}

	public NFileConfiguration(Plugin plugin) {
		this(plugin, null);
	}

	public FileConfiguration getConfig() {
		if (newConfig == null)
			reloadConfig();
		return newConfig;
	}

	public void reloadConfig() {
		newConfig = YamlConfiguration.loadConfiguration(configFile);
		final InputStream defConfigStream = plugin.getResource(this.name);
		if (defConfigStream == null)
			return;
		newConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
	}

	public void saveConfig() {
		try {
			getConfig().save(configFile);
		} catch(IOException ex) {
			plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
		}
	}

	public void setFile(File file) {
		if (file.getParentFile() != null)
			if (!file.getParentFile().exists())
				file.getParentFile().mkdirs();
		if (!file.exists())
			try {
				file.createNewFile();
			} catch(IOException e) {
				plugin.getLogger().log(Level.SEVERE, "Couldn't create the file " + file.getAbsolutePath(), e);
			}
		this.configFile = file;
	}

	public Map<String, Object> getValues(boolean deep) {
		return newConfig.getValues(deep);
	}

	public boolean contains(String path) {
		return newConfig.contains(path);
	}

	public boolean contains(String path, boolean ignoreDefault) {
		return newConfig.contains(path, ignoreDefault);
	}

	public boolean equals(Object obj) {
		return newConfig.equals(obj);
	}

	public Set<String> getKeys(boolean deep) {
		return newConfig.getKeys(deep);
	}

	public boolean isSet(String path) {
		return newConfig.isSet(path);
	}

	public void set(String path, Object value) {
		newConfig.set(path, value);
	}

	public Object get(String path) {
		return newConfig.get(path);
	}

	public Object get(String path, Object def) {
		return newConfig.get(path, def);
	}

	public String getString(String path) {
		return newConfig.getString(path);
	}

	public String getString(String path, String def) {
		return newConfig.getString(path, def);
	}

	public boolean isString(String path) {
		return newConfig.isString(path);
	}

	public int getInt(String path) {
		return newConfig.getInt(path);
	}

	public int getInt(String path, int def) {
		return newConfig.getInt(path, def);
	}

	public boolean isInt(String path) {
		return newConfig.isInt(path);
	}

	public boolean getBoolean(String path) {
		return newConfig.getBoolean(path);
	}

	public boolean getBoolean(String path, boolean def) {
		return newConfig.getBoolean(path, def);
	}

	public boolean isBoolean(String path) {
		return newConfig.isBoolean(path);
	}

	public double getDouble(String path) {
		return newConfig.getDouble(path);
	}

	public double getDouble(String path, double def) {
		return newConfig.getDouble(path, def);
	}

	public boolean isDouble(String path) {
		return newConfig.isDouble(path);
	}

	public long getLong(String path) {
		return newConfig.getLong(path);
	}

	public long getLong(String path, long def) {
		return newConfig.getLong(path, def);
	}

	public boolean isLong(String path) {
		return newConfig.isLong(path);
	}

	public List<?> getList(String path) {
		return newConfig.getList(path);
	}

	public List<?> getList(String path, List<?> def) {
		return newConfig.getList(path, def);
	}

	public boolean isList(String path) {
		return newConfig.isList(path);
	}

	public List<String> getStringList(String path) {
		return newConfig.getStringList(path);
	}

	public List<Integer> getIntegerList(String path) {
		return newConfig.getIntegerList(path);
	}

	public List<Boolean> getBooleanList(String path) {
		return newConfig.getBooleanList(path);
	}

	public List<Double> getDoubleList(String path) {
		return newConfig.getDoubleList(path);
	}

	public List<Float> getFloatList(String path) {
		return newConfig.getFloatList(path);
	}

	public List<Long> getLongList(String path) {
		return newConfig.getLongList(path);
	}

	public List<Byte> getByteList(String path) {
		return newConfig.getByteList(path);
	}

	public List<Character> getCharacterList(String path) {
		return newConfig.getCharacterList(path);
	}

	public List<Short> getShortList(String path) {
		return newConfig.getShortList(path);
	}

	public List<Map<?, ?>> getMapList(String path) {
		return newConfig.getMapList(path);
	}

	public <T> T getObject(String path, Class<T> clazz) {
		return newConfig.getObject(path, clazz);
	}

	public <T> T getObject(String path, Class<T> clazz, T def) {
		return newConfig.getObject(path, clazz, def);
	}

	public Vector getVector(String path) {
		return newConfig.getVector(path);
	}

	public Vector getVector(String path, Vector def) {
		return newConfig.getVector(path, def);
	}

	public boolean isVector(String path) {
		return newConfig.isVector(path);
	}

	public OfflinePlayer getOfflinePlayer(String path) {
		return newConfig.getOfflinePlayer(path);
	}

	public OfflinePlayer getOfflinePlayer(String path, OfflinePlayer def) {
		return newConfig.getOfflinePlayer(path, def);
	}

	public boolean isOfflinePlayer(String path) {
		return newConfig.isOfflinePlayer(path);
	}

	public ItemStack getItemStack(String path) {
		return newConfig.getItemStack(path);
	}

	public ItemStack getItemStack(String path, ItemStack def) {
		return newConfig.getItemStack(path, def);
	}

	public boolean isItemStack(String path) {
		return newConfig.isItemStack(path);
	}

	public Color getColor(String path) {
		return newConfig.getColor(path);
	}

	public Color getColor(String path, Color def) {
		return newConfig.getColor(path, def);
	}

	public boolean isColor(String path) {
		return newConfig.isColor(path);
	}

	public Location getLocation(String path) {
		return newConfig.getLocation(path);
	}

	public Location getLocation(String path, Location def) {
		return newConfig.getLocation(path, def);
	}

	public boolean isLocation(String path) {
		return newConfig.isLocation(path);
	}

	public ConfigurationSection getConfigurationSection(String path) {
		return newConfig.getConfigurationSection(path);
	}

	public boolean isConfigurationSection(String path) {
		return newConfig.isConfigurationSection(path);
	}

}