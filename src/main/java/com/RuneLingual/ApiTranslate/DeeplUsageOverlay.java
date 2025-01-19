package com.RuneLingual.ApiTranslate;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;


public class DeeplUsageOverlay  extends Overlay {
    private Client client;
    private RuneLingualConfig config;
    private RuneLingualPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public DeeplUsageOverlay(Client client, RuneLingualPlugin plugin, RuneLingualConfig config) {
        setPosition(OverlayPosition.BOTTOM_RIGHT);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }
    /**
    * overlay for the number count by the DeepL API.
     * will show the number of characters translated by the DeepL API and the limit of the API.
     * if the API key is invalid, it will show a warning message.
     */
    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showUsageOverlayConfig())
            return null;

        int enCharSize = LangCodeSelectableList.ENGLISH.getCharWidth();
        int foreignCharSize = config.getSelectedLanguage().getCharWidth();
        boolean deeplKeyValid = plugin.getDeepl().isKeyValid();
        String deeplCount = Long.toString(plugin.getDeepl().getDeeplCount());
        String deeplLimit = Long.toString(plugin.getDeepl().getDeeplLimit());

        Color bgColorCount = new Color(80, 148, 144);
        Color bgColorInvalid = new Color(194, 93, 93);
        panelComponent.getChildren().clear();
        int len;
        if (deeplKeyValid) {
            panelComponent.setBackgroundColor(bgColorCount);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("DeepL:")
                    .right(deeplCount + " / " + deeplLimit)
                    .build());
            len = (deeplLimit.length()*2+10)*enCharSize;
        } else {
            panelComponent.setBackgroundColor(bgColorInvalid);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("APIキーが無効です。APIキーが有効であることと\n上限に到達していないことを確認してください。")
                    .build());
            len = ("APIキーが無効です。APIキーが有効であることと".length()+2)*foreignCharSize;
        }//TODO: make it display in the configured language
        panelComponent.setPreferredSize(new Dimension(len,0));
        return panelComponent.render(graphics);
    }


}
