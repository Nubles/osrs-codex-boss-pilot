package com.codex.bosspilot.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The bosses Boss Pilot plans for. Kits are suggestions for a comfortable trip of a few kills,
 * not minimums; only lines marked must-have stop a trip. Notes and briefings say what a guide would
 * say before the fight starts. They are read in the panel and nothing reacts to the fight itself.
 */
public final class Bosses
{
	static final String[] PRAYER = {"Prayer potion", "Super restore", "Sanfew serum"};
	static final String[] ANTIPOISON = {"Antipoison", "Superantipoison", "Antidote+", "Antidote++", "Anti-venom", "Anti-venom+", "Sanfew serum"};
	static final String[] ANTIVENOM = {"Anti-venom", "Anti-venom+", "Extended anti-venom+"};
	static final String[] ANTIFIRE = {"Antifire potion", "Extended antifire", "Super antifire potion", "Extended super antifire"};
	static final String[] SUPER_ANTIFIRE = {"Super antifire potion", "Extended super antifire"};
	static final String[] DRAGONFIRE_SHIELD = {"Anti-dragon shield", "Dragonfire shield*", "Dragonfire ward*", "Ancient wyvern shield*"};
	static final String[] HOUSE = {"Teleport to house", "Construct. cape*", "Max cape"};

	public static final Boss OBOR = Boss.builder("obor", "Obor", Boss.Tier.EARLY)
		.where("Edgeville Dungeon", "Edgeville, then the dungeon trapdoor; the gate is past the hill giants")
		.attacks("Melee and ranged")
		.notes("The gate opens with a Giant key, which hill giants drop. One key is one fight.",
			"A free-to-play boss, so a good first boss for any account.")
		.suggested(40)
		.kit(Supply.must("Giant key", 1, "Giant key"), Supply.food(120), Supply.optional("House teleport", 1, HOUSE))
		.free()
		.brief("Club and boulders", "He hits with his club up close and throws from range. Most players fight next to him with Protect from Melee and eat through the thrown hits.")
		.brief("Knockback", "A heavy swing can knock you back. Step straight back in.")
		.build();

	public static final Boss BRYOPHYTA = Boss.builder("bryophyta", "Bryophyta", Boss.Tier.EARLY)
		.where("Varrock Sewers", "Varrock, then the manhole east of the palace; her gate is past the moss giants")
		.attacks("Melee and magic")
		.notes("The gate opens with a Mossy key, which moss giants drop. One key is one fight.",
			"She calls in growthlings during the fight. A free-to-play boss.")
		.suggested(45)
		.kit(Supply.must("Mossy key", 1, "Mossy key"), Supply.food(140), Supply.optional("House teleport", 1, HOUSE))
		.free()
		.brief("Magic from range", "She mixes melee with a magic attack. Staying next to her with Protect from Magic is the usual way.")
		.brief("Growthlings", "She calls in three growthlings. Kill them quickly so they do not add to the damage.")
		.build();

	public static final Boss SCURRIUS = Boss.builder("scurrius", "Scurrius", Boss.Tier.EARLY)
		.where("Varrock Sewers", "Varrock, then the manhole east of the palace")
		.attacks("Melee, ranged and magic")
		.notes("The rat king of the sewers. There is a shared room and a private one.",
			"Giant rats join in; they die quickly to any area attack.")
		.targets("Scurrius")
		.suggested(55)
		.kit(Supply.food(160), Supply.doses("Prayer", 8, PRAYER), Supply.optional("House teleport", 1, HOUSE))
		.brief("Three styles", "He uses melee, ranged and magic. Most players protect against the style he is using at the time.")
		.brief("Falling rocks", "Rocks fall on shaded tiles. Step off the shadows.")
		.brief("Giant rats", "Rats join in through the fight. Kill them before they pile up.")
		.build();

	public static final Boss GIANT_MOLE = Boss.builder("mole", "Giant Mole", Boss.Tier.EARLY)
		.where("Falador Park", "Falador, then dig on a mole hill in the park")
		.attacks("Melee")
		.notes("It burrows away to another part of its lair when its health drops; a Falador shield shows where it went.",
			"Its loot is mostly mole claws and skins, traded at Wyson in the park for bird nests.")
		.targets("Giant Mole")
		.suggested(70)
		.kit(Supply.must("Spade", 1, "Spade"), Supply.food(200), Supply.doses("Prayer", 8, PRAYER),
			Supply.optional("Falador shield", 1, "Falador shield*"))
		.brief("Dark lair", "The lair is dark. Bring a light source so you can see.")
		.brief("Melee only", "All its damage is melee, so Protect from Melee stops almost all of it.")
		.brief("Burrowing", "It digs away when its health drops. Follow the dust, or use a Falador shield to find it.")
		.build();

