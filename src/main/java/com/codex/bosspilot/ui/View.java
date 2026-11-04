package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.Aim;
import com.codex.bosspilot.core.BarrowsRun;
import com.codex.bosspilot.core.ChargeCheck;
import com.codex.bosspilot.core.Logbook;
import com.codex.bosspilot.core.LunarRun;
import com.codex.bosspilot.core.Outlook;
import com.codex.bosspilot.core.Ranker;
import com.codex.bosspilot.core.Reminders;
import com.codex.bosspilot.core.Review;
import com.codex.bosspilot.core.Setup;
import com.codex.bosspilot.core.Trip;
import com.codex.bosspilot.core.ZalcanoRun;
import java.util.Collections;
import java.util.List;

/** Everything the panel shows, worked out on the client thread and handed over whole. */
public final class View
{
	public final boolean loggedIn;
	public final List<Ranker.Pick> picks;
	public final Ranker.Pick next;
	/** The boss on the Prepare tab: the one picked in the panel, or the next one. */
	public final Ranker.Pick selected;
	/** The best owned setups for the selected boss, best first; empty while working them out or when it has no fight data. */
	public final List<Setup> setups;
	public final boolean setupsPending;
	public final Trip trip;
	public final Outlook outlook;
	public final Logbook logbook;
	public final Review review;
	public final Aim aim;
	/** When the bank was last seen, in Unix milliseconds, or 0. */
	public final long bankSeenAt;
	public final long now;
	/** Reminders about the player on the trip under way. */
	public final List<Reminders.Reminder> reminders;
	/** The player's own note for the selected boss, or empty. */
	public final String note;
	/** The charged items for the selected boss, and for the trip under way. */
	private List<ChargeCheck.Line> charges = Collections.emptyList();
	private List<ChargeCheck.Line> tripCharges = Collections.emptyList();
	private BarrowsRun barrows;
	private ZalcanoRun zalcano;
	private LunarRun moons;

	public View(boolean loggedIn, List<Ranker.Pick> picks, Ranker.Pick next, Ranker.Pick selected, List<Setup> setups,
		boolean setupsPending, Trip trip, Outlook outlook, Logbook logbook, Review review, Aim aim, long bankSeenAt, long now,
		List<Reminders.Reminder> reminders, String note)
	{
		this.loggedIn = loggedIn;
		this.picks = picks;
		this.next = next;
		this.selected = selected;
		this.setups = setups;
		this.setupsPending = setupsPending;
		this.trip = trip;
		this.outlook = outlook;
		this.logbook = logbook;
		this.review = review;
		this.aim = aim;
		this.bankSeenAt = bankSeenAt;
		this.now = now;
		this.reminders = reminders;
		this.note = note;
	}

	/** The same view with the charged items for the selected boss and for the trip. */
	public View withCharges(List<ChargeCheck.Line> charges, List<ChargeCheck.Line> tripCharges)
	{
		this.charges = charges == null ? Collections.emptyList() : charges;
		this.tripCharges = tripCharges == null ? Collections.emptyList() : tripCharges;
		return this;
	}

	/** The same view with where the Barrows run stands and the chest log; null leaves it out. */
	public View withBarrows(BarrowsRun barrows)
	{
		this.barrows = barrows;
		return this;
	}

	/** The same view with the Zalcano kill log; null leaves it out. */
	public View withZalcano(ZalcanoRun zalcano)
	{
		this.zalcano = zalcano;
		return this;
	}

	/** The same view with the Moons of Peril run and Lunar Chest log; null leaves it out. */
	public View withMoons(LunarRun moons)
	{
		this.moons = moons;
		return this;
	}

	/** The Moons of Peril run and Lunar Chest log, or null. */
	public LunarRun getMoons()
	{
		return moons;
	}

	/** The Zalcano kill log, or null. */
	public ZalcanoRun getZalcano()
	{
		return zalcano;
	}

	/** The Barrows run and chest log, or null. */
	public BarrowsRun getBarrows()
	{
		return barrows;
	}

	public List<ChargeCheck.Line> getCharges()
	{
		return charges;
	}

	public List<ChargeCheck.Line> getTripCharges()
	{
		return tripCharges;
	}

	public static View empty()
	{
		return new View(false, Collections.emptyList(), null, null, Collections.emptyList(), false, null, Outlook.UNKNOWN,
			new Logbook(), null, Aim.PROGRESS, 0, 0,
			Collections.emptyList(), "");
	}

	public long openCount()
	{
		return picks.stream().filter(p -> p.getStatus().isOpen()).count();
	}
}
