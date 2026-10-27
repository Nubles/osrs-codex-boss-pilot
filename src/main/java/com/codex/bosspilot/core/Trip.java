package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A trip to one boss as it happens: kills, how long each took, loot, deaths, and the food and
 * prayer the player has used and still carries. Supplies are counted only as they go down, so
 * picking up or taking out more does not read as having eaten less.
 */
public final class Trip
{
	private final Boss boss;
	private final long start;
	private long lastSeen;
	private long lastKillAt;
	private int kills;
	private final List<Double> killSeconds = new ArrayList<>();
	private long loot;
	private int deaths;
	private long foodUsed;
	private long dosesUsed;
	private long foodNow = -1;
	private long dosesNow = -1;
	/** A kill time the game sent before its kill count message, held for the kill it belongs to. */
	private double pendingSeconds;
	/** Whether the last kill's time came from the game rather than the clock. */
	private boolean lastFromGame;
	/** Whether the last kill holds a time read off the clock, which the game's own may replace. */
	private boolean lastClockTimed;
	/** A fight duration this soon after a kill count belongs to that kill. */
	static final long SAME_KILL_MS = 3000;

	Trip(Boss boss, long start)
	{
		this.boss = boss;
		this.start = start;
		this.lastSeen = start;
		this.lastKillAt = start;
	}

	void seen(long now)
	{
		lastSeen = Math.max(lastSeen, now);
	}

	/** The supplies carried now; any drop since the last look counts as used. */
	void carrying(long food, long doses)
	{
		if (foodNow >= 0 && food < foodNow)
		{
			foodUsed += foodNow - food;
		}
		if (dosesNow >= 0 && doses < dosesNow)
		{
			dosesUsed += dosesNow - doses;
		}
		foodNow = food;
		dosesNow = doses;
	}

	/** A kill; the time is the game's own when it gave one, else the time since the last kill. */
	void kill(long now, double gameSeconds)
	{
		kills++;
		lastFromGame = gameSeconds > 0 || pendingSeconds > 0;
		double seconds = gameSeconds > 0 ? gameSeconds : pendingSeconds > 0 ? pendingSeconds : (now - lastKillAt) / 1000.0;
		pendingSeconds = 0;
		// The clock only times a kill from the one before it: the first one would count the walk in.
		lastClockTimed = false;
		if (seconds > 0 && (lastFromGame || kills > 1))
		{
			killSeconds.add(seconds);
			lastClockTimed = !lastFromGame;
		}
		lastKillAt = now;
		seen(now);
	}

	/** The game's fight duration, which can come just before or just after the kill count. */
	void duration(double seconds, long now)
	{
		if (seconds <= 0)
		{
			return;
		}
		if (kills > 0 && !lastFromGame && now - lastKillAt <= SAME_KILL_MS)
		{
			// The kill came first: its guessed time, if it had one, gives way to the game's.
			if (lastClockTimed)
			{
				killSeconds.set(killSeconds.size() - 1, seconds);
			}
			else
			{
				killSeconds.add(seconds);
			}
			lastFromGame = true;
			lastClockTimed = false;
		}
		else
		{
			pendingSeconds = seconds;
		}
	}

	void loot(long value)
	{
		loot += Math.max(0, value);
	}

	void death()
	{
		deaths++;
	}

	public Boss getBoss()
	{
		return boss;
	}

	public long getStart()
	{
		return start;
	}

	public long getLastSeen()
	{
		return lastSeen;
	}

	public int getKills()
	{
		return kills;
	}

	public List<Double> getKillSeconds()
	{
		return Collections.unmodifiableList(killSeconds);
	}

	public long getLoot()
	{
		return loot;
	}

	public int getDeaths()
	{
		return deaths;
	}

	public long getFoodUsed()
	{
		return foodUsed;
	}

	public long getDosesUsed()
	{
		return dosesUsed;
	}

	/** Hitpoints of food carried, or -1 before the first look. */
	public long getFoodNow()
	{
		return foodNow;
	}

	public long getDosesNow()
	{
		return dosesNow;
	}

	public double averageKill()
	{
		double sum = 0;
		for (double s : killSeconds)
		{
			sum += s;
		}
		return killSeconds.isEmpty() ? 0 : sum / killSeconds.size();
	}

	public double bestKill()
	{
		double best = 0;
		for (double s : killSeconds)
		{
			best = best <= 0 ? s : Math.min(best, s);
		}
		return best;
	}

	public long elapsed(long now)
	{
		return Math.max(0, now - start);
	}

	public Logbook.TripRecord record()
	{
		Logbook.TripRecord r = new Logbook.TripRecord();
		r.start = start;
		r.end = lastSeen;
		r.kills = kills;
		r.loot = loot;
		r.deaths = deaths;
		r.foodUsed = foodUsed;
		r.dosesUsed = dosesUsed;
		r.bestKill = bestKill();
		return r;
	}
}
