package com.RuneLingual;

import com.RuneLingual.utils.ActionSelectableList;
import com.RuneLingual.utils.QuotaUnitsSelectableList;
import com.RuneLingual.utils.TranslatingServiceSelectableList;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("runelingual")
public interface RuneLingualConfig extends Config
{
    @ConfigSection(
        name = "Dynamic translating",
        description = "Online translation options",
        position = 0,
        closedByDefault = false)
    String SECTION_DYNAMIC_TRANSLATING = "dynamicTranslating";

    @ConfigItem(
        name = "Enable dynamic translating",
        description = "Control whether dynamic (API) translating is enabled.",
        section = SECTION_DYNAMIC_TRANSLATING,
        keyName = "enableAPI",
        position = 0)
    default boolean ApiConfig()
    {
        return false;
    }

    @ConfigItem(
        name = "Translating service",
        description = "Select your preferred translation service.",
        section = SECTION_DYNAMIC_TRANSLATING,
        keyName = "translatingService",
        position = 1)
    default TranslatingServiceSelectableList ApiServiceSelection()
    {
        return TranslatingServiceSelectableList.DeepL;
    }

    @ConfigSection(
        name = "Key and quota settings",
        description = "Advanced settings for API key and quota.",
        position = 1,
        closedByDefault = true)
    String SECTION_DYNAMIC_DANGER_ZONE = "dynamicDangerZone";

    @ConfigItem(
        name = "API Key",
        description = "Your API key for the chosen translating service.",
        section = SECTION_DYNAMIC_DANGER_ZONE,
        keyName = "serviceKey",
        position = 0,
        secret = true)
    default String getAPIKey()
    {
        return "";
    }

    @ConfigItem(
        name = "Quota limit",
        description = "The maximum amount of usage you want to allow for translating. (0 = unlimited)",
        section = SECTION_DYNAMIC_DANGER_ZONE,
        keyName = "quota",
        position = 1)
    default int quota()
    {
        return -1;
    }

    @ConfigItem(
        name = "Quota units",
        description = "Specify quota limiter units here.",
        section = SECTION_DYNAMIC_DANGER_ZONE,
        keyName = "quotaUnits",
        position = 2)
    default QuotaUnitsSelectableList quotaUnits()
    {
        return QuotaUnitsSelectableList.CHARACTERS;
    }

    @ConfigItem(
        name = "Quota countdown overlay",
        description = "Displays a widget with the remaining translation service available usage.",
        section = SECTION_DYNAMIC_DANGER_ZONE,
        keyName = "enableUsageOverlay",
        position = 3)
    default boolean showUsage()
    {
        return true;
    }

    @ConfigSection(
        name = "Translated components",
        description = "Select which game component sources that you want to be translated.",
        position = 2,
        closedByDefault = false)
    String SECTION_TRANSLATED_OBJECTS = "translatedComponents";

    @ConfigItem(
        name = "NPC Dialogue",
        description = "Allow translating for conversations with NPCs (includes overheads)",
        position = 0,
        keyName = "npcDialogue",
        section = SECTION_TRANSLATED_OBJECTS)
    default boolean allowNPCDialogue()
    {
        return true;
    }

    @ConfigItem(
        name = "Game messages (system)",
        description = "Allow translating  for system messages from the game system",
        position = 1,
        keyName = "gameMessages",
        section = SECTION_TRANSLATED_OBJECTS)
    default boolean allowGame()
    {
        return true;
    }

    @ConfigItem(
        name = "Items",
        description = "Allow translating items (includes actions, description and interactions).",
        position = 2,
        keyName = "items",
        section = SECTION_TRANSLATED_OBJECTS)
    default boolean allowItems()
    {
        return true;
    }

    @ConfigItem(
        name = "NPC names",
        description = "Allow translating NPC names from main world and dialogue entries.",
        position = 3,
        keyName = "NPCNames",
        section = SECTION_TRANSLATED_OBJECTS)
    default boolean allowNPC()
    {
        return true;
    }

    @ConfigItem(
        name = "Object Names",
        description = "Allow translating object names from the world.",
        position = 4,
        keyName = "objectNames",
        section = SECTION_TRANSLATED_OBJECTS)
    default boolean allowObjects()
    {
        return true;
    }

    @ConfigItem(
        name = "Interfaces",
        description = "Allow translating most UIs (e.g. crafting).",
        position = 5,
        keyName = "interfaceText",
        section = SECTION_TRANSLATED_OBJECTS)
    default boolean allowInterface()
    {
        return true;
    }

