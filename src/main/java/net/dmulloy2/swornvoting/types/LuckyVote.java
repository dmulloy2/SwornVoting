/**
 * (c) 2015 dmulloy2
 */
package net.dmulloy2.swornvoting.types;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author dmulloy2
 */

@Data
@AllArgsConstructor
public class LuckyVote {
	private final String name;
	private final int chance;
	private final int coins;
	private final int cash;
	private final String message;
}