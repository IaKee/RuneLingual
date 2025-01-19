package com.RuneLingual.ChatMessages;

import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Transformer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import net.runelite.api.clan.ClanChannel;

import javax.inject.Inject;

@Slf4j
public class PlayerMessage {
    @Inject
    Client client;
    @Inject
    RuneLingualConfig config;
    @Inject
    RuneLingualPlugin plugin;
    @Inject
    ChatCapture chatCapture;

    @Inject
    public PlayerMessage(Client client, RuneLingualConfig config, RuneLingualPlugin plugin){
        this.plugin = plugin;
        this.client = plugin.getClient();
        this.config = plugin.getConfig();
        this.chatCapture = plugin.getChatCapture();
    }

    public enum talkingIn {
        PUBLIC,
        CHANNEL,
        CLAN,
        GUEST_CLAN,
        GIM,
        NONE
    }

    public Transformer.TransformOption getTranslationOption(){
        talkingIn talkingIn = getTalkingIn();
        if(talkingIn == PlayerMessage.talkingIn.PUBLIC){
            return getMyChatConfig(config.getMyPublicConfig());
        }
        if(talkingIn == PlayerMessage.talkingIn.CHANNEL){
            return getMyChatConfig(config.getMyFcConfig());
        }
        if(talkingIn == PlayerMessage.talkingIn.CLAN){
            return getMyChatConfig(config.getMyClanConfig());
        }
        if(talkingIn == PlayerMessage.talkingIn.GUEST_CLAN){
            return getMyChatConfig(config.getMyGuestClanConfig());
        }
        if(talkingIn == PlayerMessage.talkingIn.GIM){
            return getMyChatConfig(config.getMyGIMConfig());
        }
        if(talkingIn == PlayerMessage.talkingIn.NONE){
            return Transformer.TransformOption.AS_IS;
        }
        return Transformer.TransformOption.AS_IS;
    }

