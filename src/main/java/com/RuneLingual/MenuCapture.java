package com.RuneLingual;

import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Transformer;
import com.RuneLingual.commonFunctions.Transformer.TransformOption;
import com.RuneLingual.SQL.SqlVariables;
import com.RuneLingual.SQL.SqlQuery;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;

import javax.inject.Inject;

import lombok.Setter;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.tuple.Pair;

import java.util.regex.Pattern;
import com.RuneLingual.debug.OutputToFile;
import com.RuneLingual.commonFunctions.Ids;
import com.RuneLingual.RuneLingualConfig.*;

@Slf4j
public class MenuCapture
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualPlugin plugin;

	@Setter
	private boolean debugMessages = true;
	private final Colors colorObj = Colors.black;
	@Inject
	private Transformer transformer;
	@Inject
	private OutputToFile outputToFile;

	private TransformOption menuOptionTransformOption = TransformOption.TRANSLATE_LOCAL;

	@Inject
	public MenuCapture(RuneLingualPlugin plugin) {
		this.plugin = plugin;
	}

	private final Colors optionColor = Colors.white;

	public void handleOpenedMenu(MenuOpened event){
		MenuEntry[] menus = event.getMenuEntries();
		for (MenuEntry menu : menus) {
			if (isGeneralMenu(menu) && plugin.getConfig().getMenuOptionConfig().equals(ingameTranslationConfig.USE_LOCAL_DATA)){
				// if config is set to use local data, it has to be on main thread (for general menus specifically)
				handleMenuEvent(menu);
			} else {
				if (plugin.getConfig().ApiConfig() &&
						!plugin.getDeepl().getDeeplPastTranslationManager().haveTranslatedMenuBefore(menu.getOption(), menu.getTarget(), menu)){
					Thread thread = new Thread(() -> {
						handleMenuEvent(menu);
					});
					thread.setDaemon(false);
					thread.start();
				} else {
					handleMenuEvent(menu);
				}
			}
		}
	}

	public void handleMenuEvent(MenuEntry currentMenu) {
		// called whenever a right click menu is opened
		String[] newMenus = translateMenuAction(currentMenu);
		String newTarget = newMenus[0];
		String newOption = newMenus[1];

		// reorder them if it is grammatically correct to do so in that language
		if(plugin.getTargetLanguage().needsSwapMenuOptionAndTarget()) {
			String temp = newOption;
			newOption = newTarget;
			newTarget = temp;
		}

		// swap out the translated menu action and target.
		if(newOption != null) {
			if (newTarget != null && !newTarget.isEmpty()) {
				currentMenu.setTarget(newTarget);
			} else {
				// if target is empty, remove the target part of the menu entry
				currentMenu.setTarget("");
			}
			currentMenu.setOption(newOption);
		}
	}

	public String[] translateMenuAction(MenuEntry currentMenu) {
		/*
		returns: String[] {newTarget, newOption}

		target and option examples
		option: Enable prayer reordering	target: <col=ff9040></col>
		option: Add-10<col=ff9040>		target: <col=ff9040>Pollnivneach teleport</col>
		 */
		MenuAction menuType = currentMenu.getType();

		String menuTarget = currentMenu.getTarget();  // eg. <col=ffff00>Sand Crab<col=ff00>  (level-15)     eg2. <col=ff9040>Dramen staff</col>
		String[] targetWordArray = Colors.getWordArray(menuTarget); // eg. ["Sand Crab", " (level-15)"]
		Colors[] targetColorArray = Colors.getColorArray(menuTarget, Colors.white); // eg. [Colors.yellow, Colors.green]

		String menuOption = currentMenu.getOption(); // doesnt seem to have color tags, always white? eg. Attack
		String[] actionWordArray = Colors.getWordArray(menuOption); // eg. ["Attack"]
		Colors[] actionColorArray = Colors.getColorArray(menuOption, optionColor);



		menuOptionTransformOption = getTransformOption(this.plugin.getConfig().getMenuOptionConfig());
		// for debug purposes
		if(!isWalkOrCancel(menuType)){
			//printMenuEntry(currentMenu);
			if(!isNpcMenu(menuType) && !isObjectMenu(menuType)
					&& !isItemOnGround(menuType) && !isItemInWidget(currentMenu) && !isPlayerMenu(menuType)){
				//outputToFile.menuTarget(menuTarget,SqlVariables.menuInSubCategory.getValue(), "");
				//outputToFile.menuOption(menuOption,SqlVariables.menuInSubCategory.getValue(), "");
			}
		}


		// todo: get translation option from settings
		String[] result = new String[] {};
		// get translation for both target and option
		if(isWalkOrCancel(menuType))
		{
			result = translateWalkOrCancel(menuTarget, menuOption, actionWordArray, actionColorArray, targetWordArray, targetColorArray);
		}
		else if (isPlayerMenu(menuType)){
			result = translatePlayer(menuOption, actionWordArray, actionColorArray, targetWordArray, targetColorArray);
		}
		else if(isNpcMenu(menuType)) { // need to split into npcs with and without level
			result = translateNpc(menuTarget, menuOption, actionWordArray, actionColorArray, targetWordArray, targetColorArray);
		}
		else if(isObjectMenu(menuType)){
			result = translateObject(menuTarget, menuOption, actionWordArray, actionColorArray, targetWordArray);
		}
		else if(isItemOnGround(menuType)){ // needs checking
			result = translateGroundItem(menuTarget, menuOption, actionWordArray, actionColorArray, targetWordArray);
		}
		else if(isItemInWidget(currentMenu)){ // either in inventory or in equipment
			result = translateInventoryItem(menuTarget, menuOption, actionWordArray, actionColorArray, targetWordArray);
		}
		else if(isWidgetOnSomething(menuType)){ // needs checking
			//printMenuEntry(currentMenu);
			Pair<String, String> results = convertWidgetOnSomething(currentMenu);
			String itemName = results.getLeft();
			String useOnX = results.getRight();
			String itemTranslation = translateInventoryItem(itemName, menuOption, actionWordArray, actionColorArray, Colors.getWordArray(itemName))[0];
			String useOnXTranslation = "";
			if(menuType.equals(MenuAction.WIDGET_TARGET_ON_PLAYER)){
				useOnXTranslation = translatePlayerTargetPart(Colors.getWordArray(useOnX), Colors.getColorArray(useOnX, Colors.white));
			} else if(menuType.equals(MenuAction.WIDGET_TARGET_ON_NPC)){
				useOnXTranslation = translateNpc(useOnX, menuOption, actionWordArray, actionColorArray, Colors.getWordArray(useOnX), Colors.getColorArray(useOnX, Colors.white))[0];
			} else if(menuType.equals(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT)){
				useOnXTranslation = translateObject(useOnX, menuOption, actionWordArray, actionColorArray, Colors.getWordArray(useOnX))[0];
			} else if(menuType.equals(MenuAction.WIDGET_TARGET_ON_WIDGET) || menuType.equals(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM)){
				useOnXTranslation = translateGroundItem(useOnX, menuOption, actionWordArray, actionColorArray, Colors.getWordArray(useOnX))[0];
			}
			String newTarget = itemTranslation + " -> " + useOnXTranslation;
			String newOption = translateInventoryItem(itemName, menuOption, actionWordArray, actionColorArray, Colors.getWordArray(menuOption))[1];
			result = new String[]{newTarget, newOption};
		} else { // is a general menu
			//printMenuEntry(currentMenu);

			// for debug purposes
			//outputToFile.menuTarget(menuTarget,SqlVariables.menuInSubCategory.getValue(), source);
			//outputToFile.menuOption(menuOption,SqlVariables.menuInSubCategory.getValue(), source);
			result = translateGeneralMenu(menuTarget, menuOption, actionWordArray, actionColorArray, targetWordArray, currentMenu);
		}

		// if the translation failed, return as is
		if (result.length < 2){
			return new String[]{menuTarget, menuOption};
		}

		String newTarget = result[0];
		String newOption = result[1];

		return new String[]{newTarget, newOption};
	}

	private String[] translateWalkOrCancel(String menuTarget, String menuOption, String[] actionWordArray, Colors[] actionColorArray, String[] targetWordArray, Colors[] targetColorArray){
		//returns String[] {newTarget, newOption}
		SqlQuery actionSqlQuery = new SqlQuery(this.plugin);
		SqlQuery targetSqlQuery = new SqlQuery(this.plugin);
		String newOption, newTarget;
		actionSqlQuery.setEnglish(menuOption);
		actionSqlQuery.setCategory(SqlVariables.actionsInCategory.getValue());

		newOption = transformer.transform(actionWordArray, actionColorArray, menuOptionTransformOption, actionSqlQuery, false);

		if(Colors.removeColorTag(menuTarget).isEmpty()) {
			newTarget = "";
		} else {
			TransformOption menuTransformOption = getTransformOption(this.plugin.getConfig().getMenuOptionConfig());
			if(hasLevel(menuTarget)){
				// if walk has a target with level, its a player
				newTarget = translatePlayerTargetPart(targetWordArray, targetColorArray);
			} else {
				// this shouldnt happen but just in case
				targetSqlQuery.setEnglish(targetWordArray[0]);
				targetSqlQuery.setCategory(SqlVariables.nameInCategory.getValue());
				targetSqlQuery.setSubCategory(SqlVariables.menuInSubCategory.getValue());
				// need to split into name and level if it has level
				newTarget = transformer.transform(targetWordArray, targetColorArray, menuTransformOption, targetSqlQuery, false);
			}
		}
		return new String[]{newTarget, newOption};
	}

	private String[] translatePlayer(String menuOption, String[] actionWordArray, Colors[] actionColorArray, String[] targetWordArray, Colors[] targetColorArray){
		//returns String[] {newTarget, newOption}
		//leave name as is (but replace to char image if needed)
		String newTarget = translatePlayerTargetPart(targetWordArray, targetColorArray);
		// set action as usual
		SqlQuery actionSqlQuery = new SqlQuery(this.plugin);
		actionSqlQuery.setPlayerActions(menuOption, Colors.white);
		String newOption = transformer.transform(actionWordArray, actionColorArray, menuOptionTransformOption, actionSqlQuery, false);
		return new String[] {newTarget, newOption};
	}

	private String[] translateNpc(String menuTarget, String menuOption, String[] actionWordArray, Colors[] actionColorArray, String[] targetWordArray, Colors[] targetColorArray) {
		String newTarget, newOption;
		SqlQuery targetSqlQuery = new SqlQuery(this.plugin);
		SqlQuery actionSqlQuery = new SqlQuery(this.plugin);
		TransformOption npcTransformOption = getTransformOption(this.plugin.getConfig().getNPCNamesConfig());
		if(npcTransformOption.equals(TransformOption.AS_IS)){
			newTarget = menuTarget;
		} else if(hasLevel(menuTarget)){
			// if npc has a level, translate the name and level separately
			targetSqlQuery.setNpcName(targetWordArray[0], targetColorArray[0]);
			String targetName = transformer.transform(targetWordArray[0], targetColorArray[0], npcTransformOption, targetSqlQuery, false);
			String targetLevel = getLevelTranslation(targetWordArray[1], targetColorArray[1]);
			newTarget = targetName + targetLevel;
		} else {
			// if npc does not have a level, translate the name only
			targetSqlQuery.setNpcName(menuTarget, targetColorArray[0]);
			targetColorArray = Colors.getColorArray(menuTarget, targetColorArray[0]); //default color is not the same as initial definition
			newTarget = transformer.transform(targetWordArray, targetColorArray, npcTransformOption, targetSqlQuery, false);
		}

		actionSqlQuery.setNpcActions(menuOption, optionColor);
		newOption = transformer.transform(actionWordArray, actionColorArray, menuOptionTransformOption, actionSqlQuery, false);

		return new String[] {newTarget, newOption};
	}

	private String[] translateObject(String menuTarget, String menuOption, String[] actionWordArray, Colors[] actionColorArray, String[] targetWordArray) {
		SqlQuery targetSqlQuery = new SqlQuery(this.plugin);
		SqlQuery actionSqlQuery = new SqlQuery(this.plugin);
		targetSqlQuery.setObjectName(menuTarget, Colors.lightblue);
		actionSqlQuery.setObjectActions(menuOption, optionColor);

		Colors[] targetColorArray = Colors.getColorArray(menuTarget, Colors.lightblue); //default color is not the same as initial definition

		TransformOption	objectTransformOption = getTransformOption(this.plugin.getConfig().getObjectNamesConfig());

		String newTarget;
		if(objectTransformOption.equals(TransformOption.AS_IS)){
			newTarget = menuTarget;
		} else {
			newTarget = transformer.transform(targetWordArray, targetColorArray, objectTransformOption, targetSqlQuery, false);
		}
		String newOption = transformer.transform(actionWordArray, actionColorArray, menuOptionTransformOption, actionSqlQuery, false);

		return new String[] {newTarget, newOption};
	}

	private String[] translateGroundItem(String menuTarget, String menuOption, String[] actionWordArray, Colors[] actionColorArray, String[] targetWordArray){
		SqlQuery targetSqlQuery = new SqlQuery(this.plugin);
		SqlQuery actionSqlQuery = new SqlQuery(this.plugin);
		targetSqlQuery.setItemName(menuTarget, Colors.orange);
		actionSqlQuery.setGroundItemActions(menuOption, optionColor);

		Colors[] targetColorArray = Colors.getColorArray(menuTarget, Colors.orange); //default color is not the same as initial definition

		TransformOption itemTransformOption = getTransformOption(this.plugin.getConfig().getItemNamesConfig());
		String newTarget;
		if (itemTransformOption.equals(TransformOption.AS_IS)){
			newTarget = menuTarget;
		} else {
			newTarget = transformer.transform(targetWordArray, targetColorArray, itemTransformOption, targetSqlQuery, false);
		}
		String newOption = transformer.transform(actionWordArray, actionColorArray, menuOptionTransformOption, actionSqlQuery, false);
		return new String[] {newTarget, newOption};
	}

	private String[] translateInventoryItem(String menuTarget, String menuOption, String[] actionWordArray, Colors[] actionColorArray, String[] targetWordArray){
		SqlQuery targetSqlQuery = new SqlQuery(this.plugin);
		SqlQuery actionSqlQuery = new SqlQuery(this.plugin);

		targetSqlQuery.setItemName(menuTarget, Colors.orange);
		actionSqlQuery.setInventoryItemActions(menuOption, optionColor);

		Colors[] targetColorArray = Colors.getColorArray(menuTarget, Colors.orange); //default color is not the same as initial definition

		TransformOption itemTransformOption = getTransformOption(this.plugin.getConfig().getItemNamesConfig());
		String newTarget = transformer.transform(targetWordArray, targetColorArray, itemTransformOption, targetSqlQuery, false);
		String newOption = transformer.transform(actionWordArray, actionColorArray, menuOptionTransformOption, actionSqlQuery, false);
		return new String[] {newTarget, newOption};
	}

	private String[] translateGeneralMenu(String menuTarget, String menuOption, String[] actionWordArray, Colors[] actionColorArray, String[] targetWordArray, MenuEntry currentMenu){
		String newTarget, newOption;
		// check what widget it is in, then set the source column value accordingly
		String source;
		if(plugin.getConfig().getMenuOptionConfig().equals(ingameTranslationConfig.USE_API) && plugin.getConfig().ApiConfig()){
			source = "";
		} else {
			source = getSourceNameFromMenu(currentMenu);
		}

		SqlQuery actionSqlQuery = new SqlQuery(this.plugin);
		actionSqlQuery.setMenuAcitons(menuOption, optionColor);
		actionSqlQuery.setSource(source);

		newOption = transformer.transform(actionWordArray, actionColorArray, menuOptionTransformOption, actionSqlQuery, false);

		if(Colors.removeColorTag(menuTarget).isEmpty()) { // if it doesnt have a target
			newTarget = "";
		} else {
			// if it is in the quest tab, the values are in a different category/sub_category
			if(source.equals(SqlVariables.questListTabInSource.getValue())) {
				menuTarget = translateQuestName(targetWordArray[0]);
				return new String[]{menuTarget, newOption};
			}
			SqlQuery targetSqlQuery = new SqlQuery(this.plugin);
			targetSqlQuery.setEnglish(targetWordArray[0]);
			targetSqlQuery.setCategory(SqlVariables.nameInCategory.getValue());
			targetSqlQuery.setSubCategory(SqlVariables.menuInSubCategory.getValue());
			targetSqlQuery.setSource(source);

			TransformOption generalMenuTransformOption = getTransformOption(this.plugin.getConfig().getMenuOptionConfig());
			Colors[] targetColorArray = Colors.getColorArray(menuTarget, Colors.orange); //default color is not the same as initial definition
			newTarget = transformer.transform(targetWordArray, targetColorArray, generalMenuTransformOption, targetSqlQuery, false);
		}
		return new String[]{newTarget, newOption};
	}

	private String translateQuestName(String questName) {
		SqlQuery targetSqlQuery = new SqlQuery(this.plugin);
		targetSqlQuery.setEnglish(questName);
		targetSqlQuery.setCategory(SqlVariables.manualInCategory.getValue());
		targetSqlQuery.setSubCategory(SqlVariables.questInSubCategory.getValue());

		TransformOption generalMenuTransformOption = getTransformOption(this.plugin.getConfig().getMenuOptionConfig());
		// color is not possible to obtain by simple means, so its just orange for now
		return transformer.transform(questName, Colors.orange, generalMenuTransformOption, targetSqlQuery, false);
	}

	private String translatePlayerTargetPart(String[] targetWordArray, Colors[] targetColorArray) {

		//leave name as is (but still give to transformer to replace to char image if needed)
		if(!targetWordArray[0].matches("^<img=.*>$")) { // doesn't have icons before their names
			String playerName = targetWordArray[0];
			String translatedName = transformer.transform(playerName, Colors.white, TransformOption.AS_IS, null, false);

			return translatedName + getLevelTranslation(targetWordArray[1], targetColorArray[1]);
		} else {
			// contains icons before their names, such as clan rank symbols
			StringBuilder newName = new StringBuilder();
			String levelString = "  (level-0)";
			for(int i = 0; i < targetWordArray.length; i++){
				if(i == targetWordArray.length - 1){ // the last element of targetWordArray is always the level part
					levelString = targetWordArray[i];
					break;
				}
				newName.append(targetWordArray[i]);
			}
			String translatedName = transformer.transform(newName.toString(), Colors.white, TransformOption.AS_IS, null, false);
			return translatedName + getLevelTranslation(levelString, targetColorArray[targetWordArray.length - 1]);
		}
	}

	private String getSourceNameFromMenu(MenuEntry menu){
		String source = "";
		Ids ids = this.plugin.getIds();
		if(isChildWidgetOf(ids.getCombatOptionParentWidgetId(), menu)){
			source = SqlVariables.combatOptionsTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getSkillsTabParentWidgetId(),menu)){
			source = SqlVariables.skillsTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getCharacterSummaryTabWidgetId(),menu)){
			source = SqlVariables.characterSummaryTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getQuestTabParentWidgetId(), menu)){
			source = SqlVariables.questListTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getAchievementDiaryTabParentWidgetId(),menu)){
			source = SqlVariables.achievementDiaryTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getInventoryTabParentWidgetId(),menu)){
			source = SqlVariables.inventTabInSource.getValue();
		}  else if(isChildWidgetOf(ids.getEquipmentTabParentWidgetId(),menu)){
			source = SqlVariables.wornEquipmentTabInSource.getValue();
		}else if(isChildWidgetOf(ids.getPrayerTabParentWidgetId(),menu)){
			source = SqlVariables.prayerTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getSpellBookTabParentWidgetId(), menu)){
			source = SqlVariables.spellBookTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getGroupsTabParentWidgetId(), menu)){
			source = SqlVariables.groupTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getFriendsTabParentWidgetId(), menu)){
			source = SqlVariables.friendsTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getIgnoreTabParentWidgetId(), menu)){
			source = SqlVariables.ignoreTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getAccountManagementTabParentWidgetId(), menu)){
			source = SqlVariables.accountManagementTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getSettingsTabParentWidgetId(), menu)){
			source = SqlVariables.settingsTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getLogoutTabParentWidgetId(), menu)){
			source = SqlVariables.logoutTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getWorldSwitcherTabParentWidgetId(), menu)) {
			source = SqlVariables.worldSwitcherTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getEmotesTabParentWidgetId(), menu)){
			source = SqlVariables.emotesTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getMusicTabParentWidgetId(), menu)){
			source = SqlVariables.musicTabInSource.getValue();
		}
		//log.info("source: " + source);
		return source;
	}

	private String getLevelTranslation(String levelString, Colors color) {
		// translates combat level, such as "  (level-15)"
		SqlQuery levelQuery = new SqlQuery(this.plugin);
		String level = levelString.replaceAll("[^0-9]", "");
		levelQuery.setPlayerLevel();
		Transformer transformer = new Transformer(this.plugin);

		TransformOption option = getTransformOption(this.plugin.getConfig().getGameMessagesConfig());
	//TODO: colours may need adjusting for () and level's digits
		if(plugin.getConfig().getSelectedLanguage().needsCharImages()) // change color to simple color variants. eg: light green to green
			color = color.getSimpleColor();
		String levelTranslation = transformer.transform(levelQuery.getEnglish(), color, option, levelQuery, false);
		String openBracket = transformer.transform("(", color, TransformOption.AS_IS, null, false);
		String lvAndCloseBracket = transformer.transform(level+")", color, TransformOption.AS_IS, null, false);
		return "  " + color.getColorTag() + openBracket  + levelTranslation + color.getColorTag() + lvAndCloseBracket;
	}

	public static TransformOption getTransformOption(ingameTranslationConfig conf) {
		TransformOption transformOption;
		if(conf.equals(ingameTranslationConfig.USE_LOCAL_DATA)){
			transformOption = TransformOption.TRANSLATE_LOCAL;
		} else if(conf.equals(ingameTranslationConfig.DONT_TRANSLATE)){
			transformOption = TransformOption.AS_IS;
		} else if(conf.equals(ingameTranslationConfig.USE_API)){
			transformOption = TransformOption.TRANSLATE_API;
		} else {
			transformOption = TransformOption.TRANSLATE_LOCAL;
		}
		return transformOption;
	}

	private void printMenuEntry(MenuEntry menuEntry)
	{
		String target = menuEntry.getTarget();
		String option = menuEntry.getOption();
		MenuAction type = menuEntry.getType();
		log.info("option: " + option + ", target: " + target + ", type: " + type);
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

	private boolean isWalkOrCancel(MenuAction action)
	{
		return ((action.equals(MenuAction.CANCEL))
				|| (action.equals(MenuAction.WALK)));
	}

	public boolean isGeneralMenu(MenuEntry menuEntry)
	{
		MenuAction action = menuEntry.getType();
		// checks if current action target is a menu that introduces general actions
		return  (!isItemInWidget(menuEntry) &&
					(action.equals(MenuAction.CC_OP)
						|| action.equals(MenuAction.CC_OP_LOW_PRIORITY)
						|| isWalkOrCancel(action)
						|| action.equals(MenuAction.RUNELITE_OVERLAY)
						|| action.equals(MenuAction.RUNELITE)
					)
		);
	}

	public boolean isObjectMenu(MenuAction action)
	{
		return ((action.equals(MenuAction.EXAMINE_OBJECT))
				|| (action.equals(MenuAction.GAME_OBJECT_FIRST_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_SECOND_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_THIRD_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_FOURTH_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_FIFTH_OPTION)));
	}
	public boolean isNpcMenu(MenuAction action)
	{
		return ((action.equals(MenuAction.EXAMINE_NPC))
				|| (action.equals(MenuAction.NPC_FIRST_OPTION))
				|| (action.equals(MenuAction.NPC_SECOND_OPTION))
				|| (action.equals(MenuAction.NPC_THIRD_OPTION))
				|| (action.equals(MenuAction.NPC_FOURTH_OPTION))
				|| (action.equals(MenuAction.NPC_FIFTH_OPTION)));
	}
	public boolean isItemOnGround(MenuAction action)
	{
		return ((action.equals(MenuAction.EXAMINE_ITEM_GROUND))
				|| (action.equals(MenuAction.GROUND_ITEM_FIRST_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_SECOND_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_THIRD_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_FOURTH_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_FIFTH_OPTION)));
	}
	public boolean isPlayerMenu(MenuAction action)
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
	public boolean isWidgetOnSomething(MenuAction action)
	{
		return ((action.equals(MenuAction.WIDGET_TARGET_ON_WIDGET))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_NPC))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_PLAYER)));
	}

	public boolean isItemInWidget(MenuEntry menuEntry){
		MenuAction action = menuEntry.getType();
		String target = menuEntry.getTarget();
		target = Colors.removeColorTag(target);
		if(target.isEmpty()){
			return false;
		}

		SqlQuery sqlQuery = new SqlQuery(this.plugin);
		sqlQuery.setItemName(target, Colors.orange);


		return sqlQuery.getMatching(SqlVariables.columnEnglish, false).length > 0 &&
				(action.equals(MenuAction.CC_OP)
						|| action.equals(MenuAction.CC_OP_LOW_PRIORITY)
						|| action.equals(MenuAction.WIDGET_TARGET)
				);

	}

	private boolean isChildWidgetOf(int widgetIdToCheck, MenuEntry menuEntry){
		if(widgetIdToCheck == -1){
			return false;
		}
		Widget widget = client.getWidget(menuEntry.getParam1());

		while(widget != null){
			if(widget.getId() == widgetIdToCheck){
				return true;
			}
			widget = widget.getParent();
		}
		return false;
	}


}