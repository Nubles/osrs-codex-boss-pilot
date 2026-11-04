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
 * Boss Pilot's own log of the player's Barrows chests, saved per RS profile under
 * {@code codexbosspilot/barrowsV1}: totals for every chest seen, the recent chests in full, the
 * Barrows pieces each brought and how many chests have passed since the last one. Loot an hour
 * comes from the chests whose run was timed from start to chest.
 */
public final class ChestLog
{
	public static final String KEY = "barrowsV1";
	static final int VERSION = 1;
	/** Chests kept in full. */
	static final int RECENT = 30;

	/** One chest. Public fields, so Gson can save it as it is. */
	public static final class Chest
	{
		public long at;
		/** The chest count the game gave, or -1. */
		public int kc = -1;
		/** Its worth at the prices of the day, or -1 when the contents were not seen. */
		public long value = -1;
		public int brothers;
		public int potential = -1;
		/** Seconds from the start of the run to the chest, or -1 when the run was not timed. */
		public long seconds = -1;
		/** The Barrows pieces it held. */
		public List<String> pieces = new ArrayList<>();
	}

	public int version = VERSION;
	public int chests;
	/** Chests whose contents were seen, and their worth. */
	public int valued;
	public long value;
	/** Timed chests, their worth and their seconds, for loot an hour. */
	public int timed;
	public long timedValue;
	public long timedSeconds;
	/** Chests since the last Barrows piece (or since the log began), and the brothers killed before each. */
	public List<Integer> dry = new ArrayList<>();
	/** Every Barrows piece from a chest, by name, how many times. */
	public Map<String, Integer> found = new LinkedHashMap<>();
	public List<Chest> recent = new ArrayList<>();

	public static ChestLog parse(Gson gson, String json)
	{
		if (json == null || json.isEmpty())
		{
			return new ChestLog();
		}
		try
		{
			ChestLog log = gson.fromJson(json, ChestLog.class);
			if (log == null)
			{
				return new ChestLog();
			}
			if (log.dry == null)
			{
				log.dry = new ArrayList<>();
			}
			if (log.found == null)
			{
				log.found = new LinkedHashMap<>();
			}
			if (log.recent == null)
			{
				log.recent = new ArrayList<>();
			}
			return log;
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return new ChestLog();
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
		if (c.seconds > 0 && c.value >= 0)
		{
			timed++;
			timedValue += c.value;
			timedSeconds += c.seconds;
		}
		if (c.pieces.isEmpty())
		{
			dry.add(c.brothers);
		}
		else
		{
			dry.clear();
			for (String p : c.pieces)
			{
				found.merge(p, 1, Integer::sum);
			}
		}
		recent.add(c);
		while (recent.size() > RECENT)
		{
			recent.remove(0);
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

	/** Coins an hour over the timed chests, or -1 before any. */
	public double lootPerHour()
	{
		return timedSeconds <= 0 ? -1 : timedValue * 3600.0 / timedSeconds;
	}

	/** Chests an hour over the timed chests, or -1 before any. */
	public double chestsPerHour()
	{
		return timedSeconds <= 0 ? -1 : timed * 3600.0 / timedSeconds;
	}

	/** The average worth of a chest, or -1 before any was seen. */
	public long average()
	{
		return valued == 0 ? -1 : value / valued;
	}

	/** Chests since the last Barrows piece. */
	public int dryStreak()
	{
		return dry.size();
	}

	/**
	 * How often a run of chests this long goes without a Barrows piece, at the brothers killed
	 * before each: the product of each chest's chance of none.
	 */
	public double dryOdds()
	{
		double p = 1;
		for (int n : dry)
		{
			p *= 1 - Barrows.chanceAny(n);
		}
		return p;
	}

	/** The pieces never seen in the player's bank, pack or chests, in set order. */
	public List<String> missing(Collection<String> ownedNames)
	{
		Set<String> owned = new LinkedHashSet<>(Barrows.pieces(ownedNames));
		owned.addAll(found.keySet());
		List<String> out = new ArrayList<>();
		for (Barrows.Brother b : Barrows.Brother.values())
		{
			for (String p : b.getPieces())
			{
				if (!owned.contains(p))
				{
					out.add(p);
				}
			}
		}
		return out;
	}
}
