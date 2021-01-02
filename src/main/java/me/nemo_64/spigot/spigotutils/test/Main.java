package me.nemo_64.spigot.spigotutils.test;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_14_R1.EntityLiving;
import net.minecraft.server.v1_14_R1.EntityShulker;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_14_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_14_R1.PlayerConnection;
import net.minecraft.server.v1_14_R1.World;

public class Main extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getCommand("test").setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player) sender;
		PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
		EntityLiving entity = new EntityShulker(EntityTypes.SHULKER, (World) ((CraftWorld) player.getWorld()).getHandle());

		entity.setLocation(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 0f, 0f);
		entity.setMot(0, 1, 0);

		connection.sendPacket(new PacketPlayOutSpawnEntityLiving(entity));
		connection.sendPacket(new PacketPlayOutEntityMetadata(entity.getId(), entity.getDataWatcher(), true));

		CompletableFuture.runAsync(() -> {

			getLogger().info("Shulker id: " + entity.getId());

			/*
			 * try { Thread.sleep(3000); } catch(InterruptedException e) {
			 * e.printStackTrace(); }
			 * 
			 * PacketPlayOutSpawnEntityLiving spawn = new
			 * PacketPlayOutSpawnEntityLiving(entity); PacketPlayOutEntityMetadata metadata
			 * = new PacketPlayOutEntityMetadata(entity.getId(), entity.getDataWatcher(),
			 * true);
			 * 
			 * connection.sendPacket(spawn); connection.sendPacket(metadata);
			 * 
			 * try { Thread.sleep(3000); } catch(InterruptedException e) {
			 * e.printStackTrace(); } entity.setLocation(player.getLocation().getX(),
			 * player.getLocation().getY(), player.getLocation().getZ(), 180f, 180f);
			 * player.sendMessage("location set");
			 */
			try {
				Thread.sleep(3000);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
			PacketPlayOutEntityTeleport tp = new PacketPlayOutEntityTeleport();
			for (Field f : tp.getClass().getDeclaredFields()) {
				f.setAccessible(true);
				try {
					switch (f.getName()) {
					case "a":
						f.set(tp, entity.getId());
						break;
					case "b":
						f.set(tp, player.getLocation().getX());
						break;
					case "c":
						f.set(tp, player.getLocation().getY());
						break;
					case "d":
						f.set(tp, player.getLocation().getZ());
						break;
					case "e":
					case "f":
						f.set(tp, (byte) 0);
						break;
					case "g":
						f.set(tp, true);
						break;
					default:
						break;
					}
				} catch(IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}

			connection.sendPacket(tp);
			connection.sendPacket(new PacketPlayOutEntityVelocity(entity));
			player.sendMessage("packet sent");
		});
		/*
		 * LocationMarker2.markLocations(new Player[] { player }, ChatColor.RED,
		 * player.getLocation()).thenAccept((marker) -> { for (Integer id :
		 * marker.getShulkersId()) getLogger().info("Shulker id: " + id); long end =
		 * System.currentTimeMillis() + 5000; while (end > System.currentTimeMillis()) {
		 * try { Thread.sleep(250); marker.moveMarkersSync(player.getLocation().add(0,
		 * 2, 0)); marker.changeColorSync(MarkerColor.random().getChatColor()); }
		 * catch(InterruptedException e) { e.printStackTrace(); } } marker.unmarkSync();
		 * });
		 */
		/*
		 * LocationMarker2.markLocations(new Player[] { player }, ChatColor.RED,
		 * getLocations(player.getLocation(), getEndLocation(player, 30),
		 * 3)).thenAccept((marker) -> { long finish = System.currentTimeMillis() + 5000;
		 * for (Integer id : marker.getShulkersId()) getLogger().info("Shulker id: " +
		 * id); while (finish > System.currentTimeMillis()) { try { Thread.sleep(1000);
		 * Location[] locs = getLocations(player.getLocation().add(0, 2, 0),
		 * getEndLocation(player, 30), 3); marker.moveMarkersSync(locs);
		 * marker.changeColorSync(MarkerColor.random().getChatColor()); }
		 * catch(InterruptedException e) { e.printStackTrace(); } } marker.unmarkSync();
		 * });
		 */

		return false;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		ChannelDuplexHandler handler = new ChannelDuplexHandler() {

			@Override
			public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
				// getLogger().info("Readed " + msg.getClass().getName());
				super.channelRead(ctx, msg);
			}

			@Override
			public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
				// getLogger().info("Writted " + msg.getClass().getName());

				if (msg instanceof PacketPlayOutEntityTeleport) {
					getLogger().info("Writted " + msg.getClass().getName());
					PacketPlayOutEntityTeleport p = (PacketPlayOutEntityTeleport) msg;
					for (Field f : p.getClass().getDeclaredFields()) {
						f.setAccessible(true);
						getLogger().info("    " + f.getName() + "=" + f.get(p));
					}
				} else if (msg instanceof PacketPlayOutEntityVelocity) {
					getLogger().info("Writted " + msg.getClass().getName());
					PacketPlayOutEntityVelocity p = (PacketPlayOutEntityVelocity) msg;
					for (Field f : p.getClass().getDeclaredFields()) {
						f.setAccessible(true);
						getLogger().info("    " + f.getName() + "=" + f.get(p));
					}
				}

				super.write(ctx, msg, promise);
			}

		};

		ChannelPipeline pipeline = ((CraftPlayer) e.getPlayer()).getHandle().playerConnection.networkManager.channel
				.pipeline();

		pipeline.addAfter("packet_handler", e.getPlayer().getName(), handler);
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