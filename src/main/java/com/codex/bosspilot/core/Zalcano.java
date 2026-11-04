package com.codex.bosspilot.core;

import java.util.Collection;
import java.util.Locale;

/**
 * The facts of Zalcano that Boss Pilot plans and logs with: where the prison is, the four rare
 * drops and how often each comes, and the odds sums made from them. Every number is from the OSRS
 * Wiki or RuneLite's source; the page is named beside it. Nothing here is about what Zalcano does
 * during the fight.
 */
public final class Zalcano
{
	/** Zalcano's prison, by map region (RuneLite's Discord plugin: BOSS_ZALCANO "Zalcano", region 12126). */
	public static final int PRISON_REGION = 12126;
	/** Prifddinas, by map region (RuneLite's Discord plugin: CITY_PRIFDDINAS). */
	static final int[] PRIFDDINAS_REGIONS = {8499, 8500, 8755, 8756, 9011, 9012, 9013, 12894, 12895, 13150, 13151};
	/** Zalcano: "Players must do at least 5 damage to Zalcano's shield to be eligible for drops". */
	public static final int DROP_DAMAGE = 5;
	/** Zalcano: "31 combined damage to be eligible for uniques and pet". */
	public static final int UNIQUE_DAMAGE = 31;
	/** Smolcano: "You have a funny feeling like you're being followed." or "You feel something weird sneaking into your backpack." */
	static final String PET_FOLLOW = "You have a funny feeling like you're being followed";
	static final String PET_BACKPACK = "You feel something weird sneaking into your backpack";

	/**
	 * The four rare drops, the chance of each on one kill at the low and the high end, and its
	 * item id (RuneLite gameval ItemID). Zalcano, drops: "Every Zalcano kill always has a 1/200
	 * chance of rolling the crystal tool seed table" (onyx 1/40 of it, otherwise the seed), so the
	 * seed is 39/8,000 and the onyx 1/8,000 a kill, with "the players with more points" more likely
	 * to receive it; Zalcano shard: "1/1,500 ... by only contributing the bare minimum" to "1/750"
	 * at the most; Smolcano: "a static 1/2,250 drop rate for any eligible players".
	 */
	public enum Unique
	{
		TOOL_SEED("Crystal tool seed", "Tool seed", 23953, 39 / 8000.0, 39 / 8000.0, "a team roll: the most points take it most often"),
		SHARD("Zalcano shard", "Shard", 23908, 1 / 1500.0, 1 / 750.0, "better the more you contribute"),
		PET("Smolcano", "Smolcano", 23760, 1 / 2250.0, 1 / 2250.0, "the same for every eligible player"),
		ONYX("Uncut onyx", "Onyx", 6571, 1 / 8000.0, 1 / 8000.0, "on the tool seed's table");

		private final String name;
		private final String shortName;
		private final int itemId;
		private final double low;
		private final double high;
		private final String how;

		Unique(String name, String shortName, int itemId, double low, double high, String how)
		{
			this.name = name;
			this.shortName = shortName;
			this.itemId = itemId;
			this.low = low;
			this.high = high;
			this.how = how;
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

		/** The chance a kill, at the least you can do and still be eligible. */
		public double getLow()
		{
			return low;
		}

		/** The chance a kill, at the most. */
		public double getHigh()
		{
			return high;
		}

		/** What the chance depends on, for the panel. */
		public String getHow()
		{
			return how;
		}

		public boolean ranged()
		{
			return low != high;
		}

		/** "1 in 205", or "1 in 1,500 to 1 in 750". */
		public String rate()
		{
			return ranged() ? oneIn(low) + " to " + oneIn(high) : oneIn(high);
		}

		/** "1/205", or "1/1,500 to 1/750", for tight spaces. */
		public String shortRate()
		{
			return rate().replace("1 in ", "1/");
		}

		/** The drop an item name is, or null. */
		public static Unique byName(String itemName)
		{
			if (itemName == null)
			{
				return null;
			}
			for (Unique u : values())
			{
				if (u.name.equalsIgnoreCase(itemName.trim()))
				{
					return u;
				}
			}
			return null;
		}

		/** The drop an item id is, or null. */
		public static Unique byId(int id)
		{
			for (Unique u : values())
			{
				if (u.itemId == id)
				{
					return u;
				}
			}
			return null;
		}
	}

	private Zalcano()
	{
	}

	/** Whether a map region is Prifddinas. */
	public static boolean inPrifddinas(int region)
	{
		for (int r : PRIFDDINAS_REGIONS)
		{
			if (r == region)
			{
				return true;
			}
		}
		return false;
	}

	/** The map region of a tile. */
	public static int region(int x, int y)
	{
		return (x >> 6) << 8 | y >> 6;
	}

	/** Whether a game message says the pet has come. */
	public static boolean petMessage(String message)
	{
		return message != null && (message.contains(PET_FOLLOW) || message.contains(PET_BACKPACK));
	}

	/** The chance of no drop in this many kills at this chance a kill. */
	public static double dry(double chance, int kills)
	{
		return kills <= 0 ? 1 : Math.pow(1 - chance, kills);
	}

	/** The chance of at least one drop in this many kills. */
	public static double atLeastOne(double chance, int kills)
	{
		return 1 - dry(chance, kills);
	}

	/** "1 in 205" for a chance of 39/8,000. */
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
