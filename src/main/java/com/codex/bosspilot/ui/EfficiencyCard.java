package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.Efficiency;
import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.Logbook;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Kill efficiency, drawn: a meter for one trip in the debrief, and the learning curve of a boss on
 * the Prepare tab. Shown only between trips.
 */
final class EfficiencyCard
{
	private EfficiencyCard()
	{
	}

	static Color color(double eff)
	{
		return eff >= 0.9 ? Theme.GREEN : eff >= 0.75 ? Theme.GOLD : Theme.EMBER;
	}

	/** The debrief line: the figure on a meter, and what it compared. Null when the trip has none. */
	static JComponent meter(Logbook.TripRecord t, int width)
	{
		double eff = Efficiency.of(t);
		if (eff <= 0)
		{
			return null;
		}
		StackPanel body = new StackPanel(2, 0);
		body.add(new Meter("KILL EFFICIENCY", eff));
		String text = Efficiency.tripText(t);
		if (text != null)
		{
			body.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, width));
		}
		return body;
	}

	/** A boss's curve on the Prepare tab, with what it says and what it rests on. */
	static JComponent curve(Efficiency.Read read, boolean byKc, int width)
	{
		Color tint = read.getTrend() == Efficiency.Trend.FALLING ? Theme.EMBER
			: read.getTrend() == Efficiency.Trend.FLAT ? Theme.TEAL : Theme.GOLD;
		Card card = new Card(new BorderLayout(0, 4)).accent(tint);
		StackPanel body = new StackPanel(4, 0);
		body.add(new Meter("LATELY", read.getRecent()));
		if (read.getPoints().size() > 1)
		{
			body.add(new Curve(read, byKc));
		}
		body.add(Ui.wrapped(read.text(), Theme.small(), read.getTrend() == Efficiency.Trend.FALLING ? Theme.EMBER : Theme.TEXT, width));
		body.add(Ui.wrapped(Efficiency.basis(), Theme.small(), Theme.TEXT_FAINT, width));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** A label, the figure, and a bar with a mark at your gear's pace. */
	static final class Meter extends JPanel
	{
		private final String label;
		private final double eff;

		Meter(String label, double eff)
		{
			this.label = label;
			this.eff = eff;
			setOpaque(false);
			setPreferredSize(new Dimension(10, 24));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			Ui.shadowed(g, label, 0, fm.getAscent(), Theme.TEXT_FAINT);
			String value = Efficiency.percent(eff);
			Color c = color(eff);
			Ui.shadowed(g, value, w - fm.stringWidth(value), fm.getAscent(), c);
			int y = fm.getAscent() + 5;
			int h = 5;
			// The bar runs to 120%, so a lucky trip past your gear's pace still shows.
			double full = 1.2;
			g.setColor(Theme.INK_DEEP);
			g.fillRoundRect(0, y, w, h, h, h);
			int fill = (int) Math.round(w * Math.min(full, eff) / full);
			g.setColor(Theme.alpha(c, 220));
			g.fillRoundRect(0, y, Math.max(h, fill), h, h, h);
			int mark = (int) Math.round(w / full);
			g.setColor(Theme.GOLD_BRIGHT);
			g.fillRect(mark - 1, y - 2, 2, h + 4);
			g.dispose();
		}
	}

	/** The learning curve: efficiency trip by trip along the kill count, the trend window shaded. */
	static final class Curve extends JComponent
	{
		static final int HEIGHT = 74;
		private final Efficiency.Read read;
		private final boolean byKc;

		Curve(Efficiency.Read read, boolean byKc)
		{
			this.read = read;
			this.byKc = byKc;
			setOpaque(false);
			setPreferredSize(new Dimension(10, HEIGHT));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			List<Logbook.Point> pts = read.getPoints();
			int w = getWidth();
			int left = 26;
			int top = 6;
			int bottom = HEIGHT - 14;
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			double lo = 1;
			double hi = 1;
			for (Logbook.Point p : pts)
			{
				lo = Math.min(lo, p.eff);
				hi = Math.max(hi, p.eff);
			}
			lo = Math.max(0, Math.floor(lo * 10 - 0.5) / 10);
			hi = Math.ceil(hi * 10 + 0.5) / 10;
			double minX = pts.get(0).at;
			double maxX = pts.get(pts.size() - 1).at;
			if (maxX <= minX)
			{
				maxX = minX + 1;
			}
			int plotW = w - left - 4;
			// The trend window, shaded.
			if (read.getTrend() != Efficiency.Trend.NONE)
			{
				int from = read.windowStart();
				int x0 = x(pts.get(Math.max(0, from)).at, minX, maxX, left, plotW);
				g.setColor(Theme.alpha(read.getTrend() == Efficiency.Trend.FLAT ? Theme.TEAL : Theme.GOLD, 26));
				g.fillRect(x0 - 3, top, left + plotW - x0 + 6, bottom - top);
			}
			// Your gear's pace, at 100%.
			int y100 = y(1, lo, hi, top, bottom);
			g.setColor(Theme.alpha(Theme.GOLD_BRIGHT, 120));
			g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{3, 3}, 0));
			g.drawLine(left, y100, left + plotW, y100);
			g.setStroke(new BasicStroke(1));
			Ui.shadowed(g, "100%", 0, y100 + fm.getAscent() / 2 - 1, Theme.GOLD_DIM);
			String lowLabel = Math.round(lo * 100) + "%";
			Ui.shadowed(g, lowLabel, 0, bottom, Theme.TEXT_FAINT);
			g.setColor(Theme.STROKE_SOFT);
			g.drawLine(left, bottom, left + plotW, bottom);
			// The line and its trips.
			Path2D line = new Path2D.Double();
			for (int i = 0; i < pts.size(); i++)
			{
				int px = x(pts.get(i).at, minX, maxX, left, plotW);
				int py = y(pts.get(i).eff, lo, hi, top, bottom);
				if (i == 0)
				{
					line.moveTo(px, py);
				}
				else
				{
					line.lineTo(px, py);
				}
			}
			g.setColor(Theme.alpha(Theme.GOLD, 200));
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(line);
			for (int i = 0; i < pts.size(); i++)
			{
				int px = x(pts.get(i).at, minX, maxX, left, plotW);
				int py = y(pts.get(i).eff, lo, hi, top, bottom);
				boolean last = i == pts.size() - 1;
				g.setColor(last ? Theme.GOLD_BRIGHT : color(pts.get(i).eff));
				int r = last ? 3 : 2;
				g.fillOval(px - r, py - r, r * 2 + 1, r * 2 + 1);
			}
			String from = (byKc ? "KC " : "kill ") + Format.number(pts.get(0).at);
			String to = Format.number(pts.get(pts.size() - 1).at);
			Ui.shadowed(g, from, left, HEIGHT - 2, Theme.TEXT_FAINT);
			Ui.shadowed(g, to, left + plotW - fm.stringWidth(to), HEIGHT - 2, Theme.TEXT_FAINT);
			g.dispose();
		}

		private static int x(double at, double minX, double maxX, int left, int plotW)
		{
			return left + (int) Math.round(plotW * (at - minX) / (maxX - minX));
		}

		private static int y(double eff, double lo, double hi, int top, int bottom)
		{
			return bottom - (int) Math.round((bottom - top) * (eff - lo) / (hi - lo));
		}
	}
}
