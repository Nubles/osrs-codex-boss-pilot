package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The charged items of a trip held against how long the trip should last: for each one, how many
 * kills its charges are good for at the player's pace, and whether that is short of the food and
 * prayer. It says how sure it is: what the game said, how many kills ago, and whether the pace is
 * the player's own or worked out from the setup.
 */
public final class ChargeCheck
{
	public enum Status
	{
		/** Never checked: the game has not said what it holds. */
		UNCHECKED,
		/** Checked, but there is no pace to turn charges into kills yet. */
		NO_PACE,
		/** Nothing left, or not enough for one more kill. */
		EMPTY,
		/** Runs out before the food and prayer would. */
		SHORT,
		/** Lasts as long as the food and prayer, or longer. */
		ENOUGH;

		/** Whether it should be topped up before leaving. */
		public boolean needsRecharge()
		{
			return this == EMPTY || this == SHORT;
		}
	}

	/** Where the pace comes from. */
	public enum PaceFrom
	{
		NONE,
		/** The setup's kill time and attack speed. */
		SETUP,
		/** Hits taken a kill on this trip. */
		TRIP,
		/** The player's own checks before and after kills here. */
		LEARNT
	}

	public static final class Line
	{
		private final Charged item;
		private final ChargeBook.Estimate estimate;
		private final double[] perKill;
		private final PaceFrom from;
		private final double kills;
		private final int limit;
		private final double target;
		private final Status status;

		Line(Charged item, ChargeBook.Estimate estimate, double[] perKill, PaceFrom from, double kills, int limit, double target, Status status)
		{
			this.item = item;
			this.estimate = estimate;
			this.perKill = perKill;
			this.from = from;
			this.kills = kills;
			this.limit = limit;
			this.target = target;
			this.status = status;
		}

		public Charged getItem()
		{
			return item;
		}

		/** Null when never checked. */
		public ChargeBook.Estimate getEstimate()
		{
			return estimate;
		}

		/** Use a kill for each pool, or null. */
		public double[] getPerKill()
		{
			return perKill == null ? null : perKill.clone();
		}

		public PaceFrom getFrom()
		{
			return from;
		}

		/** Kills it is good for, or -1 when not known. */
		public double getKills()
		{
			return kills;
		}

		/** The pool that runs out first, or -1. */
		public int getLimit()
		{
			return limit;
		}

		/** "darts", "charges", or null. */
		public String limitName()
		{
			return limit < 0 ? null : item.getPools().get(limit).getName();
		}

		/** "Toxic blowpipe darts", "Trident of the seas charges". */
		public String what()
		{
			return item.getName() + " " + (limit < 0 ? item.getPools().get(0).getName() : limitName());
		}

		/** Kills the food and prayer are good for, or -1 when not known. */
		public double getTarget()
		{
			return target;
		}

		public Status getStatus()
		{
			return status;
		}

		/** "~2 kills", "~140 kills", "empty", "check it". */
		public String killsText()
		{
			switch (status)
			{
				case UNCHECKED:
					return "check it";
				case EMPTY:
					return kills < 0 || left(limit) <= 0 ? "empty" : "<1 kill";
				case NO_PACE:
					return Format.number(Math.round(left(0))) + " " + item.getPools().get(0).getName();
				default:
					long n = (long) Math.floor(kills);
					return "~" + Format.plural(n, "kill", "kills");
			}
		}

		/** What each pool holds now: "312 darts, 1,920 scales", with a ~ once kills have gone by. */
		public String holdsText()
		{
			if (estimate == null)
			{
				return null;
			}
			List<String> parts = new ArrayList<>();
			double[] left = estimate.getLeft();
			for (int i = 0; i < left.length; i++)
			{
				if (left[i] >= 0)
				{
					parts.add((estimate.isExact() ? "" : "~") + Format.number(Math.round(left[i])) + " " + item.getPools().get(i).getName());
				}
			}
			return String.join(", ", parts);
		}

