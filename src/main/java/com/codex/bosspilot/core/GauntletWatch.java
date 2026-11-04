package com.codex.bosspilot.core;

/**
 * Follows one Gauntlet run from what the game shows, a tick at a time: the run begins when the
 * game marks the player as in the Gauntlet, the preparation ends when it marks the Hunllef's
 * fight as started (or the Hunllef is first in sight), and the run ends on the game's completion
 * count line, the player's death, or leaving. The game's own run time and preparation time lines
 * take the place of the tick count when they come. Nothing here reads the fight itself: only when
 * it began and when it ended.
 */
public final class GauntletWatch
{
	/** Ticks after the completion line to wait for the game's time lines and the pet's message. */
	static final int SETTLE_TICKS = 3;

	public enum Phase
	{
		/** Not in a run. */
		NONE,
		PREP,
		FIGHT,
		/** The run has ended and is being put together. */
		ENDING
	}

	private boolean active;
	/** A run was just logged while the game still marks the player as inside: wait for them to leave. */
	private boolean done;
	private int startTick = -1;
	private long startMs;
	private int fightTick = -1;
	private int endTick = -1;
	private Gauntlet.Mode mode = Gauntlet.Mode.REGULAR;
	private GauntletLog.Outcome outcome;
	private int commitAt = -1;
	private int kc = -1;
	private double gameTotal = -1;
	private double gamePrep = -1;
	private boolean pet;
	private long lastStartMs;

	/**
	 * One tick.
	 *
	 * @param inGauntlet whether the game marks the player as in the Gauntlet
	 * @param bossStarted whether the game marks the Hunllef's fight as started, or the Hunllef is in sight
	 * @param hint the mode the game shows this tick (the corrupted varbit, the instance's region, the Hunllef), or null
	 * @return a finished run to log, or null
	 */
	public GauntletLog.Run tick(int tick, long now, boolean inGauntlet, boolean bossStarted, Gauntlet.Mode hint)
	{
		if (done && !inGauntlet)
		{
			done = false;
		}
		if (!active && !done && inGauntlet)
		{
			begin(tick, now, hint);
		}
		if (active)
		{
			if (hint != null && outcome == null)
			{
				mode = hint;
			}
			if (bossStarted && fightTick < 0 && outcome == null)
			{
				fightTick = tick;
			}
			if (!inGauntlet && outcome == null)
			{
				end(GauntletLog.Outcome.LEFT, tick, tick);
			}
		}
		if (outcome != null && tick >= commitAt)
		{
			GauntletLog.Run r = build();
			done = inGauntlet;
			reset();
			return r;
		}
		return null;
	}

	private void begin(int tick, long now, Gauntlet.Mode hint)
	{
		reset();
		active = true;
		startTick = tick;
		startMs = now;
		mode = hint == null ? Gauntlet.Mode.REGULAR : hint;
	}

	private void end(GauntletLog.Outcome how, int tick, int commit)
	{
		outcome = how;
		endTick = tick;
		commitAt = commit;
	}

	/** The game's completion count line: the Hunllef is down. */
	public void completion(int tick, long now, Gauntlet.Mode m, int count)
	{
		if (outcome != null && outcome != GauntletLog.Outcome.COMPLETED)
		{
			return;
		}
		if (!active)
		{
			// The start was not seen (Boss Pilot came on mid-run): the game's lines still time it.
			reset();
			active = true;
			startMs = now;
		}
		mode = m == null ? mode : m;
		kc = count;
		if (outcome == null)
		{
			end(GauntletLog.Outcome.COMPLETED, tick, tick + SETTLE_TICKS);
		}
	}

	/** The game's run time line. */
	public void duration(Gauntlet.Time t)
	{
		if (t != null && (active || outcome != null))
		{
			gameTotal = t.seconds;
			if (t.mode != null)
			{
				mode = t.mode;
			}
		}
	}