	public static final Boss BARROWS = Boss.builder("barrows", "Barrows", Boss.Tier.MID)
		.where("Barrows, Morytania", "Barrows teleport, or the swamp boat from Mort'ton")
		.attacks("Ahrim: magic. Karil: ranged. Dharok, Guthan, Torag and Verac: melee")
		.notes("Six brothers, one in each crypt. Dig with a spade on a mound to go in.",
			"The chest is in the tunnels below; one brother waits in the crypt that leads there.",
			"Getting to Morytania needs Priest in Peril.")
		.npcs("Ahrim the Blighted", "Dharok the Wretched", "Guthan the Infested", "Karil the Tainted",
			"Torag the Corrupted", "Verac the Defiled")
		.kc("Barrows", "Barrows chest")
		.lootEvent("Barrows")
		.quest("PRIEST_IN_PERIL")
		.suggested(75)
		.kit(Supply.must("Spade", 1, "Spade"), Supply.doses("Prayer", 12, PRAYER), Supply.food(120),
			Supply.optional("Barrows teleport", 1, "Barrows teleport"))
		.brief("One style each", "Each brother uses one style. Protect from Magic for Ahrim, Missiles for Karil, Melee for the other four.")
		.brief("Prayer drain", "Prayer drains faster in the crypts and tunnels. Bring more prayer than the fights alone would need.")
		.brief("Dharok", "He hits harder the lower his health gets. Keep your prayer up for him above all.")
		.brief("Weaknesses", "Magic works well on the melee brothers. Ranged or melee on Ahrim.")
		.build();

	public static final Boss SARACHNIS = Boss.builder("sarachnis", "Sarachnis", Boss.Tier.MID)
		.where("Forthos Dungeon, Hosidius", "Xeric's talisman or Hosidius, then the dungeon south of the town")
		.attacks("Melee and ranged")
		.notes("Weak to crush weapons.", "She calls in spawn of Sarachnis twice in the fight.")
		.targets("Sarachnis")
		.suggested(80)
		.kit(Supply.food(200), Supply.doses("Prayer", 8, PRAYER), Supply.optional("House teleport", 1, HOUSE))
		.brief("Melee and ranged", "She bites up close and spits from range. Stay next to her with Protect from Melee.")
		.brief("Spawn", "She calls spawn of Sarachnis at two points in the fight. Kill them fast.")
		.build();

	public static final Boss KALPHITE_QUEEN = Boss.builder("kq", "Kalphite Queen", Boss.Tier.MID)
		.where("Kalphite Lair, Kharidian Desert", "Shantay Pass, then the lair south-west of it")
		.attacks("Melee, ranged and magic")
		.notes("She has two forms; the second appears when the first dies.",
			"Ropes are tied at the two drops into the lair the first time you use them.",
			"Her attacks can poison.")
		.targets("Kalphite Queen")
		.suggested(100)
		.kit(Supply.food(260), Supply.doses("Prayer", 16, PRAYER), Supply.doses("Antipoison", 4, ANTIPOISON),
			Supply.firstVisit("Rope", 2, "Rope"))
		.brief("Two forms", "The second form appears when the first dies, with new defences. Many players switch styles for it.")
		.brief("Magic and ranged", "Her magic and ranged hits reach anyone nearby. Most players protect against magic and eat through the rest.")
		.brief("Workers and guardians", "Kalphites around the lair are aggressive. Bring enough food for the walk in.")
		.build();

