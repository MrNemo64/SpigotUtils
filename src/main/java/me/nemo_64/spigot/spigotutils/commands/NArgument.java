package me.nemo_64.spigot.spigotutils.commands;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;

public interface NArgument extends Argumentable {

	public boolean onArgument(CommandSender sender, String[] args);

	/**
	 * Gets the name of the argument
	 * 
	 * @return The name of this argument
	 */
	public String getName();

	/**
	 * Gets the permission needed to run this command
	 * 
	 * @return The permission, null if no permission is needed
	 */
	public String getArgumentPermission();

	/**
	 * With a given arguments gets the argument that should go next
	 * 
	 * @param sender
	 * @param args
	 * @return
	 */
	public default NArgument findNextArgument(CommandSender sender, String[] args) {
		if (args.length == 0 || getArguments().size() == 0)
			return null;
		String arg = args[0].toLowerCase();
		for (NArgument nArg : getArguments().values())
			if (nArg.matches(sender, arg))
				return nArg;
		return null;
	}

	public default List<String> complete(CommandSender sender, String[] args) {
		return Arrays.asList(getName());
	}

	public default boolean matches(CommandSender sender, String[] args) {
		String lastArg = args[args.length - 1].toLowerCase();
		return lastArg.length() == 0 || getName().toLowerCase().contains(lastArg);
	}

	public default boolean matches(CommandSender sender, String arg) {
		return matches(sender, new String[] { arg });
	}
}