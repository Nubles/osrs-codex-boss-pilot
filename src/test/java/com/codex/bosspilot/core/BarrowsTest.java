package com.codex.bosspilot.core;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Barrows sums against the OSRS Wiki's Chest (Barrows) page: "R = S + 2N" with S capped at 1000,
 * a piece on a roll at 1/(450 - 58N) ("1 brother: 1/392 ... 6: 1/102"), one roll plus one a
 * brother to seven, "approximately 1/15.01" for a piece in a chest with all six.
 */
public class BarrowsTest
{
	private static final Set<Barrows.Brother> NONE = EnumSet.noneOf(Barrows.Brother.class);
	private static final Set<Barrows.Brother> ALL = EnumSet.allOf(Barrows.Brother.class);

	@Test
	public void brothersSumToTheWikisSixHundredFiftySix()
	{
		int sum = 0;
		for (Barrows.Brother b : Barrows.Brother.values())
		{
			sum += b.getLevel();
		}
		// Chest (Barrows): "all six have a combat sum of 656".
		assertEquals(656, sum);
		assertEquals(24, Barrows.Brother.values().length * Barrows.PIECES_PER_BROTHER);
	}

	@Test
	public void potentialIsCappedLevelsPlusTwoABrother()
	{
		assertEquals(668, Barrows.potential(656, 6));
		assertEquals(Barrows.MAX_POTENTIAL, Barrows.potential(1300, 6));
		assertEquals(100, Barrows.potential(98, 1));
		assertEquals(0, Barrows.potential(0, 0));
	}

	@Test
	public void perRollMatchesTheWikisTable()
	{
		int[] wiki = {392, 334, 276, 218, 160, 102};
		for (int n = 1; n <= 6; n++)
		{
			assertEquals(1.0 / wiki[n - 1], Barrows.perRoll(n), 1e-12);
		}
		assertEquals(0, Barrows.perRoll(0), 0);
		assertEquals(1, Barrows.rolls(0));
		assertEquals(7, Barrows.rolls(6));
	}

	@Test
	public void aChestWithAllSixIsAboutOneInFifteen()
	{
		assertEquals(15.01, 1 / Barrows.chanceAny(6), 0.01);
		assertEquals("1 in 15", Barrows.oneIn(Barrows.chanceAny(6)));
		// With every piece missing, a new piece is any piece.
		assertEquals(Barrows.chanceAny(6), Barrows.chanceNew(6, 24), 1e-12);
		assertEquals(0, Barrows.chanceNew(6, 0), 0);
		assertEquals(0, Barrows.chanceNew(0, 4), 0);
	}

	@Test
	public void oneMissingPieceIsAboutASeventhOfOneRollEach()
	{
		double one = Barrows.chanceNew(6, 1);
		// Seven rolls at 1/102, each a 1/24 share, less the small chance of two pieces in a chest.
		assertEquals(7.0 / 102 / 24, one, 0.0002);
		assertTrue(one < Barrows.chanceNew(6, 2));
		assertTrue(Barrows.chanceNew(6, 2) < Barrows.chanceNew(6, 12));
	}

	@Test
	public void piecesAreReadWithoutTheirWear()
	{
		assertEquals("Ahrim's hood", Barrows.piece("Ahrim's hood"));
		assertEquals("Ahrim's hood", Barrows.piece("Ahrim's hood 75"));
		assertEquals("Dharok's greataxe", Barrows.piece("Dharok's greataxe 0"));
		assertNull(Barrows.piece("Ahrim's hood (broken)"));
		assertNull(Barrows.piece("Blood rune"));
		assertEquals(Collections.singleton("Verac's flail"), Barrows.pieces(Arrays.asList("Verac's flail 100", "Coins")));
	}

	@Test
	public void bloodRunePlanCountsTheBrothersStillToKill()
	{
		// Nothing killed: the six brothers bring 668, and 212 more is three tunnel kills.
		Barrows.Plan p = Barrows.plan(0, NONE, Barrows.Aim.BLOOD_RUNES);
		assertEquals(668, p.getProjected());
		assertEquals(3, p.total());
		assertTrue(p.result() <= 880 && p.result() >= 877);
		assertFalse(p.reached());
		int sum = 0;
		for (java.util.Map.Entry<Barrows.Monster, Integer> e : p.getKills().entrySet())
		{
			sum += e.getKey().getLevel() * e.getValue();
		}
		assertEquals(p.getAdds(), sum);
	}

