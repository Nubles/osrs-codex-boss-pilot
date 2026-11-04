package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The debrief of a finished trip: why it ended, how it went next to your best trip at this boss,
 * where it ranks among the trips kept, and the habit your recent trips show. Everything comes
 * from the player's own kills, loot, deaths and supplies, read before and after the fight, never
 * from what the boss did.
 */
public final class Debrief
{
	/** What the tracker saw end the trip. */
	public enum Cause
	{
		/** The boss was not seen for a while: the player left. */
		TIMED_OUT,
		LOGGED_OUT,
		HOPPED,
		/** A different boss came into sight. */
		MOVED_ON
	}

	/** Why the trip ended, as the player would put it. Saved by name: never rename one. */
	public enum Ending
	{
		DIED("Died", "Death"),
		OUT_OF_FOOD("Out of food", "Food"),
		OUT_OF_PRAYER("Out of prayer", "Prayer"),
		BAG_FULL("Bag full", "Full bag"),
		OUT_OF_CHARGES("Out of charges", "Charges"),
		LOGGED_OUT("Logged out", "Logout"),
		HOPPED("Hopped", "Hop"),
		MOVED_ON("Moved on", "Moved on"),
		LEFT_EARLY("Left early", "Spare"),
		LEFT("Banked", "Banked");

		private final String label;
		private final String tag;

		Ending(String label, String tag)
		{
			this.label = label;
			this.tag = tag;
		}

		public String getLabel()
		{
			return label;
		}

		/** A word for the trip history: "Food", "Death". */
		public String getTag()
		{
			return tag;
		}

		/** A saved name back to its ending, or null for trips saved before endings were. */
		public static Ending parse(String name)
		{
			if (name == null)
			{
				return null;
			}
			try
			{
				return valueOf(name);
			}
			catch (IllegalArgumentException e)
			{
				return null;
			}
		}

		/** Whether it is a reason to change what you bring, rather than a choice you made. */
		public boolean isForced()
		{
			return this == DIED || this == OUT_OF_FOOD || this == OUT_OF_PRAYER || this == BAG_FULL || this == OUT_OF_CHARGES;
		}
	}

	/** How many more kills the supplies left with would have lasted, and whether prayer ran short first. */
	public static final class Left
	{
		public final double kills;
		public final boolean prayer;

		public Left(double kills, boolean prayer)
		{
			this.kills = kills;
			this.prayer = prayer;
		}
	}

	/** Whether this trip did better or worse than the best one on a line. */
	public enum Verdict
	{
		BETTER,
		SAME,
		WORSE
	}

	/** One line of the comparison: this trip next to the best one. */
	public static final class Row
	{
		private final String label;
		private final String now;
		private final String best;
		private final Verdict verdict;
		private final boolean up;

		Row(String label, String now, String best, Verdict verdict, boolean up)
		{
			this.label = label;
			this.now = now;
			this.best = best;
			this.verdict = verdict;
			this.up = up;
		}

		/** Whether this trip's figure is the higher one, which is not always the better one. */
		public boolean isUp()
		{
			return up;
		}

		public String getLabel()
		{
			return label;
		}

		public String getNow()
		{
			return now;
		}

		public String getBest()
		{
			return best;
		}

		public Verdict getVerdict()
		{
			return verdict;
		}
	}

	/** One trip in the history strip, oldest first. */
	public static final class Bar
	{
		private final double value;
		private final Ending ending;
		private final boolean current;
		private final boolean best;

		Bar(double value, Ending ending, boolean current, boolean best)
		{
			this.value = value;
			this.ending = ending;
			this.current = current;
			this.best = best;
		}

		public double getValue()
		{
			return value;
		}

		/** Null for a trip saved before endings were. */
		public Ending getEnding()
		{
			return ending;
		}

		public boolean isCurrent()
		{
			return current;
		}

		public boolean isBest()
		{
			return best;
		}
	}

	/** Supplies left for this many kills or more means the trip could have gone on. */
	static final double SPARE = Review.SPARE;
	/** A trip shorter than this is too short to rate an hour from. */
	static final double MIN_MINUTES = 1;
	/** Within this share of each other, two rates are the same. */
	static final double SAME = 0.03;
	/** Recent trips read for a habit, and how many of them must share an ending. */
	static final int PATTERN_TRIPS = 6;
	static final int PATTERN_MIN = 3;
	/** Doses in a prayer potion: OSRS Wiki, Prayer potion, examine "4 doses of Prayer restore potion." */
	static final int DOSES_A_POTION = 4;

