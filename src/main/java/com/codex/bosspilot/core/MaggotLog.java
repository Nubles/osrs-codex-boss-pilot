package com.codex.bosspilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Boss Pilot's own log of the player's Maggot King kills, saved per RS profile under
 * {@code codexbosspilot/maggotKingV1}: totals for every kill seen, how each corpse was looted, the
 * recent kills in full, deaths at the boss, and for each collection log drop the loots since the
 * last one, the chance of going that long without it, and how many were to be expected so far.
 * The pace (kills and loot an hour) comes only from kills timed back to back on one trip, so the
 * walk in and banking do not count against it.
 */
public final class MaggotLog
{
	public static final String KEY = "maggotKingV1";
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
		/** How the corpse was looted, by {@link MaggotKing.Choice} name, or null when it was not seen. */
		public String choice;
		/** The loot's worth at the prices of the day, or -1 when none was seen. */
		public long value = -1;
		/** Seconds since the kill before it on the same trip, or -1. */
		public long seconds = -1;
		/** The loot, as "3 x Stymphike tartare". */
		public List<String> items = new ArrayList<>();
		/** The collection log drops it brought, by {@link MaggotKing.Unique} name. */
		public List<String> uniques = new ArrayList<>();

		public MaggotKing.Choice choice()
		{
			if (choice == null)
			{
				return null;
			}
			try
			{
				return MaggotKing.Choice.valueOf(choice);
			}
			catch (IllegalArgumentException e)
			{
				return null;
			}
		}
	}

	/** Where one drop stands. */
	public static final class Odds
	{
		/** Loots that could have brought it since the last one (or since the log began). */
		public int since;
		/** The natural log of the chance of going without it over those loots. */
		public double missLn;
		/** Drops of it to be expected over every loot logged. */
		public double expected;
		/** How many came. */
		public int found;
	}

	public int version = VERSION;
	public int kills;
	public int stomachs;
	public int eggs;
	/** Deaths on a Maggot King trip. */
	public int deaths;
	/** Kills whose loot was seen, and its worth. */
	public int valued;
	public long value;
	/** Kills timed back to back, their seconds and their loot, for the pace. */
	public int timed;
	public long timedSeconds;
	public long timedValue;
	/** Each drop's odds, by {@link MaggotKing.Unique} name. */
	public Map<String, Odds> odds = new LinkedHashMap<>();
	public List<Kill> recent = new ArrayList<>();

	public static MaggotLog parse(Gson gson, String json)
	{
		if (json == null || json.isEmpty())
		{
			return new MaggotLog();
		}
		try
		{
			MaggotLog log = gson.fromJson(json, MaggotLog.class);
			if (log == null)
			{
				return new MaggotLog();
			}
			if (log.odds == null)
			{
				log.odds = new LinkedHashMap<>();
			}
			if (log.recent == null)
			{
				log.recent = new ArrayList<>();
			}
			return log;
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return new MaggotLog();
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
		MaggotKing.Choice c = k.choice();
		if (c == MaggotKing.Choice.STOMACH)
		{
			stomachs++;
		}
		else if (c == MaggotKing.Choice.EGGS)
		{
			eggs++;
		}
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
		if (c != null)
		{
			// A loot not seen is left out of the odds rather than guessed.
			for (MaggotKing.Unique u : MaggotKing.Unique.values())
			{
				double p = u.rate(c);
				if (p > 0)
				{
					Odds o = odds(u);
					o.since++;
					o.missLn += Math.log(1 - p);
					o.expected += p;
				}
			}
		}
		for (String name : k.uniques)
		{
			MaggotKing.Unique u = MaggotKing.Unique.byKey(name);
			if (u != null)
			{
				got(u);
			}
		}
		recent.add(k);
		while (recent.size() > RECENT)
		{
			recent.remove(0);
		}
	}

	/**
	 * The pet's message after its kill was logged, or from popping an egg taken earlier: it belongs
	 * to the last kill when that one has not got it yet, else it stands on its own.
	 */
	public void late(MaggotKing.Unique u, boolean ofLastKill)
	{
		Kill last = last();
		if (ofLastKill && last != null)
		{
			if (last.uniques.contains(u.name()))
			{
				return;
			}
			last.uniques.add(u.name());
		}
		got(u);
	}

	/** A death on a Maggot King trip. */
	public void died()
	{
		deaths++;
	}

	private void got(MaggotKing.Unique u)
	{
		Odds o = odds(u);
		o.found++;
		o.since = 0;
		o.missLn = 0;
	}

	private Odds odds(MaggotKing.Unique u)
	{
		return odds.computeIfAbsent(u.name(), k -> new Odds());
	}

	/** Where a drop stands; never null. */
	public Odds peek(MaggotKing.Unique u)
	{
		Odds o = odds.get(u.name());
		return o == null ? new Odds() : o;
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

	/** The share of seen loots that took eggs, or -1 before any. */
	public double eggShare()
	{
		int n = stomachs + eggs;
		return n == 0 ? -1 : eggs / (double) n;
	}

	/** Kills whose loot choice was not seen, so are left out of the odds. */
	public int unseen()
	{
		return kills - stomachs - eggs;
	}

	/** Loots since a rare drop came: the elder venator fang, the crimson kisten or the pet, whichever came last. */
	public int sinceRare()
	{
		int min = Integer.MAX_VALUE;
		for (MaggotKing.Unique u : MaggotKing.Unique.values())
		{
			if (!u.isEstimate())
			{
				min = Math.min(min, peek(u).since);
			}
		}
		return min == Integer.MAX_VALUE ? 0 : min;
	}
}
