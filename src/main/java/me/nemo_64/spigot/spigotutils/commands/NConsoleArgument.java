package me.nemo_64.spigot.spigotutils.commands;

import org.bukkit.command.CommandSender;

public interface NConsoleArgument extends NArgument {

	@Override
	default boolean onArgument(CommandSender sender, String[] args) {
		String perm = getArgumentPermission();
		if (perm != null && !hasPermission(sender, perm)) {
			String notEnoughPermissions = getNotEnoughPermissionsMessage();
			if (notEnoughPermissions != null)
				sender.sendMessage(notEnoughPermissions);
			return false;
		}
		return runArgument(sender, args);
	}

	/**
	 * Runs this argument. This method will be called after checking if the sender
	 * has enough permissions.<br>
	 * If true is returned, then
	 * {@link NCommand#sendUssage(CommandSender, String[])} is called
	 * 
	 * @param sender The sender
	 * @param args   The arguments
	 * @return true if the ussage needs to be sent
	 */
	public boolean runArgument(CommandSender sender, String[] args);

	/**
	 * Checks if a given sender has a permission or is op
	 * 
	 * @param sender The sender
	 * @param perm   The permision
	 * @return true if the sender is op or has the given permission
	 */
	default boolean hasPermission(CommandSender sender, String perm) {
		return sender.isOp() || sender.hasPermission(perm);
	}

	/**
	 * Gets the message to be sent if the sender doesn't have enough permissions to
	 * run the command. If null is returned no mensage will be sent
	 * 
	 * @return The message
	 */
	public String getNotEnoughPermissionsMessage();

	/**
	 * Gets the permission needed to run this command
	 * 
	 * @return The permission, null if no permission is needed
	 */
	public String getArgumentPermission();

}
