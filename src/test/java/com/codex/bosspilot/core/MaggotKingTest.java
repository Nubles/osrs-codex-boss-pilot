package com.codex.bosspilot.core;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The Maggot King's facts, kill assembly, log and odds. Rates are the OSRS Wiki's (Maggot King, read
 * 4 November 2026): 1/340, 1/520 and 1/3,500 from Jagex; 1/1,502.4 the pet on any egg-take; 2/159
 * the venator fang and tooth on the main table (Drop Rate Project estimate).
 */
public class MaggotKingTest
{
	private static final double EPS = 1e-9;

	private static MaggotWatch.Drop drop(String name, long qty, long worth)
	{
		return new MaggotWatch.Drop(name, qty, worth);
	}

	@Test
	public void ratesMatchTheWiki()
	{
		// "The chance of receiving any unique item is 1/205.6."
		assertEquals(205.6, 1 / MaggotKing.PRE_ROLL, 0.05);
		assertEquals(1 / 340.0, MaggotKing.Unique.ELDER_FANG.rate(MaggotKing.Choice.STOMACH), EPS);
		assertEquals(0, MaggotKing.Unique.ELDER_FANG.rate(MaggotKing.Choice.EGGS), EPS);
		assertEquals(1 / 520.0, MaggotKing.Unique.KISTEN.rate(MaggotKing.Choice.STOMACH), EPS);
		assertEquals(1 / 3500.0, MaggotKing.Unique.PET.rate(MaggotKing.Choice.STOMACH), EPS);
		assertEquals(1 / 1502.4, MaggotKing.Unique.PET.rate(MaggotKing.Choice.EGGS), EPS);
		assertEquals((1 - MaggotKing.PRE_ROLL) * 2 / 159.0, MaggotKing.Unique.VENATOR_FANG.rate(MaggotKing.Choice.STOMACH), EPS);
		assertTrue(MaggotKing.Unique.VENATOR_TOOTH.isEstimate());
		assertFalse(MaggotKing.Unique.ELDER_FANG.isEstimate());
		assertEquals("1 in 3,500 opening the stomach, 1 in 1,502 taking eggs", MaggotKing.Unique.PET.rateText());
		assertEquals("1/80~", MaggotKing.Unique.VENATOR_FANG.shortRate());
	}

	@Test
	public void idsAreRuneLites()
	{
		// RuneLite gameval: NpcID.MAGGOT_KING, MAGGOT_KING_CORPSE; ItemID.ELDER_VENATOR_FANG, CRIMSON_KISTEN,
		// MAGGOTKINGPET, VENATOR_FANG, VENATOR_TOOTH, MAGGOT_EGG..WRITHING_MAGGOT_EGG; VarPlayerID.TOTAL_MAGGOT_KING_KILLS.
		assertTrue(MaggotKing.isBoss(15742));
		assertTrue(MaggotKing.isBoss(15741));
		assertFalse(MaggotKing.isBoss(15740));
		assertEquals(33634, MaggotKing.Unique.ELDER_FANG.getItemId());
		assertEquals(33631, MaggotKing.Unique.KISTEN.getItemId());
		assertEquals(33642, MaggotKing.Unique.PET.getItemId());
		assertEquals(33661, MaggotKing.Unique.VENATOR_FANG.getItemId());
		assertEquals(33663, MaggotKing.Unique.VENATOR_TOOTH.getItemId());
		assertTrue(MaggotKing.isEgg(33665));
		assertTrue(MaggotKing.isEgg(33675));
		assertFalse(MaggotKing.isEgg(33677));
		assertEquals(5614, MaggotKing.KILLS_VARP);
		assertSame(MaggotKing.Unique.ELDER_FANG, MaggotKing.Unique.byName("Etched elder venator fang"));
		assertSame(MaggotKing.Choice.STOMACH, MaggotKing.Choice.byOption("Open-stomach"));
		assertSame(MaggotKing.Choice.EGGS, MaggotKing.Choice.byOption("take-eggs"));
		assertNull(MaggotKing.Choice.byOption("Walk here"));
	}

	@Test
	public void bossEntry()
	{
		Boss b = Bosses.MAGGOT_KING;
		assertSame(b, Bosses.byNpc("Maggot King"));
		assertSame(b, Bosses.byNpc("Maggot King Corpse"));
		assertSame(b, Bosses.byKc("Maggot King"));
		assertEquals(Collections.singletonList("THE_BLOOD_MOON_RISES"), b.getQuests());
		assertTrue(b.getSkills().isEmpty());
		assertTrue(b.getTargets().isEmpty());
		assertEquals("The Blood Moon Rises", Readiness.questName("THE_BLOOD_MOON_RISES"));
	}

	@Test
	public void killsForEvenAndNineInTen()
	{
		assertEquals(236, MaggotKing.killsFor(1 / 340.0, 0.5));
		assertEquals(782, MaggotKing.killsFor(1 / 340.0, 0.9));
		assertEquals(-1, MaggotKing.killsFor(0, 0.5));
		assertEquals(1 / 3500.0 * 0.5 + 1 / 1502.4 * 0.5, MaggotKing.perKill(MaggotKing.Unique.PET, 0.5), EPS);
	}

