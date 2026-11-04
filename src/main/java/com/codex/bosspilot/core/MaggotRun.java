package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Maggot King as the panel and plaque show it: the player's own kill log and trips, the five
 * collection log drops with the chance of having had each by now, what the account has of the
 * stats the wiki's money-making guides recommend, and how long a chosen drop should take at the
 * player's own pace. Nothing here reads or reacts to a fight.
 */
public final class MaggotRun
{
	/** A stat a wiki guide recommends, against the account. */
	public static final class Stat
	{
		private final String skill;
		private final int level;
		private final int have;

		Stat(String skill, int level, int have)
		{
			this.skill = skill;
			this.level = level;
			this.have = have;
		}

		public String getSkill()
		{
			return skill;
		}

		public int getLevel()
		{
			return level;
		}

		/** The account's level, or 0 when not known. */
		public int getHave()
		{
			return have;
		}

		public boolean isMet()
		{
			return have >= level;
		}
	}

	/** One of the wiki's two money-making methods, and its recommended stats. */
	public static final class Method
	{
		private final String name;
		private final int killsPerHour;
		private final List<Stat> stats;

		Method(String name, int killsPerHour, List<Stat> stats)
		{
			this.name = name;
			this.killsPerHour = killsPerHour;
			this.stats = stats;
		}

		public String getName()
		{
			return name;
		}

		/** The kills an hour the guide assumes "with max gear and combat stats". */
		public int getKillsPerHour()
		{
			return killsPerHour;
		}

		public List<Stat> getStats()
		{
			return stats;
		}

		public int met()
		{
			int n = 0;
			for (Stat s : stats)
			{
				n += s.isMet() ? 1 : 0;
			}
			return n;
		}
	}

	/**
	 * Money making guide/Killing the Maggot King: "Attack 90+ , Strength 90+ , recommended", "Magic
	 * 95+", "Prayer 77+ recommended", "assumes 25 kills per hour". Money making guide/Killing the
	 * Maggot King (Ranged): "Attack 80+ , Strength 85+ , recommended", "Ranged 80+", "Prayer 74+
	 * recommended", "assumes 20 kills per hour". Both: "Crafting 84 to etch the Elder venator fang".
	 * Maggot King: the woods' two agility shortcuts "require level 64 Agility"; the bank chest
	 * "requires level 70 Construction (boosts allowed)".
	 */
	static final String[][] MELEE_MAGIC = {{"Attack", "90"}, {"Strength", "90"}, {"Magic", "95"}, {"Prayer", "77"}};
	static final String[][] MELEE_RANGED = {{"Attack", "80"}, {"Strength", "85"}, {"Ranged", "80"}, {"Prayer", "74"}};
	public static final int ETCH_CRAFTING = 84;
	public static final int SHORTCUT_AGILITY = 64;
	public static final int CHEST_CONSTRUCTION = 70;

	private final MaggotLog log;
	private final Set<MaggotKing.Unique> owned;
	private final Account account;
	private final Logbook.Rates rates;
	private final List<Logbook.TripRecord> trips;
	private final MaggotKing.Unique goal;

	public MaggotRun(MaggotLog log, Collection<String> ownedNames, Account account, Logbook.Rates rates,
		List<Logbook.TripRecord> trips, MaggotKing.Unique goal)
	{
		this.log = log == null ? new MaggotLog() : log;
		Set<MaggotKing.Unique> o = EnumSet.noneOf(MaggotKing.Unique.class);
		o.addAll(MaggotKing.uniques(ownedNames));
		for (MaggotKing.Unique u : MaggotKing.Unique.values())
		{
			if (this.log.peek(u).found > 0)
			{
				o.add(u);
			}
		}
		this.owned = o;
		this.account = account == null ? Account.UNKNOWN : account;
		this.rates = rates;
		this.trips = trips == null ? Collections.emptyList() : trips;
		this.goal = goal == null ? MaggotKing.Unique.ELDER_FANG : goal;
	}

