package com.RuneLingual;

import com.RuneLingual.ApiTranslate.*;
import com.RuneLingual.ChatMessages.*;
import com.RuneLingual.MouseOverlays.MouseTooltipOverlay;
import com.RuneLingual.SQL.SqlActions;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.Wigets.DialogTranslator;
import com.RuneLingual.Wigets.WidgetCapture;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.nonLatin.*;
import com.google.inject.Provides;

import javax.annotation.Nullable;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.callback.ClientThread;


import lombok.Getter;

import com.RuneLingual.SidePanelComponents.SidePanel;
import com.RuneLingual.commonFunctions.FileActions;
import com.RuneLingual.prepareResources.Downloader;
import com.RuneLingual.commonFunctions.Ids;


import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
@PluginDescriptor(
        // Plugin name shown at plugin hub
        name = "RuneLingual",
        description = "All-in-one translation plugin for OSRS."
)

public class RuneLingualPlugin extends Plugin {
    @Inject
    @Getter
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientToolbar clientToolBar;
    @Inject
    @Getter
    private ChatIconManager chatIconManager;
    @Getter
    private HashMap<String, Integer> charIds = new HashMap<>();    // colour-char(key) <-> CharIds(val)

    @Inject
    @Getter
    private RuneLingualConfig config;
    @Inject
    private CharImageInit charImageInit;

    @Getter
    private LangCodeSelectableList targetLanguage;
    @Getter
    private String selectedLanguageName;


    // main modules
    @Inject
    @Getter
    private ChatCapture chatCapture;
    @Inject
    @Getter
    private MenuCapture menuCapture;


    @Inject
    @Getter
    private Downloader downloader;
    @Inject
    @Getter
    private SidePanel panel;
    private NavigationButton navButton;
    @Inject
    @Getter
    private GeneralFunctions generalFunctions;
    @Inject
    @Getter
    private FileNameAndPath fileNameAndPath = new FileNameAndPath();
    @Inject
    @Getter
    private SqlActions sqlActions;
    @Inject
    private SqlQuery sqlQuery;
    @Getter
    @Setter
    private String[] tsvFileNames;
    @Getter
    private String databaseUrl;
    @Getter
    @Setter
    private Connection conn;
    @Inject
    @Getter
    private Ids ids;
    @Inject
    private MouseTooltipOverlay mouseTooltipOverlay;
    @Inject
    @Getter
    private Deepl deepl;
    @Inject
    private DeeplUsageOverlay deeplUsageOverlay;
    @Inject
    @Getter
    private ChatInputRLingual chatInputRLingual;
    @Inject
    @Getter
    private ChatInputOverlay chatInputOverlay;
    @Inject
    private ChatInputCandidateOverlay chatInputCandidateOverlay;
    @Inject
    private OverheadCapture overheadCapture;
    @Inject
    private WidgetCapture widgetCapture;

    @Getter
    private TileObject interactedObject;
    @Getter
    private NPC interactedNpc;
    @Getter
    boolean attacked;
    private int clickTick;
    @Getter
    private int gameCycle;


    @Override
    protected void startUp() throws Exception {
        log.info("Starting...");
        //get selected language
        targetLanguage = config.getSelectedLanguage();
        // set database URL
        databaseUrl = "jdbc:h2:" + FileNameAndPath.getLocalBaseFolder() + File.separator + targetLanguage.getLangCode()
                + File.separator + FileNameAndPath.getLocalSQLFileName();

        // check if online files have changed, if so download and update local files
        initLangFiles();

        //connect to database
        conn = DriverManager.getConnection(databaseUrl);

        // initiate overlays
        overlayManager.add(mouseTooltipOverlay);
        overlayManager.add(deeplUsageOverlay);
        overlayManager.add(chatInputOverlay);
        overlayManager.add(chatInputCandidateOverlay);

        // load image files
        charImageInit.loadCharImages();

        // side panel
        startPanel();

        log.info("RuneLingual started!");
    }