    @ConfigItem(
        name = "Missing translation action",
        description = "Select what should be done when handling missing translation from the above categories",
        position = 6,
        keyName = "missingAction",
        section = SECTION_TRANSLATED_OBJECTS)
    default ActionSelectableList missingAction()
    {
        return ActionSelectableList.LEAVE_AS_IS;
    }

    @ConfigSection(
        name = "Other player messages",
        description = "Select player categories in which you would like to enable translations for (requires dynamic translating).",
        position = 3,
        closedByDefault = false)
    String SECTION_PLAYER_MESSAGES = "playerMessages";

    @ConfigItem(
        name = "Public",
        description = "Allow translating public messages from other players (requires dynamic translating).",
        position = 0,
        keyName = "publicChat",
        section = SECTION_PLAYER_MESSAGES)
    default boolean allowPublic()
    {
        return true;
    }

    @ConfigItem(
        name = "Clan",
        description = "Allow translating messages from clan chat (requires dynamic translating).",
        position = 1,
        keyName = "clanChat",
        section = SECTION_PLAYER_MESSAGES)
    default boolean allowClan()
    {
        return true;
    }

    @ConfigItem(
        name = "Guest Clan",
        description = "Allow translating messages from guest clan chat (requires dynamic translating).",
        position = 2,
        keyName = "guestClanChat",
        section = SECTION_PLAYER_MESSAGES)
    default boolean allowGuestClan()
    {
        return true;
    }

    @ConfigItem(
        name = "Friends",
        description = "Allow translating messages from friends (requires dynamic translating).",
        position = 3,
        keyName = "friendsChat",
        section = SECTION_PLAYER_MESSAGES)
    default boolean allowFriends()
    {
        return true;
    }

    @ConfigItem(
        name = "GIM Group",
        description = "Allow translating messages from your GIM chat (requires dynamic translating).",
        position = 4,
        keyName = "GIMChat",
        section = SECTION_PLAYER_MESSAGES)
    default boolean allowGIM()
    {
        return true;
    }

    @ConfigItem(
        name = "Override allow friends",
        description = "Override settings from this scope to allow friend messages translating from any source.",
        position = 5,
        keyName = "overrideFriends",
        section = SECTION_PLAYER_MESSAGES)
    default boolean overrideFriends()
    {
        return true;
    }

    @ConfigItem(
        name = "Override ignores",
        description = "Override settings from this scope to ignore any messages from ignored players.",
        position = 6,
        keyName = "overrideIgnores",
        section = SECTION_PLAYER_MESSAGES)
    default boolean overrideIgnores()
    {
        return true;
    }

    @ConfigItem(
            name = "Override with whitelist",
            description = "If enabled, only players in the whitelist will be translated.",
            position = 7,
            keyName = "overrideList",
            section = SECTION_PLAYER_MESSAGES)
    default boolean allowOverrideList()
    {
        return false;
    }

    @ConfigItem(
            name = "Translate with APIs",
            description = "Specific players to translate using online translators",
            position = 8,
            keyName = "specificApiTranslate",
            section = SECTION_PLAYER_MESSAGES
    )
    default String overrideWhitelist()
    {
        return "";
    }

    @ConfigSection(
        name = "My messages",
        description = "Settings for your own, sent chat messages",
        position = 4,
        closedByDefault = false)
    String SECTION_MY_MESSAGES = "myMessages";

    @ConfigItem(
        name = "Public",
        description = "Allow translating my public messages (requires dynamic translating).",
        position = 0,
        keyName = "myPublic",
        section = SECTION_MY_MESSAGES)
    default boolean allowMyPublic()
    {
        return true;
    }

    @ConfigItem(
        name = "Clan",
        description = "Allow translating my messages on clan chat (requires dynamic translating).",
        position = 1,
        keyName = "myClanChat",
        section = SECTION_MY_MESSAGES)
    default boolean allowMyClan()
    {
        return true;
    }

    @ConfigItem(
        name = "Guest Clan",
        description = "Allow translating my messages on guest clan chat (requires dynamic translating).",
        position = 2,
        keyName = "myGuestClanChat",
        section = SECTION_MY_MESSAGES)
    default boolean allowMyGuestClan()
    {
        return true;
    }

    @ConfigItem(
        name = "Friends",
        description = "Allow translating my messages to friends (requires dynamic translating).",
        position = 3,
        keyName = "myFriendsChat",
        section = SECTION_MY_MESSAGES)
    default boolean allowMyFriends()
    {
        return true;
    }

    @ConfigItem(
        name = "GIM Group",
        description = "Allow translating my messages to GIM chat (requires dynamic translating).",
        position = 4,
        keyName = "myGIMChat",
        section = SECTION_MY_MESSAGES)
    default boolean allowMyGIM()
    {
        return true;
    }
}





