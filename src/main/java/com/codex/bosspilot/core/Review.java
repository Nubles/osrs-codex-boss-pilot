package com.codex.bosspilot.core;

/**
 * A look back at a finished trip: the rates, and one thing to do differently next time, worked
 * out only from the player's own kills, supplies and loot.
 */
public final class Review
{
	/** A shark, the food most kits are counted in. */
	static final int SHARK = 20;
	/** Coming home with this many kills' worth of supplies left means the trip could have gone on. */
	static final double SPARE = 4;

	private final Boss boss;
	private final Logbook.TripRecord trip;
	private final double killsLeft;
	private final String advice;

	private Review(Boss boss, Logbook.TripRecord trip, double killsLeft, String advice)
	{
		this.boss = boss;
		this.trip = trip;
		this.killsLeft = killsLeft;
		this.advice = advice;
	}

	/**
	 * @param left how many more kills the supplies left at the end would have lasted, or -1
	 */
	public static Review of(Boss boss, Logbook.TripRecord trip, double left)
	{
		return new Review(boss, trip, left, advise(trip, left));
	}

	static String advise(Logbook.TripRecord t, double left)
	{
		if (t.kills <= 0)
		{
			return t.deaths > 0
				? "A death ended the trip before the first kill. Check the setup on the Prepare tab and bring more food."
				: "No kills on this trip.";
		}
		if (t.deaths > 0)
		{
			return t.deaths == 1
				? "You died once. More food, or the best setup on the Prepare tab, would make the next trip safer."
				: "You died " + t.deaths + " times. More food, or the best setup on the Prepare tab, would make the next trip safer.";
		}
		double perKill = (double) t.foodUsed / t.kills;
		if (left >= 0 && left < 1 && perKill > 0)
		{
			long sharks = Math.max(1, Math.round(Math.ceil(perKill * 2 / SHARK)));
			return "You ran low after " + plural(t.kills, "kill", "kills") + ". About " + plural(sharks, "more shark", "more sharks")
				+ " would last two more kills.";
		}
		if (left >= SPARE)
		{
			return "You came back with supplies for about " + (long) Math.floor(left)
				+ " more kills. Stay longer, or bring less and keep the room for loot.";
		}
		return "A well-packed trip: you used most of what you brought.";
	}

	private static String plural(long n, String one, String many)
	{
		return n + " " + (n == 1 ? one : many);
	}

	public Boss getBoss()
	{
		return boss;
	}

	public Logbook.TripRecord getTrip()
	{
		return trip;
	}

	public double getKillsLeft()
	{
		return killsLeft;
	}

	public String getAdvice()
	{
		return advice;
	}

	public double killsPerHour()
	{
		double m = trip.minutes();
		return m <= 0 ? 0 : trip.kills * 60 / m;
	}

	public long lootPerHour()
	{
		double m = trip.minutes();
		return m <= 0 ? 0 : Math.round(trip.loot * 60 / m);
	}
}
