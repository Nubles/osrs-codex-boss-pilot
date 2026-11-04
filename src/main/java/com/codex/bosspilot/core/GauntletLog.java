package com.codex.bosspilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Boss Pilot's own log of the player's Gauntlet and Corrupted Gauntlet runs, saved per RS profile
 * under {@code codexbosspilot/gauntletV1}: every run's preparation and Hunllef fight times, how it
 * ended, the best splits in each mode (the ghost), the completed runs since each rare reward in
 * each mode, and the recent runs in full. Only completed runs roll the rewards (The Gauntlet:
 * failing "will not grant any chance of unique items").
 */
public final class GauntletLog
{
	public static final String KEY = "gauntletV1";
	static final int VERSION = 1;
	/** Runs kept in full. */
	static final int RECENT = 40;
	/** A gap between one run's start and the next longer than this is a break, not one run. */
	public static final long PACE_GAP_SECONDS = 20 * 60;

	public enum Outcome
	{
		COMPLETED,
		DIED,
		LEFT
	}

	/** One run. Public fields, so Gson can save it as it is. */
	public static final class Run
	{
		/** When the run began (or ended, when its start was not seen). */
		public long at;
		public Gauntlet.Mode mode = Gauntlet.Mode.REGULAR;
		public Outcome outcome = Outcome.LEFT;
		/** Seconds in the preparation, or -1 when not known. */
		public double prep = -1;
		/** Seconds fighting the Hunllef, or -1 when not known or never reached. */
		public double fight = -1;
		/** The whole run in seconds, or -1. */
		public double total = -1;
		/** The completion count the game gave, or -1. */
		public int kc = -1;
		/** Whether the times came from the game's own lines rather than Boss Pilot's tick count. */
		public boolean gameTimed;
		/** Seconds since the run before it began, on the same sitting, or -1. */
		public double cycle = -1;
		/** This run's splits against the best before it: negative is faster; null when there was none. */
		public Double prepVsBest;
		public Double fightVsBest;
		/** The rare rewards it brought, by name. */
		public List<String> uniques = new ArrayList<>();

		public boolean completed()
		{
			return outcome == Outcome.COMPLETED;
		}
	}

	/** Totals of one mode. Public fields for Gson. */
	public static final class Totals
	{
		public int runs;
		public int completed;
		public int died;
		public int left;
		/** The game's last completion count, or -1. */
		public int kc = -1;
		/** The best preparation and fight of a completed run, and the best whole run; 0 when none. */
		public double bestPrep;
		public double bestFight;
		public double bestTotal;
		/** Completed runs with both splits known, and their summed splits, for the averages. */
		public int split;
		public double prepSum;
		public double fightSum;
		/** Completed runs since each reward last came (or since the log began), by name. */
		public Map<String, Integer> since = new LinkedHashMap<>();

		public double averagePrep()
		{
			return split == 0 ? -1 : prepSum / split;
		}

		public double averageFight()
		{
			return split == 0 ? -1 : fightSum / split;
		}

		/** The best preparation and best fight together: the ghost to beat. */
		public double ghostTotal()
		{
			return bestPrep > 0 && bestFight > 0 ? bestPrep + bestFight : -1;
		}
	}

	public int version = VERSION;
	public Totals regular = new Totals();
	public Totals corrupted = new Totals();
	/** Runs timed start to start on a sitting, their seconds, completions and deaths, for the pace. */
	public int paced;
	public double pacedSeconds;
	public int pacedCompleted;
	public int pacedDied;
	/** Each reward received, by name, how many times. */
	public Map<String, Integer> found = new LinkedHashMap<>();
	public List<Run> recent = new ArrayList<>();

	public static GauntletLog parse(Gson gson, String json)
	{
		if (json == null || json.isEmpty())
		{
			return new GauntletLog();
		}
		try
		{
			GauntletLog log = gson.fromJson(json, GauntletLog.class);
			if (log == null)
			{
				return new GauntletLog();
			}
			log.regular = fix(log.regular);
			log.corrupted = fix(log.corrupted);
			if (log.found == null)
			{
				log.found = new LinkedHashMap<>();
			}
			if (log.recent == null)
			{
				log.recent = new ArrayList<>();
			}
			log.recent.removeIf(r -> r == null);
			for (Run r : log.recent)
			{
				if (r.mode == null)
				{
					r.mode = Gauntlet.Mode.REGULAR;
				}
				if (r.outcome == null)
				{
					r.outcome = Outcome.LEFT;
				}
				if (r.uniques == null)
				{
					r.uniques = new ArrayList<>();
				}
			}
			return log;
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return new GauntletLog();
		}
	}

	private static Totals fix(Totals t)
	{
		if (t == null)
		{
			return new Totals();
		}
		if (t.since == null)
		{
			t.since = new LinkedHashMap<>();
		}
		return t;
	}

	public String toJson(Gson gson)
	{
		return gson.toJson(this);
	}