	private final Ending ending;
	private final String reason;
	private final boolean byLoot;
	private final int rank;
	private final int of;
	private final List<Row> rows;
	private final List<Bar> history;
	private final String pattern;
	private final Ending patternEnding;

	private Debrief(Ending ending, String reason, boolean byLoot, int rank, int of, List<Row> rows, List<Bar> history, String pattern, Ending patternEnding)
	{
		this.patternEnding = patternEnding;
		this.ending = ending;
		this.reason = reason;
		this.byLoot = byLoot;
		this.rank = rank;
		this.of = of;
		this.rows = rows;
		this.history = history;
		this.pattern = pattern;
	}

	/** Why a trip ended, from what it left with and what ended it. */
	static Ending ending(Trip t, Left left)
	{
		if (t.diedLast())
		{
			return Ending.DIED;
		}
		if (t.getRanOut() != null)
		{
			return Ending.OUT_OF_CHARGES;
		}
		if (left != null && left.kills < 1)
		{
			return left.prayer ? Ending.OUT_OF_PRAYER : Ending.OUT_OF_FOOD;
		}
		if (t.getLeaveFree() == 0)
		{
			return Ending.BAG_FULL;
		}
		switch (t.getCause())
		{
			case LOGGED_OUT:
				return Ending.LOGGED_OUT;
			case HOPPED:
				return Ending.HOPPED;
			case MOVED_ON:
				return Ending.MOVED_ON;
			default:
				return left != null && left.kills >= SPARE ? Ending.LEFT_EARLY : Ending.LEFT;
		}
	}

	/**
	 * @param trip the trip just finished
	 * @param earlier the trips kept for this boss before it, oldest first
	 */
	public static Debrief of(Logbook.TripRecord trip, List<Logbook.TripRecord> earlier)
	{
		Ending ending = Ending.parse(trip.ended);
		if (ending == null)
		{
			ending = trip.deaths > 0 ? Ending.DIED : Ending.LEFT;
		}
		List<Logbook.TripRecord> rated = new ArrayList<>();
		for (Logbook.TripRecord t : earlier)
		{
			if (rated(t))
			{
				rated.add(t);
			}
		}
		boolean byLoot = trip.loot > 0;
		for (Logbook.TripRecord t : rated)
		{
			byLoot |= t.loot > 0;
		}
		Logbook.TripRecord best = null;
		for (Logbook.TripRecord t : rated)
		{
			if (best == null || metric(t, byLoot) > metric(best, byLoot))
			{
				best = t;
			}
		}
		boolean thisRated = rated(trip);
		int rank = 0;
		int of = 0;
		List<Row> rows = Collections.emptyList();
		List<Bar> history = new ArrayList<>();
		if (thisRated)
		{
			double mine = metric(trip, byLoot);
			rank = 1;
			for (Logbook.TripRecord t : rated)
			{
				if (metric(t, byLoot) > mine)
				{
					rank++;
				}
			}
			of = rated.size() + 1;
			if (best != null)
			{
				rows = rows(trip, best, byLoot);
			}
			int from = Math.max(0, rated.size() - (Logbook.TRIPS - 1));
			List<Logbook.TripRecord> shown = new ArrayList<>(rated.subList(from, rated.size()));
			shown.add(trip);
			// The best bar is this trip when it ties, else the first trip to reach the top.
			int top = shown.size() - 1;
			for (int i = 0; i < shown.size() - 1; i++)
			{
				if (metric(shown.get(i), byLoot) > metric(shown.get(top), byLoot))
				{
					top = i;
				}
			}
			for (int i = 0; i < shown.size(); i++)
			{
				Logbook.TripRecord t = shown.get(i);
				boolean current = i == shown.size() - 1;
				history.add(new Bar(metric(t, byLoot), current ? ending : Ending.parse(t.ended), current, i == top));
			}
		}
		Ending[] habit = new Ending[1];
		String pattern = pattern(trip, ending, earlier, habit);
		return new Debrief(ending, reason(trip, ending), byLoot, rank, of, rows, history, pattern, habit[0]);
	}

	static boolean rated(Logbook.TripRecord t)
	{
		return t.kills > 0 && t.minutes() >= MIN_MINUTES;
	}

	static double metric(Logbook.TripRecord t, boolean byLoot)
	{
		double m = t.minutes();
		return m <= 0 ? 0 : (byLoot ? t.loot : t.kills) * 60 / m;
	}

