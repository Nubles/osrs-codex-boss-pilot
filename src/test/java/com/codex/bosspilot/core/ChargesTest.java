package com.codex.bosspilot.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.util.Text;
import org.junit.Test;

/**
 * The charged items: reading the game's check messages, carrying charges over kills, learning
 * the player's pace, and how long each lasts against the food and prayer.
 *
 * <p>The messages are the game's as logged by the Weapon Charges plugin (geheur/weapon-charges,
 * ChargedWeapon.java and WeaponChargesTest.java), tags and all.
 */
public class ChargesTest
{
	private static final double EPS = 1e-6;

	private static Charged.Reading read(String message)
	{
		return Charged.read(Text.removeTags(message));
	}

	@Test
	public void readsTheBlowpipeCheck()
	{
		Charged.Reading r = read("Darts: <col=007f00>Adamant dart x 16,383</col>. Scales: <col=007f00>16,383 (100.0%)</col>.");
		assertSame(Charged.TOXIC_BLOWPIPE, r.getItem());
		assertArrayEquals(new int[]{16383, 16383}, r.getValues());

		r = read("Darts: <col=007f00>None</col>. Scales: <col=007f00>99 (0.6%)</col>.");
		assertSame(Charged.TOXIC_BLOWPIPE, r.getItem());
		assertArrayEquals(new int[]{0, 99}, r.getValues());

		r = read("Darts: <col=007f00>Adamant dart x 16,383</col>. Scales: <col=007f00>0 (0.0%)</col>.");
		assertArrayEquals(new int[]{16383, 0}, r.getValues());
	}

	@Test
	public void readsTheHelmCheckAndRunningOut()
	{
		Charged.Reading r = read("Scales: <col=007f00>11,000 (100.0%)</col>");
		assertSame(Charged.SERPENTINE_HELM, r.getItem());
		assertArrayEquals(new int[]{11000}, r.getValues());
		r = read("Your serpentine helm has run out of Zulrah's scales.");
		assertTrue(r.isRanOut());
		assertArrayEquals(new int[]{0}, r.getValues());
	}

	@Test
	public void readsEachTridentByName()
	{
		assertSame(Charged.TRIDENT_SWAMP_E, read("Your Trident of the swamp (e) has 2,000 charges.").getItem());
		assertArrayEquals(new int[]{2000}, read("Your Trident of the swamp (e) has 2,000 charges.").getValues());
		assertSame(Charged.TRIDENT_SEAS_E, read("Your Trident of the seas (e) has one charge.").getItem());
		assertArrayEquals(new int[]{1}, read("Your Trident of the seas (e) has one charge.").getValues());
		assertSame(Charged.TRIDENT_SWAMP, read("Your Trident of the swamp has 6 charges.").getItem());
		assertSame(Charged.TRIDENT_SEAS, read("Your Trident of the seas has 100 charges.").getItem());
		// Uncharged ones say nothing to count.
		assertNull(read("Your Uncharged trident (e) has no charges."));
	}

	@Test
	public void theTridentWarningsSayNotWhichTrident()
	{
		Charged.Reading low = read("<col=ef1020>Your trident only has 100 charges left!</col>");
		assertTrue(low.isAnyTrident());
		assertArrayEquals(new int[]{100}, low.getValues());
		Charged.Reading out = read("<col=ef1020>Your trident has run out of charges.</col>");
		assertTrue(out.isAnyTrident());
		assertTrue(out.isRanOut());
		assertSame(Charged.TRIDENT_SWAMP, out.as(Charged.TRIDENT_SWAMP).getItem());
	}

	@Test
	public void readsTheStavesBowAndArmour()
	{
		assertSame(Charged.SANGUINESTI, read("Your Sanguinesti staff has 1,000 charges remaining.").getItem());
		assertSame(Charged.HOLY_SANGUINESTI, read("Your Holy sanguinesti staff has 144 charges remaining.").getItem());
		assertArrayEquals(new int[]{1}, read("Your Sanguinesti staff has 1 charges remaining.").getValues());
		assertTrue(read("Your Sanguinesti staff has run out of charges.").isRanOut());
		assertArrayEquals(new int[]{99}, read("Tumeken's shadow has 99 charges remaining.").getValues());
		assertArrayEquals(new int[]{100}, read("<col=e00a19>Tumeken's shadow only has 100 charges left!</col>").getValues());
		assertTrue(read("Tumeken's shadow has run out of charges.").isRanOut());
		assertArrayEquals(new int[]{180}, read("Your bow of Faerdhinen has 180 charges remaining.").getValues());
		assertArrayEquals(new int[]{100}, read("<col=ff0000>Your bow of Faerdhinen has 100 charges remaining.</col>").getValues());
		assertSame(Charged.CRYSTAL_HELM, read("Your crystal helm has 1,011 charges remaining.").getItem());
		assertSame(Charged.CRYSTAL_BODY, read("Your crystal body has 996 charges remaining.").getItem());
		assertSame(Charged.CRYSTAL_LEGS, read("Your crystal legs has 982 charges remaining.").getItem());
		assertNull(read("Your crystal halberd has 278 charges remaining."));
		assertNull(read("Your Zulrah kill count is: 52."));
	}