	public Totals totals(Gauntlet.Mode mode)
	{
		return mode == Gauntlet.Mode.CORRUPTED ? corrupted : regular;
	}

	/** Adds a finished run; its splits are set against the best before it. */
	public void add(Run r)
	{
		Totals t = totals(r.mode);
		t.runs++;
		switch (r.outcome)
		{
			case COMPLETED:
				t.completed++;
				break;
			case DIED:
				t.died++;
				break;
			default:
				t.left++;
		}
		if (r.kc > 0 && r.completed())
		{
			t.kc = r.kc;
		}
		if (r.prep > 0)
		{
			r.prepVsBest = t.bestPrep > 0 ? r.prep - t.bestPrep : null;
		}
		if (r.fight > 0 && r.completed())
		{
			r.fightVsBest = t.bestFight > 0 ? r.fight - t.bestFight : null;
		}
		if (r.completed())
		{
			if (r.prep > 0)
			{
				t.bestPrep = t.bestPrep <= 0 ? r.prep : Math.min(t.bestPrep, r.prep);
			}
			if (r.fight > 0)
			{
				t.bestFight = t.bestFight <= 0 ? r.fight : Math.min(t.bestFight, r.fight);
			}
			if (r.total > 0)
			{
				t.bestTotal = t.bestTotal <= 0 ? r.total : Math.min(t.bestTotal, r.total);
			}
			if (r.prep > 0 && r.fight > 0)
			{
				t.split++;
				t.prepSum += r.prep;
				t.fightSum += r.fight;
			}
			// Only a completed run rolls the rewards.
			for (Gauntlet.Unique u : Gauntlet.Unique.values())
			{
				t.since.merge(u.getName(), 1, Integer::sum);
			}
		}
		if (r.cycle > 0 && r.cycle <= PACE_GAP_SECONDS)
		{
			paced++;
			pacedSeconds += r.cycle;
			pacedCompleted += r.completed() ? 1 : 0;
			pacedDied += r.outcome == Outcome.DIED ? 1 : 0;
		}
		List<String> got = new ArrayList<>(r.uniques);
		r.uniques.clear();
		recent.add(r);
		while (recent.size() > RECENT)
		{
			recent.remove(0);
		}
		for (String name : got)
		{
			late(name);
		}
	}

	/**
	 * A reward that showed after its run was logged, such as a seed taken from the reward chest
	 * or the pet's message a tick late: it belongs to the last completed run.
	 *
	 * @return whether it was added
	 */
	public boolean late(String name)
	{
		Run last = lastCompleted();
		if (last == null || last.uniques.contains(name))
		{
			return false;
		}
		last.uniques.add(name);
		found.merge(name, 1, Integer::sum);
		regular.since.put(name, 0);
		corrupted.since.put(name, 0);
		return true;
	}

	public boolean isEmpty()
	{
		return recent.isEmpty() && regular.runs == 0 && corrupted.runs == 0;
	}

	public int runs()
	{
		return regular.runs + corrupted.runs;
	}

	public int completed()
	{
		return regular.completed + corrupted.completed;
	}

	public int died()
	{
		return regular.died + corrupted.died;
	}

	/** The last run, or null. */
	public Run last()
	{
		return recent.isEmpty() ? null : recent.get(recent.size() - 1);
	}

	/** The last completed run, or null. */
	public Run lastCompleted()
	{
		for (int i = recent.size() - 1; i >= 0; i--)
		{
			if (recent.get(i).completed())
			{
				return recent.get(i);
			}
		}
		return null;
	}

	/** Completed runs since this reward last came, in one mode. */
	public int since(Gauntlet.Unique u, Gauntlet.Mode mode)
	{
		return totals(mode).since.getOrDefault(u.getName(), 0);
	}

	/** Completed runs of both modes since this reward last came. */
	public int since(Gauntlet.Unique u)
	{
		return since(u, Gauntlet.Mode.REGULAR) + since(u, Gauntlet.Mode.CORRUPTED);
	}

	public int found(Gauntlet.Unique u)
	{
		return found.getOrDefault(u.getName(), 0);
	}

	/** Completed runs since any reward came. */
	public int sinceAny()
	{
		int min = Integer.MAX_VALUE;
		for (Gauntlet.Unique u : Gauntlet.Unique.values())
		{
			min = Math.min(min, since(u));
		}
		return min == Integer.MAX_VALUE ? 0 : min;
	}

	/** Completions an hour at the player's own pace, start to start; -1 before any. */
	public double completionsPerHour()
	{
		return pacedSeconds <= 0 ? -1 : pacedCompleted * 3600.0 / pacedSeconds;
	}

	/** Deaths an hour at the player's own pace; -1 before any run was paced. */
	public double deathsPerHour()
	{
		return pacedSeconds <= 0 ? -1 : pacedDied * 3600.0 / pacedSeconds;
	}

	/** The mode played most lately, regular before any run. */
	public Gauntlet.Mode lastMode()
	{
		Run r = last();
		return r == null ? Gauntlet.Mode.REGULAR : r.mode;
	}
}
