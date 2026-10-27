package com.codex.bosspilot.overlay;

import com.codex.bosspilot.ui.Sigil;
import com.codex.bosspilot.ui.Theme;
import com.codex.bosspilot.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The plaque in the game view, in the Codex style: a small heading, the boss in gilt letters and
 * a few lines under it, with a stripe down the left edge in the colour that matters most. It
 * shows the trip at a boss and the kit at the bank; it never draws on the boss or the ground.
 */
public class Plaque extends Overlay
{
	private static final int PAD = 8;
	private static final int MIN_WIDTH = 190;
	private static final int MAX_WIDTH = 260;

	/** One line under the title. */
	public static final class Line
	{
		final String text;
		final Color color;
		/** Text set to the right of the line, such as a figure. */
		final String right;

		public Line(String text, Color color)
		{
			this(text, color, null);
		}

		public Line(String text, Color color, String right)
		{
			this.text = text;
			this.color = color;
			this.right = right;
		}
	}

	/** What to draw. */
	public static final class Content
	{
		final String over;
		final String title;
		final Color tint;
		final List<Line> lines;

		public Content(String over, String title, Color tint, List<Line> lines)
		{
			this.over = over;
			this.title = title;
			this.tint = tint;
			this.lines = lines == null ? Collections.emptyList() : lines;
		}
	}

	private volatile Content shown;

	public Plaque(OverlayPosition position)
	{
		setPosition(position);
		setPriority(PRIORITY_HIGH);
	}

	/** The content to show; null hides the plaque. */
	public void show(Content content)
	{
		shown = content;
	}

	public Content getShown()
	{
		return shown;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Content c = shown;
		if (c == null)
		{
			return null;
		}
		return paint(g, c);
	}

	/** Draws the plaque at the origin and returns its size. */
	public static Dimension paint(Graphics2D g, Content c)
	{
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();

		int icon = 18;
		int textX = PAD + 4 + icon + 8;
		int want = textX + Math.max(bold.stringWidth(c.title), small.stringWidth(c.over)) + PAD;
		for (Line l : c.lines)
		{
			int w = small.stringWidth(l.text) + (l.right == null ? 0 : small.stringWidth(l.right) + 10);
			want = Math.max(want, textX + w + PAD);
		}
		int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, want));
		int room = width - textX - PAD;

		List<String[]> wrapped = new ArrayList<>();
		int lineCount = 0;
		for (Line l : c.lines)
		{
			int r = l.right == null ? room : room - small.stringWidth(l.right) - 8;
			String[] parts = wrap(l.text, small, r, 2);
			wrapped.add(parts);
			lineCount += parts.length;
		}
		int height = PAD + small.getHeight() + bold.getHeight() + lineCount * small.getHeight() + 8;

		Shape shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 238), 0, height, Theme.alpha(Theme.INK_DEEP, 238)));
		g.fill(shape);
		g.setClip(shape);
		g.setPaint(new GradientPaint(0, 0, c.tint, 0, height, Theme.alpha(c.tint, 80)));
		g.fillRect(0, 0, 3, height);
		g.setClip(null);
		g.setColor(Theme.alpha(Theme.GOLD, 150));
		g.draw(shape);
		g.setColor(Theme.alpha(Color.WHITE, 14));
		g.drawLine(6, 1, width - 6, 1);

		Sigil.paint(g, PAD + 4, PAD + 1, icon);

		int y = PAD + small.getAscent();
		g.setFont(Theme.small());
		Ui.shadowed(g, Ui.clip(c.over, small, room), textX, y, Theme.TEXT_MUTED);
		g.setFont(Theme.title());
		y += bold.getHeight();
		Ui.shadowed(g, Ui.clip(c.title, bold, room), textX, y, Theme.GOLD_BRIGHT);
		g.setFont(Theme.small());
		for (int i = 0; i < c.lines.size(); i++)
		{
			Line l = c.lines.get(i);
			String[] parts = wrapped.get(i);
			for (int j = 0; j < parts.length; j++)
			{
				y += small.getHeight();
				Ui.shadowed(g, parts[j], textX, y, l.color);
				if (j == 0 && l.right != null)
				{
					int rw = small.stringWidth(l.right);
					Ui.shadowed(g, l.right, width - PAD - rw, y, Theme.GOLD_DIM);
				}
			}
		}
		return new Dimension(width, height);
	}

	static String[] wrap(String text, FontMetrics fm, int width, int maxLines)
	{
		if (text == null || text.isEmpty())
		{
			return new String[0];
		}
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			String trial = line.length() == 0 ? word : line + " " + word;
			if (fm.stringWidth(trial) > width && line.length() > 0)
			{
				out.add(line.toString());
				line = new StringBuilder(word);
				if (out.size() == maxLines)
				{
					out.set(maxLines - 1, Ui.clip(out.get(maxLines - 1) + " ...", fm, width));
					return out.toArray(new String[0]);
				}
			}
			else
			{
				line = new StringBuilder(trial);
			}
		}
		if (line.length() > 0)
		{
			out.add(Ui.clip(line.toString(), fm, width));
		}
		return out.toArray(new String[0]);
	}
}
