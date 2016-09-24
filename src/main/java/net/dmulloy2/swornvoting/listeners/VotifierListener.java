/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.listeners;

import lombok.AllArgsConstructor;
import net.dmulloy2.swornvoting.SwornVoting;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

/**
 * @author dmulloy2
 */

@AllArgsConstructor
public class VotifierListener implements Listener {
	private final SwornVoting plugin;

	@EventHandler(priority = EventPriority.MONITOR)
	public void onVotifierEvent(VotifierEvent event) {
		Vote vote = event.getVote();
		plugin.getVoteHandler().handleVote(vote.getUsername(), vote.getServiceName());
	}
}