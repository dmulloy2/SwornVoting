/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import net.dmulloy2.swornvoting.SwornVoting;
import net.dmulloy2.swornvoting.types.LuckyVote;
import net.dmulloy2.swornvoting.types.PlayerData;
import net.dmulloy2.types.Reloadable;
import net.dmulloy2.util.FormatUtil;
import net.dmulloy2.util.Util;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

public class VoteHandler implements Reloadable {
	private transient int votes = 0;

	private String cashMessage;
	private String message;
	private String link;
	private int coins;
	private int cash;

	private final List<LuckyVote> luckyVotes;
	private final SwornVoting plugin;

	public VoteHandler(SwornVoting plugin) {
		this.plugin = plugin;
		this.luckyVotes = new ArrayList<>();
		this.reload();
	}

	public void handleVote(Player player, String site) {
		PlayerData data = plugin.getPlayerDataCache().getData(player);
		data.setCoins(data.getCoins() + coins);
		data.setVotes(data.getVotes() + 1);
		giveMoney(player, cash);

		// Message
		plugin.getServer().broadcastMessage(FormatUtil.format(message
				.replace("%player", player.getName())
				.replace("%site", site)
				.replace("%coins", Integer.toString(coins))
				.replace("%cash", Integer.toString(cash))
				.replace("%link", link)
		));

		// Lucky votes
		for (LuckyVote vote : luckyVotes) {
			int chance = vote.getChance();
			if (Math.random() * 100 < chance) {
				data.setCoins(data.getCoins() + vote.getCoins());
				giveMoney(player, vote.getCash());

				plugin.getServer().broadcastMessage(FormatUtil.format(vote.getMessage()
						.replace("%player", player.getName())
						.replace("%coins", Integer.toString(vote.getCoins()))
				));
			}
		}

		// Increment counter
		votes++;
	}

	private void giveMoney(Player player, int money) {
		if (plugin.getVaultHandler().depositPlayer(player, money) == null)
			player.sendMessage(FormatUtil.format(cashMessage
					.replace("%cash", plugin.getVaultHandler().format(money))));
	}

	public void handleVote(String name, String site) {
		OfflinePlayer player = Util.matchOfflinePlayer(name);
		if (player == null) {
			plugin.getLogHandler().log(Level.WARNING, "Received vote on {0} for unknown player {1}", site, name);
			return;
		}

		handleVote(player, site);
	}

	public void handleVote(OfflinePlayer player, String site) {
		if (player instanceof Player) {
			handleVote((Player) player, site);
		} else {
			// Cache the vote
			UUID uniqueId = player.getUniqueId();
			List<String> votes = plugin.getVoteCache().getData(uniqueId);
			if (votes == null)
				votes = new ArrayList<String>();

			votes.add(site);
			plugin.getVoteCache().putData(uniqueId, votes);

			// Announce the vote anyways
			plugin.getServer().broadcastMessage(FormatUtil.format(message
					.replace("%player", player.getName())
					.replace("%site", site)
					.replace("%coins", Integer.toString(coins))
					.replace("%cash", Integer.toString(cash))
					.replace("%link", link)
			));
		}
	}

	/**
	 * Gets the vote counter for this session.
	 * 
	 * @return The vote counter for this session
	 */
	public int getVotes() {
		return votes;
	}

	@Override
	public void reload() {
		this.cashMessage = plugin.getConfig().getString("cashMessage");
		this.message = plugin.getConfig().getString("message");
		this.link = plugin.getConfig().getString("votingLink");
		this.coins = plugin.getConfig().getInt("coins");
		this.cash = plugin.getConfig().getInt("cash");

		// Load lucky votes
		luckyVotes.clear();

		if (plugin.getConfig().isSet("luckyVotes")) {
			Map<String, Object> values = plugin.getConfig().getConfigurationSection("luckyVotes").getValues(false);
			for (Entry<String, Object> entry : values.entrySet()) {
				String name = entry.getKey();
				MemorySection section = (MemorySection) entry.getValue();

				int chance = section.getInt("chance");
				int coins = section.getInt("coins");
				int cash = section.getInt("coins");
				String message = section.getString("message");

				LuckyVote vote = new LuckyVote(name, chance, coins, cash, message);
				luckyVotes.add(vote);
			}
		}
	}
}