	@Test
	public void itemsMatchByWholeNameInAnyCase()
	{
		assertSame(Charged.TRIDENT_SEAS, Charged.byItem("Trident of the Seas"));
		assertSame(Charged.BOW_OF_FAERDHINEN, Charged.byItem("Bow of Faerdhinen"));
		// The corrupted bow and the ornament blowpipes are other items, not counted here.
		assertNull(Charged.byItem("Bow of Faerdhinen (c)"));
		assertNull(Charged.byItem("Blazing blowpipe"));
		assertEquals(Arrays.asList(Charged.TOXIC_BLOWPIPE, Charged.SERPENTINE_HELM),
			Charged.among(Arrays.asList("Serpentine helm", "Black d'hide body", "Toxic blowpipe")));
	}

	@Test
	public void aBlowpipeKillUsesTwoScalesInThreeShotsAndTheDartsAnAvasDeviceKeeps()
	{
		// 60 s at 2 ticks (1.2 s) a shot is 50 shots: 50 darts with no cape, 10 with an assembler (80% saved).
		double[] bare = Charged.TOXIC_BLOWPIPE.perKill(60, 2, "Rapid", Collections.emptyList());
		assertEquals(50, bare[0], EPS);
		assertEquals(50 * 2.0 / 3, bare[1], EPS);
		double[] assembler = Charged.TOXIC_BLOWPIPE.perKill(60, 2, "Rapid", Arrays.asList("Ava's assembler"));
		assertEquals(10, assembler[0], EPS);
		assertEquals(14, Charged.TOXIC_BLOWPIPE.perKill(60, 2, null, Arrays.asList("Ava's accumulator"))[0], EPS);
		// With no attack speed from a setup, the item's own: 3 ticks, 2 on rapid.
		assertEquals(60 / 1.8, Charged.TOXIC_BLOWPIPE.perKill(60, 0, "Accurate", Collections.emptyList())[0], EPS);
		assertEquals(50, Charged.TOXIC_BLOWPIPE.perKill(60, 0, "Rapid", Collections.emptyList())[0], EPS);
		// A trident cast a charge every 4 ticks.
		assertEquals(60 / 2.4, Charged.TRIDENT_SWAMP.perKill(60, 0, null, Collections.emptyList())[0], EPS);
	}

	@Test
	public void theHelmUsesTenScalesOnEnteringCombatAndTenEachNinetyTicks()
	{
		// 54 s is 90 ticks: 10 on entering combat and 10 for the 90 ticks.
		assertEquals(20, Charged.SERPENTINE_HELM.perKill(54, 0, null, Collections.emptyList())[0], EPS);
		assertNull(Charged.CRYSTAL_BODY.perKill(54, 0, null, Collections.emptyList()));
		assertEquals(7, Charged.CRYSTAL_BODY.perKillFromHits(7)[0], EPS);
	}

	@Test
	public void killsCarryTheChargesForwardAndTheNextCheckTeachesThePace()
	{
		ChargeBook book = new ChargeBook();
		book.read(Charged.TOXIC_BLOWPIPE, new int[]{300, 2000}, 1);
		book.kill("zulrah", Charged.TOXIC_BLOWPIPE, new double[]{50, 100});
		book.kill("zulrah", Charged.TOXIC_BLOWPIPE, new double[]{50, 100});
		ChargeBook.Estimate e = book.estimate(Charged.TOXIC_BLOWPIPE);
		assertFalse(e.isExact());
		assertEquals(2, e.getKills());
		assertEquals(200, e.getLeft()[0], EPS);
		assertEquals(1800, e.getLeft()[1], EPS);

		// Really 80 darts and 200 scales a kill.
		book.read(Charged.TOXIC_BLOWPIPE, new int[]{140, 1600}, 2);
		assertArrayEquals(new double[]{80, 200}, book.pace("zulrah", Charged.TOXIC_BLOWPIPE), EPS);
		assertTrue(book.estimate(Charged.TOXIC_BLOWPIPE).isExact());

		// The next check moves the pace half way to what it saw.
		book.kill("zulrah", Charged.TOXIC_BLOWPIPE, new double[]{80, 200});
		book.read(Charged.TOXIC_BLOWPIPE, new int[]{40, 1500}, 3);
		assertArrayEquals(new double[]{90, 150}, book.pace("zulrah", Charged.TOXIC_BLOWPIPE), EPS);
		assertNull(book.pace("vorkath", Charged.TOXIC_BLOWPIPE));
	}

