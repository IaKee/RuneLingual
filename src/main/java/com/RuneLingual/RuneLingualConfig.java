package com.RuneLingual;


import net.runelite.client.RuneLite;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.io.File;

@ConfigGroup(RuneLingualConfig.GROUP)
public interface RuneLingualConfig extends Config {
    final int offset_section1 = 0;
    public String helpLink = "https://github.com/YS-jack/RuneLingual-Plugin/blob/master/Readmes/Settings_select_lang.md";
    @ConfigSection(
            name = "Language selection",
            description = "Select language",
            position = offset_section1,
            closedByDefault = false
    )
    String SECTION_BASIC_SETTINGS = "basicSettings";
    final int offset = 5;
    String GROUP = "lingualConfig";
    @ConfigSection(
            name = "Dynamic translating",
            description = "Online translation options",
            position = 1 + offset,
            closedByDefault = false
    )
    String SECTION_CHAT_SETTINGS = "chatSettings";
    int offset_section2 = 20;
    @ConfigSection(
            name = "Game system text",
            description = "Options for game system texts",
            position = offset_section2,
            closedByDefault = false
    )
    String SECTION_GAME_SYSTEM_TEXT = "gameSystemText";
    final int offset_section3 = 40;
    @ConfigSection(
            name = "Others' Chat messages",
            description = "Options for chat messages",
            position = offset_section3,
            closedByDefault = false
    )
    String SECTION_CHAT_MESSAGES = "chatMessages";
    int offset_section4 = 60;
    @ConfigSection(
            name = "My Chat messages",
            description = "Options for chat messages",
            position = offset_section4,
            closedByDefault = false
    )
    String SECTION_MY_CHAT_MESSAGES = "myChatMessages";
    final int offset_section5 = 80;
    @ConfigSection(
            name = "Forceful Player Settings",
            description = "Options for specific players. This will take priority over other settings in this order",
            position = offset_section5,
            closedByDefault = false
    )
    String SECTION_SPECIFIC_PLAYER_SETTINGS = "specificPlayerSettings";

    @ConfigItem(
            name = "\uD83D\uDDE3\uD83D\uDCAC\uD83C\uDF10",
            description = "Select the language to be translated to",
            keyName = "targetLang",
            position = offset_section1,
            section = SECTION_BASIC_SETTINGS
    )
    default LangCodeSelectableList getSelectedLanguage() {
        return LangCodeSelectableList.ENGLISH;
    }

    @ConfigItem(
            name = "Help Link (right click to reset)",
            description = "right click to reset",
            position = 1 + offset_section1,
            keyName = "enableRuneLingual",
            section = SECTION_BASIC_SETTINGS
    )
    default String getHelpLink() {
        return helpLink;
    } // getHelpLink shouldnt be used anywhere, instead use helpLink

    @ConfigItem(
            name = "Enable Online Translation",
            description = "whether to translate using online services",
            section = SECTION_CHAT_SETTINGS,
            keyName = "enableAPI",
            position = 2 + offset
    )
    default boolean ApiConfig() {
        return false;
    }

    @ConfigItem(
            name = "Translating service",
            description = "Select your preferred translation service",
            section = SECTION_CHAT_SETTINGS,
            keyName = "translatingService",
            position = 3 + offset
    )
    default TranslatingServiceSelectableList getApiServiceConfig() {
        return TranslatingServiceSelectableList.DeepL;
    }

    @ConfigItem(
            name = "Service API Key",
            description = "Your API key for the chosen translating service",
            section = SECTION_CHAT_SETTINGS,
            keyName = "APIKey",
            position = 4 + offset,
            secret = true
            //hidden = true
    )
    default String getAPIKey() {
        return "";
    }

    @ConfigItem(
            name = "Enable Word Count Overlay",
            description = "whether to show how many characters you have used",
            section = SECTION_CHAT_SETTINGS,
            keyName = "enableUsageOverlay",
            position = 2 + offset
    )
    default boolean showUsageOverlayConfig() {
        return true;
    }

    @ConfigItem(
            name = "NPC Dialogue",
            description = "Option for NPC Dialogues",
            position = 1 + offset_section2,
            keyName = "npcDialogue",
            section = SECTION_GAME_SYSTEM_TEXT
    )
    default ingameTranslationConfig getNpcDialogueConfig() {
        return ingameTranslationConfig.USE_LOCAL_DATA;
    }

