package me.nemo_64.spigot.spigotutils.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public interface NConsoleCommand extends CommandExecutor, TabCompleter, NCommand<CommandSender> {

	@Override
	default List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> complete = new ArrayList<String>();
		List<NArgument> toEvalue = new ArrayList<NArgument>();

		if (args.length == 0 || args.length == 1) {
			toEvalue.addAll(getArguments().values());
		} else {
			NArgument actual = null;
			// Find the first argument
			for (NArgument arg : getArguments().values()) {
				if (arg.matches(sender, args[0])) {
					actual = arg;
					break;
				}
			}
			NArgument last = actual;
			if (actual != null) {
				for (int i = 1; i < args.length - 1; i++) {
					actual = actual.findNextArgument(sender, Arrays.copyOfRange(args, i, args.length));
					if (actual == null) {
						break;
					} else {
						last = actual;
					}
				}
			}
			if (last != null)
				toEvalue.addAll(last.getArguments().values());
		}

		// Evaluate the arguments
		for (NArgument arg : toEvalue) {
			if (arg instanceof NPlayerArgument && !(sender instanceof Player))
				continue; // The console runs the command but this is a player argument
			if (!hasPermission(sender, arg.getArgumentPermission()))
				continue; // Not enough permissions
			if (arg.matches(sender, args))
				complete.addAll(arg.complete(sender, args));
		}

		return complete;
	}

	@Override
	default boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
	 * Runs this command. This method will be called after checking if the sender
	 * has enough permissions.<br>
	 * If true is returned, then
	 * {@link NCommand#sendUssage(CommandSender, String[])} is called
	 * 
	 * @param sender The sender
	 * @param args   The arguments
	 * @return true if the ussage needs to be sent
	 */
	public boolean runCommand(CommandSender sender, String[] args);

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
	public String getCommandPermission();

}