	@Test
	public void aKillFromItsCountChoiceAndLoot()
	{
		MaggotWatch w = new MaggotWatch();
		w.baseline(40);
		assertTrue(w.kc(1, 1000, 41));
		// The game's own count of the same kill a tick later is not a second kill.
		assertFalse(w.kc(2, 1600, 41));
		w.choice(5, 4000, MaggotKing.Choice.STOMACH);
		w.loot(6, 4600, Arrays.asList(drop("Runite ore", 13, 130_000), drop("Elder venator fang", 1, 20_000_000)));
		assertTrue(w.poll(7).isEmpty());
		List<MaggotWatch.Event> out = w.poll(8);
		assertEquals(1, out.size());
		MaggotLog.Kill k = out.get(0).kill;
		assertEquals(41, k.kc);
		assertEquals("STOMACH", k.choice);
		assertEquals(20_130_000, k.value);
		assertEquals(Arrays.asList("13 x Runite ore", "Elder venator fang"), k.items);
		assertEquals(Collections.singletonList("ELDER_FANG"), k.uniques);
		assertEquals(1000, k.at);
	}

	@Test
	public void lootFirstThenALateCount()
	{
		MaggotWatch w = new MaggotWatch();
		w.baseline(10);
		w.loot(1, 0, Collections.singletonList(drop("Uncut ruby", 12, 12_000)));
		List<MaggotWatch.Event> out = w.poll(3);
		assertEquals(1, out.size());
		assertEquals(-1, out.get(0).kill.kc);
		assertSame(MaggotKing.Choice.STOMACH, out.get(0).kill.choice());
		// The count, a few ticks late, joins that kill rather than starting another.
		assertTrue(w.kc(6, 3000, 11));
		assertEquals(11, out.get(0).kill.kc);
		assertFalse(w.pending());
	}

	@Test
	public void eggsThatGiveNothingAndEggsPoppedLater()
	{
		MaggotWatch w = new MaggotWatch();
		w.kc(1, 0, 5);
		w.eggsNothing(4, 2000);
		MaggotLog.Kill k = w.poll(6).get(0).kill;
		assertSame(MaggotKing.Choice.EGGS, k.choice());
		assertEquals(0, k.value);

		// Taking eggs that give one: inferred from the egg even without the click.
		w.kc(20, 10_000, 6);
		w.loot(22, 11_000, Collections.singletonList(drop("Maggot egg", 1, 0)));
		k = w.poll(24).get(0).kill;
		assertSame(MaggotKing.Choice.EGGS, k.choice());

		// Popping that egg later, at the bank: the pet it gives is the egg's, not a new kill.
		w.pop(500);
		w.loot(501, 300_000, Collections.singletonList(drop("Maggot marquess", 1, 0)));
		List<MaggotWatch.Event> out = w.poll(510);
		assertEquals(1, out.size());
		assertNull(out.get(0).kill);
		assertSame(MaggotKing.Unique.PET, out.get(0).late);
		assertFalse(out.get(0).ofLastKill);
	}

	@Test
	public void petMessageJoinsTheKillAndUnlootedKillsAreLogged()
	{
		MaggotWatch w = new MaggotWatch();
		w.kc(1, 0, 1);
		w.choice(3, 0, MaggotKing.Choice.STOMACH);
		w.loot(4, 0, Collections.singletonList(drop("Gold ore", 30, 3000)));
		w.pet(4);
		MaggotLog.Kill k = w.poll(6).get(0).kill;
		assertEquals(Collections.singletonList("PET"), k.uniques);

		// A kill left unlooted is logged without loot when the next one begins.
		w.kc(50, 0, 2);
		w.kc(150, 60_000, 3);
		List<MaggotWatch.Event> out = w.poll(151);
		assertEquals(1, out.size());
		assertEquals(2, out.get(0).kill.kc);
		assertEquals(-1, out.get(0).kill.value);
		assertNull(out.get(0).kill.choice());
		assertEquals(1, w.end(152).size());
	}

	@Test
	public void inferenceFromLootAlone()
	{
		Map<String, Long> shared = new HashMap<>();
		shared.put("Stymphike tartare", 3L);
		shared.put("Dull ancient medal", 1L);
		assertNull(MaggotWatch.infer(shared, false));
		shared.put("Clue scroll (elite)", 1L);
		assertNull(MaggotWatch.infer(shared, false));
		shared.put("Letvek", 25L);
		assertSame(MaggotKing.Choice.STOMACH, MaggotWatch.infer(shared, false));
		assertSame(MaggotKing.Choice.EGGS, MaggotWatch.infer(new HashMap<>(), true));
	}

	private static MaggotLog.Kill kill(MaggotKing.Choice c, long value, long seconds, String... uniques)
	{
		MaggotLog.Kill k = new MaggotLog.Kill();
		k.choice = c == null ? null : c.name();
		k.value = value;
		k.seconds = seconds;
		k.uniques.addAll(Arrays.asList(uniques));
		return k;
	}

