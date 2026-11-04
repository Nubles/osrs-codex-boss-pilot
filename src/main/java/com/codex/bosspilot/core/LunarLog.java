package com.codex.bosspilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Boss Pilot's own log of the player's Lunar Chests, saved per RS profile under
 * {@code codexbosspilot/lunarV1}: totals for every chest seen, the recent chests in full, the moons
 * subdued before each, the set pieces each brought, how many kills of each moon have passed since
 * its set last gave a piece, and the supplies used on the runs that were watched. The pace (chests
 * and coins an hour) comes only from runs timed from the chest before on the same trip.
 */
public final class LunarLog
{
	public static final String KEY = "lunarV1";
	static final int VERSION = 1;
	/** Chests kept in full. */
	static final int RECENT = 30;
	/** A run longer than this is a break, not one run. */
	public static final long PACE_GAP_SECONDS = 30 * 60;

	/** One chest. Public fields, so Gson can save it as it is. */
	public static final class Chest
	{
		public long at;
		/** The chest count the game gave, or -1. */
		public int kc = -1;
		/** Its worth at the prices of the day, or -1 when the contents were not seen. */
		public long value = -1;
		/** The moons subdued before it, as a mask (bit 0 Blood, 1 Blue, 2 Eclipse), or -1 when not seen. */
		public int moons = -1;
		/** Seconds from the chest before on the same trip (or the trip's start), or -1. */
		public long seconds = -1;
		/** The loot, as "96 x Atlatl dart". */
		public List<String> items = new ArrayList<>();
		/** The set pieces it held. */
		public List<String> pieces = new ArrayList<>();
		/** The supplies used on its run, by name, or empty when none were watched. */
		public Map<String, Long> used = new LinkedHashMap<>();

		public int moonCount()
		{
			return moons < 0 ? -1 : Integer.bitCount(moons);
		}
	}

	public int version = VERSION;
	public int chests;
	/** Chests whose contents were seen, and their worth. */
	public int valued;
	public long value;
	/** Chests whose moons were seen, and how many moons. */
	public int moonsSeen;
	public int moonsTotal;
	/** Runs timed, their seconds and their worth, for the pace. */
	public int timed;
	public long timedSeconds;
	public long timedValue;
	/** Each moon's kills logged with a chest, by its name. */
	public Map<String, Integer> kills = new LinkedHashMap<>();
	/** Kills of each moon since its set last gave a piece (or since the log began), by its name. */
	public Map<String, Integer> since = new LinkedHashMap<>();
	/** Each set piece received, by name, how many times. */
	public Map<String, Integer> found = new LinkedHashMap<>();
	/** Runs whose supplies were watched, and what they used in all, by name. */
	public int suppliedRuns;
	public Map<String, Long> used = new LinkedHashMap<>();
	public List<Chest> recent = new ArrayList<>();

	public static LunarLog parse(Gson gson, String json)
	{
		if (json == null || json.isEmpty())
		{
			return new LunarLog();
		}
		try
		{
			LunarLog log = gson.fromJson(json, LunarLog.class);
			if (log == null)
			{
				return new LunarLog();
			}
			if (log.kills == null)
			{
				log.kills = new LinkedHashMap<>();
			}
			if (log.since == null)
			{
				log.since = new LinkedHashMap<>();
			}
			if (log.found == null)
			{
				log.found = new LinkedHashMap<>();
			}
			if (log.used == null)
			{
				log.used = new LinkedHashMap<>();
			}
			if (log.recent == null)
			{
				log.recent = new ArrayList<>();
			}
			for (Chest c : log.recent)
			{
				if (c.items == null)
				{
					c.items = new ArrayList<>();
				}
				if (c.pieces == null)
				{
					c.pieces = new ArrayList<>();
				}
				if (c.used == null)
				{
					c.used = new LinkedHashMap<>();
				}
			}
			return log;
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return new LunarLog();
		}
	}

