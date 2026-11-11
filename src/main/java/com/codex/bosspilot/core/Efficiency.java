package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Kill efficiency: how close your kills come to what your gear allows. A trip's figure is the
 * kill time the best setup from the gear you brought should give, over the average of the kill
 * times the game itself gave on the trip (its fight durations, never Boss Pilot's clock). 100% is
 * your gear's pace; the pauses inside a fight count against it, so few players reach it.
 *
 * <p>Worked out after each trip from the player's own kills and gear, and shown only between
 * trips. It reads nothing about what a boss does.
 */
public final class Efficiency
{
	/** A trend needs at least this many timed kills, over at least this many trips. */
	static final int WINDOW_KILLS = 30;
	static final int WINDOW_TRIPS = 3;
	/** Within this much (3 points), efficiency has not moved. */
	static final double FLAT = 0.03;

	/** Which way efficiency has gone over the recent trips. */
	public enum Trend
	{
		/** Too few timed kills to tell. */
		NONE,
		RISING,
		FLAT,
		FALLING
	}

	/** A boss's curve, read. */
	public static final class Read
	{
		private final List<Logbook.Point> points;
		private final Trend trend;
		private final double recent;
		private final double change;
		private final int windowKills;

		Read(List<Logbook.Point> points, Trend trend, double recent, double change, int windowKills)
		{
			this.points = points;
			this.trend = trend;
			this.recent = recent;
			this.change = change;
			this.windowKills = windowKills;
		}

		/** The trips on the curve, oldest first. */
		public List<Logbook.Point> getPoints()
		{
			return points;
		}

		public Trend getTrend()
		{
			return trend;
		}

		/** Efficiency over the recent trips, weighted by their kills; 0 when there are none. */
		public double getRecent()
		{
			return recent;
		}

		/** The later half of the window less the earlier half, in fractions (0.05 is 5 points). */
		public double getChange()
		{
			return change;
		}

		/** Timed kills the trend is read over. */
		public int getWindowKills()
		{
			return windowKills;
		}

		public boolean isEmpty()
		{
			return points.isEmpty();
		}

		/** The trips that make up the trend window, oldest first; all of them when there is no trend. */
		public int windowStart()
		{
			int kills = 0;
			for (int i = points.size() - 1; i >= 0; i--)
			{
				kills += points.get(i).kills;
				if (kills >= windowKills && points.size() - i >= WINDOW_TRIPS)
				{
					return i;
				}
			}
			return 0;
		}

		/** What the curve says about practice and gear, in a sentence. */
		public String text()
		{
			switch (trend)
			{
				case RISING:
					return "Up " + Math.round(change * 100) + " points over your last " + windowKills
						+ " timed kills: practice is still paying off.";
				case FLAT:
					return "Flat at about " + percent(recent) + " for your last " + windowKills
						+ " timed kills. More practice is unlikely to speed you up much; a gear upgrade would.";
				case FALLING:
					return "Down " + Math.round(-change * 100) + " points over your last " + windowKills
						+ " timed kills. Check your setup and the briefing before the next trip.";
				default:
					int kills = 0;
					for (Logbook.Point p : points)
					{
						kills += p.kills;
					}
					return Format.plural(kills, "timed kill", "timed kills") + " so far. After " + WINDOW_KILLS + " over "
						+ WINDOW_TRIPS + " trips, Boss Pilot can tell whether you are still getting faster.";
			}
		}
	}

	private Efficiency()
	{
	}

	/** A trip's efficiency, or -1 when it had no timed kills or no best-setup time. */
	public static double of(Logbook.TripRecord t)
	{
		if (t == null || t.timed <= 0 || !(t.fightSeconds > 0) || !(t.theory > 0))
		{
			return -1;
		}
		return t.theory / (t.fightSeconds / t.timed);
	}

	/** The average kill time the game gave on a trip, or -1. */
	public static double average(Logbook.TripRecord t)
	{
		return t == null || t.timed <= 0 || !(t.fightSeconds > 0) ? -1 : t.fightSeconds / t.timed;
	}

	/** Reads a boss's curve: the trend over the last {@link #WINDOW_KILLS} timed kills. */
	public static Read read(List<Logbook.Point> curve)
	{
		List<Logbook.Point> points = curve == null ? Collections.emptyList() : new ArrayList<>(curve);
		if (points.isEmpty())
		{
			return new Read(points, Trend.NONE, 0, 0, 0);
		}
		int kills = 0;
		int start = -1;
		for (int i = points.size() - 1; i >= 0; i--)
		{
			kills += points.get(i).kills;
			if (kills >= WINDOW_KILLS && points.size() - i >= WINDOW_TRIPS)
			{
				start = i;
				break;
			}
		}
		if (start < 0)
		{
			return new Read(points, Trend.NONE, weighted(points, 0, points.size()), 0, 0);
		}
		List<Logbook.Point> window = points.subList(start, points.size());
		int mid = window.size() / 2;
		double first = weighted(window, 0, mid);
		double second = weighted(window, mid, window.size());
		double change = second - first;
		Trend trend = Math.abs(change) < FLAT ? Trend.FLAT : change > 0 ? Trend.RISING : Trend.FALLING;
		return new Read(points, trend, weighted(window, 0, window.size()), change, kills);
	}

	private static double weighted(List<Logbook.Point> points, int from, int to)
	{
		double sum = 0;
		int n = 0;
		for (int i = from; i < to; i++)
		{
			Logbook.Point p = points.get(i);
			int w = Math.max(1, p.kills);
			sum += p.eff * w;
			n += w;
		}
		return n == 0 ? 0 : sum / n;
	}

	/** "Your kills averaged 1:52 against ~1:31 with the best setup you brought." */
	public static String tripText(Logbook.TripRecord t)
	{
		double avg = average(t);
		if (avg <= 0 || !(t.theory > 0))
		{
			return null;
		}
		return "Your " + Format.plural(t.timed, "timed kill", "timed kills") + " averaged " + Format.clock(avg) + " against ~"
			+ Format.clock(t.theory) + " with the best setup from the gear you brought.";
	}

	/** What the figure rests on, said every time it is shown. */
	public static String basis()
	{
		return "From the game's own fight durations, against the best setup from the gear you carried, with the boosts"
			+ " set in Boss Pilot's settings. Pauses inside a fight count against you, so 100% is rare.";
	}

	/** "81%". */
	public static String percent(double eff)
	{
		return String.format(Locale.ROOT, "%.0f%%", eff * 100);
	}
}
