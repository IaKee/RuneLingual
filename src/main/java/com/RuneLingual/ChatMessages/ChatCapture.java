package com.RuneLingual.ChatMessages;

import com.RuneLingual.*;
import com.RuneLingual.ApiTranslate.Deepl;
import com.RuneLingual.SidePanelComponents.ChatBoxSection;
import com.RuneLingual.commonFunctions.Transformer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;

import javax.inject.Inject;

import lombok.Setter;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Transformer.TransformOption;

@Slf4j
public class ChatCapture
{
    /* Captures chat messages from any source
    ignores npc dialog, as they are handled in DialogCapture*/
    
    @Inject
    private Client client;
    @Inject
    private RuneLingualConfig config;
    @Inject @Getter
    private RuneLingualPlugin plugin;
    @Inject
    private PlayerMessage playerMessage;
    @Inject
    private Deepl deepl;
    @Inject
    private ChatColorManager chatColorManager;

    
    @Inject
    ChatCapture(RuneLingualConfig config, Client client, RuneLingualPlugin plugin)
    {
        this.config = config;
        this.client = client;
        this.plugin = plugin;
        this.deepl = plugin.getDeepl();
    }

    public enum openChatbox{
        ALL,
        GAME,
        PUBLIC,
        PRIVATE,
        CHANNEL,
        CLAN,
        TRADE_GIM,
        CLOSED
    }

    public enum chatModes {
        PUBLIC,
        CHANNEL,
        CLAN,
        GUEST_CLAN,
        GROUP
    }


    public void handleChatMessage(ChatMessage chatMessage) throws Exception {
        ChatMessageType type = chatMessage.getType();
        MessageNode messageNode = chatMessage.getMessageNode();
        String message = chatMessage.getMessage();// e.g.<col=6800bf>Some cracks around the cave begin to ooze water.
        //log.info("Chat message received: " + message + " | type: " + type.toString() + " | name: " + chatMessage.getName());
        String name = chatMessage.getName(); // getName always returns player name
        TransformOption translationOption;


//        // debug
//        log.info("Chat message received: " + message + " | type: " + type.toString() + " | name: " + name);
//        openChatbox chatbox = getOpenChatbox();
//        chatModes chatMode = getChatMode();
//        log.info("Chatbox: " + chatbox.toString() + " | Chat mode: " + chatMode.toString());
//        PlayerMessage.talkingIn talkingIn = playerMessage.getTalkingIn();
//        log.info("Talking in: " + talkingIn.toString());
//        TransformOption translationOption = playerMessage.getTranslationOption();
//        log.info("player translation option: " + translationOption.toString());

        switch (chatMessage.getType()) {
            case MESBOX: // dont support this, yet
            case DIALOG: // will be treated in dialogCapture
                return;
            default:
                translationOption = getTranslationOption(chatMessage);
        }

        //log.info("Translation option: " + translationOption.toString());
        chatColorManager.setMessageColor(chatMessage.getMessageNode().getType());
        switch (translationOption) {
            case AS_IS:
                return;
            case TRANSLATE_LOCAL:
                localTranslator(message, messageNode, chatMessage);
                break;
            case TRANSLATE_API:
                Thread thread = new Thread(() -> {
                    onlineTranslator(message, messageNode, chatMessage);
                });
                thread.setDaemon(false);
                thread.start();
                break;
            case TRANSFORM: // ex: konnnitiha -> こんにちは
                chatTransformer(message, messageNode, chatMessage);
                break;
        }

    }
    
    private void localTranslator(String message, MessageNode node, ChatMessage chatMessage)
    {
        // todo after adding transcripts for this type of message
        addMsgToSidePanel(chatMessage, message);
    }
    
    private void onlineTranslator(String message, MessageNode node, ChatMessage chatMessage)
    {
        if(deepl.getDeeplCount() + message.length() + 1000 > deepl.getDeeplLimit())
        {
            log.info("DeepL limit reached, cannot translate message.");
            return;
        }

        String translation = deepl.translate(message, LangCodeSelectableList.ENGLISH, config.getSelectedLanguage());
        Transformer transformer = new Transformer(plugin);
        Colors textColor = chatColorManager.getMessageColor();
        String textToDisplay = transformer.stringToDisplayedString(translation, textColor);
        replaceChatMessage(textToDisplay, node);
        addMsgToSidePanel(chatMessage, translation);
    }

