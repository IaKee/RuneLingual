package com.RuneLingual.Wigets;

import com.RuneLingual.*;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Transformer;
import com.RuneLingual.commonFunctions.Transformer.TransformOption;
import com.RuneLingual.nonLatin.GeneralFunctions;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;

import net.runelite.api.widgets.WidgetUtil;

import static com.RuneLingual.Wigets.WidgetsUtilRLingual.removeBrAndTags;
import com.RuneLingual.Wigets.WidgetsUtilRLingual.*;


@Slf4j
public class DialogTranslator {
    // Dialog happens in a separate widget than the ChatBox itself
    // not limited to npc conversations themselves, but also chat actions
    @Inject
    private Client client;
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private RuneLingualConfig config;

    // player widget ids
    @Getter
    private final int playerNameWidgetId = 14221316;
    @Getter
    private final int playerContinueWidgetId = 14221317;
    @Getter
    private final int playerContentWidgetId = 14221318;

    // npc widget ids
    @Getter
    private final int npcNameWidgetId = 15138820;
    @Getter
    private final int npcContinueWidgetId = 15138821;
    @Getter
    private final int npcContentWidgetId = 15138822;

    // dialog option widget ids
    @Getter
    private final int dialogOptionWidgetId = 14352385; // each and every line of the option dialogue has this id, even the red "select an option" text


    private final Colors defaultTextColor = Colors.black;
    private final Colors continueTextColor = Colors.blue;
    private final String continueText = "Click here to continue";
    private final Colors nameAndSelectOptionTextColor = Colors.red;
    private final String selectOptionText = "Select an option";
    private final Colors pleaseWaitTextColor = Colors.blue;
    private final String pleaseWaitText = "Please wait...";

    private TransformOption dialogOption;
    private TransformOption npcNameOption;
    @Inject
    Transformer transformer;

    @Inject
    private GeneralFunctions generalFunctions;
    @Inject
    private WidgetsUtilRLingual widgetsUtilRLingual;

    @Inject
    public DialogTranslator(RuneLingualConfig config, Client client, RuneLingualPlugin plugin) {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        this.transformer = new Transformer(plugin);
    }

    public void handleDialogs(Widget widget) {
        if(widget.getText().contains("<img=")) {
            return;
        }
        dialogOption = MenuCapture.getTransformOption(plugin.getConfig().getNpcDialogueConfig());
        npcNameOption = MenuCapture.getTransformOption(plugin.getConfig().getNPCNamesConfig());
        if ((widget.getId() != npcNameWidgetId && dialogOption.equals(TransformOption.AS_IS))
                || (widget.getId() == npcNameWidgetId && npcNameOption.equals(TransformOption.AS_IS))) {
            return;
        }

        int interfaceID = WidgetUtil.componentToInterface(widget.getId());

        // if the widget is the npc name widget, and the config is set to use api translation
        if (npcNameOption.equals(TransformOption.TRANSLATE_API) && widget.getId() == npcNameWidgetId) {
            String npcName = widget.getText();
            Thread thread = new Thread(() -> {
                widgetsUtilRLingual.setWidgetText_ApiTranslation(widget, npcName, nameAndSelectOptionTextColor);
            });
            thread.setDaemon(false);
            thread.start();
            return;
        }
        // if the widget is not npc name nor player name, and the config is set to use api translation
        else if (dialogOption.equals(TransformOption.TRANSLATE_API) && widget.getId() != playerNameWidgetId && widget.getId() != npcNameWidgetId) {
            String dialogText = widget.getText();

            Colors[] textColor = {defaultTextColor};
            if(widget.getId() == npcContinueWidgetId || widget.getId() == playerContinueWidgetId)
                textColor[0] = continueTextColor;
            else if(widget.getId() == dialogOptionWidgetId && widget.getText().equals(selectOptionText))
                textColor[0] = nameAndSelectOptionTextColor;

            Thread thread = new Thread(() -> {
                widgetsUtilRLingual.setWidgetText_ApiTranslation(widget, dialogText, textColor[0]);
            });
            thread.setDaemon(false);
            thread.start();
            return;
        }

        switch (interfaceID) {
            case InterfaceID.DIALOG_NPC:
                handleNpcDialog(widget);
                return;
            case InterfaceID.DIALOG_PLAYER:
                handlePlayerDialog(widget);
                return;
            case InterfaceID.DIALOG_OPTION:
                handleOptionDialog(widget);
                return;
            default:
                break;
        }
        log.info("Unknown dialog widget: " + widget.getId());
    }

