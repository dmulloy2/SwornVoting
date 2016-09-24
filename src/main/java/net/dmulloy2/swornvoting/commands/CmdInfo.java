/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.commands;

import net.dmulloy2.swornvoting.SwornVoting;
import net.dmulloy2.swornvoting.types.PlayerData;
import net.dmulloy2.util.Util;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

public class CmdInfo extends SwornVotingCommand {

	public CmdInfo(SwornVoting plugin) {
		super(plugin);
		this.name = "info";
		this.aliases.add("i");
		this.addOptionalArg("player");
		this.description = "Display voting info for a player";
		this.usesPrefix = true;
	}

	@Override
	public void perform() {
		OfflinePlayer target = null;
		if (args.length == 0) {
			if (sender instanceof Player) {
				target = player;
			} else {
				err("You must specify a player!");
				return;
			}
		} else {
			target = Util.matchOfflinePlayer(args[0]);
			if (target == null) {
				err("Player \"&c{0}&4\" not found!", args[0]);
				return;
			}
		}

		PlayerData data = plugin.getPlayerDataCache().getData(target);
		if (data == null) {
			err("No data found for &c{0}", target.getName());
			return;
		}

		if (target.equals(sender)) {
			sendpMessage("You have &b{0} &ecoins from &b{1} &evotes.", data.getCoins(), data.getVotes());
		} else {
			sendpMessage("&b{0} &ehas &b{1} &ecoins from &b{2} &evotes.", target.getName(), data.getCoins(), data.getVotes());
		}
	}
}