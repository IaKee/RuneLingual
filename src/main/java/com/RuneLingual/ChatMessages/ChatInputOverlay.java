package com.RuneLingual.ChatMessages;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Transformer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ParametersAreNonnullByDefault
public class ChatInputOverlay extends Overlay //remove abstract when actually making overlays with this
{
    @Inject
    private Client client;
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private PlayerMessage playerMessage;
    private final PanelComponent panelComponent = new PanelComponent();
    private int[] ovlPos;
    private int paddingWidth = 55;
    private int inputWidth = 400 - paddingWidth; // 400 for the whole width, 52 for the padding and the char count
    private int foreignCharSize; // px width of each japanese characters
    private int enCharSize = 8;

    @Inject
    public ChatInputOverlay(Client client, RuneLingualPlugin plugin) {
        setPosition(OverlayPosition.BOTTOM_LEFT);
        this.client = client;
        this.plugin = plugin;
    }
    @Override
    public Dimension render(Graphics2D graphics) {
        if(!plugin.getConfig().getSelectedLanguage().needsInputOverlay()){
            return null;
        }

        foreignCharSize = plugin.getConfig().getSelectedLanguage().getCharWidth() + 1;


        int msgLength = playerMessage.getChatInputString().length();
        String nonLatinMsg = "";

        if(plugin.getConfig().getSelectedLanguage().equals(LangCodeSelectableList.日本語)){
            nonLatinMsg = plugin.getChatInputRLingual().getUpdateChatInputJa().getChatJpMsg();
        } // TODO: add more languages that need this overlay


        if (msgLength == 0
                || playerMessage.getTranslationOption().equals(Transformer.TransformOption.AS_IS)
                || nonLatinMsg.trim().isEmpty()) return null; // todo:also this if in npc dialogue

        int latingCharCount = countLatinCharacters(nonLatinMsg);
        int foreignCharCount = nonLatinMsg.length() - latingCharCount;

        panelComponent.getChildren().clear();

        // Set the size of the overlay
        int currentInputSize = foreignCharSize*(foreignCharCount)+enCharSize*latingCharCount;
        panelComponent.setPreferredSize(new Dimension(Math.min( currentInputSize + paddingWidth, inputWidth + paddingWidth),0));

        Color bgColor = new Color(127, 82, 33);
        panelComponent.setBackgroundColor(bgColor);

        if(foreignCharSize*foreignCharCount + enCharSize*latingCharCount > inputWidth) {
            String[] newMsgs = splitMsg(nonLatinMsg);
            for (int i = 0;i < newMsgs.length; i++) {
                if (i == newMsgs.length - 1) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left(newMsgs[i])
                            .right("(" + msgLength + "/80)")
                            .build());
                } else {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left(newMsgs[i])
                            .build());
                }
            }
        } else {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(nonLatinMsg)
                    .right(" (" + msgLength + "/80)")
                    .build());
        }
        return panelComponent.render(graphics);
    }

public int countLatinCharacters(String str) {
    int count = 0;
    for (char c : str.toCharArray()) {
        if ((Character.isLetter(c) && Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN)
                || c == ',' || c == '.' || c == '!' || c == '?' || c == ' ' || c == '\'' || c == '\"'
                || c == '(' || c == ')' || c == ':' || c == ';' || c == '-') {
            count++;
        }
    }
    return count;
}

    private int getLen(String str) {
        return str.length()*foreignCharSize;
    }

    private String[] splitMsg(String str) {//splits message
        List<String> lines = new ArrayList<>();
        int lineLength = 0;
        StringBuilder line = new StringBuilder();
        StringBuilder enWord = new StringBuilder();

        for(int i = 0; i < str.length(); i++){
            if(!isJapaneseChar(str.charAt(i))) { // make sure it doesnt go to new line in the middle of an English word
                if (str.charAt(i) != ' ') {
                    enWord.append(str.charAt(i));
                } else { // if it's a space
                    if (lineLength + enWord.length()*enCharSize  >= inputWidth) { // if adding the word would go over the width
                        lines.add(line.toString()); // add the current line to the list
                        line = enWord.append(" "); // start a new line with the word
                        lineLength = enWord.length()*(enCharSize); // set the length to the length of the word
                        enWord = new StringBuilder(); // reset the word
                    } else {
                        line.append(enWord).append(" ");
                        lineLength += enCharSize*enWord.length();
                        enWord = new StringBuilder();

                    }
                }
            } else {
                enWord = new StringBuilder();
                if (lineLength + foreignCharSize>= inputWidth) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                    line.append(str.charAt(i));
                    lineLength = foreignCharSize;
                } else {
                    line.append(str.charAt(i));
                    lineLength += foreignCharSize;
                }
            }
        }
        if(lineLength > inputWidth){
            lines.add(line.toString());
            line = new StringBuilder();
        } else if(lineLength + enWord.length()*enCharSize > inputWidth) {
            lines.add(line.toString());
            line = new StringBuilder();
        }
        line.append(enWord);
        lines.add(line.toString());
        return lines.toArray(new String[0]);
    }

    public boolean isLatinChar(char c) {
        if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN
                || c == ',' || c == '.' || c == '!' || c == '?' || c == ' ' || c == '\'' || c == '\"'
                || c == '(' || c == ')' || c == ':' || c == ';' || c == '-'
                || c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6'
                || c == '7' || c == '8' || c == '9') {
            return true;
        } else
            return false;
    }

    public boolean isJapaneseChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);

        return block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS;
    }
}
