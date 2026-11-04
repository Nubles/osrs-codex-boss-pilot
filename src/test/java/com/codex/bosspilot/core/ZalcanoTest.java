package com.codex.bosspilot.core;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Zalcano against the OSRS Wiki: "a 1/200 chance of rolling the crystal tool seed table" with
 * onyx 1/40 of it (so 39/8,000 for the seed and 1/8,000 for the onyx), the Zalcano shard from
 * 1/1,500 to 1/750 by contribution, Smolcano "a static 1/2,250"; the requirements (Song of the
 * Elves, 70 Mining and Smithing); and the kill log's sums.
 */
public class ZalcanoTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void ratesMatchTheWiki()
	{
		assertEquals(1 / 200.0 * 39 / 40.0, Zalcano.Unique.TOOL_SEED.getHigh(), 1e-15);
		assertEquals(1 / 200.0 * 1 / 40.0, Zalcano.Unique.ONYX.getHigh(), 1e-15);
		assertEquals("1 in 205", Zalcano.Unique.TOOL_SEED.rate());
		assertEquals("1 in 1,500 to 1 in 750", Zalcano.Unique.SHARD.rate());
		assertEquals("1/1,500 to 1/750", Zalcano.Unique.SHARD.shortRate());
		assertEquals("1 in 2,250", Zalcano.Unique.PET.rate());
		assertEquals("1 in 8,000", Zalcano.Unique.ONYX.rate());
		assertTrue(Zalcano.Unique.SHARD.ranged());
		assertFalse(Zalcano.Unique.PET.ranged());
	}

	@Test
	public void idsAreRuneLitesGamevals()
	{
		assertEquals(ItemID.PRIF_TOOL_SEED, Zalcano.Unique.TOOL_SEED.getItemId());
		assertEquals(ItemID.ZALCANO_PICKAXE_KIT, Zalcano.Unique.SHARD.getItemId());
		assertEquals(ItemID.ZALCANOPET, Zalcano.Unique.PET.getItemId());
		assertEquals(ItemID.UNCUT_ONYX, Zalcano.Unique.ONYX.getItemId());
		assertEquals(Zalcano.Unique.PET, Zalcano.Unique.byId(ItemID.ZALCANOPET));
		assertEquals(Zalcano.Unique.SHARD, Zalcano.Unique.byName("Zalcano shard"));
		assertNull(Zalcano.Unique.byName("Crystal shard"));
	}

	@Test
	public void bossNeedsTheQuestAndTheLevels()
	{
		Boss z = Bosses.ZALCANO;
		assertEquals(Collections.singletonList("SONG_OF_THE_ELVES"), z.getQuests());
		assertEquals(Integer.valueOf(70), z.getSkills().get("Mining"));
		assertEquals(Integer.valueOf(70), z.getSkills().get("Smithing"));
		assertEquals(z, Bosses.byKc("Zalcano"));
		assertEquals(z, Bosses.byNpc("Zalcano"));
		java.util.Map<String, Integer> levels = new java.util.HashMap<>();
		levels.put("Mining", 70);
		levels.put("Smithing", 69);
		Account a = Account.of(levels, Collections.singleton("SONG_OF_THE_ELVES"), 90, null, true);
		Readiness r = Readiness.of(z, a, null);
		assertEquals(Readiness.Status.LOCKED, r.getStatus());
		assertEquals(Collections.singletonList("70 Smithing (you have 69)"), r.getMissing());
		levels.put("Smithing", 70);
		assertEquals(Readiness.Status.READY, Readiness.of(z, Account.of(levels, Collections.singleton("SONG_OF_THE_ELVES"), 3, null, true), null)
			.getStatus());
		assertTrue(Readiness.of(z, Account.of(levels, Collections.emptySet(), 90, null, true), null).getMissing()
			.contains("Quest: Song of the Elves"));
	}

	@Test
	public void kitWantsAPickaxeAndTakesAnyTier()
	{
		Supply pick = Bosses.ZALCANO.getKit().get(0);
		assertEquals(Supply.Need.MUST, pick.getNeed());
		for (String name : new String[]{"Bronze pickaxe", "Iron pickaxe (nz)", "Rune pickaxe", "Dragon pickaxe (or)", "Crystal pickaxe", "3rd Age pickaxe"})
		{
			assertTrue(name, pick.worth(name, 1) > 0);
		}
		assertEquals(0, pick.worth("Pickaxe handle", 1));
	}

	@Test
	public void dryOddsAreOneMinusTheChanceToTheKills()
	{
		assertEquals(1, Zalcano.dry(0.01, 0), 0);
		assertEquals(Math.pow(1 - 39 / 8000.0, 205), Zalcano.dry(39 / 8000.0, 205), 1e-12);
		// About 63% have a 1/n drop by n kills.
		assertEquals(0.632, Zalcano.atLeastOne(1 / 2250.0, 2250), 0.001);
		assertEquals("under 1%", Zalcano.percent(0.004));
		assertEquals("63%", Zalcano.percent(0.632));
		assertEquals("over 99%", Zalcano.percent(0.996));
	}

	@Test
	public void prifddinasRegionsHoldTheTeleportCrystalsLanding()
	{
		// Wayfinder's teleport crystal lands at 3264 6066; the prison is elsewhere.
		assertTrue(Zalcano.inPrifddinas(Zalcano.region(3264, 6066)));
		assertFalse(Zalcano.inPrifddinas(Zalcano.region(3222, 3218)));
		assertFalse(Zalcano.inPrifddinas(Zalcano.PRISON_REGION));
	}

	@Test
	public void lootAroundTheKillMessageMakesOneKill()
	{
		ZalcanoLoot loot = new ZalcanoLoot();
		loot.item(8, "Old pickings", 1, 5);          // too early: another moment
		loot.item(99, "Crystal shard", 2, 20_000);    // a tick before the message
		loot.kill(100, 412);
		loot.item(101, "Uncut onyx", 1, 2_000_000);
		loot.item(102, "Crystal shard", 1, 10_000);
		assertNull(loot.poll(102, 0, 30));
		ZalcanoLog.Kill k = loot.poll(104, 5_000, 120);
		assertNotNull(k);
		assertEquals(412, k.kc);
		assertEquals(2_030_000, k.value);
		assertEquals(120, k.seconds);
		assertEquals(Arrays.asList("3 x Crystal shard", "Uncut onyx"), k.items);
		assertEquals(Collections.singletonList("Uncut onyx"), k.uniques);
		assertFalse(loot.pending());
	}

	@Test
	public void killWithoutLootIsStillAKill()
	{
		ZalcanoLoot loot = new ZalcanoLoot();
		loot.kill(10, 3);
		ZalcanoLog.Kill k = loot.poll(20, 0, -1);
		assertEquals(-1, k.value);
		assertTrue(k.items.isEmpty());
	}

	@Test
	public void petMessageJoinsTheKillEitherSide()
	{
		ZalcanoLoot before = new ZalcanoLoot();
		before.pet(50);
		before.kill(51, 9);
		assertEquals(Collections.singletonList("Smolcano"), before.poll(60, 0, -1).uniques);

		ZalcanoLoot after = new ZalcanoLoot();
		after.kill(5, 9);
		after.pet(6);
		assertEquals(Collections.singletonList("Smolcano"), after.poll(10, 0, -1).uniques);

		ZalcanoLoot stale = new ZalcanoLoot();
		stale.pet(1);
		stale.poll(20, 0, -1);
		stale.kill(30, 9);
		assertTrue(stale.poll(40, 0, -1).uniques.isEmpty());

		ZalcanoLoot early = new ZalcanoLoot();
		early.kill(1, 1);
		assertTrue("no pet message, no pet", early.poll(10, 0, -1).uniques.isEmpty());
	}

	@Test
	public void logKeepsPaceDryStreaksAndSaves()
	{
		ZalcanoLog log = new ZalcanoLog();
		log.add(kill(-1, 100_000));           // the first of a trip: not timed
		log.add(kill(120, 140_000));
		log.add(kill(60 * 60, 50_000));        // past the break gap: not timed
		log.add(kill(180, 160_000, "Crystal tool seed"));
		assertEquals(4, log.kills);
		assertEquals(2, log.timed);
		assertEquals(2 * 3600.0 / 300, log.killsPerHour(), 1e-9);
		assertEquals(300_000 * 3600.0 / 300, log.lootPerHour(), 1e-9);
		assertEquals(112_500, log.average());
		assertEquals(0, log.since(Zalcano.Unique.TOOL_SEED));
		assertEquals(4, log.since(Zalcano.Unique.PET));
		assertEquals(0, log.sinceAny());
		log.add(kill(150, 90_000));
		assertEquals(1, log.sinceAny());
		log.late("Smolcano");
		assertEquals(0, log.since(Zalcano.Unique.PET));
		assertEquals(1, log.found(Zalcano.Unique.PET));
		log.late("Smolcano");
		assertEquals("the same kill twice is one pet", 1, log.found(Zalcano.Unique.PET));

		ZalcanoLog back = ZalcanoLog.parse(GSON, log.toJson(GSON));
		assertEquals(log.kills, back.kills);
		assertEquals(log.killsPerHour(), back.killsPerHour(), 1e-9);
		assertEquals(1, back.found(Zalcano.Unique.TOOL_SEED));
		assertEquals(Arrays.asList("Smolcano"), back.last().uniques);
		assertTrue(ZalcanoLog.parse(GSON, "{not json").isEmpty());
		assertTrue(ZalcanoLog.parse(GSON, null).isEmpty());
	}

	@Test
	public void runTextsReadWell()
	{
		ZalcanoLog log = new ZalcanoLog();
		ZalcanoRun empty = new ZalcanoRun(log, Collections.emptyList(), null);
		assertNull(empty.dryText(Zalcano.Unique.PET));
		assertEquals("Your first logged kill is next", empty.plaqueText());
		assertNull(empty.paceText());
		for (int i = 0; i < 205; i++)
		{
			log.add(kill(i == 0 ? -1 : 150, 100_000));
		}
		ZalcanoRun r = new ZalcanoRun(log, Arrays.asList("Smolcano", "Shark"), WayIn.none());
		assertTrue(r.owns(Zalcano.Unique.PET));
		assertFalse(r.owns(Zalcano.Unique.TOOL_SEED));
		assertEquals("205 kills without one: 63% would have one by now", r.dryText(Zalcano.Unique.TOOL_SEED));
		assertEquals("205 kills without one: 13% to 24% would have one by now", r.dryText(Zalcano.Unique.SHARD));
		assertEquals(1.0, r.spell(Zalcano.Unique.TOOL_SEED), 0.01);
		assertEquals("205 kills since a rare drop", r.plaqueText());
		assertEquals("24 kills/hr", r.paceText());
	}

	@Test
	public void wayfinderAtlasGivesTheWaysIntoPrifddinas()
	{
		String json = "{\"version\":1,\"teleports\":["
			+ "{\"name\":\"Varrock Teleport\",\"x\":3213,\"y\":3424,\"plane\":0,\"seconds\":4,\"carried\":true},"
			+ "{\"name\":\"Spirit tree: Prifddinas\",\"x\":3274,\"y\":6123,\"plane\":0,\"seconds\":12,\"carried\":false},"
			+ "{\"name\":\"Teleport crystal: Prifddinas\",\"x\":3264,\"y\":6066,\"plane\":0,\"seconds\":4,\"carried\":false},"
			+ "{\"name\":\"Prifddinas tablet\",\"x\":3239,\"y\":6076,\"plane\":0,\"seconds\":4,\"carried\":true}]}";
		WayIn w = WayIn.parse(GSON, json);
		assertTrue(w.isRead());
		assertEquals(3, w.getTeleports().size());
		assertEquals("Prifddinas tablet", w.getTeleports().get(0).getName());
		assertEquals("Teleport crystal: Prifddinas", w.getTeleports().get(1).getName());
		assertFalse(WayIn.parse(GSON, "{\"version\":2,\"teleports\":[]}").isRead());
		assertFalse(WayIn.parse(GSON, "nonsense{").isRead());
		assertFalse(WayIn.parse(GSON, null).isRead());
	}

	private static ZalcanoLog.Kill kill(long seconds, long value, String... uniques)
	{
		ZalcanoLog.Kill k = new ZalcanoLog.Kill();
		k.seconds = seconds;
		k.value = value;
		k.uniques.addAll(Arrays.asList(uniques));
		return k;
	}
}
