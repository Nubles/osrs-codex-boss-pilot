package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A boss kit held against what the player owns and carries: for each line, how much is owned in
 * all, how much is on the player already, and what is still to take out of the bank.
 */
public final class KitCheck
{
	public static final class Line
	{
		private final Supply supply;
		private final long owned;
		private final long carried;

		Line(Supply supply, long owned, long carried)
		{
			this.supply = supply;
			this.owned = owned;
			this.carried = carried;
		}

		public Supply getSupply()
		{
			return supply;
		}

		/** Banked, carried and worn together. */
		public long getOwned()
		{
			return owned;
		}

		/** On the player now. */
		public long getCarried()
		{
			return carried;
		}

		/** Whether enough is owned anywhere. */
		public boolean isOwned()
		{
			return owned >= supply.getAmount();
		}

		/** Whether enough is on the player. */
		public boolean isPacked()
		{
			return carried >= supply.getAmount();
		}

		/** How much more to take out of the bank, at most what is banked. */
		public long toWithdraw()
		{
			long want = Math.max(0, supply.getAmount() - carried);
			return Math.min(want, Math.max(0, owned - carried));
		}

		/** How much more has to be got from outside the bank. */
		public long shortfall()
		{
			return Math.max(0, supply.getAmount() - owned);
		}
	}

	private final List<Line> lines;

	private KitCheck(List<Line> lines)
	{
		this.lines = Collections.unmodifiableList(lines);
	}

	/**
	 * @param bank what is banked, or {@link Stock#EMPTY} when the bank has not been seen
	 * @param carried what is in the inventory and worn
	 * @param visited whether the player has fought this boss before, so first-visit lines drop out
	 */
	public static KitCheck of(Boss boss, Stock bank, Stock carried, boolean visited)
	{
		Stock all = bank.plus(carried);
		List<Line> out = new ArrayList<>();
		for (Supply s : boss.getKit())
		{
			if (s.isFirstVisit() && visited)
			{
				continue;
			}
			out.add(new Line(s, all.count(s), carried.count(s)));
		}
		return new KitCheck(out);
	}

	public List<Line> getLines()
	{
		return lines;
	}

	/** Must-have lines not owned in full anywhere: the trip cannot happen yet. */
	public List<Line> missingMust()
	{
		List<Line> out = new ArrayList<>();
		for (Line l : lines)
		{
			if (l.getSupply().getNeed() == Supply.Need.MUST && !l.isOwned())
			{
				out.add(l);
			}
		}
		return out;
	}

	/** Suggested lines not owned in full. */
	public List<Line> shortSuggested()
	{
		List<Line> out = new ArrayList<>();
		for (Line l : lines)
		{
			if (l.getSupply().getNeed() == Supply.Need.SUGGESTED && !l.isOwned())
			{
				out.add(l);
			}
		}
		return out;
	}

	/** Lines still to take out of the bank, must-haves first. */
	public List<Line> toWithdraw()
	{
		List<Line> out = new ArrayList<>();
		for (Supply.Need need : Supply.Need.values())
		{
			for (Line l : lines)
			{
				if (l.getSupply().getNeed() == need && l.toWithdraw() > 0)
				{
					out.add(l);
				}
			}
		}
		return out;
	}

	/** Every must-have and suggested line is on the player. */
	public boolean isPacked()
	{
		for (Line l : lines)
		{
			if (l.getSupply().getNeed() != Supply.Need.OPTIONAL && !l.isPacked())
			{
				return false;
			}
		}
		return true;
	}

	/** Every must-have and suggested line is owned somewhere. */
	public boolean isOwned()
	{
		return missingMust().isEmpty() && shortSuggested().isEmpty();
	}
}
