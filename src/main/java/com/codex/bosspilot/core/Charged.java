package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The charged bossing items Boss Pilot knows: what each one holds, how much at most, how fast a
 * fight uses it, and how to read the game's own message when the player checks it. Nothing here
 * counts attacks or reads the boss: charges come from the game's messages and are carried
 * forward from the kills since, at a rate learnt from the player's own checks.
 *
 * <p>Facts, OSRS Wiki unless said:
 * <ul>
 * <li>Toxic blowpipe: "can hold up to 16,383 scales and 16,383 darts"; "on average, two scales
 * are used for every three shots fired"; attack speed 3 ticks, 2 on rapid; darts saved by Ava's
 * attractor 60%, accumulator 72%, assembler 80%.</li>
 * <li>Trident of the seas and of the swamp: "up to 2,500 charges"; the (e) "holds 20,000 charges
 * instead of 2,500"; "One charge is consumed each time you cast the built-in spell"; 4 ticks.</li>
 * <li>Sanguinesti staff: "up to 20,000 charges", a charge a cast (2 blood runes each); 4 ticks.</li>
 * <li>Tumeken's shadow: "up to 20,000 charges", a charge a cast (2 soul and 5 chaos runes); 5 ticks.</li>
 * <li>Bow of faerdhinen: "up to a maximum of 20,000", "one charge being depleted for each hit,
 * whether it is a successful one or not"; 5 ticks, 4 on rapid.</li>
 * <li>Serpentine helm: 11,000 scales; 10 scales on entering combat and 10 more each 90 ticks in
 * combat, "666.67 scales per hour of continuous combat".</li>
 * <li>Crystal armour: "the maximum charges that can be stored is 20,000"; a charge for each
 * successful hit the wearer takes.</li>
 * </ul>
 * The check messages are the game's, as logged by the Weapon Charges plugin (geheur, BSD-2) and
 * read alike by Item Charges Improved (TicTac7x); re-written here from those logs.
 */
public final class Charged
{
	/** What wears an item down. */
	public enum Wear
	{
		/** Each attack with it. */
		ATTACK,
		/** Each hit the wearer takes. */
		HIT_TAKEN,
		/** Time in combat. */
		COMBAT_TIME
	}

	/** One thing an item holds: charges, darts or scales. */
	public static final class Pool
	{
		private final String name;
		private final int max;
		private final double perUse;

		Pool(String name, int max, double perUse)
		{
			this.name = name;
			this.max = max;
			this.perUse = perUse;
		}

		/** "charges", "darts", "scales". */
		public String getName()
		{
			return name;
		}

		public int getMax()
		{
			return max;
		}

		/** Used per attack, per hit taken or per second in combat. */
		public double getPerUse()
		{
			return perUse;
		}
	}

	/** Seconds a game tick lasts: OSRS Wiki, Game tick, "0.6 seconds". */
	public static final double TICK = 0.6;
	/** Serpentine helm: 10 scales each 90 ticks of combat. */
	static final double HELM_PER_SECOND = 10 / (90 * TICK);
	/** Serpentine helm: 10 scales on entering combat. */
	static final int HELM_ON_COMBAT = 10;

