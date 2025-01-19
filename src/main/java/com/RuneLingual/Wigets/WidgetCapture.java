package com.RuneLingual.Wigets;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Transformer;
import lombok.Getter;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class WidgetCapture {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private WidgetsUtilRLingual widgetsUtilRLingual;
    @Inject
    private DialogTranslator dialogTranslator;

    @Inject
    public WidgetCapture(RuneLingualPlugin plugin){
        this.plugin = plugin;
    }

    public void translateWidget(){
        Widget[] validWidgets = widgetsUtilRLingual.getAllChildren();
        for(Widget validWidget : validWidgets){
            int widgetGroup = WidgetUtil.componentToInterface(validWidget.getId());
            if(widgetGroup == InterfaceID.CHATBOX){// skip all chatbox widgets for now TODO: chatbox buttons should be translated
                continue;
            }
            if(widgetGroup == InterfaceID.DIALOG_NPC
                || widgetGroup == InterfaceID.DIALOG_PLAYER
                || widgetGroup == InterfaceID.DIALOG_OPTION){
                dialogTranslator.handleDialogs(validWidget);
            } else if(!validWidget.getText().isEmpty()
                    && !validWidget.getText().contains("<img=") // check if already translated to japanese. TODO: need something else after adding other languages
                    && plugin.getConfig().getInterfaceTextConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)) {
                // if its only numbers and symbols dont do anything
                String re = "^[^\\p{Alpha}]+$";
                if (validWidget.getText().matches(re))
                    continue;
                Colors[] textColor = Colors.getColorArray(validWidget.getText(),Colors.black);
                // for now only translate interfaces and buttons with API
                widgetsUtilRLingual.setWidgetText_ApiTranslation(validWidget, validWidget.getText(), textColor[0]);

            }
        }
    }
}
