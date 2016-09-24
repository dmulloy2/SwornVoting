/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.commands;

import net.dmulloy2.swornvoting.SwornVoting;
import net.dmulloy2.swornvoting.types.Permission;

/**
 * @author dmulloy2
 */

public class CmdReload extends SwornVotingCommand {

	public CmdReload(SwornVoting plugin) {
		super(plugin);
		this.name = "reload";
		this.aliases.add("rl");
		this.description = "Reloads SwornVoting";
		this.permission = Permission.RELOAD;
		this.usesPrefix = true;
	}

	@Override
	public void perform() {
		long start = System.currentTimeMillis();
		sendpMessage("Reloading &bSwornVoting&e...");

		plugin.reload();

		sendpMessage("SwornVoting reloaded! Took &b{0} &ems!", System.currentTimeMillis() - start);
	}
}