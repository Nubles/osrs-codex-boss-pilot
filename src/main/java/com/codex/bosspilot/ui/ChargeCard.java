package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.ChargeBook;
import com.codex.bosspilot.core.ChargeCheck;
import com.codex.bosspilot.core.Charged;
import com.codex.bosspilot.core.Format;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * One charged item on the Prepare or Trip tab: how many kills it lasts, a meter for each thing it
 * holds, what to do about it at the bank, and how sure that is.
 */
final class ChargeCard
{
	private ChargeCard()
	{
	}

	static Color color(ChargeCheck.Status s)
	{
		switch (s)
		{
			case ENOUGH:
				return Theme.GREEN;
			case SHORT:
				return Theme.EMBER;
			case EMPTY:
				return Theme.BLOOD;
			case NO_PACE:
				return Theme.GOLD;
			default:
				return Theme.TEAL;
		}
	}

	static JComponent of(ChargeCheck.Line l, int width, boolean onTrip)
	{
		Color c = color(l.getStatus());
		Card card = new Card(new BorderLayout(0, 4)).accent(c).highlighted(l.getStatus().needsRecharge());
		StackPanel body = new StackPanel(3, 0);

		JPanel head = new JPanel(new BorderLayout(6, 0));
		head.setOpaque(false);
		head.add(Ui.label(l.getItem().getName(), Theme.body(), Theme.GOLD_BRIGHT), BorderLayout.CENTER);
		JLabel kills = Ui.label(l.killsText(), Theme.body(), c);
		kills.setHorizontalAlignment(SwingConstants.RIGHT);
		head.add(kills, BorderLayout.EAST);
		body.add(head);

		ChargeBook.Estimate e = l.getEstimate();
		if (e != null)
		{
			body.add(new Meters(l));
		}
		String advice = advice(l, onTrip);
		if (advice != null)
		{
			body.add(Ui.wrapped(advice, Theme.small(), l.getStatus().needsRecharge() ? c : Theme.TEXT, width));
		}
		body.add(Ui.wrapped(l.sureText() + ".", Theme.small(), Theme.TEXT_FAINT, width));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** What to do about it, in a sentence. */
	static String advice(ChargeCheck.Line l, boolean onTrip)
	{
		String target = l.getTarget() > 0 ? Format.plural((long) Math.floor(l.getTarget()), "kill", "kills") : null;
		String pool = l.limitName();
		switch (l.getStatus())
		{
			case EMPTY:
			{
				boolean dry = l.getEstimate() == null || l.getLimit() < 0 || l.getEstimate().getLeft()[l.getLimit()] <= 0;
				String what = dry ? (pool == null ? "Empty" : "Out of " + pool) : "Not enough " + pool + " for another kill";
				return what + (onTrip ? ". Bank after this kill." : ". " + l.getItem().getRecharge() + " before you go.");
			}
			case SHORT:
				return "The " + pool + " run out before your " + (onTrip ? "supplies" : "food and prayer") + (target == null ? "" : " (~" + target + ")")
					+ (onTrip ? "." : ". " + l.getItem().getRecharge() + " before you go.");
			case ENOUGH:
				return target == null ? "Enough for a trip at your pace." : "Lasts past your " + (onTrip ? "supplies" : "food and prayer") + " (~" + target + ").";
			case NO_PACE:
				return "Kills it lasts show once you have a kill here with it.";
			default:
				return null;
		}
	}

	/** A slim gauge for each thing the item holds, filled to what is left of its most. */
	static final class Meters extends JComponent
	{
		private static final int ROW = 16;
		private final ChargeCheck.Line line;

		Meters(ChargeCheck.Line line)
		{
			this.line = line;
			setOpaque(false);
			setPreferredSize(new Dimension(10, ROW * line.getItem().getPools().size()));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			List<Charged.Pool> pools = line.getItem().getPools();
			ChargeBook.Estimate e = line.getEstimate();
			double[] left = e.getLeft();
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			int labelW = 44;
			for (int i = 0; i < pools.size(); i++)
			{
				int y = i * ROW;
				Charged.Pool p = pools.get(i);
				Ui.shadowed(g, p.getName(), 0, y + 11, Theme.TEXT_MUTED);
				String value = left[i] < 0 ? "?" : (e.isExact() ? "" : "~") + Format.number(Math.round(left[i]));
				int vw = fm.stringWidth(value);
				Ui.shadowed(g, value, w - vw, y + 11, Theme.TEXT);
				int x = labelW;
				int bw = Math.max(10, w - labelW - vw - 8);
				RoundRectangle2D track = new RoundRectangle2D.Float(x, y + 5, bw, 6, 6, 6);
				g.setPaint(new GradientPaint(0, y + 5, Theme.INK_DEEP, 0, y + 11, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
				g.fill(track);
				g.setColor(Theme.STROKE_SOFT);
				g.draw(track);
				double share = left[i] < 0 ? 0 : Math.min(1, left[i] / p.getMax());
				boolean limit = i == line.getLimit();
				Color fill = limit ? ChargeCard.color(line.getStatus()) : Theme.GOLD_DIM;
				int fw = (int) Math.round(bw * share);
				if (fw > 0)
				{
					g.setPaint(new GradientPaint(x, 0, Theme.alpha(fill, 170), x + bw, 0, fill));
					g.fill(new RoundRectangle2D.Float(x, y + 5, Math.max(4, fw), 6, 6, 6));
					g.setColor(Theme.alpha(Color.WHITE, 40));
					g.drawLine(x + 2, y + 6, x + Math.max(4, fw) - 2, y + 6);
				}
			}
			g.dispose();
		}
	}
}