	public MaggotLog getLog()
	{
		return log;
	}

	public MaggotKing.Unique getGoal()
	{
		return goal;
	}

	/** Whether the player has this drop: received while logged, or seen in the bank or pack. */
	public boolean owns(MaggotKing.Unique u)
	{
		return owned.contains(u);
	}

	public int ownedCount()
	{
		return owned.size();
	}

	/** The account's trips here, newest first, at most this many. */
	public List<Logbook.TripRecord> recentTrips(int max)
	{
		List<Logbook.TripRecord> out = new ArrayList<>(trips);
		Collections.reverse(out);
		return out.subList(0, Math.min(max, out.size()));
	}

	/** Food (hitpoints) and prayer doses a kill over the account's trips here, or null before any. */
	public String suppliesText()
	{
		if (rates == null || !rates.isKnown() || rates.kills <= 0)
		{
			return null;
		}
		return String.format(Locale.ROOT, "%.0f hp of food and %.1f prayer doses a kill over your %s", rates.foodPerKill(),
			rates.dosesPerKill(), Format.plural(rates.trips, "trip", "trips"));
	}

	/** Deaths at the boss, every trip. */
	public int deaths()
	{
		return log.deaths;
	}

	/** Kills an hour at the player's pace: back to back on a trip, or over whole trips; -1 before any. */
	public double killsPerHour()
	{
		double kph = log.killsPerHour();
		if (kph > 0)
		{
			return kph;
		}
		return rates != null && rates.isKnown() && rates.killsPerHour() > 0 ? rates.killsPerHour() : -1;
	}

	/** The chance the player has had this drop by now, over the loots since their last: 0 before any. */
	public double hadByNow(MaggotKing.Unique u)
	{
		return 1 - Math.exp(log.peek(u).missLn);
	}

	/**
	 * How the dry spell on a drop compares: "312 stomachs since your last: 60% would have one by
	 * now", or null before any loot that could bring it.
	 */
	public String dryText(MaggotKing.Unique u)
	{
		MaggotLog.Odds o = log.peek(u);
		if (o.since == 0)
		{
			return o.found > 0 ? "Your last loot that could bring one did" : null;
		}
		String loots = Format.plural(o.since, u == MaggotKing.Unique.PET ? "loot" : "stomach", u == MaggotKing.Unique.PET ? "loots" : "stomachs")
			+ (o.found > 0 ? " since your last" : " without one");
		return loots + ": " + MaggotKing.percent(hadByNow(u)) + " of players would have one by now";
	}

	/** "1 of 2.4 expected": how many came against how many the rates give over every loot logged. */
	public String luckText(MaggotKing.Unique u)
	{
		MaggotLog.Odds o = log.peek(u);
		if (o.expected <= 0)
		{
			return null;
		}
		return Format.number(o.found) + " of " + String.format(Locale.ROOT, o.expected < 10 ? "%.1f" : "%.0f", o.expected) + " expected";
	}

	/** How far the dry spell is: the chance of having had one by now, 0 to 1. */
	public double spell(MaggotKing.Unique u)
	{
		return hadByNow(u);
	}

	/** The chance a kill brings this drop as this player loots (their share of taking eggs, else opening the stomach). */
	public double perKill(MaggotKing.Unique u)
	{
		double share = log.eggShare();
		return MaggotKing.perKill(u, share < 0 ? 0 : share);
	}

	/** The kills for half and for nine in ten players to have the goal, as this player loots. */
	public int[] goalKills()
	{
		double p = perKill(goal);
		return new int[]{MaggotKing.killsFor(p, 0.5), MaggotKing.killsFor(p, 0.9)};
	}

