package com.RuneLingual;

import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.SqlActions;
import com.RuneLingual.nonLatinChar.GeneralFunctions;
import com.RuneLingual.commonFunctions.Transformer;
import com.RuneLingual.commonFunctions.Transformer.TransformOption;
import com.RuneLingual.commonFunctions.SqlVariables;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;

import javax.inject.Inject;

import lombok.Setter;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class MenuCapture
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualPlugin plugin;
	
	@Setter
	private TranscriptManager actionTranslator;
	@Setter
	private TranscriptManager npcTranslator;
	@Setter
	private TranscriptManager objectTranslator;
	@Setter
	private TranscriptManager itemTranslator;
	
	@Setter
	private LogHandler logger;
	private boolean debugMessages = true;
	private final Colors colorObj = Colors.black;
    @Inject
	private Transformer transformer;
	//private SqlVariables sqlVariables;

	
	// TODO: right click menu title 'Chose Options' - seems to not be directly editable

	
	public void handleMenuEvent(MenuEntryAdded event) {
        //GeneralFunctions generalFunctions = plugin.getGeneralFunctions();

		boolean needCharImage = plugin.getConfig().getSelectedLanguage().needCharImages();

		// called whenever a right click menu is opened
		MenuEntry currentMenu = event.getMenuEntry();
		String menuOption = currentMenu.getOption(); // doesnt seem to have color tags, always white? eg. Attack
		String[] actionWordArray = colorObj.getWordArray(menuOption); // eg. ["Attack"]
		Colors[] actionColorArray = new Colors[]{Colors.white}; // [Colors.white]

		String menuTarget = currentMenu.getTarget(); // eg. <col=ffff00>Sand Crab<col=ff00>  (level-15)
													 //eg2. <col=ff9040>Dramen staff</col>
		String[] targetWordArray = colorObj.getWordArray(menuTarget); // eg. ["Sand Crab", " (level-15)"]
		Colors[] targetColorArray = colorObj.getColorArray(menuTarget); // eg. [Colors.yellow, Colors.green]

		// some possible targets
//		NPC targetNpc = currentMenu.getNpc();
//		Player targetPlayer = currentMenu.getPlayer();
//		int targetItem = currentMenu.getItemId();

		MenuAction menuType = currentMenu.getType();

		// used to define what column should be what value when searching for translation of each wordArray
		List<SqlVariables> targetSqlVar = null;
		List<SqlVariables> optionSqlVar = null;
		/*
		eg. if targetWordArray = ["Sand Crab", " (level-15)"]
		then sqlVariables = [SqlVariables.nameInCategory, SqlVariables.manualInCategory]
		 */


		// translate the target
		String newTarget = "";
		String newOption = "";
		if (isPlayerMenu(menuType)){
			//leave name as is (but replace to char image if needed), translate the level part
			targetSqlVar = List.of(SqlVariables.nameInCategory, SqlVariables.manualInCategory); // manualInCategory = the level part should be added manually
			newTarget = transformer.transform(targetWordArray, targetColorArray, new TransformOption[] {TransformOption.AS_IS, TransformOption.AS_IS}, targetSqlVar);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.AS_IS, SqlVariables.inventActionsInCategory);
		} else if(isNpcMenu(menuType)) {
			targetSqlVar = List.of(SqlVariables.nameInCategory, SqlVariables.npcInSubCategory);
			optionSqlVar = List.of(SqlVariables.actionsInCategory, SqlVariables.npcInSubCategory);
			newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.AS_IS, targetSqlVar);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.AS_IS, optionSqlVar);
		} else if(isObjectMenu(menuType)){
			targetSqlVar = List.of(SqlVariables.nameInCategory, SqlVariables.objInSubCategory);
			optionSqlVar = List.of(SqlVariables.actionsInCategory, SqlVariables.objInSubCategory);
			newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.AS_IS, targetSqlVar);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.AS_IS, optionSqlVar);
		} else if(isItemMenuOnGround(menuType)){ // needs checking
			targetSqlVar = List.of(SqlVariables.nameInCategory, SqlVariables.itemInSubCategory);
			optionSqlVar = List.of(SqlVariables.actionsInCategory);
			newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.AS_IS, targetSqlVar);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.AS_IS, optionSqlVar);
		} else if(isItemMenuInInvent(menuType)){ // needs checking
			targetSqlVar = List.of(SqlVariables.nameInCategory, SqlVariables.itemInSubCategory);
			optionSqlVar = List.of(SqlVariables.actionsInCategory);
			newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.AS_IS, targetSqlVar);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.AS_IS, optionSqlVar);
		} else if(isGeneralMenu(menuType)){ // needs checking
			optionSqlVar = List.of(SqlVariables.actionsInCategory);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.AS_IS, optionSqlVar);
			newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.AS_IS, targetSqlVar);
		} else if(isWidgetOnSomething(menuType)){ // needs checking
			// eg. "Use" -> "Brug"
			optionSqlVar = List.of(SqlVariables.actionsInCategory);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.AS_IS, optionSqlVar);
			// eg. "Dramen staff -> Sand Crab"
			Pair<String, String> result = convertWidgetOnSomething(currentMenu);
			String itemName = result.getLeft();
			String useOnX = result.getRight();
			targetSqlVar = List.of(SqlVariables.nameInCategory, SqlVariables.itemInSubCategory);
			newTarget = transformer.transform(new String[]{itemName, useOnX}, new Colors[]{Colors.white, Colors.white}, TransformOption.AS_IS, targetSqlVar);

		} else {
			// report to discord via webhook?
		}

		// translate the menu action



		// swap out the translated menu action and target.
		// reorder them if it is grammatically correct to do so in that language
		if(newOption != null) {
			if (newTarget != null && !newTarget.isEmpty()) {
				currentMenu.setTarget(currentMenu.getTarget().replace(currentMenu.getTarget(), newTarget));
			} else {
				// if target is empty, remove the target part of the menu entry
				currentMenu.setTarget(currentMenu.getTarget().replace(currentMenu.getTarget(),""));
			}
			currentMenu.setOption(currentMenu.getOption().replace(currentMenu.getOption(), newOption));
			//event.getMenuEntry().setOption(newOption);
		}

		//old codes
		/*
		try
		{
			if(isPlayerMenu(menuType))
			{
				translateMenuAction("playeractions", event, menuAction);
			}
			else if(isNpcMenu(menuType))
			{
				translateMenuAction("npcactions", event, menuAction);

				// translates npc name
				try
				{
					int combatLevel = targetNpc.getCombatLevel();
					if(combatLevel > 0)
					{
						// attackable npcs
						int levelIndicatorIndex = menuTarget.indexOf('(');
						if(levelIndicatorIndex != -1)
						{  // npc has a combat level
							String actualName = menuTarget.substring(0, levelIndicatorIndex);
							String newName = npcTranslator.getName(actualName, true);

							String levelIndicator = actionTranslator.getText("npcactions", "level", true);
							newName += " (" + levelIndicator + "-" + combatLevel + ")";
							event.getMenuEntry().setTarget(newName);
						}
						else
						{  // npc does not have a combat level
							String newName = npcTranslator.getName(menuTarget, true);
							event.getMenuEntry().setTarget(newName);
						}
					}
					else
					{  // non attackable npcs
						String newName = npcTranslator.getName(menuTarget, true);
						event.getMenuEntry().setTarget(newName);
					}

				}
				catch(Exception f)
				{
					if(debugMessages)
					{
						logger.log("Could not translate npc name: "
			                + menuTarget
			                + " - "
				            + f.getMessage());
					}
				}

			}
			else if(isWidgetOnSomething(menuType))
			{
				Pair<String, String> result = convertWidgetOnSomething(currentMenu);
				String itemName = result.getLeft();
				String useOnX = result.getRight();
				String newName = itemTranslator.getText("items", itemName, true);
				if (menuType.equals(MenuAction.WIDGET_TARGET_ON_NPC))
				{
					try
					{
						int combatLevel = targetNpc.getCombatLevel();
						if(combatLevel > 0)
						{
							// attackable npcs
							int levelIndicatorIndex = useOnX.indexOf('(');
							if(levelIndicatorIndex != -1)
							{  // npc has a combat level
								String actualName = useOnX.substring(0, levelIndicatorIndex);
								String NPCname = npcTranslator.getName(actualName, true);

								String levelIndicator = actionTranslator.getText("npcactions", "level", true);
								useOnX = NPCname + " (" + levelIndicator + "-" + combatLevel + ")";
								//event.getMenuEntry().setTarget(newName);
							}
							else
							{  // npc does not have a combat level
								useOnX = npcTranslator.getName(useOnX, true);
							}
						}
						else
						{  // non attackable npcs
							useOnX = npcTranslator.getName(useOnX, true);
						}
					}
					catch(Exception f)
					{
						if(debugMessages)
						{
							logger.log("Could not translate npc name: "
									+ menuTarget
									+ " - "
									+ f.getMessage());
						}
					}
				}
				else if (menuType.equals(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT))
				{
					useOnX = objectTranslator.getText("objects", useOnX, true);
				}
				else if (menuType.equals(MenuAction.WIDGET_TARGET_ON_WIDGET) || menuType.equals(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM))
				{
					useOnX = itemTranslator.getText("items", useOnX, true);
				}
				translateMenuAction("iteminterfaceactions", event, menuAction);
				event.getMenuEntry().setTarget(newName + " -> " + useOnX);
			}
			else if(isObjectMenu(menuType))
			{
				translateItemName("objects", event, menuTarget);
				translateMenuAction("objectactions", event, menuAction);
			}
			else if(isItemMenu(menuType))
			{  // ground item
				translateItemName("items", event, menuTarget);
				translateMenuAction("itemactions", event, menuAction);
			}
			else if(targetItem != -1)
			{  // inventory item
				translateItemName("items", event, menuTarget);
				translateMenuAction("iteminterfaceactions", event, menuAction);
			}
			else if(isGeneralMenu(menuType))
			{
				try
				{
					String newAction = actionTranslator.getText("generalactions", menuAction, true);
					event.getMenuEntry().setOption(newAction);
				}
				catch(Exception f)
				{
					if(debugMessages)
					{
						logger.log("Could not translate action: " + f.getMessage());
					}
				}
				try
				{
					translateItemName("items", event, menuTarget);
					translateMenuAction("iteminterfaceaction", event, menuAction);
				}
				catch(Exception f)
				{
					if(debugMessages)
					{
						logger.log("Could not translate action: " + f.getMessage());
					}
				}
			}
			else
			{
				// TODO: this
				// nor a player or npc
				logger.log("Menu action:"
			           + menuAction
			           + " - Menu target:"
			           + menuTarget
			           + "type:"
			           + event.getMenuEntry().getType());

				/*
				// tries to translate general actions
				try
				{
					String newAction = actionTranslator.getTranslatedText("generalactions", menuAction, true);
					event.getMenuEntry().setOption(newAction);
				}
				catch(Exception f)
				{

					logger.logger("Could not translate action: " + f.getMessage());

				} end comment here with * and /

			}

		}
		catch (Exception e)
		{
			if(debugMessages)
			{
				logger.log("Critical error happened while processing right click menus: " + e.getMessage());
			}
		}
		*/
	}

	static void mapWidgetText(Widget[] childComponents) {
		for (Widget component : childComponents) {
			remapWidget(component);
			String text = component.getText();
			if (text.isEmpty())
				continue;
			RemapWidgetText(component, text);
		}
	}
	static void remapWidget(Widget widget) {
		final int groupId = WidgetInfo.TO_GROUP(widget.getId());
		final int CHAT_MESSAGE = 162, PRIVATE_MESSAGE = 163, FRIENDS_LIST = 429;

		if (groupId == CHAT_MESSAGE || groupId == PRIVATE_MESSAGE || groupId == FRIENDS_LIST)
			return;

		Widget[] children = widget.getDynamicChildren();
		if (children == null)
			return;

		Widget[] childComponents = widget.getDynamicChildren();
		if (childComponents != null)
			mapWidgetText(childComponents);

		childComponents = widget.getStaticChildren();
		if (childComponents != null)
			mapWidgetText(childComponents);

		childComponents = widget.getNestedChildren();
		if (childComponents != null)
			mapWidgetText(childComponents);
	}
	static void RemapWidgetText(Widget component, String text)
	{
		if (component.getText().contains("Rapid"))
		{
			component.setText(text.replace("Rapid", "Hurtig"));
		}
	}

	private void translateItemName(String source, MenuEntryAdded entryAdded, String target)
	{
		if(target.length() == 0)
		{
			return;
		}
		
		// translates item name
		try
		{
			String newName = target;
			if(source.equals("items"))
			{
				newName = itemTranslator.getText(source, target, true);
				entryAdded.getMenuEntry().setTarget(newName);
			}
			else if(source.equals("objects"))
			{
				newName = objectTranslator.getText(source, target, true);
				entryAdded.getMenuEntry().setTarget(newName);
			}
		}
		catch(Exception f)
		{
			if(debugMessages)
			{
				logger.log("Could not translate '"
		            + source
			        + "' name: "
		            + target
					+ " - "
					+ f.getMessage());
			}
		}
	}
	private void translateMenuAction(String actionSource, MenuEntryAdded entryAdded, String target)
	{
		// translates menu action
		try
		{
			String newAction = actionTranslator.getText(actionSource, target, true);
			entryAdded.getMenuEntry().setOption(newAction);
		}
		catch(Exception f)
		{
			// if current action is not from the informed category
			// checks if it is a generic action
			if(!actionSource.equals("generalactions"))
			{
				try
				{
					translateMenuAction("generalactions", entryAdded, target);
				}
				catch(Exception g)
				{
					if(debugMessages)
					{
						logger.log("Could not translate menu '"
					        + actionSource
					        + "' action: "
					        + target
					        + " - "
					        + f.getMessage()
							+ " - "
							+ g.getMessage());
					}
				}
			}
			else if(debugMessages)
			{
				logger.log("Could not translate general action menu entry: "
		            + target
			        + " - "
			        + f.getMessage());
			}
		}
	}
	private Pair<String, String> convertWidgetOnSomething(MenuEntry entry)
	{
		String menuTarget = entry.getTarget();
		String[] parts = menuTarget.split(" -> ");
		String itemName = parts[0];
		String useOnName = parts[1];
		return Pair.of(itemName, useOnName);
	}

	private boolean hasLevel(String target)
	{
		// check if target contains <col=(numbers and alphabets)>(level-(*\d)). such as "<col=ffffff>Mama Layla<col=ffff00>(level-3000)"
		Pattern re = Pattern.compile(".+<col=[a-zA-Z0-9]+>\\s*\\(level-\\d+\\)");
		return re.matcher(target).find();
	}





	private boolean isGeneralMenu(MenuAction action)
	{
		// checks if current action target is a menu that introduces general actions
		return ((action.equals(MenuAction.CC_OP))
				|| (action.equals(MenuAction.CC_OP_LOW_PRIORITY))
				|| (action.equals(MenuAction.CANCEL))
				|| (action.equals(MenuAction.WALK)));
	}
	private boolean isObjectMenu(MenuAction action)
	{
		return ((action.equals(MenuAction.EXAMINE_OBJECT))
				|| (action.equals(MenuAction.GAME_OBJECT_FIRST_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_SECOND_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_THIRD_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_FOURTH_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_FIFTH_OPTION)));
	}
	private boolean isNpcMenu(MenuAction action)
	{
		return ((action.equals(MenuAction.EXAMINE_NPC))
				|| (action.equals(MenuAction.NPC_FIRST_OPTION))
				|| (action.equals(MenuAction.NPC_SECOND_OPTION))
				|| (action.equals(MenuAction.NPC_THIRD_OPTION))
				|| (action.equals(MenuAction.NPC_FOURTH_OPTION))
				|| (action.equals(MenuAction.NPC_FIFTH_OPTION)));
	}
	private boolean isItemMenu(MenuAction action)
	{
		return ((action.equals(MenuAction.EXAMINE_ITEM_GROUND))
				|| (action.equals(MenuAction.GROUND_ITEM_FIRST_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_SECOND_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_THIRD_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_FOURTH_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_FIFTH_OPTION)));
	}
	private boolean isPlayerMenu(MenuAction action)
	{
		return ((action.equals(MenuAction.PLAYER_FIRST_OPTION))
				|| (action.equals(MenuAction.PLAYER_SECOND_OPTION))
				|| (action.equals(MenuAction.PLAYER_THIRD_OPTION))
				|| (action.equals(MenuAction.PLAYER_FOURTH_OPTION))
				|| (action.equals(MenuAction.PLAYER_FIFTH_OPTION))
				|| (action.equals(MenuAction.PLAYER_SIXTH_OPTION))
				|| (action.equals(MenuAction.PLAYER_SEVENTH_OPTION))
				|| (action.equals(MenuAction.PLAYER_EIGHTH_OPTION))
				|| (action.equals(MenuAction.RUNELITE_PLAYER)));
	}
	private boolean isWidgetOnSomething(MenuAction action)
	{
		return ((action.equals(MenuAction.WIDGET_TARGET_ON_WIDGET))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_NPC))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_PLAYER)));
	}

	private boolean isItemMenuOnGround(MenuAction action) // needs checking
	{
		return ((action.equals(MenuAction.ITEM_FIRST_OPTION))
				|| (action.equals(MenuAction.ITEM_SECOND_OPTION))
				|| (action.equals(MenuAction.ITEM_THIRD_OPTION))
				|| (action.equals(MenuAction.ITEM_FOURTH_OPTION))
				|| (action.equals(MenuAction.ITEM_FIFTH_OPTION))
				|| (action.equals(MenuAction.ITEM_USE))
				|| (action.equals(MenuAction.ITEM_USE_ON_ITEM))
				|| (action.equals(MenuAction.WIDGET_USE_ON_ITEM))
				|| (action.equals(MenuAction.WIDGET_FIRST_OPTION))
				|| (action.equals(MenuAction.WIDGET_SECOND_OPTION))
				|| (action.equals(MenuAction.WIDGET_THIRD_OPTION))
				|| (action.equals(MenuAction.WIDGET_FOURTH_OPTION))
				|| (action.equals(MenuAction.WIDGET_FIFTH_OPTION)));
	}

	private boolean isItemMenuInInvent(MenuAction action) // needs checking
	{
		return ((action.equals(MenuAction.ITEM_FIRST_OPTION))
				|| (action.equals(MenuAction.ITEM_SECOND_OPTION))
				|| (action.equals(MenuAction.ITEM_THIRD_OPTION))
				|| (action.equals(MenuAction.ITEM_FOURTH_OPTION))
				|| (action.equals(MenuAction.ITEM_FIFTH_OPTION))
				|| (action.equals(MenuAction.ITEM_USE))
				|| (action.equals(MenuAction.ITEM_USE_ON_ITEM))
				|| (action.equals(MenuAction.WIDGET_USE_ON_ITEM))
				|| (action.equals(MenuAction.WIDGET_FIRST_OPTION))
				|| (action.equals(MenuAction.WIDGET_SECOND_OPTION))
				|| (action.equals(MenuAction.WIDGET_THIRD_OPTION))
				|| (action.equals(MenuAction.WIDGET_FOURTH_OPTION))
				|| (action.equals(MenuAction.WIDGET_FIFTH_OPTION)));
	}
}
