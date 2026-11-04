package com.codex.bosspilot.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Items by their in-game name and how many: what is banked, carried or worn. Boss Pilot keeps
 * one for the bank and one for what the player has on them, so a kit can say both "you own
 * enough" and "take these out".
 */
public final class Stock
{
	public static final Stock EMPTY = new Stock(Collections.emptyMap());

	private final Map<String, Long> items;

	private Stock(Map<String, Long> items)
	{
		this.items = items;
	}

	public static Stock of(Map<String, Long> items)
	{
		Map<String, Long> copy = new HashMap<>();
		if (items != null)
		{
			items.forEach((name, n) ->
			{
				if (name != null && n != null && n > 0)
				{
					copy.merge(name, n, Long::sum);
				}
			});
		}
		return new Stock(Collections.unmodifiableMap(copy));
	}

	/** Both stocks together. */
	public Stock plus(Stock other)
	{
		if (other == null || other.items.isEmpty())
		{
			return this;
		}
		Map<String, Long> sum = new HashMap<>(items);
		other.items.forEach((name, n) -> sum.merge(name, n, Long::sum));
		return new Stock(Collections.unmodifiableMap(sum));
	}

	/** How much of a kit line this stock holds: healing, doses or items. */
	public long count(Supply line)
	{
		long n = 0;
		for (Map.Entry<String, Long> e : items.entrySet())
		{
			n += line.worth(e.getKey(), e.getValue());
		}
		return n;
	}

	/** Hitpoints of food held. */
	public long foodHeal()
	{
		long n = 0;
		for (Map.Entry<String, Long> e : items.entrySet())
		{
			n += (long) Food.heal(e.getKey()) * e.getValue();
		}
		return n;
	}

	/** Doses of any of these potions held. */
	public long doses(String... potions)
	{
		return count(Supply.doses("", 0, potions));
	}

	public long get(String name)
	{
		return items.getOrDefault(name, 0L);
	}

	public boolean isEmpty()
	{
		return items.isEmpty();
	}

	public Map<String, Long> asMap()
	{
		return items;
	}
}
