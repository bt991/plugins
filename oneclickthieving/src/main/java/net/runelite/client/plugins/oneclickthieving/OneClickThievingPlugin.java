package net.runelite.client.plugins.oneclickthieving;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.queries.BankItemQuery;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.queries.NPCQuery;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Extension
@PluginDescriptor(name="One Click Pickpocket", description="QOL for pickpocketing", tags={"sundar", "pajeet", "pickpocket", "skilling", "thieving"}, enabledByDefault=false)
public class OneClickThievingPlugin
        extends Plugin {
    private static final Logger log = LoggerFactory.getLogger(OneClickThievingPlugin.class);
    @Inject
    private Client client;
    @Inject
    private ItemManager itemManager;
    @Inject
    private OneClickThievingConfig config;
    @Inject
    private Notifier notifier;
    @Inject
    private ChatMessageManager chatMessageManager;
    @Inject
    private ConfigManager configManager;
    @Inject
    private OverlayManager overlayManager;
    Set<String> foodMenuOption = Set.of("Drink", "Eat");
    Set<Integer> prayerPotionIDs = Set.of(139, 141, 143, 2434, 3024, 3026, 3028, 3030, 189, 191, 193, 2450, 26340, 26342, 26344, 26346);
    Set<Integer> foodBlacklist = Set.of(139, 141, 143, 2434, 3024, 3026, 3028, 3030, 24774, 189, 191, 193, 2450, 26340, 26342, 26344, 26346);
    Set<Integer> coinPouches = Set.of(22521, 22522, 22523, 22524, 22525, 22526, 22527, 22528, 22529, 22530, 22531, 22532, 22533, 22534, 22535, 22536, 22537, 22538, 24703);
    private boolean shouldHeal = false;
    private int prayerTimeOut = 0;
    private int ardyBankId = 10355;
    private static final int DODGY_NECKLACE_ID = 21143;

    @Provides
    OneClickThievingConfig provideConfig(ConfigManager configManager) {
        return (OneClickThievingConfig)configManager.getConfig(OneClickThievingConfig.class);
    }

    protected void startUp() {
    }

    protected void shutDown() {
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getMessage().contains("You have run out of prayer points")) {
            this.prayerTimeOut = 0;
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (event.getSkill() == Skill.PRAYER && event.getBoostedLevel() == 0 && this.prayerTimeOut == 0) {
            this.prayerTimeOut = 10;
        }
    }

    @Subscribe
    private void onClientTick(ClientTick event) {
        if (!this.config.clickOverride() || this.client.getLocalPlayer() == null || this.client.getGameState() != GameState.LOGGED_IN || this.client.isMenuOpen() || this.client.getWidget(378, 78) != null) {
            return;
        }
        this.client.insertMenuItem("One Click Pickpocket", "", MenuAction.UNKNOWN.getId(), 0, 0, 0, true);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        block7: {
            block8: {
                if (!this.config.clickOverride() || !event.getMenuOption().equals("One Click Pickpocket")) break block7;
                NPC npc = (NPC)((Object)new NPCQuery().idEquals(new int[]{this.config.npcID()}).result(this.client).nearestTo(this.client.getLocalPlayer()));
                if (npc == null) break block8;
                this.setEntry(event, this.client.createMenuEntry("Pickpocket", "Pickpocket", npc.getIndex(), MenuAction.NPC_THIRD_OPTION.getId(), 0, 0, false));
                switch (this.getActions(npc).indexOf("Pickpocket")) {
                    case 0: {
                        event.setMenuAction(MenuAction.NPC_FIRST_OPTION);
                        break block7;
                    }
                    case 1: {
                        event.setMenuAction(MenuAction.NPC_SECOND_OPTION);
                        break block7;
                    }
                    case 2: {
                        event.setMenuAction(MenuAction.NPC_THIRD_OPTION);
                        break block7;
                    }
                    case 3: {
                        event.setMenuAction(MenuAction.NPC_FOURTH_OPTION);
                        break block7;
                    }
                    case 4: {
                        event.setMenuAction(MenuAction.NPC_FIFTH_OPTION);
                        break block7;
                    }
                    default: {
                        this.sendGameMessage("Did not find pickpocket option on npc, check configs");
                        event.consume();
                        return;
                    }
                }
            }
            this.sendGameMessage("Npc not found please change the id");
            event.consume();
            return;
        }
        this.changeMenuAction(event);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (this.prayerTimeOut > 0) {
            --this.prayerTimeOut;
        }
        if (this.client.getBoostedSkillLevel(Skill.HITPOINTS) >= Math.min(this.client.getRealSkillLevel(Skill.HITPOINTS), this.config.HPTopThreshold())) {
            this.shouldHeal = false;
        } else if (this.client.getBoostedSkillLevel(Skill.HITPOINTS) <= Math.max(5, this.config.HPBottomThreshold())) {
            this.shouldHeal = true;
        }
    }

    private List<String> getActions(NPC npc) {
        return Arrays.stream(npc.getComposition().getActions()).map(o -> o == null ? null : Text.removeTags(o)).collect(Collectors.toList());
    }

    private void changeMenuAction(MenuOptionClicked event) {
        if (this.config.disableWalk() && event.getMenuOption().equals("Walk here")) {
            event.consume();
            return;
        }
        if (!event.getMenuOption().equals("Pickpocket")) {
            return;
        }
        Widget coinpouch = this.getItem(this.coinPouches);
        if (this.config.enableHeal() && this.shouldHeal) {
            Widget food = this.getItemMenu(this.foodMenuOption, this.foodBlacklist);
            if (this.config.haltOnLowFood() && food == null) {
                if (!this.config.bankArdyKnights()) {
                    event.consume();
                    this.notifier.notify("You are out of food");
                    this.sendGameMessage("You are out of food");
                    return;
                }
                if (!this.bankOpen()) {
                    event.setMenuEntry(this.bankMES(this.ardyBankId));
                } else {
                    if (!this.bankOpen()) {
                        event.consume();
                        this.notifier.notify("You are out of food");
                        this.sendGameMessage("You are out of food");
                        return;
                    }
                    event.setMenuEntry(this.withdrawX(this.config.bankArdyKnightsID()));
                }
            } else if (food != null) {
                this.setEntry(event, this.itemEntry(food, 2));
                return;
            }
        }
        if (this.config.enableCoinPouch() && coinpouch != null && coinpouch.getItemQuantity() > 28) {
            this.setEntry(event, this.itemEntry(coinpouch, 2));
            return;
        }
        if (this.config.enableNecklace() && this.getItem(List.of(Integer.valueOf(21143))) != null && !this.isItemEquipped(List.of(Integer.valueOf(21143)))) {
            this.setEntry(event, this.itemEntry(this.getItem(List.of(Integer.valueOf(21143))), 3));
            return;
        }
        if (this.config.enableSpell() && this.client.getVarbitValue(12414) == 0) {
            if (this.client.getVarbitValue(4070) != 3) {
                event.consume();
                this.notifier.notify("You are on the wrong spellbook");
                this.sendGameMessage("You are on the wrong spellbook");
                return;
            }
            if (this.client.getBoostedSkillLevel(Skill.MAGIC) >= 47) {
                this.setEntry(event, this.client.createMenuEntry("Cast", "Shadow Veil", 1, MenuAction.CC_OP.getId(), -1, WidgetInfo.SPELL_SHADOW_VEIL.getId(), false));
                return;
            }
            event.consume();
            this.notifier.notify("Magic level too low to cast this spell!");
            this.sendGameMessage("Magic level too low to cast this spell!");
            return;
        }
        if (!this.config.enablePray()) {
            return;
        }
        if (this.client.getBoostedSkillLevel(Skill.PRAYER) == 0 && this.prayerTimeOut == 0) {
            Widget prayerPotion = this.getItem(this.prayerPotionIDs);
            if (prayerPotion != null) {
                this.setEntry(event, this.itemEntry(prayerPotion, 2));
                return;
            }
            if (!this.config.haltOnLowFood()) {
                return;
            }
            event.consume();
            this.notifier.notify("You are out of prayer potions");
            this.sendGameMessage("You are out of prayer potions");
            return;
        }
        if (this.client.getVarbitValue(4120) != 0) {
            return;
        }
        if (this.client.getBoostedSkillLevel(Skill.PRAYER) <= 0) {
            return;
        }
        if (!(this.config.prayMethod() == PrayMethod.REACTIVE_PRAY && this.shouldPray() || this.config.prayMethod() == PrayMethod.LAZY_PRAY)) {
            return;
        }
        this.setEntry(event, this.client.createMenuEntry("Activate", "Redemption", 1, MenuAction.CC_OP.getId(), -1, WidgetInfo.PRAYER_REDEMPTION.getId(), false));
    }

    private MenuEntry withdrawX(int itemID) {
        if (this.getBankIndex(itemID) == -1) {
            return null;
        }
        return this.createMenuEntry(5, MenuAction.CC_OP, this.getBankIndex(itemID), 786445, true);
    }

    private int getBankIndex(int id) {
        WidgetItem bankItem = (WidgetItem)new BankItemQuery().idEquals(new int[]{id}).result(this.client).first();
        if (bankItem == null) {
            return -1;
        }
        return bankItem.getWidget().getIndex();
    }

    private MenuEntry bankMES(int bankID) {
        GameObject bank = this.getGameObject(bankID);
        return this.createMenuEntry(bank.getId(), MenuAction.GAME_OBJECT_SECOND_OPTION, this.getLocation(bank).getX(), this.getLocation(bank).getY(), false);
    }

    public MenuEntry createMenuEntry(int identifier, MenuAction type, int param0, int param1, boolean forceLeftClick) {
        return this.client.createMenuEntry(0).setOption("").setTarget("").setIdentifier(identifier).setType(type).setParam0(param0).setParam1(param1).setForceLeftClick(forceLeftClick);
    }

    private GameObject getGameObject(int id) {
        return (GameObject)((Object)((GameObjectQuery)new GameObjectQuery().idEquals(new int[]{id})).result(this.client).nearestTo(this.client.getLocalPlayer()));
    }

    private Point getLocation(TileObject tileObject) {
        if (tileObject == null) {
            return new Point(0, 0);
        }
        if (tileObject instanceof GameObject) {
            return ((GameObject)tileObject).getSceneMinLocation();
        }
        return new Point(tileObject.getLocalLocation().getSceneX(), tileObject.getLocalLocation().getSceneY());
    }

    private boolean bankOpen() {
        return this.client.getItemContainer(InventoryID.BANK) != null;
    }

    private boolean shouldPray() {
        return this.client.getBoostedSkillLevel(Skill.HITPOINTS) < 11;
    }

    public boolean isItemEquipped(Collection<Integer> itemIds) {
        assert (this.client.isClientThread());
        ItemContainer equipmentContainer = this.client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipmentContainer != null) {
            Item[] items;
            for (Item item : items = equipmentContainer.getItems()) {
                if (!itemIds.contains(item.getId())) continue;
                return true;
            }
        }
        return false;
    }

    private Widget getItem(Collection<Integer> ids) {
        ArrayList<Widget> matches = this.getItems(ids);
        return matches.size() != 0 ? matches.get(0) : null;
    }

    private ArrayList<Widget> getItems(Collection<Integer> ids) {
        this.client.runScript(new Object[]{6009, 0x950000, 28, 1, -1});
        Widget inventoryWidget = this.client.getWidget(WidgetInfo.INVENTORY);
        ArrayList<Widget> matchedItems = new ArrayList<Widget>();
        if (inventoryWidget != null && inventoryWidget.getDynamicChildren() != null) {
            Widget[] items;
            for (Widget item : items = inventoryWidget.getDynamicChildren()) {
                if (!ids.contains(item.getItemId())) continue;
                matchedItems.add(item);
            }
        }
        return matchedItems;
    }

    private MenuEntry itemEntry(Widget item, int action) {
        if (item == null) {
            return null;
        }
        return this.client.createMenuEntry("", "", action, action < 6 ? MenuAction.CC_OP.getId() : MenuAction.CC_OP_LOW_PRIORITY.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId(), false);
    }

    private void setEntry(MenuOptionClicked event, MenuEntry entry) {
        try {
            event.setMenuOption(entry.getOption());
            event.setMenuTarget(entry.getTarget());
            event.setId(entry.getIdentifier());
            event.setMenuAction(entry.getType());
            event.setParam0(entry.getParam0());
            event.setParam1(entry.getParam1());
        }
        catch (Exception e) {
            event.consume();
        }
    }

    private Widget getItemMenu(Collection<String> menuOptions, Collection<Integer> ignoreIDs) {
        this.client.runScript(new Object[]{6009, 0x950000, 28, 1, -1});
        Widget inventoryWidget = this.client.getWidget(WidgetInfo.INVENTORY);
        if (inventoryWidget != null && inventoryWidget.getChildren() != null) {
            Widget[] items;
            for (Widget item : items = inventoryWidget.getChildren()) {
                String[] menuActions;
                if (ignoreIDs.contains(item.getItemId())) continue;
                for (String action : menuActions = this.itemManager.getItemComposition(item.getItemId()).getInventoryActions()) {
                    if (action == null || !menuOptions.contains(action)) continue;
                    return item;
                }
            }
        }
        return null;
    }

    private void sendGameMessage(String message) {
        String chatMessage = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(message).build();
        this.chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(chatMessage).build());
    }
}