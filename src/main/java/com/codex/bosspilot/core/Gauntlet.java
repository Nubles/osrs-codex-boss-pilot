package com.codex.bosspilot.core;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The facts of the Gauntlet and the Corrupted Gauntlet that Boss Pilot logs runs with: the
 * game's own varbits, NPC and item ids (RuneLite gameval), the game's own chat lines, the
 * preparation limits and the four rare rewards with their rates in each mode (OSRS Wiki). Every
 * number is sourced beside it. Nothing here is about what the Hunllef does during the fight.
 */
public final class Gauntlet
{
	/** RuneLite gameval VarbitID.PLAYER_IN_GAUNTLET. */
	public static final int VARBIT_IN_GAUNTLET = 9178;
	/** RuneLite gameval VarbitID.GAUNTLET_BOSS_STARTED. */
	public static final int VARBIT_BOSS_STARTED = 9177;
	/** RuneLite gameval VarbitID.GAUNTLET_CORRUPTED. */
	public static final int VARBIT_CORRUPTED = 9292;

	/** RuneLite's Discord plugin: MG_GAUNTLET("The Gauntlet", ..., 12127, 7512); 12127 is the lobby by Prifddinas. */
	public static final int LOBBY_REGION = 12127;
	/** RuneLite's Discord plugin: MG_GAUNTLET's instance region, read through WorldPoint.fromLocalInstance. */
	public static final int REGULAR_REGION = 7512;
	/** RuneLite's Discord plugin: MG_CORRUPTED_GAUNTLET("Corrupted Gauntlet", ..., 7768). */
	public static final int CORRUPTED_REGION = 7768;

	/** Crystalline Hunllef, RuneLite gameval NpcID.CRYSTAL_HUNLLEF_MELEE/RANGED/MAGIC/DEATH (wiki: Monster ID "9021,9022,9023,9024"). */
	static final int[] HUNLLEF = {9021, 9022, 9023, 9024};
	/** Corrupted Hunllef, NpcID.CRYSTAL_HUNLLEF_*_HM (wiki: Monster ID "9035,9036,9037,9038"). */
	static final int[] CORRUPTED_HUNLLEF = {9035, 9036, 9037, 9038};

	/** The Gauntlet: "The player has 10 minutes to prepare to fight this boss". */
	public static final int REGULAR_PREP_SECONDS = 10 * 60;
	/** The Gauntlet: "The player only has 7 minutes and 30 seconds to prepare compared to the normal version at 10 minutes." */
	public static final int CORRUPTED_PREP_SECONDS = 7 * 60 + 30;

	/** Youngllef: "You have a funny feeling like you're being followed." / "You feel something weird sneaking into your backpack." */
	static final String PET_FOLLOW = "You have a funny feeling like you're being followed";
	static final String PET_BACKPACK = "You feel something weird sneaking into your backpack";

	/**
	 * The game's run time, RuneLite's chat commands: "Challenge duration: 10:24. Personal best: 7:59."
	 * and "Challenge duration: 10:24 (new personal best).", with "Corrupted challenge duration:" too.
	 */
	private static final Pattern DURATION = Pattern.compile("(?i)^(Corrupted )?challenge duration: ?([0-9:.]+?)\\.?(?: |\\(|$)");
	/** The game's preparation time: the line starts "Preparation time" (unconfirmed beyond that, see the PR). */
	private static final Pattern PREPARATION = Pattern.compile("(?i)^Preparation time: ?([0-9:.]+?)\\.?(?: |$)");

	/** Regular or corrupted. */
	public enum Mode
	{
		REGULAR("Gauntlet", "Regular", REGULAR_PREP_SECONDS),
		CORRUPTED("Corrupted Gauntlet", "Corrupted", CORRUPTED_PREP_SECONDS);

		private final String kcName;
		private final String label;
		private final int prepLimit;

		Mode(String kcName, String label, int prepLimit)
		{
			this.kcName = kcName;
			this.label = label;
			this.prepLimit = prepLimit;
		}

		/** The name in the game's completion count line. */
		public String getKcName()
		{
			return kcName;
		}

		public String getLabel()
		{
			return label;
		}

		/** The preparation limit in seconds. */
		public int getPrepLimit()
		{
			return prepLimit;
		}

		/** The mode a completion count name is ("Gauntlet", "Corrupted Gauntlet"), or null. */
		public static Mode byKc(String name)
		{
			if (name == null)
			{
				return null;
			}
			for (Mode m : values())
			{
				if (m.kcName.equalsIgnoreCase(name.trim()))
				{
					return m;
				}
			}
			return null;
		}
	}

	/**
	 * The four rare rewards, with each one's chance a completed run in the regular and the
	 * corrupted Gauntlet (Reward Chest (The Gauntlet): regular "1/120", "1/120", "1/2,000",
	 * "1/2,000"; corrupted "1/50", "1/50", "1/400", "1/800") and their item ids (RuneLite gameval
	 * ItemID.PRIF_WEAPON_SEED_ENHANCED, PRIF_ARMOUR_SEED, CRYSTAL_SEED_OLD "Crystal weapon seed",
	 * GAUNTLETPET and GAUNTLETPET_CORRUPT).
	 */
	public enum Unique
	{
		ENHANCED("Enhanced crystal weapon seed", "Enhanced seed", new int[]{25859}, 1 / 2000.0, 1 / 400.0),
		ARMOUR("Crystal armour seed", "Armour seed", new int[]{23956}, 1 / 120.0, 1 / 50.0),
		WEAPON("Crystal weapon seed", "Weapon seed", new int[]{4207}, 1 / 120.0, 1 / 50.0),
		PET("Youngllef", "Youngllef", new int[]{23757, 23759}, 1 / 2000.0, 1 / 800.0);

