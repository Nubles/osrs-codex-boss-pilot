package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.Debrief;
import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.Logbook;
import com.codex.bosspilot.core.Review;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The debrief of the last trip: a seal for why it ended, the trip next to your best one here,
 * a strip of the trips kept with this one lit, and the habit your recent trips show.
 */
final class DebriefCard
{
	private DebriefCard()
	{
	}

	static JComponent of(Review r, int width)
	{
		Logbook.TripRecord t = r.getTrip();
		Debrief d = r.getDebrief();
		Card card = new Card(new BorderLayout(0, 6)).accent(d == null ? Theme.GOLD : color(d.getEnding()));
		StackPanel body = new StackPanel(4, 0);
		if (d != null)
		{
			JPanel head = new JPanel(new BorderLayout(6, 0));
			head.setOpaque(false);
			head.add(new Seal(d.getEnding().getLabel().toUpperCase(Locale.ROOT), color(d.getEnding())), BorderLayout.WEST);
			if (d.isNewBest())
			{
				head.add(new Seal("NEW BEST", Theme.GOLD_BRIGHT), BorderLayout.EAST);
			}
			body.add(head);
			body.add(Ui.wrapped(d.getReason(), Theme.body(), Theme.TEXT, width));
		}
		body.add(Ui.wrapped(Format.plural(t.kills, "kill", "kills") + " in " + Format.clock(t.minutes() * 60)
			+ (t.loot > 0 ? ", " + Format.number(t.loot) + " loot" : ""), Theme.small(), Theme.TEXT_MUTED, width));
		boolean table = d != null && !d.getRows().isEmpty();
		if (t.kills > 0 && t.minutes() > 0 && !table)
		{
			body.add(Ui.wrapped(String.format(Locale.ROOT, "%.1f kills an hour", r.killsPerHour())
				+ (t.loot > 0 ? ", " + Format.number(r.lootPerHour()) + " loot an hour" : ""), Theme.small(), Theme.TEXT_MUTED, width));
		}
		if (table)
		{
			body.add(new Table(d.getRows()));
		}
		JComponent efficiency = EfficiencyCard.meter(t, width);
		if (efficiency != null)
		{
			body.add(efficiency);
		}
		if (d != null && d.getHistory().size() > 1)
		{
			body.add(new Strip(d.getHistory()));
			String rank = d.rankText();
			body.add(Ui.label(rank == null ? "" : rank, Theme.small(), d.getRank() == 1 ? Theme.GOLD_BRIGHT : Theme.GOLD));
		}
		if (r.damageText() != null)
		{
			body.add(Ui.wrapped(r.damageText(), Theme.small(), Theme.TEXT_MUTED, width));
		}
		if (d != null && d.getPattern() != null)
		{
			body.add(Ui.wrapped(d.getPattern(), Theme.small(), Theme.EMBER, width));
		}
		if (d == null || !d.isHabitOfThisTrip())
		{
			body.add(Ui.wrapped(r.getAdvice(), Theme.small(), Theme.TEAL, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	static Color color(Debrief.Ending e)
	{
		switch (e)
		{
			case DIED:
				return Theme.BLOOD;
			case OUT_OF_FOOD:
			case OUT_OF_PRAYER:
			case OUT_OF_CHARGES:
				return Theme.EMBER;
			case BAG_FULL:
				return Theme.GREEN;
			case LEFT_EARLY:
				return Theme.TEAL;
			case LEFT:
				return Theme.GOLD;
			default:
				return Theme.TEXT_MUTED;
		}
	}

	/** A small engraved seal with a word on it. */
	static final class Seal extends JComponent
	{
		private final String text;
		private final Color color;

		Seal(String text, Color color)
		{
			this.text = text;
			this.color = color;
			setOpaque(false);
			setFont(Theme.small());
			FontMetrics fm = getFontMetrics(getFont());
			setPreferredSize(new Dimension(fm.stringWidth(text) + 14, 17));
			setToolTipText(text);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getPreferredSize().width;
			int h = getHeight();
			RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h - 1, h - 1);
			g.setColor(Theme.alpha(color, 40));
			g.fill(shape);
			g.setColor(Theme.alpha(color, 170));
			g.draw(shape);
			g.setFont(getFont());
			FontMetrics fm = g.getFontMetrics();
			Ui.shadowed(g, text, 7, (h + fm.getAscent() - fm.getDescent()) / 2, color);
			g.dispose();
		}
	}

	/** This trip next to the best one, a line a figure, with an arrow for better or worse. */
	static final class Table extends JComponent
	{
		static final int ROW = 16;
		private final List<Debrief.Row> rows;

		Table(List<Debrief.Row> rows)
		{
			this.rows = rows;
			setOpaque(false);
			setPreferredSize(new Dimension(10, ROW * (rows.size() + 1) + 4));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			int bestX = w - 2;
			int nowX = w - 60;
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			int y = ROW - 4;
			right(g, "THIS TRIP", nowX, y, Theme.TEXT_FAINT);
			right(g, "BEST", bestX, y, Theme.TEXT_FAINT);
			g.setColor(Theme.STROKE_SOFT);
			g.drawLine(0, ROW, w, ROW);
			for (Debrief.Row row : rows)
			{
				y += ROW;
				Ui.shadowed(g, Ui.clip(row.getLabel(), fm, nowX - 60), 0, y, Theme.TEXT_MUTED);
				Color c = row.getVerdict() == Debrief.Verdict.BETTER ? Theme.GREEN
					: row.getVerdict() == Debrief.Verdict.WORSE ? Theme.EMBER : Theme.TEXT;
				right(g, row.getNow(), nowX, y, c);
				int ax = nowX - fm.stringWidth(row.getNow()) - 9;
				arrow(g, ax, y - 4, row, c);
				right(g, row.getBest(), bestX, y, Theme.GOLD);
			}
			g.dispose();
		}

		private static void right(Graphics2D g, String text, int x, int y, Color color)
		{
			Ui.shadowed(g, text, x - g.getFontMetrics().stringWidth(text), y, color);
		}

		/** Points the way the figure moved; its colour says whether that was for the better. */
		private static void arrow(Graphics2D g, int x, int y, Debrief.Row row, Color c)
		{
			if (row.getVerdict() == Debrief.Verdict.SAME)
			{
				return;
			}
			boolean up = row.isUp();
			Polygon p = up ? new Polygon(new int[]{x, x + 6, x + 3}, new int[]{y + 3, y + 3, y - 2}, 3)
				: new Polygon(new int[]{x, x + 6, x + 3}, new int[]{y - 2, y - 2, y + 3}, 3);
			g.setColor(c);
			g.fill(p);
		}
	}

	/** The trips kept, oldest first, as bars: this one lit, the best one crowned, forced endings marked below. */
	static final class Strip extends JComponent
	{
		static final int HEIGHT = 50;
		private final List<Debrief.Bar> bars;

		Strip(List<Debrief.Bar> bars)
		{
			this.bars = bars;
			setOpaque(false);
			setPreferredSize(new Dimension(10, HEIGHT));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			int base = HEIGHT - 10;
			int top = 8;
			double max = 0;
			for (Debrief.Bar b : bars)
			{
				max = Math.max(max, b.getValue());
			}
			g.setColor(Theme.STROKE_SOFT);
			g.drawLine(0, base, w, base);
			int n = bars.size();
			double slot = (double) w / n;
			int bw = (int) Math.max(4, Math.min(14, slot - 4));
			for (int i = 0; i < n; i++)
			{
				Debrief.Bar b = bars.get(i);
				int x = (int) Math.round(slot * i + (slot - bw) / 2);
				int h = max <= 0 ? 1 : (int) Math.max(2, Math.round((base - top) * b.getValue() / max));
				Color fill = b.isCurrent() ? Theme.GOLD_BRIGHT : b.isBest() ? Theme.GOLD : Theme.GOLD_DIM;
				g.setColor(Theme.alpha(fill, b.isCurrent() ? 255 : 200));
				g.fillRoundRect(x, base - h, bw, h, 3, 3);
				if (b.isBest())
				{
					int cx = x + bw / 2;
					int cy = base - h - 5;
					g.setColor(Theme.GOLD_BRIGHT);
					g.fill(new Polygon(new int[]{cx, cx + 3, cx, cx - 3}, new int[]{cy - 3, cy, cy + 3, cy}, 4));
				}
				if (b.getEnding() != null && b.getEnding().isForced())
				{
					g.setColor(color(b.getEnding()));
					g.fillOval(x + bw / 2 - 2, base + 3, 5, 5);
				}
			}
			g.dispose();
		}
	}
}
