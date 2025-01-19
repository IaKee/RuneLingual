package com.RuneLingual.commonFunctions;

import lombok.Getter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;

import com.RuneLingual.RuneLingualPlugin;

import javax.inject.Inject;

@Getter @Slf4j
public class Ids {
    @Inject
    public Ids(RuneLingualPlugin plugin) {
        this.plugin = plugin;
        this.client = plugin.getClient();
    }
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    Client client;

    //widget IDs of tabs
    //private final int combatOptionParentWidgetId = client.getWidget(ComponentID.COMBAT_LEVEL).getParentId();
    private final int skillsTabParentWidgetId = ComponentID.SKILLS_CONTAINER;
    //private final int characterSummaryTabWidgetId = ComponentID.CHARACTER_SUMMARY_CONTAINER;
    private final int questTabParentWidgetId = ComponentID.QUEST_LIST_BOX;
    private final int achievementDiaryTabParentWidgetId = ComponentID.ACHIEVEMENT_DIARY_CONTAINER;
    private final int inventoryTabParentWidgetId = ComponentID.INVENTORY_CONTAINER;
    private final int equipmentTabParentWidgetId = ComponentID.EQUIPMENT_INVENTORY_ITEM_CONTAINER;
    private final int prayerTabParentWidgetId = 35454976;
    private final int spellBookTabParentWidgetId = ComponentID.SPELLBOOK_PARENT;
    private final int groupsTabParentWidgetId = 47644672;
    //private final int friendsTabParentWidgetId = client.getWidget(ComponentID.FRIEND_LIST_TITLE).getParentId();
    //private final int ignoreTabParentWidgetId = client.getWidget(ComponentID.IGNORE_LIST_TITLE).getParentId();
    private final int accountManagementTabParentWidgetId = 7143424;
    private final int settingsTabParentWidgetId = 7602176;
    private final int logoutTabParentWidgetId = 11927552;
    //private final int logOutTabParentWidgetId = 4521984;
    private final int worldSwitcherTabParentWidgetId = 4521984;
    private final int emotesTabParentWidgetId = ComponentID.EMOTES_WINDOW;
    private final int musicTabParentWidgetId = ComponentID.MUSIC_CONTAINER;


    // dont translate at all, except menu option
    private final int GimGroupNameWidgetId = 47579137;
    private final int GimMemberListWidgetId = 47579140;
    private final int ignoreNameListWidgetId = ComponentID.IGNORE_LIST_FULL_CONTAINER;

    //dont translate names, but translate World ### and "Offline"
    private final int friendsNameListWidgetId = ComponentID.FRIEND_LIST_FULL_CONTAINER;



    public int getCombatOptionParentWidgetId() {
        Widget w = client.getWidget(ComponentID.COMBAT_LEVEL);
        if(w != null) {
            return w.getParentId();
        }
        //log.info("parent of ComponentID.COMBAT_LEVEL is null");
        return -1;
    }

    public int getAchievementDiaryTabParentWidgetId() {
        Widget w = client.getWidget(ComponentID.ACHIEVEMENT_DIARY_CONTAINER);
        if(w != null) {
            return w.getParent().getParent().getParentId();
        }
        //log.info("parent^3 of ComponentID.ACHIEVEMENT_DIARY_CONTAINER is null");
        return -1;
    }

    public int getFriendsTabParentWidgetId() {
        Widget w = client.getWidget(ComponentID.FRIEND_LIST_TITLE);
        if(w != null) {
            return w.getParentId();
        }
        //log.info("parent of ComponentID.FRIEND_LIST_TITLE is null");
        return -1;
    }

    public int getIgnoreTabParentWidgetId() {
        Widget w = client.getWidget(ComponentID.IGNORE_LIST_TITLE);
        if(w != null) {
            return w.getParentId();
        }
        //log.info("parent of ComponentID.IGNORE_LIST_TITLE is null");
        return -1;
    }

    public int getCharacterSummaryTabWidgetId(){
        Widget w = client.getWidget(ComponentID.CHARACTER_SUMMARY_CONTAINER);
        if(w != null) {
            return w.getParentId();
        }
        //log.info("parent of ComponentID.CHARACTER_SUMMARY_CONTAINER is null");
        return -1;
    }
}
