package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.Gauntlet;
import com.codex.bosspilot.core.GauntletLog;
import com.codex.bosspilot.core.GauntletRun;
import com.codex.bosspilot.core.GauntletWatch;
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
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The Gauntlet on the Prepare and Trip tabs: the run under way (the preparation clock against
 * the game's limit and the player's best; once the Hunllef fight begins, nothing moves), the
 * player's own runs with each one's preparation and fight split, the ghost of their best splits
 * in each mode, and the four rare rewards with each dry spell against its rate. Nothing here is
 * about the fight itself.
 */
final class GauntletCard
{
	/** The dry meter runs to this many times a reward's rate. */
	static final double METER_SPAN = 3;
	/** Recent runs listed. */
	static final int RECENT = 6;
	/** The preparation's colour, and the fight's. */
	static final Color PREP = Theme.TEAL;
	static final Color FIGHT = Theme.EMBER;
	/** The corrupted mode's colour. */
	static final Color CORRUPT = new Color(206, 84, 96);

	private GauntletCard()
	{
	}

	static Color tone(Gauntlet.Mode m)
	{
		return m == Gauntlet.Mode.CORRUPTED ? CORRUPT : Theme.TEAL;
	}

	/** The run under way. */
	static JComponent run(GauntletRun r, int width)
	{
		Card card = new Card(new BorderLayout(0, 6)).accent(r.getPhase() == GauntletWatch.Phase.NONE ? Theme.GOLD_DIM : tone(r.getMode()));
		StackPanel body = new StackPanel(5, 0);
		if (r.getPhase() == GauntletWatch.Phase.NONE)
		{
			body.add(Ui.wrapped("Between runs. Your next run is timed from the moment you step in: the preparation, then the Hunllef.",
				Theme.small(), Theme.TEXT_MUTED, width));
		}
		else
		{
			body.add(new PrepClock(r));
			body.add(Ui.wrapped(r.phaseText() + ".", Theme.small(), Theme.TEXT, width));
			if (r.getPhase() == GauntletWatch.Phase.FIGHT)
			{
				body.add(Ui.wrapped("Nothing here moves during the fight. The run is logged when it ends.", Theme.small(), Theme.TEXT_FAINT, width));
			}
			else if (r.getPhase() == GauntletWatch.Phase.PREP && r.ghostPrep() > 0)
			{
				body.add(Ui.wrapped("The gold tick is your best preparation in the " + r.getMode().getLabel().toLowerCase(Locale.ROOT) + " Gauntlet, "
					+ Format.clock(r.ghostPrep()) + ".", Theme.small(), Theme.TEXT_MUTED, width));
			}
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** The player's own runs. */
	static JComponent runs(GauntletRun r, int width)
	{
		GauntletLog log = r.getLog();
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.GOLD_DIM);
		StackPanel body = new StackPanel(5, 0);
		if (log.isEmpty())
		{
			body.add(Ui.wrapped("Your runs show here from your next one: the preparation and the Hunllef fight each timed, how it ended,"
				+ " your completion count in each mode and the ghost of your best splits. Kept on this computer only.",
				Theme.small(), Theme.TEXT_MUTED, width));
		}
		else
		{
			JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
			grid.setOpaque(false);
			grid.add(new StatTile("Gauntlet KC", kc(log.regular), log.regular.completed > 0 || log.regular.kc > 0));
			grid.add(new StatTile("Corrupted KC", kc(log.corrupted), log.corrupted.completed > 0 || log.corrupted.kc > 0));
			double cph = log.completionsPerHour();
			double dph = log.deathsPerHour();
			grid.add(new StatTile("KC/hr", cph < 0 ? "-" : String.format(Locale.ROOT, "%.1f", cph), cph > 0));
			grid.add(new StatTile("Deaths", Format.number(log.died()) + (dph > 0 ? String.format(Locale.ROOT, " (%.1f/hr)", dph) : ""),
				log.died() > 0));
			body.add(grid);
			List<GauntletLog.Run> recent = log.recent;
			double scale = 0;
			for (GauntletLog.Run run : recent.subList(Math.max(0, recent.size() - RECENT), recent.size()))
			{
				scale = Math.max(scale, Math.max(0, run.prep) + Math.max(0, run.fight));
			}
			for (int i = recent.size() - 1; i >= Math.max(0, recent.size() - RECENT); i--)
			{
				body.add(new RunRow(recent.get(i), scale, log.totals(recent.get(i).mode)));
			}
			body.add(Ui.wrapped("Teal is the preparation, ember the Hunllef. Times are the game's own where it gave them, else counted in ticks.",
				Theme.small(), Theme.TEXT_FAINT, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** "41" from the game's count, or the completions seen. */
	static String kc(GauntletLog.Totals t)
	{
		return Format.number(t.kc > 0 ? t.kc : t.completed);
	}

	/** The ghost: the best preparation and fight in each mode played, against the average. */
	static JComponent ghost(GauntletRun r, int width)
	{
		GauntletLog log = r.getLog();
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.GOLD);
		StackPanel body = new StackPanel(6, 0);
		boolean any = false;
		for (Gauntlet.Mode m : Gauntlet.Mode.values())
		{
			GauntletLog.Totals t = log.totals(m);
			if (t.completed == 0)
			{
				continue;
			}
			any = true;
			body.add(new GhostRow(m, t));
			double ghost = t.ghostTotal();
			StringBuilder b = new StringBuilder();
			if (ghost > 0)
			{
				b.append("Your ghost: ").append(Format.clock(ghost));
				if (t.bestTotal > 0 && t.bestTotal > ghost + 0.5)
				{
					b.append(", ").append(Gauntlet.delta(ghost - t.bestTotal)).append(" on your best whole run of ").append(Format.clock(t.bestTotal));
				}
				b.append('.');
			}
			if (t.averagePrep() > 0)
			{
				b.append(b.length() > 0 ? " " : "").append("Average prep ").append(Format.clock(t.averagePrep()))
					.append(", fight ").append(Format.clock(t.averageFight())).append('.');
			}
			if (b.length() > 0)
			{
				body.add(Ui.wrapped(b.toString(), Theme.small(), Theme.TEXT_MUTED, width));
			}
		}
		if (!any)
		{
			body.add(Ui.wrapped("Complete a run and its preparation and fight become the ghost to beat.", Theme.small(), Theme.TEXT_MUTED, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** The four rare rewards, each with its rates and the player's dry spell. */
	static JComponent uniques(GauntletRun r, int width)
	{
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.VIOLET);
		StackPanel body = new StackPanel(8, 0);
		for (Gauntlet.Unique u : Gauntlet.Unique.values())
		{
			StackPanel one = new StackPanel(2, 0);
			one.add(new DropRow(r, u));
			String dry = r.dryText(u);
			one.add(Ui.wrapped(dry == null ? u.rateText() + " a completed run." : dry + ".", Theme.small(),
				r.spell(u) >= 2 ? Theme.EMBER : Theme.TEXT_MUTED, width));
			body.add(one);
		}
		body.add(Ui.wrapped("Only a completed run rolls them. Owned means received while Boss Pilot watched, or seen in your bank or pack.",
			Theme.small(), Theme.TEXT_FAINT, width));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	static Color dropTone(Gauntlet.Unique u)
	{
		switch (u)
		{
			case ENHANCED:
				return Theme.GOLD;
			case ARMOUR:
				return Theme.TEAL;
			case WEAPON:
				return new Color(112, 160, 226);
			default:
				return CORRUPT;
		}
	}

	/** A rounded bar track. */
	static void track(Graphics2D g, int x, int y, int w, int h)
	{
		g.setPaint(new GradientPaint(0, y, Theme.INK_DEEP, 0, y + h, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
		g.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
		g.setColor(Theme.STROKE_SOFT);
		g.draw(new RoundRectangle2D.Float(x, y, w - 1, h, h, h));
	}

	/** A filled stretch of a bar in one colour, lit at its top. */
	static void fill(Graphics2D g, int x, int y, int w, int h, Color c)
	{
		if (w <= 0)
		{
			return;
		}
		g.setPaint(new GradientPaint(0, y, Theme.mix(c, Color.WHITE, 0.18), 0, y + h, Theme.mix(c, Theme.INK_DEEP, 0.35)));
		g.fill(new RoundRectangle2D.Float(x, y, Math.max(h, w), h, h, h));
	}

	/** The ghost's mark: a gold tick with a small diamond above it. */
	static void ghostMark(Graphics2D g, int x, int y, int h)
	{
		g.setColor(Theme.GOLD_BRIGHT);
		g.setStroke(new BasicStroke(1.5f));
		g.drawLine(x, y - 1, x, y + h + 1);
		g.fill(new Polygon(new int[]{x, x + 3, x, x - 3}, new int[]{y - 6, y - 3, y, y - 3}, 4));
		g.setStroke(new BasicStroke(1f));
	}

	/**
	 * The preparation against the game's limit, with the player's best as a gold tick. During the
	 * preparation it fills as the clock runs; once the fight begins it stands still at how long the
	 * preparation took.
	 */
	static final class PrepClock extends JComponent
	{
		private static final int H = 34;
		private final GauntletRun run;

		PrepClock(GauntletRun run)
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
			int w = getWidth();
			Gauntlet.Mode mode = run.getMode();
			double limit = mode.getPrepLimit();
			boolean prep = run.getPhase() == GauntletWatch.Phase.PREP;
			double at = prep ? run.getPrepNow() : run.getPrepTaken();
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			String left = prep ? "PREPARATION" : run.getPhase() == GauntletWatch.Phase.FIGHT ? "PREPARATION TAKEN" : "RUN OVER";
			Ui.shadowed(g, left, 0, fm.getAscent(), tone(mode));
			String right = at > 0 ? Format.clock(at) + " / " + Format.clock(limit) : Format.clock(limit) + " limit";
			Ui.shadowed(g, right, w - fm.stringWidth(right), fm.getAscent(), Theme.TEXT);
			int by = 18;
			int bh = 9;
			track(g, 0, by, w, bh);
			if (at > 0)
			{
				fill(g, 0, by, (int) Math.round(w * Math.min(1, at / limit)), bh, PREP);
			}
			double ghost = run.ghostPrep();
			if (ghost > 0 && ghost < limit)
			{
				ghostMark(g, (int) Math.round(w * ghost / limit), by, bh);
			}
			g.dispose();
		}
	}

	/** One run: its mode, how it ended, its preparation and fight as a bar, and the times. */
	static final class RunRow extends JComponent
	{
		private static final int H = 30;
		private final GauntletLog.Run run;
		private final double scale;
		private final GauntletLog.Totals totals;

		RunRow(GauntletLog.Run run, double scale, GauntletLog.Totals totals)
		{
			this.run = run;
			this.scale = scale;
			this.totals = totals;
			setOpaque(false);
			setPreferredSize(new Dimension(10, H));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			Color tone = tone(run.mode);
			// The mode's badge: R or C.
			g.setPaint(new GradientPaint(0, 2, Theme.mix(tone, Theme.INK_DEEP, 0.35), 0, 20, Theme.mix(tone, Theme.INK_DEEP, 0.7)));
			g.fill(new RoundRectangle2D.Float(0, 2, 18, 18, 6, 6));
			g.setColor(Theme.alpha(tone, 200));
			g.draw(new RoundRectangle2D.Float(0, 2, 17, 17, 6, 6));
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			String badge = run.mode == Gauntlet.Mode.CORRUPTED ? "C" : "R";
			Ui.shadowed(g, badge, 9 - fm.stringWidth(badge) / 2, 11 + fm.getAscent() / 2 - 1, Theme.TEXT);

			int tx = 24;
			String what;
			Color c;
			switch (run.outcome)
			{
				case COMPLETED:
					what = run.kc > 0 ? "KC " + Format.number(run.kc) : "Completed";
					c = Theme.GREEN;
					break;
				case DIED:
					what = run.fight > 0 ? "Died, fight" : "Died, prep";
					c = Theme.BLOOD;
					break;
				default:
					what = run.fight > 0 ? "Left, fight" : "Left, prep";
					c = Theme.TEXT_MUTED;
			}
			if (!run.uniques.isEmpty())
			{
				what += " + " + String.join(", ", run.uniques);
				c = Theme.VIOLET;
			}
			String times = run.fight > 0 ? Gauntlet.clock(run.prep) + " + " + Gauntlet.clock(run.fight) : Gauntlet.clock(run.prep);
			int tw = fm.stringWidth(times);
			Ui.shadowed(g, Ui.clip(what, fm, w - tx - tw - 8), tx, fm.getAscent() - 1, c);
			Ui.shadowed(g, times, w - tw, fm.getAscent() - 1, Theme.TEXT);

			// The split: the preparation in teal and the fight in ember, to the same scale for every row.
			int by = 16;
			int bh = 7;
			int bw = w - tx;
			track(g, tx, by, bw, bh);
			if (scale > 0)
			{
				int pw = run.prep > 0 ? (int) Math.round(bw * run.prep / scale) : 0;
				int fw = run.fight > 0 ? (int) Math.round(bw * run.fight / scale) : 0;
				if (fw > 0)
				{
					fill(g, tx, by, pw + fw, bh, FIGHT);
				}
				if (pw > 0)
				{
					fill(g, tx, by, pw, bh, PREP);
				}
				if (totals.bestPrep > 0 && run.prep > 0)
				{
					int gx = tx + (int) Math.round(bw * Math.min(1, totals.bestPrep / scale));
					g.setColor(Theme.GOLD_BRIGHT);
					g.drawLine(gx, by - 1, gx, by + bh);
				}
			}
			g.dispose();
		}
	}

	/** One mode's ghost: the best preparation and fight as one bar, the average beneath it. */
	static final class GhostRow extends JComponent
	{
		private static final int H = 44;
		private final Gauntlet.Mode mode;
		private final GauntletLog.Totals t;

		GhostRow(Gauntlet.Mode mode, GauntletLog.Totals t)
		{
			this.mode = mode;
			this.t = t;
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
			FontMetrics fm = g.getFontMetrics();
			Ui.shadowed(g, mode.getLabel(), 0, fm.getAscent() - 2, tone(mode));
			g.setFont(Theme.small());
			FontMetrics sm = g.getFontMetrics();
			String right = "prep " + Gauntlet.clock(t.bestPrep) + "  fight " + Gauntlet.clock(t.bestFight);
			Ui.shadowed(g, right, w - sm.stringWidth(right), fm.getAscent() - 2, Theme.GOLD_BRIGHT);
			double avgP = Math.max(0, t.averagePrep());
			double avgF = Math.max(0, t.averageFight());
			double scale = Math.max(Math.max(0, t.bestPrep) + Math.max(0, t.bestFight), avgP + avgF);
			int lx = sm.stringWidth("Average") + 8;
			int bw = w - lx;
			int y1 = 20;
			int y2 = 32;
			int bh = 7;
			Ui.shadowed(g, "Best", 0, y1 + bh, Theme.TEXT_MUTED);
			Ui.shadowed(g, "Average", 0, y2 + bh, Theme.TEXT_FAINT);
			track(g, lx, y1, bw, bh);
			track(g, lx, y2, bw, bh);
			if (scale > 0)
			{
				bar(g, lx, y1, bw, bh, t.bestPrep, t.bestFight, scale, 1);
				bar(g, lx, y2, bw, bh, avgP, avgF, scale, 0.55);
			}
			g.dispose();
		}

		private static void bar(Graphics2D g, int x, int y, int w, int h, double prep, double fight, double scale, double strength)
		{
			int pw = prep > 0 ? (int) Math.round(w * prep / scale) : 0;
			int fw = fight > 0 ? (int) Math.round(w * fight / scale) : 0;
			if (fw > 0)
			{
				fill(g, x, y, pw + fw, h, Theme.mix(Theme.CARD, FIGHT, strength));
			}
			if (pw > 0)
			{
				fill(g, x, y, pw, h, Theme.mix(Theme.CARD, PREP, strength));
			}
		}
	}

	/** A gem lit when owned, the reward's name and rates, and a meter of the dry spell against the rate. */
	static final class DropRow extends JComponent
	{
		private static final int H = 30;
		private final GauntletRun run;
		private final Gauntlet.Unique unique;

		DropRow(GauntletRun run, Gauntlet.Unique unique)
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
			Color tone = dropTone(unique);
			boolean owned = run.owns(unique);
			int gx = 10;
			int gy = 13;
			int gr = 9;
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

			int tx = 26;
			g.setFont(Theme.small());
			FontMetrics sm = g.getFontMetrics();
			String right = owned ? "owned" : Zrate.of(unique);
			int rw = sm.stringWidth(right);
			g.setFont(Theme.body());
			FontMetrics fm = g.getFontMetrics();
			String name = fm.stringWidth(unique.getName()) <= w - tx - rw - 6 ? unique.getName() : unique.getShortName();
			Ui.shadowed(g, Ui.clip(name, fm, w - tx - rw - 6), tx, 11, owned ? Theme.TEXT : Theme.TEXT_MUTED);
			g.setFont(Theme.small());
			Ui.shadowed(g, right, w - rw, 11, owned ? Theme.GREEN : Theme.GOLD_DIM);

			int my = 18;
			int mh = 7;
			int mw = w - tx;
			track(g, tx, my, mw, mh);
			double spell = run.spell(unique);
			if (spell > 0)
			{
				int fw = (int) Math.round(mw * Math.min(1, spell / METER_SPAN));
				Color end = spell >= 2 ? Theme.EMBER : spell >= 1 ? Theme.GOLD_BRIGHT : Theme.mix(tone, Theme.GOLD, 0.4);
				g.setPaint(new GradientPaint(tx, 0, Theme.mix(tone, Theme.INK_DEEP, 0.4), tx + Math.max(4, fw), 0, end));
				g.fill(new RoundRectangle2D.Float(tx, my, Math.max(4, fw), mh, mh, mh));
			}
			for (int m = 1; m < METER_SPAN; m++)
			{
				int mx = tx + (int) Math.round(mw * m / METER_SPAN);
				g.setColor(spell >= m ? Theme.alpha(Theme.INK_DEEP, 170) : Theme.alpha(Theme.GOLD_DIM, 170));
				g.drawLine(mx, my + 1, mx, my + mh - 1);
			}
			g.dispose();
		}
	}

	/** The short rates beside a reward: "1/2,000 · 1/400". */
	static final class Zrate
	{
		private Zrate()
		{
		}

		static String of(Gauntlet.Unique u)
		{
			return frac(u.chance(Gauntlet.Mode.REGULAR)) + " / " + frac(u.chance(Gauntlet.Mode.CORRUPTED));
		}

		static String frac(double p)
		{
			return "1/" + Format.number(Math.round(1 / p));
		}
	}
}