    private void chatTransformer(String message, MessageNode node, ChatMessage chatMessage) {
        String newMessage = plugin.getChatInputRLingual().transformChatText(message);
        Transformer transformer = new Transformer(plugin);
        Colors textColor = chatColorManager.getMessageColor();
        String textToDisplay = transformer.stringToDisplayedString(newMessage, textColor);
        replaceChatMessage(textToDisplay, node);
        addMsgToSidePanel(chatMessage, newMessage);
    }

    private void replaceChatMessage(String newMessage, MessageNode node) {
        if(plugin.getConfig().getSelectedLanguage().needsCharImages()) {
            newMessage = insertBr(newMessage, node);// inserts break line so messages are displayed in multiple lines if they are long
        }
        node.setRuneLiteFormatMessage(newMessage);
        this.client.refreshChat();
    }

    private void addMsgToSidePanel(ChatMessage chatMessage, String newMessage)
    {
        newMessage = Colors.removeAllTags(newMessage);

        String senderName = chatMessage.getName();
        senderName = Colors.removeAllTags(senderName);

        String messageToAdd = senderName.isEmpty() ? newMessage : senderName + ": " + newMessage;
        String chatType;
        ChatBoxSection chatBoxSection = plugin.getPanel().getChatBoxSection();
        switch (chatMessage.getType()) {
            case PUBLICCHAT:
                chatType = chatBoxSection.getTabNamePublic();
                break;
            case CLAN_CHAT:
            case CLAN_GUEST_CHAT:
                chatType = chatBoxSection.getTabNameClan();
                break;
            case FRIENDSCHAT:
                chatType = chatBoxSection.getTabNameChannel();
                break;
            case CLAN_GIM_CHAT:
                chatType = chatBoxSection.getTabNameGIM();
                break;
            default:
                chatType = chatBoxSection.getTabNameGame();
        }
        plugin.getPanel().getChatBoxSection().addSentenceToTab(chatType, messageToAdd);
    }

    private TransformOption getTranslationOption(ChatMessage chatMessage) {
        String playerName = Colors.removeAllTags(chatMessage.getName());
        if (isInConfigList(playerName, config.getSpecificDontTranslate()))
            return TransformOption.AS_IS;
        else if (isInConfigList(playerName, config.getSpecificApiTranslate()))
            return TransformOption.TRANSLATE_API;
        else if (isInConfigList(playerName, config.getSpecificTransform()))
            return TransformOption.TRANSFORM;

        boolean isLocalPlayer = Objects.equals(playerName, client.getLocalPlayer().getName());
        //if its by the player themselves
//        if (Objects.equals(playerName, client.getLocalPlayer().getName())) {
//            return playerMessage.getTranslationOption();
//        }

        // if its from a friend
//        boolean isFriend = client.isFriended(playerName,true);
//        if (isFriend && !isLocalPlayer) {
//            return getChatsChatConfig(config.getAllFriendsConfig());
//        }
        switch (chatMessage.getType()){
            case PUBLICCHAT:
                if(isLocalPlayer)
                    return getChatsChatConfig(config.getMyPublicConfig());
                else
                    return getChatsChatConfig(config.getPublicChatConfig());
            case CLAN_CHAT:
                if(isLocalPlayer)
                    return getChatsChatConfig(config.getMyClanConfig());
                else
                    return getChatsChatConfig(config.getClanChatConfig());
            case CLAN_GUEST_CHAT:
                if(isLocalPlayer)
                    return getChatsChatConfig(config.getMyGuestClanConfig());
                else
                    return getChatsChatConfig(config.getGuestClanChatConfig());
            case FRIENDSCHAT:
                if(isLocalPlayer)
                    return getChatsChatConfig(config.getMyFcConfig());
                else
                    return getChatsChatConfig(config.getFriendsChatConfig());
            case CLAN_GIM_CHAT:
                if (!Objects.equals(playerName, "null") && !playerName.isEmpty())
                    if(isLocalPlayer)
                        return getChatsChatConfig(config.getMyGIMConfig());
                    else
                        return getChatsChatConfig(config.getGIMChatConfig());

            default://if its examine, engine, etc
                switch (config.getGameMessagesConfig()) {
                    case DONT_TRANSLATE:
                        return TransformOption.AS_IS;
                    case USE_LOCAL_DATA:
                        return TransformOption.TRANSLATE_LOCAL;
                    case USE_API:
                        return TransformOption.TRANSLATE_API;
                }
        }
        return TransformOption.AS_IS;
    }


