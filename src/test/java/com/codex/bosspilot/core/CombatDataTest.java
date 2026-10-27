package com.codex.bosspilot.core;

import com.codex.bosspilot.combat.Fighter;
import com.codex.bosspilot.combat.Planner;
import com.codex.bosspilot.data.Gear;
import com.codex.bosspilot.data.RoadmapData;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** The shared Codex combat data loads, and every boss with a fight target finds it there. */
public class CombatDataTest
{
	@Test
	public void everyBossTargetIsInTheCombatData() throws Exception
	{
		RoadmapData data = RoadmapData.load(new Gson());
		for (Boss b : Bosses.ALL)
		{
			for (String t : b.getTargets())
			{
				assertNotNull(b + " target " + t, data.target(t));
			}
		}
	}

	@Test
	public void aWhipBeatsBareHandsAtSarachnis() throws Exception
	{
		RoadmapData data = RoadmapData.load(new Gson());
		Planner planner = new Planner(data);
		Map<String, Integer> levels = new HashMap<>();
		for (String s : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Hitpoints", "Prayer"})
		{
			levels.put(s, 75);
		}
		Fighter fighter = new Fighter(levels, Fighter.Boost.POTIONS, true, false, false, false, false, false);
		List<Gear> owned = new ArrayList<>();
		Gear whip = data.named("Abyssal whip");
		assertNotNull(whip);
		owned.add(whip);
		double bare = planner.optimise(new ArrayList<>(), fighter, data.target("Sarachnis")).dps();
		double armed = planner.optimise(owned, fighter, data.target("Sarachnis")).dps();
		assertTrue(armed + " > " + bare, armed > bare);
	}
}
