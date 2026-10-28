package com.codex.bosspilot;

import com.codex.bosspilot.combat.Fighter;
import com.codex.bosspilot.combat.Planner;
import com.codex.bosspilot.combat.Result;
import com.codex.bosspilot.combat.SlayerTask;
import com.codex.bosspilot.core.Account;
import com.codex.bosspilot.core.Aim;
import com.codex.bosspilot.core.BankSnapshot;
import com.codex.bosspilot.core.Boss;
import com.codex.bosspilot.core.Bosses;
import com.codex.bosspilot.core.Chat;
import com.codex.bosspilot.core.KitCheck;
import com.codex.bosspilot.core.Logbook;
import com.codex.bosspilot.core.Outlook;
import com.codex.bosspilot.core.Ranker;
import com.codex.bosspilot.core.Reminders;
import com.codex.bosspilot.core.Review;
import com.codex.bosspilot.core.Setup;
import com.codex.bosspilot.core.Stock;
import com.codex.bosspilot.core.Supply;
import com.codex.bosspilot.core.Trip;
import com.codex.bosspilot.core.TripTracker;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import net.runelite.api.GameState;
import net.runelite.api.Item;
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
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
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
	private Boss nearby;
	private String selected;
	private boolean warned;

	/** Names of item ids, looked up once. Client thread only. */
	private final Map<Integer, String> names = new HashMap<>();

	// The setups, worked out off the client thread whenever what they depend on changes.
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
		setups = Collections.emptyMap();
		setupsKey = null;
		review = null;
	}

	// ---- Events ----

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		ticks++;
		nearby = findNearby();
		Stock pack = carried;
		tracker.observe(nearby, pack.foodHeal(), pack.doses(PRAYER_POTIONS), System.currentTimeMillis());
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
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			tracker.end();
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
		if (CodexBossPilotConfig.GROUP.equals(event.getGroup()) && !Logbook.KEY.equals(event.getKey()) && !BANK_KEY.equals(event.getKey()))
		{
			if ("boost".equals(event.getKey()))
			{
				setupsKey = null;
			}
			clientThread.invokeLater(this::evaluate);
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
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		String message = Text.removeTags(event.getMessage());
		long now = System.currentTimeMillis();
		Chat.Kc kc = Chat.kc(message);
		if (kc != null)
		{
			Boss boss = Bosses.byKc(kc.name);
			if (boss != null)
			{
				logbook.noteKc(boss, kc.count);
				tracker.kill(boss, now);
				saveLogbook();
				saveFinished();
			}
			return;
		}
		double seconds = Chat.duration(message);
		if (seconds > 0)
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
		NPC npc = event.getNpc();
		Boss boss = npc == null ? null : Bosses.byNpc(npc.getName());
		if (boss != null)
		{
			tracker.loot(boss, value(event.getItems()), System.currentTimeMillis());
		}
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		// Chest loot, such as the Barrows chest, comes as an event rather than an NPC drop.
		Trip t = tracker.current();
		if (t != null && t.getBoss().getLootEvent() != null && t.getBoss().getLootEvent().equalsIgnoreCase(event.getName()))
		{
			tracker.loot(t.getBoss(), value(event.getItems()), System.currentTimeMillis());
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		// Only hits the player takes, for the fight review; the boss's own hitsplats are never read.
		if (event.getActor() == client.getLocalPlayer() && event.getHitsplat().getHitsplatType() != HitsplatID.HEAL)
		{
			tracker.hit(event.getHitsplat().getAmount());
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			tracker.death();
		}
	}

	// ---- Reading ----

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

	/** What the player carries and wears, by name for the kit and by id for the setups. */
	private void readCarried()
	{
		Map<String, Long> byName = new HashMap<>();
		Set<Integer> ids = new HashSet<>();
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
			}
		}
		carried = Stock.of(byName);
		carriedIds = ids;
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
		BankSnapshot own = BankSnapshot.parse(gson, configManager.getRSProfileConfiguration(CodexBossPilotConfig.GROUP, BANK_KEY));
		BankSnapshot steward = own != null ? null
			: BankSnapshot.parse(gson, configManager.getRSProfileConfiguration(STEWARD_GROUP, STEWARD_KEY));
		BankSnapshot snap = own != null ? own : steward;
		bankItems = snap == null ? Collections.emptyMap() : new HashMap<>(snap.getItems());
		bankSeenAt = snap == null ? 0 : snap.getSavedAt();
		bankStock = stockOf(bankItems);
		selected = null;
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
		Outlook outlook = trip == null ? Outlook.UNKNOWN : Outlook.of(trip, logbook.rates(trip.getBoss()));
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

		List<Reminders.Reminder> reminders = loggedIn ? reminders(trip) : Collections.emptyList();
		String tripNote = trip == null ? "" : note(trip.getBoss());
		tripPlaque.show(loggedIn && trip != null && config.tripPlaque() ? Plaques.trip(trip, outlook, logbook, reminders, tripNote, now) : null);
		kitPlaque.show(loggedIn && trip == null && next != null && config.bankPlaque() && bankOpen()
			? Plaques.kit(next.getBoss().getName(), next.getKit()) : null);

		BossPanel p = panel;
		if (p == null)
		{
			return;
		}
		List<Setup> shownSetups = shown == null ? Collections.emptyList()
			: setups.getOrDefault(shown.getBoss().getKey(), Collections.emptyList());
		View view = new View(loggedIn, picks, next, shown, shownSetups, setupsPending && shownSetups.isEmpty(), trip, outlook, logbook,
			review, in.aim, bankSeenAt, now, reminders, shown == null ? "" : note(shown.getBoss()));
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
							Double.isInfinite(secs) ? -1 : secs, r.getMaxHit(), r.getStyle(), r.getPrayer(), r.getSpell(), gear));
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
		Outlook end = Outlook.of(done, null);
		review = Review.of(done.getBoss(), record, end.getKills());
		if (record.kills > 0 || record.deaths > 0)
		{
			logbook.add(done.getBoss(), record);
		}
		saveLogbook();
	}

	private void saveLogbook()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(CodexBossPilotConfig.GROUP, Logbook.KEY, logbook.toJson(gson));
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
