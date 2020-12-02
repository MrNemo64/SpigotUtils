package me.nemo_64.spigot.spigotutils.commands;

import java.util.HashMap;
import java.util.Map;

public abstract class NConsoleCommandAdapter implements NConsoleCommand {

	private Map<String, NArgument> arguments;
	private String notEnoughPermissionsMessage;
	private String commandPermission;

	public NConsoleCommandAdapter(String commandPermission, String notEnoughPermissionsMessage) {
		arguments = new HashMap<String, NArgument>();
		this.commandPermission = commandPermission;
		this.notEnoughPermissionsMessage = notEnoughPermissionsMessage;
	}

	@Override
	public String getNotEnoughPermissionsMessage() {
		return notEnoughPermissionsMessage;
	}

	@Override
	public String getCommandPermission() {
		return commandPermission;
	}

	@Override
	public Map<String, NArgument> getArguments() {
		return arguments;
	}

	@Override
	public void addArgument(String argName, NArgument arg) {
		getArguments().put(argName.toLowerCase(), arg);
	}

	@Override
	public NArgument removeArgument(String argName) {
		return getArguments().remove(argName.toLowerCase());
	}

}
