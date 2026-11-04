package com.codex.bosspilot.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * The Boss Pilot mark: two crossed swords behind a gilt heater shield, with a teal stone set in
 * the shield's heart. A planner's crest rather than a weapon's: the swords are sheathed behind it.
 */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;

		// The swords: two blades crossing behind the shield, gilt hilts showing at the top corners.
		g.setStroke(new BasicStroke(Math.max(1.6f, 1.9f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.INK_DEEP);
		g.draw(new Line2D.Float(x + 2.2f * s, y + 2.2f * s, x + 13.8f * s, y + 13.8f * s));
		g.draw(new Line2D.Float(x + 13.8f * s, y + 2.2f * s, x + 2.2f * s, y + 13.8f * s));
		g.setStroke(new BasicStroke(Math.max(0.9f, 1.0f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Color steel = Theme.mix(Theme.TEXT, Theme.GOLD_DIM, 0.35);
		g.setColor(steel);
		g.draw(new Line2D.Float(x + 2.4f * s, y + 2.4f * s, x + 13.6f * s, y + 13.6f * s));
		g.draw(new Line2D.Float(x + 13.6f * s, y + 2.4f * s, x + 2.4f * s, y + 13.6f * s));
		// Cross-guards near the top corners.
		g.setStroke(new BasicStroke(Math.max(1.0f, 1.2f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.GOLD);
		g.draw(new Line2D.Float(x + 2.0f * s, y + 5.0f * s, x + 5.0f * s, y + 2.0f * s));
		g.draw(new Line2D.Float(x + 11.0f * s, y + 2.0f * s, x + 14.0f * s, y + 5.0f * s));

		// The shield: a heater with a gilt rim, lit from the top left.
		Path2D shield = new Path2D.Float();
		float sx = x + 4.0f * s;
		float sw = 8.0f * s;
		float sy = y + 3.6f * s;
		shield.moveTo(sx, sy);
		shield.lineTo(sx + sw, sy);
		shield.lineTo(sx + sw, sy + 4.4f * s);
		shield.quadTo(sx + sw, sy + 8.6f * s, sx + sw / 2, sy + 10.4f * s);
		shield.quadTo(sx, sy + 8.6f * s, sx, sy + 4.4f * s);
		shield.closePath();
		g.setPaint(new GradientPaint(sx, sy, Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.4), sx + sw, sy + 10 * s, Theme.INK_DEEP));
		g.fill(shield);
		g.setStroke(new BasicStroke(Math.max(1.0f, 1.1f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setPaint(new GradientPaint(sx, sy, Theme.GOLD_BRIGHT, sx, sy + 10.4f * s, Theme.GOLD_DIM));
		g.draw(shield);

		// The stone: teal, lit from above.
		float cx = sx + sw / 2;
		float cy = sy + 4.6f * s;
		float r = Math.max(1.0f, 1.5f * s);
		g.setPaint(new RadialGradientPaint(cx - r * 0.3f, cy - r * 0.4f, r * 1.6f, new float[]{0, 1},
			new Color[]{Theme.mix(Theme.TEAL, Color.WHITE, 0.45), Theme.mix(Theme.TEAL, Theme.INK_DEEP, 0.35)}));
		g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		g.setStroke(new BasicStroke(Math.max(0.4f, 0.35f * s)));
		g.setColor(Theme.INK_DEEP);
		g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
	}
}
