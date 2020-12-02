package me.nemo_64.spigot.spigotutils.commands;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.command.CommandSender;

public interface Argumentable {

	/**
	 * Calls the next argumet.<br>
	 * If true is returned, then
	 * {@link NCommand#sendUssage(CommandSender, String[])} is called
	 * 
	 * @param sender The sender
	 * @param args   The arguments
	 * @return true if the ussage needs to be sent
	 */
	public default boolean callArgument(CommandSender sender, String[] args) {
		NArgument next = getNextArgument(args);
		if (next == null)
			return true;
		return next.onArgument(sender, Arrays.copyOfRange(args, 1, args.length));
	}

	/**
	 * Calls all next argumets.<br>
	 * If true is returned, then
	 * {@link NCommand#sendUssage(CommandSender, String[])} is called
	 * 
	 * @param sender The sender
	 * @param args   The arguments
	 * @return true if the ussage needs to be sent
	 */
	public default boolean callArguments(CommandSender sender, String[] args) {
		NArgument next = getNextArgument(args);
		if (next == null)
			return true;
		String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
		next.onArgument(sender, newArgs);
		next.callArguments(sender, newArgs);
		return false;
	}

	/**
	 * Gets the next argument
	 * 
	 * @param args The arguments
	 * @return The next argument, null if no argument matched the given ones
	 */
	public default NArgument getNextArgument(String[] args) {
		if (isNextArgument(args))
			return getArguments().get(args[0].toLowerCase());
		return null;
	}

	/**
	 * Checks if the given arguments correspond to an argument
	 * 
	 * @param args The arguments
	 * @return True if the given arguments correspond to an argument
	 */
	public default boolean isNextArgument(String[] args) {
		Map<String, NArgument> allArgs = getArguments();
		if (allArgs == null || allArgs.isEmpty()) // There are no arguments
			return false;
		return allArgs.get(args[0].toLowerCase()) != null;
	}

	/**
	 * Gets a map with all the callable arguments of this command
	 * 
	 * @param <T> The type of the arguments
	 * @return A map with all the callable arguments of this command, null or an
	 *         empty map if there are no arguments
	 */
	public <T extends NArgument> Map<String, T> getArguments();

	/**
	 * Adds an argument
	 * 
	 * @param argName The name of the argument
	 * @param arg     The argument
	 */
	public void addArgument(String argName, NArgument arg);

	/**
	 * Removes an argument
	 * 
	 * @param argName The name of the afument
	 * @return The removed argument, null if no argument was removed
	 */
	public NArgument removeArgument(String argName);
}