	public String toJson(Gson gson)
	{
		return gson.toJson(this);
	}

	/** Adds a chest. */
	public void add(Chest c)
	{
		chests++;
		if (c.value >= 0)
		{
			valued++;
			value += c.value;
		}
		if (c.seconds > 0 && c.seconds <= PACE_GAP_SECONDS && c.value >= 0)
		{
			timed++;
			timedSeconds += c.seconds;
			timedValue += c.value;
		}
		Set<Moons.Moon> subdued = c.moons < 0 ? java.util.EnumSet.noneOf(Moons.Moon.class) : Moons.fromMask(c.moons);
		if (c.moons >= 0)
		{
			moonsSeen++;
			moonsTotal += subdued.size();
		}
		for (Moons.Moon m : subdued)
		{
			kills.merge(m.getName(), 1, Integer::sum);
			since.merge(m.getName(), 1, Integer::sum);
		}
		for (String p : c.pieces)
		{
			found.merge(p, 1, Integer::sum);
			Moons.Moon m = Moons.moonOf(p);
			if (m != null)
			{
				since.put(m.getName(), 0);
			}
		}
		if (!c.used.isEmpty())
		{
			suppliedRuns++;
			c.used.forEach((k, v) -> used.merge(k, v, Long::sum));
		}
		recent.add(c);
		while (recent.size() > RECENT)
		{
			recent.remove(0);
		}
	}

	/** The chest count message came a moment after its chest was logged: it belongs to the last one. */
	public void lateKc(int kc)
	{
		Chest last = last();
		if (last != null && last.kc < 0)
		{
			last.kc = kc;
		}
	}

	public boolean isEmpty()
	{
		return chests == 0;
	}

	/** The last chest, or null. */
	public Chest last()
	{
		return recent.isEmpty() ? null : recent.get(recent.size() - 1);
	}

	/** Chests an hour at the player's own pace, or -1 before any run was timed. */
	public double chestsPerHour()
	{
		return timedSeconds <= 0 ? -1 : timed * 3600.0 / timedSeconds;
	}

	/** Chest value an hour at the player's own pace, or -1 before any run was timed. */
	public double valuePerHour()
	{
		return timedSeconds <= 0 ? -1 : timedValue * 3600.0 / timedSeconds;
	}

	/** The average worth of a chest, or -1 before any was seen. */
	public long average()
	{
		return valued == 0 ? -1 : value / valued;
	}

	/** The average run time in seconds, or -1 before any run was timed. */
	public double averageRun()
	{
		return timed == 0 ? -1 : (double) timedSeconds / timed;
	}

	/** Moons subdued a run on average, or -1 before any chest's moons were seen. */
	public double moonsPerRun()
	{
		return moonsSeen == 0 ? -1 : (double) moonsTotal / moonsSeen;
	}

	/** Kills of this moon logged with a chest. */
	public int kills(Moons.Moon m)
	{
		return kills.getOrDefault(m.getName(), 0);
	}

	/** Kills of this moon since its set last gave a piece, or since the log began. */
	public int since(Moons.Moon m)
	{
		return since.getOrDefault(m.getName(), 0);
	}

	/** How many times this piece came. */
	public int found(String piece)
	{
		return found.getOrDefault(piece, 0);
	}

	/** Supplies used a run on average, by name, most first; empty before any run was watched. */
	public Map<String, Double> usedPerRun()
	{
		Map<String, Double> out = new LinkedHashMap<>();
		if (suppliedRuns == 0)
		{
			return out;
		}
		used.entrySet().stream()
			.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
			.forEach(e -> out.put(e.getKey(), e.getValue() / (double) suppliedRuns));
		return out;
	}

	/** The pieces never seen in the player's bank, pack or chests, in set order. */
	public List<String> missing(Collection<String> ownedNames)
	{
		Set<String> owned = new LinkedHashSet<>(Moons.pieces(ownedNames));
		owned.addAll(found.keySet());
		return Moons.missing(owned);
	}
}