		/** How sure: "as checked", "checked 3 kills ago, your pace here", "checked 3 kills ago, pace from the setup". */
		public String sureText()
		{
			if (estimate == null)
			{
				return "Not checked yet: use Check on it once and Boss Pilot keeps count";
			}
			String when = estimate.isExact() ? "As the game said" : "Checked " + Format.plural(estimate.getKills(), "kill", "kills") + " ago";
			String pace;
			switch (from)
			{
				case LEARNT:
					pace = "your own pace here";
					break;
				case TRIP:
					pace = "the hits you take this trip";
					break;
				case SETUP:
					pace = "pace from the setup until your checks teach it yours";
					break;
				default:
					pace = null;
			}
			String blind = estimate.getBlind() > 0 ? "; " + Format.plural(estimate.getBlind(), "kill", "kills") + " not counted" : "";
			return when + (pace == null ? "" : ", " + pace) + blind;
		}

		private double left(int pool)
		{
			if (estimate == null)
			{
				return -1;
			}
			double[] left = estimate.getLeft();
			return pool < 0 || pool >= left.length ? -1 : left[pool];
		}
	}

	private ChargeCheck()
	{
	}

	/**
	 * How long each charged item lasts at a boss.
	 *
	 * @param items the charged items to look at
	 * @param book what the game said and the paces learnt
	 * @param bossKey the boss
	 * @param setups the best setups for the boss, for a pace from the kill time
	 * @param hitsPerKill hits the player takes a kill on this trip, or -1
	 * @param combatSeconds seconds in combat a kill, for the serpentine helm, or -1 to use the setups'
	 * @param target kills the food and prayer last, or -1 when not known
	 */
	public static List<Line> of(Collection<Charged> items, ChargeBook book, String bossKey, List<Setup> setups, double hitsPerKill,
		double combatSeconds, double target)
	{
		List<Line> out = new ArrayList<>();
		for (Charged c : items)
		{
			out.add(line(c, book, bossKey, setups, hitsPerKill, combatSeconds, target));
		}
		return Collections.unmodifiableList(out);
	}

	static Line line(Charged c, ChargeBook book, String bossKey, List<Setup> setups, double hitsPerKill, double combatSeconds, double target)
	{
		ChargeBook.Estimate e = book.estimate(c);
		PaceFrom[] from = new PaceFrom[1];
		double[] per = perKill(c, book, bossKey, setups, hitsPerKill, combatSeconds, from);
		if (e == null)
		{
			return new Line(c, null, per, from[0], -1, -1, target, Status.UNCHECKED);
		}
		double[] left = e.getLeft();
		for (int i = 0; i < left.length; i++)
		{
			if (left[i] == 0)
			{
				return new Line(c, e, per, from[0], 0, i, target, Status.EMPTY);
			}
		}
		if (per == null)
		{
			return new Line(c, e, null, PaceFrom.NONE, -1, -1, target, Status.NO_PACE);
		}
		double kills = Double.POSITIVE_INFINITY;
		int limit = -1;
		for (int i = 0; i < left.length; i++)
		{
			if (left[i] >= 0 && per[i] > 0 && left[i] / per[i] < kills)
			{
				kills = left[i] / per[i];
				limit = i;
			}
		}
		if (limit < 0)
		{
			return new Line(c, e, null, PaceFrom.NONE, -1, -1, target, Status.NO_PACE);
		}
		Status s = kills < 1 ? Status.EMPTY : target > 0 && kills < target ? Status.SHORT : Status.ENOUGH;
		return new Line(c, e, per, from[0], kills, limit, target, s);
	}

