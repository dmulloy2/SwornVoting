/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.commands;

import net.dmulloy2.swornvoting.SwornVoting;
import net.dmulloy2.swornvoting.gui.ShopGUI;

/**
 * @author dmulloy2
 */

public class CmdShop extends SwornVotingCommand {

	public CmdShop(SwornVoting plugin) {
		super(plugin);
		this.name = "shop";
		this.description = "Display voting shop";
		this.mustBePlayer = true;
		this.usesPrefix = true;
	}

	@Override
	public void perform() {
		ShopGUI gui = new ShopGUI(plugin, player);
		plugin.getGuiHandler().open(player, gui);
	}
}