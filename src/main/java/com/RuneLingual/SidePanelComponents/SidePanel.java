package com.RuneLingual.SidePanelComponents;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.net.URI;
import java.util.Map;

import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.commonFunctions.FileActions;
import com.RuneLingual.SidePanelComponents.SearchSection;

@Slf4j

public class SidePanel extends PluginPanel{
    @Inject
    private RuneLingualPlugin plugin;

    String helpLink = RuneLingualConfig.helpLink;
    String titleText = "RuneLingual";
    String helpText = "Help with settings";
    String discordText = "Ask for help on Discord";
    @Getter
    ChatBoxSection chatBoxSection;

    @Inject
    private SidePanel(RuneLingualPlugin plugin){
        LangCodeSelectableList targetLanguage = FileActions.getLangCodeFromFile();
        translatePanelTexts(targetLanguage);

        this.setPreferredSize(new Dimension(200, 1500));
        this.add(createTitleLabel(titleText));
        this.add(createClickableLabel(helpText, helpLink));
        this.add(createClickableLabel(discordText, "https://discord.gg/ehwKcVdBGS"));
        this.add(new JSeparator());
        chatBoxSection = new ChatBoxSection(this, targetLanguage, plugin);
        SearchSection searchSection = new SearchSection(this, targetLanguage, plugin);
    }


    private void translatePanelTexts(LangCodeSelectableList targetLanguage) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            titleText = "RuneLingual";
            helpText = "Help with settings";
            discordText = "Ask for help on Discord";
        } /*else if (targetLanguage == LangCodeSelectableList.PORTUGUÊS_BRASILEIRO) {
            titleText = "RuneLíngual";
            helpText = "Ajuda nas configurações";
            discordText = "Peça ajuda no Discord";
        } else if (targetLanguage == LangCodeSelectableList.NORSK) {
            titleText = "RuneLingval";
            helpText = "Hjelp med innstillinger";
            discordText = "Be om hjelp på Discord";
        }*/else if (targetLanguage == LangCodeSelectableList.日本語) {
            titleText = "ルーンリンガル";
            helpText = "設定のヘルプ";
            discordText = "Discordでヘルプを求める";
        }
        // todo: add more languages as needed
    }

    private JLabel createTitleLabel(String title){
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(new Font("MS Gothic", Font.BOLD, 18)); //todo: change the font if a language requires it
        label.setPreferredSize(new Dimension(200, 20));
        label.setForeground(Color.yellow);
        return label;
    }

    private JLabel createTextLabel(String text){
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("MS Gothic", Font.PLAIN, 14));//todo: change the font if a language requires it
        label.setPreferredSize(new Dimension(200, 20));
        label.setForeground(Color.white);
        return label;
    }

    private JLabel createClickableLabel(String title, String url) {
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(new Font("MS Gothic", Font.PLAIN, 14));//todo: change the font if a language requires it
        label.setPreferredSize(new Dimension(200, 20));
        label.setForeground(Color.white);

        label.addMouseListener(new MouseAdapter() {
               @Override
               public void mouseClicked(MouseEvent e) {
                   try {
                       Desktop.getDesktop().browse(new URI(url));
                   } catch (Exception ex) {
                       ex.printStackTrace();
                   }
               }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                Font font = label.getFont();
                Map attributes = font.getAttributes();
                attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                label.setFont(font.deriveFont(attributes));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                Font font = label.getFont();
                Map attributes = font.getAttributes();
                attributes.put(TextAttribute.UNDERLINE, -1);
                label.setFont(font.deriveFont(attributes));
            }
        });

        return label;
    }
}
