package com.codex.bosspilot.core;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class EfficiencyTest
{
	private static final long T0 = 1_790_000_000_000L;

	@Test
	public void aTripsEfficiencyIsItsBestSetupTimeOverItsAverageGameTime()
	{
		Logbook.TripRecord t = new Logbook.TripRecord();
		t.timed = 4;
		t.fightSeconds = 400;
		t.theory = 80;
		assertEquals(0.8, Efficiency.of(t), 1e-9);
		assertEquals("Your 4 timed kills averaged 1:40 against ~1:20 with the best setup from the gear you brought.",
			Efficiency.tripText(t));

		Logbook.TripRecord untimed = new Logbook.TripRecord();
		untimed.theory = 80;
		assertEquals(-1, Efficiency.of(untimed), 0);
		Logbook.TripRecord noTheory = new Logbook.TripRecord();
		noTheory.timed = 3;
		noTheory.fightSeconds = 300;
		assertEquals(-1, Efficiency.of(noTheory), 0);
		assertNull(Efficiency.tripText(noTheory));
	}

	@Test
	public void onlyTheGamesOwnFightDurationsCountNotTheClock()
	{
		TripTracker tracker = new TripTracker();
		tracker.observe(Bosses.SARACHNIS, 400, 20, T0);
		// The first kill has no game time, so the clock times nothing; the second is clock-timed.
		tracker.kill(Bosses.SARACHNIS, T0 + 60_000, 10);
		tracker.kill(Bosses.SARACHNIS, T0 + 160_000, 11);
		// The third and fourth come with the game's own durations, before and after the kill count.
		tracker.duration(84, T0 + 250_000);
		tracker.kill(Bosses.SARACHNIS, T0 + 250_100, 12);
		tracker.kill(Bosses.SARACHNIS, T0 + 340_000, 13);
		tracker.duration(76, T0 + 340_500);
		tracker.theory(70);
		Trip trip = tracker.current();
		assertEquals(Arrays.asList(84.0, 76.0), trip.getGameSeconds());
		assertEquals(3, trip.getKillSeconds().size());
		Logbook.TripRecord r = trip.record();
		assertEquals(2, r.timed);
		assertEquals(160, r.fightSeconds, 1e-9);
		assertEquals(70, r.theory, 1e-9);
		assertEquals(13, r.kcEnd);
		assertEquals(70 / 80.0, Efficiency.of(r), 1e-9);
	}

	@Test
	public void theLogbookKeepsTheCurveAndTheMedianOfRecentKills()
	{
		Logbook book = new Logbook();
		for (int i = 0; i < 40; i++)
		{
			Logbook.TripRecord r = trip(5, 100, 80);
			r.kcEnd = 10 + i * 5;
			book.add(Bosses.SARACHNIS, r);
		}
		Logbook.Entry e = book.peek(Bosses.SARACHNIS);
		assertEquals(Logbook.CURVE, e.curve.size());
		assertEquals(205, e.curve.get(e.curve.size() - 1).at);
		assertEquals(0.8, e.curve.get(0).eff, 1e-9);

		List<Double> times = new ArrayList<>();
		for (int i = 1; i <= 60; i++)
		{
			times.add((double) i);
		}
		book.addTimes(Bosses.SARACHNIS, times);
		assertEquals(Logbook.TIMES, e.times.size());
		// The last 50 of 1..60 are 11..60: their median is 35.5.
		assertEquals(35.5, book.median(Bosses.SARACHNIS), 1e-9);
		assertEquals(-1, book.median(Bosses.VORKATH), 0);
	}

	@Test
	public void aBossWithSeveralKillCountsIsDrawnAlongTheKillsLogged()
	{
		Logbook book = new Logbook();
		Logbook.TripRecord a = trip(6, 60, 50);
		a.kcEnd = 300;
		Logbook.TripRecord b = trip(4, 60, 50);
		b.kcEnd = 120;
		book.add(Bosses.DAGANNOTH_KINGS, a);
		book.add(Bosses.DAGANNOTH_KINGS, b);
		Logbook.Entry e = book.peek(Bosses.DAGANNOTH_KINGS);
		assertEquals(6, e.curve.get(0).at);
		assertEquals(10, e.curve.get(1).at);
	}

	@Test
	public void aRisingCurveSaysPracticeIsPaying()
	{
		Efficiency.Read r = Efficiency.read(curve(0.60, 0.66, 0.72, 0.78, 0.84, 0.88));
		assertEquals(Efficiency.Trend.RISING, r.getTrend());
		assertTrue(r.text(), r.text().startsWith("Up "));
		assertTrue(r.text().contains("practice is still paying off"));
	}

	@Test
	public void aFlatCurveSaysAnUpgradeWouldDoMore()
	{
		Efficiency.Read r = Efficiency.read(curve(0.60, 0.70, 0.81, 0.80, 0.82, 0.81, 0.80));
		assertEquals(Efficiency.Trend.FLAT, r.getTrend());
		assertEquals(30, r.getWindowKills());
		assertEquals("Flat at about 81% for your last 30 timed kills. More practice is unlikely to speed you up much;"
			+ " a gear upgrade would.", r.text());
		// The window starts at the fifth-last trip: 6 kills each, 30 in all.
		assertEquals(2, r.windowStart());
	}

	@Test
	public void aFallingCurveSaysSo()
	{
		Efficiency.Read r = Efficiency.read(curve(0.90, 0.88, 0.86, 0.80, 0.78, 0.76));
		assertEquals(Efficiency.Trend.FALLING, r.getTrend());
		assertTrue(r.text(), r.text().startsWith("Down "));
	}

	@Test
	public void tooFewTimedKillsMakeNoTrend()
	{
		Efficiency.Read r = Efficiency.read(curve(0.7, 0.8));
		assertEquals(Efficiency.Trend.NONE, r.getTrend());
		assertEquals(0.75, r.getRecent(), 1e-9);
		assertEquals("12 timed kills so far. After 30 over 3 trips, Boss Pilot can tell whether you are still getting faster.", r.text());
		assertTrue(Efficiency.read(null).isEmpty());
		assertEquals(Efficiency.Trend.NONE, Efficiency.read(new ArrayList<>()).getTrend());
	}

	@Test
	public void logbooksSavedBeforeEfficiencyStillRead()
	{
		String old = "{\"version\":1,\"bosses\":{\"sarachnis\":{\"kc\":52,\"best\":70.2,\"trips\":[{\"start\":1,\"end\":600001,"
			+ "\"kills\":5,\"loot\":300000,\"deaths\":0,\"foodUsed\":200,\"dosesUsed\":12,\"bestKill\":70.2}]}}}";
		Logbook book = Logbook.parse(new Gson(), old);
		Logbook.Entry e = book.peek(Bosses.SARACHNIS);
		assertEquals(1, e.trips.size());
		assertEquals(-1, e.trips.get(0).theory, 0);
		assertEquals(-1, e.trips.get(0).kcEnd);
		assertEquals(0, e.trips.get(0).timed);
		assertTrue(e.times.isEmpty());
		assertTrue(e.curve.isEmpty());
		assertEquals(-1, Efficiency.of(e.trips.get(0)), 0);

		String damaged = "{\"version\":1,\"bosses\":{\"sarachnis\":{\"kc\":52,\"times\":[null,-3,80.5],\"curve\":[null,{\"at\":3,\"kills\":2,\"eff\":0}]}}}";
		Logbook d = Logbook.parse(new Gson(), damaged);
		assertEquals(Arrays.asList(80.5), d.peek(Bosses.SARACHNIS).times);
		assertTrue(d.peek(Bosses.SARACHNIS).curve.isEmpty());
	}

	private static List<Logbook.Point> curve(double... effs)
	{
		List<Logbook.Point> out = new ArrayList<>();
		int at = 0;
		for (double e : effs)
		{
			at += 6;
			Logbook.Point p = new Logbook.Point();
			p.at = at;
			p.kills = 6;
			p.eff = e;
			out.add(p);
		}
		return out;
	}

	private static Logbook.TripRecord trip(int kills, double avg, double theory)
	{
		Logbook.TripRecord r = new Logbook.TripRecord();
		r.start = T0;
		r.end = T0 + 20 * 60_000;
		r.kills = kills;
		r.timed = kills;
		r.fightSeconds = avg * kills;
		r.theory = theory;
		return r;
	}
}
