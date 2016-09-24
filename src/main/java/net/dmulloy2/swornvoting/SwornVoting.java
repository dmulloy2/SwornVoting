/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Getter;
import net.dmulloy2.SwornPlugin;
import net.dmulloy2.commands.CmdHelp;
import net.dmulloy2.gui.GUIHandler;
import net.dmulloy2.handlers.CommandHandler;
import net.dmulloy2.handlers.LogHandler;
import net.dmulloy2.handlers.PermissionHandler;
import net.dmulloy2.integration.VaultHandler;
import net.dmulloy2.swornvoting.commands.CmdFake;
import net.dmulloy2.swornvoting.commands.CmdInfo;
import net.dmulloy2.swornvoting.commands.CmdLeaderboard;
import net.dmulloy2.swornvoting.commands.CmdReload;
import net.dmulloy2.swornvoting.commands.CmdShop;
import net.dmulloy2.swornvoting.handlers.ShopHandler;
import net.dmulloy2.swornvoting.handlers.VoteHandler;
import net.dmulloy2.swornvoting.io.PlayerDataCache;
import net.dmulloy2.swornvoting.io.VoteCache;
import net.dmulloy2.swornvoting.listeners.PlayerListener;
import net.dmulloy2.swornvoting.listeners.VotifierListener;
import net.dmulloy2.util.FormatUtil;
import net.dmulloy2.util.Util;

/**
 * @author dmulloy2
 */

public class SwornVoting extends SwornPlugin {
	private @Getter VaultHandler vaultHandler;
	private @Getter ShopHandler shopHandler;
	private @Getter VoteHandler voteHandler;
	private @Getter GUIHandler guiHandler;

	private @Getter PlayerDataCache playerDataCache;
	private @Getter VoteCache voteCache;

	private List<Listener> listeners;
	private @Getter String prefix = FormatUtil.format("&3[&eSwornVoting&3]&e ");

	@Override
	public void onEnable() {
		long start = System.currentTimeMillis();

		// Register log handler
		logHandler = new LogHandler(this);

		// Votifier check
		PluginManager pm = getServer().getPluginManager();
		if (! pm.isPluginEnabled("Votifier")) {
			logHandler.log(Level.SEVERE, "Votifier is required to run SwornVoting!");
			logHandler.log(Level.SEVERE, "Download: http://dev.bukkit.org/bukkit-plugins/votifier/");
			pm.disablePlugin(this);
			return;
		}

		// Configuration
		saveDefaultConfig();
		reloadConfig();

		// Register generic handlers
		permissionHandler = new PermissionHandler(this);
		commandHandler = new CommandHandler(this);
		guiHandler = new GUIHandler(this);

		// Register other handlers
		shopHandler = new ShopHandler(this);
		voteHandler = new VoteHandler(this);

		try {
			playerDataCache = new PlayerDataCache(this);
		} catch (Throwable ex) {
			logHandler.log(Level.SEVERE, Util.getUsefulStack(ex, "connecting to SQL database"));
			pm.disablePlugin(this);
			return;
		}

		voteCache = new VoteCache(this);

		// Integration
		setupIntegration();

		// Register commands
		commandHandler.setCommandPrefix("vote");
		commandHandler.registerPrefixedCommand(new CmdFake(this));
		commandHandler.registerPrefixedCommand(new CmdHelp(this));
		commandHandler.registerPrefixedCommand(new CmdInfo(this));
		commandHandler.registerPrefixedCommand(new CmdLeaderboard(this));
		commandHandler.registerPrefixedCommand(new CmdReload(this));
		commandHandler.registerPrefixedCommand(new CmdShop(this));

		// Register listeners
		listeners = new ArrayList<>();
		registerListener(new PlayerListener(this));
		registerListener(new VotifierListener(this));

		// Deploy auto save task
		class AutoSaveTask extends BukkitRunnable {

			@Override
			public void run() {
				playerDataCache.save();
				voteCache.save();
			}

		}

		if (getConfig().getBoolean("autoSave.enabled")) {
			int interval = getConfig().getInt("autoSave.interval") * 120;
			if (getConfig().getBoolean("autoSave.async")) {
				new AutoSaveTask().runTaskTimerAsynchronously(this, interval, interval);
			} else {
				new AutoSaveTask().runTaskTimer(this, interval, interval);
			}
		}

		logHandler.log("{0} has been enabled. Took {1} ms.", getDescription().getFullName(), System.currentTimeMillis() - start);
	}

	private void setupIntegration() {
		try {
			vaultHandler = new VaultHandler(this);
		} catch (Throwable ex) {
		}
	}

	private void registerListener(Listener listener) {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(listener, this);
		listeners.add(listener);
	}

	@Override
	public void onDisable() {
		long start = System.currentTimeMillis();

		// Cancel tasks
		getServer().getScheduler().cancelTasks(this);

		// Save data
		playerDataCache.save();
		voteCache.save();

		logHandler.log("{0} has been disabled. Took {1} ms.", getDescription().getFullName(), System.currentTimeMillis() - start);
	}

	@Override
	public void reload() {
		reloadConfig();
		voteHandler.reload();
		shopHandler.reload();
		extraHelp = null;
	}

	private List<String> extraHelp;

	@Override
	public List<String> getExtraHelp() {
		if (extraHelp == null) {
			extraHelp = new ArrayList<>();

			// Add the help from the config
			extraHelp.addAll(getConfig().getStringList("extraHelp"));

			// Add the voting link
			extraHelp.add("&eLink:&b " + getConfig().getString("votingLink"));
			extraHelp.add("");
		}

		return extraHelp;
	}
}