	public static final Boss KING_BLACK_DRAGON = Boss.builder("kbd", "King Black Dragon", Boss.Tier.WILDERNESS)
		.where("King Black Dragon Lair, Wilderness", "The lever in the Lava Maze dungeon, deep in the Wilderness")
		.attacks("Melee and dragonfire")
		.notes("Its dragonfire hits hard without an anti-dragon shield or an antifire potion.",
			"The lair is in the Wilderness: other players can attack you on the way, so bring only what you can lose.")
		.suggested(75)
		.kit(Supply.must("Dragonfire shield", 1, DRAGONFIRE_SHIELD), Supply.food(200),
			Supply.doses("Prayer", 8, PRAYER), Supply.doses("Antifire", 2, ANTIFIRE))
		.wilderness()
		.brief("Four breaths", "He breathes plain, poisonous, freezing and shocking fire. An anti-dragon shield and an antifire cut them right down.")
		.brief("Melee up close", "His melee only reaches you when you stand next to him. Protect from Melee if you fight up close.")
		.build();

	public static final Boss DAGANNOTH_KINGS = Boss.builder("dks", "Dagannoth Kings", Boss.Tier.MID)
		.where("Waterbirth Island Dungeon", "Waterbirth teleport, or the boat from Rellekka")
		.attacks("Rex: melee. Prime: magic. Supreme: ranged")
		.notes("Three kings, each weak to a different style: Rex to magic, Prime to ranged, Supreme to melee.",
			"The dungeon's first doors need a rune thrownaxe and a pet rock, or a friend.")
		.npcs("Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme")
		.kc("Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme")
		.targets("Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme")
		.suggested(105)
		.kit(Supply.food(300), Supply.doses("Prayer", 16, PRAYER), Supply.optional("Rune thrownaxe", 1, "Rune thrownaxe"),
			Supply.optional("Pet rock", 1, "Pet rock"))
		.brief("One style each", "Rex hits with melee, Prime with magic and Supreme with ranged. Protect against the king you are fighting.")
		.brief("Weaknesses", "Rex is weak to magic, Prime to ranged and Supreme to melee.")
		.brief("Several at once", "All three can be up together. Many players fight one at a time from a spot where the others cannot reach.")
		.build();

	public static final Boss KRAKEN = Boss.builder("kraken", "Kraken", Boss.Tier.SLAYER)
		.where("Kraken Cove, south of Piscatoris", "Fairy ring AKQ, then the cove's boss room")
		.attacks("Magic")
		.notes("Only ranged and magic reach it in the water.",
			"Wake its four tentacles first, then the Kraken itself.")
		.targets("Kraken")
		.skill("Slayer", 87)
		.task("kraken")
		.suggested(90)
		.kit(Supply.food(100), Supply.doses("Prayer", 4, PRAYER))
		.brief("Waking it", "Disturb the four small whirlpools first, then the big one in the middle.")
		.brief("Magic", "Its hits are magic. A trident and Protect from Magic keep the damage low.")
		.build();

	public static final Boss THERMONUCLEAR = Boss.builder("thermy", "Thermonuclear smoke devil", Boss.Tier.SLAYER)
		.where("Smoke Devil Dungeon, west of Castle Wars", "Castle Wars, then the dungeon to the north-west")
		.attacks("Ranged")
		.notes("A facemask or slayer helmet is a must: the smoke drains your stats without one.")
		.targets("Thermonuclear smoke devil")
		.skill("Slayer", 93)
		.task("smoke devil")
		.suggested(95)
		.kit(Supply.must("Facemask or slayer helmet", 1, "Facemask", "Face mask", "Slayer helmet*", "Black slayer helmet*",
			"Green slayer helmet*", "Red slayer helmet*", "Purple slayer helmet*", "Turquoise slayer helmet*",
			"Hydra slayer helmet*", "Twisted slayer helmet*", "Tztok slayer helmet*", "Vampyric slayer helmet*",
			"Tzkal slayer helmet*", "Araxyte slayer helmet*"),
			Supply.food(200), Supply.doses("Prayer", 12, PRAYER))
		.brief("Smoke", "The smoke in the room drains your stats. A facemask or slayer helmet stops it.")
		.brief("Ranged hits", "Its attacks are ranged. Protect from Missiles stops them.")
		.brief("Multi-combat", "Smoke devils in the dungeon are aggressive and the room is multi-combat.")
		.build();

