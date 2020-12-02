package me.nemo_64.spigot.spigotutils.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface NPlayerCommand extends NConsoleCommand {

	@Override
	default boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			String message = getOnlyForPlayersMessage();
			if (message != null)
				sender.sendMessage(message);
			return true;
		}
		String perm = getCommandPermission();
		if (perm != null && !hasPermission(sender, perm)) {
			String notEnoughPermissions = getNotEnoughPermissionsMessage();
			if (notEnoughPermissions != null)
				sender.sendMessage(notEnoughPermissions);
			return true;
		}
		if (runCommand(sender, args))
			sendUssage(sender, args);
		return true;
	}

	@Override
	default boolean runCommand(CommandSender sender, String[] args) {
		return runCommand((Player) sender, args);
	}

	/**
	 * Runs this command. This method will be called after checking if the sender is
	 * a player and has enough permissions.<br>
	 * If true is returned, then
	 * {@link NCommand#sendUssage(CommandSender, String[])} is called
	 * 
	 * @param player The player
	 * @param args   The arguments
	 * @return true if the ussage needs to be sent
	 */
	public boolean runCommand(Player player, String[] args);

	/**
	 * Gets the message to be sent if the sender isn't a player. If null is returned
	 * no mensage will be sent
	 * 
	 * @return The message
	 */
	public String getOnlyForPlayersMessage();

}
