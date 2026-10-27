package com.codex.bosspilot.core;

/**
 * Starts, feeds and ends trips from what the plugin sees: a boss nearby, kill counts and fight
 * durations from chat, loot, deaths and the supplies the player carries. A trip ends once no
 * boss of it has been seen for a while; the finished trip is handed back once to be saved.
 */
public final class TripTracker
{
	/** A trip ends this long after its boss was last seen or killed. */
	static final long END_AFTER_MS = 5 * 60 * 1000;

	private Trip trip;
	private Trip finished;

	public Trip current()
	{
		return trip;
	}

	/**
	 * One look at the world, every few ticks.
	 *
	 * @param nearby the boss seen near the player, or null
	 * @param food hitpoints of food carried
	 * @param doses doses of prayer restoring potions carried
	 */
	public void observe(Boss nearby, long food, long doses, long now)
	{
		if (nearby != null)
		{
			begin(nearby, now);
			trip.seen(now);
		}
		if (trip != null)
		{
			if (now - trip.getLastSeen() > END_AFTER_MS)
			{
				end();
				return;
			}
			trip.carrying(food, doses);
		}
	}

	/** The game counted a kill. */
	public void kill(Boss boss, long now)
	{
		begin(boss, now);
		trip.kill(now, 0);
	}

	public void duration(double seconds, long now)
	{
		if (trip != null)
		{
			trip.duration(seconds, now);
		}
	}

	public void loot(Boss boss, long value, long now)
	{
		if (trip != null && trip.getBoss() == boss)
		{
			trip.loot(value);
			trip.seen(now);
		}
	}

	public void death()
	{
		if (trip != null)
		{
			trip.death();
		}
	}

	/** Ends the trip now, as on logging out. */
	public void end()
	{
		if (trip != null)
		{
			finished = trip;
			trip = null;
		}
	}

	/** Drops the trip without saving it, as when the account changes. */
	public void abandon()
	{
		trip = null;
		finished = null;
	}

	/** The trip that just ended, once; null after that. */
	public Trip takeFinished()
	{
		Trip t = finished;
		finished = null;
		return t;
	}

	private void begin(Boss boss, long now)
	{
		if (trip != null && trip.getBoss() != boss)
		{
			end();
		}
		if (trip == null)
		{
			trip = new Trip(boss, now);
		}
	}
}
