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
import me.nemo_64.spigot.spigotutils.marker.LocationMarker2.ShulkerMarker;

public class Main extends JavaPlugin {

	@Override
	public void onEnable() {
		getCommand("test").setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player) sender;
		LocationMarker2.markLocations(new Player[] { player }, ChatColor.RED, player.getLocation()).thenAccept((marker) -> {
			ShulkerMarker sm = marker.getShulkers()[0];
			long end = System.currentTimeMillis() + 5000;
			while (end > System.currentTimeMillis()) {
				try {
					Thread.sleep(250);
					LocationMarker2.moveMarkerSync(new Player[] { player }, sm.getId(), player.getLocation().getX(),
							player.getLocation().getY() + 2, player.getLocation().getZ());
					LocationMarker2.changeColorSync(new Player[] { player }, MarkerColor.random(), sm.getUniqueId());
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			marker.unmarkSync();
		});
		/*
		 * LocationMarker2.markLocations(new Player[] { player }, ChatColor.RED,
		 * getLocations(player.getLocation(), getEndLocation(player, 30),
		 * 15)).thenAccept((marker) -> { long finish = System.currentTimeMillis() +
		 * 5000; while (finish > System.currentTimeMillis()) { try { Thread.sleep(250);
		 * Location[] locs = getLocations(player.getLocation().add(0, 2, 0),
		 * getEndLocation(player, 30), 15); System.out.println(locs[locs.length -
		 * 1].toString()); marker.moveMarkersSync(locs);
		 * marker.changeColorSync(MarkerColor.random()); } catch(InterruptedException e)
		 * { e.printStackTrace(); } } marker.unmarkSync(); });
		 */
		return false;
	}

	public Location getEndLocation(Player player, int size) {
		Vector start = player.getEyeLocation().toVector();
		Vector lenght = player.getEyeLocation().getDirection().multiply(size);
		return start.add(lenght).toLocation(player.getWorld());
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