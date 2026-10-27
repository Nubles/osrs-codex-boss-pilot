package com.codex.bosspilot.core;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Boss Pilot's own record of the player's bossing, saved per RS profile under
 * {@code codexbosspilot/logbookV1}: the last kill count seen for each boss, the fastest kill and
 * the last few trips. It is what "your trips here make about 400k an hour" and "you usually need
 * 40 hitpoints of food a kill" are worked out from.
 */
public final class Logbook
{
	public static final String KEY = "logbookV1";
	static final int VERSION = 1;
	/** Trips kept per boss. */
	static final int TRIPS = 12;

	/** One finished trip. Public fields, so Gson can save it as it is. */
	public static final class TripRecord
	{
		public long start;
		public long end;
		public int kills;
		public long loot;
		public int deaths;
		public long foodUsed;
		public long dosesUsed;
		public double bestKill;

		public double minutes()
		{
			return Math.max(0, end - start) / 60000.0;
		}
	}

	/** Everything kept for one boss. */
	public static final class Entry
	{
		public int kc = -1;
		public double best;
		public List<TripRecord> trips = new ArrayList<>();
	}

	public int version = VERSION;
	public Map<String, Entry> bosses = new LinkedHashMap<>();

	public static Logbook parse(Gson gson, String json)
	{
		if (gson == null || json == null || json.trim().isEmpty())
		{
			return new Logbook();
		}
		try
		{
			Logbook book = gson.fromJson(json, Logbook.class);
			if (book == null || book.version != VERSION || book.bosses == null)
			{
				return new Logbook();
			}
			book.bosses.values().removeIf(e -> e == null);
			for (Entry e : book.bosses.values())
			{
				if (e.trips == null)
				{
					e.trips = new ArrayList<>();
				}
				e.trips.removeIf(t -> t == null);
			}
			return book;
		}
		catch (RuntimeException e)
		{
			return new Logbook();
		}
	}

	public String toJson(Gson gson)
	{
		return gson.toJson(this);
	}

	public Entry entry(Boss boss)
	{
		return bosses.computeIfAbsent(boss.getKey(), k -> new Entry());
	}

	/** The entry, or null when the boss has never been seen. */
	public Entry peek(Boss boss)
	{
		return bosses.get(boss.getKey());
	}

	/** The highest kill count the game has told us, or -1. */
	public int kc(Boss boss)
	{
		Entry e = peek(boss);
		return e == null ? -1 : e.kc;
	}

	/** Whether the player has been to this boss on a trip or has a kill count for it. */
	public boolean visited(Boss boss)
	{
		Entry e = peek(boss);
		return e != null && (e.kc > 0 || !e.trips.isEmpty());
	}

	public void noteKc(Boss boss, int kc)
	{
		Entry e = entry(boss);
		e.kc = Math.max(e.kc, kc);
	}

	public void noteBest(Boss boss, double seconds)
	{
		if (seconds <= 0)
		{
			return;
		}
		Entry e = entry(boss);
		e.best = e.best <= 0 ? seconds : Math.min(e.best, seconds);
	}

	public void add(Boss boss, TripRecord trip)
	{
		Entry e = entry(boss);
		e.trips.add(trip);
		while (e.trips.size() > TRIPS)
		{
			e.trips.remove(0);
		}
		if (trip.bestKill > 0)
		{
			noteBest(boss, trip.bestKill);
		}
	}

	/** Totals over the kept trips that had kills. */
	public static final class Rates
	{
		public final int trips;
		public final int kills;
		public final double minutes;
		public final long loot;
		public final long food;
		public final long doses;

		Rates(int trips, int kills, double minutes, long loot, long food, long doses)
		{
			this.trips = trips;
			this.kills = kills;
			this.minutes = minutes;
			this.loot = loot;
			this.food = food;
			this.doses = doses;
		}

		public double killsPerHour()
		{
			return minutes <= 0 ? 0 : kills * 60 / minutes;
		}

		public long lootPerHour()
		{
			return minutes <= 0 ? 0 : Math.round(loot * 60 / minutes);
		}

		/** Hitpoints of food used per kill, or -1 when not known. */
		public double foodPerKill()
		{
			return kills <= 0 ? -1 : (double) food / kills;
		}

		public double dosesPerKill()
		{
			return kills <= 0 ? -1 : (double) doses / kills;
		}

		public boolean isKnown()
		{
			return kills > 0 && minutes > 0;
		}
	}

	public Rates rates(Boss boss)
	{
		Entry e = peek(boss);
		int trips = 0;
		int kills = 0;
		double minutes = 0;
		long loot = 0;
		long food = 0;
		long doses = 0;
		if (e != null)
		{
			for (TripRecord t : e.trips)
			{
				if (t.kills <= 0)
				{
					continue;
				}
				trips++;
				kills += t.kills;
				minutes += t.minutes();
				loot += t.loot;
				food += t.foodUsed;
				doses += t.dosesUsed;
			}
		}
		return new Rates(trips, kills, minutes, loot, food, doses);
	}

	public TripRecord last(Boss boss)
	{
		Entry e = peek(boss);
		return e == null || e.trips.isEmpty() ? null : e.trips.get(e.trips.size() - 1);
	}
}
