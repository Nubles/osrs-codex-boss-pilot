package com.codex.bosspilot.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * One line of a boss kit: what to bring and how much. Items are matched by their in-game name,
 * so every charge, dose and colour of an item counts without a list of ids:
 * <ul>
 * <li>{@link Kind#FOOD} is counted in hitpoints healed, from {@link Food}.</li>
 * <li>{@link Kind#DOSES} is counted in potion doses, from names such as "Prayer potion(3)".</li>
 * <li>{@link Kind#ITEM} is counted in items; a name ending in {@code *} matches anything that
 * starts with it, so "Ring of dueling*" takes every charge of the ring.</li>
 * </ul>
 */
public final class Supply
{
	public enum Kind
	{
		FOOD,
		DOSES,
		ITEM
	}

	/** How much a line matters: a missing must-have stops the trip, the rest only make it easier. */
	public enum Need
	{
		/** The fight cannot be done without it, such as a key or a dragonfire shield. */
		MUST,
		/** Suggested amounts of food and potions. */
		SUGGESTED,
		/** Nice to have, such as a teleport. */
		OPTIONAL
	}

	private final String label;
	private final Kind kind;
	private final Need need;
	private final int amount;
	private final List<String> names;
	private final boolean firstVisit;

	private Supply(String label, Kind kind, Need need, int amount, boolean firstVisit, String... names)
	{
		this.label = label;
		this.kind = kind;
		this.need = need;
		this.amount = amount;
		this.firstVisit = firstVisit;
		this.names = Collections.unmodifiableList(Arrays.asList(names));
	}

	/** Food worth this many hitpoints, in any mix of the foods {@link Food} knows. */
	public static Supply food(int hitpoints)
	{
		return new Supply("Food", Kind.FOOD, Need.SUGGESTED, hitpoints, false);
	}

	public static Supply doses(String label, int doses, String... potions)
	{
		return new Supply(label, Kind.DOSES, Need.SUGGESTED, doses, false, potions);
	}

	public static Supply must(String label, int amount, String... names)
	{
		return new Supply(label, Kind.ITEM, Need.MUST, amount, false, names);
	}

	/** A must-have potion, such as the super antifire for Vorkath. */
	public static Supply mustDoses(String label, int doses, String... potions)
	{
		return new Supply(label, Kind.DOSES, Need.MUST, doses, false, potions);
	}

	public static Supply optional(String label, int amount, String... names)
	{
		return new Supply(label, Kind.ITEM, Need.OPTIONAL, amount, false, names);
	}

	/** Needed only until the player has used it once, such as the rope tied at an entrance. */
	public static Supply firstVisit(String label, int amount, String... names)
	{
		return new Supply(label, Kind.ITEM, Need.OPTIONAL, amount, true, names);
	}

	public String getLabel()
	{
		return label;
	}

	public Kind getKind()
	{
		return kind;
	}

	public Need getNeed()
	{
		return need;
	}

	public int getAmount()
	{
		return amount;
	}

	public List<String> getNames()
	{
		return names;
	}

	public boolean isFirstVisit()
	{
		return firstVisit;
	}

	/** How many of this line one item stack gives: healing, doses or items, or 0 when it does not match. */
	public long worth(String itemName, long quantity)
	{
		if (itemName == null || quantity <= 0)
		{
			return 0;
		}
		switch (kind)
		{
			case FOOD:
				return Food.heal(itemName) * quantity;
			case DOSES:
				int doses = Dose.of(itemName);
				return doses > 0 && matches(Dose.base(itemName)) ? (long) doses * quantity : 0;
			default:
				return matches(itemName) ? quantity : 0;
		}
	}

	boolean matches(String itemName)
	{
		String name = itemName.toLowerCase(Locale.ROOT).trim();
		for (String want : names)
		{
			String w = want.toLowerCase(Locale.ROOT);
			if (w.endsWith("*") ? name.startsWith(w.substring(0, w.length() - 1)) : name.equals(w))
			{
				return true;
			}
		}
		return false;
	}

	/** "200 hp of food", "8 doses", "1 Spade". */
	public String amountText()
	{
		switch (kind)
		{
			case FOOD:
				return amount + " hp";
			case DOSES:
				return amount + (amount == 1 ? " dose" : " doses");
			default:
				return amount == 1 ? "1" : String.valueOf(amount);
		}
	}

	@Override
	public String toString()
	{
		return label + " " + amountText();
	}
}
