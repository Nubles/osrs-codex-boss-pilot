package com.codex.bosspilot.core;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * The Gauntlet as the panel and plaque show it: the run under way (only its phase and the
 * preparation clock; nothing moves during the Hunllef fight), the player's own run log with the
 * ghost of their best preparation and fight, and the four rare rewards against their rates.
 */
public final class GauntletRun
{
	private final GauntletLog log;
	private final GauntletWatch.Phase phase;
	private final Gauntlet.Mode mode;
	/** Seconds into the preparation now, or -1. */
	private final double prepNow;
	/** How long the preparation took once the fight began, or -1. */
	private final double prepTaken;
	private final Set<Gauntlet.Unique> owned;

	public GauntletRun(GauntletLog log, GauntletWatch.Phase phase, Gauntlet.Mode mode, double prepNow, double prepTaken, Collection<String> ownedNames)
	{
		this.log = log == null ? new GauntletLog() : log;
		this.phase = phase == null ? GauntletWatch.Phase.NONE : phase;
		this.mode = mode == null ? this.log.lastMode() : mode;
		this.prepNow = prepNow;
		this.prepTaken = prepTaken;
		Set<Gauntlet.Unique> o = EnumSet.noneOf(Gauntlet.Unique.class);
		o.addAll(Gauntlet.uniques(ownedNames));
		for (Gauntlet.Unique u : Gauntlet.Unique.values())
		{
			if (this.log.found(u) > 0)
			{
				o.add(u);
			}
		}
		this.owned = o;
	}

	/** Only the log, between runs. */
	public static GauntletRun idle(GauntletLog log, Collection<String> ownedNames)
	{
		return new GauntletRun(log, GauntletWatch.Phase.NONE, null, -1, -1, ownedNames);
	}

	public GauntletLog getLog()
	{
		return log;
	}

	public GauntletWatch.Phase getPhase()
	{
		return phase;
	}

	/** The mode of the run under way, or the one played last. */
	public Gauntlet.Mode getMode()
	{
		return mode;
	}

	public double getPrepNow()
	{
		return prepNow;
	}

	public double getPrepTaken()
	{
		return prepTaken;
	}

	public boolean owns(Gauntlet.Unique u)
	{
		return owned.contains(u);
	}

	public int ownedCount()
	{
		return owned.size();
	}

	/** The best preparation in the mode under way, or 0. */
	public double ghostPrep()
	{
		return log.totals(mode).bestPrep;
	}

	/** The best fight in the mode under way, or 0. */
	public double ghostFight()
	{
		return log.totals(mode).bestFight;
	}

	/**
	 * The run under way in a line: "Corrupted: preparing, 3:12 of 7:30", "Corrupted: fighting the
	 * Hunllef. Prep took 5:58", or null between runs.
	 */
	public String phaseText()
	{
		String m = mode.getLabel();
		switch (phase)
		{
			case PREP:
				return prepNow < 0 ? m + ": preparing" : m + ": preparing, " + Format.clock(prepNow) + " of " + Format.clock(mode.getPrepLimit());
			case FIGHT:
				return prepTaken < 0 ? m + ": fighting the Hunllef" : m + ": fighting the Hunllef. Prep took " + Format.clock(prepTaken);
			case ENDING:
				return m + ": run over";
			default:
				return null;
		}
	}

	/** The run under way, short, for the plaque: "Preparing 4:07 / 7:30", "Hunllef fight, prep 5:52", or null. */
	public String plaquePhase()
	{
		switch (phase)
		{
			case PREP:
				return prepNow < 0 ? "Preparing" : "Preparing " + Format.clock(prepNow) + " / " + Format.clock(mode.getPrepLimit());
			case FIGHT:
				return prepTaken < 0 ? "Hunllef fight" : "Hunllef fight, prep " + Format.clock(prepTaken);
			case ENDING:
				return "Run over";
			default:
				return null;
		}
	}

	/** The last run, short, for the plaque: "Last: KC 49 in 8:49", "Last: died in the prep", or null. */
	public String plaqueLast()
	{
		GauntletLog.Run r = log.last();
		if (r == null)
		{
			return null;
		}
		switch (r.outcome)
		{
			case COMPLETED:
				return "Last: " + (r.kc > 0 ? "KC " + Format.number(r.kc) : "completed") + (r.total > 0 ? " in " + Format.clock(r.total) : "");
			case DIED:
				return r.fight > 0 ? "Last: died to the Hunllef" : "Last: died in the prep";
			default:
				return r.fight > 0 ? "Last: left the fight" : "Last: left in the prep";
		}
	}

