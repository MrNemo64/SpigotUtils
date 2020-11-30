package me.nemo_64.spigot.spigotutils.events;

import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractEventListenerAdapter implements PlayerInteractEventListener, Listener {

	@Override
	public void onRightClickAirNoShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onRightClickAirShiftInteract(PlayerInteractEvent e) {}

	@Override
	public boolean onRightClickAirInteract(PlayerInteractEvent e) {
		return true;
	}

	@Override
	public void onLeftClickAirNoShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onLeftClickAirShiftInteract(PlayerInteractEvent e) {}

	@Override
	public boolean onLeftClickAirInteract(PlayerInteractEvent e) {
		return true;
	}

	@Override
	public boolean onAirInteract(PlayerInteractEvent e) {
		return true;
	}

	@Override
	public void onRightClickBlockNoShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onRightClickBlockShiftInteract(PlayerInteractEvent e) {}

	@Override
	public boolean onRightClickBlockInteract(PlayerInteractEvent e) {
		return true;
	}

	@Override
	public void onLeftClickBlockNoShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onLeftClickBlockShiftInteract(PlayerInteractEvent e) {}

	@Override
	public boolean onLeftClickBlockInteract(PlayerInteractEvent e) {
		return true;
	}

	@Override
	public boolean onBlockInteract(PlayerInteractEvent e) {
		return true;
	}

}