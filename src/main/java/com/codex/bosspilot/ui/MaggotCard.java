package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.Logbook;
import com.codex.bosspilot.core.MaggotKing;
import com.codex.bosspilot.core.MaggotLog;
import com.codex.bosspilot.core.MaggotRun;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The Maggot King on the Prepare and Trip tabs: the player's own kills and trips, the five
 * collection log drops with the chance of having had each by now, the time to a chosen drop at
 * the player's own pace, and the account against the stats the wiki's guides recommend. Nothing
 * here is about the fight itself.
 */
final class MaggotCard
{
	/** Trips listed at most. */
	static final int TRIPS = 4;

	private MaggotCard()
	{
	}

	/** The player's own kills. */
	static JComponent kills(MaggotRun r, int width)
	{
		MaggotLog log = r.getLog();
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.BLOOD);
		StackPanel body = new StackPanel(5, 0);
		if (log.isEmpty())
		{
			body.add(Ui.wrapped("Your kills show here from your next one: how you looted each corpse and what it brought, kills and loot"
				+ " an hour at your own pace, deaths, and the odds on each drop. Kept on this computer only.", Theme.small(), Theme.TEXT_MUTED, width));
		}
		else
		{
			JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
			grid.setOpaque(false);
			double kph = r.killsPerHour();
			double lph = log.lootPerHour();
			grid.add(new StatTile("Kills", Format.number(log.kills), true));
			grid.add(new StatTile("Kills/hr", kph < 0 ? "-" : String.format(Locale.ROOT, "%.1f", kph), kph > 0));
			grid.add(new StatTile("Per kill", log.average() < 0 ? "-" : Format.number(log.average()), log.average() > 0));
			grid.add(new StatTile("Loot/hr", lph < 0 ? "-" : Format.number(Math.round(lph)), lph > 0));
			body.add(grid);
			body.add(Ui.wrapped(choiceText(log), Theme.small(), Theme.TEXT, width));
			MaggotLog.Kill last = log.last();
			if (last != null)
			{
				body.add(Ui.wrapped(lastText(last), Theme.small(), last.uniques.isEmpty() ? Theme.TEXT_MUTED : Theme.GREEN, width));
			}
			String supplies = r.suppliesText();
			if (supplies != null)
			{
				body.add(Ui.wrapped("Supplies: " + supplies + ".", Theme.small(), Theme.TEXT_MUTED, width));
			}
			if (r.deaths() > 0)
			{
				body.add(Ui.wrapped(r.deaths() == 1 ? "1 death here so far." : r.deaths() + " deaths here so far.", Theme.small(), Theme.EMBER, width));
			}
			if (log.timed < log.kills)
			{
				body.add(Ui.wrapped("Your pace counts the " + Format.plural(log.timed, "kill", "kills")
					+ " timed back to back on a trip, not the walk in or banking.", Theme.small(), Theme.TEXT_FAINT, width));
			}
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	static String choiceText(MaggotLog log)
	{
		StringBuilder b = new StringBuilder();
		b.append("Opened ").append(Format.plural(log.stomachs, "stomach", "stomachs"));
		b.append(", took eggs ").append(log.eggs == 1 ? "once" : log.eggs + " times");
		if (log.unseen() > 0)
		{
			b.append(", ").append(log.unseen()).append(" not seen");
		}
		return b.append('.').toString();
	}

	static String lastText(MaggotLog.Kill k)
	{
		StringBuilder b = new StringBuilder("Last kill");
		if (k.kc > 0)
		{
			b.append(" (KC ").append(Format.number(k.kc)).append(')');
		}
		MaggotKing.Choice c = k.choice();
		if (c != null)
		{
			b.append(", ").append(c.getOption());
		}
		b.append(": ");
		if (k.value < 0)
		{
			b.append("not looted");
		}
		else if (k.items.isEmpty())
		{
			b.append("nothing");
		}
		else
		{
			b.append(Format.number(k.value)).append(", ").append(String.join(", ", k.items.subList(0, Math.min(3, k.items.size()))));
			if (k.items.size() > 3)
			{
				b.append(" and more");
			}
		}
		if (!k.uniques.isEmpty())
		{
			b.append(". ");
			for (int i = 0; i < k.uniques.size(); i++)
			{
				MaggotKing.Unique u = MaggotKing.Unique.byKey(k.uniques.get(i));
				b.append(i > 0 ? ", " : "").append(u == null ? k.uniques.get(i) : u.getName());
			}
			b.append('!');
		}
		else
		{
			b.append('.');
		}
		return b.toString();
	}

	/** The account's recent trips here: kills, time, loot, average kill, supplies and deaths. */
	static JComponent trips(MaggotRun r, int width)
	{
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.GOLD_DIM);
		StackPanel body = new StackPanel(6, 0);
		List<Logbook.TripRecord> trips = r.recentTrips(TRIPS);
		if (trips.isEmpty())
		{
			body.add(Ui.wrapped("Each trip goes here when it ends: kills, time, loot, the average kill, food and prayer a kill, and deaths.",
				Theme.small(), Theme.TEXT_MUTED, width));
		}
		for (Logbook.TripRecord t : trips)
		{
			body.add(new TripRow(t));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** The five drops, each with its rate and the chance of having had it by now. */
	static JComponent uniques(MaggotRun r, int width)
	{
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.VIOLET);
		StackPanel body = new StackPanel(8, 0);
		for (MaggotKing.Unique u : MaggotKing.Unique.values())
		{
			StackPanel one = new StackPanel(2, 0);
			one.add(new DropRow(r, u));
			String dry = r.dryText(u);
			String luck = r.luckText(u);
			String text = dry == null ? u.rateText() + "." : dry + (luck == null ? "." : ". " + luck + ".");
			one.add(Ui.wrapped(text, Theme.small(), r.hadByNow(u) >= 0.9 ? Theme.EMBER : Theme.TEXT_MUTED, width));
			body.add(one);
		}
		StringBuilder foot = new StringBuilder("Rates: Jagex's for the elder fang, kisten and pet; the wiki's drop-log estimate (marked ~) for the"
			+ " venator fang and tooth. The table changed on 8 July 2026; checked against the wiki on 4 November 2026.");
		if (r.getLog().unseen() > 0)
		{
			foot.append(' ').append(Format.plural(r.getLog().unseen(), "kill", "kills")).append(" whose loot Boss Pilot did not see are left out of the odds.");
		}
		body.add(Ui.wrapped(foot.toString(), Theme.small(), Theme.TEXT_FAINT, width));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** The chosen drop, and how long it should take at the player's pace. */
	static JComponent goal(MaggotRun r, int width)
	{
		MaggotKing.Unique g = r.getGoal();
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.TEAL);
		StackPanel body = new StackPanel(5, 0);
		JPanel head = new JPanel(new BorderLayout(6, 0));
		head.setOpaque(false);
		head.add(Ui.label(g.getName(), Theme.body(), Theme.TEXT), BorderLayout.WEST);
		head.add(Ui.label(r.owns(g) ? "owned" : MaggotKing.oneIn(r.perKill(g)).replace("1 in ", "1/"), Theme.small(),
			r.owns(g) ? Theme.GREEN : Theme.GOLD_DIM), BorderLayout.EAST);
		body.add(head);
		body.add(Ui.wrapped(capital(r.goalRateText()) + ".", Theme.small(), Theme.TEXT_MUTED, width));
		int[] k = r.goalKills();
		if (k[0] > 0)
		{
			JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
			grid.setOpaque(false);
			double kph = r.killsPerHour();
			grid.add(new StatTile("Even odds", kph > 0 ? hoursShort(k[0] / kph) : Format.number(k[0]) + " kills", true));
			grid.add(new StatTile("9 in 10", kph > 0 ? hoursShort(k[1] / kph) : Format.number(k[1]) + " kills", true));
			body.add(grid);
		}
		body.add(Ui.wrapped(r.etaText(), Theme.small(), Theme.TEXT, width));
		body.add(Ui.wrapped("Every kill is a fresh roll, so the time ahead is the same however long you have gone. Pick the drop under"
			+ " Maggot King goal in the settings.", Theme.small(), Theme.TEXT_FAINT, width));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	static String hoursShort(double h)
	{
		if (h < 1)
		{
			return Math.max(1, Math.round(h * 60)) + " min";
		}
		return h < 10 ? String.format(Locale.ROOT, "%.1f hr", h) : Format.number(Math.round(h)) + " hr";
	}

	private static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/** The account against the wiki's two money-making methods, and the unlocks on the way. */
	static JComponent ready(MaggotRun r, int width)
	{
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.GOLD);
		StackPanel body = new StackPanel(6, 0);
		if (!r.isAccountKnown())
		{
			body.add(Ui.wrapped("Log in and Boss Pilot checks your levels against these.", Theme.small(), Theme.TEXT_MUTED, width));
		}
		for (MaggotRun.Method m : r.methods())
		{
			JPanel head = new JPanel(new BorderLayout(6, 0));
			head.setOpaque(false);
			head.add(Ui.label(m.getName(), Theme.body(), m.met() == m.getStats().size() ? Theme.GREEN : Theme.TEXT), BorderLayout.WEST);
			head.add(Ui.label(m.met() + " of " + m.getStats().size(), Theme.small(), Theme.GOLD_DIM), BorderLayout.EAST);
			body.add(head);
			body.add(new StatStrip(m.getStats()));
			body.add(Ui.wrapped("The guide assumes " + m.getKillsPerHour() + " kills an hour with max gear and stats.", Theme.small(),
				Theme.TEXT_FAINT, width));
		}
		body.add(Ui.label("On the way", Theme.body(), Theme.TEXT));
		body.add(new StatStrip(r.extras()));
		body.add(Ui.wrapped("64 Agility for the two shortcuts through the woods, 70 Construction to build the bank chest by the lair, 84 Crafting"
			+ " to etch an elder venator fang. The aranei scout's quick travel cannot be read from the game, so it is not checked.",
			Theme.small(), Theme.TEXT_FAINT, width));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** The skill's name cut to fit a narrow pip: "Strength" is "Str". */
	static String shortSkill(String skill)
	{
		switch (skill)
		{
			case "Construction":
				return "Cons";
			case "Prayer":
				return "Pray";
			case "Ranged":
				return "Range";
			default:
				return skill.length() <= 4 ? skill : skill.substring(0, 3);
		}
	}

	/** Colour of each drop's gem. */
	static Color tone(MaggotKing.Unique u)
	{
		switch (u)
		{
			case ELDER_FANG:
				return Theme.GOLD;
			case KISTEN:
				return Theme.BLOOD;
			case PET:
				return Theme.GREEN;
			default:
				return Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.3);
		}
	}

	/** One trip: kills and time on the left, loot on the right, then the average kill, supplies and deaths. */
	static final class TripRow extends JComponent
	{
		private static final int H = 30;
		private final Logbook.TripRecord trip;

		TripRow(Logbook.TripRecord trip)
		{
			this.trip = trip;
			setOpaque(false);
			setPreferredSize(new Dimension(10, H));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			g.setFont(Theme.body());
			String left = Format.plural(trip.kills, "kill", "kills") + " in " + Format.clock(trip.minutes() * 60);
			Ui.shadowed(g, left, 0, 11, trip.kills > 0 ? Theme.TEXT : Theme.TEXT_MUTED);
			g.setFont(Theme.small());
			FontMetrics sm = g.getFontMetrics();
			String loot = trip.loot > 0 ? Format.number(trip.loot) : "no loot";
			Ui.shadowed(g, loot, w - sm.stringWidth(loot), 11, trip.loot > 0 ? Theme.GOLD : Theme.TEXT_FAINT);
			StringBuilder b = new StringBuilder();
			if (trip.kills > 0)
			{
				b.append("avg ").append(Format.clock(trip.minutes() * 60 / trip.kills));
				if (trip.foodUsed > 0)
				{
					b.append(" · ").append(Math.round(trip.foodUsed / (double) trip.kills)).append(" hp");
				}
				if (trip.dosesUsed > 0)
				{
					b.append(" · ").append(String.format(Locale.ROOT, "%.1f", trip.dosesUsed / (double) trip.kills)).append(" doses");
				}
			}
			else
			{
				b.append("no kills");
			}
			int room = w;
			if (trip.deaths > 0)
			{
				String d = trip.deaths == 1 ? "1 death" : trip.deaths + " deaths";
				Ui.shadowed(g, d, w - sm.stringWidth(d), 26, Theme.EMBER);
				room = w - sm.stringWidth(d) - 6;
			}
			Ui.shadowed(g, Ui.clip(b.toString(), sm, room), 0, 26, Theme.TEXT_MUTED);
			g.setColor(Theme.STROKE_SOFT);
			g.drawLine(0, H - 1, w, H - 1);
			g.dispose();
		}
	}

	/** Skill pips: each recommended level, lit green when the account has it. */
	static final class StatStrip extends JComponent
	{
		private static final int H = 34;
		private final List<MaggotRun.Stat> stats;

		StatStrip(List<MaggotRun.Stat> stats)
		{
			this.stats = stats;
			setOpaque(false);
			setPreferredSize(new Dimension(10, H));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			int n = Math.max(1, stats.size());
			int gap = 4;
			int cw = (w - gap * (n - 1)) / n;
			for (int i = 0; i < stats.size(); i++)
			{
				MaggotRun.Stat s = stats.get(i);
				int x = i * (cw + gap);
				boolean met = s.isMet();
				Color tone = met ? Theme.GREEN : Theme.EMBER;
				g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, H, Theme.INK_DEEP));
				g.fill(new RoundRectangle2D.Float(x, 0, cw, H - 1, 6, 6));
				g.setColor(Theme.alpha(tone, met ? 150 : 110));
				g.draw(new RoundRectangle2D.Float(x, 0, cw - 1, H - 2, 6, 6));
				g.setFont(Theme.small());
				FontMetrics sm = g.getFontMetrics();
				String name = Ui.clip(sm.stringWidth(s.getSkill()) <= cw - 6 ? s.getSkill() : shortSkill(s.getSkill()), sm, cw - 6);
				Ui.shadowed(g, name, x + (cw - sm.stringWidth(name)) / 2, 12, Theme.TEXT_MUTED);
				g.setFont(Theme.body());
				FontMetrics fm = g.getFontMetrics();
				String lv = s.getHave() + "/" + s.getLevel();
				Ui.shadowed(g, lv, x + (cw - fm.stringWidth(lv)) / 2, 28, met ? Theme.GREEN : Theme.TEXT);
			}
			g.dispose();
		}
	}

	/** A gem lit when owned, the drop's name and rate, and a meter of the chance of having had it by now. */
	static final class DropRow extends JComponent
	{
		private static final int H = 30;
		private final MaggotRun run;
		private final MaggotKing.Unique unique;

		DropRow(MaggotRun run, MaggotKing.Unique unique)
		{
			this.run = run;
			this.unique = unique;
			setOpaque(false);
			setPreferredSize(new Dimension(10, H));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			Color tone = tone(unique);
			boolean owned = run.owns(unique);
			int gx = 10;
			int gy = 13;
			int gr = 9;
			if (unique == MaggotKing.Unique.PET)
			{
				// The pet's gem is an egg.
				Ellipse2D egg = new Ellipse2D.Float(gx - gr + 2, gy - gr - 1, (gr - 2) * 2, gr * 2 + 2);
				g.setPaint(owned ? new GradientPaint(0, gy - gr, Theme.mix(tone, Color.WHITE, 0.35), 0, gy + gr, Theme.mix(tone, Theme.INK_DEEP, 0.45))
					: new GradientPaint(0, gy - gr, Theme.CARD_TOP, 0, gy + gr, Theme.INK_DEEP));
				g.fill(egg);
				g.setColor(owned ? tone : Theme.alpha(tone, 120));
				g.setStroke(new BasicStroke(1f));
				g.draw(egg);
			}
			else
			{
				Polygon gem = new Polygon(new int[]{gx, gx + gr, gx, gx - gr}, new int[]{gy - gr - 2, gy, gy + gr + 2, gy}, 4);
				if (owned)
				{
					g.setPaint(new GradientPaint(0, gy - gr, Theme.mix(tone, Color.WHITE, 0.35), 0, gy + gr, Theme.mix(tone, Theme.INK_DEEP, 0.45)));
					g.fill(gem);
					g.setColor(Theme.alpha(Color.WHITE, 90));
					g.drawLine(gx, gy - gr - 1, gx - gr + 2, gy);
					g.setColor(tone);
				}
				else
				{
					g.setPaint(new GradientPaint(0, gy - gr, Theme.CARD_TOP, 0, gy + gr, Theme.INK_DEEP));
					g.fill(gem);
					g.setColor(Theme.alpha(tone, 120));
				}
				g.setStroke(new BasicStroke(1f));
				g.draw(gem);
			}

			int tx = 26;
			g.setFont(Theme.body());
			Ui.shadowed(g, unique.getName(), tx, 11, owned ? Theme.TEXT : Theme.TEXT_MUTED);
			g.setFont(Theme.small());
			FontMetrics sm = g.getFontMetrics();
			int found = run.getLog().peek(unique).found;
			String right = owned ? (found > 1 ? "x" + found : "owned") : unique.shortRate();
			Ui.shadowed(g, right, w - sm.stringWidth(right), 11, owned ? Theme.GREEN : Theme.GOLD_DIM);

			// The meter: the chance of having had one by now, ticked at even odds and at 9 in 10.
			int my = 18;
			int mh = 7;
			int mw = w - tx;
			g.setPaint(new GradientPaint(0, my, Theme.INK_DEEP, 0, my + mh, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
			g.fill(new RoundRectangle2D.Float(tx, my, mw, mh, mh, mh));
			g.setColor(Theme.STROKE_SOFT);
			g.draw(new RoundRectangle2D.Float(tx, my, mw - 1, mh, mh, mh));
			double had = run.hadByNow(unique);
			if (had > 0)
			{
				int fw = (int) Math.round(mw * Math.min(1, had));
				Color end = had >= 0.9 ? Theme.EMBER : had >= 0.5 ? Theme.GOLD_BRIGHT : Theme.mix(tone, Theme.GOLD, 0.4);
				g.setPaint(new GradientPaint(tx, 0, Theme.mix(tone, Theme.INK_DEEP, 0.4), tx + Math.max(4, fw), 0, end));
				g.fill(new RoundRectangle2D.Float(tx, my, Math.max(4, fw), mh, mh, mh));
			}
			for (double m : new double[]{0.5, 0.9})
			{
				int mx = tx + (int) Math.round(mw * m);
				g.setColor(had >= m ? Theme.alpha(Theme.INK_DEEP, 170) : Theme.alpha(Theme.GOLD_DIM, 170));
				g.drawLine(mx, my + 1, mx, my + mh - 1);
			}
			g.dispose();
		}
	}
}
