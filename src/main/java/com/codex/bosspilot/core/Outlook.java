package com.codex.bosspilot.core;

/**
 * How many more kills the supplies the player carries will last, from what this trip has used
 * so far, or from past trips when this one has no kills yet. It only ever looks at the player's
 * own food and potions, never at the boss.
 */
public final class Outlook
{
	public enum Level
	{
		/** Not enough to go on yet. */
		UNKNOWN,
		/** Three kills or more. */
		PLENTY,
		/** One to three kills. */
		FEW,
		/** Less than one more kill: time to bank after this one. */
		LAST
	}

	/** At or above this, supplies are plenty. */
	static final double PLENTY = 3;

	private final Level level;
	private final double kills;
	private final boolean fromHistory;
	private final String limit;

	private Outlook(Level level, double kills, boolean fromHistory, String limit)
	{
		this.level = level;
		this.kills = kills;
		this.fromHistory = fromHistory;
		this.limit = limit;
	}

	public static final Outlook UNKNOWN = new Outlook(Level.UNKNOWN, -1, false, null);

	/**
	 * @param trip the trip so far
	 * @param past rates from the player's earlier trips to this boss
	 */
	public static Outlook of(Trip trip, Logbook.Rates past)
	{
		if (trip == null || trip.getFoodNow() < 0)
		{
			return UNKNOWN;
		}
		double food;
		double doses;
		boolean history;
		if (trip.getKills() > 0)
		{
			food = (double) trip.getFoodUsed() / trip.getKills();
			doses = (double) trip.getDosesUsed() / trip.getKills();
			history = false;
		}
		else if (past != null && past.kills > 0)
		{
			food = past.foodPerKill();
			doses = past.dosesPerKill();
			history = true;
		}
		else
		{
			return UNKNOWN;
		}
		double byFood = food > 0 ? trip.getFoodNow() / food : Double.POSITIVE_INFINITY;
		double byDoses = doses > 0 ? trip.getDosesNow() / doses : Double.POSITIVE_INFINITY;
		double left = Math.min(byFood, byDoses);
		if (Double.isInfinite(left))
		{
			// Nothing used yet per kill: nothing to say until something runs down.
			return UNKNOWN;
		}
		String limit = byFood <= byDoses ? "food" : "prayer";
		Level level = left >= PLENTY ? Level.PLENTY : left >= 1 ? Level.FEW : Level.LAST;
		return new Outlook(level, left, history, limit);
	}

	public Level getLevel()
	{
		return level;
	}

	/** Kills the supplies carried will last, or -1 when not known. */
	public double getKills()
	{
		return kills;
	}

	/** Whether the figure comes from earlier trips because this one has no kills yet. */
	public boolean isFromHistory()
	{
		return fromHistory;
	}

	/** What runs out first: "food" or "prayer", or null. */
	public String getLimit()
	{
		return limit;
	}

	/** "Supplies for about 4 more kills", "Bank after this kill: your food is nearly gone". */
	public String text()
	{
		switch (level)
		{
			case PLENTY:
			case FEW:
				long n = Math.max(1, (long) Math.floor(kills));
				return "Supplies for about " + n + " more " + (n == 1 ? "kill" : "kills") + (fromHistory ? " (from past trips)" : "");
			case LAST:
				return "Bank after this kill: your " + limit + " is nearly gone";
			default:
				return "Supplies: learning your pace";
		}
	}
}
