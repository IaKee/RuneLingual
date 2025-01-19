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

@Slf4j
@ParametersAreNonnullByDefault
public class ChatInputCandidateOverlay extends Overlay
{
    @Inject
    private PlayerMessage playerMessage;
    private Client client;
    private RuneLingualPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();
    private int[] ovlPos;
    private int inputWidth = 400;
    private int foreignCharSize; // px width of each japanese characters
    private int enCharSize = 8;
    private final int candListMax = 7;//max vert number of words


    @Inject
    public ChatInputCandidateOverlay(Client client, RuneLingualPlugin plugin) {
        setPosition(OverlayPosition.BOTTOM_LEFT);
        this.client = client;
        this.plugin = plugin;
    }
    @Override
    public Dimension render(Graphics2D graphics) {
        if(!plugin.getConfig().getSelectedLanguage().needsInputCandidateOverlay()){
            return null;
        }

        foreignCharSize = plugin.getConfig().getSelectedLanguage().getCharWidth() + 1;

        String[] nonLatinMsg = {};
        
        if(plugin.getConfig().getSelectedLanguage().equals(LangCodeSelectableList.日本語)){
            nonLatinMsg = plugin.getChatInputRLingual().getUpdateChatInputJa().getKanjKatCandidates().toArray(new String[0]);
        }

        int candSelectN = plugin.getChatInputRLingual().getUpdateChatInputJa().getInstCandidateSelection();
        int msgCount = plugin.getChatInputRLingual().getUpdateChatInputJa().getInputCount();

        if (msgCount == 0
                || playerMessage.getTranslationOption().equals(Transformer.TransformOption.AS_IS)
                || nonLatinMsg.length == 0) return null;// todo:also this if in npc dialogue
        if (nonLatinMsg.length == 1 && nonLatinMsg[0].matches("[^\\p{IsAlphabetic}\\p{IsHiragana}\\p{IsKatakana}]+")) return  null;

        
        
        panelComponent.getChildren().clear();
        int panelN = nonLatinMsg.length/candListMax + 1;

        Color bgColor = new Color(127, 82, 33);
        panelComponent.setBackgroundColor(bgColor);

        int[] panelWordLen = new int[panelN];
        int panelWidth = 0;
        for (int j = 0; j < panelN; j++) {
            for(int i = 0; i < candListMax && i + j * candListMax < nonLatinMsg.length; i++) {
                if (nonLatinMsg[i + j * candListMax].length() > panelWordLen[j]) {
                    String word = nonLatinMsg[i + j * candListMax].split("\\d")[0];
                    panelWordLen[j] = word.length();
                }
            }
            panelWidth += panelWordLen[j] * foreignCharSize;
        }
        panelWidth += foreignCharSize *panelN + enCharSize*3*panelN + enCharSize*2*(panelN-1);
        //if (panelN > 1)
        //   panelWidth += japCharSize*(panelN-1);
        for(int i = 0; i < candListMax; i++) {
            StringBuilder jp = new StringBuilder();
            String numbering;

            for (int j = 0; j < panelN; j++) {
                if (i + j * candListMax == candSelectN)
                    jp.append("<col=00ffff>");
                else
                    jp.append("<col=ffffff>");

                if (i + j * candListMax < nonLatinMsg.length) {
                    numbering = Integer.toString(i + j * candListMax) + "  ";
                    if (i+j*candListMax < 10)
                        numbering = " " + numbering;
//                    if (i + j * candListMax == candSelectN)
//                        numbering = "＞" + numbering;
//                    else
//                        numbering = "＿" + numbering;
                    if (j > 0) {
                        jp.append("　");

                    }
                    jp.append(numbering);

                    String word = nonLatinMsg[i + j * candListMax].split("\\d")[0];
                    jp.append(word);

                    int w = panelWordLen[j] - nonLatinMsg[i + j * candListMax].length();
                    jp.append("　".repeat(Math.max(0, w)));
                }
            }
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(jp.toString().trim())
                    .build());
        }
        // Set the size of the overlay
        panelComponent.setPreferredSize(new Dimension(panelWidth,0));
        return panelComponent.render(graphics);
    }

    private int getLen(String str) {
        return str.length()*14;
    }

    private String[] splitMsg(String string) {//splits message into 2, first with length of chat input width, second (and third if needed) with remaining
        String[] ret = {
                string.substring(0, string.length() / 2),  // First half
                string.substring(string.length() / 2)       // Second half
        };
        return ret;
    }

    public static String toFullWidth(String input) {
        StringBuilder fullWidthForm = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '0' && c <= '9') { // 半角数字の範囲をチェック
                fullWidthForm.append((char) (c - '0' + '０')); // '０' は全角の '0'
            } else {
                fullWidthForm.append(c); // 数字以外はそのまま追加
            }
        }
        return fullWidthForm.toString();
    }
}