	public static final Charged TOXIC_BLOWPIPE = new Charged("blowpipe", "Toxic blowpipe", Wear.ATTACK, 3, 2,
		"Add darts and Zulrah's scales", Arrays.asList("Toxic blowpipe"),
		new Pool("darts", 16383, 1), new Pool("scales", 16383, 2.0 / 3));
	public static final Charged TRIDENT_SEAS = trident("seas", "Trident of the seas", 2500,
		"Add death, chaos and fire runes and coins");
	public static final Charged TRIDENT_SEAS_E = trident("seas-e", "Trident of the seas (e)", 20000,
		"Add death, chaos and fire runes and coins");
	public static final Charged TRIDENT_SWAMP = trident("swamp", "Trident of the swamp", 2500,
		"Add death, chaos and fire runes and Zulrah's scales");
	public static final Charged TRIDENT_SWAMP_E = trident("swamp-e", "Trident of the swamp (e)", 20000,
		"Add death, chaos and fire runes and Zulrah's scales");
	public static final Charged SANGUINESTI = new Charged("sang", "Sanguinesti staff", Wear.ATTACK, 4, 4,
		"Add blood runes", Arrays.asList("Sanguinesti staff"), new Pool("charges", 20000, 1));
	public static final Charged HOLY_SANGUINESTI = new Charged("holy-sang", "Holy sanguinesti staff", Wear.ATTACK, 4, 4,
		"Add blood runes", Arrays.asList("Holy sanguinesti staff"), new Pool("charges", 20000, 1));
	public static final Charged TUMEKENS_SHADOW = new Charged("shadow", "Tumeken's shadow", Wear.ATTACK, 5, 5,
		"Add soul and chaos runes", Arrays.asList("Tumeken's shadow"), new Pool("charges", 20000, 1));
	public static final Charged BOW_OF_FAERDHINEN = new Charged("bowfa", "Bow of faerdhinen", Wear.ATTACK, 5, 4,
		"Add crystal shards", Arrays.asList("Bow of faerdhinen"), new Pool("charges", 20000, 1));
	public static final Charged SERPENTINE_HELM = new Charged("serp", "Serpentine helm", Wear.COMBAT_TIME, 0, 0,
		"Add Zulrah's scales", Arrays.asList("Serpentine helm"), new Pool("scales", 11000, HELM_PER_SECOND));
	public static final Charged CRYSTAL_HELM = crystal("crystal-helm", "Crystal helm");
	public static final Charged CRYSTAL_BODY = crystal("crystal-body", "Crystal body");
	public static final Charged CRYSTAL_LEGS = crystal("crystal-legs", "Crystal legs");

	public static final List<Charged> ALL = Collections.unmodifiableList(Arrays.asList(TOXIC_BLOWPIPE, TRIDENT_SEAS, TRIDENT_SEAS_E,
		TRIDENT_SWAMP, TRIDENT_SWAMP_E, SANGUINESTI, HOLY_SANGUINESTI, TUMEKENS_SHADOW, BOW_OF_FAERDHINEN, SERPENTINE_HELM,
		CRYSTAL_HELM, CRYSTAL_BODY, CRYSTAL_LEGS));

	private static final List<Charged> TRIDENTS = Arrays.asList(TRIDENT_SEAS, TRIDENT_SEAS_E, TRIDENT_SWAMP, TRIDENT_SWAMP_E);

	/** Darts an Ava's device saves, by cape name: OSRS Wiki, Toxic blowpipe, ammo recovery table. */
	private static final String[][] RETRIEVAL = {
		{"ava's assembler", "0.80"},
		{"ava's accumulator", "0.72"},
		{"ava's attractor", "0.60"},
	};

	private final String key;
	private final String name;
	private final Wear wear;
	private final int ticks;
	private final int rapidTicks;
	private final String recharge;
	private final List<String> itemNames;
	private final List<Pool> pools;

	private Charged(String key, String name, Wear wear, int ticks, int rapidTicks, String recharge, List<String> itemNames, Pool... pools)
	{
		this.key = key;
		this.name = name;
		this.wear = wear;
		this.ticks = ticks;
		this.rapidTicks = rapidTicks;
		this.recharge = recharge;
		List<String> lower = new ArrayList<>();
		for (String n : itemNames)
		{
			lower.add(n.toLowerCase(Locale.ROOT));
		}
		this.itemNames = Collections.unmodifiableList(lower);
		this.pools = Collections.unmodifiableList(Arrays.asList(pools));
	}

	private static Charged trident(String key, String name, int max, String recharge)
	{
		return new Charged(key, name, Wear.ATTACK, 4, 4, recharge, Arrays.asList(name), new Pool("charges", max, 1));
	}

	private static Charged crystal(String key, String name)
	{
		return new Charged(key, name, Wear.HIT_TAKEN, 0, 0, "Add crystal shards", Arrays.asList(name), new Pool("charges", 20000, 1));
	}

	/** The charged item an item name is, matched whole and in any case, or null. */
	public static Charged byItem(String itemName)
	{
		if (itemName == null)
		{
			return null;
		}
		String n = itemName.toLowerCase(Locale.ROOT).trim();
		for (Charged c : ALL)
		{
			if (c.itemNames.contains(n))
			{
				return c;
			}
		}
		return null;
	}

	public static Charged byKey(String key)
	{
		for (Charged c : ALL)
		{
			if (c.key.equals(key))
			{
				return c;
			}
		}
		return null;
	}

	/** The charged items among these item names, in catalogue order, each once. */
	public static List<Charged> among(Collection<String> itemNames)
	{
		List<Charged> out = new ArrayList<>();
		for (Charged c : ALL)
		{
			for (String n : itemNames)
			{
				if (byItem(n) == c)
				{
					out.add(c);
					break;
				}
			}
		}
		return out;
	}

