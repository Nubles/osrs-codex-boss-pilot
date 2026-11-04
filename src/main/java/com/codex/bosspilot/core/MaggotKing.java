package com.codex.bosspilot.core;

import java.util.Collection;
import java.util.Locale;

/**
 * The facts of the Maggot King that Boss Pilot plans and logs with: the two ways to loot its
 * corpse, the five collection log drops and how often each comes, and the odds sums made from
 * them. Every number is from the OSRS Wiki (Maggot King, read 4 November 2026; its drop table was
 * last changed on 8 July 2026 and its unique rates published by Jagex on 15 July 2026) or
 * RuneLite's gameval; the page is named beside it. Nothing here is about what the boss does
 * during the fight.
 */
public final class MaggotKing
{
	/** RuneLite gameval NpcID.MAGGOT_KING. */
	public static final int NPC = 15742;
	/** RuneLite gameval NpcID.MAGGOT_KING_CORPSE: the corpse looted after a kill. */
	public static final int CORPSE = 15741;
	/** Maggot marquess: "Opening the Maggot King Corpse's stomach". */
	public static final String CORPSE_NAME = "Maggot King Corpse";
	/**
	 * RuneLite gameval VarPlayerID.TOTAL_MAGGOT_KING_KILLS. That it holds the kill count is read
	 * from its name only; Boss Pilot takes a rise in it as a kill, the same as the chat line.
	 */
	public static final int KILLS_VARP = 5614;
	/** Maggot King: "The eggs pop as you try to take them." (the take-eggs roll that gives nothing). */
	public static final String EGGS_NOTHING = "The eggs pop as you try to take them.";
	/** RuneLite's loot tracker: the menu option on a maggot egg that pops it. */
	public static final String POP = "Pop";
	/** The maggot eggs, RuneLite gameval ItemID MAGGOT_EGG, SICKLY_, WARM_, PULSATING_, WRIGGLING_, WRITHING_MAGGOT_EGG. */
	static final int[] EGGS = {33665, 33667, 33669, 33671, 33673, 33675};
	/** Maggot marquess: "You have a funny feeling like you're being followed." or "You feel something weird sneaking into your backpack." */
	static final String PET_FOLLOW = "You have a funny feeling like you're being followed";
	static final String PET_BACKPACK = "You feel something weird sneaking into your backpack";
	/**
	 * Maggot King, drops (open-stomach): "The chance of receiving any unique item is 1/205.6", that
	 * is the elder venator fang (1/340) and the crimson kisten (1/520) rolled before the main table.
	 */
	static final double PRE_ROLL = 1 / 340.0 + 1 / 520.0;
	/** The main table's 159 shares; venator fang and venator tooth are 2/159 each (Drop Rate Project estimate). */
	static final double MAIN_SHARE = 2 / 159.0;

	/** The two options on the corpse. Maggot King: "Open-stomach" and "Take-eggs". */
	public enum Choice
	{
		/** "grants a roll on the normal drop table". */
		STOMACH("Open-stomach"),
		/** "sacrifices normal drops for an increased chance to receive the pet". */
		EGGS("Take-eggs");

		private final String option;

		Choice(String option)
		{
			this.option = option;
		}

		/** The menu option, as the game spells it. */
		public String getOption()
		{
			return option;
		}

		/** The choice a menu option on the corpse is, or null. */
		public static Choice byOption(String option)
		{
			if (option == null)
			{
				return null;
			}
			String o = option.trim();
			for (Choice c : values())
			{
				if (c.option.equalsIgnoreCase(o))
				{
					return c;
				}
			}
			return null;
		}
	}

	/**
	 * The five collection log drops ("The Maggot King has 5 collection log items"), the chance of
	 * each on one loot of each kind, and its item id (RuneLite gameval ItemID).
	 */
	public enum Unique
	{
		/** Jagex, via the wiki: 1/340, open-stomach, rolled before the main table. */
		ELDER_FANG("Elder venator fang", "Elder fang", 33634, 1 / 340.0, 0, false, "Etched elder venator fang"),
		/** Jagex, via the wiki: 1/520, open-stomach, rolled before the main table. */
		KISTEN("Crimson kisten", "Kisten", 33631, 1 / 520.0, 0, false),
		/** Jagex, via the wiki: 1/3,500 on open-stomach; "the total chance to receive the pet following any given egg-take is 1/1,502.4". */
		PET("Maggot marquess", "Marquess", 33642, 1 / 3500.0, 1 / 1502.4, false),
		/** 2/159 of the main table, reached when the pre-roll misses. The 2/159 is a Drop Rate Project estimate. */
		VENATOR_FANG("Venator fang", "Venator fang", 33661, (1 - PRE_ROLL) * MAIN_SHARE, 0, true),
		/** 2/159 of the main table, reached when the pre-roll misses. The 2/159 is a Drop Rate Project estimate. */
		VENATOR_TOOTH("Venator tooth", "Venator tooth", 33663, (1 - PRE_ROLL) * MAIN_SHARE, 0, true);

