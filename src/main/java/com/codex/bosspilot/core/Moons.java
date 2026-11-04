package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The facts of the Moons of Peril that Boss Pilot logs the Lunar Chest with: the three moons, the
 * four pieces of each moon's set, how the chest rolls by the moons subdued, and the odds sums made
 * from them. Every number is from the OSRS Wiki or RuneLite's source; the page is named beside it.
 * Nothing here is about what a moon does during its fight.
 */
public final class Moons
{
	/**
	 * Lunar Chest: "Defeating any one of the Moons of Peril grants the player a non-stackable 1/56
	 * chance at a piece of their equipment".
	 */
	public static final double SET_CHANCE = 1 / 56.0;
	/** Lunar Chest: each of the twelve uniques is listed at "1/224" (1/56 for the set, then one of its four pieces). */
	public static final double PIECE_CHANCE = 1 / 224.0;
	/** Lunar Chest: "1 Moon gives 1 roll", "2 Moons give 3 rolls", "3 Moons give 6 rolls" on the standard table. */
	static final int[] ROLLS = {0, 1, 3, 6};
	/** Lunar Chest: "The expected number of chests to complete all three sets is 320." */
	public static final int CHESTS_FOR_ALL_SETS = 320;
	/** Lunar Chest, infobox: object ID 51346, opened 51347 (RuneLite gameval ObjectID.CHEST_SUN01_DEFAULT01, CHEST_SUN01_OPEN01). */
	public static final int CHEST = 51346;
	public static final int CHEST_OPEN = 51347;
	/** The supplies made inside Neypotzli (RuneLite gameval ItemID.BREAM_FISH_COOKED, COOKED_LIZARD, _4DOSEMOONLIGHTPOTION and lower). */
	static final List<String> CAVERN_SUPPLIES = Collections.unmodifiableList(Arrays.asList("Cooked bream", "Cooked moss lizard", "Moonlight potion"));

	/**
	 * The three moons: the NPC name and id (OSRS Wiki infobox "Monster ID", RuneLite gameval
	 * NpcID.PMOON_BOSS_*_MOON_VIS), the varbit RuneLite names for the moon being dead
	 * (VarbitID.PMOON_BOSS_*_DEAD), and the four pieces of its set with their item ids
	 * (RuneLite gameval ItemID; the Blue Moon pieces are FROSTMOON_SPEAR and FROST_MOON_*).
	 */
	public enum Moon
	{
		BLOOD("Blood Moon", "Blood", 13011, 9858,
			new Piece("Dual macuahuitl", "Wpn", 28997), new Piece("Blood Moon helm", "Helm", 29028),
			new Piece("Blood Moon chestplate", "Body", 29022), new Piece("Blood Moon tassets", "Legs", 29025)),
		BLUE("Blue Moon", "Blue", 13013, 9859,
			new Piece("Blue Moon spear", "Wpn", 28988), new Piece("Blue Moon helm", "Helm", 29019),
			new Piece("Blue Moon chestplate", "Body", 29013), new Piece("Blue Moon tassets", "Legs", 29016)),
		ECLIPSE("Eclipse Moon", "Eclipse", 13012, 9860,
			new Piece("Eclipse atlatl", "Wpn", 29000), new Piece("Eclipse Moon helm", "Helm", 29010),
			new Piece("Eclipse Moon chestplate", "Body", 29004), new Piece("Eclipse Moon tassets", "Legs", 29007));

		private final String name;
		private final String shortName;
		private final int npcId;
		private final int deadVarbit;
		private final List<Piece> pieces;

		Moon(String name, String shortName, int npcId, int deadVarbit, Piece... pieces)
		{
			this.name = name;
			this.shortName = shortName;
			this.npcId = npcId;
			this.deadVarbit = deadVarbit;
			this.pieces = Collections.unmodifiableList(Arrays.asList(pieces));
		}

		public String getName()
		{
			return name;
		}

		public String getShortName()
		{
			return shortName;
		}

		public int getNpcId()
		{
			return npcId;
		}

		public int getDeadVarbit()
		{
			return deadVarbit;
		}

		public List<Piece> getPieces()
		{
			return pieces;
		}

		/** The moon a name is ("Blood Moon"), or null. */
		public static Moon byName(String name)
		{
			if (name == null)
			{
				return null;
			}
			for (Moon m : values())
			{
				if (m.name.equalsIgnoreCase(name.trim()))
				{
					return m;
				}
			}
			return null;
		}
	}

	/** One piece of a moon's set. */
	public static final class Piece
	{
		private final String name;
		private final String slot;
		private final int itemId;

		Piece(String name, String slot, int itemId)
		{
			this.name = name;
			this.slot = slot;
			this.itemId = itemId;
		}

		public String getName()
		{
			return name;
		}

		/** The slot, short enough for a pip: "Helm", "Body", "Legs" or "Wpn". */
		public String getSlot()
		{
			return slot;
		}

		/** The slot in words: "helm", "body", "legs" or "weapon". */
		public String slotName()
		{
			return "Wpn".equals(slot) ? "weapon" : slot.toLowerCase(java.util.Locale.ROOT);
		}

		public int getItemId()
		{
			return itemId;
		}
	}

	private Moons()
	{
	}

