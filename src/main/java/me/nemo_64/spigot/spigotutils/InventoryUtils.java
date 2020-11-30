package me.nemo_64.spigot.spigotutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryUtils {

	private InventoryUtils() {}

	public static List<ItemStack> stack(List<ItemStack> items) {
		Objects.requireNonNull(items, "The list of items can't be null");
		List<ItemStack> stacked = new ArrayList<ItemStack>();
		for (ItemStack item : items) {
			if (isAir(item))
				continue;
			boolean found = false;
			for (ItemStack stack : stacked) {
				if (item.isSimilar(stack)) {
					stack.setAmount(stack.getAmount() + item.getAmount());
					found = true;
					break;
				}
			}
			if (!found)
				stacked.add(item);
		}
		return stacked;
	}

	public static List<ItemStack> removeItems(Inventory inv, boolean useShulkers, ItemStack... items) {
		return removeItems(inv, Arrays.asList(items), useShulkers);
	}

	public static List<ItemStack> removeItems(Inventory inv, List<ItemStack> items, boolean useShulkers) {
		List<ItemStack> unRemoved = new ArrayList<ItemStack>();
		for (ItemStack item : items) {
			ItemStack i = removeItem(inv, item, useShulkers);
			if (!isAir(i))
				unRemoved.add(i);
		}
		return unRemoved;
	}

	public static ItemStack removeItem(Inventory inv, ItemStack item, boolean useShulkers) {
		Objects.requireNonNull(inv, "The inventory can't be null");
		Objects.requireNonNull(item, "The item can't be null");
		if (item.getType() == Material.AIR)
			throw new IllegalArgumentException("The item can't be air");
		int remaining = item.getAmount();
		ItemStack clone = item.clone();
		ItemStack[] contents = inv.getStorageContents();
		for (int i = 0; i < contents.length; i++) {
			if (remaining <= 0)
				break;
			ItemStack stack = contents[i];
			if (isAir(stack))
				continue;
			if (stack.isSimilar(item)) {
				int remove = stack.getAmount() < remaining ? stack.getAmount() : remaining;
				contents[i].setAmount(contents[i].getAmount() - remove);
				if (isAir(contents[i]))
					contents[i] = null;
				remaining -= remove;
			}
		}
		clone.setAmount(remaining);
		if (remaining > 0 && useShulkers) {
			for (ItemStack stack : contents) {
				if (isShulkerBox(stack)) {
					BlockStateMeta meta = (BlockStateMeta) stack.getItemMeta();
					ShulkerBox state = (ShulkerBox) meta.getBlockState();
					clone = removeItem(state.getInventory(), clone, useShulkers);
					meta.setBlockState(state);
					stack.setItemMeta(meta);
				}
				if (clone == null || clone.getAmount() == 0)
					break;
			}
		}
		inv.setStorageContents(contents);
		return isAir(clone) ? null : clone;
	}

	public static List<Item> addItems(Inventory inv, Location drop, boolean useShulkers, boolean dropNaturally,
			ItemStack... items) {
		return addItems(inv, Arrays.asList(items), drop, useShulkers, dropNaturally);
	}

	public static List<Item> addItems(Inventory inv, List<ItemStack> items, Location drop, boolean useShulkers,
			boolean dropNaturally) {
		Objects.requireNonNull(inv, "The provided inventory can't be null");
		Objects.requireNonNull(items, "The provided items can't be null");
		Objects.requireNonNull(drop, "The provided location can't be null");
		List<ItemStack> toDrop = addItems(inv, items, useShulkers);
		List<Item> droped = new ArrayList<Item>();
		World world = drop.getWorld();
		for (ItemStack item : toDrop) {
			if (isAir(item))
				continue;
			if (dropNaturally) {
				droped.add(world.dropItemNaturally(drop, item));
			} else {
				droped.add(world.dropItem(drop, item));
			}
		}
		return droped;
	}

	public static List<ItemStack> addItems(Inventory inv, boolean useShulkers, ItemStack... items) {
		return addItems(inv, Arrays.asList(items), useShulkers);
	}

	public static List<ItemStack> addItems(Inventory inv, List<ItemStack> items, boolean useShulkers) {
		List<ItemStack> unAdded = new ArrayList<ItemStack>();
		for (ItemStack i : items) {
			ItemStack item = addItem(inv, i, useShulkers);
			if (!isAir(item))
				unAdded.add(item);
		}
		return unAdded;
	}

	public static ItemStack addItem(Inventory inv, ItemStack item, boolean useShulkers) {
		Objects.requireNonNull(inv, "The inventory can't be null");
		Objects.requireNonNull(item, "The item can't be null");
		if (item.getType() == Material.AIR)
			throw new IllegalArgumentException("The item can't be air");
		ItemStack clone = item.clone();
		int rem = item.getAmount();
		int add;
		ItemStack[] contents = inv.getStorageContents();
		for (int i = 0; i < contents.length; i++) {
			if (rem <= 0)
				break;
			ItemStack stack = contents[i];
			if (isAir(stack)) {
				add = rem < item.getMaxStackSize() ? rem : item.getMaxStackSize();
				clone.setAmount(add);
				contents[i] = clone.clone();
				rem -= add;
			} else if (item.isSimilar(stack)) {
				if (stack.getAmount() < item.getMaxStackSize()) {
					int toFillStack = item.getMaxStackSize() - stack.getAmount();
					add = toFillStack < rem ? toFillStack : rem;
					contents[i].setAmount(contents[i].getAmount() + add);
					rem -= add;
				}
			}
		}
		clone.setAmount(rem);
		if (rem > 0 && useShulkers) { // Aun hay que añadir y podemos usar shulkers
			for (ItemStack stack : contents) {
				if (isShulkerBox(stack)) {
					BlockStateMeta meta = (BlockStateMeta) stack.getItemMeta();
					ShulkerBox state = (ShulkerBox) meta.getBlockState();
					clone = addItem(state.getInventory(), clone, useShulkers);
					meta.setBlockState(state);
					stack.setItemMeta(meta);
				}
				if (clone == null || clone.getAmount() == 0)
					break;
			}
		}
		inv.setStorageContents(contents);
		return isAir(item) ? null : clone;
	}

	/**
	 * Counts all the empty space in a given inventory for a given item. The empty
	 * space for a given item are the empty slots and all the stacks of that item
	 * that are not filled
	 * 
	 * @param inv              The inventory
	 * @param item             The item. Can't be air
	 * @param searchInShulkers If true, shulkers in the inventory will also be used
	 *                         for searching
	 * @throws IllegalArgumentException If the provided item is air
	 * @return
	 */
	public static int getEmptySpaceFor(Inventory inv, ItemStack item, boolean searchInShulkers) {
		Objects.requireNonNull(inv, "The inventory can't be null");
		Objects.requireNonNull(item, "The item can't be null");
		if (item.getType() == Material.AIR)
			throw new IllegalArgumentException("The item can't be air");
		int space = 0;
		for (ItemStack i : inv.getContents()) {
			if (isAir(i)) {
				space += item.getMaxStackSize();
			} else if (i.isSimilar(item)) {
				if (i.getAmount() < item.getMaxStackSize())
					space += item.getMaxStackSize() - i.getAmount();
			} else if (searchInShulkers && isShulkerBox(i)) {
				space += getEmptySpaceFor(getShulkerBox(i).getInventory(), item, searchInShulkers);
			}
		}
		return space;
	}

	/**
	 * Counts all the empty space in a given inventory. The empty space are all the
	 * empty slots.
	 * 
	 * @param inv              The inventory
	 * @param searchInShulkers If true, shulkers in the inventory will also be used
	 *                         for searching
	 * @return
	 */
	public static int getAmountOfEmptySlots(Inventory inv, boolean searchInShulkers) {
		Objects.requireNonNull(inv, "The inventory can't be null");
		int slots = 0;
		for (ItemStack item : inv.getContents()) {
			if (isAir(item)) {
				slots++;
			} else if (isShulkerBox(item) && searchInShulkers) {
				slots += getAmountOfEmptySlots(getShulkerBox(item).getInventory(), searchInShulkers);
			}
		}
		return slots;
	}

	public static boolean isShulkerBox(ItemStack item) {
		if (isAir(item))
			return false;
		ItemMeta meta = item.getItemMeta();
		if (meta instanceof BlockStateMeta) {
			BlockState state = ((BlockStateMeta) meta).getBlockState();
			return state instanceof ShulkerBox;
		}
		return false;
	}

	public static ShulkerBox getShulkerBox(ItemStack item) {
		return isShulkerBox(item) ? ((ShulkerBox) ((BlockStateMeta) item.getItemMeta()).getBlockState()) : null;
	}

	public static boolean isAir(ItemStack item) {
		return item == null || item.getType() == Material.AIR || item.getAmount() == 0;
	}

}