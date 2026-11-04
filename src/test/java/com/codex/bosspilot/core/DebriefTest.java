package com.codex.bosspilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class DebriefTest
{
	private static final long MIN = 60_000;

	/** A trip of so many kills over so many minutes, with loot, food and prayer used and an ending. */
	private static Logbook.TripRecord trip(int kills, double minutes, long loot, long food, long doses, Debrief.Ending ended)
	{
		Logbook.TripRecord t = new Logbook.TripRecord();
		t.start = 0;
		t.end = Math.round(minutes * MIN);
		t.kills = kills;
		t.loot = loot;
		t.foodUsed = food;
		t.dosesUsed = doses;
		t.ended = ended == null ? null : ended.name();
		return t;
	}

	/** A Sarachnis trip through the tracker: three kills a minute apart, then what happens next. */
	private static TripTracker threeKills(long food, long doses, int free)
	{
		TripTracker t = new TripTracker();
		long now = 0;
		t.observe(Bosses.SARACHNIS, 400, 16, 20, now);
		for (int k = 1; k <= 3; k++)
		{
			now += MIN;
			t.kill(Bosses.SARACHNIS, now);
			t.observe(Bosses.SARACHNIS, 400 - (400 - food) * k / 3, 16 - (16 - doses) * k / 3, free, now);
		}
		return t;
	}

	private static Logbook.TripRecord leave(TripTracker t, long when)
	{
		// Out of sight long enough that the trip ends by itself.
		t.observe(null, 400, 16, 28, when);
		t.observe(null, 400, 16, 28, when + TripTracker.END_AFTER_MS + 1);
		return t.takeFinished().record();
	}

	@Test
	public void foodRunningOutEndsTheTrip()
	{
		// 360 hp of food over three kills is 120 a kill: 40 left would not last another.
		Logbook.TripRecord r = leave(threeKills(40, 13, 10), 4 * MIN);
		assertEquals("OUT_OF_FOOD", r.ended);
		assertEquals(1 / 3.0, r.killsLeft, 1e-9);
		// Restocking at the bank after leaving is not counted as food eaten on the trip.
		assertEquals(360, r.foodUsed);
		assertEquals(3, r.dosesUsed);
	}

	@Test
	public void prayerRunningOutFirstSaysPrayer()
	{
		// 15 doses over three kills, 1 left: prayer runs out before the 280 hp of food. Two more kills
		// are 10 doses: three four-dose potions.
		Logbook.TripRecord r = leave(threeKills(280, 1, 10), 4 * MIN);
		assertEquals("OUT_OF_PRAYER", r.ended);
		Review review = Review.of(Bosses.SARACHNIS, r, Collections.emptyList());
		assertTrue(review.getAdvice(), review.getAdvice().contains("3 more prayer potions"));
		assertEquals("Out of prayer after 3 kills: what was left would not last another.", review.getDebrief().getReason());
	}

	@Test
	public void aFullBagWithSuppliesLeftSaysSo()
	{
		Logbook.TripRecord r = leave(threeKills(250, 13, 0), 4 * MIN);
		assertEquals("BAG_FULL", r.ended);
		assertEquals("Your bag was full when you left, with supplies for about 5 more kills.",
			Debrief.of(r, Collections.emptyList()).getReason());
		assertEquals("Your bag filled up with supplies for about 5 more kills still in it. Bring less, and keep the room for loot.",
			Review.of(Bosses.SARACHNIS, r, Collections.emptyList()).getAdvice());
	}

	@Test
	public void leavingWithPlentyIsLeavingEarly()
	{
		Logbook.TripRecord r = leave(threeKills(250, 13, 10), 4 * MIN);
		assertEquals("LEFT_EARLY", r.ended);
		assertEquals(5, r.killsLeft, 1e-9);
	}

	@Test
	public void aDeathAfterTheLastKillEndsTheTripButOneBeforeDoesNot()
	{
		TripTracker t = threeKills(250, 13, 10);
		t.death();
		assertEquals("DIED", leave(t, 4 * MIN).ended);

		TripTracker early = new TripTracker();
		early.observe(Bosses.SARACHNIS, 400, 16, 20, 0);
		early.death();
		early.kill(Bosses.SARACHNIS, MIN);
		early.kill(Bosses.SARACHNIS, 2 * MIN);
		early.observe(Bosses.SARACHNIS, 200, 8, 20, 2 * MIN);
		Logbook.TripRecord r = leave(early, 3 * MIN);
		assertEquals(1, r.deaths);
		assertEquals("LEFT", r.ended);
	}

	@Test
	public void logoutHopAndAnotherBossAreNamed()
	{
		TripTracker out = threeKills(250, 13, 10);
		out.end();
		assertEquals("LOGGED_OUT", out.takeFinished().record().ended);
		TripTracker hop = threeKills(250, 13, 10);
		hop.end(Debrief.Cause.HOPPED);
		assertEquals("HOPPED", hop.takeFinished().record().ended);
		TripTracker moved = threeKills(250, 13, 10);
		moved.observe(Bosses.GIANT_MOLE, 250, 13, 10, 5 * MIN);
		assertEquals("MOVED_ON", moved.takeFinished().record().ended);
		// Running out still comes first: logging out with nothing left is running out.
		TripTracker dry = threeKills(40, 13, 10);
		dry.end();
		assertEquals("OUT_OF_FOOD", dry.takeFinished().record().ended);
	}

	@Test
	public void theTripIsSetAgainstTheBestKeptOne()
	{
		List<Logbook.TripRecord> earlier = new ArrayList<>();
		earlier.add(trip(10, 60, 600_000, 800, 20, Debrief.Ending.LEFT));
		earlier.add(trip(12, 60, 900_000, 600, 24, Debrief.Ending.LEFT)); // the best: 900k an hour
		earlier.add(trip(2, 0.5, 900_000, 0, 0, Debrief.Ending.LEFT)); // too short to rate
		Logbook.TripRecord now = trip(14, 60, 1_200_000, 700, 21, Debrief.Ending.BAG_FULL);
		Debrief d = Debrief.of(now, earlier);
		assertTrue(d.isByLoot());
		assertTrue(d.isNewBest());
		assertEquals(1, d.getRank());
		assertEquals(3, d.getOf());
		assertEquals("Your best of 3 trips by loot an hour", d.rankText());
		Debrief.Row loot = d.getRows().get(1);
		assertEquals("Loot an hour", loot.getLabel());
		assertEquals("1.2M", loot.getNow());
		assertEquals("900k", loot.getBest());
		assertEquals(Debrief.Verdict.BETTER, loot.getVerdict());
		Debrief.Row food = d.getRows().get(2);
		assertEquals("Food a kill", food.getLabel());
		assertEquals("50 hp", food.getNow());
		assertEquals(Debrief.Verdict.SAME, food.getVerdict());
		assertEquals(Debrief.Verdict.WORSE, Debrief.verdict(60, 50, false));
		// The strip: the two rated trips, then this one, lit and crowned.
		assertEquals(3, d.getHistory().size());
		assertTrue(d.getHistory().get(2).isCurrent());
		assertTrue(d.getHistory().get(2).isBest());
		assertFalse(d.getHistory().get(1).isBest());

		Debrief slower = Debrief.of(trip(9, 60, 700_000, 900, 20, Debrief.Ending.LEFT), earlier);
		assertEquals("2nd best of 3 trips by loot an hour", slower.rankText());
		assertTrue(slower.getHistory().get(1).isBest());
		assertEquals(Debrief.Verdict.WORSE, slower.getRows().get(0).getVerdict());
	}

	@Test
	public void withoutLootTripsAreRatedByKills()
	{
		Debrief d = Debrief.of(trip(6, 30, 0, 300, 0, Debrief.Ending.LEFT),
			Collections.singletonList(trip(10, 60, 0, 500, 0, Debrief.Ending.LEFT)));
		assertFalse(d.isByLoot());
		assertEquals("Your best of 2 trips by kills an hour", d.rankText());
		for (Debrief.Row r : d.getRows())
		{
			assertFalse(r.getLabel().contains("Loot") || r.getLabel().contains("Prayer"));
		}
		assertEquals("Your first timed trip here", Debrief.of(trip(6, 30, 0, 300, 0, null), Collections.emptyList()).rankText());
		assertTrue(Debrief.of(trip(6, 30, 0, 300, 0, null), Collections.emptyList()).getRows().isEmpty());
	}

	@Test
	public void aHabitOfRunningOutNamesWhatToBring()
	{
		List<Logbook.TripRecord> earlier = new ArrayList<>();
		earlier.add(trip(8, 40, 0, 400, 32, Debrief.Ending.OUT_OF_PRAYER));
		earlier.add(trip(8, 40, 0, 400, 32, Debrief.Ending.LEFT));
		earlier.add(trip(8, 40, 0, 400, 32, Debrief.Ending.OUT_OF_PRAYER));
		Debrief d = Debrief.of(trip(8, 40, 0, 400, 32, Debrief.Ending.OUT_OF_PRAYER), earlier);
		// 4 doses a kill: two more kills are 8 doses, two four-dose potions.
		assertEquals("3 of your last 4 trips here ran out of prayer. About 2 more prayer potions would last two more kills.",
			d.getPattern());
		earlier.add(trip(8, 40, 0, 400, 32, Debrief.Ending.LEFT));
		earlier.add(trip(8, 40, 0, 400, 32, Debrief.Ending.LEFT));
		assertNull("only 2 of the last 6 trips ran out", Debrief.of(trip(8, 40, 0, 400, 32, Debrief.Ending.LEFT), earlier).getPattern());
	}

	@Test
	public void olderSavesWithoutEndingsStillRead()
	{
		String json = "{\"version\":1,\"bosses\":{\"sarachnis\":{\"kc\":10,\"best\":40.0,\"trips\":[{\"start\":0,\"end\":3600000,\"kills\":10,"
			+ "\"loot\":500000,\"deaths\":0,\"foodUsed\":400,\"dosesUsed\":10}]}}}";
		Logbook book = Logbook.parse(new Gson(), json);
		Logbook.TripRecord old = book.peek(Bosses.SARACHNIS).trips.get(0);
		assertNull(old.ended);
		assertEquals(-1, old.killsLeft, 0);
		Debrief d = Debrief.of(trip(12, 60, 600_000, 400, 10, Debrief.Ending.LEFT), book.peek(Bosses.SARACHNIS).trips);
		assertNull(d.getHistory().get(0).getEnding());
		assertEquals(1, d.getRank());
		assertEquals("You left the boss.", Debrief.reason(old, Debrief.Ending.LEFT));
	}

	@Test
	public void ordinals()
	{
		assertEquals("1st", Debrief.ordinal(1));
		assertEquals("2nd", Debrief.ordinal(2));
		assertEquals("3rd", Debrief.ordinal(3));
		assertEquals("11th", Debrief.ordinal(11));
		assertEquals("12th", Debrief.ordinal(12));
		assertEquals("22nd", Debrief.ordinal(22));
	}
}