	/** The game's preparation time line. */
	public void preparation(double seconds)
	{
		if (seconds > 0 && active)
		{
			gamePrep = seconds;
		}
	}

	/** The player died. */
	public void died(int tick)
	{
		if (active && outcome == null)
		{
			end(GauntletLog.Outcome.DIED, tick, tick);
		}
	}

	/** The player logged out or hopped: the run is over. */
	public GauntletLog.Run abort(int tick)
	{
		if (!active)
		{
			done = false;
			return null;
		}
		if (outcome == null)
		{
			end(GauntletLog.Outcome.LEFT, tick, tick);
		}
		GauntletLog.Run r = build();
		reset();
		done = false;
		return r;
	}

	/**
	 * The pet's message.
	 *
	 * @return whether it belongs to the run under way; false means it is for the run just logged
	 */
	public boolean pet()
	{
		if (active && outcome == GauntletLog.Outcome.COMPLETED)
		{
			pet = true;
			return true;
		}
		return false;
	}

	private GauntletLog.Run build()
	{
		GauntletLog.Run r = new GauntletLog.Run();
		r.at = startMs;
		r.mode = mode;
		r.outcome = outcome;
		r.kc = kc;
		boolean ticked = startTick >= 0;
		double prep = -1;
		if (gamePrep > 0)
		{
			prep = gamePrep;
		}
		else if (ticked && fightTick >= 0)
		{
			prep = Gauntlet.ticks(fightTick - startTick);
		}
		else if (ticked && fightTick < 0)
		{
			// Never reached the Hunllef: the preparation lasted the whole run.
			prep = Gauntlet.ticks(endTick - startTick);
		}
		double fight = -1;
		if (gameTotal > 0 && prep > 0 && gameTotal > prep && (fightTick >= 0 || outcome == GauntletLog.Outcome.COMPLETED))
		{
			fight = gameTotal - prep;
		}
		else if (fightTick >= 0)
		{
			fight = Gauntlet.ticks(endTick - fightTick);
		}
		r.prep = prep > 0 ? prep : -1;
		r.fight = fight > 0 ? fight : -1;
		if (gameTotal > 0)
		{
			r.total = gameTotal;
		}
		else if (ticked)
		{
			r.total = Gauntlet.ticks(endTick - startTick);
		}
		r.gameTimed = gameTotal > 0;
		if (ticked && lastStartMs > 0 && startMs > lastStartMs)
		{
			r.cycle = (startMs - lastStartMs) / 1000.0;
		}
		if (ticked)
		{
			lastStartMs = startMs;
		}
		if (pet)
		{
			r.uniques.add(Gauntlet.Unique.PET.getName());
		}
		return r;
	}

	private void reset()
	{
		active = false;
		startTick = -1;
		startMs = 0;
		fightTick = -1;
		endTick = -1;
		mode = Gauntlet.Mode.REGULAR;
		outcome = null;
		commitAt = -1;
		kc = -1;
		gameTotal = -1;
		gamePrep = -1;
		pet = false;
	}

	/** A new sitting: the pace starts afresh. */
	public void newSitting()
	{
		lastStartMs = 0;
	}

	public Phase phase()
	{
		if (!active)
		{
			return Phase.NONE;
		}
		if (outcome != null)
		{
			return Phase.ENDING;
		}
		return fightTick >= 0 ? Phase.FIGHT : Phase.PREP;
	}

	public Gauntlet.Mode getMode()
	{
		return mode;
	}

	/** Seconds into the preparation, or -1 when its start was not seen or it is over. */
	public double prepElapsed(int tick)
	{
		return phase() == Phase.PREP && startTick >= 0 ? Gauntlet.ticks(tick - startTick) : -1;
	}

	/** How long the preparation took, once the fight began: fixed, it never moves during the fight. */
	public double prepTaken()
	{
		return startTick >= 0 && fightTick >= 0 ? Gauntlet.ticks(fightTick - startTick) : -1;
	}
}
