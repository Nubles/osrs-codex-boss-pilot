package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The facts of a Barrows run and the sums made from them: the six brothers, the tunnel monsters,
 * reward potential and the chance of Barrows equipment from the chest. Every number is from the
 * OSRS Wiki; the page is named beside it.
 */
public final class Barrows
{
	/**
	 * Chest (Barrows): "reward potential is the sum of all the combat levels of the monsters
	 * killed, capped at 1000 ... The number of brothers killed times two is then added, giving a
	 * total maximum reward potential of 1012."
	 */
	public static final int LEVELS_CAP = 1000;
	public static final int POTENTIAL_PER_BROTHER = 2;
	public static final int MAX_POTENTIAL = 1012;
	/** Chest (Barrows): "Each additional brother killed grants an additional roll at the drop table, to a maximum of seven total rolls." */
	static final int MAX_ROLLS = 7;
	/** Barrows equipment: four pieces for each brother. */
	public static final int PIECES_PER_BROTHER = 4;
	/** How far under the aim a plan may land to save a kill. */
	static final int PLAN_SLACK = 3;

	/** The six brothers: combat level from each brother's page, the RuneLite varbit set when he is killed this run, and his crypt's stairs and sarcophagus. */
	public enum Brother
	{
		// Combat levels: Ahrim the Blighted, Karil the Tainted "98"; Dharok, Guthan, Torag, Verac "115".
		// Varbits: VarbitID.BARROWS_KILLED_*. Objects: ObjectID.BARROWS_STAIRS_* and ObjectID.BARROW_*_SARCOPHAGUS.
		AHRIM("Ahrim", "Ahrim the Blighted", 98, 457, 20667, 20770, "Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt", "Ahrim's staff"),
		DHAROK("Dharok", "Dharok the Wretched", 115, 458, 20668, 20720, "Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe"),
		GUTHAN("Guthan", "Guthan the Infested", 115, 459, 20669, 20722, "Guthan's helm", "Guthan's platebody", "Guthan's chainskirt", "Guthan's warspear"),
		KARIL("Karil", "Karil the Tainted", 98, 460, 20670, 20771, "Karil's coif", "Karil's leathertop", "Karil's leatherskirt", "Karil's crossbow"),
		TORAG("Torag", "Torag the Corrupted", 115, 461, 20671, 20721, "Torag's helm", "Torag's platebody", "Torag's platelegs", "Torag's hammers"),
		VERAC("Verac", "Verac the Defiled", 115, 462, 20672, 20772, "Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail");

		private final String shortName;
		private final String npcName;
		private final int level;
		private final int varbit;
		private final int stairs;
		private final int sarcophagus;
		private final List<String> pieces;

		Brother(String shortName, String npcName, int level, int varbit, int stairs, int sarcophagus, String... pieces)
		{
			this.shortName = shortName;
			this.npcName = npcName;
			this.level = level;
			this.varbit = varbit;
			this.stairs = stairs;
			this.sarcophagus = sarcophagus;
			this.pieces = Collections.unmodifiableList(java.util.Arrays.asList(pieces));
		}

		public String getShortName()
		{
			return shortName;
		}

		public String getNpcName()
		{
			return npcName;
		}

		public int getLevel()
		{
			return level;
		}

		public int getVarbit()
		{
			return varbit;
		}

		public List<String> getPieces()
		{
			return pieces;
		}

		/** The brother whose crypt an object (its stairs or sarcophagus) belongs to, or null. */
		public static Brother byObject(int id)
		{
			for (Brother b : values())
			{
				if (b.stairs == id || b.sarcophagus == id)
				{
					return b;
				}
			}
			return null;
		}
	}

	/** The monsters of the tunnels, by combat level (Barrows/Strategies: "Crypt rat 43, bloodworm 52, crypt spider 56, giant crypt rat 76, skeleton 77, giant crypt spider 79"). */
	public enum Monster
	{
		GIANT_CRYPT_SPIDER("Giant crypt spider", "Giant crypt spiders", 79),
		SKELETON("Skeleton", "Skeletons", 77),
		GIANT_CRYPT_RAT("Giant crypt rat", "Giant crypt rats", 76),
		CRYPT_SPIDER("Crypt spider", "Crypt spiders", 56),
		BLOODWORM("Bloodworm", "Bloodworms", 52),
		CRYPT_RAT("Crypt rat", "Crypt rats", 43);

		private final String name;
		private final String plural;
		private final int level;

		Monster(String name, String plural, int level)
		{
			this.name = name;
			this.plural = plural;
			this.level = level;
		}

		public String getName()
		{
			return name;
		}

		public int getLevel()
		{
			return level;
		}

		public String named(int n)
		{
			return n == 1 ? name : plural;
		}
	}

