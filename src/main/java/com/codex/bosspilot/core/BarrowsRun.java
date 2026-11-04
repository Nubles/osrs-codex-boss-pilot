package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Where a Barrows run stands, read from the game between fights: the brothers killed, the crypt
 * with the tunnel once the player has found it, the reward potential so far and the tunnel kills
 * that would bring it up to the aim; and, from the player's own chest log, the pieces still
 * missing and the odds of the next chest.
 */
public final class BarrowsRun
{
	private final Set<Barrows.Brother> killed;
	private final int levels;
	private final Barrows.Brother tunnel;
	private final Barrows.Aim aim;
	private final Barrows.Plan plan;
	private final ChestLog log;
	private final List<String> missing;

	public BarrowsRun(Set<Barrows.Brother> killed, int levels, Barrows.Brother tunnel, Barrows.Aim aim, ChestLog log, List<String> missing)
	{
		this.killed = killed.isEmpty() ? EnumSet.noneOf(Barrows.Brother.class) : EnumSet.copyOf(killed);
		this.levels = Math.max(0, levels);
		this.tunnel = tunnel;
		this.aim = aim == null ? Barrows.Aim.OFF : aim;
		this.plan = this.aim == Barrows.Aim.OFF ? null : Barrows.plan(this.levels, this.killed, this.aim);
		this.log = log == null ? new ChestLog() : log;
		this.missing = missing == null ? Collections.emptyList() : missing;
	}

	public Set<Barrows.Brother> getKilled()
	{
		return killed;
	}

	public int getLevels()
	{
		return levels;
	}

	/** The crypt with the tunnel, once known this run, or null. */
	public Barrows.Brother getTunnel()
	{
		return tunnel;
	}

	public Barrows.Aim getAim()
	{
		return aim;
	}

	/** The tunnel kills to the aim, or null when the aim is off. */
	public Barrows.Plan getPlan()
	{
		return plan;
	}

	public ChestLog getLog()
	{
		return log;
	}

	/** The pieces not seen in the bank, pack or chests. */
	public List<String> getMissing()
	{
		return missing;
	}

	public int potential()
	{
		return Barrows.potential(levels, killed.size());
	}

	/** Whether anything has happened on this run yet. */
	public boolean started()
	{
		return !killed.isEmpty() || levels > 0;
	}

	/** "3 of 6 down: Ahrim, Karil, Verac", short enough for the plaque. */
	public String brothersShort()
	{
		if (killed.size() == Barrows.Brother.values().length)
		{
			return "All six brothers down";
		}
		return killed.isEmpty() ? brothersText() : brothersText().replace(" brothers:", " down:");
	}

	/** "4 of 6 brothers: Ahrim, Dharok, Karil, Verac". */
	public String brothersText()
	{
		if (killed.isEmpty())
		{
			return "No brothers killed yet this run";
		}
		List<String> names = new ArrayList<>();
		for (Barrows.Brother b : killed)
		{
			names.add(b.getShortName());
		}
		return killed.size() + " of 6 brothers: " + String.join(", ", names);
	}

	/** "Tunnel: Guthan's crypt", or the hint while it is not known. */
	public String tunnelText()
	{
		if (tunnel == null)
		{
			return "Tunnel not found yet this run";
		}
		return "Tunnel: " + tunnel.getShortName() + "'s crypt" + (killed.contains(tunnel) ? "" : ", go there last");
	}

	/** "Potential 742 of 1012". */
	public String potentialText()
	{
		return "Potential " + potential() + " of " + Barrows.MAX_POTENTIAL;
	}

	/** What the plan says, in a line, or null when the aim is off. */
	public String planText()
	{
		if (plan == null)
		{
			return null;
		}
		if (plan.reached())
		{
			return plan.getProjected() > plan.getAim()
				? "Past " + plan.getAim() + " for " + aim.reward() + ": no more tunnel kills"
				: "Reaches " + plan.getAim() + " for " + aim.reward() + " with no more tunnel kills";
		}
		if (plan.getKills().isEmpty())
		{
			return "No tunnel kill fits under " + plan.getAim();
		}
		return "For " + aim.reward() + ": " + plan.killsText();
	}

	/** The chance the next chest, with the brothers killed so far (or all six before any), brings a missing piece. */
	public double nextNew()
	{
		int n = killed.isEmpty() ? Barrows.Brother.values().length : killed.size();
		int m = 0;
		for (Barrows.Brother b : (killed.isEmpty() ? EnumSet.allOf(Barrows.Brother.class) : killed))
		{
			for (String p : b.getPieces())
			{
				if (missing.contains(p))
				{
					m++;
				}
			}
		}
		return Barrows.chanceNew(n, m);
	}

	/** "7 chests since your last piece; a dry spell this long happens 61% of the time", or null with no streak. */
	public String dryText()
	{
		int streak = log.dryStreak();
		if (streak == 0)
		{
			return null;
		}
		double odds = log.dryOdds();
		return Format.plural(streak, "chest", "chests") + " since your last piece; a dry spell this long happens "
			+ percent(odds) + " of the time";
	}

	static String percent(double share)
	{
		double p = share * 100;
		if (p >= 10 || p == 0)
		{
			return Math.round(p) + "%";
		}
		return String.format(Locale.ROOT, p >= 1 ? "%.1f%%" : "%.2f%%", p);
	}
}
