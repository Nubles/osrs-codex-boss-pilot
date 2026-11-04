package com.codex.bosspilot.core;

import java.util.Collections;
import java.util.List;

/**
 * The best setup the player can put together from what they own, against one of a boss's
 * monsters: the gear, the attack style and offensive prayer it assumes, and the kill time it
 * gives. Worked out with the Codex combat formulas shared with Codex Gear Roadmap.
 */
public final class Setup
{
	private final String target;
	private final String kind;
	private final double dps;
	private final double killSeconds;
	private final int maxHit;
	private final String style;
	private final String prayer;
	private final String spell;
	private final List<String> gear;

	public Setup(String target, String kind, double dps, double killSeconds, int maxHit, String style, String prayer, String spell,
		List<String> gear)
	{
		this.target = target;
		this.kind = kind;
		this.dps = dps;
		this.killSeconds = killSeconds;
		this.maxHit = maxHit;
		this.style = style;
		this.prayer = prayer;
		this.spell = spell;
		this.gear = Collections.unmodifiableList(gear);
	}

	/**
	 * The monster's name as a setup card shows it, without what the boss's other monsters share:
	 * "Dagannoth Prime" under the Dagannoth Kings reads "Prime".
	 */
	public static String shortName(String bossName, String target)
	{
		if (target == null)
		{
			return null;
		}
		String first = bossName.split(" ")[0] + " ";
		return target.startsWith(first) && !target.equals(bossName) ? target.substring(first.length()) : target;
	}

	public String getTarget()
	{
		return target;
	}

	/** "Melee", "Ranged" or "Magic". */
	public String getKind()
	{
		return kind;
	}

	public double getDps()
	{
		return dps;
	}

	/** Seconds to kill at full uptime, or -1 when it cannot be hurt. */
	public double getKillSeconds()
	{
		return killSeconds;
	}

	public int getMaxHit()
	{
		return maxHit;
	}

	public String getStyle()
	{
		return style;
	}

	/** The offensive prayer assumed, such as "Piety", or null. */
	public String getPrayer()
	{
		return prayer;
	}

	public String getSpell()
	{
		return spell;
	}

	/** The gear, weapon first, by name. */
	public List<String> getGear()
	{
		return gear;
	}
}
