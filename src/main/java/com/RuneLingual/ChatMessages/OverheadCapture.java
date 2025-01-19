package com.RuneLingual.ChatMessages;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Transformer;
import com.RuneLingual.commonFunctions.Transformer.TransformOption;
import com.RuneLingual.ChatMessages.ChatCapture;
import com.RuneLingual.RuneLingualConfig;

import net.runelite.api.*;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.client.game.ChatIconManager;

import javax.inject.Inject;
import java.util.HashMap;


public class OverheadCapture {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private Client client;
    @Inject
    private PlayerMessage playerMessage;
    @Inject
    Transformer transformer;


    @Inject
    public OverheadCapture(RuneLingualPlugin plugin) {
        this.plugin = plugin;
    }

    public void translateOverhead(OverheadTextChanged event) throws Exception {
        String enMsg = event.getOverheadText();
        Actor actor = event.getActor();

        String name;
        if (actor.getName() != null) {
            name = Colors.removeAllTags(actor.getName());
        } else {
            name = null;
        }

        TransformOption option = getOverheadOption(actor);
        if(option == TransformOption.AS_IS)
            return;

        if (option == TransformOption.TRANSLATE_API) {
            String pastTranslation = plugin.getDeepl().getDeeplPastTranslationManager().getPastTranslation(enMsg);
            if(pastTranslation != null) { // have translated before
                String textToDisplay = strToYellowDisplayStr(pastTranslation);
                event.getActor().setOverheadText(textToDisplay);
            } else { // never translated before
                translateOverheadWithApi(event, enMsg);
            }
        }
        else if (option == TransformOption.TRANSFORM) {
            String japaneseMsg = plugin.getChatInputRLingual().transformChatText(enMsg);
            String textToDisplay = strToYellowDisplayStr(japaneseMsg);
            event.getActor().setOverheadText(textToDisplay);
        }
        else if (option == TransformOption.TRANSLATE_LOCAL) {// todo: would need to test this
            SqlQuery dialogueQuery = new SqlQuery(plugin);
            dialogueQuery.setDialogue(enMsg, name, false , Colors.yellow);
            String localTranslation = transformer.transform(enMsg, TransformOption.TRANSLATE_LOCAL, dialogueQuery,Colors.yellow, false);
            event.getActor().setOverheadText(localTranslation);
        }
    }

    private void translateOverheadWithApi(OverheadTextChanged event, String enMsg) {
        Thread thread = new Thread(() -> {//process with new thread because games freezes while waiting for api response
            try {
                String apiTranslation = plugin.getDeepl().translate(Colors.removeAllTags(enMsg),
                                            LangCodeSelectableList.ENGLISH,
                                            plugin.getConfig().getSelectedLanguage());
                String textToDisplay = strToYellowDisplayStr(apiTranslation);

                event.getActor().setOverheadText(textToDisplay);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        thread.setDaemon(false);
        thread.start();
    }

    private String strToYellowDisplayStr(String str){
        Transformer transformer = new Transformer(plugin);
        return transformer.stringToDisplayedString(str, Colors.yellow);
    }

    private TransformOption getOverheadOption(Actor actor){
        if (actor instanceof NPC || actor instanceof GameObject){//is overhead of NPC
            switch (plugin.getConfig().getNpcDialogueConfig()) {
                case DONT_TRANSLATE:
                    return TransformOption.AS_IS;
                case USE_LOCAL_DATA:
                    return TransformOption.TRANSLATE_LOCAL;
                case USE_API:
                    return TransformOption.TRANSLATE_API;
            }
        }
        String name = actor.getName();
        if(name != null) {
            name = Colors.removeAllTags(name);
            //the player is the local player
            if (name.equals(client.getLocalPlayer().getName())) {
                //return playerMessage.getTranslationOption();
                return getMatchingOption(plugin.getConfig().getMyPublicConfig());
            }

            // check inside forceful config setting
            if (plugin.getChatCapture().isInConfigList(name, plugin.getConfig().getSpecificDontTranslate()))
                return TransformOption.AS_IS;
            if (plugin.getChatCapture().isInConfigList(name, plugin.getConfig().getSpecificTransform()))
                return TransformOption.TRANSFORM;
            if (plugin.getChatCapture().isInConfigList(name, plugin.getConfig().getSpecificApiTranslate()))
                return TransformOption.TRANSLATE_API;

            // if its from a friend
//            if (client.isFriended(name, true)) {
//                return getMatchingOption(plugin.getConfig().getAllFriendsConfig());
//            }

            // if its not from local player nor a friend
            return getMatchingOption(plugin.getConfig().getPublicChatConfig());
        }
        return TransformOption.AS_IS;
    }

    private TransformOption getMatchingOption(RuneLingualConfig.chatSelfConfig configSelf){
        switch (configSelf) {
            case LEAVE_AS_IS:
                return TransformOption.AS_IS;
            case TRANSFORM:
                return TransformOption.TRANSFORM;
            default:
                return TransformOption.AS_IS;
        }
    }

    private TransformOption getMatchingOption(RuneLingualConfig.chatConfig config){
        switch (config) {
            case LEAVE_AS_IS:
                return TransformOption.AS_IS;
            case TRANSFORM:
                return TransformOption.TRANSFORM;
            case USE_API:
                return TransformOption.TRANSLATE_API;
            default:
                return TransformOption.AS_IS;
        }
    }

}
