package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.Barrows;
import com.codex.bosspilot.core.BarrowsRun;
import com.codex.bosspilot.core.ChestLog;
import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.Logbook;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Barrows on the Prepare and Trip tabs: the run under way (six seals for the brothers, the crypt
 * with the tunnel, a potential gauge marked with the reward bands and the tunnel kills to the aim),
 * the player's own chests (loot an hour, the dry streak and its odds) and the set still to finish.
 */
final class BarrowsCard
{
	/** Chest (Barrows), "RP for max quantity": coins 380, mind 505, chaos 630, death 755, blood 880, bolt racks 1005; key halves 1006, dragon med helm 1012. */
	static final int[] BANDS = {380, 505, 630, 755, 880, 1005, 1012};
	/** Missing pieces named in words at most; past this the grid says it. */
	static final int LIST_MISSING = 6;

	private BarrowsCard()
	{
	}

	/** The run under way. */
	static JComponent run(BarrowsRun r, int width)
	{
		Card card = new Card(new BorderLayout(0, 6)).accent(r.getKilled().size() == 6 ? Theme.GREEN : Theme.GOLD).highlighted(true);
		StackPanel body = new StackPanel(5, 0);
		body.add(new Seals(r));
		body.add(Ui.wrapped(r.brothersText() + ".", Theme.small(), Theme.TEXT, width));
		body.add(Ui.wrapped(r.tunnelText() + (r.getTunnel() == null ? ": Boss Pilot marks it when you find it." : "."), Theme.small(),
			r.getTunnel() == null ? Theme.TEXT_MUTED : Theme.GOLD, width));
		body.add(new Gauge(r));
		String plan = r.planText();
		if (plan != null)
		{
			Barrows.Plan p = r.getPlan();
			String tail = p.reached() || p.getKills().isEmpty() ? "." : " (+" + p.getAdds() + ", to " + p.result() + "), counting the brothers still to kill.";
			body.add(Ui.wrapped(plan + tail, Theme.small(), p.reached() ? Theme.GREEN : Theme.TEAL, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** The player's own chests. */
	static JComponent chests(BarrowsRun r, Logbook.Rates rates, int width)
	{
		ChestLog log = r.getLog();
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.GOLD_DIM);
		StackPanel body = new StackPanel(5, 0);
		if (log.isEmpty())
		{
			body.add(Ui.wrapped("Your chests show here once you open one: what each was worth, loot an hour and how long since your last piece."
				+ " Kept on this computer only.", Theme.small(), Theme.TEXT_MUTED, width));
		}
		else
		{
			JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
			grid.setOpaque(false);
			double lph = log.lootPerHour();
			double cph = log.chestsPerHour();
			grid.add(new StatTile("Chests", Format.number(log.chests), true));
			grid.add(new StatTile("Loot/hr", lph < 0 ? "-" : Format.number(Math.round(lph)), lph > 0));
			grid.add(new StatTile("Average", log.average() < 0 ? "-" : Format.number(log.average()), log.average() > 0));
			grid.add(new StatTile("Chests/hr", cph < 0 ? "-" : String.format(Locale.ROOT, "%.1f", cph), cph > 0));
			body.add(grid);
			ChestLog.Chest last = log.last();
			if (last != null)
			{
				body.add(Ui.wrapped(lastText(last), Theme.small(), last.pieces.isEmpty() ? Theme.TEXT_MUTED : Theme.GREEN, width));
			}
			String dry = r.dryText();
			if (dry != null)
			{
				body.add(Ui.wrapped(dry + ".", Theme.small(), log.dryOdds() < 0.25 ? Theme.EMBER : Theme.TEXT, width));
			}
			if (log.timed < log.chests)
			{
				body.add(Ui.wrapped("Loot an hour counts the " + Format.plural(log.timed, "run", "runs") + " timed from start to chest.",
					Theme.small(), Theme.TEXT_FAINT, width));
			}
		}
		if (rates != null && rates.kills > 0 && rates.dosesPerKill() >= 0)
		{
			body.add(Ui.wrapped(String.format(Locale.ROOT, "Your runs use about %.1f prayer doses and %d hitpoints of food a chest.",
				rates.dosesPerKill(), Math.round(rates.foodPerKill())), Theme.small(), Theme.TEXT, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	static String lastText(ChestLog.Chest c)
	{
		StringBuilder b = new StringBuilder("Last chest: ");
		b.append(c.value < 0 ? "contents not seen" : Format.number(c.value));
		if (c.brothers > 0)
		{
			b.append(", ").append(Format.plural(c.brothers, "brother", "brothers"));
		}
		if (c.potential > 0)
		{
			b.append(", potential ").append(c.potential);
		}
		if (!c.pieces.isEmpty())
		{
			b.append(". ").append(String.join(", ", c.pieces)).append('!');
		}
		else
		{
			b.append('.');
		}
		return b.toString();
	}

	/** The 24 pieces, owned or not. */
	static JComponent set(BarrowsRun r, int width)
	{
		List<String> missing = r.getMissing();
		int owned = Barrows.Brother.values().length * Barrows.PIECES_PER_BROTHER - missing.size();
		Card card = new Card(new BorderLayout(0, 6)).accent(missing.isEmpty() ? Theme.GREEN : Theme.VIOLET);
		StackPanel body = new StackPanel(5, 0);
		body.add(new SetGrid(r));
		if (missing.isEmpty())
		{
			body.add(Ui.wrapped("All 24 pieces seen in your bank, on you or in your chests.", Theme.small(), Theme.GREEN, width));
		}
		else
		{
			double next = r.nextNew();
			body.add(Ui.wrapped(owned + " of 24 pieces seen in your bank, on you or in your chests."
				+ (missing.size() <= LIST_MISSING ? " Still to find: " + String.join(", ", missing) + "." : ""),
				Theme.small(), Theme.TEXT, width));
			body.add(Ui.wrapped("Next chest" + (r.getKilled().isEmpty() ? " with all six brothers" : " with the brothers down so far") + ": "
				+ Barrows.oneIn(next) + " to bring one of them (any piece: " + Barrows.oneIn(Barrows.chanceAny(r.getKilled().isEmpty() ? 6 : r.getKilled().size())) + ").",
				Theme.small(), Theme.VIOLET, width));
		}
		body.add(Ui.wrapped("Your collection log is not read, so a piece sold or used up before Boss Pilot saw it shows as still to find.",
			Theme.small(), Theme.TEXT_FAINT, width));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** Six seals, one a brother: lit gold when killed this run, ringed in teal for the crypt with the tunnel. */
	static final class Seals extends JComponent
	{
		private static final int H = 34;
		private final BarrowsRun run;

		Seals(BarrowsRun run)
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
			Barrows.Brother[] bs = Barrows.Brother.values();
			int w = getWidth();
			int cell = w / bs.length;
			int d = Math.min(28, cell - 4);
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			for (int i = 0; i < bs.length; i++)
			{
				Barrows.Brother b = bs[i];
				boolean down = run.getKilled().contains(b);
				boolean tunnel = b == run.getTunnel();
				int cx = i * cell + cell / 2;
				int x = cx - d / 2;
				int y = 1;
				Ellipse2D disc = new Ellipse2D.Float(x, y, d, d);
				if (down)
				{
					g.setPaint(new GradientPaint(0, y, Theme.GOLD_BRIGHT, 0, y + d, Theme.GOLD_DIM));
				}
				else
				{
					g.setPaint(new GradientPaint(0, y, Theme.CARD_TOP, 0, y + d, Theme.INK_DEEP));
				}
				g.fill(disc);
				g.setStroke(new BasicStroke(tunnel ? 2f : 1f));
				g.setColor(tunnel ? Theme.TEAL : down ? Theme.GOLD : Theme.STROKE);
				g.draw(disc);
				if (tunnel)
				{
					g.setColor(Theme.alpha(Theme.TEAL, 70));
					g.draw(new Ellipse2D.Float(x - 3, y - 3, d + 6, d + 6));
				}
				String initial = b.getShortName().substring(0, 1);
				int iw = fm.stringWidth(initial);
				int ty = y + d / 2 + fm.getAscent() / 2 - 2;
				if (down)
				{
					g.setColor(Theme.INK_DEEP);
					g.drawString(initial, cx - iw / 2, ty);
				}
				else
				{
					Ui.shadowed(g, initial, cx - iw / 2, ty, tunnel ? Theme.TEAL : Theme.TEXT_MUTED);
				}
				if (tunnel)
				{
					// A small mark under the crypt with the tunnel.
					g.setColor(Theme.TEAL);
					g.fillPolygon(new int[]{cx - 3, cx + 3, cx}, new int[]{y + d + 6, y + d + 6, y + d + 2}, 3);
				}
			}
			g.dispose();
		}
	}

	/** Reward potential along 0 to 1012, with the reward bands ticked and the aim and plan marked. */
	static final class Gauge extends JComponent
	{
		private static final int H = 34;
		private final BarrowsRun run;

		Gauge(BarrowsRun run)
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
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			String label = run.potentialText();
			Ui.shadowed(g, label, 0, 11, Theme.TEXT_MUTED);
			Barrows.Plan p = run.getPlan();
			if (p != null)
			{
				String aim = "aim " + p.getAim();
				Ui.shadowed(g, aim, w - fm.stringWidth(aim), 11, Theme.TEAL);
			}
			int y = 17;
			int bh = 8;
			g.setPaint(new GradientPaint(0, y, Theme.INK_DEEP, 0, y + bh, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
			g.fill(new RoundRectangle2D.Float(0, y, w, bh, bh, bh));
			g.setColor(Theme.STROKE_SOFT);
			g.draw(new RoundRectangle2D.Float(0, y, w - 1, bh, bh, bh));
			int now = run.potential();
			if (p != null && !p.reached() && p.getAdds() > 0)
			{
				// The brothers still to kill and the planned tunnel kills, as a faint lead.
				int to = x(p.result(), w);
				g.setColor(Theme.alpha(Theme.TEAL, 70));
				g.fill(new RoundRectangle2D.Float(0, y, Math.max(4, to), bh, bh, bh));
			}
			int fw = x(now, w);
			if (fw > 0)
			{
				g.setPaint(new GradientPaint(0, 0, Theme.GOLD_DIM, w, 0, Theme.GOLD_BRIGHT));
				g.fill(new RoundRectangle2D.Float(0, y, Math.max(4, fw), bh, bh, bh));
				g.setColor(Theme.alpha(Color.WHITE, 40));
				g.drawLine(2, y + 1, Math.max(4, fw) - 2, y + 1);
			}
			for (int band : BANDS)
			{
				int bx = x(band, w);
				g.setColor(band <= now ? Theme.alpha(Theme.INK_DEEP, 160) : Theme.alpha(Theme.GOLD_DIM, 150));
				g.drawLine(bx, y + 1, bx, y + bh - 1);
			}
			if (p != null)
			{
				int ax = x(p.getAim(), w);
				g.setColor(Theme.TEAL);
				g.fillPolygon(new int[]{ax - 4, ax + 4, ax}, new int[]{y + bh + 7, y + bh + 7, y + bh + 2}, 3);
			}
			g.dispose();
		}

		private static int x(int potential, int w)
		{
			return (int) Math.round((w - 1) * Math.min(1.0, Math.max(0, potential) / (double) Barrows.MAX_POTENTIAL));
		}
	}

	/** Six rows of four pips: a piece owned is lit in its brother's row. */
	static final class SetGrid extends JComponent
	{
		private static final int ROW = 15;
		private final BarrowsRun run;

		SetGrid(BarrowsRun run)
		{
			this.run = run;
			setOpaque(false);
			setPreferredSize(new Dimension(10, ROW * Barrows.Brother.values().length));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setFont(Theme.small());
			int w = getWidth();
			int labelW = 52;
			int gap = 4;
			int pw = (w - labelW - gap * 3) / Barrows.PIECES_PER_BROTHER;
			List<String> missing = run.getMissing();
			Barrows.Brother[] bs = Barrows.Brother.values();
			for (int i = 0; i < bs.length; i++)
			{
				Barrows.Brother b = bs[i];
				int y = i * ROW;
				int have = 0;
				for (String piece : b.getPieces())
				{
					have += missing.contains(piece) ? 0 : 1;
				}
				Ui.shadowed(g, b.getShortName(), 0, y + 11, have == 4 ? Theme.GOLD : Theme.TEXT_MUTED);
				for (int j = 0; j < b.getPieces().size(); j++)
				{
					boolean got = !missing.contains(b.getPieces().get(j));
					int x = labelW + j * (pw + gap);
					RoundRectangle2D pip = new RoundRectangle2D.Float(x, y + 4, pw, 7, 7, 7);
					if (got)
					{
						g.setPaint(new GradientPaint(x, 0, have == 4 ? Theme.GOLD : Theme.alpha(Theme.VIOLET, 200), x + pw, 0,
							have == 4 ? Theme.GOLD_BRIGHT : Theme.VIOLET));
					}
					else
					{
						g.setPaint(new GradientPaint(0, y + 4, Theme.INK_DEEP, 0, y + 11, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
					}
					g.fill(pip);
					g.setColor(got ? Theme.alpha(Color.WHITE, 30) : Theme.STROKE_SOFT);
					g.draw(pip);
				}
			}
			g.dispose();
		}
	}
}