	/** Share of darts an Ava's device saves, from the cape's name; 0 without one. */
	public static double dartsSaved(Collection<String> worn)
	{
		for (String n : worn)
		{
			String lower = n == null ? "" : n.toLowerCase(Locale.ROOT);
			for (String[] r : RETRIEVAL)
			{
				if (lower.equals(r[0]))
				{
					return Double.parseDouble(r[1]);
				}
			}
		}
		return 0;
	}

	/**
	 * What each pool wears down by in one kill, estimated from a setup: attacks from the kill time
	 * at full uptime and the attack speed, time in combat for the helm. Null when it cannot be
	 * estimated so (crystal armour wears by hits taken, which only the player's own trips show).
	 *
	 * @param killSeconds the setup's kill time at full uptime, in seconds
	 * @param attackTicks the setup's ticks between attacks, or 0 to go by the item's own speed
	 * @param style the setup's attack style, such as "Rapid", or null
	 * @param worn what the setup wears, for an Ava's device
	 */
	public double[] perKill(double killSeconds, int attackTicks, String style, Collection<String> worn)
	{
		if (killSeconds <= 0)
		{
			return null;
		}
		double[] out = new double[pools.size()];
		switch (wear)
		{
			case ATTACK:
			{
				int t = attackTicks > 0 ? attackTicks : "Rapid".equalsIgnoreCase(style) ? rapidTicks : ticks;
				double attacks = killSeconds / (t * TICK);
				for (int i = 0; i < out.length; i++)
				{
					double per = pools.get(i).perUse;
					if (this == TOXIC_BLOWPIPE && i == 0)
					{
						per *= 1 - dartsSaved(worn);
					}
					out[i] = attacks * per;
				}
				return out;
			}
			case COMBAT_TIME:
				out[0] = HELM_ON_COMBAT + killSeconds * pools.get(0).perUse;
				return out;
			default:
				return null;
		}
	}

	/** From hits taken a kill, for crystal armour. */
	public double[] perKillFromHits(double hitsPerKill)
	{
		if (wear != Wear.HIT_TAKEN || hitsPerKill <= 0)
		{
			return null;
		}
		return new double[]{hitsPerKill * pools.get(0).perUse};
	}

	public String getKey()
	{
		return key;
	}

	/** The item's name as the panel shows it. */
	public String getName()
	{
		return name;
	}

	public Wear getWear()
	{
		return wear;
	}

	public List<Pool> getPools()
	{
		return pools;
	}

	/** What to do at the bank to top it up: "Add darts and Zulrah's scales". */
	public String getRecharge()
	{
		return recharge;
	}

	@Override
	public String toString()
	{
		return name;
	}

	// ---- The game's messages ----

	/** What one game message says about an item's charges. */
	public static final class Reading
	{
		private final Charged item;
		private final int[] values;
		private final boolean ranOut;

		Reading(Charged item, int[] values, boolean ranOut)
		{
			this.item = item;
			this.values = values;
			this.ranOut = ranOut;
		}

		/** The item, or null when the message names a kind of item the caller must pick (any trident). */
		public Charged getItem()
		{
			return item;
		}

		/** A value for each of the item's pools, or -1 where the message does not say. */
		public int[] getValues()
		{
			return values.clone();
		}

		/** Whether the message says it has run out. */
		public boolean isRanOut()
		{
			return ranOut;
		}

		/** The same reading for another item of the same kind, such as the trident the player wears. */
		public Reading as(Charged other)
		{
			return new Reading(other, values, ranOut);
		}

		/** Whether the message names a trident without saying which. */
		public boolean isAnyTrident()
		{
			return item == null;
		}
	}

