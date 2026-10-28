package com.codex.bosspilot.overlay;

import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.KitCheck;
import com.codex.bosspilot.core.Logbook;
import com.codex.bosspilot.core.Outlook;
import com.codex.bosspilot.core.Reminders;
import com.codex.bosspilot.core.Supply;
import com.codex.bosspilot.core.Trip;
import com.codex.bosspilot.ui.Theme;
import java.awt.Color;
import java.util.ArrayList;
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
		List<Plaque.Line> lines = new ArrayList<>();
		boolean urgent = false;
		List<Reminders.Reminder> sorted = new ArrayList<>(reminders);
		sorted.sort((a, b) -> Boolean.compare(b.isUrgent(), a.isUrgent()));
		for (Reminders.Reminder r : sorted.subList(0, Math.min(REMINDER_LINES, sorted.size())))
		{
			lines.add(new Plaque.Line(r.getText(), r.isUrgent() ? Theme.EMBER : Theme.GOLD));
			urgent |= r.isUrgent();
		}
		String kills = Format.plural(trip.getKills(), "kill", "kills") + " in " + Format.clock(trip.elapsed(now) / 1000.0);
		lines.add(new Plaque.Line(kills, Theme.TEXT, trip.getLoot() > 0 ? Format.number(trip.getLoot()) + " loot" : null));
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

	static String cut(String text, int max)
	{
		return text.length() <= max ? text : text.substring(0, max - 3).trim() + "...";
	}

	/** The kit plaque, or null when everything is packed. */
	public static Plaque.Content kit(String bossName, KitCheck kit)
	{
		List<KitCheck.Line> out = kit.toWithdraw();
		List<KitCheck.Line> missing = kit.missingMust();
		if (out.isEmpty() && missing.isEmpty())
		{
			List<Plaque.Line> lines = new ArrayList<>();
			lines.add(new Plaque.Line("Your kit is packed. Good luck.", Theme.GREEN));
			return new Plaque.Content("KIT FOR THE NEXT BOSS", bossName, Theme.GREEN, lines);
		}
		List<Plaque.Line> lines = new ArrayList<>();
		for (KitCheck.Line l : missing)
		{
			lines.add(new Plaque.Line("Not in your bank: " + l.getSupply().getLabel(), Theme.EMBER));
		}
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
		}
		int more = out.size() + missing.size() - lines.size();
		if (more > 0)
		{
			lines.add(new Plaque.Line("and " + more + " more in the panel", Theme.TEXT_FAINT));
		}
		Color tint = missing.isEmpty() ? Theme.GOLD : Theme.EMBER;
		return new Plaque.Content("TAKE OUT FOR THE NEXT BOSS", bossName, tint, lines);
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