	@Test
	public void theLogCountsOddsOnlyForLootsThatCouldBringThem()
	{
		MaggotLog log = new MaggotLog();
		for (int i = 0; i < 10; i++)
		{
			log.add(kill(MaggotKing.Choice.STOMACH, 100_000, 150));
		}
		log.add(kill(MaggotKing.Choice.EGGS, 0, 150));
		log.add(kill(null, -1, -1));
		assertEquals(12, log.kills);
		assertEquals(10, log.stomachs);
		assertEquals(1, log.eggs);
		assertEquals(1, log.unseen());
		assertEquals(10, log.peek(MaggotKing.Unique.ELDER_FANG).since);
		assertEquals(11, log.peek(MaggotKing.Unique.PET).since);
		assertEquals(10 * Math.log(1 - 1 / 340.0), log.peek(MaggotKing.Unique.ELDER_FANG).missLn, EPS);
		assertEquals(10 / 3500.0 + 1 / 1502.4, log.peek(MaggotKing.Unique.PET).expected, EPS);
		assertEquals(11, log.timed);
		assertEquals(24.0, log.killsPerHour(), 1e-6);
		assertEquals(1 / 11.0, log.eggShare(), EPS);

		log.add(kill(MaggotKing.Choice.STOMACH, 5_000_000, 150, "KISTEN"));
		assertEquals(0, log.peek(MaggotKing.Unique.KISTEN).since);
		assertEquals(0, log.peek(MaggotKing.Unique.KISTEN).missLn, EPS);
		assertEquals(1, log.peek(MaggotKing.Unique.KISTEN).found);
		assertEquals(0, log.sinceRare());

		// The pet from an egg popped later stands on its own; from the last kill's message, it joins that kill.
		log.late(MaggotKing.Unique.PET, true);
		assertEquals(Arrays.asList("KISTEN", "PET"), log.last().uniques);
		assertEquals(1, log.peek(MaggotKing.Unique.PET).found);
		log.late(MaggotKing.Unique.PET, true);
		assertEquals(1, log.peek(MaggotKing.Unique.PET).found);

		Gson gson = new Gson();
		MaggotLog back = MaggotLog.parse(gson, log.toJson(gson));
		assertEquals(13, back.kills);
		assertEquals(1, back.peek(MaggotKing.Unique.KISTEN).found);
		assertTrue(MaggotLog.parse(gson, "not json").isEmpty());
		assertTrue(MaggotLog.parse(gson, null).isEmpty());
	}

	@Test
	public void theRunSaysTheOddsAndTheTimeToTheGoal()
	{
		MaggotLog log = new MaggotLog();
		for (int i = 0; i < 340; i++)
		{
			log.add(kill(MaggotKing.Choice.STOMACH, 300_000, 150));
		}
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Attack", 92);
		levels.put("Strength", 90);
		levels.put("Magic", 94);
		levels.put("Prayer", 77);
		levels.put("Ranged", 85);
		levels.put("Agility", 70);
		Account account = Account.of(levels, Collections.singleton("THE_BLOOD_MOON_RISES"), 120, null, true);
		MaggotRun run = new MaggotRun(log, Collections.singletonList("Crimson kisten"), account, null, null, MaggotKing.Unique.ELDER_FANG);
		assertTrue(run.owns(MaggotKing.Unique.KISTEN));
		assertFalse(run.owns(MaggotKing.Unique.ELDER_FANG));
		// 340 stomachs at 1/340: 1 - (339/340)^340, about 63%.
		assertEquals("340 stomachs without one: 63% of players would have one by now", run.dryText(MaggotKing.Unique.ELDER_FANG));
		assertEquals("0 of 1.0 expected", run.luckText(MaggotKing.Unique.ELDER_FANG));
		assertEquals("1 in 340 a kill as you loot (always the stomach)", run.goalRateText());
		assertEquals("Even odds in about 9.8 hours, 9 in 10 in about 33 hours, at your 24.0 kills an hour.", run.etaText());
		assertEquals("Elder fang: 340 dry, 63% would have one", run.plaqueText());
		assertEquals("24 kills/hr", run.paceText());

		List<MaggotRun.Method> m = run.methods();
		assertEquals(3, m.get(0).met());   // 95 Magic missing
		assertEquals(4, m.get(1).met());
		assertTrue(run.extras().get(0).isMet());
		assertFalse(run.extras().get(1).isMet());

		// A player who only takes eggs never gets the fang.
		MaggotLog eggs = new MaggotLog();
		eggs.add(kill(MaggotKing.Choice.EGGS, 0, -1));
		MaggotRun r2 = new MaggotRun(eggs, null, account, null, null, MaggotKing.Unique.ELDER_FANG);
		assertTrue(r2.etaText().startsWith("Taking eggs never brings"));
		MaggotRun pet = new MaggotRun(eggs, null, account, null, null, MaggotKing.Unique.PET);
		assertTrue(pet.etaText().startsWith("Even odds within 1,042 kills"));
	}
}