	public static final Boss GROTESQUE_GUARDIANS = Boss.builder("gg", "Grotesque Guardians", Boss.Tier.SLAYER)
		.where("Roof of the Slayer Tower, Morytania", "Slayer ring or Canifis, then the tower's roof")
		.attacks("Melee, ranged and magic")
		.notes("Two gargoyles, Dusk and Dawn, fought together.",
			"The roof needs a gargoyle task and a brittle key.")
		.npcs("Dusk", "Dawn")
		.kc("Grotesque Guardians")
		.targets("Grotesque Guardians")
		.skill("Slayer", 75)
		.task("gargoyle")
		.quest("PRIEST_IN_PERIL")
		.suggested(100)
		.kit(Supply.must("Brittle key", 1, "Brittle key"), Supply.food(260), Supply.doses("Prayer", 16, PRAYER))
		.brief("Two gargoyles", "Dawn flies and is hit only with ranged. Dusk is hit with melee, and crush is best.")
		.brief("Falling rocks", "Rocks fall on shaded tiles. Step off the shadows.")
		.brief("Lightning", "Electric orbs cross the roof. Keep clear of them.")
		.brief("Prayer", "Dawn uses ranged and Dusk melee. Protect against the one fighting you.")
		.build();

	public static final Boss ZULRAH = Boss.builder("zulrah", "Zulrah", Boss.Tier.LATE)
		.where("Zul-Andra, west of Tirannwn", "Zul-andra teleport, then the boat on the dock")
		.attacks("Ranged and magic, and melee from one of its forms")
		.notes("It changes between three forms during the fight; ranged and magic both get used.",
			"Its attacks cause venom.", "Getting to Zul-Andra needs Regicide.")
		.targets("Zulrah")
		.quest("REGICIDE")
		.suggested(100)
		.kit(Supply.food(300), Supply.doses("Prayer", 16, PRAYER), Supply.doses("Anti-venom", 4, ANTIVENOM),
			Supply.optional("Zul-andra teleport", 1, "Zul-andra teleport"))
		.brief("Three forms", "Green uses ranged, blue uses magic and ranged, red uses melee.")
		.brief("Usual answer", "Green: Protect from Missiles and hit it with magic. Blue: Protect from Magic and hit it with ranged. Red: step away from where it stares.")
		.brief("Venom clouds", "It spits clouds that venom you. Keep off them.")
		.brief("Rotations", "Its forms come in a few set orders. A guide shows them, and they are worth learning before your first trip.")
		.build();

	public static final Boss VORKATH = Boss.builder("vorkath", "Vorkath", Boss.Tier.LATE)
		.where("Ungael, north of Rellekka", "Rellekka, then Torfinn's boat at the north docks")
		.attacks("Ranged, magic and dragonfire")
		.notes("Dragonfire needs both a dragonfire shield and a super antifire potion.",
			"Undead and a dragon, so dragonbane and the salve amulet both help.",
			"Fighting it needs Dragon Slayer II.")
		.targets("Vorkath")
		.quest("DRAGON_SLAYER_II")
		.suggested(105)
		.kit(Supply.must("Dragonfire shield", 1, DRAGONFIRE_SHIELD), Supply.mustDoses("Super antifire", 2, SUPER_ANTIFIRE),
			Supply.food(300), Supply.doses("Prayer", 12, PRAYER), Supply.doses("Anti-venom", 4, ANTIVENOM))
		.brief("Dragonfire", "Its fire needs both a dragonfire shield and a super antifire.")
		.brief("Acid", "It covers the floor in acid. Walk a clean path and keep off the pools.")
		.brief("Zombified spawn", "It freezes you and sends a spawn. Kill the spawn with Crumble Undead.")
		.brief("Firebomb", "A purple or pink fireball is aimed at your tile. Move at least two tiles.")
		.brief("Ranged and magic", "Most players pray Protect from Missiles.")
		.build();

	public static final Boss GRAARDOR = Boss.builder("graardor", "General Graardor", Boss.Tier.GOD_WARS)
		.where("Bandos stronghold, God Wars Dungeon", "Trollheim, then the dungeon north of it")
		.attacks("Melee and ranged")
		.notes("The door needs 70 Strength and 40 kills of Bandos followers.",
			"His three bodyguards fight with him.")
		.targets("General Graardor")
		.skill("Strength", 70)
		.suggested(110)
		.kit(Supply.food(360), Supply.doses("Prayer", 24, PRAYER), Supply.firstVisit("Rope", 1, "Rope"))
		.brief("Melee and ranged", "He hits up close and throws at the whole room. Most players pray Protect from Melee and eat through the ranged hits.")
		.brief("Bodyguards", "Three bodyguards fight with him. Kill them, or step around them so fewer reach you.")
		.build();

