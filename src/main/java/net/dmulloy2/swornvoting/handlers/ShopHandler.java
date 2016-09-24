/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.dmulloy2.swornvoting.SwornVoting;
import net.dmulloy2.swornvoting.types.ItemSet;
import net.dmulloy2.swornvoting.types.ShopItem;
import net.dmulloy2.types.Reloadable;
import net.dmulloy2.util.ItemUtil;
import net.dmulloy2.util.ListUtil;
import net.dmulloy2.util.Util;

import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * @author dmulloy2
 */

public class ShopHandler implements Reloadable {
	private final List<ItemSet> sets;
	private final SwornVoting plugin;

	public ShopHandler(SwornVoting plugin) {
		this.plugin = plugin;
		this.sets = new ArrayList<>();
		this.reload();
	}

	public List<ShopItem> getItems(String world) {
		world = world.toLowerCase();

		List<ShopItem> ret = new ArrayList<>();

		for (ItemSet set : sets) {
			if (set.isDefault() || set.getWorlds().contains(world)) {
				ret.addAll(set.getItems());
			}
		}

		return ret;
	}

	public ShopItem getItem(String world, ItemStack icon) {
		for (ShopItem item : getItems(world)) {
			ItemStack other = item.getIcon();
			if (icon.getType() == other.getType()
					&& icon.getAmount() == other.getAmount()
					&& icon.getEnchantments().equals(other.getEnchantments()))
				return item;
		}

		return null;
	}

	@Override
	public void reload() {
		sets.clear();

		FileConfiguration config = plugin.getConfig();
		if (! config.isSet("shopItems"))
			return;

		Map<String, Object> values = config.getConfigurationSection("shopItems").getValues(false);
		for (Entry<String, Object> entry : values.entrySet()) {
			String setName = entry.getKey();

			try {
				MemorySection setSection = (MemorySection) entry.getValue();

				boolean isDefault = setSection.getBoolean("default", false);

				List<String> worlds = new ArrayList<>();
				if (setSection.isSet("worlds")) {
					for (String world : setSection.getStringList("worlds")) {
						worlds.add(world.toLowerCase());
					}
				}

				List<ShopItem> items = new ArrayList<>();

				Map<String, Object> itemMap = setSection.getConfigurationSection("items").getValues(false);
				for (Entry<String, Object> itemEntry : itemMap.entrySet()) {
					String name = itemEntry.getKey();

					try {
						MemorySection section = (MemorySection) itemEntry.getValue();

						int cost = section.getInt("cost");
						if (cost <= 0)
							throw new IllegalArgumentException("Invalid cost: " + cost);

						List<String> commands = null;
						if (section.isSet("commands")) {
							commands = section.getStringList("commands");
						} else if (section.isSet("command")) {
							commands = ListUtil.toList(section.getString("command"));
						} else {
							throw new IllegalArgumentException("Invalid command!");
						}

						ItemStack icon = ItemUtil.readItem(section.getString("item"));

						ShopItem item = new ShopItem(name, cost, commands, icon);
						items.add(item);
					} catch (Throwable ex) {
						plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "loading shop item {0}", name));
					}
				}

				ItemSet set = new ItemSet(setName, isDefault, worlds, items);
				sets.add(set);
			} catch (Throwable ex) {
				plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "loading items for set {0}", setName));
			}
		}
	}
}