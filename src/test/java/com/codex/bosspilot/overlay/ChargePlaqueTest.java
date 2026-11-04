package com.codex.bosspilot.overlay;

import static org.junit.Assert.assertEquals;

import com.codex.bosspilot.core.Bosses;
import com.codex.bosspilot.core.ChargeBook;
import com.codex.bosspilot.core.ChargeCheck;
import com.codex.bosspilot.core.Charged;
import com.codex.bosspilot.core.KitCheck;
import com.codex.bosspilot.core.Setup;
import com.codex.bosspilot.core.Stock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** The kit plaque at the bank with charged gear to top up or check. */
public class ChargePlaqueTest
{
	private static Setup blowpipeSetup(double killSeconds)
	{
		return new Setup(null, "Ranged", 5, killSeconds, 20, "Rapid", "Eagle Eye", null,
			Arrays.asList("Toxic blowpipe", "Ava's assembler", "Black d'hide body"), 2);
	}

	@Test
	public void theKitPlaqueSaysToTopUpBeforeTheTakeOutList()
	{
		ChargeBook book = new ChargeBook();
		book.read(Charged.TOXIC_BLOWPIPE, new int[]{30, 2000}, 1);
		List<Charged> items = new ArrayList<>(Arrays.asList(Charged.TOXIC_BLOWPIPE, Charged.SERPENTINE_HELM));
		List<ChargeCheck.Line> lines = ChargeCheck.of(items, book, "zulrah", Collections.singletonList(blowpipeSetup(60)), -1, -1, 6);
		Map<String, Long> pack = new HashMap<>();
		pack.put("Shark", 15L);
		pack.put("Prayer potion(4)", 4L);
		pack.put("Anti-venom+(4)", 1L);
		// Zulrah's kit all packed, so what is left to say is the charged gear.
		KitCheck packed = KitCheck.of(Bosses.ZULRAH, Stock.EMPTY, Stock.of(pack), true);
		Plaque.Content c = Plaques.kit("Zulrah", packed, lines);
		assertEquals("TAKE OUT FOR THE NEXT BOSS", c.over);
		assertEquals("Top up Toxic blowpipe", c.lines.get(0).text);
		assertEquals("darts ~3 kills of 6", c.lines.get(0).right);
		assertEquals("Check Serpentine helm", c.lines.get(1).text);

		// Topped up, the kit is packed and only the unchecked helm is left to say.
		book.read(Charged.TOXIC_BLOWPIPE, new int[]{3000, 2000}, 2);
		lines = ChargeCheck.of(items, book, "zulrah", Collections.singletonList(blowpipeSetup(60)), -1, -1, 6);
		c = Plaques.kit("Zulrah", packed, lines);
		assertEquals("Your kit is packed. Good luck.", c.lines.get(0).text);
		assertEquals("Check Serpentine helm to count its charges", c.lines.get(1).text);
	}

}
