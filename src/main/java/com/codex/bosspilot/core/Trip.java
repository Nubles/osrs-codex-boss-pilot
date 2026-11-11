package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	/** For each kill time, whether the game gave it (a fight duration) rather than the clock. */
	private final List<Boolean> gameTimed = new ArrayList<>();
	/** Seconds a kill should take with the best setup from the gear brought, or -1 when not worked out. */
	private double theory = -1;
	/** The game's kill count at the last kill, or -1. */
	private int kc = -1;
	private long loot;
	private int deaths;
	private long foodUsed;
	private long dosesUsed;
	private long foodNow = -1;
	private long dosesNow = -1;
	private long damage;
	private int biggestHit;
	private int hits;
	/** Must-have kit lines carried at some point on this trip, by label. */
	private final Set<String> mustSeen = new HashSet<>();
	/** The biggest boost seen on this trip, by skill name, for the boost reminders. */
	private final Map<String, Integer> boostPeaks = new HashMap<>();
	/** A kill time the game sent before its kill count message, held for the kill it belongs to. */
	private double pendingSeconds;
	/** Whether the last kill's time came from the game rather than the clock. */
	private boolean lastFromGame;
	/** Whether the last kill holds a time read off the clock, which the game's own may replace. */
	private boolean lastClockTimed;
	/** Whether the player died since the last kill. */
	private boolean diedSinceKill;
	/** What the player carried the last time the boss was in sight, or -1: what they left with. */
	private long leaveFood = -1;
	private long leaveDoses = -1;
	private int leaveFree = -1;
	private long leaveFoodUsed;
	private long leaveDosesUsed;
	/** The charged item that ran out on this trip, such as "Toxic blowpipe darts", or null. */
	private String ranOut;
	/** Why the tracker ended the trip, set as it ends. */
	private Debrief.Cause cause = Debrief.Cause.TIMED_OUT;
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
		carry(food, doses);
	}

	/**
	 * The supplies and free inventory slots carried now. When the boss is in sight this tick,
	 * they are also what the player will have left with if it is the last time it is seen; what
	 * goes after that (a bank deposit on the way home, say) is not counted as used on the trip.
	 *
	 * @param free free inventory slots, or -1 when not known
	 */
	void carrying(long food, long doses, int free, long now)
	{
		carry(food, doses);
		if (now == lastSeen)
		{
			leaveFood = food;
			leaveDoses = doses;
			leaveFree = free;
			leaveFoodUsed = foodUsed;
			leaveDosesUsed = dosesUsed;
		}
	}

	private void carry(long food, long doses)
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
		diedSinceKill = false;
		lastFromGame = gameSeconds > 0 || pendingSeconds > 0;
		double seconds = gameSeconds > 0 ? gameSeconds : pendingSeconds > 0 ? pendingSeconds : (now - lastKillAt) / 1000.0;
		pendingSeconds = 0;
		// The clock only times a kill from the one before it: the first one would count the walk in.
		lastClockTimed = false;
		if (seconds > 0 && (lastFromGame || kills > 1))
		{
			killSeconds.add(seconds);
			gameTimed.add(lastFromGame);
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
				gameTimed.set(gameTimed.size() - 1, true);
			}
			else
			{
				killSeconds.add(seconds);
				gameTimed.add(true);
			}
			lastFromGame = true;
			lastClockTimed = false;
		}
		else
		{
			pendingSeconds = seconds;
		}
	}

	/** The game's kill count at the kill just counted. */
	void kc(int count)
	{
		kc = Math.max(kc, count);
	}

	/** Seconds a kill should take with the best setup from the gear brought; -1 when not known. */
	void theory(double seconds)
	{
		theory = seconds > 0 && !Double.isInfinite(seconds) ? seconds : -1;
	}

	public double getTheory()
	{
		return theory;
	}

	/** The kill times the game gave itself, oldest first: the fight durations, never the clock's. */
	public List<Double> getGameSeconds()
	{
		List<Double> out = new ArrayList<>();
		for (int i = 0; i < killSeconds.size(); i++)
		{
			if (gameTimed.get(i))
			{
				out.add(killSeconds.get(i));
			}
		}
		return out;
	}

	void loot(long value)
	{
		loot += Math.max(0, value);
	}

	void death()
	{
		deaths++;
		diedSinceKill = true;
	}

	/** The game said a charged item the player carries has run out; null when it was topped up again. */
	void ranOut(String what)
	{
		ranOut = what;
	}

	/** The charged item that ran out on this trip, or null. */
	public String getRanOut()
	{
		return ranOut;
	}

	void endedBy(Debrief.Cause cause)
	{
		this.cause = cause;
	}

	public Debrief.Cause getCause()
	{
		return cause;
	}

	/** Whether a death came after the last kill, so it is what ended the trip. */
	public boolean diedLast()
	{
		return diedSinceKill;
	}

	/** Free inventory slots when the boss was last in sight, or -1. */
	public int getLeaveFree()
	{
		return leaveFree;
	}

	/** Hitpoints of food carried when the boss was last in sight, or -1. */
	public long getLeaveFood()
	{
		return leaveFood;
	}

	public long getLeaveDoses()
	{
		return leaveDoses;
	}

	/**
	 * How many more kills the food and prayer left with would have lasted, at this trip's own use
	 * a kill, and which ran short first; null when not known.
	 */
	public Debrief.Left left()
	{
		if (kills <= 0 || leaveFood < 0)
		{
			return null;
		}
		double byFood = leaveFoodUsed > 0 ? leaveFood / ((double) leaveFoodUsed / kills) : Double.POSITIVE_INFINITY;
		double byDoses = leaveDosesUsed > 0 ? leaveDoses / ((double) leaveDosesUsed / kills) : Double.POSITIVE_INFINITY;
		if (Double.isInfinite(byFood) && Double.isInfinite(byDoses))
		{
			return null;
		}
		return byFood <= byDoses ? new Debrief.Left(byFood, false) : new Debrief.Left(byDoses, true);
	}

	/** Damage the player took. */
	void hit(int amount)
	{
		if (amount > 0)
		{
			damage += amount;
			hits++;
			biggestHit = Math.max(biggestHit, amount);
		}
	}

	/** Notes which must-have lines are carried now; once carried, a line counts for the whole trip. */
	void mustCarried(Set<String> labels)
	{
		mustSeen.addAll(labels);
	}

	/** The must-have lines not carried at any point so far, only until the first kill. */
	public List<String> missingMust(List<String> must)
	{
		List<String> out = new ArrayList<>();
		if (kills > 0)
		{
			return out;
		}
		for (String m : must)
		{
			if (!mustSeen.contains(m))
			{
				out.add(m);
			}
		}
		return out;
	}

	public Map<String, Integer> getBoostPeaks()
	{
		return boostPeaks;
	}

	public long getDamage()
	{
		return damage;
	}

	public int getBiggestHit()
	{
		return biggestHit;
	}

	public int getHits()
	{
		return hits;
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
		r.foodUsed = leaveFood >= 0 ? leaveFoodUsed : foodUsed;
		r.dosesUsed = leaveFood >= 0 ? leaveDosesUsed : dosesUsed;
		r.bestKill = bestKill();
		r.damage = damage;
		r.biggestHit = biggestHit;
		Debrief.Left left = left();
		r.killsLeft = left == null ? -1 : left.kills;
		r.ended = Debrief.ending(this, left).name();
		r.outOf = ranOut;
		List<Double> game = getGameSeconds();
		r.timed = game.size();
		double sum = 0;
		for (double s : game)
		{
			sum += s;
		}
		r.fightSeconds = Math.round(sum * 10) / 10.0;
		r.theory = theory > 0 ? Math.round(theory * 10) / 10.0 : -1;
		r.kcEnd = kc;
		return r;
	}
}
