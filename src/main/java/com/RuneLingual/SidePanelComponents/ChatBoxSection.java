package com.RuneLingual.SidePanelComponents;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ChatBoxSection {
    private SidePanel sidePanel;
    @Getter
    private String tabNameGame = "Game";
    @Getter
    private String tabNamePublic = "Public";
    @Getter
    private String tabNameChannel = "Channel";
    @Getter
    private String tabNameClan = "Clan";
    @Getter
    private String tabNameGIM = "GIM";
    JTabbedPane tabbedPane = new JTabbedPane();

    public ChatBoxSection(SidePanel sideP, LangCodeSelectableList langList, RuneLingualPlugin plugin) {
        this.sidePanel = sideP;

        translateTabNames(langList);

        addTab(tabbedPane, tabNameGame);
        addTab(tabbedPane, tabNamePublic);
        addTab(tabbedPane, tabNameChannel);
        addTab(tabbedPane, tabNameClan);
        addTab(tabbedPane, tabNameGIM);

        sidePanel.add(tabbedPane);
        sidePanel.setSize(400, 500);
        sidePanel.setVisible(true);
    }

private static void addTab(JTabbedPane tabbedPane, String title) {
    JTextArea textArea = new JTextArea();
    textArea.setEditable(false); // This line makes the text uneditable
    textArea.setCursor(new Cursor(Cursor.TEXT_CURSOR)); // This line changes the cursor to the I-beam shape
    textArea.setSelectionColor(new Color(50,50,200)); // This line sets the background color of the selected text to black
    textArea.setSelectedTextColor(Color.WHITE); // This line sets the color of the selected text to white
    textArea.setLineWrap(true); // This line enables line wrapping

    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // This line disables horizontal scrolling

    tabbedPane.addTab(title, scrollPane);
    textArea.setBackground(new Color(30,30,30));
}

public void addSentenceToTab(String tabTitle, String sentence) {
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
        if (tabbedPane.getTitleAt(i).equals(tabTitle)) {
            JScrollPane scrollPane = (JScrollPane) tabbedPane.getComponentAt(i);
            JViewport viewport = scrollPane.getViewport();
            JTextArea textArea = (JTextArea) viewport.getView();
            if (textArea.getDocument().getLength() != 0) { // Check if the text area is not empty
                textArea.append("--------------------\n"); // Append a separator line
            }
            textArea.append(sentence + "\n");
            return;
        }
    }
    log.info("No tab found with title: " + tabTitle);
}


    private void translateTabNames(LangCodeSelectableList targetLanguage) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            tabNameGame = "Game";
            tabNamePublic = "Public";
            tabNameChannel = "Channel";
            tabNameClan = "Clan";
            tabNameGIM = "GIM";
        } /*else if (targetLanguage == LangCodeSelectableList.PORTUGUÊS_BRASILEIRO) {
            tabNameGame = "Jogo";
            tabNamePublic = "Público";
            tabNameChannel = "Canal";
            tabNameClan = "Clã";
            tabNameGIM = "GIM";
        } else if (targetLanguage == LangCodeSelectableList.NORSK) {
            tabNameGame = "Spill";
            tabNamePublic = "Offentlig";
            tabNameChannel = "Kanal";
            tabNameClan = "Klan";
            tabNameGIM = "GIM";
        } */else if (targetLanguage == LangCodeSelectableList.日本語) {
            tabNameGame = "ゲーム";
            tabNamePublic = "公共";
            tabNameChannel = "チャンネル";
            tabNameClan = "クラン";
            tabNameGIM = "GIM";
        }// todo: add more here as languages are added
    }
}
