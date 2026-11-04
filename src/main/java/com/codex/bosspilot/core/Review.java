package com.codex.bosspilot.core;

import java.util.List;

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
	private final Debrief debrief;

	private Review(Boss boss, Logbook.TripRecord trip, double killsLeft, String advice, Debrief debrief)
	{
		this.boss = boss;
		this.trip = trip;
		this.killsLeft = killsLeft;
		this.advice = advice;
		this.debrief = debrief;
	}

	/**
	 * @param left how many more kills the supplies left at the end would have lasted, or -1
	 */
	public static Review of(Boss boss, Logbook.TripRecord trip, double left)
	{
		return new Review(boss, trip, left, advise(trip, left), null);
	}

	/**
	 * The review with its debrief: why the trip ended and how it compares with the trips before it.
	 *
	 * @param earlier the trips kept for this boss before this one, oldest first
	 */
	public static Review of(Boss boss, Logbook.TripRecord trip, List<Logbook.TripRecord> earlier)
	{
		return new Review(boss, trip, trip.killsLeft, advise(trip, trip.killsLeft), Debrief.of(trip, earlier));
	}

	/** Why the trip ended and how it went next to your best, or null on a review made without one. */
	public Debrief getDebrief()
	{
		return debrief;
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
			return (t.deaths == 1 ? "You died once." : "You died " + t.deaths + " times.")
				+ " Read the briefing on the Prepare tab, and bring more food or the best setup there.";
		}
		if (Debrief.Ending.OUT_OF_CHARGES.name().equals(t.ended))
		{
			return "Your " + (t.outOf == null ? "charged gear" : t.outOf) + " ran out before your supplies did. Top it up at the bank:"
				+ " the kit step on the Prepare tab says when it will not last the trip.";
		}
		double perKill = (double) t.foodUsed / t.kills;
		double dosesPerKill = (double) t.dosesUsed / t.kills;
		if (left >= 0 && left < 1 && Debrief.Ending.OUT_OF_PRAYER.name().equals(t.ended) && dosesPerKill > 0)
		{
			long potions = Math.max(1, Math.round(Math.ceil(dosesPerKill * 2 / Debrief.DOSES_A_POTION)));
			return "Your prayer ran out after " + plural(t.kills, "kill", "kills") + ". About "
				+ plural(potions, "more prayer potion", "more prayer potions") + " would last two more kills.";
		}
		if (left >= 0 && left < 1 && perKill > 0)
		{
			long sharks = Math.max(1, Math.round(Math.ceil(perKill * 2 / SHARK)));
			return "You ran low after " + plural(t.kills, "kill", "kills") + ". About " + plural(sharks, "more shark", "more sharks")
				+ " would last two more kills.";
		}
		if (left >= SPARE && Debrief.Ending.BAG_FULL.name().equals(t.ended))
		{
			return "Your bag filled up with supplies for about " + (long) Math.floor(left)
				+ " more kills still in it. Bring less, and keep the room for loot.";
		}
		if (left >= SPARE)
		{
			return "You came back with supplies for about " + (long) Math.floor(left)
				+ " more kills. Stay longer, or bring less and keep the room for loot.";
		}
		return "A well-packed trip: you used most of what you brought.";
	}

	/** "Took 412 damage, 82 a kill. Biggest hit 38.", or null when no hits were seen. */
	public String damageText()
	{
		if (trip.damage <= 0)
		{
			return null;
		}
		StringBuilder out = new StringBuilder("Took ").append(trip.damage).append(" damage");
		if (trip.kills > 0)
		{
			out.append(", ").append(Math.round((double) trip.damage / trip.kills)).append(" a kill");
		}
		out.append(". Biggest hit ").append(trip.biggestHit).append('.');
		return out.toString();
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
