package com.codex.bosspilot.core;

/**
 * Where a trip's time went when the player was not fighting, from the trip clock and the game's
 * own kill times.
 */
public final class Overhead
{
	private Overhead()
	{
	}

	/** Seconds the trip covers. */
	public static double span(Logbook.TripRecord t)
	{
		return t.minutes() * 60;
	}

	/**
	 * The share of the trip not spent fighting, 0 to 1; -1 when not every kill was timed by the game,
	 * so the fighting time is not known.
	 */
	public static double share(Logbook.TripRecord t)
	{
		if (t == null || t.kills <= 0 || t.timed < t.kills || !(t.fightSeconds > 0))
		{
			return -1;
		}
		double span = span(t);
		if (span <= 0)
		{
			return -1;
		}
		return Math.max(0, Math.min(1, 1 - t.fightSeconds / span));
	}
}
