package me.nemo_64.spigot.spigotutils.events;

import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractEventListenerAdapter implements PlayerInteractEventListener {

	@Override
	public void onRightClickAirNoShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onRightClickAirShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onRightClickAirInteract(PlayerInteractEvent e) {}

	@Override
	public void onLeftClickAirNoShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onLeftClickAirShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onLeftClickAirInteract(PlayerInteractEvent e) {}

	@Override
	public void onAirInteract(PlayerInteractEvent e) {}

	@Override
	public void onRightClickBlockNoShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onRightClickBlockShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onRightClickBlockInteract(PlayerInteractEvent e) {}

	@Override
	public void onLeftClickBlockNoShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onLeftClickBlockShiftInteract(PlayerInteractEvent e) {}

	@Override
	public void onLeftClickBlockInteract(PlayerInteractEvent e) {}

	@Override
	public void onBlockInteract(PlayerInteractEvent e) {}

}