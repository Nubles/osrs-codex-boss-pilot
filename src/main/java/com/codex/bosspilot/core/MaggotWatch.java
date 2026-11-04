package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts one Maggot King kill together from what the game shows around it, after the fight: the kill
 * count (from the chat line or the kill count the game keeps, whichever comes first, counted once),
 * the option the player picked on the corpse, the loot the game hands over, the take-eggs line
 * that gives nothing, and the pet's message. It never looks at the fight itself.
 *
 * <p>A kill is ready a couple of ticks after its loot, so a pet message on the same tick still
 * joins it. A kill whose loot never comes (the player walked off) is logged without loot when the
 * next kill begins or the trip ends. Loot just after an egg is popped is the egg's, not a kill's.
 */
public final class MaggotWatch
{
	/** Ticks after the loot to wait for anything else of the same kill. */
	static final int SETTLE = 2;
	/** Ticks after a kill was logged without a kill count in which a late kill count still joins it. */
	static final int JOIN = 10;
	/** Ticks after popping an egg in which loot or the pet's message is the egg's. */
	public static final int POP_TICKS = 3;

	/** One drop of the loot. */
	public static final class Drop
	{
		final String name;
		final long quantity;
		final long worth;

		public Drop(String name, long quantity, long worth)
		{
			this.name = name;
			this.quantity = quantity;
			this.worth = worth;
		}
	}

	/** What happened, for the plugin to put in the log. */
	public static final class Event
	{
		/** A kill to add, or null. */
		public final MaggotLog.Kill kill;
		/** A pet that came on its own, or null. */
		public final MaggotKing.Unique late;
		/** Whether the late pet belongs to the last kill logged (else to an egg taken earlier). */
		public final boolean ofLastKill;

		Event(MaggotLog.Kill kill, MaggotKing.Unique late, boolean ofLastKill)
		{
			this.kill = kill;
			this.late = late;
			this.ofLastKill = ofLastKill;
		}
	}

	private static final class Pending
	{
		final long at;
		int kc = -1;
		MaggotKing.Choice choice;
		boolean looted;
		int lootTick;
		long value = -1;
		final Map<String, Long> items = new LinkedHashMap<>();
		final List<String> uniques = new ArrayList<>();

		Pending(long at)
		{
			this.at = at;
		}
	}

	private Pending pending;
	private final List<Event> out = new ArrayList<>();
	/** The highest kill count seen, so the chat line and the game's own count are taken once. */
	private int lastKc = -1;
	/** The last kill handed out, and the tick, so a late kill count joins it. */
	private MaggotLog.Kill lastOut;
	private int lastOutTick = -1000;
	private int popTick = -1000;

	/** The kill count as it stands, without a kill: on logging in, say. */
	public void baseline(int count)
	{
		lastKc = Math.max(lastKc, count);
	}

	/** The highest kill count seen, or -1. */
	public int getLastKc()
	{
		return lastKc;
	}

	/**
	 * A kill count: from the chat line or the game's own count. A count already seen is the other
	 * source of the same kill, and is ignored.
	 *
	 * @return whether it was a new kill
	 */
	public boolean kc(int tick, long now, int count)
	{
		if (lastKc >= 0 && count <= lastKc)
		{
			return false;
		}
		lastKc = count;
		if (pending != null && pending.kc < 0)
		{
			// The corpse was clicked or looted first: this count is that kill's.
			pending.kc = count;
			return true;
		}
		if (pending == null && lastOut != null && lastOut.kc < 0 && tick - lastOutTick <= JOIN)
		{
			lastOut.kc = count;
			return true;
		}
		flush(tick);
		pending = new Pending(now);
		pending.kc = count;
		return true;
	}

	/** The player picked an option on the corpse. */
	public void choice(int tick, long now, MaggotKing.Choice c)
	{
		if (c == null)
		{
			return;
		}
		if (pending != null && pending.looted)
		{
			flush(tick);
		}
		if (pending == null)
		{
			pending = new Pending(now);
		}
		pending.choice = c;
	}

	/** The player popped a maggot egg. */
	public void pop(int tick)
	{
		popTick = tick;
	}

