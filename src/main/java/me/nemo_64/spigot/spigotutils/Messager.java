package me.nemo_64.spigot.spigotutils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Messager {

	private String INFO_PREFIX;
	private String WARN_PREFIX;

	private CommandSender reciver;

	public Messager(String infoPrefix, String warnPrefix, CommandSender reciver) {
		super();
		INFO_PREFIX = infoPrefix;
		WARN_PREFIX = warnPrefix;
		this.reciver = reciver;
	}

	public Messager(CommandSender reciver) {
		this("&6&l[i]&r ", "&c&l[!]&r ", reciver);
	}

	public void info(Object message) {
		info(String.valueOf(message), null);
	}

	public void info(Object message, Map<String, String> vars) {
		send(getInfoPrefix(), String.valueOf(message), vars);
	}

	public void warn(Object message) {
		warn(String.valueOf(message), null);
	}

	public void warn(Object message, Map<String, String> vars) {
		send(getWarnPrefix(), String.valueOf(message), vars);
	}

	public void info(String message) {
		info(message, null);
	}

	public void info(String message, Map<String, String> vars) {
		send(getInfoPrefix(), message, vars);
	}

	public void warn(String message) {
		warn(message, null);
	}

	public void warn(String message, Map<String, String> vars) {
		send(getWarnPrefix(), message, vars);
	}

	public void send(String prefix, String message, Map<String, String> vars) {
		reciver.sendMessage(color(prefix) + color(vars == null ? message : replaceVariables(message, vars)));
	}

	public String replaceVariables(String message, Map<String, String> vars) {
		for (Entry<String, String> var : vars.entrySet())
			message = message.replace(var.getKey(), var.getValue());
		return message;
	}

	public Map<String, String> createMap(String... strs) {
		Map<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < strs.length; i += 2)
			if (i + 1 < strs.length)
				map.put(strs[i], strs[i + 1]);
		return map;
	}

	public String color(String str) {
		return ChatColor.translateAlternateColorCodes('&', str);
	}

	public String getInfoPrefix() {
		return INFO_PREFIX;
	}

	public void setInfoPrefix(String infoPrefix) {
		INFO_PREFIX = infoPrefix;
	}

	public String getWarnPrefix() {
		return WARN_PREFIX;
	}

	public void setWarnPrefix(String warnPrefix) {
		WARN_PREFIX = warnPrefix;
	}

	public CommandSender getReciver() {
		return reciver;
	}

	public void setReciver(CommandSender reciver) {
		this.reciver = reciver;
	}

}