	@Test
	public void planStopsWhereTheRunAlreadyIs()
	{
		// All six and 300 levels of tunnel kills already: 956 + 12 is past blood runes.
		Barrows.Plan p = Barrows.plan(956, ALL, Barrows.Aim.BLOOD_RUNES);
		assertTrue(p.reached());
		assertEquals(0, p.total());
		// Off plans nothing.
		assertEquals(0, Barrows.plan(0, NONE, Barrows.Aim.OFF).total());
	}

	@Test
	public void fullPotentialMayOvershootTheCap()
	{
		// 344 levels to the cap: four kills reach at most 316, so five, and the potential is 1012.
		Barrows.Plan p = Barrows.plan(656, ALL, Barrows.Aim.FULL);
		assertEquals(5, p.total());
		assertEquals(Barrows.MAX_POTENTIAL, p.result());
	}

	@Test
	public void killedBrothersComeFromTheirVarbits()
	{
		assertEquals(EnumSet.of(Barrows.Brother.AHRIM, Barrows.Brother.VERAC), Barrows.killed(new int[]{1, 0, 0, 0, 0, 1}));
		assertEquals(Barrows.Brother.GUTHAN, Barrows.Brother.byObject(net.runelite.api.gameval.ObjectID.BARROWS_STAIRS_GUTHAN));
		assertEquals(Barrows.Brother.KARIL, Barrows.Brother.byObject(net.runelite.api.gameval.ObjectID.BARROW_KARIL_SARCOPHAGUS));
		assertEquals(net.runelite.api.gameval.VarbitID.BARROWS_KILLED_TORAG, Barrows.Brother.TORAG.getVarbit());
	}

	@Test
	public void chestLogKeepsLootAnHourAndTheDryStreak()
	{
		ChestLog log = new ChestLog();
		log.add(chest(200_000, 6, 600));
		log.add(chest(100_000, 6, 600));
		ChestLog.Chest lucky = chest(1_000_000, 6, 600);
		lucky.pieces.add("Karil's coif");
		log.add(lucky);
		log.add(chest(150_000, 5, -1));
		assertEquals(4, log.chests);
		assertEquals(3, log.timed);
		assertEquals(1_300_000 * 3600.0 / 1800, log.lootPerHour(), 0.001);
		assertEquals(1, log.dryStreak());
		assertEquals(1 - Barrows.chanceAny(5), log.dryOdds(), 1e-12);

		List<String> missing = log.missing(Arrays.asList("Ahrim's hood 50", "Dharok's helm"));
		assertEquals(21, missing.size());
		assertFalse(missing.contains("Karil's coif"));
		assertFalse(missing.contains("Ahrim's hood"));

		Gson gson = new Gson();
		ChestLog back = ChestLog.parse(gson, log.toJson(gson));
		assertEquals(log.lootPerHour(), back.lootPerHour(), 0.001);
		assertEquals(1, (int) back.found.get("Karil's coif"));
		assertTrue(ChestLog.parse(gson, "not json {").isEmpty());
	}

	@Test
	public void runTextsSayWhereTheRunStands()
	{
		ChestLog log = new ChestLog();
		log.add(chest(100_000, 6, 600));
		log.add(chest(100_000, 6, 600));
		BarrowsRun r = new BarrowsRun(EnumSet.of(Barrows.Brother.AHRIM, Barrows.Brother.KARIL), 196, Barrows.Brother.GUTHAN,
			Barrows.Aim.BLOOD_RUNES, log, log.missing(Collections.emptyList()));
		assertEquals("2 of 6 brothers: Ahrim, Karil", r.brothersText());
		assertEquals("Tunnel: Guthan's crypt, go there last", r.tunnelText());
		assertEquals(200, r.potential());
		assertTrue(r.planText(), r.planText().startsWith("For blood runes: "));
		assertTrue(r.dryText(), r.dryText().equals("2 chests since your last piece; a dry spell this long happens 87% of the time"));
		// Two brothers down, all their eight pieces missing.
		assertEquals(Barrows.chanceAny(2), r.nextNew(), 1e-12);
		assertEquals("Tunnel not found yet this run",
			new BarrowsRun(NONE, 0, null, Barrows.Aim.OFF, log, Collections.emptyList()).tunnelText());
	}

	private static ChestLog.Chest chest(long value, int brothers, long seconds)
	{
		ChestLog.Chest c = new ChestLog.Chest();
		c.value = value;
		c.brothers = brothers;
		c.seconds = seconds;
		return c;
	}
}
