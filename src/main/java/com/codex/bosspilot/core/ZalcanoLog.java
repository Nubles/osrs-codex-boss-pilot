package com.codex.bosspilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Boss Pilot's own log of the player's Zalcano kills, saved per RS profile under
 * {@code codexbosspilot/zalcanoV1}: totals for every kill seen, the recent kills in full, how many
 * kills have passed since each rare drop and how often each came. The pace (kills and loot an
 * hour) comes only from kills timed back to back on one trip, so the walk in and breaks do not
 * count against it.
 */
public final class ZalcanoLog
{
	public static final String KEY = "zalcanoV1";
	static final int VERSION = 1;
	/** Kills kept in full. */
	static final int RECENT = 30;
	/** A gap between kills longer than this is a break, not one kill. */
	public static final long PACE_GAP_SECONDS = 15 * 60;

	/** One kill. Public fields, so Gson can save it as it is. */
	public static final class Kill
	{
		public long at;
		/** The kill count the game gave, or -1. */
		public int kc = -1;
		/** The loot's worth at the prices of the day, or -1 when none was seen. */
		public long value = -1;
		/** Seconds since the kill before it on the same trip, or -1. */
		public long seconds = -1;
		/** The loot, as "3 x Crystal shard". */
		public List<String> items = new ArrayList<>();
		/** The rare drops it brought, by name. */
		public List<String> uniques = new ArrayList<>();
	}

	public int version = VERSION;
	public int kills;
	/** Kills whose loot was seen, and its worth. */
	public int valued;
	public long value;
	/** Kills timed back to back, their seconds and their loot, for the pace. */
	public int timed;
	public long timedSeconds;
	public long timedValue;
	/** Kills since each rare drop last came (or since the log began), by its name. */
	public Map<String, Integer> since = new LinkedHashMap<>();
	/** Each rare drop received, by name, how many times. */
	public Map<String, Integer> found = new LinkedHashMap<>();
	public List<Kill> recent = new ArrayList<>();

	public static ZalcanoLog parse(Gson gson, String json)
	{
		if (json == null || json.isEmpty())
		{
			return new ZalcanoLog();
		}
		try
		{
			ZalcanoLog log = gson.fromJson(json, ZalcanoLog.class);
			if (log == null)
			{
				return new ZalcanoLog();
			}
			if (log.since == null)
			{
				log.since = new LinkedHashMap<>();
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
			return new ZalcanoLog();
		}
	}

	public String toJson(Gson gson)
	{
		return gson.toJson(this);
	}

	/** Adds a kill. */
	public void add(Kill k)
	{
		kills++;
		if (k.value >= 0)
		{
			valued++;
			value += k.value;
		}
		if (k.seconds > 0 && k.seconds <= PACE_GAP_SECONDS)
		{
			timed++;
			timedSeconds += k.seconds;
			timedValue += Math.max(0, k.value);
		}
		for (Zalcano.Unique u : Zalcano.Unique.values())
		{
			since.merge(u.getName(), 1, Integer::sum);
		}
		for (String name : k.uniques)
		{
			got(name);
		}
		recent.add(k);
		while (recent.size() > RECENT)
		{
			recent.remove(0);
		}
	}

	/**
	 * A rare drop that showed after its kill was logged, such as the pet's message a tick late:
	 * it belongs to the last kill.
	 */
	public void late(String name)
	{
		Kill last = last();
		if (last == null || last.uniques.contains(name))
		{
			return;
		}
		last.uniques.add(name);
		got(name);
	}

	private void got(String name)
	{
		found.merge(name, 1, Integer::sum);
		since.put(name, 0);
	}

	public boolean isEmpty()
	{
		return kills == 0;
	}

	/** The last kill, or null. */
	public Kill last()
	{
		return recent.isEmpty() ? null : recent.get(recent.size() - 1);
	}

	/** Kills an hour at the player's own pace, or -1 before any kill was timed. */
	public double killsPerHour()
	{
		return timedSeconds <= 0 ? -1 : timed * 3600.0 / timedSeconds;
	}

	/** Coins an hour at the player's own pace, or -1 before any kill was timed. */
	public double lootPerHour()
	{
		return timedSeconds <= 0 ? -1 : timedValue * 3600.0 / timedSeconds;
	}

	/** The average worth of a kill, or -1 before any loot was seen. */
	public long average()
	{
		return valued == 0 ? -1 : value / valued;
	}

	/** Kills since this drop last came, or since the log began. */
	public int since(Zalcano.Unique u)
	{
		return since.getOrDefault(u.getName(), 0);
	}

	/** How many times this drop came. */
	public int found(Zalcano.Unique u)
	{
		return found.getOrDefault(u.getName(), 0);
	}

	/** Kills since any rare drop came. */
	public int sinceAny()
	{
		int min = Integer.MAX_VALUE;
		for (Zalcano.Unique u : Zalcano.Unique.values())
		{
			min = Math.min(min, since(u));
		}
		return min == Integer.MAX_VALUE ? 0 : min;
	}
}
