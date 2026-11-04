package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts one Zalcano kill together from what the game shows around it: the kill count message, the
 * ground items marked as the player's own that appear in the prison a few ticks either side of
 * it, and the pet's message. The kill is ready a few ticks after its message, so loot that lands
 * a moment late still counts.
 */
public final class ZalcanoLoot
{
	/** Ticks before the kill message whose items still belong to it. */
	static final int BEFORE = 2;
	/** Ticks after the kill message to wait for its items. */
	static final int AFTER = 4;

	private static final class Seen
	{
		final int tick;
		final String name;
		final long quantity;
		final long worth;

		Seen(int tick, String name, long quantity, long worth)
		{
			this.tick = tick;
			this.name = name;
			this.quantity = quantity;
			this.worth = worth;
		}
	}

	private final List<Seen> items = new ArrayList<>();
	private int killTick = -1;
	private int kc = -1;
	/** The tick of the pet's message, or -1. */
	private int petTick = -1;

	/** A ground item of the player's own appeared in the prison. */
	public void item(int tick, String name, long quantity, long worth)
	{
		if (name == null || quantity <= 0)
		{
			return;
		}
		items.add(new Seen(tick, name, quantity, Math.max(0, worth)));
		trim(tick);
	}

	/** The kill count message for Zalcano. */
	public void kill(int tick, int count)
	{
		killTick = tick;
		kc = count;
	}

	/** Whether a kill is being put together. */
	public boolean pending()
	{
		return killTick >= 0;
	}

	/** The pet's message: it belongs to a kill whose message came a little before or comes a little after. */
	public void pet(int tick)
	{
		petTick = tick;
	}

	/** Forget everything, such as on leaving or logging out. */
	public void clear()
	{
		items.clear();
		killTick = -1;
		kc = -1;
		petTick = -1;
	}

	/**
	 * The kill, once its wait is over; null before then or when no kill is pending.
	 *
	 * @param seconds seconds since the kill before it on this trip, or -1
	 */
	public ZalcanoLog.Kill poll(int tick, long now, long seconds)
	{
		if (killTick < 0)
		{
			trim(tick);
			return null;
		}
		if (tick - killTick < AFTER)
		{
			return null;
		}
		ZalcanoLog.Kill k = new ZalcanoLog.Kill();
		k.at = now;
		k.kc = kc;
		k.seconds = seconds;
		Map<String, Long> counts = new LinkedHashMap<>();
		long worth = 0;
		boolean any = false;
		for (Seen s : items)
		{
			if (s.tick < killTick - BEFORE || s.tick > killTick + AFTER)
			{
				continue;
			}
			any = true;
			counts.merge(s.name, s.quantity, Long::sum);
			worth += s.worth;
		}
		for (Map.Entry<String, Long> e : counts.entrySet())
		{
			k.items.add(e.getValue() == 1 ? e.getKey() : Format.number(e.getValue()) + " x " + e.getKey());
			Zalcano.Unique u = Zalcano.Unique.byName(e.getKey());
			if (u != null && !k.uniques.contains(u.getName()))
			{
				k.uniques.add(u.getName());
			}
		}
		if (petTick >= 0 && petTick >= killTick - BEFORE && !k.uniques.contains(Zalcano.Unique.PET.getName()))
		{
			k.uniques.add(Zalcano.Unique.PET.getName());
		}
		k.value = any ? worth : -1;
		clear();
		return k;
	}

	private void trim(int tick)
	{
		items.removeIf(s -> s.tick < tick - BEFORE - AFTER);
		if (killTick < 0 && petTick >= 0 && petTick < tick - BEFORE)
		{
			petTick = -1;
		}
	}
}
