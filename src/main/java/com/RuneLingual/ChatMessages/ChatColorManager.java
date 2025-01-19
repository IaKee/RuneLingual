package com.RuneLingual.ChatMessages;


import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;

import javax.inject.Inject;

@Slf4j
public class ChatColorManager {
    @Inject
    private Client client;
    @Inject
    private RuneLingualPlugin plugin;
    @Getter
    private Colors messageColor = Colors.black;

    @Inject
    public ChatColorManager(RuneLingualPlugin plugin) {
        this.plugin = plugin;
        this.client = plugin.getClient();
    }



    public void setMessageColor(ChatMessageType chatMessageType){
        int opaqueInt = 0;
        if (client.getVarbitValue(Varbits.TRANSPARENT_CHATBOX) == opaqueInt) {
            messageColor = getOpaqueBoxMessageColor(chatMessageType);
        } else {
            messageColor = getTransparentBoxMessageColor(chatMessageType);
        }
    }

    public Colors getOpaqueBoxMessageColor(ChatMessageType type) {
        int color;
        switch (type) {
            case PUBLICCHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_PUBLIC);
                break;
            case PRIVATECHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_PRIVATE);
                break;
            case AUTOTYPER:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_AUTO);
                break;
            case BROADCAST:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_BROADCAST);
                break;
            case FRIENDSCHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_FRIEND);
                break;
            case CLAN_CHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_CLAN);
                break;
            case CLAN_GUEST_CHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_GUEST_CLAN);
                break;
            case CLAN_MESSAGE:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_CLAN_BROADCAST);
                break;
            case CLAN_GIM_CHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_IRON_GROUP_CHAT);
                break;
            case CLAN_GIM_MESSAGE:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_IRON_GROUP_BROADCAST);
                break;
            case TRADE:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_TRADE_REQUEST);
                break;
            case CHALREQ_CLANCHAT:
            case CHALREQ_FRIENDSCHAT:
            case CHALREQ_TRADE:
                color = client.getVarpValue(VarPlayer.SETTINGS_OPAQUE_CHAT_CHALLENGE_REQUEST);
                break;
            default:
                color = 0;
        }
        if(color==0)
            return Colors.black;
        color--;
        return Colors.getColorFromHex(Integer.toHexString(color));
    }

    private Colors getTransparentBoxMessageColor(ChatMessageType type) {
        int color;
        switch (type) {
            case PUBLICCHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_PUBLIC);
                break;
            case PRIVATECHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_PRIVATE);
                break;
            case AUTOTYPER:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_AUTO);
                break;
            case BROADCAST:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_BROADCAST);
                break;
            case FRIENDSCHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_FRIEND);
                break;
            case CLAN_CHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_CLAN);
                break;
            case CLAN_GUEST_CHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_GUEST_CLAN);
                break;
            case CLAN_MESSAGE:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_CLAN_BROADCAST);
                break;
            case CLAN_GIM_CHAT:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_IRON_GROUP_CHAT);
                break;
            case CLAN_GIM_MESSAGE:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_IRON_GROUP_BROADCAST);
                break;
            case TRADE:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_TRADE_REQUEST);
                break;
            case CHALREQ_CLANCHAT:
            case CHALREQ_FRIENDSCHAT:
            case CHALREQ_TRADE:
                color = client.getVarpValue(VarPlayer.SETTINGS_TRANSPARENT_CHAT_CHALLENGE_REQUEST);
                break;
            default:
                color = 0;
        }
        color--;
        return Colors.getColorFromHex(Integer.toHexString(color));
    }
}
