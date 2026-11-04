package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reminders on a trip that look only at the player: hitpoints and prayer under the levels they
 * set, boosts and antifire wearing off, and a must-have item left at home. Nothing here reads
 * the boss, so nothing here says what the boss is doing or what to do about it.
 */
public final class Reminders
{
	/** A boost this big counts as having been drunk. */
	static final int BOOSTED = 4;
	/** At or under this, a boost that was drunk has worn off. */
	static final int WORN_OFF = 1;

	/** One reminder; urgent ones are about staying alive. */
	public static final class Reminder
	{
		private final String text;
		private final boolean urgent;

		Reminder(String text, boolean urgent)
		{
			this.text = text;
			this.urgent = urgent;
		}

		public String getText()
		{
			return text;
		}

		public boolean isUrgent()
		{
			return urgent;
		}

		@Override
		public String toString()
		{
			return text;
		}
	}

	/** What the player is like right now. */
	public static final class Self
	{
		public int hitpoints;
		public int maxHitpoints;
		public int prayer;
		public int maxPrayer;
		/** Hitpoints of food carried. */
		public long food;
		/** Doses of prayer restoring potions carried. */
		public long doses;
		/** The antifire varbit, the larger of the plain and super ones: 0 when none is active, 1 when it is about to end. */
		public int antifire;
		public boolean needsAntifire;
		/** Combat skills by name, with how far above the base level they are now. */
		public final Map<String, Integer> boosts = new LinkedHashMap<>();
		/** Must-have kit not carried at any point on this trip, shown only before the first kill. */
		public List<String> missing = Collections.emptyList();
	}

	/** The levels the player set. */
	public static final class Limits
	{
		/** Remind to eat under this share of hitpoints, in percent; 0 turns it off. */
		public int eatPercent = 50;
		/** Remind to drink under this many prayer points; 0 turns it off. */
		public int prayerPoints = 10;
		public boolean boosts = true;
	}

	private Reminders()
	{
	}

	/**
	 * @param peaks the biggest boost seen on this trip by skill name, kept by the caller for the
	 *              length of the trip; this records new peaks in it
	 */
	public static List<Reminder> of(Self me, Limits limits, Map<String, Integer> peaks)
	{
		List<Reminder> out = new ArrayList<>();
		for (String name : me.missing)
		{
			out.add(new Reminder("Not carrying: " + name, true));
		}
		if (limits.eatPercent > 0 && me.maxHitpoints > 0 && me.hitpoints > 0
			&& me.hitpoints * 100 < limits.eatPercent * me.maxHitpoints)
		{
			out.add(new Reminder(me.food > 0
				? "Eat: " + me.hitpoints + " of " + me.maxHitpoints + " hitpoints"
				: "Low hitpoints and no food left: time to leave", true));
		}
		if (limits.prayerPoints > 0 && me.maxPrayer > 0 && me.prayer < limits.prayerPoints && me.prayer < me.maxPrayer)
		{
			out.add(new Reminder(me.doses > 0
				? "Drink: " + me.prayer + " prayer points left"
				: "Prayer is low and no potions are left", me.doses > 0));
		}
		if (me.needsAntifire)
		{
			if (me.antifire == 1)
			{
				out.add(new Reminder("Antifire is about to run out", true));
			}
			else if (me.antifire <= 0)
			{
				out.add(new Reminder("No antifire active", true));
			}
		}
		if (limits.boosts)
		{
			for (Map.Entry<String, Integer> e : me.boosts.entrySet())
			{
				int now = e.getValue();
				int peak = Math.max(peaks.getOrDefault(e.getKey(), 0), now);
				peaks.put(e.getKey(), peak);
				if (peak >= BOOSTED && now <= WORN_OFF)
				{
					out.add(new Reminder(e.getKey() + " boost has worn off", false));
				}
			}
		}
		return out;
	}
}
