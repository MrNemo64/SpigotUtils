package me.nemo_64.spigot.spigotutils.commands;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface NPlayerArgument extends NConsoleArgument {

	@Override
	default boolean onArgument(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			String message = getOnlyForPlayersMessage();
			if (message != null)
				sender.sendMessage(message);
			return false;
		}
		String perm = getArgumentPermission();
		if (perm != null && !hasPermission(sender, perm)) {
			String notEnoughPermissions = getNotEnoughPermissionsMessage();
			if (notEnoughPermissions != null)
				sender.sendMessage(notEnoughPermissions);
			return false;
		}
		return runArgument(sender, args);
	}

	@Override
	default boolean runArgument(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return false;
		return runArgument((Player) sender, args);
	}

	@Override
	default List<String> complete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return null;
		return complete((Player) sender, args);
	}

	public default List<String> complete(Player player, String[] args) {
		return Arrays.asList(getName());
	}

	@Override
	default boolean matches(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return false;
		return matches((Player) sender, args);
	}

	default boolean matches(Player player, String[] args) {
		String lastArg = args[args.length - 1].toLowerCase();
		return lastArg.length() == 0 || getName().toLowerCase().contains(lastArg);
	}

	@Override
	default NArgument findNextArgument(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return null;
		return findNextArgument((Player) sender, args);
	}

	default NArgument findNextArgument(Player player, String[] args) {
		if (args.length == 0 || getArguments().size() == 0)
			return null;
		String arg = args[0].toLowerCase();
		for (NArgument nArg : getArguments().values())
			if (nArg.matches(player, arg))
				return nArg;
		return null;
	}

	/**
	 * Runs this argument. This method will be called after checking if the sender
	 * is a player and has enough permissions.<br>
	 * If true is returned, then
	 * {@link NCommand#sendUssage(CommandSender, String[])} is called
	 * 
	 * @param player The player
	 * @param args   The arguments
	 * @return true if the ussage needs to be sent
	 */
	public boolean runArgument(Player player, String[] args);

	/**
	 * Gets the message to be sent if the sender isn't a player. If null is returned
	 * no mensage will be sent
	 * 
	 * @return The message
	 */
	public String getOnlyForPlayersMessage();

}
