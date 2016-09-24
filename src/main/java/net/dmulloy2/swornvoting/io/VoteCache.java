/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.io;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import net.dmulloy2.swornvoting.SwornVoting;
import net.dmulloy2.util.Util;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * @author dmulloy2
 */

public class VoteCache {
	private static final String FILE_NAME = "cache.yml";
	private YamlConfiguration config;
	private File file;

	private final ConcurrentMap<UUID, List<String>> cache;
	private final SwornVoting plugin;

	public VoteCache(SwornVoting plugin) {
		this.plugin = plugin;
		this.cache = new ConcurrentHashMap<>();
		this.loadCache();
	}

	private void loadCache() {
		try {
			this.file = new File(plugin.getDataFolder(), FILE_NAME);
			if (! file.exists())
				file.createNewFile();
		} catch (Throwable ex) {
			// This really shouldn't happen ever
			plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "ensuring vote cache exists"));
			return;
		}

		this.config = new YamlConfiguration();

		try {
			config.load(file);
		} catch (Throwable ex) {
			// The vote cache is corrupt
			plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "loading vote cache"));
			if (! file.renameTo(new File(plugin.getDataFolder(), FILE_NAME + "_bad")))
				file.delete();
		}

		Map<String, Object> values = config.getValues(false);
		for (Entry<String, Object> entry : values.entrySet()) {
			try {
				UUID uniqueId = UUID.fromString(entry.getKey());

				@SuppressWarnings("unchecked")
				List<String> votes = (List<String>) entry.getValue();
				cache.put(uniqueId, votes);
			} catch (Throwable ex) {
				plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "loading cache for " + entry.getKey()));
			}
		}
	}

	public List<String> getData(UUID uniqueId) {
		List<String> value = cache.get(uniqueId);
		if (value == null)
			value = loadData(uniqueId);

		return value;
	}

	public List<String> getData(OfflinePlayer player) {
		return getData(player.getUniqueId());
	}

	public List<String> loadData(UUID uniqueId) {
		if (config.isSet(uniqueId.toString()))
			return config.getStringList(uniqueId.toString());

		return null;
	}

	public void putData(OfflinePlayer player, List<String> list) {
		putData(player.getUniqueId(), list);
	}

	public void putData(UUID uniqueId, List<String> list) {
		cache.put(uniqueId, list);
	}

	public void removeData(OfflinePlayer player) {
		removeData(player.getUniqueId());
	}

	public void removeData(UUID uniqueId) {
		cache.remove(uniqueId);
		config.set(uniqueId.toString(), null);
	}

	public void save() {
		if (file.exists())
			file.delete();

		try {
			file.createNewFile();
		} catch (Throwable ex) {
			plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "creating cache save"));
			return;
		}

		for (Entry<UUID, List<String>> entry : cache.entrySet()) {
			config.set(entry.getKey().toString(), entry.getValue());
		}

		try {
			config.save(file);
		} catch (Throwable ex) {
			plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "saving cache"));
			if (! file.renameTo(new File(plugin.getDataFolder(), FILE_NAME + "_bad")))
				file.delete();
		}
	}
}