    @ConfigItem(
            name = "Game Messages",
            description = "Option for game messages",
            position = 2 + offset_section2,
            keyName = "gameMessages",
            section = SECTION_GAME_SYSTEM_TEXT
    )
    default ingameTranslationConfig getGameMessagesConfig() {
        return ingameTranslationConfig.USE_LOCAL_DATA;
    }

    @ConfigItem(
            name = "Item Names",
            description = "Option for item names",
            position = 4 + offset_section2,
            keyName = "itemNames",
            section = SECTION_GAME_SYSTEM_TEXT
    )
    default ingameTranslationConfig getItemNamesConfig() {
        return ingameTranslationConfig.USE_LOCAL_DATA;
    }

    @ConfigItem(
            name = "NPC Names",
            description = "Option for NPC names",
            position = 5 + offset_section2,
            keyName = "NPCNames",
            section = SECTION_GAME_SYSTEM_TEXT
    )
    default ingameTranslationConfig getNPCNamesConfig() {
        return ingameTranslationConfig.USE_LOCAL_DATA;
    }

    @ConfigItem(
            name = "Object Names",
            description = "Option for object names",
            position = 6 + offset_section2,
            keyName = "objectNames",
            section = SECTION_GAME_SYSTEM_TEXT
    )
    default ingameTranslationConfig getObjectNamesConfig() {
        return ingameTranslationConfig.USE_LOCAL_DATA;
    }

    @ConfigItem(
            name = "Interfaces",
            description = "Option for interface texts",
            position = 7 + offset_section2,
            keyName = "interfaceText",
            section = SECTION_GAME_SYSTEM_TEXT
    )
    default ingameTranslationConfig getInterfaceTextConfig() {
        return ingameTranslationConfig.USE_LOCAL_DATA;
    }

    //not using this, makes configuration annoying
//	@ConfigItem(
//			name = "All Friends",
//			description = "Option that applies to all friends",
//			position = 2 + offset_section3,
//			keyName = "allFriends",
//			section = SECTION_CHAT_MESSAGES
//	)
//	default chatConfig getAllFriendsConfig() {return chatConfig.LEAVE_AS_IS;}

    @ConfigItem(
            name = "Mouse Menu Options",
            description = "Option for items, NPCs, objects, such as 'Use', 'Talk-to', etc.",
            position = 8 + offset_section2,
            keyName = "menuOption",
            section = SECTION_GAME_SYSTEM_TEXT
    )
    default ingameTranslationConfig getMenuOptionConfig() {
        return ingameTranslationConfig.USE_LOCAL_DATA;
    }

    @ConfigItem(
            name = "Enable Mouse Hover Text",
            description = "Option to toggle mouse hover texts",
            position = 9 + offset_section2,
            keyName = "overheadText",
            section = SECTION_GAME_SYSTEM_TEXT
    )
    default boolean getMouseHoverConfig() {
        return true;
    }

    @ConfigItem(
            name = "Public",
            description = "Option for public chat messages",
            position = 3 + offset_section3,
            keyName = "publicChat",
            section = SECTION_CHAT_MESSAGES
    )
    default chatConfig getPublicChatConfig() {
        return chatConfig.LEAVE_AS_IS;
    }

    @ConfigItem(
            name = "Clan",
            description = "Option for clan chat messages",
            position = 4 + offset_section3,
            keyName = "clanChat",
            section = SECTION_CHAT_MESSAGES
    )
    default chatConfig getClanChatConfig() {
        return chatConfig.LEAVE_AS_IS;
    }

    @ConfigItem(
            name = "Guest Clan",
            description = "Option for guest clan chat messages",
            position = 5 + offset_section3,
            keyName = "guestClanChat",
            section = SECTION_CHAT_MESSAGES
    )
    default chatConfig getGuestClanChatConfig() {
        return chatConfig.LEAVE_AS_IS;
    }

    @ConfigItem(
            name = "Friends Chat",
            description = "Option for friends chat messages",
            position = 6 + offset_section3,
            keyName = "friendsChat",
            section = SECTION_CHAT_MESSAGES
    )
    default chatConfig getFriendsChatConfig() {
        return chatConfig.LEAVE_AS_IS;
    }