		private final String name;
		private final String shortName;
		private final int itemId;
		private final double stomach;
		private final double eggs;
		private final boolean estimate;
		private final String[] alsoNames;

		Unique(String name, String shortName, int itemId, double stomach, double eggs, boolean estimate, String... alsoNames)
		{
			this.name = name;
			this.shortName = shortName;
			this.itemId = itemId;
			this.stomach = stomach;
			this.eggs = eggs;
			this.estimate = estimate;
			this.alsoNames = alsoNames;
		}

		public String getName()
		{
			return name;
		}

		public String getShortName()
		{
			return shortName;
		}

		public int getItemId()
		{
			return itemId;
		}

		/** The chance on one loot of this kind. */
		public double rate(Choice c)
		{
			return c == Choice.EGGS ? eggs : c == Choice.STOMACH ? stomach : 0;
		}

		/** Whether the rate is the wiki's estimate from drop logs rather than Jagex's own figure. */
		public boolean isEstimate()
		{
			return estimate;
		}

		/** "1 in 340 opening the stomach", "1 in 3,500 opening the stomach, 1 in 1,502 taking eggs". */
		public String rateText()
		{
			String s = MaggotKing.oneIn(stomach) + " opening the stomach";
			if (eggs > 0)
			{
				s += ", " + MaggotKing.oneIn(eggs) + " taking eggs";
			}
			return estimate ? s + " (estimated)" : s;
		}

		/** "1/340", for tight spaces. */
		public String shortRate()
		{
			return MaggotKing.oneIn(stomach).replace("1 in ", "1/") + (estimate ? "~" : "");
		}

		/** Whether an item name is this drop, or what it becomes (an etched fang is a fang received). */
		public boolean is(String itemName)
		{
			if (itemName == null)
			{
				return false;
			}
			String n = itemName.trim();
			if (name.equalsIgnoreCase(n))
			{
				return true;
			}
			for (String a : alsoNames)
			{
				if (a.equalsIgnoreCase(n))
				{
					return true;
				}
			}
			return false;
		}

		/** The drop an item name is, or null. */
		public static Unique byName(String itemName)
		{
			for (Unique u : values())
			{
				if (u.is(itemName))
				{
					return u;
				}
			}
			return null;
		}

		@Override
		public String toString()
		{
			return name;
		}

		/** The drop a saved key is, or null. */
		public static Unique byKey(String key)
		{
			for (Unique u : values())
			{
				if (u.name().equals(key))
				{
					return u;
				}
			}
			return null;
		}
	}

	private MaggotKing()
	{
	}

	/** Whether an NPC id is the boss or its corpse. */
	public static boolean isBoss(int npcId)
	{
		return npcId == NPC || npcId == CORPSE;
	}

	/** Whether an item id is one of the maggot eggs. */
	public static boolean isEgg(int itemId)
	{
		for (int e : EGGS)
		{
			if (e == itemId)
			{
				return true;
			}
		}
		return false;
	}

	/** Whether an item name is a maggot egg ("Maggot egg" is the name of all six). */
	static boolean isEggName(String name)
	{
		return name != null && name.trim().equalsIgnoreCase("Maggot egg");
	}

	/** Whether a game message says the pet has come. */
	public static boolean petMessage(String message)
	{
		return message != null && (message.contains(PET_FOLLOW) || message.contains(PET_BACKPACK));
	}

	/**
	 * The chance a kill brings this drop when the player takes eggs this share of the time and
	 * opens the stomach otherwise.
	 */
	public static double perKill(Unique u, double eggShare)
	{
		double s = Math.max(0, Math.min(1, eggShare));
		return (1 - s) * u.rate(Choice.STOMACH) + s * u.rate(Choice.EGGS);
	}

	/** The kills it takes for this share of players to have had a drop of this chance a kill; -1 if never. */
	public static int killsFor(double chance, double share)
	{
		if (chance <= 0 || share <= 0)
		{
			return chance <= 0 ? -1 : 0;
		}
		if (chance >= 1)
		{
			return 1;
		}
		return (int) Math.ceil(Math.log(1 - share) / Math.log(1 - chance));
	}

	/** "1 in 340" for a chance of 1/340. */
	public static String oneIn(double chance)
	{
		if (chance <= 0)
		{
			return "never";
		}
		return "1 in " + Format.number(Math.round(1 / chance));
	}

	/** A share as a whole percent, "under 1%" or "over 99%" at the ends. */
	public static String percent(double p)
	{
		double pc = p * 100;
		if (pc > 0 && pc < 1)
		{
			return "under 1%";
		}
		if (pc > 99 && pc < 100)
		{
			return "over 99%";
		}
		return String.format(Locale.ROOT, "%d%%", Math.round(pc));
	}

	/** The drops among these item names. */
	public static java.util.List<Unique> uniques(Collection<String> names)
	{
		java.util.List<Unique> out = new java.util.ArrayList<>();
		if (names == null)
		{
			return out;
		}
		for (String n : names)
		{
			Unique u = Unique.byName(n);
			if (u != null && !out.contains(u))
			{
				out.add(u);
			}
		}
		return out;
	}
}
