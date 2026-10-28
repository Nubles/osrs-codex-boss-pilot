package com.codex.bosspilot.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * How much the common bossing foods heal. Anglerfish heals more the higher the player's
 * hitpoints; it is counted at what it heals around 90 hitpoints. A Saradomin brew is counted
 * per dose at what one heals at 99 hitpoints.
 */
public final class Food
{
	private static final Map<String, Integer> HEALS = new HashMap<>();

	static
	{
		put("Manta ray", 22);
		put("Dark crab", 22);
		put("Tuna potato", 22);
		put("Anglerfish", 21);
		put("Sea turtle", 21);
		put("Shark", 20);
		put("Cooked karambwan", 18);
		put("Monkfish", 16);
		put("Potato with cheese", 16);
		put("Swordfish", 14);
		put("Bass", 13);
		put("Lobster", 12);
		put("Tuna", 10);
		put("Salmon", 9);
		put("Trout", 7);
	}

	/** Hitpoints one Saradomin brew dose heals at 99 hitpoints. */
	static final int BREW_DOSE = 16;

	private Food()
	{
	}

	private static void put(String name, int heal)
	{
		HEALS.put(name.toLowerCase(Locale.ROOT), heal);
	}

	/** What one of this item heals, or 0 when it is not food. */
	public static int heal(String itemName)
	{
		if (itemName == null)
		{
			return 0;
		}
		Integer heal = HEALS.get(itemName.toLowerCase(Locale.ROOT).trim());
		if (heal != null)
		{
			return heal;
		}
		int doses = Dose.of(itemName);
		return doses > 0 && "saradomin brew".equalsIgnoreCase(Dose.base(itemName)) ? doses * BREW_DOSE : 0;
	}

	/** True for anything {@link #heal} counts. */
	public static boolean isFood(String itemName)
	{
		return heal(itemName) > 0;
	}
}