	@Test
	public void noPaceIsLearntAcrossATopUpTwoBossesOrRunningDry()
	{
		ChargeBook book = new ChargeBook();
		book.read(Charged.TRIDENT_SWAMP, new int[]{500}, 1);
		book.kill("zulrah", Charged.TRIDENT_SWAMP, new double[]{30});
		book.read(Charged.TRIDENT_SWAMP, new int[]{2500}, 2);
		assertNull("topped up", book.pace("zulrah", Charged.TRIDENT_SWAMP));

		book.kill("zulrah", Charged.TRIDENT_SWAMP, new double[]{30});
		book.kill("kraken", Charged.TRIDENT_SWAMP, new double[]{30});
		book.read(Charged.TRIDENT_SWAMP, new int[]{2400}, 3);
		assertNull("two bosses", book.pace("zulrah", Charged.TRIDENT_SWAMP));
		assertNull(book.pace("kraken", Charged.TRIDENT_SWAMP));

		book.kill("kraken", Charged.TRIDENT_SWAMP, new double[]{30});
		book.read(Charged.TRIDENT_SWAMP, new int[]{0}, 4);
		assertNull("ran dry part way", book.pace("kraken", Charged.TRIDENT_SWAMP));

		// A kill whose use is not known leaves the estimate short of it, and teaches nothing.
		book.read(Charged.TRIDENT_SWAMP, new int[]{100}, 5);
		book.kill("kraken", Charged.TRIDENT_SWAMP, null);
		assertEquals(1, book.estimate(Charged.TRIDENT_SWAMP).getBlind());
		book.read(Charged.TRIDENT_SWAMP, new int[]{70}, 6);
		assertNull(book.pace("kraken", Charged.TRIDENT_SWAMP));
	}

	@Test
	public void theBookIsSavedAndReadBack()
	{
		Gson gson = new Gson();
		ChargeBook book = new ChargeBook();
		book.read(Charged.SANGUINESTI, new int[]{12000}, 5);
		book.kill("vorkath", Charged.SANGUINESTI, new double[]{40});
		book.read(Charged.SANGUINESTI, new int[]{11950}, 6);
		ChargeBook back = ChargeBook.parse(gson, book.toJson(gson));
		assertArrayEquals(new double[]{50}, back.pace("vorkath", Charged.SANGUINESTI), EPS);
		assertEquals(11950, back.estimate(Charged.SANGUINESTI).getLeft()[0], EPS);
		assertNotNull(ChargeBook.parse(gson, "not json"));
		assertNotNull(ChargeBook.parse(gson, null));
	}

	private static Setup blowpipeSetup(double killSeconds)
	{
		return new Setup(null, "Ranged", 5, killSeconds, 20, "Rapid", "Eagle Eye", null,
			Arrays.asList("Toxic blowpipe", "Ava's assembler", "Black d'hide body"), 2);
	}

	@Test
	public void eachItemSaysHowManyKillsItLastsAgainstTheFoodAndPrayer()
	{
		ChargeBook book = new ChargeBook();
		List<Setup> setups = Collections.singletonList(blowpipeSetup(60));
		List<Charged> items = ChargeCheck.inSetups(setups);
		assertEquals(Collections.singletonList(Charged.TOXIC_BLOWPIPE), items);

		// Never checked.
		ChargeCheck.Line l = ChargeCheck.of(items, book, "zulrah", setups, -1, -1, 6).get(0);
		assertEquals(ChargeCheck.Status.UNCHECKED, l.getStatus());
		assertEquals("check it", l.killsText());

		// 30 darts at 10 a kill (assembler) is 3 kills, short of the 6 the food lasts: darts run out first.
		book.read(Charged.TOXIC_BLOWPIPE, new int[]{30, 2000}, 1);
		l = ChargeCheck.of(items, book, "zulrah", setups, -1, -1, 6).get(0);
		assertEquals(ChargeCheck.Status.SHORT, l.getStatus());
		assertEquals(ChargeCheck.PaceFrom.SETUP, l.getFrom());
		assertEquals(3, l.getKills(), EPS);
		assertEquals("darts", l.limitName());
		assertEquals("Toxic blowpipe darts", l.what());
		assertEquals("~3 kills", l.killsText());
		assertEquals("30 darts, 2,000 scales", l.holdsText());
		assertTrue(l.getStatus().needsRecharge());

		// Enough for a trip of 2.
		assertEquals(ChargeCheck.Status.ENOUGH, ChargeCheck.of(items, book, "zulrah", setups, -1, -1, 2).get(0).getStatus());
		// Not enough for one kill.
		book.read(Charged.TOXIC_BLOWPIPE, new int[]{5, 2000}, 2);
		assertEquals(ChargeCheck.Status.EMPTY, ChargeCheck.of(items, book, "zulrah", setups, -1, -1, 6).get(0).getStatus());
		book.read(Charged.TOXIC_BLOWPIPE, new int[]{0, 2000}, 3);
		l = ChargeCheck.of(items, book, "zulrah", setups, -1, -1, 6).get(0);
		assertEquals(ChargeCheck.Status.EMPTY, l.getStatus());
		assertEquals("empty", l.killsText());
	}

