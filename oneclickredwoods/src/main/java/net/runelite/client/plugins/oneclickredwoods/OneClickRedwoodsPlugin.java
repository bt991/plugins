package net.runelite.client.plugins.oneclickredwoods;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.VarPlayer;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.oneclickredwoods.OneClickRedwoodsConfig;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
@PluginDescriptor(name="One Click Redwoods", enabledByDefault=false, description="Chops and banks redwoods. Drops logs if option is ticked.")
public class OneClickRedwoodsPlugin
        extends Plugin {
    private static final Logger log = LoggerFactory.getLogger(OneClickRedwoodsPlugin.class);
    List<Integer> NESTS = Arrays.asList(5070, 5071, 5072, 5073, 5074, 5075, 22798);
    List<Integer> LOGS = Arrays.asList(19669);
    List<Integer> CHOPPING_ANIMATION = Arrays.asList(2846, 867, 2117, 8324);
    List<TileItem> GroundItems = new ArrayList<TileItem>();
    private final int DOWN_LADDER = 34478;
    private final int UP_LADDER = 34477;
    private final int DEPOSIT_BOX = 25937;
    @Inject
    private Client client;
    @Inject
    private OneClickRedwoodsConfig config;

    @Provides
    OneClickRedwoodsConfig provideConfig(ConfigManager configManager) {
        return (OneClickRedwoodsConfig)configManager.getConfig(OneClickRedwoodsConfig.class);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuOption().equals("<col=00ff00>One Click Redwoods")) {
            this.handleClick(event);
        }
    }

    @Subscribe
    private void onClientTick(ClientTick event) {
        if (this.client.getLocalPlayer() == null || this.client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        String text = "<col=00ff00>One Click Redwoods";
        this.client.insertMenuItem(text, "", MenuAction.UNKNOWN.getId(), 0, 0, 0, true);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
    }

    @Subscribe
    private void onItemSpawned(ItemSpawned event) {
        for (TileItem item : this.GroundItems) {
            if (item != event.getItem()) continue;
            return;
        }
        for (Integer nest : this.NESTS) {
            if (!nest.equals(event.getItem().getId())) continue;
            this.GroundItems.add(event.getItem());
        }
    }

    @Subscribe
    private void onItemDespawned(ItemDespawned event) {
        for (Integer nest : this.NESTS) {
            if (!nest.equals(event.getItem().getId())) continue;
            this.GroundItems.remove(event.getItem());
        }
    }

    private void handleClick(MenuOptionClicked event) {
        if (this.isNearDeposit()) {
            if (this.client.getLocalPlayer().isMoving() || this.client.getLocalPlayer().getPoseAnimation() != this.client.getLocalPlayer().getIdlePoseAnimation() || this.client.getLocalPlayer().getAnimation() == 828) {
                event.consume();
                return;
            }
            if (this.client.getWidget(0xC00000) != null && this.getEmptySlots() == 0) {
                this.setMenuEntry(event, this.depositAllMES());
                return;
            }
            if (this.getEmptySlots() == 0) {
                this.setMenuEntry(event, this.bankMES());
                return;
            }
            this.setMenuEntry(event, this.upLadder());
        }
        if (this.isNearTree()) {
            if (this.config.useSpec() && this.client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) == 1000) {
                this.setMenuEntry(event, this.specAtk());
                return;
            }
            if (this.CHOPPING_ANIMATION.contains(this.client.getLocalPlayer().getAnimation()) || this.client.getLocalPlayer().isMoving() || this.client.getLocalPlayer().getAnimation() == 827 || this.client.getLocalPlayer().getAnimation() == 2876) {
                event.consume();
                return;
            }
            if (this.getEmptySlots() > 0) {
                this.setMenuEntry(event, this.chopRedwoods());
                return;
            }
            if (this.config.useBank()) {
                this.setMenuEntry(event, this.downLadder());
                return;
            }
        }
    }

    private MenuEntry chopRedwoods() {
        GameObject tree = this.getRedwoodSpot();
        return this.createMenuEntry(tree.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, this.getLocation(tree).getX(), this.getLocation(tree).getY(), true);
    }

    private MenuEntry pickUpBirdNest() {
        if (!this.GroundItems.isEmpty()) {
            TileItem tileItem = this.getNearestTileItem(this.GroundItems);
            return this.createMenuEntry(this.getNearestTileItem(this.GroundItems).getId(), MenuAction.GROUND_ITEM_THIRD_OPTION, tileItem.getTile().getSceneLocation().getX(), tileItem.getTile().getSceneLocation().getY(), true);
        }
        return null;
    }

    private MenuEntry specAtk() {
        Widget specAtk = this.client.getWidget(WidgetInfo.MINIMAP_SPEC_CLICKBOX);
        return this.createMenuEntry(1, MenuAction.CC_OP, -1, specAtk.getId(), false);
    }

    private MenuEntry dropLogs(Widget logs) {
        return this.createMenuEntry(7, MenuAction.CC_OP_LOW_PRIORITY, logs.getIndex(), WidgetInfo.INVENTORY.getId(), false);
    }

    private MenuEntry bankMES() {
        return this.createMenuEntry(25937, MenuAction.GAME_OBJECT_FIRST_OPTION, this.getLocation(this.getGameObject(25937)).getX(), this.getLocation(this.getGameObject(25937)).getY(), false);
    }

    private MenuEntry depositAllMES() {
        return this.createMenuEntry(1, MenuAction.CC_OP, -1, 0xC00004, false);
    }

    private MenuEntry downLadder() {
        GameObject ladder = this.getGameObject(34478);
        return this.createMenuEntry(ladder.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, this.getLocation(ladder).getX(), this.getLocation(ladder).getY(), true);
    }

    private MenuEntry upLadder() {
        GameObject ladder = this.getGameObject(34477);
        return this.createMenuEntry(ladder.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, this.getLocation(ladder).getX(), this.getLocation(ladder).getY(), true);
    }

    private boolean isNearTree() {
        WorldArea AREA = new WorldArea(new WorldPoint(1224, 3750, 1), new WorldPoint(1233, 3759, 1));
        return this.client.getLocalPlayer().getWorldLocation().isInArea(new WorldArea[]{AREA});
    }

    private boolean isNearDeposit() {
        WorldArea AREA = new WorldArea(new WorldPoint(1222, 3747, 0), new WorldPoint(1240, 3765, 0));
        return this.client.getLocalPlayer().getWorldLocation().isInArea(new WorldArea[]{AREA});
    }

    private GameObject getRedwoodSpot() {
        HashMap<Integer, Integer> Trees = new HashMap<Integer, Integer>();
        Trees.put(34637, 34314);
        Trees.put(34633, 34312);
        Trees.put(34639, 34315);
        Trees.put(34635, 34313);
        ArrayList<Integer> ChoppableTrees = new ArrayList<Integer>();
        for (Integer gameObjectID : Trees.keySet()) {
            if (this.client.getObjectDefinition(gameObjectID).getImpostor().getId() == ((Integer)Trees.get(gameObjectID)).intValue()) continue;
            ChoppableTrees.add(gameObjectID);
        }
        return (GameObject)((Object)((GameObjectQuery)new GameObjectQuery().idEquals(ChoppableTrees)).result(this.client).nearestTo(this.client.getLocalPlayer()));
    }

    private GameObject getGameObject(int ID) {
        return (GameObject)((Object)((GameObjectQuery)new GameObjectQuery().idEquals(new int[]{ID})).result(this.client).nearestTo(this.client.getLocalPlayer()));
    }

    private TileItem getNearestTileItem(List<TileItem> tileItems) {
        if (tileItems.size() == 0 || tileItems.get(0) == null) {
            return null;
        }
        return tileItems.stream().min(Comparator.comparing(tileItem -> tileItem.getTile().getWorldLocation().distanceTo(this.client.getLocalPlayer().getWorldLocation()))).orElse(null);
    }

    private Point getLocation(TileObject tileObject) {
        if (tileObject instanceof GameObject) {
            return ((GameObject)tileObject).getSceneMinLocation();
        }
        return new Point(tileObject.getLocalLocation().getSceneX(), tileObject.getLocalLocation().getSceneY());
    }

    private Widget getInventoryItem(int id) {
        this.client.runScript(new Object[]{6009, 0x950000, 28, 1, -1});
        Widget inventoryWidget = this.client.getWidget(WidgetInfo.INVENTORY);
        Widget bankInventoryWidget = this.client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);
        if (bankInventoryWidget != null && !bankInventoryWidget.isHidden()) {
            return this.getWidgetItem(bankInventoryWidget, id);
        }
        if (inventoryWidget != null) {
            return this.getWidgetItem(inventoryWidget, id);
        }
        return null;
    }

    private Widget getWidgetItem(Widget widget, int id) {
        for (Widget item : widget.getDynamicChildren()) {
            if (item.getItemId() != id) continue;
            return item;
        }
        return null;
    }

    private Widget getLastInventoryItem(int id) {
        Widget inventoryWidget = this.client.getWidget(WidgetInfo.INVENTORY);
        if (inventoryWidget != null && !inventoryWidget.isHidden()) {
            return this.getLastWidgetItem(inventoryWidget, id);
        }
        return null;
    }

    private Widget getLastWidgetItem(Widget widget, int id) {
        return Arrays.stream(widget.getDynamicChildren()).filter(item -> item.getItemId() == id).reduce((first, second) -> second).orElse(null);
    }

    private int getEmptySlots() {
        Widget inventory = this.client.getWidget(WidgetInfo.INVENTORY.getId());
        Widget bankInventory = this.client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId());
        if (bankInventory != null && !bankInventory.isHidden() && bankInventory.getDynamicChildren() != null) {
            return this.getEmptySlots(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);
        }
        if (inventory != null && inventory.getDynamicChildren() != null) {
            return this.getEmptySlots(WidgetInfo.INVENTORY);
        }
        return -1;
    }

    private int getEmptySlots(WidgetInfo widgetInfo) {
        this.client.runScript(new Object[]{6009, 0x950000, 28, 1, -1});
        List<Widget> inventoryItems = Arrays.asList(this.client.getWidget(widgetInfo.getId()).getDynamicChildren());
        return (int)inventoryItems.stream().filter(item -> item.getItemId() == 6512).count();
    }

    private void setMenuEntry(MenuOptionClicked event, MenuEntry menuEntry) {
        event.setId(menuEntry.getIdentifier());
        event.setMenuAction(menuEntry.getType());
        event.setParam0(menuEntry.getParam0());
        event.setParam1(menuEntry.getParam1());
    }

    public MenuEntry createMenuEntry(int identifier, MenuAction type, int param0, int param1, boolean forceLeftClick) {
        return this.client.createMenuEntry(0).setOption("").setTarget("").setIdentifier(identifier).setType(type).setParam0(param0).setParam1(param1).setForceLeftClick(forceLeftClick);
    }

    private void Print(String string) {
        if (this.config.debug()) {
            this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", string, "");
        }
    }
}