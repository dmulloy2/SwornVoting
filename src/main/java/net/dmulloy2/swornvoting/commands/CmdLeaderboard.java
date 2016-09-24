package net.dmulloy2.swornvoting.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import net.dmulloy2.swornvoting.SwornVoting;
import net.dmulloy2.swornvoting.types.Permission;
import net.dmulloy2.swornvoting.types.PlayerData;
import net.dmulloy2.util.FormatUtil;
import net.dmulloy2.util.Util;

import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author dmulloy2
 */

public class CmdLeaderboard extends SwornVotingCommand {
	private static final long DELAY = 20 * 60 * 15; // 15 minutes

	protected boolean updating;
	protected long lastUpdateTime;
	protected List<String> leaderboard;

	public CmdLeaderboard(SwornVoting plugin) {
		super(plugin);
		this.name = "leaderboard";
		this.aliases.add("lb");
		this.addOptionalArg("force");
		this.description = "Display experience leaderboard";
		this.usesPrefix = true;
	}

	@Override
	public void perform() {
		if (updating) {
			err("Leaderboard is already updating!");
			return;
		}

		if (leaderboard == null) {
			leaderboard = new ArrayList<>();
		}

		boolean force = argAsBoolean(0, false) && hasPermission(Permission.LEADERBOARD_FORCE);
		if (force || System.currentTimeMillis() - lastUpdateTime >= DELAY) {
			sendpMessage("Updating leaderboard... Please wait!");

			leaderboard.clear();
			updating = true;

			new BuildLeaderboardThread();
		}

		new DisplayLeaderboardThread(sender.getName(), args);
	}

	public void displayLeaderboard(String senderName, String[] args) {
		CommandSender sender = getSender(senderName);
		if (sender == null)
			return;

		int index = 1;
		if (args.length > 0) {
			int indexFromArg = argAsInt(0, false);
			if (indexFromArg > 1)
				index = indexFromArg;
		}

		int pageCount = getPageCount();
		if (index > pageCount) {
			sendMessage(sender, "&cError: &4No page with the index &c{0} &4exists!", index);
			return;
		}

		for (String s : getPage(index))
			sendMessage(sender, s);
	}

	private int linesPerPage = 10;

	public int getPageCount() {
		return (getListSize() + linesPerPage - 1) / linesPerPage;
	}

	public int getListSize() {
		return leaderboard.size();
	}

	public List<String> getPage(int index) {
		List<String> lines = new ArrayList<String>();

		StringBuilder line = new StringBuilder();
		line.append(getHeader(index));
		lines.add(line.toString());

		lines.addAll(getLines((index - 1) * linesPerPage, index * linesPerPage));

		if (index != getPageCount()) {
			line = new StringBuilder();
			line.append(FormatUtil.format("Type &b/vote lb &3{0} &efor the next page!", index + 1));
			lines.add(line.toString());
		}

		return lines;
	}

	public String getHeader(int index) {
		return FormatUtil.format("&3====[ &eTop Voters &3(&e{0}&3/&e{1}&3) ]====", index, getPageCount());
	}

	public List<String> getLines(int startIndex, int endIndex) {
		List<String> lines = new ArrayList<String>();
		for (int i = startIndex; i < endIndex && i < getListSize(); i++) {
			lines.add(leaderboard.get(i));
		}

		return lines;
	}

	public class BuildLeaderboardThread extends Thread {
		public BuildLeaderboardThread() {
			super("SwornVoting-BuildLeaderboard");
			this.setPriority(MIN_PRIORITY);
			this.start();
		}

		@Override
		public void run() {
			plugin.getLogHandler().log("Updating leaderboard...");

			long start = System.currentTimeMillis();

			Map<UUID, PlayerData> allData = plugin.getPlayerDataCache().getAllData();
			Map<PlayerData, Integer> voteMap = new HashMap<>();

			for (Entry<UUID, PlayerData> entry : allData.entrySet()) {
				PlayerData value = entry.getValue();
				if (value.getVotes() > 0)
					voteMap.put(value, value.getVotes());
			}

			if (voteMap.isEmpty()) {
				err("No players with votes found!");
				updating = false;
				return;
			}

			List<Entry<PlayerData, Integer>> sortedEntries = new ArrayList<>(voteMap.entrySet());
			Collections.sort(sortedEntries, new Comparator<Entry<PlayerData, Integer>>() {

				@Override
				public int compare(Entry<PlayerData, Integer> entry1, Entry<PlayerData, Integer> entry2) {
					return -entry1.getValue().compareTo(entry2.getValue());
				}

			});

			// Clear the map
			voteMap.clear();
			voteMap = null;

			String format = "&b{0}&e) &b{1}  &b{2} &evotes";

			for (int i = 0; i < sortedEntries.size(); i++) {
				try {
					PlayerData data = sortedEntries.get(i).getKey();
					String name = data.getLastKnownBy();

					leaderboard.add(FormatUtil.format(format, i + 1, name, data.getVotes()));
				} catch (Throwable ex) {
				}
			}

			// Clear the entries
			sortedEntries.clear();
			sortedEntries = null;

			lastUpdateTime = System.currentTimeMillis();
			updating = false;

			plugin.getLogHandler().log("Leaderboard updated. Took {0} ms.", System.currentTimeMillis() - start);

			// Save the data
			plugin.getPlayerDataCache().save();

			// Clean up the data
			new BukkitRunnable() {

				@Override
				public void run() {
					plugin.getPlayerDataCache().cleanupData();
				}

			}.runTaskLater(plugin, 2L);
		}
	}

	public class DisplayLeaderboardThread extends Thread {
		private final String[] args;
		private final String senderName;

		public DisplayLeaderboardThread(String senderName, String[] args) {
			super("SwornVoting-DisplayLeaderboard");
			this.setPriority(MIN_PRIORITY);
			this.senderName = senderName;
			this.args = args;
			this.start();
		}

		@Override
		public void run() {
			try {
				while (updating) {
					sleep(500L);
				}

				displayLeaderboard(senderName, args);
			} catch (Throwable ex) {
				CommandSender sender = getSender(senderName);
				if (sender != null)
					sendMessage(sender, "&cError: &4Failed to update leaderboard: &c{0}", ex);

				plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "updating leaderboard"));
			}
		}
	}

	private final CommandSender getSender(String name) {
		if (name.equalsIgnoreCase("CONSOLE"))
			return plugin.getServer().getConsoleSender();
		else
			return Util.matchPlayer(name);
	}
}