	@Test
	public void crystalArmourGoesByTheHitsTakenThisTrip()
	{
		ChargeBook book = new ChargeBook();
		book.read(Charged.CRYSTAL_BODY, new int[]{200}, 1);
		List<Charged> body = Collections.singletonList(Charged.CRYSTAL_BODY);
		assertEquals(ChargeCheck.Status.NO_PACE, ChargeCheck.of(body, book, "vorkath", Collections.emptyList(), -1, -1, 5).get(0).getStatus());
		ChargeCheck.Line l = ChargeCheck.of(body, book, "vorkath", Collections.emptyList(), 25, -1, 5).get(0);
		assertEquals(ChargeCheck.PaceFrom.TRIP, l.getFrom());
		assertEquals(8, l.getKills(), EPS);
		assertEquals(ChargeCheck.Status.ENOUGH, l.getStatus());
	}

	@Test
	public void theKitLastsAsLongAsItsFoodAndPrayerAtYourPastPace()
	{
		// Zulrah's kit: 300 hp of food and 16 doses of prayer.
		Logbook.Rates past = new Logbook.Rates(2, 10, 30, 0, 500, 40);
		// 50 hp a kill is 6 kills of food; 4 doses a kill is 4 kills of prayer.
		assertEquals(4, ChargeCheck.kitKills(Bosses.ZULRAH, past), EPS);
		assertEquals(-1, ChargeCheck.kitKills(Bosses.ZULRAH, null), EPS);
	}

	@Test
	public void theTripOutlookTurnsOnWhatRunsOutFirst()
	{
		ChargeBook book = new ChargeBook();
		book.read(Charged.TOXIC_BLOWPIPE, new int[]{15, 2000}, 1);
		ChargeCheck.Line l = ChargeCheck.of(Collections.singletonList(Charged.TOXIC_BLOWPIPE), book, "zulrah",
			Collections.singletonList(blowpipeSetup(60)), -1, -1, 5).get(0);
		Outlook none = Outlook.UNKNOWN.withCharges(l);
		assertTrue(none.isCharges());
		assertEquals("Toxic blowpipe darts for about 1 more kill", none.text());
		assertEquals("Toxic blowpipe darts: 1 more kill", none.shortText());
		assertSame(Outlook.UNKNOWN, Outlook.UNKNOWN.withCharges(null));

		book.read(Charged.TOXIC_BLOWPIPE, new int[]{5, 2000}, 2);
		l = ChargeCheck.of(Collections.singletonList(Charged.TOXIC_BLOWPIPE), book, "zulrah",
			Collections.singletonList(blowpipeSetup(60)), -1, -1, 5).get(0);
		assertEquals("Bank after this kill: your Toxic blowpipe darts are nearly gone", Outlook.UNKNOWN.withCharges(l).text());
	}

	@Test
	public void aTripThatRanOutOfChargesSaysSoAndWhat()
	{
		TripTracker t = new TripTracker();
		t.observe(Bosses.ZULRAH, 300, 16, 20, 0);
		t.kill(Bosses.ZULRAH, 60_000);
		t.observe(Bosses.ZULRAH, 250, 12, 20, 60_000);
		t.ranOut("Toxic blowpipe darts");
		t.observe(Bosses.ZULRAH, 250, 12, 20, 70_000);
		t.end();
		Logbook.TripRecord r = t.takeFinished().record();
		assertEquals("OUT_OF_CHARGES", r.ended);
		assertEquals("Toxic blowpipe darts", r.outOf);
		assertEquals("Your Toxic blowpipe darts ran out after 1 kill, with food and prayer for about 3 more kills.",
			Debrief.reason(r, Debrief.Ending.OUT_OF_CHARGES));
		assertTrue(Debrief.Ending.OUT_OF_CHARGES.isForced());

		// Topped up again on the trip: it did not end the trip.
		TripTracker again = new TripTracker();
		again.observe(Bosses.ZULRAH, 300, 16, 20, 0);
		again.ranOut("Toxic blowpipe darts");
		again.ranOut(null);
		again.kill(Bosses.ZULRAH, 60_000);
		again.end();
		assertFalse("OUT_OF_CHARGES".equals(again.takeFinished().record().ended));
	}
}
