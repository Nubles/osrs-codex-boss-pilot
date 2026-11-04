package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The Moons of Peril as the panel and plaque show them, all from between fights: the moons
 * subdued on the run under way and what it has used, the player's own Lunar Chest log, and the set
 * pieces still missing with the odds of each moon's set and how dry the player is on it.
 */
public final class LunarRun
{
	private final Set<Moons.Moon> subdued;
	private final Map<String, Long> used;
	private final LunarLog log;
	private final List<String> missing;

	public LunarRun(Set<Moons.Moon> subdued, Map<String, Long> used, LunarLog log, List<String> missing)
	{
		this.subdued = subdued == null || subdued.isEmpty() ? EnumSet.noneOf(Moons.Moon.class) : EnumSet.copyOf(subdued);
		this.used = used == null ? Collections.emptyMap() : used;
		this.log = log == null ? new LunarLog() : log;
		this.missing = missing == null ? Moons.missing(Collections.emptyList()) : missing;
	}

	public Set<Moons.Moon> getSubdued()
	{
		return subdued;
	}

	/** What the run under way has used so far, by name. */
	public Map<String, Long> getUsed()
	{
		return used;
	}

	public LunarLog getLog()
	{
		return log;
	}

	/** The pieces not seen in the bank, pack or chests, in set order. */
	public List<String> getMissing()
	{
		return missing;
	}

	public boolean owns(String piece)
	{
		return !missing.contains(piece);
	}

	/** Pieces owned of the twelve. */
	public int owned()
	{
		return Moons.Moon.values().length * 4 - missing.size();
	}

	/** "2 of 3 moons: Blood, Blue", or the hint while none is down. */
	public String moonsText()
	{
		if (subdued.isEmpty())
		{
			return "No moon subdued yet this run";
		}
		if (subdued.size() == 3)
		{
			return "All three moons subdued";
		}
		List<String> names = new ArrayList<>();
		for (Moons.Moon m : subdued)
		{
			names.add(m.getShortName());
		}
		return subdued.size() + " of 3 moons: " + String.join(", ", names);
	}

	/** What the chest would roll now: "6 rolls, 1 in 19 for a piece", or null with no moon down. */
	public String chestText()
	{
		if (subdued.isEmpty())
		{
			return null;
		}
		double newPiece = Moons.chanceNew(subdued, missing);
		String rolls = Format.plural(Moons.rolls(subdued.size()), "roll", "rolls");
		if (missing.isEmpty() || newPiece <= 0)
		{
			return "Chest now: " + rolls + ", " + Moons.oneIn(Moons.chanceAny(subdued.size())) + " for a piece";
		}
		return "Chest now: " + rolls + ", " + Moons.oneIn(newPiece) + " for a new piece";
	}

	/** "4 cooked bream, 6 doses of moonlight potion" used this run, or null when nothing was. */
	public String usedText()
	{
		return usedText(used, 1, 3);
	}

	/** The supplies a run uses on average, or null before any run was watched. */
	public String usedPerRunText()
	{
		Map<String, Double> per = log.usedPerRun();
		if (per.isEmpty())
		{
			return null;
		}
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, Double> e : per.entrySet())
		{
			if (parts.size() == 3)
			{
				break;
			}
			parts.add(amount(e.getValue(), e.getKey()));
		}
		return "A run uses about " + String.join(", ", parts) + " (over " + Format.plural(log.suppliedRuns, "run", "runs") + ")";
	}

	static String usedText(Map<String, Long> used, int scale, int most)
	{
		if (used.isEmpty())
		{
			return null;
		}
		List<String> parts = new ArrayList<>();
		used.entrySet().stream()
			.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
			.limit(most)
			.forEach(e -> parts.add(amount(e.getValue() / (double) scale, e.getKey())));
		return "Used this run: " + String.join(", ", parts);
	}

	/** "4 cooked bream" or "6.5 doses of moonlight potion": potions are counted by the dose under their name. */
	static String amount(double n, String name)
	{
		String num = Math.abs(n - Math.round(n)) < 0.05 ? String.valueOf(Math.round(n)) : String.format(Locale.ROOT, "%.1f", n);
		String lower = Character.toLowerCase(name.charAt(0)) + name.substring(1);
		if (Food.heal(name) <= 0 && Moons.supply(name + "(4)"))
		{
			return num + ("1".equals(num) ? " dose of " : " doses of ") + lower;
		}
		return num + " " + lower;
	}

	/**
	 * How dry the player is on a moon's set: "48 Blood Moon kills without a piece: 58% would have one
	 * by now", or null before any of its kills was logged.
	 */
	public String dryText(Moons.Moon m)
	{
		int kills = log.kills(m);
		if (kills == 0)
		{
			return null;
		}
		int n = log.since(m);
		if (n == 0)
		{
			return "Its last kill brought a piece";
		}
		boolean had = false;
		for (Moons.Piece p : m.getPieces())
		{
			had |= log.found(p.getName()) > 0;
		}
		return Format.plural(n, "kill", "kills") + (had ? " since its last piece" : " without a piece") + ": "
			+ Moons.percent(1 - Moons.dry(n)) + " would have one by now";
	}

	/** How far the dry spell on a moon's set is past its rate, 1 at the rate. */
	public double spell(Moons.Moon m)
	{
		return log.since(m) * Moons.SET_CHANCE;
	}

	/** The plaque line on a trip: the moons down this run. */
	public String plaqueText()
	{
		return moonsText();
	}

	/** The pace on the plaque: "312k/hr", or null before a timed run. */
	public String paceText()
	{
		double v = log.valuePerHour();
		return v < 0 ? null : Format.coins(Math.round(v)) + "/hr";
	}
}
