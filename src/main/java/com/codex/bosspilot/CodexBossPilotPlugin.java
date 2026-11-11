package com.codex.bosspilot;

import com.codex.bosspilot.combat.Fighter;
import com.codex.bosspilot.combat.Planner;
import com.codex.bosspilot.combat.Result;
import com.codex.bosspilot.combat.SlayerTask;
import com.codex.bosspilot.core.Account;
import com.codex.bosspilot.core.Aim;
import com.codex.bosspilot.core.BankSnapshot;
import com.codex.bosspilot.core.Barrows;
import com.codex.bosspilot.core.BarrowsRun;
import com.codex.bosspilot.core.Boss;
import com.codex.bosspilot.core.Bosses;
import com.codex.bosspilot.core.ChargeBook;
import com.codex.bosspilot.core.ChargeCheck;
import com.codex.bosspilot.core.Charged;
import com.codex.bosspilot.core.Chat;
import com.codex.bosspilot.core.ChestLog;
import com.codex.bosspilot.core.Debrief;
import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.KitCheck;
import com.codex.bosspilot.core.Logbook;
import com.codex.bosspilot.core.Gauntlet;
import com.codex.bosspilot.core.GauntletLog;
import com.codex.bosspilot.core.GauntletRun;
import com.codex.bosspilot.core.GauntletWatch;
import com.codex.bosspilot.core.LunarLog;
import com.codex.bosspilot.core.LunarRun;
import com.codex.bosspilot.core.Moons;
import com.codex.bosspilot.core.Outlook;
import com.codex.bosspilot.core.Ranker;
import com.codex.bosspilot.core.RatesV1;
import com.codex.bosspilot.core.Reminders;
import com.codex.bosspilot.core.Review;
import com.codex.bosspilot.core.RunSupplies;
import com.codex.bosspilot.core.Setup;
import com.codex.bosspilot.core.Stock;
import com.codex.bosspilot.core.Supply;
import com.codex.bosspilot.core.Trip;
import com.codex.bosspilot.core.TripTracker;
import com.codex.bosspilot.core.WayIn;
import com.codex.bosspilot.core.WorldMode;
import com.codex.bosspilot.core.MaggotKing;
import com.codex.bosspilot.core.MaggotLog;
import com.codex.bosspilot.core.MaggotRun;
import com.codex.bosspilot.core.MaggotWatch;
import com.codex.bosspilot.core.Zalcano;
import com.codex.bosspilot.core.ZalcanoLog;
import com.codex.bosspilot.core.ZalcanoLoot;
import com.codex.bosspilot.core.ZalcanoRun;
import com.codex.bosspilot.data.Gear;
import com.codex.bosspilot.data.RoadmapData;
import com.codex.bosspilot.data.Target;
import com.codex.bosspilot.overlay.Plaque;
import com.codex.bosspilot.overlay.Plaques;
import com.codex.bosspilot.ui.Actions;
import com.codex.bosspilot.ui.BossPanel;
import com.codex.bosspilot.ui.Sigil;
import com.codex.bosspilot.ui.View;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.HitsplatID;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.TileItem;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plans bossing around the player's own account: which boss to do next, the best setup they own
 * for it, the kit to take out of the bank, and, once at the boss, how the trip is going and how
 * long the supplies will last. Its reminders on a trip look only at the player: hitpoints,
 * prayer, boosts, antifire and kit. It never reads what a boss is doing, never tells the player
 * what to pray, where to stand or what a boss will do next, never clicks, and sends nothing off
 * the computer.
 */
