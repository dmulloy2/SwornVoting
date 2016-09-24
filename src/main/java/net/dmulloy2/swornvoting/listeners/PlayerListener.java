/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.listeners;

import java.util.List;

import lombok.AllArgsConstructor;
import net.dmulloy2.swornvoting.SwornVoting;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author dmulloy2
 */

@AllArgsConstructor
public class PlayerListener implements Listener {
	private final SwornVoting plugin;

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final List<String> votes = plugin.getVoteCache().getData(player);

		if (votes != null && ! votes.isEmpty()) {
			class CachedVotesTask extends BukkitRunnable {

				@Override
				public void run() {
					// Process the votes
					for (String site : votes) {
						plugin.getVoteHandler().handleVote(player, site);
					}

					// Remove their data
					plugin.getVoteCache().removeData(player);
				}

			}

			new CachedVotesTask().runTaskLater(plugin, 120L);
		}
	}
}