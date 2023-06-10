package net.runelite.client.plugins.oneclicktithe;

import com.google.inject.Inject;
import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.queries.GroundObjectQuery;
import net.runelite.api.queries.WallObjectQuery;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.coords.ScenePoint;
import net.unethicalite.api.game.Game;
import org.checkerframework.checker.units.qual.A;
import org.pf4j.Extension;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Extension
@PluginDescriptor(
        name = "One Click Tithe",
        description = "Start in tithe farm lobby.",
        tags = {"one","click","tithe","farm"},
        enabledByDefault = false
)
public class OneClickTithePlugin extends Plugin
{
    private int tickCounter = 0;
    private int canCharges = 0;
    private int SEED_TABLE_ID = 27430;
    private int FARM_DOOR_ID = 27445;
    private int WATER_BARREL_ID = 5598;
    private int EMPTY_PATCH_ID = 27383;
    private int SEEDED_GOL_PATCH = 27384;
    private int DEPOSIT_SACK_ID = 27431;
    private static final List<Integer> seedIDs = Arrays.asList(
            ItemID.GOLOVANOVA_SEED,ItemID.BOLOGANO_SEED,ItemID.LOGAVANO_SEED);

    private static final List<Integer> fruitIDs = Arrays.asList(
            ItemID.GOLOVANOVA_FRUIT, ItemID.BOLOGANO_FRUIT, ItemID.LOGAVANO_FRUIT
    );
    private static final List<Integer> wateringCans = Arrays.asList(
            ItemID.WATERING_CAN,ItemID.WATERING_CAN1,ItemID.WATERING_CAN2,ItemID.WATERING_CAN3,
            ItemID.WATERING_CAN4,ItemID.WATERING_CAN5,ItemID.WATERING_CAN6,ItemID.WATERING_CAN7);
    private static final List<Integer> useableCans = Arrays.asList(
            ItemID.WATERING_CAN1,ItemID.WATERING_CAN2,ItemID.WATERING_CAN3,ItemID.WATERING_CAN4,
            ItemID.WATERING_CAN5,ItemID.WATERING_CAN6,ItemID.WATERING_CAN7,ItemID.WATERING_CAN8);
    private static final List<Integer> PLANTS_FIRST_STAGE_UNWATERED = Arrays.asList(
            ObjectID.GOLOVANOVA_SEEDLING, ObjectID.BOLOGANO_SEEDLING, ObjectID.LOGAVANO_SEEDLING);
    private static final List<Integer> PLANTS_FIRST_STAGE_WATERED = Arrays.asList(
            ObjectID.GOLOVANOVA_SEEDLING_27385, ObjectID.BOLOGANO_SEEDLING_27396, ObjectID.LOGAVANO_SEEDLING_27407);

    private static final List<Integer> PLANTS_SECOND_STAGE_UNWATERED = Arrays.asList(
            ObjectID.GOLOVANOVA_PLANT, ObjectID.BOLOGANO_PLANT, ObjectID.LOGAVANO_PLANT);
    private static final List<Integer> PLANTS_SECOND_STAGE_WATERED = Arrays.asList(
            ObjectID.GOLOVANOVA_PLANT_27388, ObjectID.BOLOGANO_PLANT_27399, ObjectID.LOGAVANO_PLANT_27410);

    private static final List<Integer> PLANTS_THIRD_STAGE_UNWATERED = Arrays.asList(
            ObjectID.GOLOVANOVA_PLANT_27390, ObjectID.BOLOGANO_PLANT_27401, ObjectID.LOGAVANO_PLANT_27412);
    private static final List<Integer> PLANTS_THIRD_STAGE_WATERED = Arrays.asList(
            ObjectID.GOLOVANOVA_PLANT_27391, ObjectID.BOLOGANO_PLANT_27402, ObjectID.LOGAVANO_PLANT_27413);