@PluginDescriptor(
	name = "Codex Boss Pilot",
	description = "Plans your bossing: the next boss for your account, your best owned setup, the kit to bring and how long your supplies will last",
	tags = {"boss", "bossing", "pvm", "trip", "kit", "gear", "supplies", "planner", "kill", "codex"}
)
public class CodexBossPilotPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CodexBossPilotPlugin.class);
	static final String BANK_KEY = "bankV1";
	/** Boss Pilot's saved data in its own config group: a change to one is not a setting changing. */
	private static final Set<String> DATA_KEYS = new HashSet<>(Arrays.asList(Logbook.KEY, "bankV1", ChargeBook.KEY, ChestLog.KEY,
		"barrowsTunnel", ZalcanoLog.KEY, LunarLog.KEY, GauntletLog.KEY, MaggotLog.KEY, RatesV1.KEY));
	static final String PINNED_KEY = "pinned";
	/** The player's own note for a boss, under this prefix and the boss key. */
	static final String NOTE_KEY = "note.";
	/** Notes longer than this are cut when saved. */
	static final int NOTE_MAX = 500;
	private static final Skill[] BOOSTABLE = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};
	private static final int EVALUATE_TICKS = 5;
	private static final int QUEST_TICKS = 100;
	/** How close a boss has to be to count as being at it. */
	private static final int NEARBY = 24;
	/** Codex Bank Steward's shared bank, read only when Boss Pilot has not seen the bank itself. */
	private static final String STEWARD_GROUP = "codexbanksteward";
	private static final String STEWARD_KEY = "bankV1";
	private static final int KNIGHT_WAVES_DONE = 8;
	private static final int BOSS_TASK = 98;
	private static final Skill[] SKILLS = {
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.PRAYER, Skill.HITPOINTS, Skill.AGILITY,
		Skill.SLAYER,
	};
	private static final String[] PRAYER_POTIONS = {"Prayer potion", "Super restore", "Sanfew serum"};
	/** OSRS Wiki, Inventory: "A player's backpack inventory can hold up to 28 items". */
	static final int INVENTORY_SLOTS = 28;
	/** The Barrows above ground and the crypts and tunnels below, by map region (RuneLite's Barrows plugin: BARROWS_REGION_ID, CRYPT_REGION_ID). */
	static final int BARROWS_REGION = 14131;
	static final int CRYPT_REGION = 14231;
	/** The crypt with the tunnel this run, once found, saved per account so a relog keeps it. */
	static final String TUNNEL_KEY = "barrowsTunnel";
	/** The game's prompt on the sarcophagus that hides the tunnel (OSRS Wiki, Poll 75 blog: "You've found a hidden tunnel"). */
	static final String TUNNEL_PROMPT = "hidden tunnel";
	/** How long the chest count and the chest's contents may arrive apart and still be one chest. */
	static final int CHEST_TICKS = 10;
	/** How long after a Zalcano kill is logged the pet's message still belongs to it. */
	static final int ZALCANO_LATE_TICKS = 5;
	/** How long after a Gauntlet run is logged the pet's message still belongs to it. */
	static final int GAUNTLET_LATE_TICKS = 5;
	/** How long after a completed run a rare seed newly in the pack is taken as its reward (20 minutes). */
	static final int GAUNTLET_REWARD_TICKS = 2000;
	/** The Hunllef within this many tiles: the fight has begun, when the game's own mark is not read. */
	static final int HUNLLEF_NEAR = 16;
	/** A Lunar Chest reward seen again this soon with the same contents is the same chest, shown again. */
	static final int LUNAR_SAME_TICKS = 100;
	/** The varbits of the six brothers, in Barrows.Brother order. */
	private static final int[] BROTHER_VARBITS = {
		VarbitID.BARROWS_KILLED_AHRIM, VarbitID.BARROWS_KILLED_DHAROK, VarbitID.BARROWS_KILLED_GUTHAN,
		VarbitID.BARROWS_KILLED_KARIL, VarbitID.BARROWS_KILLED_TORAG, VarbitID.BARROWS_KILLED_VERAC,
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private Notifier notifier;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private CodexBossPilotConfig config;

	private final Plaque tripPlaque = new Plaque(OverlayPosition.TOP_LEFT);
	private final Plaque kitPlaque = new Plaque(OverlayPosition.TOP_LEFT);
	private final TripTracker tracker = new TripTracker();

	private BossPanel panel;
	private NavigationButton navButton;
	private RoadmapData data;
	private Planner planner;

	private int ticks;
	private Logbook logbook = new Logbook();
	private Review review;
	private Set<String> quests = Collections.emptySet();
	private Map<Integer, Long> bankItems = Collections.emptyMap();
	private long bankSeenAt;
	private Stock bankStock = Stock.EMPTY;
	private Stock carried = Stock.EMPTY;
	private Set<Integer> carriedIds = Collections.emptySet();
	/** Names of what the player wears, for an Ava's device and the trident a message means. */
	private Set<String> wornNames = Collections.emptySet();
	/** The game's last word on the player's charged items, and the paces learnt from it. */
	private ChargeBook charges = new ChargeBook();
	private Boss nearby;
	private String selected;
	private boolean warned;

	// ---- Barrows ----
	/** The player's own Barrows chests. */
	private ChestLog chestLog = new ChestLog();
	/** The crypt with the tunnel this run, or null until found. */
	private Barrows.Brother tunnel;
	/** The stairs and sarcophagi of the crypts loaded now, where they stand. */
	private final Map<WorldPoint, Barrows.Brother> cryptObjects = new HashMap<>();
	/** The brothers and levels of the run under way, kept as the game clears them when the chest opens. */
	private Set<Barrows.Brother> runKilled = Collections.emptySet();
	private int runLevels;
	/** A chest being put together from the chest count message and the reward's contents. */
	private int chestKc = -1;
	private List<String> chestItems;
	private long chestValue = -1;
	private int chestWait = -1;
	/** When the last chest of this trip was opened, to time the next run. */
	private long lastChestAt;
	/** After a chest, the run's varbits are the old run's until the game clears them. */
	private boolean runCleared;

	// ---- The Maggot King ----
	/** The player's own Maggot King kills. */
	private MaggotLog maggotLog = new MaggotLog();
	/** Puts each Maggot King kill together from its kill count, the corpse option and the loot. */
	private final MaggotWatch maggotWatch = new MaggotWatch();
	/** When the last Maggot King kill of this trip began, to time the next; 0 when none. */
	private long lastMaggotAt;
	private Trip lastMaggotTrip;
	/** The tick a maggot egg was last popped, so the pet's message just after it is the egg's. */
	private int lastPopTick = -1000;

	// ---- Zalcano ----
	/** The player's own Zalcano kills. */
	private ZalcanoLog zalcanoLog = new ZalcanoLog();
	/** The kill being put together from its message, its loot and the pet's message. */
	private final ZalcanoLoot zalcanoLoot = new ZalcanoLoot();
	/** When the last Zalcano kill of this trip came, to time the next; 0 when none. */
	private long lastZalcanoKillAt;
	private Trip lastZalcanoTrip;
	/** The tick the last Zalcano kill was logged, so a pet message just after it joins it. */
	private int lastZalcanoTick = -1000;
	// ---- The Gauntlet ----
	/** The player's own Gauntlet runs. */
	private GauntletLog gauntletLog = new GauntletLog();
	/** The run under way. */
	private final GauntletWatch gauntletWatch = new GauntletWatch();
	/** Game ticks since the plugin started, never reset: a Gauntlet run's clock, through its loading screens. */
	private int gauntletTicks;
	/** The tick the last Gauntlet run was logged, so a pet message just after it joins it. */
	private int lastGauntletTick = -1000;
	/** The tick the last completed run was logged, for the seeds taken from the reward chest after it. */
	private int lastGauntletWinTick = -100000;
	/** The rare seeds in the pack as last counted, by reward; null before the first count. */
	private Map<Gauntlet.Unique, Integer> gauntletPack;
	// ---- Moons of Peril ----
	/** The player's own Lunar Chests. */
	private LunarLog lunarLog = new LunarLog();
	/** The moons subdued on the run under way, from the game's varbits and kill count lines. */
	private final Set<Moons.Moon> moonsDown = java.util.EnumSet.noneOf(Moons.Moon.class);
	/** The moons' varbits as last read, a bit a moon, so a moon newly down counts even before they clear. */
	private int moonVarbits;
	/** After a chest, the moons' varbits are the old run's until the game clears them. */
	private boolean moonsCleared;
	/** The supplies the run under way has used. */
	private final RunSupplies runSupplies = new RunSupplies();
	/** Lunar Chest objects loaded in the scene now. */
	private int lunarChests;
	/** A chest being put together from its reward and the chest count message. */
	private int lunarKc = -1;
	private List<String> lunarItems;
	private long lunarValue = -1;
	private int lunarWait = -1;
	/** The last chest logged: when, at which tick, and its contents, so the same reward shown again is not logged twice. */
	private long lastLunarAt;
	private int lastLunarTick = -1000;
	private List<String> lastLunarItems;

	/** The player's ways into Prifddinas, from Codex Wayfinder. */
	private WayIn wayIn = WayIn.none();

	/** Names of item ids, looked up once. Client thread only. */
	private final Map<Integer, String> names = new HashMap<>();

	// The setups, worked out off the client thread whenever what they depend on changes.
	/** The best-setup kill time for the trip under way, from the gear carried, worked out off the client thread. */
	private volatile String theoryKey;
	private volatile double theorySeconds = -1;

	/** The rates last shared, without their time, so they are written only when they change. */
	private String ratesSignature;

	private volatile String setupsKey;
	private volatile Map<String, List<Setup>> setups = Collections.emptyMap();
	private volatile boolean setupsPending;

	@Provides
	CodexBossPilotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexBossPilotConfig.class);
	}

	@Override
	protected void startUp()
	{
		try
		{
			data = RoadmapData.load(gson);
			planner = new Planner(data);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Boss Pilot could not load its combat data", e);
			data = null;
			planner = null;
		}
		panel = new BossPanel(new PanelActions());
		navButton = NavigationButton.builder()
			.tooltip("Codex Boss Pilot")
			.icon(Sigil.icon(16))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(tripPlaque);
		overlayManager.add(kitPlaque);
		clientThread.invokeLater(() ->
		{
			loadProfile();
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				readQuests();
				readCarried();
			}
			evaluate();
		});
	}

	@Override
	protected void shutDown()
	{
		saveFinished();
		overlayManager.remove(tripPlaque);
		overlayManager.remove(kitPlaque);
		clientToolbar.removeNavigation(navButton);
		tripPlaque.show(null);
		kitPlaque.show(null);
		panel = null;
		tracker.abandon();
		names.clear();
		cryptObjects.clear();
		setups = Collections.emptyMap();
		setupsKey = null;
		review = null;
	}

	// ---- Events ----

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (!learning())
		{
			// Not the account's own bossing (Last Man Standing, Fresh Start, seasonal and the like): no trips.
			tracker.abandon();
			nearby = null;
			if (++ticks % EVALUATE_TICKS == 0)
			{
				evaluate();
			}
			return;
		}
		ticks++;
		nearby = findNearby();
		readBarrows();
		readZalcano();
		readMaggot();
		readMoons();
		gauntletTicks++;
		readGauntlet();
		Stock pack = carried;
		tracker.observe(nearby, pack.foodHeal(), pack.doses(PRAYER_POTIONS), freeSlots(), System.currentTimeMillis());
		Trip on = tracker.current();
		if (on != null)
		{
			tracker.mustCarried(mustCarried(on.getBoss()));
		}
		saveFinished();
		if (ticks % QUEST_TICKS == 0)
		{
			readQuests();
		}
		// On a trip the plaque's clock and supplies move, so it is worked out twice as often.
		if (ticks % EVALUATE_TICKS == 0 || tracker.current() != null && ticks % 2 == 0)
		{
			evaluate();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOADING)
		{
			cryptObjects.clear();
			lunarChests = 0;
		}
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			endMaggot();
			endGauntlet();
			tracker.end(state == GameState.HOPPING ? Debrief.Cause.HOPPED : Debrief.Cause.LOGGED_OUT);
			saveFinished();
			nearby = null;
			evaluate();
		}
		else if (state == GameState.LOGGED_IN)
		{
			ticks = 0;
			clientThread.invokeLater(() ->
			{
				readQuests();
				readCarried();
				evaluate();
			});
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// Another account: its trips, bank and logbook are its own.
		tracker.abandon();
		review = null;
		selected = null;
		setupsKey = null;
		clientThread.invokeLater(() ->
		{
			loadProfile();
			evaluate();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CodexBossPilotConfig.GROUP.equals(event.getGroup()) && !DATA_KEYS.contains(event.getKey()))
		{
			if ("boost".equals(event.getKey()))
			{
				setupsKey = null;
			}
			clientThread.invokeLater(this::evaluate);
		}
		else if (WayIn.GROUP.equals(event.getGroup()) && WayIn.KEY.equals(event.getKey()))
		{
			wayIn = WayIn.parse(gson, configManager.getRSProfileConfiguration(WayIn.GROUP, WayIn.KEY));
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id == InventoryID.BANK)
		{
			readBank(event.getItemContainer());
			evaluate();
		}
		else if (id == InventoryID.INV || id == InventoryID.WORN)
		{
			readCarried();
			if (id == InventoryID.INV)
			{
				gauntletSeeds(event.getItemContainer());
			}
		}
		else if (!learning())
		{
			return;
		}
		else if (id == InventoryID.TRAIL_REWARDINV && region() == CRYPT_REGION)
		{
			// The Barrows chest's reward; clue caskets share this container, but not this place.
			readChest(event.getItemContainer());
		}
		else if (id == InventoryID.PMOON_REWARDINV)
		{
			readLunar(event.getItemContainer());
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// The Lunar Chest's reward, read as RuneLite's loot tracker reads it when the interface opens.
		if (event.getGroupId() == InterfaceID.PMOON_REWARD)
		{
			readLunar(client.getItemContainer(InventoryID.PMOON_REWARDINV));
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		TileItem item = event.getItem();
		if (item == null || item.getOwnership() != TileItem.OWNERSHIP_SELF || region() != Zalcano.PRISON_REGION)
		{
			return;
		}
		// Only the player's own drops in the prison: loot is put together around the kill message.
		zalcanoLoot.item(ticks, name(item.getId()), item.getQuantity(), (long) itemManager.getItemPrice(item.getId()) * item.getQuantity());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject o = event.getGameObject();
		Barrows.Brother b = Barrows.Brother.byObject(o.getId());
		if (b != null)
		{
			cryptObjects.put(o.getWorldLocation(), b);
		}
		if (o.getId() == Moons.CHEST || o.getId() == Moons.CHEST_OPEN)
		{
			lunarChests++;
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject o = event.getGameObject();
		if (Barrows.Brother.byObject(o.getId()) != null)
		{
			cryptObjects.remove(o.getWorldLocation());
		}
		if (o.getId() == Moons.CHEST || o.getId() == Moons.CHEST_OPEN)
		{
			lunarChests = Math.max(0, lunarChests - 1);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM || !learning())
		{
			return;
		}
		String message = Text.removeTags(event.getMessage());
		long now = System.currentTimeMillis();
		if (config.charges() && readCharges(message, now))
		{
			return;
		}
		if (readGauntletChat(message, now))
		{
			return;
		}
		Chat.Kc kc = Chat.kc(message);
		if (kc != null)
		{
			Moons.Moon moon = Moons.Moon.byName(kc.name);
			if (moon != null)
			{
				// A moon's own kill count line: it is down this run.
				moonsDown.add(moon);
				return;
			}
			Boss boss = Bosses.byKc(kc.name);
			if (boss == Bosses.MAGGOT_KING)
			{
				maggotKill(kc.count, now);
				return;
			}
			if (boss == Bosses.MOONS)
			{
				// The chest count: the chest is logged, and counted on the trip, from its reward.
				logbook.noteKc(boss, kc.count);
				lunarCount(kc.count);
				saveLogbook();
				return;
			}
			if (boss != null)
			{
				logbook.noteKc(boss, kc.count);
				if (boss == Bosses.BARROWS)
				{
					chestKc = kc.count;
					chestWait = chestWait < 0 ? 0 : chestWait;
				}
				else if (boss == Bosses.ZALCANO)
				{
					zalcanoLoot.kill(ticks, kc.count);
				}
				tracker.kill(boss, now, kc.count);
				chargesKill(boss);
				saveLogbook();
				saveFinished();
			}
			return;
		}
		if (Zalcano.petMessage(message) && region() == Zalcano.PRISON_REGION)
		{
			petMessage();
			return;
		}
		if (MaggotKing.EGGS_NOTHING.equals(message))
		{
			maggotWatch.eggsNothing(ticks, now);
			return;
		}
		Trip trip = tracker.current();
		if (MaggotKing.petMessage(message) && (trip != null && trip.getBoss() == Bosses.MAGGOT_KING || maggotWatch.pending()
			|| ticks - lastPopTick <= MaggotWatch.POP_TICKS))
		{
			maggotWatch.pet(ticks);
			return;
		}
		double seconds = Chat.duration(message);
		Trip on = tracker.current();
		// A moon's own fight time is not a run's: the run is timed from chest to chest.
		if (seconds > 0 && (on == null || on.getBoss() != Bosses.MOONS))
		{
			tracker.duration(seconds, now);
			Trip t = tracker.current();
			if (t != null)
			{
				logbook.noteBest(t.getBoss(), seconds);
			}
		}
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		if (!learning())
		{
			return;
		}
		NPC npc = event.getNpc();
		Boss boss = npc == null ? null : Bosses.byNpc(npc.getName());
		// Zalcano's loot is read from the ground by the kill log, which adds it to the trip.
		// The Maggot King's is read by its kill log too.
		if (boss != null && boss != Bosses.ZALCANO && boss != Bosses.MAGGOT_KING)
		{
			tracker.loot(boss, value(event.getItems()), System.currentTimeMillis());
		}
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (!learning())
		{
			return;
		}
		if (Bosses.MAGGOT_KING.getName().equalsIgnoreCase(event.getName()))
		{
			// The corpse's loot, as RuneLite's loot tracker hands it on; its kill log adds it to the trip.
			if (!event.getItems().isEmpty())
			{
				maggotWatch.loot(ticks, System.currentTimeMillis(), drops(event.getItems()));
			}
			return;
		}
		// Chest loot, such as the Barrows chest, comes as an event rather than an NPC drop.
		Trip t = tracker.current();
		// The Barrows chest is read from its reward by the chest log, which adds it to the trip.
		if (t != null && t.getBoss() != Bosses.BARROWS && t.getBoss() != Bosses.MOONS && t.getBoss().getLootEvent() != null
			&& t.getBoss().getLootEvent().equalsIgnoreCase(event.getName()))
		{
			tracker.loot(t.getBoss(), value(event.getItems()), System.currentTimeMillis());
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		// Only hits the player takes, for the fight review; the boss's own hitsplats are never read.
		// In the Gauntlet not even those: nothing there counts the fight while it goes on.
		Trip on = tracker.current();
		if (event.getActor() == client.getLocalPlayer() && event.getHitsplat().getHitsplatType() != HitsplatID.HEAL
			&& (on == null || on.getBoss() != Bosses.GAUNTLET))
		{
			tracker.hit(event.getHitsplat().getAmount());
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() == client.getLocalPlayer() && learning())
		{
			Trip on = tracker.current();
			if (on != null && on.getBoss() == Bosses.MAGGOT_KING)
			{
				maggotLog.died();
				saveMaggot();
			}
			tracker.death();
			gauntletWatch.died(gauntletTicks);
		}
	}

	// ---- Reading ----

	/**
	 * Takes what a game message says about a charged item the player has on them. A message
	 * that only says "Your trident" means the trident worn, else the one carried.
	 *
	 * @return whether the message was about charges
	 */
	private boolean readCharges(String message, long now)
	{
		Charged.Reading r = Charged.read(message);
		if (r == null)
		{
			return false;
		}
		Charged item = r.getItem();
		if (r.isAnyTrident())
		{
			item = null;
			for (Charged t : Charged.tridents())
			{
				if (item == null && has(t, wornNames))
				{
					item = t;
				}
			}
			for (Charged t : Charged.tridents())
			{
				if (item == null && has(t, carried.asMap().keySet()))
				{
					item = t;
				}
			}
			if (item == null)
			{
				return true;
			}
			r = r.as(item);
		}
		if (!has(item, carried.asMap().keySet()))
		{
			// Another item gives the same message (a tanzanite helm's scales, a blazing blowpipe): not one Boss Pilot counts.
			return true;
		}
		charges.read(item, r.getValues(), now);
		Trip on = tracker.current();
		if (on != null)
		{
			int[] v = r.getValues();
			String out = null;
			for (int i = 0; i < v.length && out == null; i++)
			{
				if (v[i] == 0)
				{
					out = item.getName() + " " + item.getPools().get(i).getName();
				}
			}
			if (out != null)
			{
				tracker.ranOut(out);
			}
			else if (on.getRanOut() != null && on.getRanOut().startsWith(item.getName() + " "))
			{
				tracker.ranOut(null);
			}
		}
		saveCharges();
		clientThread.invokeLater(this::evaluate);
		return true;
	}

	private static boolean has(Charged item, Set<String> names)
	{
		for (String n : names)
		{
			if (Charged.byItem(n) == item)
			{
				return true;
			}
		}
		return false;
	}

	/** A kill: what it used of each charged item the player has on them, at the best pace known. */
	private void chargesKill(Boss boss)
	{
		if (!config.charges())
		{
			return;
		}
		List<Charged> items = Charged.among(carried.asMap().keySet());
		if (items.isEmpty())
		{
			return;
		}
		Trip t = tracker.current();
		List<Setup> list = setups.getOrDefault(boss.getKey(), Collections.emptyList());
		for (Charged c : items)
		{
			double[] per = ChargeCheck.perKill(c, charges, boss.getKey(), list, hitsPerKill(t), combatSeconds(t), new ChargeCheck.PaceFrom[1]);
			charges.kill(boss.getKey(), c, per);
		}
		saveCharges();
	}

	private static double hitsPerKill(Trip t)
	{
		return t == null || t.getKills() <= 0 ? -1 : (double) t.getHits() / t.getKills();
	}

	private static double combatSeconds(Trip t)
	{
		return t == null || t.getKillSeconds().isEmpty() ? -1 : t.averageKill();
	}

	/** How long each charged item lasts at a boss: those its setups use and those worn now. */
	private List<ChargeCheck.Line> chargeLines(Boss boss, List<Setup> list, Trip t, double target)
	{
		Set<Charged> items = new LinkedHashSet<>();
		if (t == null)
		{
			items.addAll(ChargeCheck.inSetups(list));
			items.addAll(Charged.among(wornNames));
		}
		else
		{
			items.addAll(Charged.among(carried.asMap().keySet()));
		}
		List<Charged> ordered = new ArrayList<>();
		for (Charged c : Charged.ALL)
		{
			if (items.contains(c))
			{
				ordered.add(c);
			}
		}
		return ChargeCheck.of(ordered, charges, boss.getKey(), list, t == null ? -1 : hitsPerKill(t), t == null ? -1 : combatSeconds(t), target);
	}

	/** The must-have kit lines the player carries now, by label. */
	private Set<String> mustCarried(Boss boss)
	{
		Set<String> out = new HashSet<>();
		for (KitCheck.Line l : KitCheck.of(boss, Stock.EMPTY, carried, true).getLines())
		{
			if (l.getSupply().getNeed() == Supply.Need.MUST && l.getCarried() > 0)
			{
				out.add(l.getSupply().getLabel());
			}
		}
		return out;
	}

	private static List<String> mustLabels(Boss boss)
	{
		List<String> out = new ArrayList<>();
		for (Supply s : boss.getKit())
		{
			if (s.getNeed() == Supply.Need.MUST)
			{
				out.add(s.getLabel());
			}
		}
		return out;
	}

	/** Reminders about the player alone, on a trip. */
	private List<Reminders.Reminder> reminders(Trip trip)
	{
		if (trip == null || !config.reminders())
		{
			return Collections.emptyList();
		}
		Reminders.Self me = new Reminders.Self();
		me.hitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
		me.maxHitpoints = client.getRealSkillLevel(Skill.HITPOINTS);
		me.prayer = client.getBoostedSkillLevel(Skill.PRAYER);
		me.maxPrayer = client.getRealSkillLevel(Skill.PRAYER);
		me.food = carried.foodHeal();
		me.doses = carried.doses(PRAYER_POTIONS);
		me.needsAntifire = trip.getBoss().needsAntifire();
		me.antifire = Math.max(client.getVarbitValue(VarbitID.ANTIFIRE_POTION), client.getVarbitValue(VarbitID.SUPER_ANTIFIRE_POTION));
		for (Skill skill : BOOSTABLE)
		{
			me.boosts.put(skill.getName(), client.getBoostedSkillLevel(skill) - client.getRealSkillLevel(skill));
		}
		me.missing = trip.missingMust(mustLabels(trip.getBoss()));
		Reminders.Limits limits = new Reminders.Limits();
		limits.eatPercent = config.eatPercent();
		limits.prayerPoints = config.prayerPoints();
		limits.boosts = config.boostReminders();
		return Reminders.of(me, limits, trip.getBoostPeaks());
	}

	private String note(Boss boss)
	{
		String n = boss == null ? null : configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, NOTE_KEY + boss.getKey());
		return n == null ? "" : n;
	}

	private Boss findNearby()
	{
		Player me = client.getLocalPlayer();
		if (me == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}
		WorldPoint at = me.getWorldLocation();
		int region = at.getRegionID();
		if (region == BARROWS_REGION || region == CRYPT_REGION)
		{
			// Barrows is a run through the mounds, crypts and tunnels, not one fight in sight.
			return Bosses.BARROWS;
		}
		if (inGauntlet() || region == Gauntlet.LOBBY_REGION)
		{
			// A Gauntlet run is a preparation in the maze, then the Hunllef: the whole run and the
			// lobby between runs are the trip.
			return Bosses.GAUNTLET;
		}
		if (region == Zalcano.PRISON_REGION)
		{
			// The whole prison is the trip, with or without Zalcano up between kills.
			return Bosses.ZALCANO;
		}
		if (lunarChests > 0 || moonsHudShowing())
		{
			// The moons are a run through Neypotzli to the chest: near the chest or with the moons' own
			// panel up, the trip goes on between fights too.
			return Bosses.MOONS;
		}
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc == null || npc.getName() == null)
			{
				continue;
			}
			Boss b = Bosses.byNpc(npc.getName());
			if (b != null && npc.getWorldLocation().distanceTo(at) <= NEARBY)
			{
				return b;
			}
		}
		return null;
	}

	// ---- The Maggot King ----

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		// Only which option the player picked, to know how a corpse was looted or an egg popped.
		MaggotKing.Choice c = MaggotKing.Choice.byOption(event.getMenuOption());
		if (c != null && event.getMenuTarget() != null && Text.removeTags(event.getMenuTarget()).contains(Bosses.MAGGOT_KING.getName()))
		{
			maggotWatch.choice(ticks, System.currentTimeMillis(), c);
		}
		else if (MaggotKing.POP.equals(event.getMenuOption()) && MaggotKing.isEgg(event.getItemId()))
		{
			lastPopTick = ticks;
			maggotWatch.pop(ticks);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() != MaggotKing.KILLS_VARP)
		{
			return;
		}
		int count = event.getValue();
		Trip on = tracker.current();
		if (maggotWatch.getLastKc() >= 0 && count == maggotWatch.getLastKc() + 1 && on != null && on.getBoss() == Bosses.MAGGOT_KING)
		{
			maggotKill(count, System.currentTimeMillis());
		}
		else
		{
			// The count as it stands, such as on logging in: not a kill. Only a rise of one at the boss is.
			maggotWatch.baseline(count);
		}
	}

	/** A Maggot King kill count, from the chat line or the game's own count: a kill once. */
	private void maggotKill(int count, long now)
	{
		if (!maggotWatch.kc(ticks, now, count))
		{
			return;
		}
		logbook.noteKc(Bosses.MAGGOT_KING, count);
		tracker.kill(Bosses.MAGGOT_KING, now);
		chargesKill(Bosses.MAGGOT_KING);
		saveLogbook();
		saveFinished();
	}

	/** Each tick: logs a kill once its loot is in, and a kill left unlooted when the trip ends. */
	private void readMaggot()
	{
		Trip t = tracker.current();
		boolean on = t != null && t.getBoss() == Bosses.MAGGOT_KING;
		List<MaggotWatch.Event> events;
		if (!on && lastMaggotTrip != null)
		{
			events = maggotWatch.end(ticks);
		}
		else
		{
			events = maggotWatch.poll(ticks);
		}
		apply(events);
		if (t != lastMaggotTrip)
		{
			// A new trip times its kills afresh: the walk in is not a kill.
			lastMaggotAt = 0;
			lastMaggotTrip = on ? t : null;
		}
	}

	private void endMaggot()
	{
		apply(maggotWatch.end(ticks));
		lastMaggotAt = 0;
		lastMaggotTrip = null;
	}

	private void apply(List<MaggotWatch.Event> events)
	{
		if (events.isEmpty())
		{
			return;
		}
		for (MaggotWatch.Event e : events)
		{
			if (e.kill != null)
			{
				MaggotLog.Kill k = e.kill;
				k.seconds = lastMaggotAt > 0 && k.at > lastMaggotAt ? Math.max(1, (k.at - lastMaggotAt) / 1000) : -1;
				maggotLog.add(k);
				if (lastMaggotTrip != null)
				{
					lastMaggotAt = k.at;
					if (k.value > 0)
					{
						tracker.loot(Bosses.MAGGOT_KING, k.value, System.currentTimeMillis());
					}
				}
			}
			if (e.late != null)
			{
				maggotLog.late(e.late, e.ofLastKill);
			}
		}
		saveMaggot();
	}

	private List<MaggotWatch.Drop> drops(java.util.Collection<ItemStack> items)
	{
		List<MaggotWatch.Drop> out = new ArrayList<>();
		for (ItemStack s : items)
		{
			out.add(new MaggotWatch.Drop(name(s.getId()), s.getQuantity(), (long) itemManager.getItemPrice(s.getId()) * s.getQuantity()));
		}
		return out;
	}

	private void saveMaggot()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, MaggotLog.KEY, maggotLog.toJson(gson));
		}
	}

	/** The Maggot King's kill log and trips, the drops in the bank or pack, and the account against the wiki's guides. */
	private MaggotRun maggotRun(Account account)
	{
		List<String> owned = new ArrayList<>(bankStock.asMap().keySet());
		owned.addAll(carried.asMap().keySet());
		Logbook.Entry e = logbook.peek(Bosses.MAGGOT_KING);
		return new MaggotRun(maggotLog, owned, account, logbook.rates(Bosses.MAGGOT_KING), e == null ? null : e.trips, config.maggotGoal());
	}

	// ---- Zalcano ----

	/** Each tick: logs the kill once its loot has had time to land. */
	private void readZalcano()
	{
		Trip t = tracker.current();
		if (t == null || t.getBoss() != Bosses.ZALCANO || t != lastZalcanoTrip)
		{
			// A new trip times its kills afresh: the walk in is not a kill.
			lastZalcanoKillAt = 0;
			lastZalcanoTrip = t != null && t.getBoss() == Bosses.ZALCANO ? t : null;
		}
		long now = System.currentTimeMillis();
		long seconds = lastZalcanoKillAt > 0 ? Math.max(1, (now - lastZalcanoKillAt) / 1000) : -1;
		ZalcanoLog.Kill kill = zalcanoLoot.poll(ticks, now, seconds);
		if (kill == null)
		{
			return;
		}
		zalcanoLog.add(kill);
		lastZalcanoTick = ticks;
		if (lastZalcanoTrip != null)
		{
			lastZalcanoKillAt = now;
			if (kill.value > 0)
			{
				tracker.loot(Bosses.ZALCANO, kill.value, now);
			}
		}
		saveZalcano();
	}

	/** The pet's message: with the kill being put together, or late for the one just logged. */
	private void petMessage()
	{
		if (!zalcanoLoot.pending() && ticks - lastZalcanoTick <= ZALCANO_LATE_TICKS)
		{
			zalcanoLog.late(Zalcano.Unique.PET.getName());
			saveZalcano();
		}
		else
		{
			zalcanoLoot.pet(ticks);
		}
	}

	private void saveZalcano()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, ZalcanoLog.KEY, zalcanoLog.toJson(gson));
		}
	}

	/** Zalcano's kill log, the rare drops in the bank or pack and the ways into Prifddinas. */
	private ZalcanoRun zalcanoRun()
	{
		List<String> owned = new ArrayList<>(bankStock.asMap().keySet());
		owned.addAll(carried.asMap().keySet());
		return new ZalcanoRun(zalcanoLog, owned, wayIn);
	}

	// ---- The Gauntlet ----

	/** Whether the game marks the player as in a Gauntlet run, or they stand in its maze. */
	private boolean inGauntlet()
	{
		if (client.getVarbitValue(VarbitID.PLAYER_IN_GAUNTLET) != 0 || client.getVarbitValue(VarbitID.GAUNTLET_BOSS_STARTED) != 0)
		{
			return true;
		}
		int r = gauntletRegion();
		return r == Gauntlet.REGULAR_REGION || r == Gauntlet.CORRUPTED_REGION;
	}

	/** The map region of the player's instance as RuneLite's Discord plugin reads it, or -1. */
	private int gauntletRegion()
	{
		Player me = client.getLocalPlayer();
		if (me == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return -1;
		}
		WorldPoint p = WorldPoint.fromLocalInstance(client, me.getLocalLocation());
		return p == null ? -1 : p.getRegionID();
	}

	/** The Hunllef in sight close by, by mode, or null. Read only to time the fight's start. */
	private Gauntlet.Mode hunllefNear()
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return null;
		}
		WorldPoint at = me.getWorldLocation();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			Gauntlet.Mode m = npc == null ? null : Gauntlet.hunllef(npc.getId());
			if (m != null && npc.getWorldLocation().distanceTo(at) <= HUNLLEF_NEAR)
			{
				return m;
			}
		}
		return null;
	}

	/** Each tick: the run under way, from the game's own marks, and logs it once over. */
	private void readGauntlet()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		boolean in = inGauntlet();
		Gauntlet.Mode hunllef = in ? hunllefNear() : null;
		Gauntlet.Mode hint = hunllef;
		int r = in ? gauntletRegion() : -1;
		if (r == Gauntlet.CORRUPTED_REGION || in && client.getVarbitValue(VarbitID.GAUNTLET_CORRUPTED) != 0)
		{
			hint = Gauntlet.Mode.CORRUPTED;
		}
		else if (hint == null && r == Gauntlet.REGULAR_REGION)
		{
			hint = Gauntlet.Mode.REGULAR;
		}
		boolean boss = client.getVarbitValue(VarbitID.GAUNTLET_BOSS_STARTED) != 0 || hunllef != null;
		GauntletLog.Run run = gauntletWatch.tick(gauntletTicks, System.currentTimeMillis(), in, boss, hint);
		if (run != null)
		{
			logGauntlet(run);
		}
	}

	/** The game's Gauntlet lines: the completion count, the run and preparation times, the pet. */
	private boolean readGauntletChat(String message, long now)
	{
		Chat.Kc kc = Chat.kc(message);
		Gauntlet.Mode mode = kc == null ? null : Gauntlet.Mode.byKc(kc.name);
		if (mode != null)
		{
			gauntletWatch.completion(gauntletTicks, now, mode, kc.count);
			GauntletLog.Totals t = gauntletLog.totals(mode);
			t.kc = kc.count;
			logbook.noteKc(Bosses.GAUNTLET, Math.max(0, gauntletLog.regular.kc) + Math.max(0, gauntletLog.corrupted.kc));
			tracker.kill(Bosses.GAUNTLET, now);
			saveLogbook();
			saveFinished();
			return true;
		}
		Gauntlet.Time time = Gauntlet.duration(message);
		if (time != null)
		{
			gauntletWatch.duration(time);
			Trip on = tracker.current();
			if (on != null && on.getBoss() == Bosses.GAUNTLET)
			{
				// The game's whole run time, for the trip's run times; it comes before the count.
				tracker.duration(time.seconds, now);
			}
			return true;
		}
		double prep = Gauntlet.preparation(message);
		if (prep > 0)
		{
			gauntletWatch.preparation(prep);
			return true;
		}
		if (Gauntlet.petMessage(message) && (inGauntlet() || gauntletTicks - lastGauntletTick <= GAUNTLET_LATE_TICKS))
		{
			if (!gauntletWatch.pet() && gauntletTicks - lastGauntletTick <= GAUNTLET_LATE_TICKS && gauntletLog.late(Gauntlet.Unique.PET.getName()))
			{
				saveGauntlet();
			}
			return true;
		}
		return false;
	}

	/** Logs a finished run. */
	private void logGauntlet(GauntletLog.Run run)
	{
		gauntletLog.add(run);
		lastGauntletTick = gauntletTicks;
		if (run.completed())
		{
			lastGauntletWinTick = gauntletTicks;
		}
		saveGauntlet();
	}

	/** Logging out or hopping ends the run under way. */
	private void endGauntlet()
	{
		GauntletLog.Run run = gauntletWatch.abort(gauntletTicks);
		if (run != null)
		{
			logGauntlet(run);
		}
		gauntletWatch.newSitting();
	}

	/**
	 * The rare seeds in the pack: one newly there soon after a completed run, with the bank shut,
	 * is taken from the reward chest and belongs to that run.
	 */
	private void gauntletSeeds(ItemContainer inv)
	{
		Map<Gauntlet.Unique, Integer> now = new java.util.EnumMap<>(Gauntlet.Unique.class);
		if (inv != null)
		{
			for (Item item : inv.getItems())
			{
				Gauntlet.Unique u = Gauntlet.Unique.byId(item.getId());
				if (u != null && u != Gauntlet.Unique.PET)
				{
					now.merge(u, Math.max(1, item.getQuantity()), Integer::sum);
				}
			}
		}
		Map<Gauntlet.Unique, Integer> before = gauntletPack;
		gauntletPack = now;
		if (before == null || bankOpen() || gauntletTicks - lastGauntletWinTick > GAUNTLET_REWARD_TICKS)
		{
			return;
		}
		boolean changed = false;
		for (Map.Entry<Gauntlet.Unique, Integer> e : now.entrySet())
		{
			if (e.getValue() > before.getOrDefault(e.getKey(), 0))
			{
				changed |= gauntletLog.late(e.getKey().getName());
			}
		}
		if (changed)
		{
			saveGauntlet();
		}
	}

	private void saveGauntlet()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, GauntletLog.KEY, gauntletLog.toJson(gson));
		}
	}

	/** The run under way, the run log and the rare rewards owned. */
	private GauntletRun gauntletRun()
	{
		List<String> owned = new ArrayList<>(bankStock.asMap().keySet());
		owned.addAll(carried.asMap().keySet());
		return new GauntletRun(gauntletLog, gauntletWatch.phase(),
			gauntletWatch.phase() == GauntletWatch.Phase.NONE ? null : gauntletWatch.getMode(),
			gauntletWatch.prepElapsed(gauntletTicks), gauntletWatch.prepTaken(), owned);
	}

	// ---- Moons of Peril ----

	/** Whether the game's Moons of Peril panel (RuneLite's InterfaceID.PmoonHud) is up. */
	private boolean moonsHudShowing()
	{
		Widget hud = client.getWidget(InterfaceID.PmoonHud.UNIVERSE);
		return hud != null && !hud.isHidden();
	}

	/**
	 * Each tick: the moons down this run from the game's varbits (kept, as the game may clear them
	 * when the chest opens), the supplies the run uses, and the chest once both its reward and its
	 * count are in.
	 */
	private void readMoons()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		int mask = 0;
		for (Moons.Moon m : Moons.Moon.values())
		{
			if (client.getVarbitValue(m.getDeadVarbit()) != 0)
			{
				mask |= 1 << m.ordinal();
			}
		}
		int risen = mask & ~moonVarbits;
		moonVarbits = mask;
		if (mask == 0)
		{
			moonsCleared = false;
		}
		moonsDown.addAll(Moons.fromMask(moonsCleared ? risen : mask));
		Trip t = tracker.current();
		if (t != null && t.getBoss() == Bosses.MOONS)
		{
			runSupplies.observe(carried.asMap(), bankOpen());
		}
		else
		{
			runSupplies.clear();
		}
		if (lunarWait >= 0)
		{
			lunarWait++;
			if (lunarKc >= 0 && lunarItems != null || lunarWait > CHEST_TICKS)
			{
				commitLunar();
			}
		}
	}

	/** The chest count message: with the chest being put together, or a moment late for the one just logged. */
	private void lunarCount(int count)
	{
		if (lunarWait < 0 && lunarItems == null && ticks - lastLunarTick <= CHEST_TICKS)
		{
			lunarLog.lateKc(count);
			saveLunar();
			return;
		}
		lunarKc = count;
		lunarWait = lunarWait < 0 ? 0 : lunarWait;
	}

	/** The Lunar Chest's reward, valued at the day's prices. */
	private void readLunar(ItemContainer c)
	{
		if (c == null)
		{
			return;
		}
		Map<String, Long> counts = new LinkedHashMap<>();
		long worth = 0;
		for (Item item : c.getItems())
		{
			if (item.getId() < 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			counts.merge(name(item.getId()), (long) item.getQuantity(), Long::sum);
			worth += (long) itemManager.getItemPrice(item.getId()) * item.getQuantity();
		}
		if (counts.isEmpty())
		{
			return;
		}
		List<String> items = new ArrayList<>();
		counts.forEach((n, q) -> items.add(q == 1 ? n : Format.number(q) + " x " + n));
		lunarItems = items;
		lunarValue = worth;
		lunarWait = lunarWait < 0 ? 0 : lunarWait;
	}

	/** Logs the chest, adds it to the trip and starts the next run afresh. */
	private void commitLunar()
	{
		long now = System.currentTimeMillis();
		if (lunarItems == null && lunarKc < 0 || lunarKc < 0 && sameAsLast())
		{
			resetLunar();
			return;
		}
		// A count with no reward seen is still a chest, logged without its worth.
		LunarLog.Chest chest = new LunarLog.Chest();
		chest.at = now;
		chest.kc = lunarKc;
		chest.value = lunarItems == null ? -1 : lunarValue;
		chest.moons = moonsDown.isEmpty() ? -1 : Moons.mask(moonsDown);
		if (lunarItems != null)
		{
			chest.items.addAll(lunarItems);
			chest.pieces.addAll(Moons.pieces(lunarItems));
		}
		Trip t = tracker.current();
		boolean onTrip = t != null && t.getBoss() == Bosses.MOONS;
		if (onTrip)
		{
			long from = Math.max(t.getStart(), lastLunarAt);
			chest.seconds = Math.max(1, (now - from) / 1000);
			chest.used.putAll(runSupplies.used());
		}
		tracker.kill(Bosses.MOONS, now);
		if (chest.value > 0)
		{
			tracker.loot(Bosses.MOONS, chest.value, now);
		}
		chargesKill(Bosses.MOONS);
		lunarLog.add(chest);
		lastLunarAt = now;
		lastLunarTick = ticks;
		lastLunarItems = lunarItems;
		moonsDown.clear();
		moonsCleared = moonVarbits != 0;
		runSupplies.nextRun();
		resetLunar();
		saveLunar();
		saveFinished();
	}

	/**
	 * Whether the reward is the last chest's shown again, such as the interface opening after its
	 * items came: the same contents with no moon down since, or within a minute.
	 */
	private boolean sameAsLast()
	{
		return lunarItems != null && lunarItems.equals(lastLunarItems) && (moonsDown.isEmpty() || ticks - lastLunarTick <= LUNAR_SAME_TICKS);
	}

	private void resetLunar()
	{
		lunarKc = -1;
		lunarItems = null;
		lunarValue = -1;
		lunarWait = -1;
	}

	private void saveLunar()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, LunarLog.KEY, lunarLog.toJson(gson));
		}
	}

	/** The moons down this run, its supplies, the chest log and the set pieces not yet owned. */
	private LunarRun lunarRun()
	{
		List<String> owned = new ArrayList<>(bankStock.asMap().keySet());
		owned.addAll(carried.asMap().keySet());
		return new LunarRun(moonsDown, runSupplies.used(), lunarLog, lunarLog.missing(owned)).withTripUsed(runSupplies.tripUsed());
	}

	// ---- Barrows ----

	/** The map region the player stands in, or -1. */
	private int region()
	{
		Player me = client.getLocalPlayer();
		return me == null || client.getGameState() != GameState.LOGGED_IN ? -1 : me.getWorldLocation().getRegionID();
	}

	/**
	 * Each tick: the brothers and levels of the run so far (kept, since the game clears them when
	 * the chest opens), the crypt with the tunnel when its prompt shows, and the chest once both
	 * its count and its contents are in.
	 */
	private void readBarrows()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		int[] values = new int[BROTHER_VARBITS.length];
		for (int i = 0; i < values.length; i++)
		{
			values[i] = client.getVarbitValue(BROTHER_VARBITS[i]);
		}
		Set<Barrows.Brother> killed = Barrows.killed(values);
		int levels = client.getVarbitValue(VarbitID.BARROWS_KILLED_MONSTER);
		boolean any = !killed.isEmpty() || levels > 0;
		if (!any)
		{
			runCleared = false;
		}
		else if (!runCleared)
		{
			runKilled = killed;
			runLevels = levels;
		}
		if (region() == CRYPT_REGION && tunnelPromptShowing())
		{
			Barrows.Brother here = cryptHere();
			if (here != null && here != tunnel)
			{
				tunnel = here;
				configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, TUNNEL_KEY, here.name());
			}
		}
		if (chestWait >= 0)
		{
			chestWait++;
			if (chestKc >= 0 && chestItems != null || chestWait > CHEST_TICKS)
			{
				commitChest();
			}
		}
	}

	/** Whether the game is asking about the hidden tunnel, in its message box or its choice. */
	private boolean tunnelPromptShowing()
	{
		for (int id : new int[]{InterfaceID.Messagebox.TEXT, InterfaceID.Chatmenu.OPTIONS})
		{
			Widget w = client.getWidget(id);
			if (w == null || w.isHidden())
			{
				continue;
			}
			if (mentionsTunnel(w.getText()))
			{
				return true;
			}
			Widget[] children = w.getDynamicChildren();
			if (children != null)
			{
				for (Widget c : children)
				{
					if (c != null && mentionsTunnel(c.getText()))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	static boolean mentionsTunnel(String text)
	{
		return text != null && Text.removeTags(text).toLowerCase(java.util.Locale.ROOT).contains(TUNNEL_PROMPT);
	}

	/** The crypt the player stands in: the brother whose stairs or sarcophagus is nearest. */
	private Barrows.Brother cryptHere()
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return null;
		}
		WorldPoint at = me.getWorldLocation();
		Barrows.Brother best = null;
		int bestD = Integer.MAX_VALUE;
		for (Map.Entry<WorldPoint, Barrows.Brother> e : cryptObjects.entrySet())
		{
			if (e.getKey().getPlane() != at.getPlane())
			{
				continue;
			}
			int d = e.getKey().distanceTo(at);
			if (d < bestD)
			{
				bestD = d;
				best = e.getValue();
			}
		}
		return best;
	}

	/** The chest's reward, as the game shows it. */
	private void readChest(ItemContainer c)
	{
		if (c == null)
		{
			return;
		}
		List<String> items = new ArrayList<>();
		long worth = 0;
		boolean any = false;
		for (Item item : c.getItems())
		{
			if (item.getId() < 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			any = true;
			items.add(name(item.getId()));
			worth += (long) itemManager.getItemPrice(item.getId()) * item.getQuantity();
		}
		if (!any)
		{
			return;
		}
		chestItems = items;
		chestValue = worth;
		chestWait = chestWait < 0 ? 0 : chestWait;
	}

	/** Logs the chest, adds it to the trip and starts the next run afresh. */
	private void commitChest()
	{
		long now = System.currentTimeMillis();
		ChestLog.Chest chest = new ChestLog.Chest();
		chest.at = now;
		chest.kc = chestKc;
		chest.value = chestItems == null ? -1 : chestValue;
		chest.brothers = runKilled.size();
		chest.potential = runKilled.isEmpty() && runLevels == 0 ? -1 : Barrows.potential(runLevels, runKilled.size());
		if (chestItems != null)
		{
			chest.pieces.addAll(Barrows.pieces(chestItems));
		}
		Trip t = tracker.current();
		if (t != null && t.getBoss() == Bosses.BARROWS)
		{
			long from = Math.max(t.getStart(), lastChestAt);
			chest.seconds = Math.max(1, (now - from) / 1000);
			if (chest.value > 0)
			{
				tracker.loot(Bosses.BARROWS, chest.value, now);
			}
		}
		chestLog.add(chest);
		lastChestAt = now;
		chestKc = -1;
		chestItems = null;
		chestValue = -1;
		chestWait = -1;
		runKilled = Collections.emptySet();
		runLevels = 0;
		runCleared = true;
		if (tunnel != null)
		{
			tunnel = null;
			configManager.unsetRSProfileConfiguration(CodexBossPilotConfig.GROUP, TUNNEL_KEY);
		}
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, ChestLog.KEY, chestLog.toJson(gson));
		}
	}

	/** Where the Barrows run stands, with the chest log and the pieces not yet owned. */
	private BarrowsRun barrowsRun()
	{
		List<String> owned = new ArrayList<>(bankStock.asMap().keySet());
		owned.addAll(carried.asMap().keySet());
		return new BarrowsRun(runKilled, runLevels, tunnel, config.barrowsAim(), chestLog, chestLog.missing(owned));
	}

	static Barrows.Brother brother(String name)
	{
		if (name == null || name.isEmpty())
		{
			return null;
		}
		try
		{
			return Barrows.Brother.valueOf(name);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	private void readQuests()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		Set<String> done = new HashSet<>();
		for (Boss b : Bosses.ALL)
		{
			for (String q : b.getQuests())
			{
				try
				{
					if (Quest.valueOf(q).getState(client) == QuestState.FINISHED)
					{
						done.add(q);
					}
				}
				catch (RuntimeException e)
				{
					log.debug("Could not read quest {}", q, e);
				}
			}
		}
		quests = done;
	}

	/** Free inventory slots, or -1 before the inventory is loaded; read for "bag full" in the trip debrief. */
	private int freeSlots()
	{
		ItemContainer c = client.getItemContainer(InventoryID.INV);
		if (c == null)
		{
			return -1;
		}
		int used = 0;
		for (Item item : c.getItems())
		{
			if (item.getId() >= 0 && item.getQuantity() > 0)
			{
				used++;
			}
		}
		return Math.max(0, INVENTORY_SLOTS - used);
	}

	/** What the player carries and wears, by name for the kit and by id for the setups. */
	private void readCarried()
	{
		Map<String, Long> byName = new HashMap<>();
		Set<Integer> ids = new HashSet<>();
		Set<String> worn = new HashSet<>();
		for (int containerId : new int[]{InventoryID.INV, InventoryID.WORN})
		{
			ItemContainer c = client.getItemContainer(containerId);
			if (c == null)
			{
				continue;
			}
			for (Item item : c.getItems())
			{
				if (item.getId() < 0 || item.getQuantity() <= 0)
				{
					continue;
				}
				ItemComposition comp = itemManager.getItemComposition(item.getId());
				if (comp.getNote() != -1)
				{
					// A noted item is a bank in the pack, not a supply.
					continue;
				}
				byName.merge(comp.getName(), (long) item.getQuantity(), Long::sum);
				ids.add(item.getId());
				if (containerId == InventoryID.WORN)
				{
					worn.add(comp.getName());
				}
			}
		}
		carried = Stock.of(byName);
		carriedIds = ids;
		wornNames = worn;
	}

	private void readBank(ItemContainer bank)
	{
		if (bank == null)
		{
			return;
		}
		Map<Integer, Long> items = new HashMap<>();
		for (Item item : bank.getItems())
		{
			if (item.getId() < 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			ItemComposition comp = itemManager.getItemComposition(item.getId());
			if (comp.getPlaceholderTemplateId() != -1)
			{
				continue;
			}
			items.merge(item.getId(), (long) item.getQuantity(), Long::sum);
		}
		bankItems = items;
		bankSeenAt = System.currentTimeMillis();
		bankStock = stockOf(items);
		Map<String, Object> saved = new HashMap<>();
		saved.put("version", 1);
		saved.put("savedAt", bankSeenAt);
		Map<String, Long> byId = new HashMap<>();
		items.forEach((id, n) -> byId.put(String.valueOf(id), n));
		saved.put("items", byId);
		configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, BANK_KEY, gson.toJson(saved));
	}

	private Stock stockOf(Map<Integer, Long> items)
	{
		Map<String, Long> byName = new HashMap<>();
		items.forEach((id, n) -> byName.merge(name(id), n, Long::sum));
		return Stock.of(byName);
	}

	private String name(int id)
	{
		return names.computeIfAbsent(id, i -> itemManager.getItemComposition(i).getName());
	}

	private void loadProfile()
	{
		logbook = Logbook.parse(gson, configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, Logbook.KEY));
		ratesSignature = storedRatesSignature();
		saveRates();
		charges = ChargeBook.parse(gson, configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, ChargeBook.KEY));
		chestLog = ChestLog.parse(gson, configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, ChestLog.KEY));
		tunnel = brother(configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, TUNNEL_KEY));
		runKilled = Collections.emptySet();
		runLevels = 0;
		chestKc = -1;
		chestItems = null;
		chestWait = -1;
		lastChestAt = 0;
		zalcanoLog = ZalcanoLog.parse(gson, configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, ZalcanoLog.KEY));
		maggotLog = MaggotLog.parse(gson, configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, MaggotLog.KEY));
		maggotWatch.reset();
		int maggotKc = client.getVarpValue(MaggotKing.KILLS_VARP);
		if (maggotKc > 0)
		{
			maggotWatch.baseline(maggotKc);
		}
		lastMaggotAt = 0;
		lastMaggotTrip = null;
		lastPopTick = -1000;
		lunarLog = LunarLog.parse(gson, configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, LunarLog.KEY));
		gauntletLog = GauntletLog.parse(gson, configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, GauntletLog.KEY));
		gauntletWatch.abort(gauntletTicks);
		gauntletWatch.newSitting();
		lastGauntletTick = -1000;
		lastGauntletWinTick = -100000;
		gauntletPack = null;
		moonsDown.clear();
		moonsCleared = false;
		runSupplies.clear();
		resetLunar();
		lastLunarAt = 0;
		lastLunarTick = -1000;
		lastLunarItems = null;
		zalcanoLoot.clear();
		lastZalcanoKillAt = 0;
		lastZalcanoTrip = null;
		wayIn = WayIn.parse(gson, configManager.getRSProfileConfiguration(WayIn.GROUP, WayIn.KEY));
		BankSnapshot own = BankSnapshot.parse(gson, configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, BANK_KEY));
		BankSnapshot steward = own != null ? null
			: BankSnapshot.parse(gson, configManager.getRSProfileConfiguration(STEWARD_GROUP, STEWARD_KEY));
		BankSnapshot snap = own != null ? own : steward;
		bankItems = snap == null ? Collections.emptyMap() : new HashMap<>(snap.getItems());
		bankSeenAt = snap == null ? 0 : snap.getSavedAt();
		bankStock = stockOf(bankItems);
		selected = null;
	}

	/** The signature of the rates saved last time, so a login does not rewrite unchanged rates; null when none. */
	private String storedRatesSignature()
	{
		String json = configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, RatesV1.KEY);
		if (json == null)
		{
			return null;
		}
		try
		{
			RatesV1 stored = gson.fromJson(json, RatesV1.class);
			return stored == null ? null : stored.signature(gson);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private long value(java.util.Collection<ItemStack> items)
	{
		long total = 0;
		for (ItemStack s : items)
		{
			total += (long) itemManager.getItemPrice(s.getId()) * s.getQuantity();
		}
		return total;
	}

	private Account account()
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return Account.UNKNOWN;
		}
		Map<String, Integer> levels = new HashMap<>();
		for (Skill skill : SKILLS)
		{
			levels.put(skill.getName(), client.getRealSkillLevel(skill));
		}
		boolean members = client.getWorldType().contains(WorldType.MEMBERS);
		return Account.of(levels, quests, client.getLocalPlayer().getCombatLevel(), currentTask(), members);
	}

	/** The name of the player's slayer task, read as Codex Gear Roadmap reads it, or null. */
	private String currentTask()
	{
		if (client.getVarpValue(VarPlayerID.SLAYER_COUNT) <= 0)
		{
			return null;
		}
		try
		{
			int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
			int row;
			if (taskId == BOSS_TASK)
			{
				List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID,
					DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0, client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
				if (rows.isEmpty())
				{
					return null;
				}
				row = (Integer) client.getDBTableField(rows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
			}
			else
			{
				List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, taskId);
				if (rows.isEmpty())
				{
					return null;
				}
				row = rows.get(0);
			}
			return (String) client.getDBTableField(row, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0];
		}
		catch (RuntimeException e)
		{
			log.debug("Could not read the slayer task", e);
			return null;
		}
	}

	private Fighter fighter(Account account)
	{
		boolean ancients;
		try
		{
			ancients = Quest.DESERT_TREASURE_I.getState(client) == QuestState.FINISHED;
		}
		catch (RuntimeException e)
		{
			ancients = false;
		}
		boolean piety = client.getVarbitValue(VarbitID.KR_KNIGHTWAVES_STATE) >= KNIGHT_WAVES_DONE;
		boolean rigour = client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED) == 1;
		boolean augury = client.getVarbitValue(VarbitID.PRAYER_AUGURY_UNLOCKED) == 1;
		boolean deadeye = client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) == 1;
		boolean vigour = client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) == 1;
		boolean kandarin = client.getVarbitValue(VarbitID.KANDARIN_DIARY_HARD_COMPLETE) == 1;
		SlayerTask task = SlayerTask.of(SlayerTask.Mode.SMART, account.getTask());
		return new Fighter(account.getLevels(), config.boost(), true, piety, rigour, augury, ancients, task)
			.withUnlocks(deadeye, vigour, kandarin);
	}

	// ---- Working it out ----

	private void evaluate()
	{
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && configManager.getRSProfileKey() != null;
		long now = System.currentTimeMillis();
		Account account = account();

		Ranker.Inputs in = new Ranker.Inputs();
		in.account = account;
		in.bank = bankStock;
		in.carried = carried;
		in.logbook = logbook;
		in.aim = config.aim();
		in.pinned = configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, PINNED_KEY);
		in.wilderness = config.wilderness();
		if (loggedIn)
		{
			refreshSetups(account);
		}
		in.killSeconds = killSeconds(setups);
		Trip on = tracker.current();
		if (loggedIn && on != null && on.getBoss() == nearby)
		{
			// Only while the boss is in sight: on the way home the gear carried is no longer the trip's.
			refreshTheory(on.getBoss(), account);
			String want = theoryKey;
			if (want != null && want.startsWith(on.getBoss().getKey() + "/") && theorySeconds > 0)
			{
				tracker.theory(theorySeconds);
			}
		}
		List<Ranker.Pick> picks = loggedIn ? Ranker.rank(Bosses.ALL, in) : Collections.emptyList();
		Ranker.Pick next = Ranker.next(picks);
		Ranker.Pick shown = next;
		if (selected != null)
		{
			for (Ranker.Pick p : picks)
			{
				if (p.getBoss().getKey().equals(selected))
				{
					shown = p;
				}
			}
		}

		Trip trip = tracker.current();
		// In the Gauntlet nothing carried comes in, so there are no supplies to count down.
		boolean gauntlet = trip != null && trip.getBoss() == Bosses.GAUNTLET;
		// At the Maggot King nothing on the plaque moves during a fight: no supplies countdown, no reminders.
		boolean maggot = trip != null && trip.getBoss() == Bosses.MAGGOT_KING;
		Outlook outlook = trip == null || gauntlet || maggot ? Outlook.UNKNOWN : Outlook.of(trip, logbook.rates(trip.getBoss()));
		List<ChargeCheck.Line> tripCharges = Collections.emptyList();
		if (trip != null && !gauntlet && !maggot && loggedIn && config.charges())
		{
			tripCharges = chargeLines(trip.getBoss(), setups.getOrDefault(trip.getBoss().getKey(), Collections.emptyList()), trip,
				outlook.getLevel() == Outlook.Level.UNKNOWN ? -1 : outlook.getKills());
			outlook = outlook.withCharges(ChargeCheck.first(tripCharges));
		}
		if (trip != null && outlook.getLevel() == Outlook.Level.LAST)
		{
			if (!warned && config.notifyLow())
			{
				notifier.notify("Boss Pilot: " + outlook.text() + ".");
			}
			warned = true;
		}
		else if (trip == null || outlook.getLevel() == Outlook.Level.PLENTY)
		{
			warned = false;
		}

		// No eat or prayer reminders in the Gauntlet: nothing there speaks during the Hunllef fight.
		List<Reminders.Reminder> reminders = loggedIn && !gauntlet && !maggot ? reminders(trip) : Collections.emptyList();
		String tripNote = trip == null ? "" : note(trip.getBoss());
		BarrowsRun barrows = loggedIn ? barrowsRun() : null;
		ZalcanoRun zalcano = loggedIn ? zalcanoRun() : null;
		LunarRun moons = loggedIn ? lunarRun() : null;
		GauntletRun gauntletRun = loggedIn ? gauntletRun() : null;
		MaggotRun maggotRun = loggedIn ? maggotRun(account) : null;
		tripPlaque.show(loggedIn && trip != null && config.tripPlaque()
			? gauntlet ? Plaques.gauntlet(trip, gauntletRun, now)
			: maggot ? Plaques.maggot(trip, maggotRun, logbook, now)
			: Plaques.trip(trip, outlook, logbook, reminders, tripNote, now, trip.getBoss() == Bosses.BARROWS ? barrows : null,
				trip.getBoss() == Bosses.ZALCANO ? zalcano : null, trip.getBoss() == Bosses.MOONS ? moons : null) : null);
		List<ChargeCheck.Line> nextCharges = !loggedIn || next == null || !config.charges() ? Collections.emptyList()
			: chargeLines(next.getBoss(), setups.getOrDefault(next.getBoss().getKey(), Collections.emptyList()), null,
				ChargeCheck.kitKills(next.getBoss(), logbook.rates(next.getBoss())));
		kitPlaque.show(loggedIn && trip == null && next != null && config.bankPlaque() && bankOpen()
			? Plaques.kit(next.getBoss().getName(), next.getKit(), nextCharges) : null);

		BossPanel p = panel;
		if (p == null)
		{
			return;
		}
		List<Setup> shownSetups = shown == null ? Collections.emptyList()
			: setups.getOrDefault(shown.getBoss().getKey(), Collections.emptyList());
		List<ChargeCheck.Line> shownCharges = shown == next ? nextCharges : !loggedIn || shown == null || !config.charges()
			? Collections.emptyList()
			: chargeLines(shown.getBoss(), shownSetups, null, ChargeCheck.kitKills(shown.getBoss(), logbook.rates(shown.getBoss())));
		View view = new View(loggedIn, picks, next, shown, shownSetups, setupsPending && shownSetups.isEmpty(), trip, outlook, logbook,
			review, in.aim, bankSeenAt, now, reminders, shown == null ? "" : note(shown.getBoss()))
			.withCharges(shownCharges, tripCharges)
			.withBarrows(barrows)
			.withZalcano(zalcano)
			.withMoons(moons)
			.withGauntlet(gauntletRun)
			.withMaggot(maggotRun);
		SwingUtilities.invokeLater(() -> p.update(view));
	}

	private boolean bankOpen()
	{
		Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		return bank != null && !bank.isHidden();
	}

	/** Seconds a kill takes with the best setup, by boss; a boss of several monsters takes their average. */
	static Map<String, Double> killSeconds(Map<String, List<Setup>> setups)
	{
		Map<String, Double> out = new HashMap<>();
		setups.forEach((key, list) ->
		{
			double sum = 0;
			int n = 0;
			for (Setup s : list)
			{
				if (s.getKillSeconds() > 0)
				{
					sum += s.getKillSeconds();
					n++;
				}
			}
			if (n > 0)
			{
				out.put(key, sum / n);
			}
		});
		return out;
	}

	/**
	 * Starts working out, off the client thread, how long a kill of this boss should take with the
	 * best setup from the gear carried now: the kill efficiency's yardstick for the trip.
	 */
	private void refreshTheory(Boss boss, Account account)
	{
		if (planner == null || boss.getTargets().isEmpty() || !boss.bringsGear())
		{
			return;
		}
		Set<Gear> owned = new LinkedHashSet<>();
		for (int id : carriedIds)
		{
			Gear g = data.gear(id);
			if (g != null)
			{
				owned.add(g);
			}
		}
		if (owned.isEmpty())
		{
			return;
		}
		String key = boss.getKey() + "/" + owned.hashCode() + "/" + account.getLevels().hashCode() + "/" + config.boost()
			+ "/" + Objects.hashCode(account.getTask());
		if (key.equals(theoryKey))
		{
			return;
		}
		theoryKey = key;
		theorySeconds = -1;
		Fighter fighter = fighter(account);
		List<Gear> ownedList = new ArrayList<>(owned);
		executor.execute(() ->
		{
			double secs = -1;
			try
			{
				secs = bestSeconds(boss, ownedList, fighter);
			}
			catch (RuntimeException e)
			{
				log.warn("Boss Pilot could not work out the trip's kill time", e);
			}
			if (key.equals(theoryKey))
			{
				theorySeconds = secs;
			}
		});
	}

	/** Seconds a kill takes with the best setup from this gear; a boss of several monsters takes their average; -1 when none. */
	private double bestSeconds(Boss b, List<Gear> owned, Fighter fighter)
	{
		double sum = 0;
		int n = 0;
		for (String name : b.getTargets())
		{
			Target t = data.target(name);
			if (t == null)
			{
				continue;
			}
			Result r = planner.optimise(owned, fighter, t).best().getResult();
			if (r.getKind() == null || r.getDps() <= 0)
			{
				continue;
			}
			double secs = r.secondsToKill(t);
			if (secs > 0 && !Double.isInfinite(secs))
			{
				sum += secs;
				n++;
			}
		}
		return n == 0 ? -1 : sum / n;
	}

	/** Starts working the setups out again off the client thread when what they hang on has changed. */
	private void refreshSetups(Account account)
	{
		if (planner == null)
		{
			return;
		}
		Set<Integer> ids = new HashSet<>(bankItems.keySet());
		ids.addAll(carriedIds);
		Set<Gear> owned = new LinkedHashSet<>();
		for (int id : ids)
		{
			Gear g = data.gear(id);
			if (g != null)
			{
				owned.add(g);
			}
		}
		Fighter fighter = fighter(account);
		String key = owned.hashCode() + "/" + account.getLevels().hashCode() + "/" + config.boost() + "/" + Objects.hashCode(account.getTask());
		if (key.equals(setupsKey))
		{
			return;
		}
		setupsKey = key;
		setupsPending = true;
		List<Gear> ownedList = new ArrayList<>(owned);
		executor.execute(() ->
		{
			Map<String, List<Setup>> out = new HashMap<>();
			try
			{
				for (Boss b : Bosses.ALL)
				{
					List<Setup> list = new ArrayList<>();
					for (String name : b.getTargets())
					{
						Target t = data.target(name);
						if (t == null)
						{
							continue;
						}
						Planner.Plan plan = planner.optimise(ownedList, fighter, t).best();
						Result r = plan.getResult();
						if (r.getKind() == null || r.getDps() <= 0)
						{
							continue;
						}
						List<String> gear = new ArrayList<>();
						for (int slot : new int[]{Gear.WEAPON, Gear.SHIELD, Gear.HEAD, Gear.BODY, Gear.LEGS, Gear.NECK, Gear.CAPE,
							Gear.HANDS, Gear.FEET, Gear.RING, Gear.AMMO})
						{
							Gear g = plan.getLoadout().get(slot);
							if (g != null)
							{
								gear.add(g.getName());
							}
						}
						double secs = r.secondsToKill(t);
						list.add(new Setup(b.getTargets().size() > 1 ? Setup.shortName(b.getName(), t.getName()) : null, r.getKind().toString(), r.getDps(),
							Double.isInfinite(secs) ? -1 : secs, r.getMaxHit(), r.getStyle(), r.getPrayer(), r.getSpell(), gear, r.getTicks()));
					}
					out.put(b.getKey(), list);
				}
			}
			catch (RuntimeException e)
			{
				log.warn("Boss Pilot could not work out setups", e);
			}
			if (key.equals(setupsKey))
			{
				setups = out;
				setupsPending = false;
				clientThread.invokeLater(this::evaluate);
			}
		});
	}

	// ---- Saving ----

	private void saveFinished()
	{
		Trip done = tracker.takeFinished();
		if (done == null)
		{
			return;
		}
		Logbook.TripRecord record = done.record();
		Logbook.Entry kept = logbook.peek(done.getBoss());
		review = Review.of(done.getBoss(), record, kept == null ? Collections.emptyList() : new ArrayList<>(kept.trips));
		if (record.kills > 0 || record.deaths > 0)
		{
			logbook.add(done.getBoss(), record);
			logbook.addTimes(done.getBoss(), done.getGameSeconds());
		}
		saveLogbook();
		saveRates();
	}

	/** Whether this world's trips are the account's own, to log and learn from. */
	private boolean learning()
	{
		return client.getGameState() != GameState.LOGGED_IN || WorldMode.learns(client.getWorldType());
	}

	/** Shares the rates other Codex apps read, when they have changed. */
	private void saveRates()
	{
		if (configManager.getRSProfileKey() == null)
		{
			return;
		}
		RatesV1 rates = RatesV1.of(logbook, System.currentTimeMillis());
		String sig = rates.signature(gson);
		if (sig.equals(ratesSignature))
		{
			return;
		}
		ratesSignature = sig;
		configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, RatesV1.KEY, rates.toJson(gson));
	}

	private void saveLogbook()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, Logbook.KEY, logbook.toJson(gson));
		}
	}

	private void saveCharges()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, ChargeBook.KEY, charges.toJson(gson));
		}
	}

	private final class PanelActions implements Actions
	{
		@Override
		public void select(String bossKey)
		{
			clientThread.invokeLater(() ->
			{
				selected = bossKey;
				evaluate();
			});
		}

		@Override
		public void pin(String bossKey)
		{
			clientThread.invokeLater(() ->
			{
				if (bossKey == null)
				{
					configManager.unsetRSProfileConfiguration(CodexBossPilotConfig.GROUP, PINNED_KEY);
				}
				else
				{
					configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, PINNED_KEY, bossKey);
				}
				evaluate();
			});
		}

		@Override
		public void note(String bossKey, String text)
		{
			clientThread.invokeLater(() ->
			{
				String t = text == null ? "" : text.trim();
				if (t.length() > NOTE_MAX)
				{
					t = t.substring(0, NOTE_MAX);
				}
				if (t.isEmpty())
				{
					configManager.unsetRSProfileConfiguration(CodexBossPilotConfig.GROUP, NOTE_KEY + bossKey);
				}
				else
				{
					configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, NOTE_KEY + bossKey, t);
				}
				evaluate();
			});
		}

		@Override
		public void aim(Aim aim)
		{
			configManager.setConfiguration(CodexBossPilotConfig.GROUP, CodexBossPilotConfig.AIM_KEY, aim);
		}
	}
}
