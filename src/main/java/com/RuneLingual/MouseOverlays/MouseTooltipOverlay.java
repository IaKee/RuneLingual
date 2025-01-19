package com.RuneLingual.MouseOverlays;

import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.MenuCapture;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.*;
import javax.inject.Inject;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import static java.lang.Thread.sleep;

/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
@Slf4j
public class MouseTooltipOverlay extends Overlay
{

    /**
     * Menu types which are on widgets.
     */
    private static final Set<MenuAction> WIDGET_MENU_ACTIONS = ImmutableSet.of(
            MenuAction.WIDGET_TYPE_1,
            MenuAction.WIDGET_TARGET,
            MenuAction.WIDGET_CLOSE,
            MenuAction.WIDGET_TYPE_4,
            MenuAction.WIDGET_TYPE_5,
            MenuAction.WIDGET_CONTINUE,
            MenuAction.ITEM_USE_ON_ITEM,
            MenuAction.WIDGET_USE_ON_ITEM,
            MenuAction.ITEM_FIRST_OPTION,
            MenuAction.ITEM_SECOND_OPTION,
            MenuAction.ITEM_THIRD_OPTION,
            MenuAction.ITEM_FOURTH_OPTION,
            MenuAction.ITEM_FIFTH_OPTION,
            MenuAction.ITEM_USE,
            MenuAction.WIDGET_FIRST_OPTION,
            MenuAction.WIDGET_SECOND_OPTION,
            MenuAction.WIDGET_THIRD_OPTION,
            MenuAction.WIDGET_FOURTH_OPTION,
            MenuAction.WIDGET_FIFTH_OPTION,
            MenuAction.EXAMINE_ITEM,
            MenuAction.WIDGET_TARGET_ON_WIDGET,
            MenuAction.CC_OP_LOW_PRIORITY,
            MenuAction.CC_OP
    );

    private final TooltipManager tooltipManager;
    private final Client client;
    private final RuneLingualConfig config;
    @Inject
    private RuneLingualPlugin plugin;
    @Setter
    private static List<String> attemptedTranslation = Collections.synchronizedList(new ArrayList<>());

    @Inject
    MouseTooltipOverlay(Client client, TooltipManager tooltipManager, RuneLingualConfig config, RuneLingualPlugin plugin)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        // additionally allow tooltips above the full screen world map and welcome screen
        drawAfterInterface(InterfaceID.FULLSCREEN_CONTAINER_TLI);
        this.client = client;
        this.tooltipManager = tooltipManager;
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.isMenuOpen())
        {
            return null;
        }

        MenuEntry[] menuEntries = client.getMenuEntries();
        int last = menuEntries.length - 1;

        if (last < 0)
        {
            return null;
        }

        MenuEntry menuEntry = menuEntries[last];
        String target = menuEntry.getTarget();
        String option = menuEntry.getOption();
        MenuAction type = menuEntry.getType();

        if (!isNecessaryMenu(type, option, target))
        {
            return null;
        }

        // if is set to be translated with API,
        if(this.plugin.getConfig().ApiConfig()){
            // only translate if it has been translated before
            if(plugin.getDeepl().getDeeplPastTranslationManager().haveTranslatedMenuBefore(option, target, menuEntry)) {
                setMouseHover(menuEntry, true);
            } else { // dont translate if it hasnt been translated before
                setMouseHover(menuEntry, false);
                return null;
            }
        } else {
            // translate with local data if not set to use API
            setMouseHover(menuEntry, true);
            return null;
        }
        return null;
    }





    private void setMouseHover(MenuEntry menuEntry, boolean transform){
        String newTarget = "";
        String newOption = "";

        MenuCapture menuCapture = this.plugin.getMenuCapture();
        // if set not to transform, add the current target and option
        if (!transform){
            tooltipManager.addFront(new Tooltip(menuEntry.getOption() + (Strings.isNullOrEmpty(menuEntry.getTarget()) ? "" : " " + menuEntry.getTarget())));
            return;
        }

        //otherwise translate the target and option
        String[] newMenus = menuCapture.translateMenuAction(menuEntry);
        if (newMenus != null)
        {
            newTarget = newMenus[0];
            newOption = newMenus[1];
        }
        if (this.plugin.getTargetLanguage().needsSwapMenuOptionAndTarget())
        {
            tooltipManager.addFront(new Tooltip((Strings.isNullOrEmpty(newTarget) ? newOption : newTarget + " " + newOption)));
            return;
        }
        tooltipManager.addFront(new Tooltip(newOption + (Strings.isNullOrEmpty(newTarget) ? "" : " " + newTarget)));
    }


    private boolean isNecessaryMenu(MenuAction type, String option, String target)
    {
        if (type == MenuAction.RUNELITE_OVERLAY || type == MenuAction.CC_OP_LOW_PRIORITY)
        {
            // These are always right click only
            return false;
        }

        if (Strings.isNullOrEmpty(option))
        {
            return false;
        }

//        // Trivial options that don't need to be highlighted, add more as they appear.
        if (option.equals("Walk here") || option.equals("Cancel") || option.equals("Continue") || target.contains("Slide"))
            return false;
        if (!config.getMouseHoverConfig())
        {
            return false;
        }


        // If this varc is set, a tooltip will be displayed soon
        int tooltipTimeout = client.getVarcIntValue(VarClientInt.TOOLTIP_TIMEOUT);
        if (tooltipTimeout > client.getGameCycle())
        {
            return false;
        }

        // If this varc is set, a tooltip is already being displayed
        int tooltipDisplayed = client.getVarcIntValue(VarClientInt.TOOLTIP_VISIBLE);
        if (tooltipDisplayed == 1)
        {
            return false;
        }
        return true;
    }
}
