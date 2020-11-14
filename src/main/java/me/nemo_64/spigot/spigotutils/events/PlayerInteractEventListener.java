package me.nemo_64.spigot.spigotutils.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public interface PlayerInteractEventListener extends Listener {

	@EventHandler
	public default void onPlayerInteractEvent(PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			onBlockInteract(e);
			if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
				onLeftClickBlockInteract(e);
				if (e.getPlayer().isSneaking()) {
					onLeftClickBlockShiftInteract(e);
				} else {
					onLeftClickBlockNoShiftInteract(e);
				}
			} else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				onRightClickBlockInteract(e);
				if (e.getPlayer().isSneaking()) {
					onRightClickBlockShiftInteract(e);
				} else {
					onRightClickBlockNoShiftInteract(e);
				}
			}
		} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR) {
			onAirInteract(e);
			if (e.getAction() == Action.LEFT_CLICK_AIR) {
				onLeftClickAirInteract(e);
				if (e.getPlayer().isSneaking()) {
					onLeftClickAirShiftInteract(e);
				} else {
					onLeftClickAirNoShiftInteract(e);
				}
			} else if (e.getAction() == Action.RIGHT_CLICK_AIR) {
				onRightClickAirInteract(e);
				if (e.getPlayer().isSneaking()) {
					onRightClickAirShiftInteract(e);
				} else {
					onRightClickAirNoShiftInteract(e);
				}
			}
		}
	}

	/**
	 * Called when a player right clicks air without sneaking
	 * 
	 * @param e The event
	 */
	public void onRightClickAirNoShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when a player right clicks air while sneaking
	 * 
	 * @param e The event
	 */
	public void onRightClickAirShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when a player right clicks air with or without sneaking. After this
	 * method is called,
	 * {@link #onRightClickAirNoShiftInteract(PlayerInteractEvent)} or
	 * {@link #onRightClickAirShiftInteract(PlayerInteractEvent)} will be called
	 * 
	 * @param e The event
	 */
	public void onRightClickAirInteract(PlayerInteractEvent e);

	/**
	 * Called when a player left clicks air without sneaking
	 * 
	 * @param e The event
	 */
	public void onLeftClickAirNoShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when a player left clicks air while sneaking
	 * 
	 * @param e The event
	 */
	public void onLeftClickAirShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when a player left clicks air with or without sneaking. After this
	 * method is called, {@link #onLeftClickAirNoShiftInteract(PlayerInteractEvent)}
	 * or {@link #onLeftClickAirShiftInteract(PlayerInteractEvent)} will be called
	 * 
	 * @param e The event
	 */
	public void onLeftClickAirInteract(PlayerInteractEvent e);

	/**
	 * Called when a player interacts with air. After this method is called,
	 * {@link #onRightClickAirInteract(PlayerInteractEvent)} or
	 * {@link #onLeftClickAirInteract(PlayerInteractEvent)} will be called
	 * 
	 * @param e The event
	 */
	public void onAirInteract(PlayerInteractEvent e);

	/**
	 * Called when a player right clicks a block without sneaking
	 * 
	 * @param e The event
	 */
	public void onRightClickBlockNoShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when a player right clicks a block while sneaking
	 * 
	 * @param e The event
	 */
	public void onRightClickBlockShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when a player right clicks a block with or without sneaking. After
	 * this method, {@link #onRightClickBlockShiftInteract(PlayerInteractEvent)} or
	 * {@link #onRightClickBlockNoShiftInteract(PlayerInteractEvent)} will be called
	 * 
	 * @param e The event
	 */
	public void onRightClickBlockInteract(PlayerInteractEvent e);

	/**
	 * Called when a player left clicks a block without sneaking
	 * 
	 * @param e The event
	 */
	public void onLeftClickBlockNoShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when a player left clicks a block while sneaking
	 * 
	 * @param e The event
	 */
	public void onLeftClickBlockShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when a player left clicks a block with or without sneaking. After this
	 * method, {@link #onLeftClickBlockShiftInteract(PlayerInteractEvent)} or
	 * {@link #onLeftClickBlockNoShiftInteract(PlayerInteractEvent)} will be called
	 * 
	 * @param e The event
	 */
	public void onLeftClickBlockInteract(PlayerInteractEvent e);

	/**
	 * Called when a player interacts with a block. After this method is called,
	 * {@link #onRightClickBlockInteract(PlayerInteractEvent)} or
	 * {@link #onLeftClickBlockInteract(PlayerInteractEvent)} will be called
	 * 
	 * @param e The event
	 */
	public void onBlockInteract(PlayerInteractEvent e);

}