    private void handleNpcDialog(Widget widget) {
        if (widget.getId() == npcNameWidgetId) {
            String npcName = widget.getText();
            npcName = removeBrAndTags(npcName);

            SqlQuery query = new SqlQuery(this.plugin);
            query.setNpcName(npcName, nameAndSelectOptionTextColor);
            String translatedText = transformer.transform(npcName, nameAndSelectOptionTextColor,
                    npcNameOption, query, false);
            widget.setText(translatedText);
        } else if (widget.getId() == npcContinueWidgetId) {
            translateContinueWidget(widget);
        } else if (widget.getId() == npcContentWidgetId) {
            String npcContent = widget.getText(); // this can contain tags like <br> and probably color tags
            npcContent = removeBrAndTags(npcContent);
            String npcName = getInteractingNpcName();
            SqlQuery query = new SqlQuery(this.plugin);
            query.setDialogue(npcContent, npcName, false, defaultTextColor);
            String translatedText = transformer.transform(npcContent, defaultTextColor, dialogOption, query, false);
            widgetsUtilRLingual.setWidgetText_NiceBr(widget, translatedText);
        }
    }

    private void handlePlayerDialog(Widget widget) {
        if (widget.getId() == playerContinueWidgetId) {
            log.info(widget.getText());
            translateContinueWidget(widget);
            return;
        }
        if (widget.getId() == playerContentWidgetId) {
            String playerContent = widget.getText(); // this can contain tags like <br> and probably color tags
            playerContent = removeBrAndTags(playerContent);


            String npcName = getInteractingNpcName();
            log.info("playerContent: " + playerContent + " with npc: " + npcName);

            SqlQuery query = new SqlQuery(this.plugin);
            query.setDialogue(playerContent, npcName, true, defaultTextColor);
            String translatedText = transformer.transform(playerContent, defaultTextColor, dialogOption, query, false);
            widgetsUtilRLingual.setWidgetText_NiceBr(widget, translatedText);
        }
        // player name does not need to be translated
    }

    private void handleOptionDialog(Widget widget) {
        // the red "Select an option" text is not tagged with red color
        String dialogOption = widget.getText();
        if (dialogOption.equals(selectOptionText)) {
            widget.setText(getSelectOptionTranslation());
            return;
        }
        if (dialogOption.equals(pleaseWaitText)) {
            widget.setText(getPleaseWaitTranslation());
            return;
        }
        dialogOption = removeBrAndTags(dialogOption);
        SqlQuery query = new SqlQuery(this.plugin);
        query.setDialogue(dialogOption, getInteractingNpcName(), false, defaultTextColor);
        String translatedText = transformer.transform(dialogOption, defaultTextColor, this.dialogOption, query, false);
        widgetsUtilRLingual.setWidgetText_NiceBr(widget, translatedText);
    }

    private String getInteractingNpcName() {
        NPC npc = plugin.getInteractedNpc();
        if (npc == null) {
            return "";
        }
        return npc.getName();
    }

    private String getContinueTranslation() {
        SqlQuery query = new SqlQuery(this.plugin);
        query.setDialogue(continueText, "", true, continueTextColor);
        return transformer.transform(continueText, continueTextColor, dialogOption, query, false);
    }

    private String getSelectOptionTranslation() {
        SqlQuery query = new SqlQuery(this.plugin);
        query.setDialogue(selectOptionText, "", true, nameAndSelectOptionTextColor);
        return transformer.transform(selectOptionText, nameAndSelectOptionTextColor, dialogOption, query, false);
    }

    private String getPleaseWaitTranslation() {
        SqlQuery query = new SqlQuery(this.plugin);
        query.setDialogue(pleaseWaitText, "", true, pleaseWaitTextColor);
        return transformer.transform(pleaseWaitText, pleaseWaitTextColor, dialogOption, query, false);
    }

    private void translateContinueWidget(Widget widget) {
        if (widget.getText().equals(continueText)) {
            widget.setText(getContinueTranslation());
        } else if (widget.getText().equals(pleaseWaitText)) {
            widget.setText(getPleaseWaitTranslation());
        }
    }
}