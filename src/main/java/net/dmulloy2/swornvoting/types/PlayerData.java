/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.types;

import java.sql.ResultSet;

import lombok.Data;

/**
 * @author dmulloy2
 */

@Data
public class PlayerData {
	private int coins;
	private int votes;
	private String lastKnownBy;

	public PlayerData() {
	}

	public PlayerData(ResultSet results) throws Throwable {
		this.coins = results.getInt(2);
		this.votes = results.getInt(3);
		this.lastKnownBy = results.getString(4);
	}

	@Override
	public String toString() {
		return "PlayerData[coins=" + coins + ", votes=" + votes + ", lastKnownBy=" + lastKnownBy + "]";
	}
}