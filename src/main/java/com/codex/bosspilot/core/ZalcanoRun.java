package com.codex.bosspilot.core;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Zalcano as the panel and plaque show it: the player's own kill log, the rare drops already owned
 * (from the log, the bank or the pack), and the ways into Prifddinas from Codex Wayfinder.
 */
public final class ZalcanoRun
{
	private final ZalcanoLog log;
	private final Set<Zalcano.Unique> owned;
	private final WayIn wayIn;

	public ZalcanoRun(ZalcanoLog log, Collection<String> ownedNames, WayIn wayIn)
	{
		this.log = log == null ? new ZalcanoLog() : log;
		Set<Zalcano.Unique> o = EnumSet.noneOf(Zalcano.Unique.class);
		if (ownedNames != null)
		{
			o.addAll(Zalcano.uniques(ownedNames));
		}
		for (Zalcano.Unique u : Zalcano.Unique.values())
		{
			if (this.log.found(u) > 0)
			{
				o.add(u);
			}
		}
		this.owned = o;
		this.wayIn = wayIn == null ? WayIn.none() : wayIn;
	}

	public ZalcanoLog getLog()
	{
		return log;
	}

	/** Whether the player has this drop: received while logged, or seen in the bank or pack. */
	public boolean owns(Zalcano.Unique u)
	{
		return owned.contains(u);
	}

	public WayIn getWayIn()
	{
		return wayIn;
	}

	/**
	 * How the dry spell on a drop compares: "312 kills dry. 78% of players get one by now", or
	 * null before any kill.
	 */
	public String dryText(Zalcano.Unique u)
	{
		int n = log.since(u);
		if (log.isEmpty())
		{
			return null;
		}
		if (n == 0)
		{
			return "Your last kill brought one";
		}
		String kills = Format.plural(n, "kill", "kills") + (log.found(u) > 0 ? " since your last" : " without one");
		double hi = Zalcano.atLeastOne(u.getHigh(), n);
		if (u.ranged())
		{
			double lo = Zalcano.atLeastOne(u.getLow(), n);
			return kills + ": " + Zalcano.percent(lo) + " to " + Zalcano.percent(hi) + " would have one by now";
		}
		return kills + ": " + Zalcano.percent(hi) + " would have one by now";
	}

	/** How far the dry spell is past the drop's rate, 1 at the rate: kills times the chance. */
	public double spell(Zalcano.Unique u)
	{
		return log.since(u) * (u.getLow() + u.getHigh()) / 2;
	}

	/** The plaque line on a trip: "214 kills since a rare drop", or the pace. */
	public String plaqueText()
	{
		if (log.isEmpty())
		{
			return "Your first logged kill is next";
		}
		int n = log.sinceAny();
		return n == 0 ? "Your last kill brought a rare drop!" : Format.plural(n, "kill", "kills") + " since a rare drop";
	}

	/** The pace on the plaque: "24 kills/hr", or null before a timed kill. */
	public String paceText()
	{
		double kph = log.killsPerHour();
		return kph < 0 ? null : String.format(Locale.ROOT, "%.0f kills/hr", kph);
	}
}
