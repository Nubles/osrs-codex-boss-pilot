package com.codex.bosspilot;

import com.codex.bosspilot.combat.Fighter;
import com.codex.bosspilot.core.Aim;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CodexBossPilotConfig.GROUP)
public interface CodexBossPilotConfig extends Config
{
	String GROUP = "codexbosspilot";
	String AIM_KEY = "aim";

	@ConfigSection(name = "Planning", description = "How the next boss is chosen", position = 0)
	String planning = "planning";

	@ConfigSection(name = "On a trip", description = "What shows while you are at a boss", position = 1)
	String trip = "trip";

	@ConfigItem(keyName = AIM_KEY, name = "Aim", position = 0, section = planning,
		description = "What the next boss should serve: moving your account on, the best loot an hour from your own trips, or the fastest kills")
	default Aim aim()
	{
		return Aim.PROGRESS;
	}

	@ConfigItem(keyName = "wilderness", name = "Include Wilderness bosses", position = 1, section = planning,
		description = "List bosses in the Wilderness, where other players can attack you")
	default boolean wilderness()
	{
		return false;
	}

	@ConfigItem(keyName = "boost", name = "Assume boosts", position = 2, section = planning,
		description = "The potions to assume when working out your best setup and kill times")
	default Fighter.Boost boost()
	{
		return Fighter.Boost.POTIONS;
	}

	@ConfigItem(keyName = "bankPlaque", name = "Kit at the bank", position = 3, section = planning,
		description = "While the bank is open, show what to take out for the next boss")
	default boolean bankPlaque()
	{
		return true;
	}

	@ConfigItem(keyName = "tripPlaque", name = "Trip plaque", position = 0, section = trip,
		description = "Show kills, kill times and how long your supplies will last while at a boss")
	default boolean tripPlaque()
	{
		return true;
	}

	@ConfigItem(keyName = "notifyLow", name = "Notify when supplies run low", position = 1, section = trip,
		description = "Send a notification once when your supplies will not last another kill")
	default boolean notifyLow()
	{
		return true;
	}

	@ConfigItem(keyName = "reminders", name = "Reminders about you", position = 2, section = trip,
		description = "On a trip, remind you when your own hitpoints or prayer are low, a boost or antifire wears off, or a must-have item was left behind. They never look at the boss")
	default boolean reminders()
	{
		return true;
	}

	@Range(max = 90)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "eatPercent", name = "Remind to eat under", position = 3, section = trip,
		description = "Remind you to eat when your hitpoints fall under this share; 0 turns it off")
	default int eatPercent()
	{
		return 50;
	}

	@Range(max = 99)
	@ConfigItem(keyName = "prayerPoints", name = "Remind to drink under", position = 4, section = trip,
		description = "Remind you to drink a prayer potion when your prayer points fall under this; 0 turns it off")
	default int prayerPoints()
	{
		return 10;
	}

	@ConfigItem(keyName = "boostReminders", name = "Boost reminders", position = 5, section = trip,
		description = "Remind you when a combat boost you drank has worn off")
	default boolean boostReminders()
	{
		return true;
	}
}
