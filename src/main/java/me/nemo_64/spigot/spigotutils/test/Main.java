package me.nemo_64.spigot.spigotutils.test;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import me.nemo_64.spigot.spigotutils.marker.LocationMarker2;
import me.nemo_64.spigot.spigotutils.marker.LocationMarker2.MarkerColor;

public class Main extends JavaPlugin {

	@Override
	public void onEnable() {
		getCommand("test").setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player) sender;
		Location end = new Location(player.getWorld(), Double.valueOf(args[0]), Double.valueOf(args[1]),
				Double.valueOf(args[2]));
		LocationMarker2.markLocations(player, ChatColor.RED, getLocations(player.getLocation(), end, 15))
				.thenAccept((marker) -> {
					for (int i = 0; i < 15; i++) {
						try {
							Thread.sleep(1000);
							marker.changeColorSync(MarkerColor.random());
						} catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
					marker.unmarkSync();
				});
		return false;
	}

	public Location[] getLocations(Location start, Location end, int am) {
		Location[] locs = new Location[am];
		Location startClone = start.clone();
		Location endClone = end.clone();
		Vector vector = endClone.toVector().add(startClone.toVector().multiply(-1));
		double mod = vector.length() / am;
		vector = vector.normalize().multiply(mod);
		for (int i = 0; i < am; i++) {
			startClone = startClone.add(vector);
			locs[i] = startClone.clone();
		}
		return locs;
	}

}