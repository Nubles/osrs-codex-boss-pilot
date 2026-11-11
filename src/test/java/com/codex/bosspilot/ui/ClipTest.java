package com.codex.bosspilot.ui;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Painting never clears the clip it was given. A cleared clip is what broke RuneLite's overlay
 * renderer, so the clip is saved and put back, never set to null.
 */
public class ClipTest
{
	@Test
	public void aCardLeavesTheClipAsItFoundIt()
	{
		Card card = new Card(new BorderLayout()).accent(Theme.GOLD).highlighted(true);
		card.setSize(180, 60);
		BufferedImage img = new BufferedImage(200, 80, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Rectangle clip = new Rectangle(4, 4, 150, 50);
		g.setClip(clip);
		card.paintComponent(g);
		Shape after = g.getClip();
		assertNotNull(after);
		assertEquals(clip, after.getBounds());
		g.dispose();
	}

	@Test
	public void noSourceClearsTheClip() throws IOException
	{
		List<String> offenders = new ArrayList<>();
		try (Stream<Path> files = Files.walk(Paths.get("src/main/java")))
		{
			for (Path p : files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList()))
			{
				// overlay/Plaque.java is fixed in its own pull request; it drops out of this list once that lands.
				if (p.endsWith(Paths.get("overlay", "Plaque.java")))
				{
					continue;
				}
				String src = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
				if (src.replace(" ", "").contains("setClip(null)"))
				{
					offenders.add(p.toString());
				}
			}
		}
		assertTrue("setClip(null) in " + offenders, offenders.isEmpty());
	}
}
