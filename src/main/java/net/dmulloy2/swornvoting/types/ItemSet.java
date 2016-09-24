/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.types;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author dmulloy2
 */

@Data
@AllArgsConstructor
public class ItemSet {
	private final String name;
	private final boolean isDefault;
	private final List<String> worlds;
	private final List<ShopItem> items;
}