    @Subscribe
    public void onOverheadTextChanged(OverheadTextChanged event) throws Exception {
        overheadCapture.translateOverhead(event);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }
        log.info("Widget loaded:" + event.getGroupId());
//		clientThread.invokeLater(() -> {
//			widgetCapture.translateWidget();
//		});
    }

    @Subscribe
    private void onBeforeRender(BeforeRender event) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }

        chatInputRLingual.updateChatInput();


        //clientThread.invokeLater(() -> {
        widgetCapture.translateWidget();
        //});

    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {

//		MenuEntry[] ev = client.getMenuEntries();
//		for (MenuEntry e: ev ){
//			e.setOption(generalFunctions.StringToTags(testString, Colors.fromName("black")));
//		}

        menuCapture.handleOpenedMenu(event);
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }
        //menuTranslator.handleMenuEvent(event);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) throws Exception {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }
        if (client.getGameState() != GameState.LOGGED_IN && client.getGameState() != GameState.HOPPING) {
            return;
        }
        chatCapture.handleChatMessage(event);

    }


    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }
        if (gameStateChanged.getGameState() == GameState.LOADING) {
            interactedObject = null;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        // if language is changed
        if (targetLanguage != config.getSelectedLanguage()) {
            targetLanguage = config.getSelectedLanguage();
            initLangFiles();
            // todo: change the database URL and the connection to it
            databaseUrl = "jdbc:h2:" + FileNameAndPath.getLocalBaseFolder() + File.separator +
                    targetLanguage.getLangCode() + File.separator + FileNameAndPath.getLocalSQLFileName();
            try {
                conn = DriverManager.getConnection(databaseUrl);
            } catch (Exception e) {
                log.error("Error connecting to database: " + databaseUrl);
                targetLanguage = LangCodeSelectableList.ENGLISH;
                e.printStackTrace();
            }
            // download language files and structure language data
            clientToolBar.removeNavigation(navButton);
            boolean charImageChanged = initLangFiles();
            if (charImageChanged) {
                charImageInit.loadCharImages();
            }

            // reset language specific variables

            overlayManager.remove(mouseTooltipOverlay);
            MouseTooltipOverlay.setAttemptedTranslation(new ArrayList<>());
            overlayManager.add(mouseTooltipOverlay);

            //reset deepl's past translations
            deepl = new Deepl(this);

            restartPanel();
        }

    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned npcDespawned) {
        if (npcDespawned.getNpc() == interactedNpc) {
            interactedNpc = null;
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if (client.getTickCount() > clickTick && client.getLocalDestinationLocation() == null) {
            // when the destination is reached, clear the interacting object
            interactedObject = null;
            interactedNpc = null;
        }
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged interactingChanged) {
        if (interactingChanged.getSource() == client.getLocalPlayer()
                && client.getTickCount() > clickTick && interactingChanged.getTarget() != interactedNpc) {
            interactedNpc = null;
            attacked = interactingChanged.getTarget() != null && interactingChanged.getTarget().getCombatLevel() > 0;
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked) {
        switch (menuOptionClicked.getMenuAction()) {
            case WIDGET_TARGET_ON_GAME_OBJECT:
            case GAME_OBJECT_FIRST_OPTION:
            case GAME_OBJECT_SECOND_OPTION:
            case GAME_OBJECT_THIRD_OPTION:
            case GAME_OBJECT_FOURTH_OPTION:
            case GAME_OBJECT_FIFTH_OPTION: {
                int x = menuOptionClicked.getParam0();
                int y = menuOptionClicked.getParam1();
                int id = menuOptionClicked.getId();
                interactedObject = findTileObject(x, y, id);
                interactedNpc = null;
                clickTick = client.getTickCount();
                gameCycle = client.getGameCycle();
                break;
            }
            case WIDGET_TARGET_ON_NPC:
            case NPC_FIRST_OPTION:
            case NPC_SECOND_OPTION:
            case NPC_THIRD_OPTION:
            case NPC_FOURTH_OPTION:
            case NPC_FIFTH_OPTION: {
                interactedObject = null;
                interactedNpc = menuOptionClicked.getMenuEntry().getNpc();
                attacked = menuOptionClicked.getMenuAction() == MenuAction.NPC_SECOND_OPTION ||
                        menuOptionClicked.getMenuAction() == MenuAction.WIDGET_TARGET_ON_NPC
                                && client.getSelectedWidget() != null
                                && WidgetUtil.componentToInterface(client.getSelectedWidget().getId()) == InterfaceID.SPELLBOOK;
                clickTick = client.getTickCount();
                gameCycle = client.getGameCycle();
                break;
            }
            // Any menu click which clears an interaction
            case WALK:
            case WIDGET_TARGET_ON_WIDGET:
            case WIDGET_TARGET_ON_GROUND_ITEM:
            case WIDGET_TARGET_ON_PLAYER:
            case GROUND_ITEM_FIRST_OPTION:
            case GROUND_ITEM_SECOND_OPTION:
            case GROUND_ITEM_THIRD_OPTION:
            case GROUND_ITEM_FOURTH_OPTION:
            case GROUND_ITEM_FIFTH_OPTION:
                interactedObject = null;
                interactedNpc = null;
                break;
            default:
                if (menuOptionClicked.isItemOp()) {
                    interactedObject = null;
                    interactedNpc = null;
                }
        }
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolBar.removeNavigation(navButton);
        overlayManager.remove(mouseTooltipOverlay);
        overlayManager.remove(deeplUsageOverlay);
        overlayManager.remove(chatInputOverlay);
        overlayManager.remove(chatInputCandidateOverlay);
        log.info("RuneLingual plugin stopped!");
    }


    @Provides
    RuneLingualConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RuneLingualConfig.class);
    }

    private boolean initLangFiles() {
        //download necessary files
        downloader.setLangCode(targetLanguage.getLangCode());
        return downloader.initDownloader(targetLanguage.getLangCode());
    }

    public void restartPanel() {
        //update Language named folder (which is used to determine what language is selected)
        FileActions.deleteAllLangCodeNamedFile();
        FileActions.createLangCodeNamedFile(config.getSelectedLanguage());
        clientToolBar.removeNavigation(navButton);
        startPanel();
    }

    private void startPanel() {
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "globe.png");
        //panel.setTargetLanguage(config.getSelectedLanguage());
        panel = injector.getInstance(SidePanel.class);

        navButton = NavigationButton.builder()
                .tooltip("RuneLingual")
                .icon(icon)
                .priority(6)
                .panel(panel)
                .build();
        clientToolBar.addNavigation(navButton);
    }

    TileObject findTileObject(int x, int y, int id) {
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        Tile tile = tiles[client.getPlane()][x][y];
        if (tile != null) {
            for (GameObject gameObject : tile.getGameObjects()) {
                if (gameObject != null && gameObject.getId() == id) {
                    return gameObject;
                }
            }

            WallObject wallObject = tile.getWallObject();
            if (wallObject != null && wallObject.getId() == id) {
                return wallObject;
            }

            DecorativeObject decorativeObject = tile.getDecorativeObject();
            if (decorativeObject != null && decorativeObject.getId() == id) {
                return decorativeObject;
            }

            GroundObject groundObject = tile.getGroundObject();
            if (groundObject != null && groundObject.getId() == id) {
                return groundObject;
            }
        }
        return null;
    }

    @Nullable
    Actor getInteractedTarget() {
        return interactedNpc != null ? interactedNpc : client.getLocalPlayer().getInteracting();
    }

}

