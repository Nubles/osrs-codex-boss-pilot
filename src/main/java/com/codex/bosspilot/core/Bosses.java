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
			"Getting to Morytania needs Priest in Peril, and digging into the crypts needs His Faithful Servants started.")
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
		.where("Smoke Devil Dungeon, south of Castle Wars", "Fairy ring BKP, or Castle Wars and then south-west")
		.attacks("Magical ranged that goes through prayer")
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
		.brief("Through prayer", "Its magical ranged attack hits through protection prayers, up to 8 every two ticks. Eat through it.")
		.brief("Multi-combat", "Smoke devils in the dungeon are aggressive and the room is multi-combat.")
		.build();

	public static final Boss GROTESQUE_GUARDIANS = Boss.builder("gg", "Grotesque Guardians", Boss.Tier.SLAYER)
		.where("Roof of the Slayer Tower, Morytania", "Slayer ring or Canifis, then the tower's roof")
		.attacks("Melee, ranged and magic")
		.notes("Two gargoyles, Dusk and Dawn, fought together.",
			"The roof needs a gargoyle task, and a brittle key once to open it for good.")
		.npcs("Dusk", "Dawn")
		.kc("Grotesque Guardians")
		.targets("Grotesque Guardians")
		.skill("Slayer", 75)
		.task("gargoyle")
		.quest("PRIEST_IN_PERIL")
		.suggested(100)
		.kit(Supply.firstVisit("Brittle key", 1, "Brittle key"), Supply.food(260), Supply.doses("Prayer", 16, PRAYER))
		.brief("Two gargoyles", "Dawn flies, so she is hit with ranged, magic or a halberd; earth spells hurt her most. Dusk is hit only with melee, and crush is best.")
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
		.notes("The door needs 70 Strength and 40 kills of Bandos followers (35 to 15 with Combat Achievement tiers).",
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
		.notes("The way in needs 70 Agility and 40 kills of Saradomin followers (35 to 15 with Combat Achievement tiers).",
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
		.notes("The way in needs 70 Ranged, a crossbow and a mith grapple, and 40 kills of Armadyl followers (35 to 15 with Combat Achievement tiers).",
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
		.notes("The way in needs 70 Hitpoints and 40 kills of Zamorak followers (35 to 15 with Combat Achievement tiers).",
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

	/** Pickaxes by their in-game names (RuneLite gameval ItemID); "Iron pickaxe*" also takes the (nz) ones. */
	static final String[] PICKAXE = {"Bronze pickaxe", "Iron pickaxe*", "Steel pickaxe", "Black pickaxe", "Mithril pickaxe*",
		"Adamant pickaxe", "Rune pickaxe*", "Gilded pickaxe", "Dragon pickaxe", "Dragon pickaxe (or)", "Infernal pickaxe",
		"Infernal pickaxe (or)", "Crystal pickaxe", "3rd Age pickaxe"};

	public static final Boss ZALCANO = Boss.builder("zalcano", "Zalcano", Boss.Tier.SKILLING)
		.where("Zalcano's prison, under Prifddinas", "Prifddinas, then the teleport platform just west of the south-eastern bank")
		.notes("A skilling boss: higher Mining, Smithing and Runecraft help most, and more Hitpoints to stand her attacks.",
			"Prifddinas needs Song of the Elves. The quickest way there is a teleport crystal or an eternal one.",
			"Your reward goes to item drops or Mining experience; Rhiannon switches it. Rare drops roll either way.")
		.quest("SONG_OF_THE_ELVES")
		.skill("Mining", 70)
		.skill("Smithing", 70)
		.kit(Supply.must("Pickaxe", 1, PICKAXE), Supply.doses("Stamina", 4, "Stamina potion"), Supply.food(100),
			Supply.optional("Teleport crystal", 1, "Teleport crystal*", "Eternal teleport crystal"),
			Supply.optional("Varrock armour", 1, "Varrock armour*"), Supply.optional("Graceful top", 1, "Graceful top*"))
		.brief("Earning drops", "Do at least 5 damage to her shield for any drop, and 31 combined damage to be in the running for rare drops and the pet.")
		.brief("MVP", "The player who deals the most damage gets 10% more of their non-unique loot, rounded up.")
		.brief("Rare drops", "The tool seed goes to one player, more often the one with more points. The shard comes more often the more you contribute. Smolcano is the same for everyone eligible.")
		.build();

	/**
	 * The Moons of Peril, as a run of up to three moons and the Lunar Chest. Facts: Neypotzli ("The
	 * entrance to Neypotzli is accessed from the northernmost end of Cam Torum"; the Lunar Chest is in
	 * the Ancient Shrine), Moons of Peril ("At least one Moon must be subdued to open the Lunar
	 * Chest"; "it is highly recommended to gather supplies from within these caverns"), Lunar Chest
	 * (the rolls), Perilous Moons (the quest; "Combat level 75" recommended; "It is also possible to
	 * bank between fighting each boss without losing progress"). No line here is about a fight.
	 */
	public static final Boss MOONS = Boss.builder("moons", "Moons of Peril", Boss.Tier.MID)
		.where("Neypotzli, beneath Cam Torum", "The Quetzal to Cam Torum, then the entrance at the northernmost end of Cam Torum")
		.notes("Three moons, each in its own cavern off the antechamber. The Lunar Chest is in the Ancient Shrine.",
			"Subdue at least one moon to open the chest; it resets each time it is opened.",
			"Food and potions made in the caverns are the usual supplies; the supply crates hold the tools.",
			"Neypotzli needs Perilous Moons.")
		.npcs("Blood Moon", "Blue Moon", "Eclipse Moon")
		.kc("Lunar Chest")
		.lootEvent("Lunar Chest")
		.quest("PERILOUS_MOONS")
		.suggested(75)
		.kit(Supply.optional("House teleport", 1, HOUSE))
		.brief("The chest", "One moon subdued gives one roll on the chest, two give three rolls and all three give six. A set piece takes the place of the rolls.")
		.brief("Set pieces", "Each moon subdued is a 1 in 56 chance at a piece of its own set, and you get every piece of a set before a duplicate.")
		.brief("Banking", "You can bank between moons without losing the run's progress.")
		.build();

	/**
	 * The Gauntlet and the Corrupted Gauntlet, as runs: a preparation in the maze and a fight with the
	 * Hunllef. Facts: The Gauntlet ("The Gauntlet can be found by entering the Gauntlet Portal, found
	 * in the north-western part of Prifddinas"; "Players cannot bring any items into the Gauntlet";
	 * "The player has 10 minutes to prepare to fight this boss"; "After completing the Gauntlet once,
	 * players may try the Corrupted Gauntlet"; "The player only has 7 minutes and 30 seconds to
	 * prepare"; failing "will not grant any chance of unique items"), Prifddinas ("In order to enter
	 * the city, the quest Song of the Elves must be completed"), Reward Chest (The Gauntlet) for the
	 * rates. The suggested combat level, 104, is the level of The Gauntlet/Strategies' advised stats
	 * (80 Attack, 85 Strength, 80 Defence, 85 Ranged, 80 Magic, 85 Hitpoints, 77 Prayer). No line here
	 * is about the fight.
	 */
	public static final Boss GAUNTLET = Boss.builder("gauntlet", "The Gauntlet", Boss.Tier.LATE)
		.where("The Gauntlet, north-western Prifddinas", "Prifddinas, then the Gauntlet Portal in the north-west of the city")
		.notes("A run is a preparation in the maze, gathering and crafting your own gear and potions, then a fight with the Hunllef.",
			"Nothing you carry comes in: the game stores it while you are inside.",
			"The Corrupted Gauntlet opens once you have completed the Gauntlet.",
			"Prifddinas needs Song of the Elves.")
		.npcs("Crystalline Hunllef", "Corrupted Hunllef")
		.kc("Gauntlet", "Corrupted Gauntlet")
		.quest("SONG_OF_THE_ELVES")
		.suggested(104)
		.gearless()
		.kit(Supply.optional("Teleport crystal", 1, "Teleport crystal*", "Eternal teleport crystal"))
		.brief("The clock", "You have 10 minutes to prepare in the Gauntlet and 7 minutes 30 seconds in the Corrupted Gauntlet. When it runs out, the game takes you to the Hunllef.")
		.brief("Bringing nothing", "Everything on you is stored as you enter, so a run is built from what you gather inside.")
		.brief("Rewards", "Only a completed run rolls the seeds and the pet. Dying or leaving early gives a small token reward.")
		.build();

	/**
	 * The Maggot King, as a planned trip and a kill log. Facts (oldschool.runescape.wiki, Maggot
	 * King, read 4 November 2026): "requires completion of The Blood Moon Rises to kill"; "Players
	 * may only fight the boss by themselves, and the fight takes place inside an instance"; "a mix
	 * of melee and either magic or ranged is required to fight the Maggot King" and "it cannot be
	 * attacked with melee until it falls below ~980 hitpoints (65%)"; "Use Drakan's medallion to get
	 * to Castle Drakan, enter Vampyrium, and then run east"; "The entrance to its lair can be found
	 * in the forest east of Sangvesti, near the Ruined Village"; the aranei scout's quick travel;
	 * "Open-stomach" rolls the normal drop table and "Take-eggs" trades it for a better pet chance.
	 * The suggested combat level is The Blood Moon Rises' own recommendation ("110 Combat level"), a
	 * floor: the wiki gives none for the boss. The food and prayer in the kit are Boss Pilot's suggestion for a few kills, not the wiki's;
	 * your own trips' rates replace them. No line here is about the fight as it happens.
	 */
	public static final Boss MAGGOT_KING = Boss.builder("maggotking", "Maggot King", Boss.Tier.LATE)
		.where("The Maggot King's lair, east of Sangvesti in Vampyrium",
			"Drakan's medallion to Castle Drakan, into Vampyrium, then east to the forest past Sangvesti, near the Ruined Village")
		.notes("Solo only, in an instance.",
			"Bring melee and either magic or ranged: melee cannot touch it until it is below about 65% of its hitpoints.",
			"Rescue the aranei scout south-east of Sangvesti first to unlock quick travel to the lair.",
			"After each kill, Open-stomach rolls the drop table; Take-eggs gives up the loot for a better pet chance.",
			"Vampyrium needs The Blood Moon Rises.")
		.npcs("Maggot King", MaggotKing.CORPSE_NAME)
		.kc("Maggot King")
		.quest("THE_BLOOD_MOON_RISES")
		.suggested(110)
		.kit(Supply.optional("Drakan's medallion", 1, "Drakan's medallion"), Supply.food(400), Supply.doses("Prayer", 16, PRAYER))
		.brief("Two styles", "Pack melee and magic or ranged. The fight opens at range; melee only lands once it is below about 65%.")
		.brief("Solo instance", "Only you can fight it, and the fight is in your own instance.")
		.brief("The corpse", "Open-stomach for the drop table and its uniques, or Take-eggs for the pet at 1 in 1,502 instead of 1 in 3,500.")
		.build();

	public static final List<Boss> ALL = Collections.unmodifiableList(Arrays.asList(
		OBOR, BRYOPHYTA, SCURRIUS, GIANT_MOLE, BARROWS, SARACHNIS, KALPHITE_QUEEN, KING_BLACK_DRAGON,
		DAGANNOTH_KINGS, KRAKEN, THERMONUCLEAR, GROTESQUE_GUARDIANS, ZULRAH, VORKATH,
		GRAARDOR, ZILYANA, KREEARRA, KRIL, ZALCANO, MOONS, GAUNTLET, MAGGOT_KING));

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