		private final String name;
		private final String shortName;
		private final int[] itemIds;
		private final double regular;
		private final double corrupted;

		Unique(String name, String shortName, int[] itemIds, double regular, double corrupted)
		{
			this.name = name;
			this.shortName = shortName;
			this.itemIds = itemIds;
			this.regular = regular;
			this.corrupted = corrupted;
		}

		public String getName()
		{
			return name;
		}

		public String getShortName()
		{
			return shortName;
		}

		public int[] getItemIds()
		{
			return itemIds.clone();
		}

		/** The chance a completed run in this mode. */
		public double chance(Mode mode)
		{
			return mode == Mode.CORRUPTED ? corrupted : regular;
		}

		/** "1/2,000 or 1/400 corrupted". */
		public String rateText()
		{
			return Zalcano.oneIn(regular).replace("1 in ", "1/") + ", corrupted " + Zalcano.oneIn(corrupted).replace("1 in ", "1/");
		}

		/** The reward an item name is ("Corrupted Youngllef" is the pet too), or null. */
		public static Unique byName(String itemName)
		{
			if (itemName == null)
			{
				return null;
			}
			String n = itemName.trim();
			if (n.equalsIgnoreCase("Corrupted Youngllef"))
			{
				return PET;
			}
			for (Unique u : values())
			{
				if (u.name.equalsIgnoreCase(n))
				{
					return u;
				}
			}
			return null;
		}

		/** The reward an item id is, or null. */
		public static Unique byId(int id)
		{
			for (Unique u : values())
			{
				for (int i : u.itemIds)
				{
					if (i == id)
					{
						return u;
					}
				}
			}
			return null;
		}
	}

	private Gauntlet()
	{
	}

	/** Whether an NPC id is the Hunllef, and which: null when it is neither. */
	public static Mode hunllef(int npcId)
	{
		for (int id : HUNLLEF)
		{
			if (id == npcId)
			{
				return Mode.REGULAR;
			}
		}
		for (int id : CORRUPTED_HUNLLEF)
		{
			if (id == npcId)
			{
				return Mode.CORRUPTED;
			}
		}
		return null;
	}

	/** Whether a game message says the pet has come. */
	public static boolean petMessage(String message)
	{
		return message != null && (message.contains(PET_FOLLOW) || message.contains(PET_BACKPACK));
	}

	/** A run time line from the game, read. */
	public static final class Time
	{
		/** The corrupted line, the regular one, or null for the preparation line. */
		public final Mode mode;
		public final double seconds;

		Time(Mode mode, double seconds)
		{
			this.mode = mode;
			this.seconds = seconds;
		}
	}

	/** The whole run's time from "Challenge duration: 10:24. ...", or null. */
	public static Time duration(String message)
	{
		if (message == null)
		{
			return null;
		}
		Matcher m = DURATION.matcher(message.trim());
		if (!m.find())
		{
			return null;
		}
		double s = Chat.seconds(m.group(2));
		return s <= 0 ? null : new Time(m.group(1) != null ? Mode.CORRUPTED : Mode.REGULAR, s);
	}

	/** The preparation time from a line starting "Preparation time: 5:58", in seconds, or 0. */
	public static double preparation(String message)
	{
		if (message == null)
		{
			return 0;
		}
		Matcher m = PREPARATION.matcher(message.trim());
		return m.find() ? Chat.seconds(m.group(1)) : 0;
	}

	/** The rewards among these item names. */
	public static Set<Unique> uniques(Collection<String> names)
	{
		Set<Unique> out = EnumSet.noneOf(Unique.class);
		if (names != null)
		{
			for (String n : names)
			{
				Unique u = Unique.byName(n);
				if (u != null)
				{
					out.add(u);
				}
			}
		}
		return out;
	}

	/** The chance of at least one in these completed runs of each mode. */
	public static double atLeastOne(Unique u, int regular, int corrupted)
	{
		return 1 - Math.pow(1 - u.chance(Mode.REGULAR), Math.max(0, regular)) * Math.pow(1 - u.chance(Mode.CORRUPTED), Math.max(0, corrupted));
	}

	/** How many of this reward the runs would bring on average: 1 is a run as long as its rate. */
	public static double expected(Unique u, int regular, int corrupted)
	{
		return Math.max(0, regular) * u.chance(Mode.REGULAR) + Math.max(0, corrupted) * u.chance(Mode.CORRUPTED);
	}

	/** "+0:12" or "-0:31" for a time against the best. */
	public static String delta(double seconds)
	{
		long r = Math.round(Math.abs(seconds));
		return (seconds < 0 ? "-" : "+") + Format.clock(r);
	}

	/** "4:07" for a time in seconds, or "-" when it is not known. */
	public static String clock(double seconds)
	{
		return seconds <= 0 ? "-" : Format.clock(Math.round(seconds));
	}

	static String percent(double p)
	{
		return Zalcano.percent(p);
	}

	/** Seconds from game ticks. */
	public static double ticks(int ticks)
	{
		return ticks * 0.6;
	}

	static String fixed(double v)
	{
		return String.format(Locale.ROOT, "%.1f", v);
	}
}
