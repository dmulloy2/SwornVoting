/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.io;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import net.dmulloy2.io.Closer;
import net.dmulloy2.swornvoting.SwornVoting;
import net.dmulloy2.swornvoting.types.PlayerData;
import net.dmulloy2.util.Util;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

public class PlayerDataCache {
	private static final String PREFIX = "jdbc:sqlite:";
	private static final String FILE_NAME = "players.db";
	private static final String TABLE = "players";

	private final ConcurrentMap<UUID, PlayerData> data;
	private final Connection connection;
	private final SwornVoting plugin;

	public PlayerDataCache(SwornVoting plugin) throws Throwable {
		this.plugin = plugin;
		this.data = new ConcurrentHashMap<>();

		File file = new File(plugin.getDataFolder(), FILE_NAME);
		String database = PREFIX + file.getPath();

		Class.forName("org.sqlite.JDBC");
		this.connection = DriverManager.getConnection(database);

		// Create table
		Statement statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " (identifier VARCHAR(255));");
		Closer.closeQuietly(statement);
	}

	public PlayerData getData(UUID uniqueId) {
		PlayerData value = data.get(uniqueId);
		if (value == null)
			value = loadData(uniqueId);

		return value;
	}

	public PlayerData getData(OfflinePlayer player) {
		// Special case for online
		if (player.isOnline())
			return getData(player.getPlayer());

		return getData(player.getUniqueId());
	}

	public PlayerData getData(Player player) {
		PlayerData value = getData(player.getUniqueId());
		if (value == null) {
			value = new PlayerData();
			data.put(player.getUniqueId(), value);
		}

		value.setLastKnownBy(player.getName());
		return value;
	}

	public PlayerData loadData(UUID uniqueId) {
		Statement statement = null;
		ResultSet results = null;

		try {
			statement = connection.createStatement();
			String sql = "SELECT * FROM " + TABLE + " WHERE identifier='" + uniqueId + "';";
			results = statement.executeQuery(sql);

			PlayerData value = null;
			if (results.next()) {
				value = new PlayerData(results);
			}

			if (value != null) {
				data.put(uniqueId, value);
			}

			return value;
		} catch (Throwable ex) {
			plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "loading data for {0}", uniqueId));
		} finally {
			Closer.closeQuietly(statement);
			Closer.closeQuietly(results);
		}

		return null;
	}

	public Map<UUID, PlayerData> getLoadedData() {
		return Collections.unmodifiableMap(data);
	}

	public Map<UUID, PlayerData> getAllData() {
		Map<UUID, PlayerData> data = new HashMap<>();
		data.putAll(this.data);

		Statement statement = null;
		ResultSet results = null;

		try {
			statement = connection.createStatement();
			results = statement.executeQuery("SELECT * FROM " + TABLE + " ORDER BY votes");

			while (results.next()) {
				String identifier = results.getString(1);

				try {
					UUID uniqueId = UUID.fromString(identifier);
					if (! data.containsKey(uniqueId)) {
						PlayerData value = new PlayerData(results);
						data.put(uniqueId, value);
					}
				} catch (Throwable ex) {
					plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "loading data for {0}", identifier));
				}
			}
		} catch (Throwable ex) {
			plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "loading all data"));
		} finally {
			Closer.closeQuietly(statement);
			Closer.closeQuietly(results);
		}

		return data;
	}

	public void save() {
		String sql = "";
		Statement statement = null;

		try {
			statement = connection.createStatement();

			// Ensure columns exist
			Map<String, String> columns = new LinkedHashMap<>();
			columns.put("coins", "INTEGER");
			columns.put("votes", "INTEGER");
			columns.put("lastKnownBy", "VARCHAR(255)");

			for (Entry<String, String> entry : columns.entrySet()) {
				String name = entry.getKey();
				String type = entry.getValue();

				try {
					if (! columnExists(name)) {
						sql = "ALTER TABLE " + TABLE + " ADD " + name + " " + type + ";";
						statement.executeUpdate(sql);
					}
				} catch (SQLException ex) {
					// This is usually caused by no data existing, just ignore it
					// plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "ensuring column {0} exists", name));
				}
			}

			Closer.closeQuietly(statement);
			statement = connection.createStatement();

			for (Entry<UUID, PlayerData> entry : data.entrySet()) {
				UUID uniqueId = entry.getKey();
				PlayerData data = entry.getValue();

				try {
					if (! rowExists("identifier", uniqueId.toString())) {
						sql = String.format("INSERT INTO %s (identifier, coins, votes, lastKnownBy) VALUES ('%s', %s, %s, '%s')",
								TABLE,
								uniqueId.toString(),
								data.getCoins(),
								data.getVotes(),
								data.getLastKnownBy()
						);
					} else {
						sql = String.format("UPDATE %s SET coins=%s, votes=%s, lastKnownBy='%s' WHERE identifier='%s';",
								TABLE,
								data.getCoins(),
								data.getVotes(),
								data.getLastKnownBy(),
								uniqueId.toString()
						);
					}

					statement.executeUpdate(sql);
				} catch (Throwable ex) {
					plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "saving data for {0}", uniqueId));
				}
			}
		} catch (Throwable ex) {
			plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "saving data"));
		} finally {
			Closer.closeQuietly(statement);
		}
	}

	public void cleanupData() {
		// Get all online players into an array list
		List<UUID> online = new ArrayList<>();
		for (Player player : Util.getOnlinePlayers()) {
			online.add(player.getUniqueId());
		}

		// Actually cleanup the data
		for (UUID key : getLoadedData().keySet()) {
			if (! online.contains(key))
				data.remove(key);
		}

		// Clear references
		online.clear();
		online = null;
	}

	private boolean columnExists(String column) {
		Statement statement = null;

		try {
			String sql = "SELECT " + column + " from " + TABLE + ";";
			statement = connection.createStatement();
			return statement.executeQuery(sql).next();
		} catch (Throwable ex) {
			return false;
		} finally {
			Closer.closeQuietly(statement);
		}
	}

	private boolean rowExists(String identifier, String row) {
		Statement statement = null;

		try {
			String sql = "SELECT * from " + TABLE + " WHERE " + identifier + "='" + row + "'";
			statement = connection.createStatement();
			return statement.executeQuery(sql).next();
		} catch (Throwable ex) {
			return false;
		} finally {
			Closer.closeQuietly(statement);
		}
	}
}