	/**
	 * The time to the goal at the player's own pace, as the panel says it: "About 13 hours for even
	 * odds and 42 hours for 9 in 10, at your 18.5 kills an hour", or why it cannot be said yet.
	 */
	public String etaText()
	{
		double p = perKill(goal);
		if (p <= 0)
		{
			return "Taking eggs never brings the " + goal.getName().toLowerCase(Locale.ROOT) + ". Open the stomach for a chance at it.";
		}
		int[] k = goalKills();
		double kph = killsPerHour();
		if (kph <= 0)
		{
			return "Even odds within " + Format.number(k[0]) + " kills, 9 in 10 within " + Format.number(k[1])
				+ ". Kill a few back to back and Boss Pilot turns that into hours at your own pace.";
		}
		return "Even odds in about " + hours(k[0] / kph) + ", 9 in 10 in about " + hours(k[1] / kph)
			+ String.format(Locale.ROOT, ", at your %.1f kills an hour.", kph);
	}

	/** The goal's chance a kill as this player loots, for the panel: "1 in 340 a kill as you loot". */
	public String goalRateText()
	{
		double share = log.eggShare();
		String how = share < 0 ? "opening the stomach" : share == 0 ? "as you loot (always the stomach)"
			: share == 1 ? "as you loot (always eggs)" : "as you loot (" + MaggotKing.percent(share) + " eggs)";
		return MaggotKing.oneIn(perKill(goal)) + " a kill " + how;
	}

	static String hours(double h)
	{
		if (h < 1)
		{
			return Math.max(1, Math.round(h * 60)) + " minutes";
		}
		if (h < 10)
		{
			return String.format(Locale.ROOT, "%.1f hours", h);
		}
		return Format.number(Math.round(h)) + " hours";
	}

	/** The wiki's two money-making methods and the account's levels against their stats. */
	public List<Method> methods()
	{
		List<Method> out = new ArrayList<>();
		out.add(new Method("Melee and magic", 25, stats(MELEE_MAGIC)));
		out.add(new Method("Melee and ranged", 20, stats(MELEE_RANGED)));
		return out;
	}

	/** Unlocks along the way that help, with the account's level. */
	public List<Stat> extras()
	{
		List<Stat> out = new ArrayList<>();
		out.add(new Stat("Agility", SHORTCUT_AGILITY, account.level("Agility")));
		out.add(new Stat("Construction", CHEST_CONSTRUCTION, account.level("Construction")));
		out.add(new Stat("Crafting", ETCH_CRAFTING, account.level("Crafting")));
		return out;
	}

	public boolean isAccountKnown()
	{
		return account.isKnown();
	}

	private List<Stat> stats(String[][] table)
	{
		List<Stat> out = new ArrayList<>();
		for (String[] s : table)
		{
			out.add(new Stat(s[0], Integer.parseInt(s[1]), account.level(s[0])));
		}
		return out;
	}

	/** The plaque line on a trip: "214 loots since a rare drop", or the first kill. */
	public String plaqueText()
	{
		if (log.isEmpty())
		{
			return "Your first logged kill is next";
		}
		MaggotLog.Kill last = log.last();
		if (last != null && !last.uniques.isEmpty())
		{
			return "Your last kill brought " + MaggotKing.Unique.byKey(last.uniques.get(0)).getName() + "!";
		}
		MaggotLog.Odds g = log.peek(goal);
		if (g.since == 0)
		{
			return perKill(goal) <= 0 ? goal.getShortName() + ": only from the stomach"
				: goal.getShortName() + ": " + MaggotKing.oneIn(perKill(goal)) + " a kill";
		}
		return goal.getShortName() + ": " + Format.number(g.since) + " dry, " + MaggotKing.percent(hadByNow(goal)) + " would have one";
	}

	/** The pace on the plaque: "18 kills/hr", or null before a timed kill. */
	public String paceText()
	{
		double kph = log.killsPerHour();
		return kph < 0 ? null : String.format(Locale.ROOT, "%.0f kills/hr", kph);
	}
}
