package me.nemo_64.spigot.spigotutils.commands;

import java.util.List;
import java.util.function.BiFunction;

import org.bukkit.entity.Player;

public class NLambdaPlayerArgumentAdapter extends NPlayerArgumentAdapter {

	private BiFunction<Player, String[], Boolean> function;

	public NLambdaPlayerArgumentAdapter(BiFunction<Player, String[], Boolean> argument, String name,
			String argumentPermission, String notEnoughPermissionsMessage, String onlyForPlayersMessage,
			List<NArgument> args) {
		super(name, argumentPermission, notEnoughPermissionsMessage, onlyForPlayersMessage);
		this.function = argument;
		if (args != null)
			for (NArgument arg : args)
				addArgument(arg);
	}

	public NLambdaPlayerArgumentAdapter(BiFunction<Player, String[], Boolean> argument, String name,
			String argumentPermission, String notEnoughPermissionsMessage, String onlyForPlayersMessage) {
		this(argument, name, argumentPermission, notEnoughPermissionsMessage, onlyForPlayersMessage, null);
	}

	@Override
	public boolean runArgument(Player player, String[] args) {
		return function.apply(player, args);
	}
}
