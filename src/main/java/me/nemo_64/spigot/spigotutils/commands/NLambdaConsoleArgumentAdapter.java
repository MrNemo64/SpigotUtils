package me.nemo_64.spigot.spigotutils.commands;

import java.util.List;
import java.util.function.BiFunction;

import org.bukkit.command.CommandSender;

public class NLambdaConsoleArgumentAdapter extends NConsoleArgumentAdapter {

	private BiFunction<CommandSender, String[], Boolean> function;

	public NLambdaConsoleArgumentAdapter(BiFunction<CommandSender, String[], Boolean> argument, String name,
			String argumentPermission, String notEnoughPermissionsMessage, List<NArgument> args) {
		super(name, argumentPermission, notEnoughPermissionsMessage);
		this.function = argument;
		if (args != null)
			for (NArgument arg : args)
				addArgument(arg);
	}

	public NLambdaConsoleArgumentAdapter(BiFunction<CommandSender, String[], Boolean> argument, String name,
			String argumentPermission, String notEnoughPermissionsMessage) {
		this(argument, name, argumentPermission, notEnoughPermissionsMessage, null);
	}

	@Override
	public boolean runArgument(CommandSender sender, String[] args) {
		return function.apply(sender, args);
	}

}