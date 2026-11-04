package com.codex.bosspilot.overlay;

import com.codex.bosspilot.core.Barrows;
import com.codex.bosspilot.core.BarrowsRun;
import com.codex.bosspilot.core.ChargeCheck;
import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.KitCheck;
import com.codex.bosspilot.core.Logbook;
import com.codex.bosspilot.core.LunarRun;
import com.codex.bosspilot.core.Outlook;
import com.codex.bosspilot.core.Reminders;
import com.codex.bosspilot.core.Supply;
import com.codex.bosspilot.core.Trip;
import com.codex.bosspilot.core.ZalcanoRun;
import com.codex.bosspilot.ui.Theme;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** What the two plaques say: the trip at a boss, and the kit to take out at the bank. */
public final class Plaques
{
	/** Kit lines shown at most; the panel has the rest. */
	static final int KIT_LINES = 5;
	/** Reminders shown at most, most urgent first. */
	static final int REMINDER_LINES = 3;
	/** The player's note is cut to this on the plaque. */
	static final int NOTE_CHARS = 44;

	private Plaques()
	{
	}

	public static Plaque.Content trip(Trip trip, Outlook outlook, Logbook book, List<Reminders.Reminder> reminders, String note, long now)
	{
		return trip(trip, outlook, book, reminders, note, now, null);
	}

	/**
	 * The trip plaque; on a Barrows trip, with where the run stands: the brothers down, the crypt
	 * with the tunnel and the tunnel kills to the potential aimed for.
	 */
	public static Plaque.Content trip(Trip trip, Outlook outlook, Logbook book, List<Reminders.Reminder> reminders, String note, long now,
		BarrowsRun barrows)
	{
		return trip(trip, outlook, book, reminders, note, now, barrows, null);
	}

	/**
	 * The trip plaque; on a Zalcano trip, with the player's own pace and the kills since a rare
	 * drop.
	 */
	public static Plaque.Content trip(Trip trip, Outlook outlook, Logbook book, List<Reminders.Reminder> reminders, String note, long now,
		BarrowsRun barrows, ZalcanoRun zalcano)
	{
		return trip(trip, outlook, book, reminders, note, now, barrows, zalcano, null);
	}

	/**
	 * The trip plaque; on a Moons of Peril trip, with the moons down this run, what the chest would
	 * roll now and the player's own chest value an hour.
	 */
	public static Plaque.Content trip(Trip trip, Outlook outlook, Logbook book, List<Reminders.Reminder> reminders, String note, long now,
		BarrowsRun barrows, ZalcanoRun zalcano, LunarRun moons)
	{
		List<Plaque.Line> lines = new ArrayList<>();
		boolean urgent = false;
		List<Reminders.Reminder> sorted = new ArrayList<>(reminders);
		sorted.sort((a, b) -> Boolean.compare(b.isUrgent(), a.isUrgent()));
		for (Reminders.Reminder r : sorted.subList(0, Math.min(REMINDER_LINES, sorted.size())))
		{
			lines.add(new Plaque.Line(r.getText(), r.isUrgent() ? Theme.EMBER : Theme.GOLD));
			urgent |= r.isUrgent();
		}
		String kills = (barrows != null || moons != null ? Format.plural(trip.getKills(), "chest", "chests") : Format.plural(trip.getKills(), "kill", "kills"))
			+ " in " + Format.clock(trip.elapsed(now) / 1000.0);
		lines.add(new Plaque.Line(kills, Theme.TEXT, trip.getLoot() > 0 ? Format.number(trip.getLoot()) + " loot" : null));
		if (barrows != null)
		{
			lines.addAll(barrows(barrows));
		}
		if (zalcano != null)
		{
			lines.add(new Plaque.Line(zalcano.plaqueText(), zalcano.getLog().sinceAny() == 0 && !zalcano.getLog().isEmpty() ? Theme.GREEN : Theme.VIOLET,
				zalcano.paceText()));
		}
		if (moons != null)
		{
			lines.add(new Plaque.Line(moons.plaqueText(), moons.getSubdued().isEmpty() ? Theme.TEXT_MUTED
				: moons.getSubdued().size() == 3 ? Theme.GREEN : Theme.TEXT, moons.paceText()));
			String chest = moons.chestText();
			if (chest != null)
			{
				lines.add(new Plaque.Line(chest, Theme.VIOLET));
			}
		}
		if (!trip.getKillSeconds().isEmpty())
		{
			Logbook.Entry e = book.peek(trip.getBoss());
			double best = e == null ? 0 : e.best;
			double thisBest = trip.bestKill();
			best = best <= 0 ? thisBest : Math.min(best, thisBest);
			List<Double> times = trip.getKillSeconds();
			lines.add(new Plaque.Line("Last " + Format.clock(times.get(times.size() - 1)) + ", average " + Format.clock(trip.averageKill()),
				Theme.TEXT_MUTED, best > 0 ? "best " + Format.clock(best) : null));
		}
		lines.add(new Plaque.Line(outlook.text(), color(outlook)));
		if (trip.getKills() == 0 && note != null && !note.isEmpty())
		{
			lines.add(new Plaque.Line("Your note: " + cut(note.replace('\n', ' '), NOTE_CHARS), Theme.TEXT_MUTED));
		}
		return new Plaque.Content("BOSS PILOT: ON A TRIP", trip.getBoss().getName(), urgent ? Theme.EMBER : color(outlook), lines);
	}