	/**
	 * Use a kill for each pool: the player's own learnt pace first, then this trip's hits for
	 * crystal armour, then the setups' kill times. Null when none of these is known.
	 */
	public static double[] perKill(Charged c, ChargeBook book, String bossKey, List<Setup> setups, double hitsPerKill, double combatSeconds,
		PaceFrom[] from)
	{
		double[] fromSetup = null;
		PaceFrom how = PaceFrom.NONE;
		if (c.getWear() == Charged.Wear.HIT_TAKEN)
		{
			fromSetup = c.perKillFromHits(hitsPerKill);
			how = fromSetup == null ? PaceFrom.NONE : PaceFrom.TRIP;
		}
		else if (c.getWear() == Charged.Wear.COMBAT_TIME)
		{
			double secs = combatSeconds > 0 ? combatSeconds : averageKill(setups);
			fromSetup = c.perKill(secs, 0, null, Collections.emptyList());
			how = fromSetup == null ? PaceFrom.NONE : PaceFrom.SETUP;
		}
		else
		{
			fromSetup = fromSetups(c, setups);
			how = fromSetup == null ? PaceFrom.NONE : PaceFrom.SETUP;
		}
		double[] learnt = book.pace(bossKey, c);
		if (learnt != null)
		{
			double[] out = new double[learnt.length];
			for (int i = 0; i < out.length; i++)
			{
				out[i] = learnt[i] >= 0 ? learnt[i] : fromSetup == null ? -1 : fromSetup[i];
			}
			from[0] = PaceFrom.LEARNT;
			return out;
		}
		from[0] = how;
		return fromSetup;
	}

	/** An attack item's use a kill, averaged over the setups that use it. */
	static double[] fromSetups(Charged c, List<Setup> setups)
	{
		double[] sum = null;
		int n = 0;
		for (Setup s : setups)
		{
			if (s.getKillSeconds() <= 0 || !Charged.among(s.getGear()).contains(c))
			{
				continue;
			}
			double[] one = c.perKill(s.getKillSeconds(), s.getTicks(), s.getStyle(), s.getGear());
			if (one == null)
			{
				continue;
			}
			if (sum == null)
			{
				sum = new double[one.length];
			}
			for (int i = 0; i < one.length; i++)
			{
				sum[i] += one[i];
			}
			n++;
		}
		if (sum == null)
		{
			return null;
		}
		for (int i = 0; i < sum.length; i++)
		{
			sum[i] /= n;
		}
		return sum;
	}

	private static double averageKill(List<Setup> setups)
	{
		double sum = 0;
		int n = 0;
		for (Setup s : setups)
		{
			if (s.getKillSeconds() > 0)
			{
				sum += s.getKillSeconds();
				n++;
			}
		}
		return n == 0 ? -1 : sum / n;
	}

	/** The line that runs out first among those that need a top-up, or null. */
	public static Line first(List<Line> lines)
	{
		Line out = null;
		for (Line l : lines)
		{
			if (l.getKills() >= 0 && (out == null || l.getKills() < out.getKills()))
			{
				out = l;
			}
		}
		return out;
	}

	/** Every charged item the setups for a boss use, in catalogue order. */
	public static List<Charged> inSetups(List<Setup> setups)
	{
		List<String> names = new ArrayList<>();
		for (Setup s : setups)
		{
			names.addAll(s.getGear());
		}
		return Charged.among(names);
	}

	/**
	 * Kills the kit's food and prayer last at the player's own past pace at this boss, or -1 when
	 * there are no past trips to go by.
	 */
	public static double kitKills(Boss boss, Logbook.Rates past)
	{
		if (past == null || past.kills <= 0)
		{
			return -1;
		}
		double food = 0;
		double doses = 0;
		for (Supply s : boss.getKit())
		{
			if (s.getKind() == Supply.Kind.FOOD)
			{
				food += s.getAmount();
			}
			else if (s.getKind() == Supply.Kind.DOSES && s.getLabel().toLowerCase(java.util.Locale.ROOT).contains("prayer"))
			{
				doses += s.getAmount();
			}
		}
		double byFood = past.foodPerKill() > 0 && food > 0 ? food / past.foodPerKill() : Double.POSITIVE_INFINITY;
		double byDoses = past.dosesPerKill() > 0 && doses > 0 ? doses / past.dosesPerKill() : Double.POSITIVE_INFINITY;
		double kills = Math.min(byFood, byDoses);
		return Double.isInfinite(kills) ? -1 : kills;
	}
}