	/** The moon whose set a piece belongs to, by item name, or null. */
	public static Moon moonOf(String itemName)
	{
		Piece p = piece(itemName);
		if (p == null)
		{
			return null;
		}
		for (Moon m : Moon.values())
		{
			if (m.pieces.contains(p))
			{
				return m;
			}
		}
		return null;
	}

	/** The piece an item name is, or null. */
	public static Piece piece(String itemName)
	{
		if (itemName == null)
		{
			return null;
		}
		String n = itemName.trim();
		for (Moon m : Moon.values())
		{
			for (Piece p : m.pieces)
			{
				if (p.name.equalsIgnoreCase(n))
				{
					return p;
				}
			}
		}
		return null;
	}

	/** The set pieces among these item names, by name, each once. */
	public static List<String> pieces(Collection<String> names)
	{
		List<String> out = new ArrayList<>();
		if (names == null)
		{
			return out;
		}
		for (String n : names)
		{
			Piece p = piece(stripCount(n));
			if (p != null && !out.contains(p.name))
			{
				out.add(p.name);
			}
		}
		return out;
	}

	/** "3 x Atlatl dart" is "Atlatl dart". */
	static String stripCount(String n)
	{
		if (n == null)
		{
			return null;
		}
		int x = n.indexOf(" x ");
		return x > 0 && n.substring(0, x).replace(",", "").matches("\\d+") ? n.substring(x + 3) : n;
	}

	/** The rolls on the standard table for this many moons subdued. */
	public static int rolls(int moons)
	{
		return ROLLS[Math.max(0, Math.min(3, moons))];
	}

	/**
	 * The chance a chest brings at least one piece, for these moons subdued: each moon's 1/56 is its
	 * own roll (Lunar Chest: all three give "1/19").
	 */
	public static double chanceAny(int moons)
	{
		return 1 - Math.pow(1 - SET_CHANCE, Math.max(0, Math.min(3, moons)));
	}

	/**
	 * The chance a chest brings a piece the player has not got yet, for these moons subdued. Lunar
	 * Chest: "the player will not obtain duplicate pieces from a set until each piece in the set has
	 * been received at least once", so while a moon's set is unfinished, its 1/56 is always a new piece.
	 */
	public static double chanceNew(Set<Moon> subdued, Collection<String> missing)
	{
		double none = 1;
		for (Moon m : subdued)
		{
			if (missingOf(m, missing) > 0)
			{
				none *= 1 - SET_CHANCE;
			}
		}
		return 1 - none;
	}

	/** How many of a moon's four pieces are in the missing list. */
	public static int missingOf(Moon m, Collection<String> missing)
	{
		int n = 0;
		for (Piece p : m.pieces)
		{
			n += missing.contains(p.name) ? 1 : 0;
		}
		return n;
	}

	/** The pieces not among the owned names, in set order. */
	public static List<String> missing(Collection<String> owned)
	{
		List<String> out = new ArrayList<>();
		for (Moon m : Moon.values())
		{
			for (Piece p : m.pieces)
			{
				if (owned == null || !owned.contains(p.name))
				{
					out.add(p.name);
				}
			}
		}
		return out;
	}

	/** The chance of no piece of a set in this many kills of its moon. */
	public static double dry(int kills)
	{
		return kills <= 0 ? 1 : Math.pow(1 - SET_CHANCE, kills);
	}

	/** Moons from a bit mask (bit 0 Blood, 1 Blue, 2 Eclipse). */
	public static Set<Moon> fromMask(int mask)
	{
		Set<Moon> out = EnumSet.noneOf(Moon.class);
		for (Moon m : Moon.values())
		{
			if ((mask & 1 << m.ordinal()) != 0)
			{
				out.add(m);
			}
		}
		return out;
	}

	/** A bit mask of moons. */
	public static int mask(Collection<Moon> moons)
	{
		int mask = 0;
		for (Moon m : moons)
		{
			mask |= 1 << m.ordinal();
		}
		return mask;
	}

	/** Whether an item name is a supply the trip counts: food Boss Pilot knows, the cavern food, or any potion with doses. */
	public static boolean supply(String name)
	{
		if (name == null)
		{
			return false;
		}
		if (Food.heal(name) > 0 || CAVERN_SUPPLIES.contains(name))
		{
			return true;
		}
		if (Dose.of(name) <= 0)
		{
			return false;
		}
		// Potions only: jewellery such as a games necklace(8) also counts down in brackets.
		String base = Dose.base(name).toLowerCase(Locale.ROOT);
		for (String word : POTION_WORDS)
		{
			if (base.contains(word))
			{
				return true;
			}
		}
		return false;
	}

	private static final String[] POTION_WORDS = {"potion", "brew", "restore", "serum", "antidote", "anti-venom", "antifire", "mix"};

	/** "1 in 56" for a chance of 1/56. */
	public static String oneIn(double chance)
	{
		return Zalcano.oneIn(chance);
	}

	/** A share as a whole percent, "under 1%" or "over 99%" at the ends. */
	public static String percent(double p)
	{
		return Zalcano.percent(p);
	}

	/** "2.6" moons. */
	static String tenths(double v)
	{
		return String.format(Locale.ROOT, "%.1f", v);
	}
}
