package me.nemo_64.spigot.spigotutils.commands;

import java.util.HashMap;
import java.util.Map;

public abstract class NPlayerArgumentAdapter implements NPlayerArgument {

	private Map<String, NArgument> arguments;
	private String notEnoughPermissionsMessage;
	private String argumentPermission;
	private String onlyForPlayersMessage;
	private String name;

	public NPlayerArgumentAdapter(String name, String argumentPermission, String notEnoughPermissionsMessage,
			String onlyForPlayersMessage) {
		arguments = new HashMap<String, NArgument>();
		this.name = name;
		this.argumentPermission = argumentPermission;
		this.notEnoughPermissionsMessage = notEnoughPermissionsMessage;
		this.onlyForPlayersMessage = onlyForPlayersMessage;
	}

	@Override
	public String getNotEnoughPermissionsMessage() {
		return notEnoughPermissionsMessage;
	}

	@Override
	public String getArgumentPermission() {
		return argumentPermission;
	}

	@Override
	public String getOnlyForPlayersMessage() {
		return onlyForPlayersMessage;
	}

	@Override
	public String getName() {
		return name;
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