	/** Loot the game handed over from the Maggot King. */
	public void loot(int tick, long now, List<Drop> drops)
	{
		if (tick - popTick <= POP_TICKS)
		{
			// The egg's own reward: only the pet counts, and it belongs to that egg's kill, logged before.
			for (Drop d : drops)
			{
				if (MaggotKing.Unique.byName(d.name) == MaggotKing.Unique.PET)
				{
					out.add(new Event(null, MaggotKing.Unique.PET, false));
				}
			}
			return;
		}
		if (pending != null && pending.looted)
		{
			flush(tick);
		}
		if (pending == null)
		{
			pending = new Pending(now);
		}
		long worth = 0;
		boolean egg = false;
		for (Drop d : drops)
		{
			if (d.name == null || d.quantity <= 0)
			{
				continue;
			}
			pending.items.merge(d.name, d.quantity, Long::sum);
			worth += Math.max(0, d.worth);
			egg |= MaggotKing.isEggName(d.name);
			MaggotKing.Unique u = MaggotKing.Unique.byName(d.name);
			if (u != null && !pending.uniques.contains(u.name()))
			{
				pending.uniques.add(u.name());
			}
		}
		pending.value = Math.max(0, pending.value) + worth;
		pending.looted = true;
		pending.lootTick = tick;
		if (pending.choice == null)
		{
			pending.choice = infer(pending.items, egg);
		}
	}

	/** The take-eggs roll that gives nothing. */
	public void eggsNothing(int tick, long now)
	{
		if (pending != null && pending.looted)
		{
			flush(tick);
		}
		if (pending == null)
		{
			pending = new Pending(now);
		}
		pending.choice = MaggotKing.Choice.EGGS;
		pending.value = Math.max(0, pending.value);
		pending.looted = true;
		pending.lootTick = tick;
	}

	/** The pet's message. */
	public void pet(int tick)
	{
		if (tick - popTick <= POP_TICKS)
		{
			out.add(new Event(null, MaggotKing.Unique.PET, false));
		}
		else if (pending != null)
		{
			if (!pending.uniques.contains(MaggotKing.Unique.PET.name()))
			{
				pending.uniques.add(MaggotKing.Unique.PET.name());
			}
		}
		else
		{
			out.add(new Event(null, MaggotKing.Unique.PET, lastOut != null && tick - lastOutTick <= JOIN));
		}
	}

	/** Whether a kill is being put together. */
	public boolean pending()
	{
		return pending != null;
	}

	/** Each tick: hands out what is ready. */
	public List<Event> poll(int tick)
	{
		if (pending != null && pending.looted && tick - pending.lootTick >= SETTLE)
		{
			flush(tick);
		}
		return drain();
	}

	/** Logs a kill being put together as it stands, such as when the trip ends, and hands out everything. */
	public List<Event> end(int tick)
	{
		flush(tick);
		return drain();
	}

	/** Forget everything but the kill count, such as on logging out. */
	public void clear()
	{
		pending = null;
		out.clear();
		lastOut = null;
		popTick = -1000;
	}

	/** Forget the kill count too, such as on changing account. */
	public void reset()
	{
		clear();
		lastKc = -1;
	}

	private List<Event> drain()
	{
		if (out.isEmpty())
		{
			return java.util.Collections.emptyList();
		}
		List<Event> e = new ArrayList<>(out);
		out.clear();
		return e;
	}

	private void flush(int tick)
	{
		if (pending == null)
		{
			return;
		}
		Pending p = pending;
		pending = null;
		MaggotLog.Kill k = new MaggotLog.Kill();
		k.at = p.at;
		k.kc = p.kc;
		k.choice = p.choice == null ? null : p.choice.name();
		k.value = p.looted ? p.value : -1;
		for (Map.Entry<String, Long> e : p.items.entrySet())
		{
			k.items.add(e.getValue() == 1 ? e.getKey() : Format.number(e.getValue()) + " x " + e.getKey());
		}
		k.uniques.addAll(p.uniques);
		lastOut = k;
		lastOutTick = tick;
		out.add(new Event(k, null, false));
	}

	/**
	 * How the corpse was looted, from the loot alone: eggs come only from taking eggs, and anything
	 * but the shared supply drop (stymphike tartare with a dull ancient medal) or an elite clue only
	 * from the stomach. Null when the loot could be either.
	 */
	static MaggotKing.Choice infer(Map<String, Long> items, boolean egg)
	{
		if (egg)
		{
			return MaggotKing.Choice.EGGS;
		}
		for (String name : items.keySet())
		{
			if (!SHARED.contains(name.toLowerCase(java.util.Locale.ROOT)) && MaggotKing.Unique.byName(name) != MaggotKing.Unique.PET)
			{
				return MaggotKing.Choice.STOMACH;
			}
		}
		return null;
	}

	/** Maggot King: drops on both options (lower-case names). */
	private static final java.util.Set<String> SHARED = new java.util.HashSet<>(java.util.Arrays.asList(
		"stymphike tartare", "dull ancient medal", "clue scroll (elite)", "scroll box (elite)"));
}