	private static final String N = "([\\d,]+)";
	private static final Pattern BLOWPIPE_EMPTY = Pattern.compile("^Darts: None\\. Scales: " + N + " \\(");
	private static final Pattern BLOWPIPE = Pattern.compile("^Darts: .+? x " + N + "\\. Scales: " + N + " \\(");
	private static final Pattern HELM = Pattern.compile("^Scales: " + N + " \\(");
	private static final Pattern HELM_OUT = Pattern.compile("^Your serpentine helm has run out of Zulrah's scales\\.");
	private static final Pattern TRIDENT = Pattern.compile("^Your (Trident of the (?:seas|swamp)(?: \\(e\\))?) has (one|" + N + ") charges?\\.",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern ANY_TRIDENT_LOW = Pattern.compile("^Your trident only has " + N + " charges left!");
	private static final Pattern ANY_TRIDENT_OUT = Pattern.compile("^Your trident has run out of charges\\.");
	private static final Pattern SANG = Pattern.compile("^Your (Holy s|S)anguinesti staff has " + N + " charges? remaining\\.");
	private static final Pattern SANG_OUT = Pattern.compile("^Your (Holy s|S)anguinesti staff has run out of charges\\.");
	private static final Pattern SHADOW = Pattern.compile("^Tumeken's shadow (?:only )?has " + N + " charges? (?:remaining|left)");
	private static final Pattern SHADOW_OUT = Pattern.compile("^Tumeken's shadow has run out of charges\\.");
	private static final Pattern BOWFA = Pattern.compile("^Your bow of Faerdhinen has " + N + " charges? remaining\\.", Pattern.CASE_INSENSITIVE);
	private static final Pattern CRYSTAL = Pattern.compile("^Your crystal (helm|body|legs) has " + N + " charges? remaining\\.");

	/**
	 * What a game message, tags removed, says about a charged item; null when it says nothing.
	 * The serpentine helm's "Scales:" check does not name the helm, so the caller makes sure the
	 * player has one before taking it.
	 */
	public static Reading read(String message)
	{
		if (message == null)
		{
			return null;
		}
		String m = message.trim();
		Matcher x = BLOWPIPE_EMPTY.matcher(m);
		if (x.find())
		{
			return new Reading(TOXIC_BLOWPIPE, new int[]{0, num(x.group(1))}, false);
		}
		x = BLOWPIPE.matcher(m);
		if (x.find())
		{
			return new Reading(TOXIC_BLOWPIPE, new int[]{num(x.group(1)), num(x.group(2))}, false);
		}
		x = HELM.matcher(m);
		if (x.find())
		{
			return new Reading(SERPENTINE_HELM, new int[]{num(x.group(1))}, false);
		}
		if (HELM_OUT.matcher(m).find())
		{
			return new Reading(SERPENTINE_HELM, new int[]{0}, true);
		}
		x = TRIDENT.matcher(m);
		if (x.find())
		{
			Charged t = byItem(x.group(1));
			int n = "one".equalsIgnoreCase(x.group(2)) ? 1 : num(x.group(3));
			return t == null ? null : new Reading(t, new int[]{n}, false);
		}
		x = ANY_TRIDENT_LOW.matcher(m);
		if (x.find())
		{
			return new Reading(null, new int[]{num(x.group(1))}, false);
		}
		if (ANY_TRIDENT_OUT.matcher(m).find())
		{
			return new Reading(null, new int[]{0}, true);
		}
		x = SANG.matcher(m);
		if (x.find())
		{
			return new Reading(x.group(1).startsWith("Holy") ? HOLY_SANGUINESTI : SANGUINESTI, new int[]{num(x.group(2))}, false);
		}
		x = SANG_OUT.matcher(m);
		if (x.find())
		{
			return new Reading(x.group(1).startsWith("Holy") ? HOLY_SANGUINESTI : SANGUINESTI, new int[]{0}, true);
		}
		x = SHADOW.matcher(m);
		if (x.find())
		{
			return new Reading(TUMEKENS_SHADOW, new int[]{num(x.group(1))}, false);
		}
		if (SHADOW_OUT.matcher(m).find())
		{
			return new Reading(TUMEKENS_SHADOW, new int[]{0}, true);
		}
		x = BOWFA.matcher(m);
		if (x.find())
		{
			return new Reading(BOW_OF_FAERDHINEN, new int[]{num(x.group(1))}, false);
		}
		x = CRYSTAL.matcher(m);
		if (x.find())
		{
			Charged c = "helm".equals(x.group(1)) ? CRYSTAL_HELM : "body".equals(x.group(1)) ? CRYSTAL_BODY : CRYSTAL_LEGS;
			return new Reading(c, new int[]{num(x.group(2))}, false);
		}
		return null;
	}

	/** The tridents, for a message that says only "Your trident". */
	public static List<Charged> tridents()
	{
		return TRIDENTS;
	}

	private static int num(String s)
	{
		try
		{
			return Integer.parseInt(s.replace(",", ""));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}
}
