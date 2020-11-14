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
		} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR) {
			onInteract(e);
		}
	}

	/**
	 * Called when the player interacts with a block
	 * 
	 * @param e The event
	 */
	public default void onBlockInteract(PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
			onLeftClickBlockInteract(e);
		} else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			onRightClickBlockInteract(e);
		}
	}

	/**
	 * Called when the player makes a left click on a block
	 * 
	 * @param e The event
	 */
	public default void onLeftClickBlockInteract(PlayerInteractEvent e) {
		if (e.getPlayer().isSneaking())
			onLeftClickBlockShiftInteract(e);
	}

	/**
	 * Called when the player makes a right click on a block
	 * 
	 * @param e The event
	 */
	public default void onRightClickBlockInteract(PlayerInteractEvent e) {
		if (e.getPlayer().isSneaking())
			onRightClickBlockShiftInteract(e);
	}

	/**
	 * Called when the player makes a left click on a block while sneaking
	 * 
	 * @param e The event
	 */
	public void onLeftClickBlockShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when the player makes a right click on a block while sneaking
	 * 
	 * @param e The event
	 */
	public void onRightClickBlockShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when the player doesn't interacts with a block
	 * 
	 * @param e The event
	 */
	public default void onInteract(PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_AIR) {
			onLeftClickInteract(e);
		} else if (e.getAction() == Action.RIGHT_CLICK_AIR) {
			onRightClickInteract(e);
		}
	}

	/**
	 * Called when the player doesn't interacts with a block and does a left click
	 * 
	 * @param e The event
	 */
	public default void onLeftClickInteract(PlayerInteractEvent e) {
		if (e.getPlayer().isSneaking())
			onLeftClickShiftInteract(e);
	}

	/**
	 * Called when the player doesn't interacts with a block and does a right click
	 * 
	 * @param e The event
	 */
	public default void onRightClickInteract(PlayerInteractEvent e) {
		if (e.getPlayer().isSneaking())
			onRightClickShiftInteract(e);
	}

	/**
	 * Called when the player doesn't interacts with a block and does a left click
	 * while sneaking
	 * 
	 * @param e The event
	 */
	public void onLeftClickShiftInteract(PlayerInteractEvent e);

	/**
	 * Called when the player doesn't interacts with a block and does a right click
	 * while sneaking
	 * 
	 * @param e The event
	 */
	public void onRightClickShiftInteract(PlayerInteractEvent e);

}