    public TransformOption getChatsChatConfig(RuneLingualConfig.chatConfig chatConfig) {
        switch (chatConfig) {
            case TRANSFORM:
                return TransformOption.TRANSFORM;
            case LEAVE_AS_IS:
                return TransformOption.AS_IS;
            case USE_API:
                return TransformOption.TRANSLATE_API;
            default:
                switch (config.getGameMessagesConfig()) {
                    case USE_API:
                        return TransformOption.TRANSLATE_API;
                    case USE_LOCAL_DATA:
                        return TransformOption.TRANSLATE_LOCAL;
                    default:
                        return TransformOption.AS_IS;
                }
        }
    }

    public TransformOption getChatsChatConfig(RuneLingualConfig.chatSelfConfig chatConfig) {
        switch (chatConfig) {
            case TRANSFORM:
                return TransformOption.TRANSFORM;
            case LEAVE_AS_IS:
                return TransformOption.AS_IS;
            default:
                return TransformOption.AS_IS;
        }
    }



    public boolean isInConfigList(String item, String arrayInString) {
        String[] array = arrayInString.split("[,、\n]");
        for (String s:array)
            if (item.equals(s.trim()))
                return true;
        return false;
    }

    public ChatCapture.openChatbox getOpenChatbox() {
        int chatboxVarbitValue = client.getVarcIntValue(41);
        switch (chatboxVarbitValue) {
            case 0:
                return ChatCapture.openChatbox.ALL;
            case 1:
                return ChatCapture.openChatbox.GAME;
            case 2:
                return ChatCapture.openChatbox.PUBLIC;
            case 3:
                return ChatCapture.openChatbox.PRIVATE;
            case 4:
                return ChatCapture.openChatbox.CHANNEL;
            case 5:
                return ChatCapture.openChatbox.CLAN;
            case 6:
                return ChatCapture.openChatbox.TRADE_GIM;
            case 1337:
                return ChatCapture.openChatbox.CLOSED;
            default:
                log.info("Chatbox not found, defaulting to all");
                return ChatCapture.openChatbox.ALL;
        }
    }

    public chatModes getChatMode() {
        int forceSendVarbitValue = client.getVarcIntValue(945);
        switch(forceSendVarbitValue) {
            case 0:
                return chatModes.PUBLIC;
            case 1:
                return chatModes.CHANNEL;
            case 2:
                return chatModes.CLAN;
            case 3:
                return chatModes.GUEST_CLAN;
            case 4:
                return chatModes.GROUP;
            default:
                log.info("Chat mode not found, defaulting to public");
                return chatModes.PUBLIC;
        }
    }
    private String insertBr(String str, MessageNode messageNode) {
        String name = messageNode.getName();
        String chatName = messageNode.getSender();
        int nameCharCount = replaceTagWithAA(name).length()+2; // swap out IM icons to make it easier to count. +2 because of ": " after name
        int chatNameCount = (chatName == null ? 0:chatName.length()+4); //+2 because of [] brackets
        int enCharCount = nameCharCount + chatNameCount + 8; //+8 because timestamp is probably on
        double enWidth = LangCodeSelectableList.ENGLISH.getChatBoxCharWidth(); //width of 1 en character
        double foreignWidth = plugin.getConfig().getSelectedLanguage().getChatBoxCharWidth(); //width of 1 <img=> character
        int chatWidth = 485;
        int width = chatWidth - (int) (enCharCount*enWidth+2); //-2 just to be safe

        String regex = "(<img=\\d+>)|.";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);

        StringBuilder stringBuilder = new StringBuilder();
        double wLeft = width;
        while(matcher.find()){
            String c = matcher.group();
            if (c.matches("<img=\\d+>"))
                wLeft -= foreignWidth;
            else
                wLeft -= enWidth;
            if (wLeft - foreignWidth < 0){
                wLeft = width;
                stringBuilder.append("<br>");
                stringBuilder.append(c);
            } else {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

    private String replaceTagWithAA (String string){ //"<img=41>sand in sand" into "11sand in sand" for easy counting
        return string.replaceAll("<img=(\\d+)>","AA");
    }
}