    private static final List<Integer> PLANTS_GROWN = Arrays.asList(
            ObjectID.GOLOVANOVA_PLANT_27393, ObjectID.BOLOGANO_PLANT_27404, ObjectID.LOGAVANO_PLANT_27415);

    private static final List<Integer> DEAD_PLANTS1 = Arrays.asList(
            ObjectID.BLIGHTED_GOLOVANOVA_SEEDLING, ObjectID.BLIGHTED_BOLOGANO_SEEDLING, ObjectID.BLIGHTED_LOGAVANO_SEEDLING
    );
    private static final List<Integer> DEAD_PLANTS2 = Arrays.asList(
            ObjectID.BLIGHTED_GOLOVANOVA_PLANT, ObjectID.BLIGHTED_BOLOGANO_PLANT, ObjectID.BLIGHTED_LOGAVANO_PLANT
    );
    private static final List<Integer> DEAD_PLANTS3 = Arrays.asList(
            ObjectID.BLIGHTED_GOLOVANOVA_PLANT_27392, ObjectID.BLIGHTED_BOLOGANO_PLANT_27403, ObjectID.BLIGHTED_LOGAVANO_PLANT_27414
    );
    private static final List<Integer> DEAD_PLANTS4 = Arrays.asList(
            ObjectID.BLIGHTED_GOLOVANOVA_PLANT_27394, ObjectID.BLIGHTED_BOLOGANO_PLANT_27405, ObjectID.BLIGHTED_LOGAVANO_PLANT_27416
    );
    private static final List<Integer> GOLOVANOVA_IDS = Arrays.asList(27384,27385,27386,27387,27388,27389,27390,27391,27392,27393, 27394);
    private static final List<Integer> BOLOGANO_IDS = Arrays.asList(27395,27396,27397,27398,27399,27400,27401,27402,27403,27404,27405);
    private static final List<Integer> LOGAVANO_IDS = Arrays.asList(27406,27407,27408,27409,27410,27411,27412,27413,27414,27415,27416);

    public LinkedHashMap<WorldPoint, GameObject> EAST_PATCHES = new LinkedHashMap<>();
    public LinkedHashMap<WorldPoint, GameObject> WEST_PATCHES = new LinkedHashMap<>();

    public int seedID = 0;

    private boolean keepFilling = false;
    @Inject
    private Client client;

    @Inject
    private OneClickTitheConfig config;

