package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Puts the bosses in the order to do them: the one pinned first, then every boss that is open
 * now by the player's aim, then those a task or a bit of kit away, and the locked ones last with
 * what is in the way. Every pick says in a sentence why it is where it is.
 */
public final class Ranker
{
	/** One boss, placed. */
	public static final class Pick
	{
		private final Boss boss;
		private final Readiness readiness;
		private final KitCheck kit;
		private final double killSeconds;
		private final double score;
		private final String why;
		private final boolean pinned;

		Pick(Boss boss, Readiness readiness, KitCheck kit, double killSeconds, double score, String why, boolean pinned)
		{
			this.boss = boss;
			this.readiness = readiness;
			this.kit = kit;
			this.killSeconds = killSeconds;
			this.score = score;
			this.why = why;
			this.pinned = pinned;
		}

		public Boss getBoss()
		{
			return boss;
		}

		public Readiness getReadiness()
		{
			return readiness;
		}

		public Readiness.Status getStatus()
		{
			return readiness.getStatus();
		}

		public KitCheck getKit()
		{
			return kit;
		}

		/** Seconds a kill takes with the best owned setup, or -1 when not worked out. */
		public double getKillSeconds()
		{
			return killSeconds;
		}

		public double getScore()
		{
			return score;
		}

		public String getWhy()
		{
			return why;
		}

		public boolean isPinned()
		{
			return pinned;
		}
	}

	/** What the ranking needs to know, besides the bosses themselves. */
	public static final class Inputs
	{
		public Account account = Account.UNKNOWN;
		public Stock bank = Stock.EMPTY;
		public Stock carried = Stock.EMPTY;
		public Logbook logbook = new Logbook();
		/** Seconds a kill takes with the best owned setup, by boss key. */
		public Map<String, Double> killSeconds = Collections.emptyMap();
		public Aim aim = Aim.PROGRESS;
		public String pinned;
		public boolean wilderness;
	}

	private Ranker()
	{
	}

	public static List<Pick> rank(List<Boss> bosses, Inputs in)
	{
		List<Pick> picks = new ArrayList<>();
		for (Boss boss : bosses)
		{
			if (boss.isWilderness() && !in.wilderness && !boss.getKey().equals(in.pinned))
			{
				continue;
			}
			KitCheck kit = KitCheck.of(boss, in.bank, in.carried, in.logbook.visited(boss));
			Readiness r = Readiness.of(boss, in.account, kit);
			double secs = in.killSeconds.getOrDefault(boss.getKey(), -1.0);
			boolean pinned = boss.getKey().equals(in.pinned);
			picks.add(new Pick(boss, r, kit, secs, score(boss, r, kit, secs, in), why(boss, r, kit, secs, in), pinned));
		}
		picks.sort(Comparator.comparing((Pick p) -> !p.pinned).thenComparing(Comparator.comparingDouble((Pick p) -> p.score).reversed()));
		return picks;
	}

	/** The first open boss, or the first of all when none is open, or null when there are none. */
	public static Pick next(List<Pick> picks)
	{
		for (Pick p : picks)
		{
			if (p.isPinned() || p.getStatus().isOpen())
			{
				return p;
			}
		}
		return picks.isEmpty() ? null : picks.get(0);
	}

	static double base(Readiness.Status s)
	{
		switch (s)
		{
			case READY:
				return 4000;
			case STRETCH:
				return 3000;
			case NEEDS_KIT:
				return 2000;
			case NEEDS_TASK:
				return 1000;
			default:
				return 0;
		}
	}

	static double score(Boss boss, Readiness r, KitCheck kit, double secs, Inputs in)
	{
		double s = base(r.getStatus());
		int kc = in.logbook.kc(boss);
		Logbook.Rates rates = in.logbook.rates(boss);
		switch (in.aim)
		{
			case LOOT:
				// Known paying bosses first, by their loot an hour; then the rest by progress.
				if (rates.isKnown())
				{
					s += 500 + Math.min(499, rates.lootPerHour() / 10_000.0);
				}
				else
				{
					s += boss.getSuggestedCombat();
				}
				break;
			case QUICK:
				s += secs > 0 ? 900 - Math.min(899, secs) : 0;
				break;
			default:
				s += boss.getSuggestedCombat() * 4;
				if (kc <= 0 && !in.logbook.visited(boss))
				{
					// A boss never killed moves the account on more than one farmed already.
					s += 120;
				}
				else if (kc > 0 && kc < 50)
				{
					s += 40;
				}
				break;
		}
		if (r.getStatus() == Readiness.Status.LOCKED)
		{
			// Closer locks first: one thing missing beats three.
			s -= r.getMissing().size() * 50;
		}
		if (kit.isOwned())
		{
			s += 10;
		}
		return s;
	}

	static String why(Boss boss, Readiness r, KitCheck kit, double secs, Inputs in)
	{
		switch (r.getStatus())
		{
			case LOCKED:
				return "Needs " + String.join(", ", r.getMissing()) + ".";
			case NEEDS_TASK:
				return "Open on " + r.getMissing().get(0).replaceFirst("^A ", "a ") + ".";
			case NEEDS_KIT:
				return "Get " + String.join(", ", r.getMissing()) + " first.";
			default:
				break;
		}
		StringBuilder out = new StringBuilder();
		int kc = in.logbook.kc(boss);
		Logbook.Rates rates = in.logbook.rates(boss);
		if (in.aim == Aim.LOOT && rates.isKnown())
		{
			out.append("Your trips here make about ").append(Format.number(rates.lootPerHour())).append(" an hour.");
		}
		else if (kc <= 0 && !in.logbook.visited(boss))
		{
			out.append(r.getStatus() == Readiness.Status.STRETCH ? "Open to you, though a step up." : "You meet every requirement and have not killed it yet.");
		}
		else if (kc > 0)
		{
			out.append(Format.plural(kc, "kill", "kills")).append(" so far.");
		}
		else
		{
			out.append("You meet every requirement.");
		}
		if (secs > 0)
		{
			out.append(" Your best gear kills one in about ").append(Format.clock(secs)).append('.');
		}
		List<KitCheck.Line> shortLines = kit.shortSuggested();
		if (!shortLines.isEmpty())
		{
			out.append(" Short on ").append(shortLines.get(0).getSupply().getLabel().toLowerCase()).append('.');
		}
		return out.toString();
	}
}