    public talkingIn getTalkingIn() {

        ChatCapture.openChatbox chatbox = chatCapture.getOpenChatbox();
        ChatCapture.chatModes chatMode = chatCapture.getChatMode();

        if(chatbox == ChatCapture.openChatbox.CLOSED) {
            return talkingIn.NONE;
        }

        // check the chat box for chat code, such as /// and /g etc
        if(typedPublicCode()){
            return talkingIn.PUBLIC;
        }

        if(typedFriendsChannelCode()){
            if(joinedFC())
                return talkingIn.CHANNEL;
            else
                return talkingIn.PUBLIC;
        }

        if(typedClanCode()){
            if(joinedClan()){
                if(joinedGIM())
                    return talkingIn.GIM;
                else
                    return talkingIn.CLAN;
            } else {
                return talkingIn.NONE;
            }
        }

        if(typedGuestClanCode()){
            if(joinedGuestClan())
                return talkingIn.GUEST_CLAN;
            else
                return talkingIn.NONE;
        }

        if(typedGimCode()){
            if(joinedGIM())
                return talkingIn.GIM;
            else {
                if(getChatInputString().startsWith("////")){
                    if(joinedGuestClan())
                        return talkingIn.GUEST_CLAN;
                    else
                        return talkingIn.NONE;
                }
                else if(getChatInputString().startsWith("/g"))
                    return talkingIn.PUBLIC;
                else if(getChatInputString().startsWith("@g")){
                    if(joinedClan())
                        return talkingIn.CLAN;
                    else
                        return talkingIn.NONE;
                }
            }
        }

        // if no chat code is found
        if(chatMode == ChatCapture.chatModes.PUBLIC) {
            if (chatbox == ChatCapture.openChatbox.ALL
                    || chatbox == ChatCapture.openChatbox.GAME
                    || chatbox == ChatCapture.openChatbox.PUBLIC
                    || chatbox == ChatCapture.openChatbox.PRIVATE) {
                return talkingIn.PUBLIC;
            }

            // if friends chat tab is opened
            if (chatbox == ChatCapture.openChatbox.CHANNEL) {
                if(joinedFC())
                    return talkingIn.CHANNEL;
                else
                    return talkingIn.PUBLIC;
            }

            // if clan chat tab is opened
            if (chatbox == ChatCapture.openChatbox.CLAN) {
                if(joinedClan())
                    return talkingIn.CLAN;
                else
                    return talkingIn.PUBLIC;
            }

            // if trade or gim tab is opened
            if (chatbox == ChatCapture.openChatbox.TRADE_GIM) {
                if(joinedGIM())
                    return talkingIn.GIM;
                else
                    return talkingIn.PUBLIC;
            }

        }
        else if(chatMode == ChatCapture.chatModes.CHANNEL){
            if(!joinedFC()){
                if(chatbox == ChatCapture.openChatbox.ALL
                        || chatbox == ChatCapture.openChatbox.GAME
                        || chatbox == ChatCapture.openChatbox.PUBLIC
                        || chatbox == ChatCapture.openChatbox.PRIVATE) {
                    return talkingIn.NONE;
                }
                if(chatbox == ChatCapture.openChatbox.CHANNEL){
                    return talkingIn.PUBLIC;
                }

                if(chatbox == ChatCapture.openChatbox.CLAN && joinedClan()){
                    return talkingIn.CLAN;
                } else if(chatbox == ChatCapture.openChatbox.CLAN && !joinedClan()){
                    return talkingIn.NONE;
                }

                if(chatbox == ChatCapture.openChatbox.TRADE_GIM && joinedGIM()){
                    return talkingIn.GIM;
                } else if(chatbox == ChatCapture.openChatbox.TRADE_GIM && !joinedGIM()){
                    return talkingIn.NONE;
                }
            }
            if(joinedFC()){
                if(chatbox == ChatCapture.openChatbox.ALL
                        || chatbox == ChatCapture.openChatbox.GAME
                        || chatbox == ChatCapture.openChatbox.PUBLIC
                        || chatbox == ChatCapture.openChatbox.PRIVATE
                        || chatbox == ChatCapture.openChatbox.CHANNEL) {
                    return talkingIn.CHANNEL;
                }

                if(chatbox == ChatCapture.openChatbox.CLAN && joinedClan()){
                    return talkingIn.CLAN;
                } else if(chatbox == ChatCapture.openChatbox.CLAN && !joinedClan()){
                    return talkingIn.NONE;
                }

                if(chatbox == ChatCapture.openChatbox.TRADE_GIM && joinedGIM()){
                    return talkingIn.GIM;
                } else if(chatbox == ChatCapture.openChatbox.TRADE_GIM && !joinedGIM()){
                    return talkingIn.CHANNEL;
                }
            }
        }
        else if(chatMode == ChatCapture.chatModes.CLAN){
            if(!joinedClan()){
                if(chatbox == ChatCapture.openChatbox.ALL
                        || chatbox == ChatCapture.openChatbox.GAME
                        || chatbox == ChatCapture.openChatbox.PUBLIC
                        || chatbox == ChatCapture.openChatbox.PRIVATE
                        || chatbox == ChatCapture.openChatbox.CLAN) {
                    return talkingIn.NONE;
                }

                if(chatbox == ChatCapture.openChatbox.CHANNEL && joinedFC()){
                    return talkingIn.CHANNEL;
                } else if(chatbox == ChatCapture.openChatbox.CHANNEL && !joinedFC()){
                    return talkingIn.PUBLIC;
                }
                if(chatbox == ChatCapture.openChatbox.TRADE_GIM && joinedGIM()){
                    return talkingIn.GIM;
                } else if(chatbox == ChatCapture.openChatbox.TRADE_GIM && !joinedGIM()){
                    return talkingIn.NONE;
                }

            }
            if(joinedClan()){
                if(chatbox == ChatCapture.openChatbox.ALL
                        || chatbox == ChatCapture.openChatbox.GAME
                        || chatbox == ChatCapture.openChatbox.PUBLIC
                        || chatbox == ChatCapture.openChatbox.PRIVATE
                        || chatbox == ChatCapture.openChatbox.CLAN) {
                    return talkingIn.CLAN;
                }

                if(chatbox == ChatCapture.openChatbox.CHANNEL && joinedFC()){
                    return talkingIn.CHANNEL;
                } else if(chatbox == ChatCapture.openChatbox.CHANNEL && !joinedFC()){
                    return talkingIn.PUBLIC;
                }
                if(chatbox == ChatCapture.openChatbox.TRADE_GIM && joinedGIM()){
                    return talkingIn.GIM;
                } else if(chatbox == ChatCapture.openChatbox.TRADE_GIM && !joinedGIM()){
                    return talkingIn.CLAN;
                }

            }
        }
        else if(chatMode == ChatCapture.chatModes.GUEST_CLAN){
            if(!joinedGuestClan()){
                if(chatbox == ChatCapture.openChatbox.ALL
                        || chatbox == ChatCapture.openChatbox.GAME
                        || chatbox == ChatCapture.openChatbox.PUBLIC
                        || chatbox == ChatCapture.openChatbox.PRIVATE) {
                    return talkingIn.NONE;
                }

                if(chatbox == ChatCapture.openChatbox.CHANNEL && joinedFC()){
                    return talkingIn.CHANNEL;
                } else if(chatbox == ChatCapture.openChatbox.CHANNEL && !joinedFC()){
                    return talkingIn.PUBLIC;
                }
                if(chatbox == ChatCapture.openChatbox.CLAN && joinedClan()){
                    return talkingIn.CLAN;
                } else if(chatbox == ChatCapture.openChatbox.CLAN && !joinedClan()){
                    return talkingIn.NONE;
                }
                if(chatbox == ChatCapture.openChatbox.TRADE_GIM && joinedGIM()){
                    return talkingIn.GIM;
                } else if(chatbox == ChatCapture.openChatbox.TRADE_GIM && !joinedGIM()){
                    return talkingIn.NONE;
                }

            }
            if(joinedGuestClan()){
                if(chatbox == ChatCapture.openChatbox.ALL
                        || chatbox == ChatCapture.openChatbox.GAME
                        || chatbox == ChatCapture.openChatbox.PUBLIC
                        || chatbox == ChatCapture.openChatbox.PRIVATE) {
                    return talkingIn.GUEST_CLAN;
                }
                if(chatbox == ChatCapture.openChatbox.CHANNEL && joinedFC()){
                    return talkingIn.CHANNEL;
                } else if(chatbox == ChatCapture.openChatbox.CHANNEL && !joinedFC()){
                    return talkingIn.PUBLIC;
                }
                if(chatbox == ChatCapture.openChatbox.CLAN && joinedClan()){
                    return talkingIn.CLAN;
                } else if(chatbox == ChatCapture.openChatbox.CLAN && !joinedClan()){
                    return talkingIn.NONE;
                }
                if(chatbox == ChatCapture.openChatbox.TRADE_GIM && joinedGIM()){
                    return talkingIn.GIM;
                } else if(chatbox == ChatCapture.openChatbox.TRADE_GIM && !joinedGIM()){
                    return talkingIn.GUEST_CLAN;
                }
            }
        }
        else if(chatMode == ChatCapture.chatModes.GROUP){
            if(!joinedGIM()) {
                // this shouldn't even happen, but to be safe
                return talkingIn.NONE;
            }
            if(joinedGIM()){
                if(chatbox == ChatCapture.openChatbox.ALL
                        || chatbox == ChatCapture.openChatbox.GAME
                        || chatbox == ChatCapture.openChatbox.PUBLIC
                        || chatbox == ChatCapture.openChatbox.PRIVATE
                        || chatbox == ChatCapture.openChatbox.TRADE_GIM) {
                    return talkingIn.GIM;
                }
                if(chatbox == ChatCapture.openChatbox.CHANNEL && joinedFC()){
                    return talkingIn.CHANNEL;
                } else if(chatbox == ChatCapture.openChatbox.CHANNEL && !joinedFC()){
                    return talkingIn.PUBLIC;
                }
                if(chatbox == ChatCapture.openChatbox.CLAN && joinedClan()){
                    return talkingIn.CLAN;
                } else if(chatbox == ChatCapture.openChatbox.CLAN && !joinedClan()){
                    return talkingIn.NONE;
                }
            }
        }
        //log.info("warning: no chat mode found");
        return  talkingIn.NONE;
    }

