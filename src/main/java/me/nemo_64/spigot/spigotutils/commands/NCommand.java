package me.nemo_64.spigot.spigotutils.commands;

import org.bukkit.command.CommandSender;

public interface NCommand<S extends CommandSender> extends Argumentable {

	/**
	 * Runs this command
	 * 
	 * @param sender The sender
	 * @param args   The arguments
	 * @return true if the ussage needs to be sent
	 */
	public boolean runCommand(S sender, String[] args);

	/**
	 * Sends the ussage of this command to a sender
	 * 
	 * @param sender The sender
	 * @param The    arguments provided by the sender
	 */
	public void sendUssage(S sender, String[] args);

}
