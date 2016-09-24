/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.types;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dmulloy2.types.IPermission;

/**
 * @author dmulloy2
 */

@Getter
@AllArgsConstructor
public enum Permission implements IPermission {
	FAKE("cmd.fake"),
	LEADERBOARD_FORCE("cmd.leaderboard.force"),
	RELOAD("cmd.reload"),
	;

	private final String node;
}