	public static final Boss ZILYANA = Boss.builder("zilyana", "Commander Zilyana", Boss.Tier.GOD_WARS)
		.where("Saradomin encampment, God Wars Dungeon", "Trollheim, then the dungeon north of it")
		.attacks("Melee and magic")
		.notes("The way in needs 70 Agility and 40 kills of Saradomin followers.",
			"Ropes are tied the first time you climb down to her.")
		.targets("Commander Zilyana")
		.skill("Agility", 70)
		.suggested(110)
		.kit(Supply.food(320), Supply.doses("Prayer", 24, PRAYER), Supply.firstVisit("Rope", 2, "Rope"))
		.brief("Fast melee", "She is quick and hits hard with melee. Protect from Melee.")
		.brief("Magic", "Her magic hits everyone in the room and cannot be prayed against while you pray melee. Keep your hitpoints high.")
		.brief("Bodyguards", "Three bodyguards fight with her. Kill them, or keep them on another player.")
		.build();

	public static final Boss KREEARRA = Boss.builder("kreearra", "Kree'arra", Boss.Tier.GOD_WARS)
		.where("Armadyl eyrie, God Wars Dungeon", "Trollheim, then the dungeon north of it")
		.attacks("Ranged, magic and melee")
		.notes("The way in needs 70 Ranged, a crossbow and a mith grapple, and 40 kills of Armadyl followers.",
			"Only ranged and magic reach it.")
		.targets("Kree'arra")
		.skill("Ranged", 70)
		.suggested(105)
		.kit(Supply.must("Mith grapple", 1, "Mith grapple"), Supply.food(320), Supply.doses("Prayer", 24, PRAYER))
		.brief("Ranged and magic", "He hits the whole room with ranged and magic. Most players pray Protect from Missiles.")
		.brief("Melee knockback", "Standing next to him earns a melee hit that knocks you back. Fight from range.")
		.brief("Bodyguards", "Three bodyguards fight with him, one of each style.")
		.build();

	public static final Boss KRIL = Boss.builder("kril", "K'ril Tsutsaroth", Boss.Tier.GOD_WARS)
		.where("Zamorak fortress, God Wars Dungeon", "Trollheim, then the dungeon north of it")
		.attacks("Melee and magic")
		.notes("The way in needs 70 Hitpoints and 40 kills of Zamorak followers.",
			"A demon, so demonbane weapons and spells help. His attacks can poison.")
		.targets("K'ril Tsutsaroth")
		.skill("Hitpoints", 70)
		.suggested(105)
		.kit(Supply.food(320), Supply.doses("Prayer", 24, PRAYER), Supply.doses("Antipoison", 4, ANTIPOISON))
		.brief("Melee and magic", "He hits with melee and magic. Most players pray Protect from Melee.")
		.brief("Special attack", "Now and then he shouts and hits through Protect from Melee, draining your prayer. Keep prayer up and food ready.")
		.brief("Poison", "His hits can poison. Bring antipoison.")
		.brief("Bodyguards", "Three bodyguards fight with him.")
		.build();

	public static final List<Boss> ALL = Collections.unmodifiableList(Arrays.asList(
		OBOR, BRYOPHYTA, SCURRIUS, GIANT_MOLE, BARROWS, SARACHNIS, KALPHITE_QUEEN, KING_BLACK_DRAGON,
		DAGANNOTH_KINGS, KRAKEN, THERMONUCLEAR, GROTESQUE_GUARDIANS, ZULRAH, VORKATH,
		GRAARDOR, ZILYANA, KREEARRA, KRIL));

	private Bosses()
	{
	}

	public static Boss byKey(String key)
	{
		for (Boss b : ALL)
		{
			if (b.getKey().equals(key))
			{
				return b;
			}
		}
		return null;
	}

	/** The boss this NPC name belongs to, or null. */
	public static Boss byNpc(String npcName)
	{
		for (Boss b : ALL)
		{
			if (b.isNpc(npcName))
			{
				return b;
			}
		}
		return null;
	}

	/** The boss a kill count name belongs to, or null. */
	public static Boss byKc(String kcName)
	{
		for (Boss b : ALL)
		{
			if (b.isKc(kcName))
			{
				return b;
			}
		}
		return null;
	}
}
