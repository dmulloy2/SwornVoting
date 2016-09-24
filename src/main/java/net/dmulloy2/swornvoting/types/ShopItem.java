/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.types;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.bukkit.inventory.ItemStack;

/**
 * @author dmulloy2
 */

@Data
@AllArgsConstructor
public class ShopItem {
	private final String name;
	private final int cost;
	private final List<String> commands;
	private final ItemStack icon;

	public ItemStack getIcon() {
		return icon.clone();
	}
}