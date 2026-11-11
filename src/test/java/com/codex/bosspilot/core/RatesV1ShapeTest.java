package com.codex.bosspilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.runelite.http.api.RuneLiteAPI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Pins the shape of {@code codexbosspilot/ratesV1} (contract N10), as the client's own Gson writes
 * it. Readers (Gear Roadmap, Money Pilot, Autopilot, Annals) keep a byte copy of
 * {@code ratesV1.json} in their tests: a change here that breaks them must get a new key.
 */
public class RatesV1ShapeTest
{
	static final long T0 = 1_790_000_000_000L;
	private static final Gson GSON = RuneLiteAPI.GSON;

	/** The logbook the fixture is written from: two Vorkath trips and a Sarachnis trip. */
	static Logbook logbook()
	{
		Logbook book = new Logbook();
		// Vorkath: 6 kills in 10 minutes, every kill timed; then 4 kills in 8 minutes with a death and no prices.
		Logbook.TripRecord a = trip(T0, 10, 6, 1_800_000, 0, 240, 9);
		a.timed = 6;
		a.fightSeconds = 450;
		a.supplies = 150_000;
		a.theory = 60;
		book.add(Bosses.VORKATH, a);
		Logbook.TripRecord b = trip(T0 + 3_600_000, 8, 4, 1_200_000, 1, 200, 6);
		book.add(Bosses.VORKATH, b);
		book.addTimes(Bosses.VORKATH, Arrays.asList(70.0, 75.0, 80.0, 72.5, 76.0, 76.5));
		book.noteBest(Bosses.VORKATH, 68.4);
		book.noteKc(Bosses.VORKATH, 212);
		// Sarachnis: 5 kills in 12 minutes, untimed and unpriced.
		book.add(Bosses.SARACHNIS, trip(T0, 12, 5, 300_000, 0, 0, 10));
		// A boss seen but never killed is left out.
		book.noteKc(Bosses.GIANT_MOLE, 3);
		return book;
	}

	static Logbook.TripRecord trip(long start, int minutes, int kills, long loot, int deaths, long food, long doses)
	{
		Logbook.TripRecord r = new Logbook.TripRecord();
		r.start = start;
		r.end = start + minutes * 60_000L;
		r.kills = kills;
		r.loot = loot;
		r.deaths = deaths;
		r.foodUsed = food;
		r.dosesUsed = doses;
		return r;
	}

	@Test
	public void theSharedRatesKeepTheirShape() throws Exception
	{
		String json = RatesV1.of(logbook(), T0 + 7_200_000).toJson(GSON);
		assertEquals(fixture(), json);
	}

	@Test
	public void theFieldsAreTheContractsAndNoMore()
	{
		JsonObject root = new JsonParser().parse(RatesV1.of(logbook(), T0).toJson(GSON)).getAsJsonObject();
		assertEquals("[version, savedAt, bosses]", root.keySet().toString());
		assertEquals(1, root.get("version").getAsInt());
		JsonObject vorkath = root.getAsJsonObject("bosses").getAsJsonObject("Vorkath");
		assertEquals("[trips, kills, killsPerHour, killSeconds, lootPerHour, suppliesPerHour, foodPerKill, dosesPerKill, deathsPerHour,"
			+ " overheadShare]", vorkath.keySet().toString());
		assertEquals("[best, median]", vorkath.getAsJsonObject("killSeconds").keySet().toString());
		assertFalse(root.getAsJsonObject("bosses").has("Giant Mole"));
	}

	@Test
	public void theFiguresAreTheLogbooks()
	{
		RatesV1 r = RatesV1.of(logbook(), T0);
		RatesV1.BossRates v = r.bosses.get("Vorkath");
		assertEquals(2, v.trips);
		assertEquals(10, v.kills);
		// 10 kills in 18 minutes.
		assertEquals(33.3, v.killsPerHour, 1e-9);
		assertEquals(10_000_000, v.lootPerHour);
		// Priced only on the first trip: 150k in 10 minutes.
		assertEquals(900_000, v.suppliesPerHour);
		assertEquals(44.0, v.foodPerKill, 1e-9);
		assertEquals(1.5, v.dosesPerKill, 1e-9);
		// One death in 18 minutes.
		assertEquals(3.33, v.deathsPerHour, 1e-9);
		// 450 s of fighting in the first trip's 600 s; the second trip's kills were not timed.
		assertEquals(0.25, v.overheadShare, 1e-9);
		assertEquals(68.4, v.killSeconds.best, 1e-9);
		assertEquals(75.5, v.killSeconds.median, 1e-9);

		RatesV1.BossRates s = r.bosses.get("Sarachnis");
		assertEquals(-1, s.killSeconds.best, 0);
		assertEquals(-1, s.killSeconds.median, 0);
		assertEquals(-1, s.suppliesPerHour);
		assertEquals(-1, s.overheadShare, 0);
		assertEquals(0, s.foodPerKill, 0);
		// Most kills first.
		assertEquals(Arrays.asList("Vorkath", "Sarachnis"), Arrays.asList(r.bosses.keySet().toArray()));
	}

	@Test
	public void anEmptyLogbookSharesNoBosses()
	{
		assertEquals("{\"version\":1,\"savedAt\":5,\"bosses\":{}}", RatesV1.of(new Logbook(), 5).toJson(GSON));
		assertEquals("{\"version\":1,\"savedAt\":5,\"bosses\":{}}", RatesV1.of(null, 5).toJson(GSON));
	}

	@Test
	public void theSignatureIgnoresTheTime()
	{
		assertEquals(RatesV1.of(logbook(), 1).signature(GSON), RatesV1.of(logbook(), 2).signature(GSON));
		RatesV1 r = RatesV1.of(logbook(), 7);
		r.signature(GSON);
		assertEquals(7, r.savedAt);
	}

	@Test
	public void manyBossesAreCappedAndSmall()
	{
		Logbook book = new Logbook();
		for (Boss b : Bosses.ALL)
		{
			for (int i = 0; i < Logbook.TRIPS; i++)
			{
				book.add(b, trip(T0 + i * 3_600_000L, 30, 20, 5_000_000, 1, 900, 40));
			}
		}
		String json = RatesV1.of(book, T0).toJson(GSON);
		assertTrue(json.length() < 16_000);
		assertTrue(RatesV1.of(book, T0).bosses.size() <= RatesV1.MAX_BOSSES);
	}

	@Test
	public void tripsOnSpecialWorldsAreNotLearned()
	{
		assertTrue(WorldMode.learns(java.util.EnumSet.noneOf(net.runelite.api.WorldType.class)));
		assertTrue(WorldMode.learns(java.util.EnumSet.of(net.runelite.api.WorldType.MEMBERS)));
		assertTrue(WorldMode.learns(null));
		for (net.runelite.api.WorldType t : new net.runelite.api.WorldType[]{net.runelite.api.WorldType.LAST_MAN_STANDING,
			net.runelite.api.WorldType.PVP_ARENA, net.runelite.api.WorldType.BETA_WORLD, net.runelite.api.WorldType.TOURNAMENT_WORLD,
			net.runelite.api.WorldType.FRESH_START_WORLD, net.runelite.api.WorldType.SEASONAL, net.runelite.api.WorldType.DEADMAN})
		{
			assertFalse(t.name(), WorldMode.learns(java.util.EnumSet.of(net.runelite.api.WorldType.MEMBERS, t)));
		}
	}

	static String fixture() throws Exception
	{
		try (InputStream in = RatesV1ShapeTest.class.getResourceAsStream("ratesV1.json"))
		{
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
		}
	}
}
