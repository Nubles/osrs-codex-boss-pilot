package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.LunarLog;
import com.codex.bosspilot.core.LunarRun;
import com.codex.bosspilot.core.Moons;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RadialGradientPaint;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The Moons of Peril on the Prepare and Trip tabs: the run under way (the moons down, what the
 * chest would roll now, the supplies used), the player's own Lunar Chests (value an hour and a
 * run at their own pace, moons a run), and the three sets with each moon's dry spell against its
 * rate. All of it is read between fights; nothing here is about a fight.
 */
final class MoonsCard
{
	/** The dry meter runs to this many times a set's rate. */
	static final double METER_SPAN = 3;

	private MoonsCard()
	{
	}

	/** Each moon's own colour. */
	static Color tone(Moons.Moon m)
	{
		switch (m)
		{
			case BLOOD:
				return Theme.BLOOD;
			case BLUE:
				return new Color(112, 160, 226);
			default:
				return Theme.GOLD;
		}
	}

	/** The run under way: the three moons, what the chest would roll now, and the supplies used. */
	static JComponent run(LunarRun r, int width)
	{
		Card card = new Card(new BorderLayout(0, 6)).accent(r.getSubdued().size() == 3 ? Theme.GREEN : Theme.GOLD_DIM);
		StackPanel body = new StackPanel(5, 0);
		body.add(new Phases(r));
		body.add(Ui.wrapped(r.moonsText() + ".", Theme.small(), r.getSubdued().isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT, width));
		String chest = r.chestText();
		if (chest != null)
		{
			body.add(Ui.wrapped(chest + ".", Theme.small(), Theme.VIOLET, width));
		}
		String used = r.usedText();
		if (used != null)
		{
			body.add(Ui.wrapped(used + ".", Theme.small(), Theme.TEXT_MUTED, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** The player's own chests. */
	static JComponent chests(LunarRun r, int width)
	{
		LunarLog log = r.getLog();
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.GOLD_DIM);
		StackPanel body = new StackPanel(5, 0);
		if (log.isEmpty())
		{
			body.add(Ui.wrapped("Your Lunar Chests show here from your next one: what each held and its worth at the day's prices,"
				+ " chest value an hour and a run at your own pace, the moons you take a run and the supplies they cost."
				+ " Kept on this computer only.", Theme.small(), Theme.TEXT_MUTED, width));
		}
		else
		{
			JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
			grid.setOpaque(false);
			double cph = log.chestsPerHour();
			double vph = log.valuePerHour();
			grid.add(new StatTile("Chests", Format.number(log.chests), true));
			grid.add(new StatTile("Chests/hr", cph < 0 ? "-" : String.format(Locale.ROOT, "%.1f", cph), cph > 0));
			grid.add(new StatTile("Per chest", log.average() < 0 ? "-" : Format.number(log.average()), log.average() > 0));
			grid.add(new StatTile("Value/hr", vph < 0 ? "-" : Format.number(Math.round(vph)), vph > 0));
			body.add(grid);
			String run = runText(log);
			if (run != null)
			{
				body.add(Ui.wrapped(run, Theme.small(), Theme.TEXT, width));
			}
			LunarLog.Chest last = log.last();
			if (last != null)
			{
				body.add(Ui.wrapped(lastText(last), Theme.small(), last.pieces.isEmpty() ? Theme.TEXT_MUTED : Theme.GREEN, width));
			}
			String per = r.usedPerRunText();
			if (per != null)
			{
				body.add(Ui.wrapped(per + ".", Theme.small(), Theme.TEXT_MUTED, width));
			}
			if (log.timed < log.chests)
			{
				body.add(Ui.wrapped("Your pace counts the " + Format.plural(log.timed, "run", "runs")
					+ " timed chest to chest on a trip, not the walk in or long breaks.", Theme.small(), Theme.TEXT_FAINT, width));
			}
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** "A run: 2.7 moons, 14:20, worth 380k on average", or null before anything to say. */
	static String runText(LunarLog log)
	{
		double moons = log.moonsPerRun();
		double secs = log.averageRun();
		long avg = log.average();
		if (moons < 0 && secs < 0)
		{
			return null;
		}
		StringBuilder b = new StringBuilder("A run: ");
		boolean any = false;
		if (moons >= 0)
		{
			b.append(String.format(Locale.ROOT, "%.1f", moons)).append(" moons");
			any = true;
		}
		if (secs > 0)
		{
			b.append(any ? ", " : "").append(Format.clock(secs));
			any = true;
		}
		if (avg >= 0)
		{
			b.append(any ? ", " : "").append("worth ").append(Format.coins(avg));
		}
		return b.append(" on average.").toString();
	}

	static String lastText(LunarLog.Chest c)
	{
		StringBuilder b = new StringBuilder("Last chest");
		if (c.kc > 0)
		{
			b.append(" (KC ").append(Format.number(c.kc)).append(')');
		}
		b.append(": ");
		b.append(c.value < 0 ? "contents not seen" : Format.number(c.value));
		int moons = c.moonCount();
		if (moons > 0)
		{
			b.append(", ").append(Format.plural(moons, "moon", "moons"));
		}
		if (!c.pieces.isEmpty())
		{
			b.append(". ").append(String.join(", ", c.pieces)).append('!');
		}
		else if (!c.items.isEmpty())
		{
			b.append(", ").append(String.join(", ", c.items.subList(0, Math.min(2, c.items.size()))));
			b.append(c.items.size() > 2 ? " and more." : ".");
		}
		else
		{
			b.append('.');
		}
		return b.toString();
	}

	/** The three sets: four pieces each, lit when owned, the dry meter and the dry spell in words. */
	static JComponent sets(LunarRun r, int width)
	{
		Card card = new Card(new BorderLayout(0, 6)).accent(r.getMissing().isEmpty() ? Theme.GREEN : Theme.VIOLET);
		StackPanel body = new StackPanel(9, 0);
		for (Moons.Moon m : Moons.Moon.values())
		{
			StackPanel one = new StackPanel(3, 0);
			one.add(new SetRow(r, m));
			String dry = r.dryText(m);
			int missing = Moons.missingOf(m, r.getMissing());
			String text;
			if (missing == 0)
			{
				text = "Set complete. " + (dry == null ? "" : dry + ".");
			}
			else
			{
				text = (dry == null ? Moons.oneIn(Moons.SET_CHANCE) + " a kill for a piece you lack" : dry)
					+ (missing == 4 ? ". No piece yet." : ". Still to find: " + missingNames(m, r) + ".");
			}
			one.add(Ui.wrapped(text.trim(), Theme.small(), r.spell(m) >= 2 && missing > 0 ? Theme.EMBER : Theme.TEXT_MUTED, width));
			body.add(one);
		}
		body.add(Ui.wrapped("Each moon subdued is a 1 in 56 chance at a piece of its set, and you get every piece of a set before a"
			+ " duplicate (each piece 1 in 224 as the wiki lists it). About " + Moons.CHESTS_FOR_ALL_SETS
			+ " chests finish all three sets.", Theme.small(), Theme.TEXT_FAINT, width));
		body.add(Ui.wrapped("Owned means seen in your bank, on you or in your chests. Your collection log page for the moons is in"
			+ " Codex Log Insight.", Theme.small(), Theme.TEXT_FAINT, width));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	static String missingNames(Moons.Moon m, LunarRun r)
	{
		StringBuilder b = new StringBuilder();
		for (Moons.Piece p : m.getPieces())
		{
			if (!r.owns(p.getName()))
			{
				b.append(b.length() == 0 ? "" : ", ").append(p.slotName());
			}
		}
		return b.toString();
	}

	/** A moon disc, lit in its colour when down, or dark with a thin crescent when not. */
	static void disc(Graphics2D g, Moons.Moon m, int x, int y, int d, boolean down)
	{
		Color tone = tone(m);
		Ellipse2D face = new Ellipse2D.Float(x, y, d, d);
		if (down)
		{
			g.setPaint(new RadialGradientPaint(x + d * 0.38f, y + d * 0.34f, d * 0.7f, new float[]{0f, 0.6f, 1f},
				new Color[]{Theme.mix(tone, Color.WHITE, 0.45), tone, Theme.mix(tone, Theme.INK_DEEP, 0.55)}));
			g.fill(face);
			g.setColor(Theme.alpha(tone, 60));
			g.setStroke(new BasicStroke(2f));
			g.draw(new Ellipse2D.Float(x - 3, y - 3, d + 6, d + 6));
		}
		else
		{
			g.setPaint(new GradientPaint(0, y, Theme.CARD_TOP, 0, y + d, Theme.INK_DEEP));
			g.fill(face);
			// The lit edge of a moon not yet down: a crescent in its colour.
			Area crescent = new Area(face);
			crescent.subtract(new Area(new Ellipse2D.Float(x + d * 0.22f, y - d * 0.04f, d, d)));
			g.setColor(Theme.alpha(tone, 150));
			g.fill(crescent);
		}
		g.setStroke(new BasicStroke(1f));
		g.setColor(down ? Theme.mix(tone, Theme.GOLD_BRIGHT, 0.4) : Theme.STROKE);
		g.draw(face);
	}

	/** The three moons in a row, each lit when down this run. */
	static final class Phases extends JComponent
	{
		private static final int H = 48;
		private final LunarRun run;

		Phases(LunarRun run)
		{
			this.run = run;
			setOpaque(false);
			setPreferredSize(new Dimension(10, H));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			Moons.Moon[] ms = Moons.Moon.values();
			int w = getWidth();
			int cell = w / ms.length;
			int d = Math.min(28, cell - 10);
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			for (int i = 0; i < ms.length; i++)
			{
				Moons.Moon m = ms[i];
				boolean down = run.getSubdued().contains(m);
				int cx = i * cell + cell / 2;
				disc(g, m, cx - d / 2, 4, d, down);
				String label = m.getShortName();
				Ui.shadowed(g, label, cx - fm.stringWidth(label) / 2, 4 + d + 13, down ? Theme.TEXT : Theme.TEXT_FAINT);
			}
			g.dispose();
		}
	}

	/** A moon's row: its disc, name and rate, its four pieces as pips, and the dry meter. */
	static final class SetRow extends JComponent
	{
		private static final int H = 44;
		private final LunarRun run;
		private final Moons.Moon moon;

		SetRow(LunarRun run, Moons.Moon moon)
		{
			this.run = run;
			this.moon = moon;
			setOpaque(false);
			setPreferredSize(new Dimension(10, H));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			Color tone = tone(moon);
			int have = 4 - Moons.missingOf(moon, run.getMissing());
			disc(g, moon, 1, 3, 20, have == 4);

			int tx = 30;
			g.setFont(Theme.body());
			Ui.shadowed(g, moon.getName(), tx, 11, have == 4 ? Theme.GOLD_BRIGHT : Theme.TEXT);
			g.setFont(Theme.small());
			FontMetrics sm = g.getFontMetrics();
			String right = have + " of 4";
			Ui.shadowed(g, right, w - sm.stringWidth(right), 11, have == 4 ? Theme.GREEN : Theme.GOLD_DIM);

			// Four pips, one a piece, lit in the moon's colour when owned.
			int gap = 3;
			int pw = (w - tx - gap * 3) / 4;
			int py = 17;
			for (int j = 0; j < moon.getPieces().size(); j++)
			{
				Moons.Piece p = moon.getPieces().get(j);
				boolean got = run.owns(p.getName());
				int x = tx + j * (pw + gap);
				RoundRectangle2D pip = new RoundRectangle2D.Float(x, py, pw, 11, 5, 5);
				if (got)
				{
					g.setPaint(new GradientPaint(0, py, Theme.mix(tone, Color.WHITE, 0.25), 0, py + 11, Theme.mix(tone, Theme.INK_DEEP, 0.35)));
				}
				else
				{
					g.setPaint(new GradientPaint(0, py, Theme.INK_DEEP, 0, py + 11, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
				}
				g.fill(pip);
				g.setColor(got ? Theme.alpha(Color.WHITE, 40) : Theme.STROKE_SOFT);
				g.draw(pip);
				String slot = Ui.clip(p.getSlot(), sm, pw - 2);
				int sx = x + (pw - sm.stringWidth(slot)) / 2;
				if (got)
				{
					g.setColor(Theme.INK_DEEP);
					g.drawString(slot, sx, py + 9);
				}
				else
				{
					g.setColor(Theme.TEXT_FAINT);
					g.drawString(slot, sx, py + 9);
				}
			}

			// The dry meter: kills of this moon since its last piece against 1 in 56, ticked at once and twice the rate.
			int my = 34;
			int mh = 6;
			int mw = w - tx;
			g.setPaint(new GradientPaint(0, my, Theme.INK_DEEP, 0, my + mh, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
			g.fill(new RoundRectangle2D.Float(tx, my, mw, mh, mh, mh));
			g.setColor(Theme.STROKE_SOFT);
			g.draw(new RoundRectangle2D.Float(tx, my, mw - 1, mh, mh, mh));
			double spell = have == 4 ? 0 : run.spell(moon);
			if (spell > 0)
			{
				int fw = (int) Math.round(mw * Math.min(1, spell / METER_SPAN));
				Color end = spell >= 2 ? Theme.EMBER : spell >= 1 ? Theme.GOLD_BRIGHT : Theme.mix(tone, Theme.GOLD, 0.4);
				g.setPaint(new GradientPaint(tx, 0, Theme.mix(tone, Theme.INK_DEEP, 0.4), tx + Math.max(4, fw), 0, end));
				g.fill(new RoundRectangle2D.Float(tx, my, Math.max(4, fw), mh, mh, mh));
			}
			for (int k = 1; k < METER_SPAN; k++)
			{
				int mx = tx + (int) Math.round(mw * k / METER_SPAN);
				g.setColor(spell >= k ? Theme.alpha(Theme.INK_DEEP, 170) : Theme.alpha(Theme.GOLD_DIM, 170));
				g.drawLine(mx, my + 1, mx, my + mh - 1);
			}
			g.dispose();
		}
	}
}
