/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.commands;

import net.dmulloy2.swornvoting.SwornVoting;
import net.dmulloy2.swornvoting.types.Permission;
import net.dmulloy2.util.Util;

import org.bukkit.OfflinePlayer;

/**
 * @author dmulloy2
 */

public class CmdFake extends SwornVotingCommand {

	public CmdFake(SwornVoting plugin) {
		super(plugin);
		this.name = "fake";
		this.addRequiredArg("player");
		this.addRequiredArg("site");
		this.description = "Process a fake vote";
		this.permission = Permission.FAKE;
		this.usesPrefix = true;
	}

	@Override
	public void perform() {
		OfflinePlayer player = Util.matchOfflinePlayer(args[0]);
		if (player == null) {
			err("Player \"&c{0}&4\" not found!", args[0]);
			return;
		}

		plugin.getVoteHandler().handleVote(player, args[1]);
	}
}