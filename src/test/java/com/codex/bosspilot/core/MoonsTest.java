package com.codex.bosspilot.core;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The Moons of Peril against the OSRS Wiki's Lunar Chest page: "a non-stackable 1/56 chance at a
 * piece of their equipment" for each moon, every unique at "1/224", "1 Moon gives 1 roll", "2
 * Moons give 3 rolls", "3 Moons give 6 rolls", all three moons "1/19" for a unique, no duplicates
 * "until each piece in the set has been received at least once"; the ids against RuneLite's
 * gameval; and the chest log's sums.
 */
public class MoonsTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void ratesMatchTheWiki()
	{
		assertEquals(1 / 56.0, Moons.SET_CHANCE, 1e-15);
		assertEquals(Moons.SET_CHANCE / 4, Moons.PIECE_CHANCE, 1e-15);
		assertEquals(1, Moons.rolls(1));
		assertEquals(3, Moons.rolls(2));
		assertEquals(6, Moons.rolls(3));
		assertEquals(0, Moons.rolls(0));
		assertEquals("1 in 19", Moons.oneIn(Moons.chanceAny(3)));
		assertEquals("1 in 56", Moons.oneIn(Moons.chanceAny(1)));
		assertEquals(320, Moons.CHESTS_FOR_ALL_SETS);
	}

	@Test
	public void idsAreRuneLitesGamevals()
	{
		assertEquals(ItemID.DUAL_MACUAHUITL, Moons.piece("Dual macuahuitl").getItemId());
		assertEquals(ItemID.BLOOD_MOON_HELM, Moons.piece("Blood Moon helm").getItemId());
		assertEquals(ItemID.BLOOD_MOON_CHESTPLATE, Moons.piece("Blood Moon chestplate").getItemId());
		assertEquals(ItemID.BLOOD_MOON_TASSETS, Moons.piece("Blood Moon tassets").getItemId());
		assertEquals(ItemID.FROSTMOON_SPEAR, Moons.piece("Blue Moon spear").getItemId());
		assertEquals(ItemID.FROST_MOON_HELM, Moons.piece("Blue Moon helm").getItemId());
		assertEquals(ItemID.FROST_MOON_CHESTPLATE, Moons.piece("Blue Moon chestplate").getItemId());
		assertEquals(ItemID.FROST_MOON_TASSETS, Moons.piece("Blue Moon tassets").getItemId());
		assertEquals(ItemID.ECLIPSE_ATLATL, Moons.piece("Eclipse atlatl").getItemId());
		assertEquals(ItemID.ECLIPSE_MOON_HELM, Moons.piece("Eclipse Moon helm").getItemId());
		assertEquals(ItemID.ECLIPSE_MOON_CHESTPLATE, Moons.piece("Eclipse Moon chestplate").getItemId());
		assertEquals(ItemID.ECLIPSE_MOON_TASSETS, Moons.piece("Eclipse Moon tassets").getItemId());
		assertEquals(NpcID.PMOON_BOSS_BLOOD_MOON_VIS, Moons.Moon.BLOOD.getNpcId());
		assertEquals(NpcID.PMOON_BOSS_BLUE_MOON_VIS, Moons.Moon.BLUE.getNpcId());
		assertEquals(NpcID.PMOON_BOSS_ECLIPSE_MOON_VIS, Moons.Moon.ECLIPSE.getNpcId());
		assertEquals(VarbitID.PMOON_BOSS_BLOOD_DEAD, Moons.Moon.BLOOD.getDeadVarbit());
		assertEquals(VarbitID.PMOON_BOSS_BLUE_DEAD, Moons.Moon.BLUE.getDeadVarbit());
		assertEquals(VarbitID.PMOON_BOSS_ECLIPSE_DEAD, Moons.Moon.ECLIPSE.getDeadVarbit());
		assertEquals(ObjectID.CHEST_SUN01_DEFAULT01, Moons.CHEST);
		assertEquals(ObjectID.CHEST_SUN01_OPEN01, Moons.CHEST_OPEN);
		assertEquals(868, InterfaceID.PMOON_REWARD);
		assertEquals(847, net.runelite.api.gameval.InventoryID.PMOON_REWARDINV);
	}

	@Test
	public void twelvePiecesThreeSets()
	{
		int n = 0;
		for (Moons.Moon m : Moons.Moon.values())
		{
			assertEquals(4, m.getPieces().size());
			for (Moons.Piece p : m.getPieces())
			{
				assertSame(m, Moons.moonOf(p.getName()));
				n++;
			}
		}
		assertEquals(12, n);
		assertSame(Moons.Moon.BLUE, Moons.Moon.byName("blue moon"));
		assertNull(Moons.Moon.byName("Lunar Chest"));
		assertEquals(Arrays.asList("Eclipse atlatl"), Moons.pieces(Arrays.asList("96 x Atlatl dart", "Eclipse atlatl", "Eclipse atlatl")));
	}

	@Test
	public void newPieceOddsFollowDuplicateProtection()
	{
		List<String> missing = Moons.missing(Collections.emptyList());
		assertEquals(12, missing.size());
		assertEquals(1 / 56.0, Moons.chanceNew(EnumSet.of(Moons.Moon.BLOOD), missing), 1e-12);
		// A finished set gives nothing new, so only the other two moons count.
		List<String> blueDone = Moons.missing(Arrays.asList("Blue Moon spear", "Blue Moon helm", "Blue Moon chestplate", "Blue Moon tassets"));
		assertEquals(0, Moons.missingOf(Moons.Moon.BLUE, blueDone));
		assertEquals(1 - Math.pow(55 / 56.0, 2), Moons.chanceNew(EnumSet.allOf(Moons.Moon.class), blueDone), 1e-12);
		assertEquals(7, Moons.mask(EnumSet.allOf(Moons.Moon.class)));
		assertEquals(EnumSet.of(Moons.Moon.BLOOD, Moons.Moon.ECLIPSE), Moons.fromMask(5));
	}

	@Test
	public void chestLogSums()
	{
		LunarLog log = new LunarLog();
		log.add(chest(12, 300_000, 7, 900, Collections.emptyList()));
		log.add(chest(13, 200_000, 3, 600, Collections.emptyList()));
		log.add(chest(14, 1_000_000, 1, 300, Collections.singletonList("Blood Moon helm")));
		assertEquals(3, log.chests);
		assertEquals(500_000, log.average());
		assertEquals(3 * 3600.0 / 1800, log.chestsPerHour(), 1e-9);
		assertEquals(1_500_000 * 3600.0 / 1800, log.valuePerHour(), 1e-6);
		assertEquals(2.0, log.moonsPerRun(), 1e-9);
		assertEquals(600, log.averageRun(), 1e-9);
		assertEquals(3, log.kills(Moons.Moon.BLOOD));
		assertEquals(0, log.since(Moons.Moon.BLOOD));
		assertEquals(2, log.since(Moons.Moon.BLUE));
		assertEquals(1, log.since(Moons.Moon.ECLIPSE));
		assertEquals(1, log.found("Blood Moon helm"));
		assertEquals(11, log.missing(Collections.emptyList()).size());
		assertEquals(10, log.missing(Arrays.asList("Eclipse atlatl", "Shark")).size());

		LunarLog back = LunarLog.parse(GSON, log.toJson(GSON));
		assertEquals(3, back.chests);
		assertEquals(2, back.since(Moons.Moon.BLUE));
		assertEquals("Blood Moon helm", back.last().pieces.get(0));
		assertTrue(LunarLog.parse(GSON, "not json").isEmpty());
		assertTrue(LunarLog.parse(GSON, null).isEmpty());
	}

	@Test
	public void aLongBreakIsNotARun()
	{
		LunarLog log = new LunarLog();
		log.add(chest(1, 100_000, 7, LunarLog.PACE_GAP_SECONDS + 1, Collections.emptyList()));
		assertEquals(-1, log.valuePerHour(), 0);
		assertEquals(100_000, log.average());
		log.lateKc(5);
		assertEquals(1, log.last().kc);
		LunarLog.Chest c = chest(-1, 1, -1, -1, Collections.emptyList());
		log.add(c);
		log.lateKc(9);
		assertEquals(9, log.last().kc);
		assertEquals(1, log.moonsSeen);
	}

	@Test
	public void suppliesCountABiteAtATime()
	{
		RunSupplies s = new RunSupplies();
		Map<String, Long> carried = new HashMap<>();
		carried.put("Cooked bream", 6L);
		carried.put("Moonlight potion(4)", 2L);
		carried.put("Games necklace(4)", 1L);
		s.observe(carried, false);
		carried.put("Cooked bream", 5L);
		s.observe(carried, false);
		carried.put("Moonlight potion(4)", 1L);
		carried.put("Moonlight potion(3)", 1L);
		carried.remove("Games necklace(4)");
		carried.put("Games necklace(3)", 1L);
		s.observe(carried, false);
		assertEquals(Long.valueOf(1), s.used().get("Cooked bream"));
		assertEquals(Long.valueOf(1), s.used().get("Moonlight potion"));
		assertFalse(s.used().containsKey("Games necklace"));
		// A deposit at the bank, or everything gone at once, is not eaten.
		carried.put("Cooked bream", 4L);
		s.observe(carried, true);
		carried.clear();
		s.observe(carried, false);
		assertEquals(Long.valueOf(1), s.used().get("Cooked bream"));
		s.nextRun();
		assertTrue(s.used().isEmpty());
	}

	@Test
	public void runTexts()
	{
		LunarLog log = new LunarLog();
		LunarRun none = new LunarRun(EnumSet.noneOf(Moons.Moon.class), Collections.emptyMap(), log, Moons.missing(Collections.emptyList()));
		assertEquals("No moon subdued yet this run", none.moonsText());
		assertNull(none.chestText());
		assertNull(none.dryText(Moons.Moon.BLOOD));
		Map<String, Long> used = new HashMap<>();
		used.put("Cooked bream", 4L);
		used.put("Moonlight potion", 1L);
		LunarRun two = new LunarRun(EnumSet.of(Moons.Moon.BLOOD, Moons.Moon.ECLIPSE), used, log, Moons.missing(Collections.emptyList()));
		assertEquals("2 of 3 moons: Blood, Eclipse", two.moonsText());
		assertEquals("Chest now: 3 rolls, 1 in 28 for a new piece", two.chestText());
		assertEquals("Used this run: 4 cooked bream, 1 dose of moonlight potion", two.usedText());

		for (int i = 0; i < 56; i++)
		{
			log.add(chest(i, 50_000, 1, 400, Collections.emptyList()));
		}
		LunarRun dry = new LunarRun(EnumSet.noneOf(Moons.Moon.class), Collections.emptyMap(), log, Moons.missing(Collections.emptyList()));
		// 1 - (55/56)^56 is 63.5%, shown rounded.
		assertEquals("56 kills without a piece: 64% would have one by now", dry.dryText(Moons.Moon.BLOOD));
		assertEquals(1.0, dry.spell(Moons.Moon.BLOOD), 1e-12);
		assertNull(dry.dryText(Moons.Moon.BLUE));
	}

	@Test
	public void cavernFoodIsCountedInBites()
	{
		// Cooked bream and cooked moss lizard heal by the player's levels, so Food knows no fixed heal for them.
		assertEquals(0, Food.heal("Cooked bream"));
		assertEquals(0, Food.heal("Cooked moss lizard"));
		Map<String, Long> used = new HashMap<>();
		used.put("Cooked bream", 4L);
		used.put("Shark", 1L);
		used.put("Moonlight potion", 3L);
		assertEquals("5 bites", LunarRun.foodTileText(used));
		used.clear();
		used.put("Cooked moss lizard", 1L);
		assertEquals("1 bite", LunarRun.foodTileText(used));
		// Only food with a known heal, or only potions: the tile keeps showing hitpoints.
		used.clear();
		used.put("Shark", 3L);
		used.put("Saradomin brew", 2L);
		assertNull(LunarRun.foodTileText(used));
		assertNull(LunarRun.foodTileText(Collections.emptyMap()));

		RunSupplies s = new RunSupplies();
		Map<String, Long> carried = new HashMap<>();
		carried.put("Cooked bream", 3L);
		s.observe(carried, false);
		carried.put("Cooked bream", 2L);
		s.observe(carried, false);
		s.nextRun();
		carried.put("Cooked bream", 1L);
		s.observe(carried, false);
		assertEquals(Long.valueOf(1), s.used().get("Cooked bream"));
		assertEquals(Long.valueOf(2), s.tripUsed().get("Cooked bream"));
		LunarRun r = new LunarRun(null, s.used(), new LunarLog(), null).withTripUsed(s.tripUsed());
		assertEquals("2 bites", r.foodTileText());
		s.clear();
		assertTrue(s.tripUsed().isEmpty());
	}

	@Test
	public void chatReadsTheChestCount()
	{
		Chat.Kc kc = Chat.kc("Your Lunar Chest count is: 1,204.");
		assertEquals("Lunar Chest", kc.name);
		assertEquals(1204, kc.count);
		assertSame(Bosses.MOONS, Bosses.byKc(kc.name));
		Chat.Kc moon = Chat.kc("Your Eclipse Moon kill count is: 40.");
		assertSame(Moons.Moon.ECLIPSE, Moons.Moon.byName(moon.name));
		for (String npc : Bosses.MOONS.getNpcNames())
		{
			assertSame(Bosses.MOONS, Bosses.byNpc(npc));
		}
		assertTrue(Bosses.MOONS.getQuests().contains("PERILOUS_MOONS"));
	}

	private static LunarLog.Chest chest(int kc, long value, int moons, long seconds, List<String> pieces)
	{
		LunarLog.Chest c = new LunarLog.Chest();
		c.kc = kc;
		c.value = value;
		c.moons = moons;
		c.seconds = seconds;
		c.pieces.addAll(pieces);
		return c;
	}
}
