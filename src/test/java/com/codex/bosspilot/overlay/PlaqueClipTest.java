package com.codex.bosspilot.overlay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * RuneLite's OverlayRenderer compares the clip after every overlay with
 * getClip().equals(...). An overlay that leaves the clip null makes that throw
 * for every overlay drawn after it, so the plaque must hand the clip back as it found it.
 */
public class PlaqueClipTest
{
	@Test
	public void leavesTheCallersClipAlone()
	{
		Plaque.Content content = new Plaque.Content("NEXT TRIP", "Zulrah", Color.ORANGE,
			Collections.singletonList(new Plaque.Line("Bring an antivenom", Color.WHITE)));

		BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Rectangle layer = new Rectangle(0, 0, 400, 300);
		g.setClip(layer);
		Plaque.paint(g, content);
		assertEquals(layer, g.getClip());
		g.dispose();
	}
}
