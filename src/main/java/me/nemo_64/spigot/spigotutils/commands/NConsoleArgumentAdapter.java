package me.nemo_64.spigot.spigotutils.commands;

import java.util.HashMap;
import java.util.Map;

public abstract class NConsoleArgumentAdapter implements NConsoleArgument {

	private Map<String, NArgument> arguments;
	private String notEnoughPermissionsMessage;
	private String argumentPermission;

	public NConsoleArgumentAdapter(String argumentPermission, String notEnoughPermissionsMessage) {
		arguments = new HashMap<String, NArgument>();
		this.argumentPermission = argumentPermission;
		this.notEnoughPermissionsMessage = notEnoughPermissionsMessage;
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
