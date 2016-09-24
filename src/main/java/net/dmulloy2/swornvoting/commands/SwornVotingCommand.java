/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.commands;

import net.dmulloy2.commands.Command;
import net.dmulloy2.swornvoting.SwornVoting;

/**
 * @author dmulloy2
 */

public abstract class SwornVotingCommand extends Command {
	protected final SwornVoting plugin;

	public SwornVotingCommand(SwornVoting plugin) {
		super(plugin);
		this.plugin = plugin;
	}
}