	/** The Barrows run on the trip plaque. */
	static List<Plaque.Line> barrows(BarrowsRun run)
	{
		List<Plaque.Line> lines = new ArrayList<>();
		lines.add(new Plaque.Line(run.brothersShort(), run.getKilled().size() == 6 ? Theme.GREEN : Theme.TEXT, "potential " + run.potential()));
		lines.add(new Plaque.Line(run.tunnelText(), run.getTunnel() == null ? Theme.TEXT_MUTED : Theme.GOLD));
		String plan = run.planText();
		if (plan != null)
		{
			Barrows.Plan p = run.getPlan();
			lines.add(new Plaque.Line(plan, p.reached() ? Theme.GREEN : Theme.TEAL,
				p.reached() || p.getKills().isEmpty() ? null : "+" + p.getAdds() + " = " + p.result()));
		}
		return lines;
	}

	static String cut(String text, int max)
	{
		return text.length() <= max ? text : text.substring(0, max - 3).trim() + "...";
	}

	/** The kit plaque. */
	public static Plaque.Content kit(String bossName, KitCheck kit)
	{
		return kit(bossName, kit, Collections.emptyList());
	}

	/**
	 * The kit plaque: what is not in the bank, the charged gear to top up or check, then what to
	 * take out.
	 */
	public static Plaque.Content kit(String bossName, KitCheck kit, List<ChargeCheck.Line> charges)
	{
		List<KitCheck.Line> out = kit.toWithdraw();
		List<KitCheck.Line> missing = kit.missingMust();
		List<ChargeCheck.Line> topUp = new ArrayList<>();
		List<ChargeCheck.Line> unchecked = new ArrayList<>();
		for (ChargeCheck.Line c : charges)
		{
			if (c.getStatus().needsRecharge())
			{
				topUp.add(c);
			}
			else if (c.getStatus() == ChargeCheck.Status.UNCHECKED)
			{
				unchecked.add(c);
			}
		}
		if (out.isEmpty() && missing.isEmpty() && topUp.isEmpty())
		{
			List<Plaque.Line> lines = new ArrayList<>();
			lines.add(new Plaque.Line("Your kit is packed. Good luck.", Theme.GREEN));
			for (ChargeCheck.Line c : unchecked)
			{
				lines.add(new Plaque.Line("Check " + c.getItem().getName() + " to count its charges", Theme.TEXT_MUTED));
			}
			return new Plaque.Content("KIT FOR THE NEXT BOSS", bossName, Theme.GREEN, lines);
		}
		List<Plaque.Line> lines = new ArrayList<>();
		for (KitCheck.Line l : missing)
		{
			lines.add(new Plaque.Line("Not in your bank: " + l.getSupply().getLabel(), Theme.EMBER));
		}
		for (ChargeCheck.Line c : topUp)
		{
			lines.add(new Plaque.Line("Top up " + c.getItem().getName(), c.getStatus() == ChargeCheck.Status.EMPTY ? Theme.BLOOD : Theme.EMBER,
				topUpRight(c)));
		}
		int shown = 0;
		for (KitCheck.Line l : out)
		{
			if (lines.size() >= KIT_LINES)
			{
				break;
			}
			if (missing.contains(l))
			{
				continue;
			}
			lines.add(new Plaque.Line(take(l), l.getSupply().getNeed() == Supply.Need.OPTIONAL ? Theme.TEXT_MUTED : Theme.TEXT,
				amount(l)));
			shown++;
		}
		for (ChargeCheck.Line c : unchecked)
		{
			if (lines.size() >= KIT_LINES + 1)
			{
				break;
			}
			lines.add(new Plaque.Line("Check " + c.getItem().getName(), Theme.TEXT_MUTED, "charges unknown"));
		}
		int more = out.size() - shown;
		if (more > 0)
		{
			lines.add(new Plaque.Line("and " + more + " more in the panel", Theme.TEXT_FAINT));
		}
		Color tint = missing.isEmpty() ? Theme.GOLD : Theme.EMBER;
		return new Plaque.Content("TAKE OUT FOR THE NEXT BOSS", bossName, tint, lines);
	}

	/** "darts: ~2 kills, food 6", "empty". */
	static String topUpRight(ChargeCheck.Line c)
	{
		String pool = c.limitName();
		String kills = c.killsText();
		if (c.getStatus() == ChargeCheck.Status.SHORT && c.getTarget() > 0)
		{
			return (pool == null ? "" : pool + " ") + kills + " of " + (long) Math.floor(c.getTarget());
		}
		return (pool == null ? "" : pool + " ") + kills;
	}

	static String take(KitCheck.Line l)
	{
		Supply s = l.getSupply();
		return s.getNeed() == Supply.Need.OPTIONAL ? s.getLabel() + " (optional)" : s.getLabel();
	}

	/** "+120 hp", "+8 doses", "1". */
	static String amount(KitCheck.Line l)
	{
		long n = l.toWithdraw();
		switch (l.getSupply().getKind())
		{
			case FOOD:
				return "+" + n + " hp";
			case DOSES:
				return "+" + n + (n == 1 ? " dose" : " doses");
			default:
				return String.valueOf(n);
		}
	}

	static Color color(Outlook o)
	{
		switch (o.getLevel())
		{
			case PLENTY:
				return Theme.GREEN;
			case FEW:
				return Theme.GOLD;
			case LAST:
				return Theme.EMBER;
			default:
				return Theme.TEAL;
		}
	}
}
