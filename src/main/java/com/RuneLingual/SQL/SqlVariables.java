package com.RuneLingual.SQL;

import lombok.Getter;

@Getter
public enum SqlVariables {
    // SQL Variables
    columnEnglish("","english"),// name of column in the database
    columnTranslation("","translation"),
    columnCategory("","category"),
    columnSubCategory("","sub_category"),
    columnSource("","source"),

    dialogueInCategory("dialogue","category"), // string value for "dialogue" in category column
    examineInCategory("examine","category"),
    nameInCategory("name","category"),
    manualInCategory("manual","category"),//probably wont use
    actionsInCategory("actions","category"),
    lvlUpMessageInCategory("lvl_up_Message","category"),
    inventActionsInCategory("inventoryActions","category"), // this is for every menu entries in the main panel(Inventory, Worn Equipment, friends list, etc.


    itemInSubCategory("item","sub_category"),
    npcInSubCategory("npc","sub_category"),
    objInSubCategory("object","sub_category"),
    levelInSubCategory("level","sub_category"), // for "(level-%d)" of player or npcs with levels, category should be "name"
    menuInSubCategory("menu","sub_category"), // for widgets that are not buttons nor interface, such as one of the skills in skill list tab, name of tabs ("Combat Options", "Quest List")
    playerInSubCategory("player","sub_category"), // for player options, such as report, trade, follow, etc.
    questInSubCategory("quest","sub_category"),

    playerInSource("player","source"), // for player options, such as report, trade, follow, etc.
    //for tabs
    combatOptionsTabInSource("combatOptionsTab","source"), // for combat options, attack styles etc. query eg: Block	actions	menu	combatOption
    skillsTabInSource("skillsTab","source"), // for skills tab
    characterSummaryTabInSource("characterSummaryTab","source"), // for character summary tab
    questListTabInSource("questListTab","source"), // for quest list tab
    achievementDiaryTabInSource("achievementDiaryTab","source"), // for achievement Diary Tab
    inventTabInSource("inventTab","source"), // for inventory tab
    wornEquipmentTabInSource("wornEquipmentTab","source"), // for worn equipment tab
    prayerTabInSource("prayerTab","source"), // for prayer tab
    spellBookTabInSource("spellBookTab","source"), // for spell book tab
    groupTabInSource("groupTab","source"), // for group tab
    friendsTabInSource("friendsTab","source"), // for friends tab
    ignoreTabInSource("ignoreTab","source"), // for ignore tab
    accountManagementTabInSource("accountManagementTab","source"), // for account management tab
    settingsTabInSource("settingsTab","source"), // for settings tab
    logoutTabInSource("logoutTab","source"), // for logout tab
    worldSwitcherTabInSource("worldSwitcherTab","source"), // for world switcher
    emotesTabInSource("emotesTab","source"), // for emotes tab
    musicTabInSource("musicTab","source"), // for music tab

    ;


    private final String value;
    private final String columnName;

    SqlVariables(String val, String columnName) {
        this.value = val;
        this.columnName = columnName;
    }

}