	/** The last run against the ghost, short: "prep -0:12", else "fight +0:05", or null. */
	public String plaqueLastRight()
	{
		GauntletLog.Run r = log.last();
		if (r == null)
		{
			return null;
		}
		if (r.prepVsBest != null)
		{
			return "prep " + Gauntlet.delta(r.prepVsBest);
		}
		return r.fightVsBest == null ? null : "fight " + Gauntlet.delta(r.fightVsBest);
	}

	/** The ghost beside the phase: "best prep 5:40", or null. Fixed while the fight goes on. */
	public String ghostText()
	{
		double g = ghostPrep();
		if (g <= 0)
		{
			return null;
		}
		if (phase == GauntletWatch.Phase.FIGHT && prepTaken > 0)
		{
			return "prep " + Gauntlet.delta(prepTaken - g);
		}
		return "best " + Format.clock(g);
	}

	/** The last run in a line: "Last: completed in 8:31, prep 5:58, fight 2:33", or null. */
	public String lastText()
	{
		GauntletLog.Run r = log.last();
		return r == null ? null : runText(r);
	}

	/** One run in a line, for the plaque and the panel. */
	public static String runText(GauntletLog.Run r)
	{
		StringBuilder b = new StringBuilder();
		switch (r.outcome)
		{
			case COMPLETED:
				b.append(r.total > 0 ? "Completed in " + Format.clock(r.total) : "Completed");
				break;
			case DIED:
				b.append(r.fight > 0 ? "Died to the Hunllef" : "Died in the prep");
				break;
			default:
				b.append(r.fight > 0 ? "Left the fight" : "Left in the prep");
		}
		if (r.prep > 0)
		{
			b.append(", prep ").append(Format.clock(r.prep));
		}
		if (r.fight > 0)
		{
			b.append(", fight ").append(Format.clock(r.fight));
		}
		return b.toString();
	}

	/** The last run's splits against the ghost before it: "prep -0:12, fight +0:05", or null. */
	public String lastVsGhost()
	{
		GauntletLog.Run r = log.last();
		if (r == null || r.prepVsBest == null && r.fightVsBest == null)
		{
			return null;
		}
		StringBuilder b = new StringBuilder();
		if (r.prepVsBest != null)
		{
			b.append("prep ").append(Gauntlet.delta(r.prepVsBest));
		}
		if (r.fightVsBest != null)
		{
			b.append(b.length() > 0 ? ", " : "").append("fight ").append(Gauntlet.delta(r.fightVsBest));
		}
		return b.toString();
	}

	/**
	 * How the dry spell on a reward compares: "212 runs since your last (180 regular, 32
	 * corrupted): 46% would have one by now", or null before any completed run.
	 */
	public String dryText(Gauntlet.Unique u)
	{
		if (log.completed() == 0)
		{
			return null;
		}
		int r = log.since(u, Gauntlet.Mode.REGULAR);
		int c = log.since(u, Gauntlet.Mode.CORRUPTED);
		int n = r + c;
		if (n == 0)
		{
			return "Your last completed run brought one";
		}
		StringBuilder b = new StringBuilder(Format.plural(n, "run", "runs")).append(log.found(u) > 0 ? " since your last" : " without one");
		if (r > 0 && c > 0)
		{
			b.append(" (").append(Format.number(r)).append(" regular, ").append(Format.number(c)).append(" corrupted)");
		}
		else
		{
			b.append(c > 0 ? ", corrupted" : ", regular");
		}
		b.append(": ").append(Gauntlet.percent(Gauntlet.atLeastOne(u, r, c))).append(" would have one by now");
		return b.toString();
	}

	/** The dry spell against the rate, 1 at the rate: runs times the chance, by mode. */
	public double spell(Gauntlet.Unique u)
	{
		return Gauntlet.expected(u, log.since(u, Gauntlet.Mode.REGULAR), log.since(u, Gauntlet.Mode.CORRUPTED));
	}

	/** The plaque line on the rewards: "Enhanced seed: 46% by now", for the one most wanted not owned. */
	public String plaqueDropText()
	{
		if (log.completed() == 0)
		{
			return null;
		}
		for (Gauntlet.Unique u : Gauntlet.Unique.values())
		{
			if (!owns(u))
			{
				return u.getShortName() + ": " + Format.plural(log.since(u), "run", "runs") + " dry";
			}
		}
		return Format.plural(log.sinceAny(), "run", "runs") + " since a rare reward";
	}

	/** The chance figure beside the plaque's drop line, or null. */
	public String plaqueDropRight()
	{
		for (Gauntlet.Unique u : Gauntlet.Unique.values())
		{
			if (!owns(u) && log.completed() > 0)
			{
				return Gauntlet.percent(Gauntlet.atLeastOne(u, log.since(u, Gauntlet.Mode.REGULAR), log.since(u, Gauntlet.Mode.CORRUPTED))) + " by now";
			}
		}
		return null;
	}
}
