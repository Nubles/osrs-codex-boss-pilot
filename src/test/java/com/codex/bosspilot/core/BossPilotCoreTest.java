package com.codex.bosspilot.core;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BossPilotCoreTest
{
	private static final Gson GSON = new Gson();

	private static Map<String, Long> items(Object... pairs)
	{
		Map<String, Long> m = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			m.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
		}
		return m;
	}

	private static Account account(int combat, String task, String... quests)
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Hitpoints", "Prayer", "Agility", "Slayer"})
		{
			levels.put(s, 70);
		}
		return Account.of(levels, new HashSet<>(Arrays.asList(quests)), combat, task, true);
	}

	// ---- Items by name ----

	@Test
	public void doseAndFoodAreReadFromNames()
	{
		assertEquals(3, Dose.of("Prayer potion(3)"));
		assertEquals("Prayer potion", Dose.base("Prayer potion(3)"));
		assertEquals(0, Dose.of("Shark"));
		assertEquals(20, Food.heal("Shark"));
		assertEquals(4 * Food.BREW_DOSE, Food.heal("Saradomin brew(4)"));
		assertEquals(0, Food.heal("Spade"));
	}

	@Test
	public void supplyLinesCountHealingDosesAndPrefixedItems()
	{
		Stock s = Stock.of(items("Shark", 10, "Prayer potion(4)", 2, "Super restore(2)", 3, "Ring of dueling(8)", 1, "Spade", 1));
		assertEquals(200, s.count(Supply.food(100)));
		assertEquals(14, s.count(Supply.doses("Prayer", 1, Bosses.PRAYER)));
		assertEquals(1, s.count(Supply.optional("Dueling", 1, "Ring of dueling*")));
		assertEquals(1, s.count(Supply.must("Spade", 1, "Spade")));
		assertEquals(0, s.count(Supply.must("Rope", 1, "Rope")));
	}

	// ---- Kit ----

	@Test
	public void kitSaysWhatToTakeOutOfTheBank()
	{
		Stock bank = Stock.of(items("Spade", 1, "Shark", 30, "Prayer potion(4)", 5));
		Stock carried = Stock.of(items("Shark", 4));
		KitCheck kit = KitCheck.of(Bosses.GIANT_MOLE, bank, carried, false);
		assertTrue(kit.missingMust().isEmpty());
		assertTrue(kit.isOwned());
		assertFalse(kit.isPacked());
		List<KitCheck.Line> out = kit.toWithdraw();
		assertEquals("Spade", out.get(0).getSupply().getLabel());
		KitCheck.Line food = out.get(1);
		assertEquals("Food", food.getSupply().getLabel());
		assertEquals(200 - 80, food.toWithdraw());
	}

	@Test
	public void firstVisitLinesDropOutOnceVisited()
	{
		KitCheck fresh = KitCheck.of(Bosses.KALPHITE_QUEEN, Stock.EMPTY, Stock.EMPTY, false);
		KitCheck again = KitCheck.of(Bosses.KALPHITE_QUEEN, Stock.EMPTY, Stock.EMPTY, true);
		assertEquals(fresh.getLines().size() - 1, again.getLines().size());
	}

	// ---- Readiness ----

	@Test
	public void questsLevelsTasksAndKitDecideReadiness()
	{
		KitCheck noShield = KitCheck.of(Bosses.VORKATH, Stock.EMPTY, Stock.EMPTY, false);
		Readiness locked = Readiness.of(Bosses.VORKATH, account(110, null), noShield);
		assertEquals(Readiness.Status.LOCKED, locked.getStatus());
		assertEquals("Quest: Dragon Slayer II", locked.getMissing().get(0));

		Readiness kit = Readiness.of(Bosses.VORKATH, account(110, null, "DRAGON_SLAYER_II"), noShield);
		assertEquals(Readiness.Status.NEEDS_KIT, kit.getStatus());

		Readiness slayer = Readiness.of(Bosses.KRAKEN, account(110, null), null);
		assertEquals(Readiness.Status.LOCKED, slayer.getStatus());
		assertEquals("87 Slayer (you have 70)", slayer.getMissing().get(0));

		Map<String, Integer> levels = new HashMap<>(account(110, null).getLevels());
		levels.put("Slayer", 90);
		Account slayerAccount = Account.of(levels, Collections.emptySet(), 110, null, true);
		assertEquals(Readiness.Status.NEEDS_TASK, Readiness.of(Bosses.KRAKEN, slayerAccount, null).getStatus());
		Account onTask = Account.of(levels, Collections.emptySet(), 110, "Cave kraken", true);
		assertEquals(Readiness.Status.READY, Readiness.of(Bosses.KRAKEN, onTask, null).getStatus());

		assertEquals(Readiness.Status.STRETCH, Readiness.of(Bosses.SARACHNIS, account(60, null), null).getStatus());
		assertEquals("Priest in Peril", Readiness.questName("PRIEST_IN_PERIL"));
	}

	// ---- Ranking ----

	@Test
	public void progressPutsTheHardestNewOpenBossFirst()
	{
		Ranker.Inputs in = new Ranker.Inputs();
		in.account = account(90, null, "PRIEST_IN_PERIL");
		in.bank = Stock.of(items("Spade", 1, "Shark", 100, "Prayer potion(4)", 20));
		List<Ranker.Pick> picks = Ranker.rank(Bosses.ALL, in);
		Ranker.Pick next = Ranker.next(picks);
		assertNotNull(next);
		assertTrue(next.getStatus().isOpen());
		// Every open boss comes before every locked one.
		boolean seenLocked = false;
		for (Ranker.Pick p : picks)
		{
			if (p.getStatus() == Readiness.Status.LOCKED)
			{
				seenLocked = true;
			}
			else
			{
				assertFalse(p.getBoss() + " after a locked boss", seenLocked);
			}
		}
		// Wilderness bosses stay out unless asked for.
		for (Ranker.Pick p : picks)
		{
			assertFalse(p.getBoss().isWilderness());
		}
	}

	@Test
	public void aPinnedBossLeadsWhateverItsStatus()
	{
		Ranker.Inputs in = new Ranker.Inputs();
		in.account = account(60, null);
		in.pinned = Bosses.VORKATH.getKey();
		List<Ranker.Pick> picks = Ranker.rank(Bosses.ALL, in);
		assertSame(Bosses.VORKATH, picks.get(0).getBoss());
		assertSame(Bosses.VORKATH, Ranker.next(picks).getBoss());
	}

	@Test
	public void lootAimFollowsTheLogbook()
	{
		Ranker.Inputs in = new Ranker.Inputs();
		in.account = account(110, null, "PRIEST_IN_PERIL");
		in.bank = Stock.of(items("Spade", 1, "Shark", 100, "Prayer potion(4)", 40));
		in.aim = Aim.LOOT;
		Logbook.TripRecord t = new Logbook.TripRecord();
		t.start = 0;
		t.end = 60 * 60_000;
		t.kills = 20;
		t.loot = 3_000_000;
		in.logbook.add(Bosses.GIANT_MOLE, t);
		Ranker.Pick next = Ranker.next(Ranker.rank(Bosses.ALL, in));
		assertSame(Bosses.GIANT_MOLE, next.getBoss());
		assertTrue(next.getWhy(), next.getWhy().contains("3.0M an hour"));
	}

	// ---- Chat ----

	@Test
	public void chatGivesKillCountsAndDurations()
	{
		Chat.Kc kc = Chat.kc("Your Sarachnis kill count is: 1,052.");
		assertEquals("Sarachnis", kc.name);
		assertEquals(1052, kc.count);
		assertSame(Bosses.BARROWS, Bosses.byKc(Chat.kc("Your Barrows chest count is: 12.").name));
		assertSame(Bosses.DAGANNOTH_KINGS, Bosses.byKc(Chat.kc("Your Dagannoth Prime kill count is: 3.").name));
		assertNull(Chat.kc("Welcome to Old School RuneScape."));
		assertEquals(83.0, Chat.duration("Fight duration: 1:23. Personal best: 1:05."), 1e-9);
		assertEquals(65.4, Chat.duration("Fight duration: 1:05.40 (new personal best)"), 1e-9);
		assertEquals(0, Chat.duration("You eat the shark."), 1e-9);
		assertEquals(3723, Chat.seconds("1:02:03"), 1e-9);
	}

	// ---- Trips ----

	@Test
	public void aTripCountsKillsSuppliesAndEndsWhenTheBossIsLeft()
	{
		TripTracker t = new TripTracker();
		long now = 1_000_000;
		t.observe(Bosses.SARACHNIS, 400, 16, now);
		Trip trip = t.current();
		assertNotNull(trip);
		t.kill(Bosses.SARACHNIS, now += 90_000);
		t.duration(70, now + 10);
		t.observe(Bosses.SARACHNIS, 340, 13, now += 1000);
		t.kill(Bosses.SARACHNIS, now += 80_000);
		t.duration(75, now);
		t.observe(Bosses.SARACHNIS, 280, 10, now += 1000);
		assertEquals(2, trip.getKills());
		assertEquals(Arrays.asList(70.0, 75.0), trip.getKillSeconds());
		assertEquals(120, trip.getFoodUsed());
		assertEquals(6, trip.getDosesUsed());

		// Taking more food out does not count as having eaten less.
		t.observe(null, 400, 10, now += 1000);
		assertEquals(120, trip.getFoodUsed());

		Outlook o = Outlook.of(trip, null);
		assertEquals(Outlook.Level.PLENTY, o.getLevel());
		assertEquals("prayer", o.getLimit());
		assertEquals(10 / 3.0, o.getKills(), 1e-9);

		t.observe(null, 400, 10, now + TripTracker.END_AFTER_MS + 1);
		assertNull(t.current());
		assertSame(trip, t.takeFinished());
		assertNull(t.takeFinished());
	}

	@Test
	public void theFirstKillIsNotTimedFromTheWalkIn()
	{
		TripTracker t = new TripTracker();
		t.observe(Bosses.GIANT_MOLE, 200, 8, 0);
		t.kill(Bosses.GIANT_MOLE, 300_000);
		assertTrue(t.current().getKillSeconds().isEmpty());
		t.kill(Bosses.GIANT_MOLE, 360_000);
		assertEquals(Collections.singletonList(60.0), t.current().getKillSeconds());
	}

	@Test
	public void outlookWarnsOnTheLastKill()
	{
		TripTracker t = new TripTracker();
		t.observe(Bosses.SARACHNIS, 300, 20, 0);
		t.kill(Bosses.SARACHNIS, 60_000);
		t.observe(Bosses.SARACHNIS, 40, 20, 61_000);
		Outlook o = Outlook.of(t.current(), null);
		assertEquals(Outlook.Level.LAST, o.getLevel());
		assertTrue(o.text().startsWith("Bank after this kill"));
	}

	@Test
	public void outlookUsesPastTripsBeforeTheFirstKill()
	{
		Logbook book = new Logbook();
		Logbook.TripRecord r = new Logbook.TripRecord();
		r.end = 600_000;
		r.kills = 5;
		r.foodUsed = 250;
		r.dosesUsed = 10;
		book.add(Bosses.SARACHNIS, r);
		TripTracker t = new TripTracker();
		t.observe(Bosses.SARACHNIS, 200, 20, 0);
		Outlook o = Outlook.of(t.current(), book.rates(Bosses.SARACHNIS));
		assertTrue(o.isFromHistory());
		assertEquals(4, o.getKills(), 1e-9);
	}

	@Test
	public void anotherBossEndsTheTrip()
	{
		TripTracker t = new TripTracker();
		t.observe(Bosses.SARACHNIS, 200, 8, 0);
		Trip first = t.current();
		t.kill(Bosses.GIANT_MOLE, 1000);
		assertSame(first, t.takeFinished());
		assertSame(Bosses.GIANT_MOLE, t.current().getBoss());
	}

	// ---- Logbook and review ----

	@Test
	public void logbookSurvivesASaveAndKeepsTheLastTrips()
	{
		Logbook book = new Logbook();
		for (int i = 0; i < Logbook.TRIPS + 3; i++)
		{
			Logbook.TripRecord r = new Logbook.TripRecord();
			r.start = i;
			r.end = i + 60_000;
			r.kills = 1;
			r.bestKill = 100 - i;
			book.add(Bosses.ZULRAH, r);
		}
		book.noteKc(Bosses.ZULRAH, 40);
		book.noteKc(Bosses.ZULRAH, 12);
		Logbook back = Logbook.parse(GSON, book.toJson(GSON));
		assertEquals(Logbook.TRIPS, back.peek(Bosses.ZULRAH).trips.size());
		assertEquals(40, back.kc(Bosses.ZULRAH));
		assertEquals(100 - Logbook.TRIPS - 2, back.peek(Bosses.ZULRAH).best, 1e-9);
		assertTrue(back.visited(Bosses.ZULRAH));
		assertFalse(back.visited(Bosses.VORKATH));
		assertEquals(0, Logbook.parse(GSON, "{not json").bosses.size());
		assertEquals(0, Logbook.parse(GSON, "{\"version\":9,\"bosses\":{}}").bosses.size());
	}

	@Test
	public void reviewGivesOneThingToDoNextTime()
	{
		Logbook.TripRecord r = new Logbook.TripRecord();
		r.kills = 4;
		r.foodUsed = 240;
		assertTrue(Review.advise(r, 0.4), Review.advise(r, 0.4).startsWith("You ran low after 4 kills. About 6 more sharks"));
		assertTrue(Review.advise(r, 6).startsWith("You came back with supplies for about 6 more kills"));
		assertTrue(Review.advise(r, 2).startsWith("A well-packed trip"));
		r.deaths = 1;
		assertTrue(Review.advise(r, 2).startsWith("You died once"));
	}

	@Test
	public void everyBossIsWellFormed()
	{
		HashSet<String> keys = new HashSet<>();
		for (Boss b : Bosses.ALL)
		{
			assertTrue("duplicate key " + b.getKey(), keys.add(b.getKey()));
			assertFalse(b.getNpcNames().isEmpty());
			assertFalse(b.getKcNames().isEmpty());
			assertFalse(b.getKit().isEmpty());
			assertFalse(b.getWhere().isEmpty());
			assertTrue(b.getSuggestedCombat() > 0);
			assertSame(b, Bosses.byKey(b.getKey()));
			for (String npc : b.getNpcNames())
			{
				assertSame(b, Bosses.byNpc(npc));
			}
		}
	}

	@Test
	public void formatWritesKillTimes()
	{
		assertEquals("0:48", Format.clock(48));
		assertEquals("1:23", Format.clock(83.4));
		assertEquals("1:02:03", Format.clock(3723));
	}
}