    @Inject
    private OneClickTitheOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Provides
    OneClickTitheConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OneClickTitheConfig.class);
    }


    @Override
    protected void startUp()
    {
        keepFilling = false;
        seedID = config.seeds().ID;
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
    }

    @Subscribe
    private void onClientTick(ClientTick event) {
        if (client.getLocalPlayer() == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (client.getLocalPlayer() == null || client.getGameState() != GameState.LOGGED_IN) return;
        String text = "<col=00ff00>One Click Tithe";
        client.insertMenuItem(text, "", MenuAction.UNKNOWN.getId(), 0, 0, 0, true);
        client.setTempMenuEntry(Arrays.stream(client.getMenuEntries()).filter(x->x.getOption().equals(text)).findFirst().orElse(null));
    }

    @Subscribe
    private void onGameTick(GameTick tick){
        if(tickCounter > 0){
            tickCounter--;
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event){
        if(event.getActor() instanceof Player){
            Player p = (Player) event.getActor();
            if(p == client.getLocalPlayer()){
                if(tickCounter == 0){
                    if(event.getActor().getAnimation() == 2293){
                            tickCounter = 2;
                    }
                    if(event.getActor().getAnimation() == 830){
                            tickCounter = 1;
                    }
                }

            }
         }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event){
        //System.out.println("New object spawned, loc -> " + event.getGameObject().getWorldLocation() + "  Name -> " + event.getGameObject().getName() + "  ID -> " +event.getGameObject().getId());
        if((GOLOVANOVA_IDS.contains(event.getGameObject().getId()) || event.getGameObject().getId() == EMPTY_PATCH_ID) && config.seeds().ItemID == ItemID.GOLOVANOVA_SEED){
            updatePatches(event.getGameObject(), GOLOVANOVA_IDS);
        }
        else if((BOLOGANO_IDS.contains(event.getGameObject().getId()) || event.getGameObject().getId() == EMPTY_PATCH_ID) && config.seeds().ItemID == ItemID.BOLOGANO_SEED){
            updatePatches(event.getGameObject(), BOLOGANO_IDS);
        }
        else if((LOGAVANO_IDS.contains(event.getGameObject().getId()) || event.getGameObject().getId() == EMPTY_PATCH_ID) && config.seeds().ItemID == ItemID.LOGAVANO_SEED){
            updatePatches(event.getGameObject(), LOGAVANO_IDS);
        }

    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        MenuEntry entry = event.getMenuEntry();
        MenuAction action = event.getMenuAction();
        //return createMenuEntry(
        //                patch.getId(),
        //                MenuAction.WIDGET_TARGET_ON_GAME_OBJECT,
        //                getLocation(patch).getX(),
        //                getLocation(patch).getY(),
        //                false);

        //System.out.println("Identifier -> " + entry.getIdentifier() + "  Item id -> " + entry.getItemId());
       // System.out.println("Menu action -> " + action);
       //System.out.println("Param 0 -> " + entry.getParam0());
       // System.out.println("Param 1 -> " + entry.getParam1());
        if (event.getMenuOption().equals("<col=00ff00>One Click Tithe"))
        {
            handleClick(event);
        }
    }

    private void handleClick(MenuOptionClicked event) {
        if (isInLobby()) {
            EAST_PATCHES.clear();
            if (client.getLocalPlayer().isMoving()
                    || client.getLocalPlayer().getPoseAnimation()
                    != client.getLocalPlayer().getIdlePoseAnimation()
                    || client.getLocalPlayer().getAnimation() == AnimationID.LOOKING_INTO) {
                Print("consuming click while animating");
                event.consume();
                return;
            }
            for (int seeds : seedIDs) {
                if (getInventoryItem(seeds) == null) {
                    setMenuEntry(event, clickSeedTable());

                    if (client.getWidget(14352385) != null) {
                        setMenuEntry(event, selectSeed());
                        return;
                    }
                }
                if (getInventoryItem(seeds) != null) {
                    setMenuEntry(event, farmDoor());
                    return;
                }
            }
        }

        if(isInTithe()) {
            if(EAST_PATCHES.size() < 20){
                populatePatches();
            }

            //populatePatches();
            /**
            for(int cans : wateringCans){
                if(getInventoryItem(cans)!=null){
                    setMenuEntry(event,fillCans());
                    return;
                }
            }**/
            //|| client.getLocalPlayer().getPoseAnimation()
            //                    != client.getLocalPlayer().getIdlePoseAnimation())
            if(client.getLocalPlayer().isMoving())
            {
                Print("consuming click while animating");
                event.consume();
                return;
            }
            if(tickCounter!=0){
                event.consume();
                return;
            }


            //If theres stage 1 seeds, water them
            int firstStageID = PLANTS_FIRST_STAGE_UNWATERED.get(seedID-1);
            int secondStageID = PLANTS_SECOND_STAGE_UNWATERED.get(seedID-1);
            int thirdStageID = PLANTS_THIRD_STAGE_UNWATERED.get(seedID-1);
            int readyID = PLANTS_GROWN.get(seedID-1);
            int deadFirstStageID = DEAD_PLANTS1.get(seedID-1);
            int deadSecondStageID = DEAD_PLANTS2.get(seedID-1);
            int deadThirdStageID = DEAD_PLANTS3.get(seedID-1);
            int deadFourthStageID = DEAD_PLANTS4.get(seedID-1);
            boolean stage0 = checkPatches(EMPTY_PATCH_ID);
            boolean stage1 = checkPatches(firstStageID);
            boolean stage2 = checkPatches(secondStageID);
            boolean stage3 = checkPatches(thirdStageID);
            boolean stage4 = checkPatches(readyID);
            boolean deadStage1 = checkPatches(deadFirstStageID);
            boolean deadStage2 = checkPatches(deadSecondStageID);
            boolean deadStage3 = checkPatches(deadThirdStageID);
            boolean deadStage4 = checkPatches(deadFourthStageID);



            boolean allowedToFillCans = true;
            for(GameObject obj : EAST_PATCHES.values())
            {
                if(obj.getId() != EMPTY_PATCH_ID){
                    allowedToFillCans = false;
                }
            }

            if(getWaterCount() < 60 && allowedToFillCans){
                keepFilling = true;
            }

            if(keepFilling){
                for(int id : wateringCans){
                    if(getInventoryItem(id)!=null){
                        System.out.println("Filling can");
                        setMenuEntry(event,fillCans());
                        return;
                    }
                }
                keepFilling = false;
            } else {
                if(stage1){ //water
                    for(GameObject obj : EAST_PATCHES.values()){
                        if(obj.getId() == firstStageID){
                            System.out.println("Watering stage 1 seed -> ");
                            setMenuEntry(event,waterSeedsNew(obj));
                            tickCounter = 2;
                            break;
                        }
                    }
                }
                else if(stage0 && (!stage4 && !deadStage1 && !deadStage2 && !deadStage3 && !deadStage4) && hasSeeds()){//empty patches and nothing to harvest or clear
                    for(GameObject obj : EAST_PATCHES.values()){
                        if(obj.getId() == EMPTY_PATCH_ID){
                            System.out.println("planting new seed -> ");
                            setMenuEntry(event,plantSeedsNew(obj));
                            tickCounter = 2;
                            break;
                        }
                    }
                }
                else if(stage2){ //water
                    for(GameObject obj : EAST_PATCHES.values()){
                        if(obj.getId() == secondStageID){
                            System.out.println("Watering stage 2 seed -> ");
                            setMenuEntry(event,waterSeedsNew(obj));
                            break;
                        }
                    }
                }
                else if (stage3) {
                    for(GameObject obj : EAST_PATCHES.values()){
                        if(obj.getId() == thirdStageID){
                            System.out.println("Watering stage 3 seed -> ");
                            setMenuEntry(event,waterSeedsNew(obj));
                            break;
                        }
                    }
                }
                else if (stage4){
                    for(GameObject obj : EAST_PATCHES.values()){
                        if(obj.getId() == readyID){
                            System.out.println("Picking plant");
                            setMenuEntry(event, harvestPlant(obj));
                            break;
                        }
                    }
                }
                else if (deadStage1){
                    for(GameObject obj : EAST_PATCHES.values()){
                        if(obj.getId() == deadFirstStageID){
                            System.out.println("DEAD PLANT STAGE 1");
                            setMenuEntry(event, harvestPlant(obj));
                            break;
                        }
                    }
                }
                else if (deadStage2){
                    for(GameObject obj : EAST_PATCHES.values()){
                        if(obj.getId() == deadSecondStageID){
                            System.out.println("DEAD PLANT STAGE 2");
                            setMenuEntry(event, harvestPlant(obj));
                            break;
                        }
                    }
                }
                else if (deadStage3){
                    for(GameObject obj : EAST_PATCHES.values()){
                        if(obj.getId() == deadThirdStageID){
                            System.out.println("DEAD PLANT STAGE 3");
                            setMenuEntry(event, harvestPlant(obj));
                            break;
                        }
                    }
                }
                else if (deadStage4){
                    for(GameObject obj : EAST_PATCHES.values()){
                        if(obj.getId() == deadFourthStageID){
                            System.out.println("DEAD PLANT STAGE 4");
                            setMenuEntry(event, harvestPlant(obj));
                            break;
                        }
                    }
                }
                else if (!hasSeeds()){
                    Widget invFruit = inventoryFruit();
                    if(invFruit != null){
                        setMenuEntry(event, useFruitOnSack(getGameObject(DEPOSIT_SACK_ID), invFruit));
                        tickCounter = 1;
                    } else {
                        setMenuEntry(event, farmDoor());
                        return;
                    }
                }
            }
        }
    }

    private boolean isInLobby() {
        WorldArea AREA = new WorldArea(new WorldPoint(1795,3497,0),new WorldPoint(1805,3505,0));
        return (client.getLocalPlayer().getWorldLocation().isInArea(AREA));
    }
    private boolean isInTithe() {
        WorldArea AREA = new WorldArea(WorldPoint.fromLocal(client,new LocalPoint(6848,4800))
                ,WorldPoint.fromLocal(client,new LocalPoint(10816,8896)));
        return (client.getLocalPlayer().getWorldLocation().isInArea(AREA));
    }
    private MenuEntry clickSeedTable(){
        GameObject barrel = getGameObject(SEED_TABLE_ID);
        return createMenuEntry(
                SEED_TABLE_ID,
                MenuAction.GAME_OBJECT_FIRST_OPTION,
                getLocation(barrel).getX(),
                getLocation(barrel).getY(),
                true);
    }
    private MenuEntry selectSeed(){
        return createMenuEntry(0,MenuAction.WIDGET_CONTINUE,config.seeds().ID,WidgetInfo.DIALOG_OPTION_OPTION1.getId(),false);
    }
    private MenuEntry farmDoor(){
        WallObject door = getWallObject(FARM_DOOR_ID);
        return createMenuEntry(
                door.getId(),
                MenuAction.GAME_OBJECT_FIRST_OPTION,
                getLocation(door).getX(),
                getLocation(door).getY(),
                true);
    }
    private MenuEntry fillCans() {
        GameObject barrel = getGameObject(WATER_BARREL_ID);
        for(int cans:wateringCans){
            if (getInventoryItem(cans) != null) {
                return useItemOnBarrel(barrel, getInventoryItem(cans));
            }
        }
        return null;
    }
    private MenuEntry plantSeedsNew(GameObject patch){
        Print("Seed is being planted");
        for(int seeds:seedIDs){
            if(getInventoryItem(seeds)!=null){
                MenuEntry m = useSeedOnPatch(patch, getInventoryItem(seeds));
                return useSeedOnPatch(patch, getInventoryItem(seeds));
            }
        }
        return null;
    }
    private MenuEntry plantSeeds(){
        GameObject patch = getGameObject(EMPTY_PATCH_ID);
        Print("Seed is being planted");
        for(int seeds:seedIDs){
            if(getInventoryItem(seeds)!=null){
                return useSeedOnPatch(patch, getInventoryItem(seeds));
            }
        }
        return null;
    }
    private MenuEntry waterSeedsNew(GameObject obj){
        GameObject patch = obj;
        //GameObject patch = EAST_PATCHES.get(EAST_PATCHES.indexOf(obj));
            System.out.println("Watering patch -> " + patch.getWorldLocation() + "  Name -> " + patch.getName());
            for(int cans:useableCans){
                if(getInventoryItem(cans)!=null){
                    return useCanOnPatch(patch, getInventoryItem(cans));
                }
            }

        return null;
    }
    private MenuEntry waterSeeds(){
        for(int seedling : PLANTS_FIRST_STAGE_UNWATERED) {
            GameObject patch = getGameObject(seedling);
            Print("Seed is being watered");
            for(int cans:useableCans){
                if(getInventoryItem(cans)!=null){
                    return useCanOnPatch(patch, getInventoryItem(cans));
                }
            }
        }
        return null;
    }
    private MenuEntry harvestPlant(GameObject obj){
        Print("Harvesting plant");
        return createMenuEntry(
                obj.getId(),
                MenuAction.GAME_OBJECT_FIRST_OPTION,
                getLocation(obj).getX(),
                getLocation(obj).getY(),
                false
                );
    }
    private MenuEntry useItemOnBarrel(GameObject barrel,Widget item){
        setSelectedInventoryItem(item);
        return createMenuEntry(
                barrel.getId(),
                MenuAction.WIDGET_TARGET_ON_GAME_OBJECT,
                getLocation(barrel).getX(),
                getLocation(barrel).getY(),
                false);
    }
    private MenuEntry useFruitOnSack(GameObject sack, Widget item){
        setSelectedInventoryItem(item);
        return createMenuEntry(
                sack.getId(),
                MenuAction.WIDGET_TARGET_ON_GAME_OBJECT,
                getLocation(sack).getX(),
                getLocation(sack).getY(),
                false);
    }
    private MenuEntry useSeedOnPatch(GameObject patch,Widget item){
        setSelectedInventoryItem(item);
        return createMenuEntry(
                patch.getId(),
                MenuAction.WIDGET_TARGET_ON_GAME_OBJECT,
                getLocation(patch).getX(),
                getLocation(patch).getY(),
                false);
    }
    private MenuEntry useCanOnPatch(GameObject patch,Widget item){
        setSelectedInventoryItem(item);
        return createMenuEntry(
                patch.getId(),
                MenuAction.WIDGET_TARGET_ON_GAME_OBJECT,
                getLocation(patch).getX(),
                getLocation(patch).getY(),
                false);
    }
    private Point getLocation(TileObject tileObject) {
        if (tileObject instanceof GameObject)
            return ((GameObject)tileObject).getSceneMinLocation();
        return new Point(tileObject.getLocalLocation().getSceneX(), tileObject.getLocalLocation().getSceneY());
    }
    private GameObject getGameObject(int ID) {
        return new GameObjectQuery()
                .idEquals(ID)
                .result(client)
                .nearestTo(client.getLocalPlayer());
    }
    private WallObject getWallObject(int ID) {
        return new WallObjectQuery()
                .idEquals(ID)
                .result(client)
                .nearestTo(client.getLocalPlayer());
    }
    private Widget inventoryFruit(){
        client.runScript(6009, 9764864, 28, 1, -1);
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        if (inventoryWidget != null && inventoryWidget.getDynamicChildren() != null)
        {
            Widget[] items = inventoryWidget.getDynamicChildren();
            for (Widget item : items)
            {
                if(fruitIDs.contains(item.getItemId())){
                    return item;
                }
            }
        }
        return null;
    }
    private boolean hasSeeds(){
        client.runScript(6009, 9764864, 28, 1, -1);
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        if (inventoryWidget != null && inventoryWidget.getDynamicChildren() != null)
        {
            Widget[] items = inventoryWidget.getDynamicChildren();
            for (Widget item : items)
            {
                if(seedIDs.contains(item.getItemId())){
                    return true;
                }
            }
        }
        return false;
    }
    private int getWaterCount(){
        int waterCount = 0;
        int oneDose = ItemID.WATERING_CAN1;
        int twoDose = ItemID.WATERING_CAN2;
        int threeDose = ItemID.WATERING_CAN3;
        int fourDose = ItemID.WATERING_CAN4;
        int fiveDose = ItemID.WATERING_CAN5;
        int sixDose = ItemID.WATERING_CAN6;
        int sevenDose = ItemID.WATERING_CAN7;
        int eightDose = ItemID.WATERING_CAN8;
        client.runScript(6009, 9764864, 28, 1, -1);
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);

        if (inventoryWidget != null && inventoryWidget.getDynamicChildren() != null)
        {
            Widget[] items = inventoryWidget.getDynamicChildren();
            for (Widget item : items)
            {
                if (item.getItemId() == oneDose)
                {
                    waterCount += 1;
                }
                else if (item.getItemId() == twoDose)
                {
                    waterCount += 2;
                }
                else if (item.getItemId() == threeDose)
                {
                    waterCount += 3;
                }
                else if (item.getItemId() == fourDose)
                {
                    waterCount += 4;
                }
                else if (item.getItemId() == fiveDose)
                {
                    waterCount += 5;
                }
                else if (item.getItemId() == sixDose)
                {
                    waterCount += 6;
                }
                else if (item.getItemId() == sevenDose)
                {
                    waterCount += 7;
                }
                else if (item.getItemId() == eightDose)
                {
                    waterCount += 8;
                }
            }
        }
        return waterCount;
    }
    private Widget getInventoryItem(int id) {
        client.runScript(6009, 9764864, 28, 1, -1); //rebuild inventory
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        if (inventoryWidget!=null)
        {
            return getWidgetItem(inventoryWidget,id);
        }
        return null;
    }
    private Widget getWidgetItem(Widget widget,int id) {
        for (Widget item : widget.getDynamicChildren())
        {
            if (item.getItemId() == id)
            {
                return item;
            }
        }
        return null;
    }
    private void setSelectedInventoryItem(Widget item) {
        client.setSelectedSpellWidget(WidgetInfo.INVENTORY.getId());
        client.setSelectedSpellChildIndex(item.getIndex());
        client.setSelectedSpellItemId(item.getItemId()); //this might be different inside/outside bank? getID works sometimes idfk
    }
    public MenuEntry createMenuEntry(int identifier, MenuAction type, int param0, int param1, boolean forceLeftClick) {
        return client.createMenuEntry(0).setOption("").setTarget("").setIdentifier(identifier).setType(type)
                .setParam0(param0).setParam1(param1).setForceLeftClick(forceLeftClick);
    }
    private void setMenuEntry(MenuOptionClicked event, MenuEntry menuEntry){
        event.setId(menuEntry.getIdentifier());
        event.setMenuAction(menuEntry.getType());
        event.setParam0(menuEntry.getParam0());
        event.setParam1(menuEntry.getParam1());
    }
    private void Print(String string){
        if (config.debug())
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE,"",string,"");
        }
    }
    private void updatePatches(GameObject newObject, List<Integer> acceptableIds){
        if(!acceptableIds.contains(newObject.getId()) && newObject.getId() != EMPTY_PATCH_ID) {
            return;
        }
        WorldPoint newObjLoc = newObject.getWorldLocation();
        boolean shouldReplace = false;
        for(GameObject obj : EAST_PATCHES.values()){
            //System.out.println(obj.getWorldLocation() + "  New loc ->" + newObjLoc.getWorldLocation());
            if(obj.getWorldX() == newObjLoc.getWorldX() && obj.getWorldY() == newObjLoc.getWorldY()){
                shouldReplace = true;
                break;
            }
        }
        if(shouldReplace){
            System.out.println("Removing object and adding new");
            EAST_PATCHES.put(newObjLoc, newObject);
            for(GameObject obj :EAST_PATCHES.values()){
                System.out.println("name -> " + obj.getName());
            }
        }

    }
    private boolean populatePatches(){

        WEST_PATCHES.clear();
        EAST_PATCHES.clear();
        HashMap<WorldPoint, GameObject> TEMP_EAST_PATCHES = new HashMap<>();
        HashMap<WorldPoint, GameObject> TEMP_WEST_PATCHES = new HashMap<>();
        List<GameObject> mostEast = new ArrayList<>();
        List<GameObject> secondMostEast = new ArrayList<>();
        List<GameObject> middleNorth = new ArrayList<>();
        GameObject furthestSouthEast = null;
        GameObject furthestNorthWest = null;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        List<GameObject> EmptyPatches = new ArrayList<>(new GameObjectQuery()
                .idEquals(EMPTY_PATCH_ID)
                .result(client));
        for(GameObject obj : EmptyPatches){
            int x = obj.getWorldX();
            int y = obj.getWorldY();
            int yDiff = maxY - y;
            if(yDiff == 30){
                continue;
            }
            minX = Math.min(x, minX);
            minY = Math.min(y, minY);
            maxX = Math.max(x, maxX);
            maxY = Math.max(y, maxY);
            if(y == maxY && x == minX){ //Most NW
                furthestNorthWest = obj;
            }
            else if(y == minY && x == maxX){
                furthestSouthEast = obj;
            }
        }
        //EAST_PATCHES.put(furthestSouthEast.getWorldLocation(), furthestSouthEast);
        for(GameObject obj : EmptyPatches){
            if(obj.getWorldX() == furthestSouthEast.getWorldX()){ //same column as initial tile
                mostEast.add(obj);
            }
            else if (obj.getWorldX() == furthestSouthEast.getWorldX() - 5){ //second east column
                secondMostEast.add(obj);
            }
            else if (obj.getWorldX() == furthestSouthEast.getWorldX() - 10){ //middle
                if(obj.getWorldY() >= furthestSouthEast.getWorldY() + 15){
                    middleNorth.add(obj);
                }
            }
        }
        ArrayList<GameObject> test = new ArrayList<>();
        if(mostEast.size() == 8 && secondMostEast.size() == 8 && middleNorth.size() == 4){
            for(int i = 0; i < 8; i++){
                test.add(mostEast.get(i));
                test.add(secondMostEast.get(i));
                EAST_PATCHES.put(mostEast.get(i).getWorldLocation(), mostEast.get(i));
                EAST_PATCHES.put(secondMostEast.get(i).getWorldLocation(), secondMostEast.get(i));
            }
            for(int i = 3; i >= 0; i--){
                test.add(middleNorth.get(i));
                EAST_PATCHES.put(middleNorth.get(i).getWorldLocation(), middleNorth.get(i));
            }
        }else{
            System.out.println("mosteastsize -> " + mostEast.size() + " 2ndmosteastsize -> " + secondMostEast.size() + "  midN size -> " + middleNorth.size());
            return false;
        }
        for(int i = 0; i< mostEast.size(); i++){
            System.out.println("East -> " + mostEast.get(i).getWorldLocation());
        }
        for(int i = 0; i< secondMostEast.size(); i++){
            System.out.println("SecondMost East -> " + secondMostEast.get(i).getWorldLocation());
        }
        for(int i = 0; i< middleNorth.size(); i++){
            System.out.println("Mid North -> " + middleNorth.get(i).getWorldLocation());
        }
        int i = 1;
        for(Map.Entry<WorldPoint, GameObject> entry : EAST_PATCHES.entrySet()){
            System.out.println(i + " ->  " + entry.getKey());
            i++;
        }

        /**
        for(GameObject obj : EmptyPatches){
            if(obj.getWorldX() > furthestSouthEast.getWorldX() -10){
                TEMP_EAST_PATCHES.put(obj.getWorldLocation(),obj);
            }
            else if (obj.getWorldX() == furthestSouthEast.getWorldX() -10){
                if(obj.getWorldY() >= furthestSouthEast.getWorldY() + 15){
                    TEMP_EAST_PATCHES.put(obj.getWorldLocation(), obj);
                }
            }

            if(obj.getWorldX() < furthestNorthWest.getWorldX() + 10){
                TEMP_WEST_PATCHES.put(obj.getWorldLocation(), obj);
            }
            else if (obj.getWorldX() == furthestNorthWest.getWorldX() + 10){
                if((obj.getWorldY() <= furthestNorthWest.getWorldY() - 15) && obj.getWorldY() >= furthestSouthEast.getWorldY()){
                    TEMP_WEST_PATCHES.put(obj.getWorldLocation(), obj);
                }
            }
        }**/
        //EAST_PATCHES.put(furthestSouthEast.getWorldLocation(), furthestSouthEast);
       // while(EAST_PATCHES.size() < 20){
          // for(Map.Entry<WorldPoint, GameObject> entry : TEMP_EAST_PATCHES.entrySet()){

           // }
       // }
        return EAST_PATCHES.size() == 20;
    }
    private boolean checkPatches(int id){
        for(GameObject obj : EAST_PATCHES.values()){
            if(obj.getId() == id){
                return true;
            }
        }
        return false;
    }
}