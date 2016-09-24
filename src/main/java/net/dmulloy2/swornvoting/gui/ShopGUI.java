/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.gui;

import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.gui.AbstractGUI;
import net.dmulloy2.swornvoting.SwornVoting;
import net.dmulloy2.swornvoting.types.PlayerData;
import net.dmulloy2.swornvoting.types.ShopItem;
import net.dmulloy2.util.FormatUtil;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author dmulloy2
 */

public class ShopGUI extends AbstractGUI {
	private final String world;
	private final SwornVoting plugin;

	public ShopGUI(SwornVoting plugin, Player player) {
		super(plugin, player);
		this.plugin = plugin;
		this.world = player.getWorld().getName();
		this.setup();
	}

	@Override
	public int getSize() {
		return roundUp(getItems().size(), 9);
	}

	private int roundUp(double x, double f) {
		return (int) (f * Math.ceil(x / f));
	}

	@Override
	public String getTitle() {
		return plugin.getConfig().getString("shopTitle");
	}

	@Override
	public void stock(Inventory inventory) {
		PlayerData data = plugin.getPlayerDataCache().getData(player);

		for (ShopItem item : getItems()) {
			ItemStack icon = item.getIcon();

			ItemMeta meta = icon.getItemMeta();
			List<String> lore = meta.getLore();
			if (lore == null)
				lore = new ArrayList<>();

			lore.add(FormatUtil.format("&7Cost: {0}{1} &7coins",
					data.getCoins() >= item.getCost() ? "&a" : "&c",
					item.getCost()
			));

			meta.setLore(lore);
			icon.setItemMeta(meta);
			inventory.addItem(icon);
		}
	}

	@Override
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack current = event.getCurrentItem();
		if (current != null) {
			ShopItem reward = plugin.getShopHandler().getItem(world, current);
			if (reward != null) {
				PlayerData data = plugin.getPlayerDataCache().getData(player);
				if (data.getCoins() >= reward.getCost()) {
					List<String> commands = reward.getCommands();

					// Execute all commands
					boolean executed = false;
					for (String command : commands) {
						if (execute(command)) {
							executed = true;
						}
					}

					if (executed) {
						data.setCoins(data.getCoins() - reward.getCost());
						player.sendMessage(plugin.getPrefix() + FormatUtil.format("&eYou have purchased a &b{0} &efor &b{1} &ecoins!",
								reward.getName(), reward.getCost()));
					} else {
						player.sendMessage(plugin.getPrefix() + FormatUtil.format("&cFailed to execute command(s)! Check console!"));
					}
				} else {
					player.sendMessage(plugin.getPrefix() + FormatUtil.format("&cYou lack the necessary funds to purchase this!"));
				}
			}
		}

		event.setCancelled(true);
		player.closeInventory();
	}

	private boolean execute(String command) {
		command = command.replaceAll("%player", player.getName());
		return plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
	}

	private List<ShopItem> getItems() {
		return plugin.getShopHandler().getItems(world);
	}
}
