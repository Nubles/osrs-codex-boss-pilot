package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.WayIn;
import com.codex.bosspilot.core.Zalcano;
import com.codex.bosspilot.core.ZalcanoLog;
import com.codex.bosspilot.core.ZalcanoRun;
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
 * Zalcano on the Prepare and Trip tabs: the player's own kills (kills and loot an hour at their
 * pace, the last kill), the four rare drops with each dry spell against its rate, and the ways into
 * Prifddinas from Codex Wayfinder. Nothing here is about the fight itself.
 */
final class ZalcanoCard
{
	/** The dry meter runs to this many times a drop's rate. */
	static final double METER_SPAN = 3;
	/** Teleports into Prifddinas listed at most. */
	static final int WAYS = 3;

	private ZalcanoCard()
	{
	}

	/** The player's own kills. */
	static JComponent kills(ZalcanoRun r, int width)
	{
		ZalcanoLog log = r.getLog();
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.GOLD_DIM);
		StackPanel body = new StackPanel(5, 0);
		if (log.isEmpty())
		{
			body.add(Ui.wrapped("Your kills show here from your next one: what each brought, kills and loot an hour at your own pace,"
				+ " and how long since each rare drop. Kept on this computer only.", Theme.small(), Theme.TEXT_MUTED, width));
		}
		else
		{
			JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
			grid.setOpaque(false);
			double kph = log.killsPerHour();
			double lph = log.lootPerHour();
			grid.add(new StatTile("Kills", Format.number(log.kills), true));
			grid.add(new StatTile("Kills/hr", kph < 0 ? "-" : String.format(Locale.ROOT, "%.1f", kph), kph > 0));
			grid.add(new StatTile("Per kill", log.average() < 0 ? "-" : Format.number(log.average()), log.average() > 0));
			grid.add(new StatTile("Loot/hr", lph < 0 ? "-" : Format.number(Math.round(lph)), lph > 0));
			body.add(grid);
			ZalcanoLog.Kill last = log.last();
			if (last != null)
			{
				body.add(Ui.wrapped(lastText(last), Theme.small(), last.uniques.isEmpty() ? Theme.TEXT_MUTED : Theme.GREEN, width));
			}
			if (log.timed < log.kills)
			{
				body.add(Ui.wrapped("Your pace counts the " + Format.plural(log.timed, "kill", "kills")
					+ " timed back to back on a trip, not the walk in or breaks.", Theme.small(), Theme.TEXT_FAINT, width));
			}
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	static String lastText(ZalcanoLog.Kill k)
	{
		StringBuilder b = new StringBuilder("Last kill");
		if (k.kc > 0)
		{
			b.append(" (KC ").append(Format.number(k.kc)).append(')');
		}
		b.append(": ");
		if (k.value < 0)
		{
			b.append("no loot seen");
		}
		else
		{
			b.append(Format.number(k.value));
			if (!k.items.isEmpty())
			{
				b.append(", ").append(String.join(", ", k.items.subList(0, Math.min(3, k.items.size()))));
				if (k.items.size() > 3)
				{
					b.append(" and more");
				}
			}
		}
		if (!k.uniques.isEmpty())
		{
			b.append(". ").append(String.join(", ", k.uniques)).append('!');
		}
		else
		{
			b.append('.');
		}
		return b.toString();
	}

	/** The four rare drops, each with its rate and the player's dry spell. */
	static JComponent uniques(ZalcanoRun r, int width)
	{
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.VIOLET);
		StackPanel body = new StackPanel(8, 0);
		for (Zalcano.Unique u : Zalcano.Unique.values())
		{
			StackPanel one = new StackPanel(2, 0);
			one.add(new DropRow(r, u));
			String dry = r.dryText(u);
			one.add(Ui.wrapped(dry == null ? u.rate() + " a kill, " + u.getHow() + "." : dry + ".", Theme.small(),
				r.spell(u) >= 2 ? Theme.EMBER : Theme.TEXT_MUTED, width));
			body.add(one);
		}
		body.add(Ui.wrapped("Rare drops need 31 combined damage. Owned means received while Boss Pilot watched, or seen in your bank or pack.",
			Theme.small(), Theme.TEXT_FAINT, width));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** The ways into Prifddinas this account owns, from Codex Wayfinder. */
	static JComponent travel(ZalcanoRun r, int width)
	{
		WayIn w = r.getWayIn();
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.TEAL);
		StackPanel body = new StackPanel(4, 0);
		if (!w.isRead())
		{
			body.add(Ui.wrapped("Codex Wayfinder lists the teleports into Prifddinas you own here. The quickest way is a teleport crystal or an"
				+ " eternal one; a spirit tree or a house in Prifddinas also work.", Theme.small(), Theme.TEXT_MUTED, width));
		}
		else if (w.getTeleports().isEmpty())
		{
			body.add(Ui.wrapped("Wayfinder knows no teleport into Prifddinas that you own. A teleport crystal is the quickest way there.",
				Theme.small(), Theme.EMBER, width));
		}
		else
		{
			List<WayIn.Teleport> ts = w.getTeleports();
			for (WayIn.Teleport t : ts.subList(0, Math.min(WAYS, ts.size())))
			{
				JPanel line = new JPanel(new BorderLayout(6, 0));
				line.setOpaque(false);
				line.add(Ui.wrapped(t.getName(), Theme.small(), t.isCarried() ? Theme.TEXT : Theme.TEXT_MUTED, width - 60), BorderLayout.CENTER);
				line.add(Ui.label(t.isCarried() ? "on you" : "in bank", Theme.small(), t.isCarried() ? Theme.GREEN : Theme.GOLD_DIM),
					BorderLayout.EAST);
				body.add(line);
			}
			body.add(Ui.wrapped("Then the teleport platform just west of the south-eastern bank.", Theme.small(), Theme.TEXT_FAINT, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** Colour of each drop's gem. */
	static Color tone(Zalcano.Unique u)
	{
		switch (u)
		{
			case TOOL_SEED:
				return Theme.TEAL;
			case SHARD:
				return Theme.GOLD;
			case PET:
				return Theme.EMBER;
			default:
				return Theme.VIOLET;
		}
	}

	/** A gem lit when owned, the drop's name and rate, and a meter of the dry spell against the rate. */
	static final class DropRow extends JComponent
	{
		private static final int H = 30;
		private final ZalcanoRun run;
		private final Zalcano.Unique unique;

		DropRow(ZalcanoRun run, Zalcano.Unique unique)
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
			g.setFont(Theme.body());
			FontMetrics fm = g.getFontMetrics();
			Ui.shadowed(g, unique.getName(), tx, 11, owned ? Theme.TEXT : Theme.TEXT_MUTED);
			g.setFont(Theme.small());
			FontMetrics sm = g.getFontMetrics();
			String right = owned ? "owned" : unique.shortRate().replace(" to ", "-");
			Ui.shadowed(g, right, w - sm.stringWidth(right), 11, owned ? Theme.GREEN : Theme.GOLD_DIM);

			// The dry meter: kills since the last one against the rate, ticked at once and twice the rate.
			int my = 18;
			int mh = 7;
			int mw = w - tx;
			g.setPaint(new GradientPaint(0, my, Theme.INK_DEEP, 0, my + mh, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
			g.fill(new RoundRectangle2D.Float(tx, my, mw, mh, mh, mh));
			g.setColor(Theme.STROKE_SOFT);
			g.draw(new RoundRectangle2D.Float(tx, my, mw - 1, mh, mh, mh));
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
}