	private static List<Row> rows(Logbook.TripRecord now, Logbook.TripRecord best, boolean byLoot)
	{
		List<Row> out = new ArrayList<>();
		out.add(row("Kills an hour", one(metric(now, false)), one(metric(best, false)), metric(now, false), metric(best, false), true));
		if (byLoot)
		{
			out.add(row("Loot an hour", Format.number(Math.round(metric(now, true))), Format.number(Math.round(metric(best, true))),
				metric(now, true), metric(best, true), true));
		}
		double foodNow = (double) now.foodUsed / now.kills;
		double foodBest = (double) best.foodUsed / best.kills;
		if (foodNow > 0 || foodBest > 0)
		{
			out.add(row("Food a kill", Math.round(foodNow) + " hp", Math.round(foodBest) + " hp", foodNow, foodBest, false));
		}
		double dosesNow = (double) now.dosesUsed / now.kills;
		double dosesBest = (double) best.dosesUsed / best.kills;
		if (dosesNow > 0 || dosesBest > 0)
		{
			out.add(row("Prayer a kill", one(dosesNow), one(dosesBest), dosesNow, dosesBest, false));
		}
		if (now.deaths > 0 || best.deaths > 0)
		{
			out.add(row("Deaths", String.valueOf(now.deaths), String.valueOf(best.deaths), now.deaths, best.deaths, false));
		}
		out.add(row("Kills", String.valueOf(now.kills), String.valueOf(best.kills), now.kills, best.kills, true));
		return out;
	}

	private static Row row(String label, String now, String best, double a, double b, boolean higherIsBetter)
	{
		return new Row(label, now, best, verdict(a, b, higherIsBetter), a > b);
	}

	static Verdict verdict(double now, double best, boolean higherIsBetter)
	{
		double scale = Math.max(Math.abs(now), Math.abs(best));
		if (scale <= 0 || Math.abs(now - best) <= scale * SAME)
		{
			return Verdict.SAME;
		}
		return (now > best) == higherIsBetter ? Verdict.BETTER : Verdict.WORSE;
	}

	static String reason(Logbook.TripRecord t, Ending ending)
	{
		String spare = t.killsLeft >= 1 ? ", with supplies for about " + Format.plural((long) Math.floor(t.killsLeft), "more kill", "more kills") : "";
		switch (ending)
		{
			case DIED:
				return t.kills > 0 ? "A death after " + Format.plural(t.kills, "kill", "kills") + " ended the trip."
					: "A death ended the trip before the first kill.";
			case OUT_OF_FOOD:
				return "Out of food after " + Format.plural(t.kills, "kill", "kills") + ": what was left would not last another.";
			case OUT_OF_PRAYER:
				return "Out of prayer after " + Format.plural(t.kills, "kill", "kills") + ": what was left would not last another.";
			case OUT_OF_CHARGES:
				return "Your " + (t.outOf == null ? "charged gear" : t.outOf) + " ran out after " + Format.plural(t.kills, "kill", "kills")
					+ (t.killsLeft >= 1 ? ", with food and prayer for about " + Format.plural((long) Math.floor(t.killsLeft), "more kill", "more kills") : "")
					+ ".";
			case BAG_FULL:
				return "Your bag was full when you left" + spare + ".";
			case LOGGED_OUT:
				return "You logged out" + spare + ".";
			case HOPPED:
				return "You hopped worlds" + spare + ".";
			case MOVED_ON:
				return "You moved on to another boss" + spare + ".";
			case LEFT_EARLY:
				return "You left with supplies for about " + Format.plural((long) Math.floor(t.killsLeft), "more kill", "more kills") + ".";
			default:
				return t.killsLeft >= 0 ? "You left as your supplies ran low: a well-packed trip." : "You left the boss.";
		}
	}

	/** The ending most of your recent trips here share, and what to bring for it; null when there is none. */
	static String pattern(Logbook.TripRecord now, Ending ending, List<Logbook.TripRecord> earlier)
	{
		return pattern(now, ending, earlier, new Ending[1]);
	}