    private boolean joinedFC(){
        FriendsChatManager fc = client.getFriendsChatManager();
        return fc != null;
    }

    private boolean joinedClan() {
        ClanChannel clan = client.getClanChannel();
        return clan != null;
    }

    private boolean joinedGIM() {
        int account_type = client.getVarbitValue(Varbits.ACCOUNT_TYPE);
        return account_type == 4 || account_type == 5 || account_type == 6;
    }

    private boolean joinedGuestClan() {
        ClanChannel guestClan = client.getGuestClanChannel();
        return guestClan != null;
    }


    private boolean typedPublicCode() {
        if(getChatInputString().startsWith("/@p"))
            return true;
        return false;
    }

    private boolean typedFriendsChannelCode() {
        if((getChatInputString().startsWith("/") && !getChatInputString().startsWith("//")
                && !getChatInputString().startsWith("/c ") && !getChatInputString().startsWith("/@c")
                && !getChatInputString().startsWith("/gc ") && !getChatInputString().startsWith("/@gc")
                && !getChatInputString().startsWith("/g ") && !getChatInputString().startsWith("/@g")
                && !getChatInputString().startsWith("/@p"))
                || getChatInputString().startsWith("/@f"))
            return true;
        return false;
    }
    private boolean typedClanCode() {
        if((getChatInputString().startsWith("//") && !getChatInputString().startsWith("///"))
                || getChatInputString().startsWith("/c ")
                || getChatInputString().startsWith("/@c"))
            return true;
        return false;
    }
    private boolean typedGuestClanCode() {
        if((getChatInputString().startsWith("///") && !getChatInputString().startsWith("////"))
                || getChatInputString().startsWith("/gc ")
                || getChatInputString().startsWith("/@gc"))
            return true;
        return false;
    }
    private boolean typedGimCode() {
        if(getChatInputString().startsWith("////")
                || getChatInputString().startsWith("/g ")
                || (getChatInputString().startsWith("/@g") && !getChatInputString().startsWith("/@gc")) )
            return true;
        return false;
    }

    public String getChatInputString(){
        return client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT);
    }

    public Transformer.TransformOption getMyChatConfig(RuneLingualConfig.chatSelfConfig config) {
        if(config == RuneLingualConfig.chatSelfConfig.LEAVE_AS_IS){
            return Transformer.TransformOption.AS_IS;
        }
        if(config == RuneLingualConfig.chatSelfConfig.TRANSFORM){
            return Transformer.TransformOption.TRANSFORM;
        }
        return  Transformer.TransformOption.AS_IS;
    }
}