	/**
	 * What reward potential to aim for: the potential that gives the most of a reward
	 * (Chest (Barrows), "RP for max quantity": blood rune 880, bolt rack 1005; dragon med helm
	 * "Requires 1,012 reward potential to roll").
	 */
	public enum Aim
	{
		OFF("Off", 0),
		DEATH_RUNES("Death runes (755)", 755),
		BLOOD_RUNES("Blood runes (880)", 880),
		BOLT_RACKS("Bolt racks (1005)", 1005),
		FULL("Everything (1012)", MAX_POTENTIAL);

		private final String label;
		private final int potential;

		Aim(String label, int potential)
		{
			this.label = label;
			this.potential = potential;
		}

		public int getPotential()
		{
			return potential;
		}

		/** "blood runes", for sentences. */
		public String reward()
		{
			switch (this)
			{
				case DEATH_RUNES:
					return "death runes";
				case BLOOD_RUNES:
					return "blood runes";
				case BOLT_RACKS:
					return "bolt racks";
				case FULL:
					return "the full table";
				default:
					return "";
			}
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private Barrows()
	{
	}

	/** R = min(sum of levels, 1000) + 2 x brothers killed. */
	public static int potential(int levels, int brothers)
	{
		return Math.min(Math.max(0, levels), LEVELS_CAP) + POTENTIAL_PER_BROTHER * Math.max(0, brothers);
	}

	/** Chest (Barrows), fig. 1: a Barrows piece on one roll is 1/(450 - 58N); none with no brother killed. */
	public static double perRoll(int brothers)
	{
		int n = Math.min(Brother.values().length, brothers);
		return n <= 0 ? 0 : 1.0 / (450 - 58 * n);
	}

	/** "There is initially one roll" and one more for each brother, at most seven. */
	public static int rolls(int brothers)
	{
		return Math.min(MAX_ROLLS, 1 + Math.max(0, brothers));
	}

	/** The chance a chest holds at least one Barrows piece (about 1/15.01 with all six, as the wiki gives). */
	public static double chanceAny(int brothers)
	{
		return 1 - Math.pow(1 - perRoll(brothers), rolls(brothers));
	}

	/**
	 * The chance a chest holds at least one of the pieces still missing. Each roll that hits picks
	 * one of the pieces of the brothers killed, all equally likely, and never one already picked
	 * for this chest (Chest (Barrows): "each part has equal chance ... once chosen it will not be
	 * chosen again on future rolls").
	 *
	 * @param brothers brothers killed before the chest
	 * @param missing pieces of those brothers still missing
	 */
	public static double chanceNew(int brothers, int missing)
	{
		int n = Math.min(Brother.values().length, Math.max(0, brothers));
		int pool = PIECES_PER_BROTHER * n;
		int m = Math.min(Math.max(0, missing), pool);
		if (n == 0 || m == 0)
		{
			return 0;
		}
		double p = perRoll(n);
		// dry[c]: chance that c pieces were picked so far, none of them missing ones.
		double[] dry = new double[pool + 1];
		dry[0] = 1;
		for (int r = 0; r < rolls(n); r++)
		{
			double[] next = new double[pool + 1];
			for (int c = 0; c <= pool; c++)
			{
				if (dry[c] == 0)
				{
					continue;
				}
				int left = pool - c;
				if (left == 0)
				{
					next[c] += dry[c];
					continue;
				}
				next[c] += dry[c] * (1 - p);
				int owned = pool - m - c;
				if (owned > 0)
				{
					next[c + 1] += dry[c] * p * owned / left;
				}
			}
			dry = next;
		}
		double stillDry = 0;
		for (double d : dry)
		{
			stillDry += d;
		}
		return 1 - stillDry;
	}

	/** "1 in 15". */
	public static String oneIn(double chance)
	{
		if (chance <= 0)
		{
			return "none";
		}
		double n = 1 / chance;
		return "1 in " + (n < 10 ? String.format(Locale.ROOT, "%.1f", n) : String.valueOf(Math.round(n)));
	}

	/** The plain name of a Barrows piece without its wear ("Ahrim's hood 75" is "Ahrim's hood"), or null when it is not one. */
	public static String piece(String itemName)
	{
		if (itemName == null)
		{
			return null;
		}
		for (Brother b : Brother.values())
		{
			for (String p : b.pieces)
			{
				if (itemName.equals(p) || itemName.startsWith(p + " ") && itemName.substring(p.length() + 1).matches("\\d+"))
				{
					return p;
				}
			}
		}
		return null;
	}

	/** The pieces among some item names, without wear. */
	public static Set<String> pieces(Collection<String> itemNames)
	{
		Set<String> out = new java.util.LinkedHashSet<>();
		for (String n : itemNames)
		{
			String p = piece(n);
			if (p != null)
			{
				out.add(p);
			}
		}
		return out;
	}

	/** The fewest tunnel kills that bring a run's potential up to an aim. */
	public static final class Plan
	{
		/** Potential the run will have once every brother still standing is killed, before tunnel kills. */
		private final int projected;
		private final int aim;
		private final Map<Monster, Integer> kills;
		private final int adds;

		Plan(int projected, int aim, Map<Monster, Integer> kills, int adds)
		{
			this.projected = projected;
			this.aim = aim;
			this.kills = kills;
			this.adds = adds;
		}

		public int getProjected()
		{
			return projected;
		}

		public int getAim()
		{
			return aim;
		}

		public Map<Monster, Integer> getKills()
		{
			return kills;
		}

		public int getAdds()
		{
			return adds;
		}

		public int total()
		{
			int n = 0;
			for (int k : kills.values())
			{
				n += k;
			}
			return n;
		}

		/** The potential the plan ends on. */
		public int result()
		{
			return projected + adds;
		}

		/** Whether the brothers alone already reach the aim. */
		public boolean reached()
		{
			return projected >= aim;
		}

		/** "2 Skeletons, 1 Bloodworm". */
		public String killsText()
		{
			List<String> parts = new ArrayList<>();
			kills.forEach((m, n) -> parts.add(n + " " + m.named(n)));
			return String.join(", ", parts);
		}
	}

	/**
	 * The fewest tunnel kills from where the run stands to the aim, counting the brothers not yet
	 * killed as killed (the usual run kills all six). It never plans past the aim, and lands up to
	 * a few points under it when that saves a kill.
	 *
	 * @param levels the combat levels killed so far this run (brothers' and tunnel monsters')
	 * @param killed the brothers killed so far
	 */
	public static Plan plan(int levels, Set<Brother> killed, Aim aim)
	{
		int sum = Math.max(0, levels);
		for (Brother b : Brother.values())
		{
			if (!killed.contains(b))
			{
				sum += b.level;
			}
		}
		int projected = potential(sum, Brother.values().length);
		int target = aim.getPotential();
		Map<Monster, Integer> kills = new LinkedHashMap<>();
		// Levels past the cap add nothing, so the room left is also bounded by the cap.
		int room = Math.min(target - projected, LEVELS_CAP - Math.min(sum, LEVELS_CAP));
		if (aim == Aim.OFF || room <= 0)
		{
			return new Plan(projected, target, kills, 0);
		}
		Monster[] ms = Monster.values();
		// Everything (1012) needs the cap itself, and levels past it are free, so that aim may
		// overshoot; the others stop at their mark, since more potential spreads the rolls wider.
		boolean over = aim == Aim.FULL;
		int top = over ? room + ms[0].level : room;
		int[] fewest = new int[top + 1];
		int[] last = new int[top + 1];
		java.util.Arrays.fill(fewest, Integer.MAX_VALUE);
		fewest[0] = 0;
		for (int s = 1; s <= top; s++)
		{
			for (int i = 0; i < ms.length; i++)
			{
				int l = ms[i].level;
				if (l <= s && fewest[s - l] != Integer.MAX_VALUE && fewest[s - l] + 1 < fewest[s])
				{
					fewest[s] = fewest[s - l] + 1;
					last[s] = i;
				}
			}
		}
		int best = -1;
		for (int s = room; over && s <= top; s++)
		{
			if (fewest[s] != Integer.MAX_VALUE && (best < 0 || fewest[s] < fewest[best]))
			{
				best = s;
			}
		}
		for (int s = room; !over && s >= Math.max(1, room - PLAN_SLACK); s--)
		{
			if (fewest[s] != Integer.MAX_VALUE && (best < 0 || fewest[s] < fewest[best]))
			{
				best = s;
			}
		}
		if (best < 0)
		{
			for (int s = room; s >= 1 && best < 0; s--)
			{
				if (fewest[s] != Integer.MAX_VALUE)
				{
					best = s;
				}
			}
		}
		if (best < 0)
		{
			return new Plan(projected, target, kills, 0);
		}
		Map<Monster, Integer> counted = new java.util.EnumMap<>(Monster.class);
		for (int s = best; s > 0; s -= ms[last[s]].level)
		{
			counted.merge(ms[last[s]], 1, Integer::sum);
		}
		kills.putAll(counted);
		return new Plan(projected, target, kills, Math.min(best, room));
	}

	/** The brothers killed, from the six varbits' values in brother order. */
	public static Set<Brother> killed(int[] values)
	{
		Set<Brother> out = EnumSet.noneOf(Brother.class);
		Brother[] bs = Brother.values();
		for (int i = 0; i < bs.length && i < values.length; i++)
		{
			if (values[i] > 0)
			{
				out.add(bs[i]);
			}
		}
		return out;
	}
}
