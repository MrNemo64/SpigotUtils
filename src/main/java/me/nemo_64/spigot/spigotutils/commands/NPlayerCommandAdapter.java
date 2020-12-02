package me.nemo_64.spigot.spigotutils.commands;

import java.util.HashMap;
import java.util.Map;

public abstract class NPlayerCommandAdapter implements NPlayerCommand {

	private Map<String, NArgument> arguments;
	private String notEnoughPermissionsMessage;
	private String commandPermission;
	private String onlyForPlayersMessage;

	public NPlayerCommandAdapter(String commandPermission, String notEnoughPermissionsMessage,
			String onlyForPlayersMessage) {
		arguments = new HashMap<String, NArgument>();
		this.commandPermission = commandPermission;
		this.notEnoughPermissionsMessage = notEnoughPermissionsMessage;
		this.onlyForPlayersMessage = onlyForPlayersMessage;
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
	public String getOnlyForPlayersMessage() {
		return onlyForPlayersMessage;
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
