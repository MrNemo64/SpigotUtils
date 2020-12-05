package me.nemo_64.spigot.spigotutils.commands;

import org.bukkit.command.CommandSender;

public interface NArgument extends Argumentable {

	public boolean onArgument(CommandSender sender, String[] args);

	public String getName();
}