    @ConfigItem(
            name = "GIM Group",
            description = "Option for GIM group chat messages",
            position = 7 + offset_section3,
            keyName = "GIMChat",
            section = SECTION_CHAT_MESSAGES
    )
    default chatConfig getGIMChatConfig() {
        return chatConfig.LEAVE_AS_IS;
    }

    @ConfigItem(
            name = "Me in Public",
            description = "Option for your own messages in Public chat",
            position = 1 + offset_section4,
            keyName = "myChatConfig",
            section = SECTION_MY_CHAT_MESSAGES
    )
    default chatSelfConfig getMyPublicConfig() {
        return chatSelfConfig.TRANSFORM;
    }

    @ConfigItem(
            name = "Me in Friends Chat",
            description = "Option for your own messages in Friends chat",
            position = 2 + offset_section4,
            keyName = "myFcConfig",
            section = SECTION_MY_CHAT_MESSAGES
    )
    default chatSelfConfig getMyFcConfig() {
        return chatSelfConfig.TRANSFORM;
    }

    @ConfigItem(
            name = "Me in Clan",
            description = "Option for your own messages in Clan chat",
            position = 3 + offset_section4,
            keyName = "myClanConfig",
            section = SECTION_MY_CHAT_MESSAGES
    )
    default chatSelfConfig getMyClanConfig() {
        return chatSelfConfig.TRANSFORM;
    }

    @ConfigItem(
            name = "Me in Guest Clan",
            description = "Option for your own messages in Guest Clan chat",
            position = 4 + offset_section4,
            keyName = "myGuestClanConfig",
            section = SECTION_MY_CHAT_MESSAGES
    )
    default chatSelfConfig getMyGuestClanConfig() {
        return chatSelfConfig.TRANSFORM;
    }

    @ConfigItem(
            name = "Me in GIM",
            description = "Option for your own messages in GIM chat",
            position = 5 + offset_section4,
            keyName = "myGimConfig",
            section = SECTION_MY_CHAT_MESSAGES
    )
    default chatSelfConfig getMyGIMConfig() {
        return chatSelfConfig.TRANSFORM;
    }

    String defaultText4ForcefulPlayerSettings = "enter player names here, separated by commas or new line";

    @ConfigItem(
            name = "Leave as is",
            description = "Specific players to not translate",
            position = 1 + offset_section5,
            keyName = "specificDontTranslate",
            section = SECTION_SPECIFIC_PLAYER_SETTINGS
    )
    default String getSpecificDontTranslate() {return defaultText4ForcefulPlayerSettings;}

    @ConfigItem(
            name = "Translate with APIs",
            description = "Specific players to translate using online translators",
            position = 2 + offset_section5,
            keyName = "specificApiTranslate",
            section = SECTION_SPECIFIC_PLAYER_SETTINGS
    )
    default String getSpecificApiTranslate() {
        return defaultText4ForcefulPlayerSettings;
    }

    @ConfigItem(
            name = "Transform",
            description = "Specific players to transform",
            position = 3 + offset_section5,
            keyName = "specificTransform",
            section = SECTION_SPECIFIC_PLAYER_SETTINGS
    )
    default String getSpecificTransform() {
        return defaultText4ForcefulPlayerSettings;
    }

    @ConfigItem(
            name = "Local file location",
            description = "Location of the files to be translated",
            keyName = "fileLocation",
            position = 200 + offset,
            secret = true
    )
    default String getFileLocation() {
        return RuneLite.RUNELITE_DIR.getPath() + File.separator + "RuneLingual_resources";
    }

    enum ingameTranslationConfig {
        USE_LOCAL_DATA,
        USE_API,
        //TRANSLITERATE, // not for now, need to prepare transliteration data for all languages
        DONT_TRANSLATE,
    }

    enum chatConfig {
        TRANSFORM, //eg: watasi ha inu ga suki -> 私は犬が好き
        USE_API, // eg: I like dogs -> 私は犬が好き
        LEAVE_AS_IS, // eg: I like dogs -> I like dogs
    }


    enum chatSelfConfig {
        TRANSFORM,
        LEAVE_AS_IS,
    }

}
