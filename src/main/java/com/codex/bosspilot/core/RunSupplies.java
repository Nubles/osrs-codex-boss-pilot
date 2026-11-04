package com.codex.bosspilot.core;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Counts the supplies one run uses, from what the player carries tick by tick: food by the piece
 * and potions by the dose ("Moonlight potion"), whether brought in or made in the caverns. Only a
 * small drop between two looks counts as used, a bite or a sip at a time; a larger drop, such as
 * a deposit at the bank or the cavern taking its supplies back, is not counted, and nor is
 * anything while the bank is open.
 */
public final class RunSupplies
{
	/** Units that may go in one look and still be eaten or drunk, not deposited. */
	static final int MAX_STEP = 3;

	private Map<String, Long> last;
	private final Map<String, Long> used = new LinkedHashMap<>();

	/** The supplies carried now, by item name; the bank being open makes this look not count. */
	public void observe(Map<String, Long> carried, boolean bankOpen)
	{
		Map<String, Long> now = units(carried);
		if (last != null && !bankOpen)
		{
			long drop = 0;
			Map<String, Long> step = new HashMap<>();
			for (Map.Entry<String, Long> e : last.entrySet())
			{
				long d = e.getValue() - now.getOrDefault(e.getKey(), 0L);
				if (d > 0)
				{
					step.put(e.getKey(), d);
					drop += d;
				}
			}
			if (drop > 0 && drop <= MAX_STEP)
			{
				step.forEach((k, v) -> used.merge(k, v, Long::sum));
			}
		}
		last = now;
	}

	/** What this run used so far, by name. */
	public Map<String, Long> used()
	{
		return new LinkedHashMap<>(used);
	}

	/** Starts the next run, keeping the look at what is carried now. */
	public void nextRun()
	{
		used.clear();
	}

	/** Forgets everything, such as on leaving or another account. */
	public void clear()
	{
		used.clear();
		last = null;
	}

	/** Food by the piece and potions by the dose, under the potion's name without its dose. */
	static Map<String, Long> units(Map<String, Long> carried)
	{
		Map<String, Long> out = new HashMap<>();
		if (carried == null)
		{
			return out;
		}
		for (Map.Entry<String, Long> e : carried.entrySet())
		{
			String name = e.getKey();
			if (!Moons.supply(name))
			{
				continue;
			}
			int doses = Dose.of(name);
			if (doses > 0)
			{
				out.merge(Dose.base(name), e.getValue() * doses, Long::sum);
			}
			else
			{
				out.merge(name, e.getValue(), Long::sum);
			}
		}
		return out;
	}
}