	private static String pattern(Logbook.TripRecord now, Ending ending, List<Logbook.TripRecord> earlier, Ending[] habit)
	{
		List<Logbook.TripRecord> recent = new ArrayList<>();
		for (int i = earlier.size() - 1; i >= 0 && recent.size() < PATTERN_TRIPS - 1; i--)
		{
			if (Ending.parse(earlier.get(i).ended) != null)
			{
				recent.add(earlier.get(i));
			}
		}
		recent.add(now);
		int n = recent.size();
		Map<Ending, Integer> counts = new EnumMap<>(Ending.class);
		for (Logbook.TripRecord t : recent)
		{
			Ending e = t == now ? ending : Ending.parse(t.ended);
			if (e.isForced())
			{
				counts.merge(e, 1, Integer::sum);
			}
		}
		Ending top = null;
		for (Map.Entry<Ending, Integer> e : counts.entrySet())
		{
			if (top == null || e.getValue() > counts.get(top))
			{
				top = e.getKey();
			}
		}
		if (top == null || counts.get(top) < PATTERN_MIN || counts.get(top) * 2 < n)
		{
			return null;
		}
		habit[0] = top;
		String lead = counts.get(top) + " of your last " + n + " trips here ";
		long kills = 0;
		long food = 0;
		long doses = 0;
		for (Logbook.TripRecord t : recent)
		{
			kills += t.kills;
			food += t.foodUsed;
			doses += t.dosesUsed;
		}
		switch (top)
		{
			case DIED:
				return lead + "ended in a death. Check the setup and briefing on the Prepare tab before the next one.";
			case OUT_OF_CHARGES:
				return lead + "ran out of charges. Check your charged gear before you leave the bank: the Prepare tab says how many kills each lasts.";
			case OUT_OF_FOOD:
			{
				if (kills <= 0 || food <= 0)
				{
					return lead + "ran out of food.";
				}
				long sharks = Math.max(1, (long) Math.ceil(food * 2.0 / kills / Review.SHARK));
				return lead + "ran out of food. About " + Format.plural(sharks, "more shark", "more sharks") + " would last two more kills.";
			}
			case OUT_OF_PRAYER:
			{
				if (kills <= 0 || doses <= 0)
				{
					return lead + "ran out of prayer.";
				}
				long potions = Math.max(1, (long) Math.ceil(doses * 2.0 / kills / DOSES_A_POTION));
				return lead + "ran out of prayer. About " + Format.plural(potions, "more prayer potion", "more prayer potions")
					+ " would last two more kills.";
			}
			default:
			{
				String use = food > 0 ? Format.number(Math.round((double) food / n)) + " hp of food" : null;
				String pray = doses > 0 ? Format.plural(Math.round((double) doses / n), "dose", "doses") + " of prayer" : null;
				String bring = use != null && pray != null ? use + " and " + pray : use != null ? use : pray;
				return lead + "ended with a full bag." + (bring == null ? " Bring less, to keep room for loot."
					: " They used about " + bring + ": bring that, and keep the rest of the room for loot.");
			}
		}
	}

	private static String one(double v)
	{
		return String.format(Locale.ROOT, "%.1f", v);
	}

	public Ending getEnding()
	{
		return ending;
	}

	/** "Out of prayer after 9 kills: what was left would not last another." */
	public String getReason()
	{
		return reason;
	}

	/** Whether trips are rated by loot an hour, else by kills an hour. */
	public boolean isByLoot()
	{
		return byLoot;
	}

	/** This trip's place among the trips kept, from 1; 0 when the trip was too short to rate. */
	public int getRank()
	{
		return rank;
	}

	public int getOf()
	{
		return of;
	}

	/** Whether this trip beat every earlier trip kept. */
	public boolean isNewBest()
	{
		return rank == 1 && of > 1;
	}

	/** This trip next to your best one; empty when there is no earlier trip to compare with. */
	public List<Row> getRows()
	{
		return rows;
	}

	/** The rated trips kept, oldest first, ending with this one. */
	public List<Bar> getHistory()
	{
		return history;
	}

	/** The habit your recent trips show, or null. */
	public String getPattern()
	{
		return pattern;
	}

	/** Whether the habit is this trip's own ending, so it says all the trip's advice would. */
	public boolean isHabitOfThisTrip()
	{
		return pattern != null && patternEnding == ending;
	}

	/** "2nd best of 8 trips by loot an hour", or null. */
	public String rankText()
	{
		if (rank <= 0)
		{
			return null;
		}
		String by = byLoot ? "loot an hour" : "kills an hour";
		if (of == 1)
		{
			return "Your first timed trip here";
		}
		if (rank == 1)
		{
			return "Your best of " + of + " trips by " + by;
		}
		return ordinal(rank) + " best of " + of + " trips by " + by;
	}

	static String ordinal(int n)
	{
		int mod100 = n % 100;
		String suffix = mod100 >= 11 && mod100 <= 13 ? "th" : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
		return n + suffix;
	}
}
