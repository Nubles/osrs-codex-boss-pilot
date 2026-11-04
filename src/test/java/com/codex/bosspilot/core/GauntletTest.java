package com.codex.bosspilot.core;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The Gauntlet against its sources: Reward Chest (The Gauntlet) for the rates (regular "1/120",
 * "1/120", "1/2,000", "1/2,000"; corrupted "1/50", "1/50", "1/400", "1/800"), The Gauntlet for
 * the preparation limits, RuneLite's gameval for the ids and its chat commands for the lines; then
 * the run watch, the run log with its ghost and the dry spells. Nothing here is about the fight.
 */
public class GauntletTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void ratesMatchTheWiki()
	{
		assertEquals(1 / 2000.0, Gauntlet.Unique.ENHANCED.chance(Gauntlet.Mode.REGULAR), 1e-15);
		assertEquals(1 / 400.0, Gauntlet.Unique.ENHANCED.chance(Gauntlet.Mode.CORRUPTED), 1e-15);
		assertEquals(1 / 120.0, Gauntlet.Unique.ARMOUR.chance(Gauntlet.Mode.REGULAR), 1e-15);
		assertEquals(1 / 50.0, Gauntlet.Unique.ARMOUR.chance(Gauntlet.Mode.CORRUPTED), 1e-15);
		assertEquals(1 / 120.0, Gauntlet.Unique.WEAPON.chance(Gauntlet.Mode.REGULAR), 1e-15);
		assertEquals(1 / 50.0, Gauntlet.Unique.WEAPON.chance(Gauntlet.Mode.CORRUPTED), 1e-15);
		assertEquals(1 / 2000.0, Gauntlet.Unique.PET.chance(Gauntlet.Mode.REGULAR), 1e-15);
		assertEquals(1 / 800.0, Gauntlet.Unique.PET.chance(Gauntlet.Mode.CORRUPTED), 1e-15);
		assertEquals("1/2,000, corrupted 1/400", Gauntlet.Unique.ENHANCED.rateText());
		assertEquals(600, Gauntlet.REGULAR_PREP_SECONDS);
		assertEquals(450, Gauntlet.CORRUPTED_PREP_SECONDS);
	}

	@Test
	public void idsMatchRuneLite()
	{
		assertEquals(VarbitID.PLAYER_IN_GAUNTLET, Gauntlet.VARBIT_IN_GAUNTLET);
		assertEquals(VarbitID.GAUNTLET_BOSS_STARTED, Gauntlet.VARBIT_BOSS_STARTED);
		assertEquals(VarbitID.GAUNTLET_CORRUPTED, Gauntlet.VARBIT_CORRUPTED);
		assertSame(Gauntlet.Unique.ENHANCED, Gauntlet.Unique.byId(ItemID.PRIF_WEAPON_SEED_ENHANCED));
		assertSame(Gauntlet.Unique.ARMOUR, Gauntlet.Unique.byId(ItemID.PRIF_ARMOUR_SEED));
		assertSame(Gauntlet.Unique.WEAPON, Gauntlet.Unique.byId(ItemID.CRYSTAL_SEED_OLD));
		assertSame(Gauntlet.Unique.PET, Gauntlet.Unique.byId(ItemID.GAUNTLETPET));
		assertSame(Gauntlet.Unique.PET, Gauntlet.Unique.byId(ItemID.GAUNTLETPET_CORRUPT));
		assertSame(Gauntlet.Mode.REGULAR, Gauntlet.hunllef(NpcID.CRYSTAL_HUNLLEF_MELEE));
		assertSame(Gauntlet.Mode.REGULAR, Gauntlet.hunllef(NpcID.CRYSTAL_HUNLLEF_DEATH));
		assertSame(Gauntlet.Mode.CORRUPTED, Gauntlet.hunllef(NpcID.CRYSTAL_HUNLLEF_MAGIC_HM));
		assertSame(Gauntlet.Mode.CORRUPTED, Gauntlet.hunllef(NpcID.CRYSTAL_HUNLLEF_DEATH_HM));
		assertNull(Gauntlet.hunllef(NpcID.CRYSTAL_HUNLLEF_CRYSTALS));
		assertSame(Gauntlet.Unique.PET, Gauntlet.Unique.byName("Corrupted Youngllef"));
		assertSame(Gauntlet.Unique.ARMOUR, Gauntlet.Unique.byName("Crystal armour seed"));
	}

	@Test
	public void readsTheGamesLines()
	{
		// RuneLite's chat commands tests, colour tags taken out.
		Chat.Kc kc = Chat.kc("Your Gauntlet completion count is: 124.");
		assertSame(Gauntlet.Mode.REGULAR, Gauntlet.Mode.byKc(kc.name));
		assertEquals(124, kc.count);
		kc = Chat.kc("Your Corrupted Gauntlet completion count is: 4729.");
		assertSame(Gauntlet.Mode.CORRUPTED, Gauntlet.Mode.byKc(kc.name));
		assertEquals(4729, kc.count);
		assertSame(Bosses.GAUNTLET, Bosses.byKc("Corrupted Gauntlet"));

		Gauntlet.Time t = Gauntlet.duration("Challenge duration: 10:24. Personal best: 7:59.");
		assertEquals(624, t.seconds, 1e-9);
		assertSame(Gauntlet.Mode.REGULAR, t.mode);
		t = Gauntlet.duration("Challenge duration: 10:24.40 (new personal best).");
		assertEquals(624.4, t.seconds, 1e-9);
		t = Gauntlet.duration("Corrupted challenge duration: 7:41.20. Personal best: 6:12.00.");
		assertEquals(461.2, t.seconds, 1e-9);
		assertSame(Gauntlet.Mode.CORRUPTED, t.mode);
		assertNull(Gauntlet.duration("Fight duration: 1:23. Personal best: 1:05."));
		// A plain "duration" line is not the Gauntlet's, and the boss log's reader leaves the Gauntlet's alone.
		assertEquals(0, Chat.duration("Challenge duration: 10:24. Personal best: 7:59."), 0);

		assertEquals(358, Gauntlet.preparation("Preparation time: 5:58. Hunllef kill time: 2:33."), 1e-9);
		assertEquals(0, Gauntlet.preparation("You leave the Gauntlet."), 0);
		assertTrue(Gauntlet.petMessage("You have a funny feeling like you're being followed."));
		assertTrue(Gauntlet.petMessage("You feel something weird sneaking into your backpack."));
	}

	@Test
	public void theBossIsWellFormed()
	{
		assertSame(Bosses.GAUNTLET, Bosses.byNpc("Corrupted Hunllef"));
		assertSame(Bosses.GAUNTLET, Bosses.byNpc("Crystalline Hunllef"));
		assertFalse(Bosses.GAUNTLET.bringsGear());
		assertTrue(Bosses.ZALCANO.bringsGear());
		// The level of the wiki's advised stats: 80/85/80 melee, 85 Ranged, 80 Magic, 85 Hitpoints, 77 Prayer.
		assertEquals(104, Bosses.GAUNTLET.getSuggestedCombat());
		for (Boss.Brief b : Bosses.GAUNTLET.getBriefing())
		{
			String a = b.getAnswer().toLowerCase();
			assertFalse(a, a.contains("pray") || a.contains("tornado") || a.contains("tile") || a.contains("attack"));
		}
	}

	/** A watch fed ticks: in for the given ticks, the boss from a tick on. */
	private static GauntletLog.Run play(GauntletWatch w, int from, int bossAt, int to, Gauntlet.Mode hint)
	{
		GauntletLog.Run out = null;
		for (int t = from; t <= to; t++)
		{
			GauntletLog.Run r = w.tick(t, t * 600L, true, t >= bossAt, hint);
			out = r != null ? r : out;
		}
		return out;
	}

	@Test
	public void aCompletedRunIsTimedPrepThenFight()
	{
		GauntletWatch w = new GauntletWatch();
		assertNull(play(w, 100, 700, 950, null));
		assertSame(GauntletWatch.Phase.FIGHT, w.phase());
		assertEquals(360, w.prepTaken(), 1e-9);
		w.completion(951, 951 * 600L, Gauntlet.Mode.REGULAR, 12);
		assertSame(GauntletWatch.Phase.ENDING, w.phase());
		assertNull(play(w, 951, 0, 952, null));
		assertTrue(w.pet());
		GauntletLog.Run r = play(w, 953, 0, 954, null);
		assertNotNull(r);
		assertSame(GauntletLog.Outcome.COMPLETED, r.outcome);
		assertEquals(360, r.prep, 1e-9);
		assertEquals((951 - 700) * 0.6, r.fight, 1e-9);
		assertEquals(12, r.kc);
		assertFalse(r.gameTimed);
		assertEquals(Collections.singletonList("Youngllef"), r.uniques);
		// Still inside after the run: no new run until the player has left.
		assertNull(play(w, 955, 0, 960, null));
		assertSame(GauntletWatch.Phase.NONE, w.phase());
		assertNull(w.tick(961, 961 * 600L, false, false, null));
		assertNull(play(w, 1100, 1500, 1101, Gauntlet.Mode.CORRUPTED));
		assertSame(GauntletWatch.Phase.PREP, w.phase());
		assertSame(Gauntlet.Mode.CORRUPTED, w.getMode());
	}

	@Test
	public void theGamesTimesWin()
	{
		GauntletWatch w = new GauntletWatch();
		play(w, 0, 500, 800, Gauntlet.Mode.CORRUPTED);
		w.duration(Gauntlet.duration("Corrupted challenge duration: 8:00.00. Personal best: 7:00.00."));
		w.preparation(Gauntlet.preparation("Preparation time: 5:10."));
		w.completion(801, 0, Gauntlet.Mode.CORRUPTED, 40);
		GauntletLog.Run r = play(w, 801, 0, 805, null);
		assertNotNull(r);
		assertTrue(r.gameTimed);
		assertEquals(310, r.prep, 1e-9);
		assertEquals(170, r.fight, 1e-9);
		assertEquals(480, r.total, 1e-9);
		assertSame(Gauntlet.Mode.CORRUPTED, r.mode);
	}

	@Test
	public void deathAndLeavingAreRunsToo()
	{
		GauntletWatch w = new GauntletWatch();
		play(w, 0, 900, 1000, null);
		w.died(1001);
		GauntletLog.Run r = w.tick(1001, 0, true, true, null);
		assertSame(GauntletLog.Outcome.DIED, r.outcome);
		assertEquals(540, r.prep, 1e-9);
		assertEquals(60.6, r.fight, 1e-9);

		w.tick(1002, 0, false, false, null);
		play(w, 2000, 99999, 2100, null);
		r = w.tick(2101, 0, false, false, null);
		assertSame(GauntletLog.Outcome.LEFT, r.outcome);
		assertEquals(-1, r.fight, 0);
		assertEquals(60.6, r.prep, 1e-9);

		play(w, 3000, 99999, 3010, null);
		r = w.abort(3011);
		assertSame(GauntletLog.Outcome.LEFT, r.outcome);
		assertNull(w.abort(3012));
	}

	@Test
	public void aCompletionWithoutItsStartStillCounts()
	{
		GauntletWatch w = new GauntletWatch();
		w.duration(Gauntlet.duration("Challenge duration: 9:00. Personal best: 8:00."));
		w.completion(10, 0, Gauntlet.Mode.REGULAR, 3);
		GauntletLog.Run r = null;
		for (int t = 10; t < 20 && r == null; t++)
		{
			r = w.tick(t, 0, false, false, null);
		}
		assertNotNull(r);
		assertSame(GauntletLog.Outcome.COMPLETED, r.outcome);
		assertEquals(-1, r.prep, 0);
		assertEquals(3, r.kc);
	}

	private static GauntletLog.Run run(Gauntlet.Mode m, GauntletLog.Outcome o, double prep, double fight)
	{
		GauntletLog.Run r = new GauntletLog.Run();
		r.mode = m;
		r.outcome = o;
		r.prep = prep;
		r.fight = fight;
		r.total = prep > 0 && fight > 0 ? prep + fight : -1;
		return r;
	}

	@Test
	public void theLogKeepsTheGhostAndTheDrySpells()
	{
		GauntletLog log = new GauntletLog();
		log.add(run(Gauntlet.Mode.REGULAR, GauntletLog.Outcome.COMPLETED, 400, 150));
		log.add(run(Gauntlet.Mode.REGULAR, GauntletLog.Outcome.COMPLETED, 380, 170));
		log.add(run(Gauntlet.Mode.REGULAR, GauntletLog.Outcome.DIED, 300, 20));
		GauntletLog.Totals t = log.regular;
		assertEquals(3, t.runs);
		assertEquals(2, t.completed);
		assertEquals(1, t.died);
		assertEquals(380, t.bestPrep, 1e-9);
		assertEquals(150, t.bestFight, 1e-9);
		assertEquals(530, t.ghostTotal(), 1e-9);
		assertEquals(550, t.bestTotal, 1e-9);
		assertEquals(390, t.averagePrep(), 1e-9);
		// The second run against the ghost before it.
		assertEquals(-20, log.recent.get(1).prepVsBest, 1e-9);
		assertEquals(20, log.recent.get(1).fightVsBest, 1e-9);
		// A death does not roll the rewards.
		assertEquals(2, log.since(Gauntlet.Unique.ARMOUR));

		log.add(run(Gauntlet.Mode.CORRUPTED, GauntletLog.Outcome.COMPLETED, 330, 200));
		assertEquals(2, log.since(Gauntlet.Unique.ARMOUR, Gauntlet.Mode.REGULAR));
		assertEquals(1, log.since(Gauntlet.Unique.ARMOUR, Gauntlet.Mode.CORRUPTED));
		assertTrue(log.late("Crystal armour seed"));
		assertFalse(log.late("Crystal armour seed"));
		assertEquals(0, log.since(Gauntlet.Unique.ARMOUR));
		assertEquals(3, log.since(Gauntlet.Unique.ENHANCED));
		assertEquals(1, log.found(Gauntlet.Unique.ARMOUR));
		assertEquals(Collections.singletonList("Crystal armour seed"), log.last().uniques);

		GauntletLog back = GauntletLog.parse(GSON, log.toJson(GSON));
		assertEquals(4, back.runs());
		assertEquals(530, back.regular.ghostTotal(), 1e-9);
		assertSame(Gauntlet.Mode.CORRUPTED, back.last().mode);
		assertTrue(GauntletLog.parse(GSON, "not json").isEmpty());
		assertTrue(GauntletLog.parse(GSON, null).isEmpty());
	}

	@Test
	public void theChanceByNowMixesTheModes()
	{
		// 100 regular and 50 corrupted runs without the armour seed: 1 - (119/120)^100 * (49/50)^50.
		double p = Gauntlet.atLeastOne(Gauntlet.Unique.ARMOUR, 100, 50);
		assertEquals(1 - Math.pow(119 / 120.0, 100) * Math.pow(49 / 50.0, 50), p, 1e-12);
		assertEquals(100 / 120.0 + 1.0, Gauntlet.expected(Gauntlet.Unique.ARMOUR, 100, 50), 1e-12);

		GauntletLog log = new GauntletLog();
		for (int i = 0; i < 100; i++)
		{
			log.add(run(Gauntlet.Mode.REGULAR, GauntletLog.Outcome.COMPLETED, 400, 150));
		}
		for (int i = 0; i < 50; i++)
		{
			log.add(run(Gauntlet.Mode.CORRUPTED, GauntletLog.Outcome.COMPLETED, 330, 200));
		}
		GauntletRun r = GauntletRun.idle(log, Arrays.asList("Enhanced crystal weapon seed"));
		assertTrue(r.owns(Gauntlet.Unique.ENHANCED));
		assertFalse(r.owns(Gauntlet.Unique.ARMOUR));
		assertEquals("150 runs without one (100 regular, 50 corrupted): 84% would have one by now", r.dryText(Gauntlet.Unique.ARMOUR));
		assertEquals("Armour seed: 150 runs dry", r.plaqueDropText());
		assertEquals("84% by now", r.plaqueDropRight());
	}

	@Test
	public void nothingMovesDuringTheFight()
	{
		GauntletLog log = new GauntletLog();
		log.add(run(Gauntlet.Mode.CORRUPTED, GauntletLog.Outcome.COMPLETED, 330, 200));
		GauntletRun prep = new GauntletRun(log, GauntletWatch.Phase.PREP, Gauntlet.Mode.CORRUPTED, 192, -1, null);
		assertEquals("Corrupted: preparing, 3:12 of 7:30", prep.phaseText());
		assertEquals("best 5:30", prep.ghostText());
		assertEquals("Preparing 3:12 / 7:30", prep.plaquePhase());
		GauntletRun fight = new GauntletRun(log, GauntletWatch.Phase.FIGHT, Gauntlet.Mode.CORRUPTED, -1, 345, null);
		assertEquals("Corrupted: fighting the Hunllef. Prep took 5:45", fight.phaseText());
		assertEquals("prep +0:15", fight.ghostText());
		assertEquals("Hunllef fight, prep 5:45", fight.plaquePhase());
		assertEquals("Last: completed in 8:50", fight.plaqueLast());
		assertEquals("Completed in 8:50, prep 5:30, fight 3:20", fight.lastText());
	}
}
