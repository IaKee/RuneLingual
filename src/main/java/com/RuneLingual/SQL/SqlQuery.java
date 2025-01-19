package com.RuneLingual.SQL;

import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import lombok.Getter;
import lombok.Setter;

import com.RuneLingual.SQL.SqlActions;
import com.RuneLingual.SQL.SqlVariables;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter @Setter
public class SqlQuery {
    private String english; // the whole text, not a part of Colors.wordArray
    private String translation;
    private String category;
    private String subCategory;
    private String source;

    private Colors color;


    @Inject
    RuneLingualPlugin plugin;

    @Inject
    public SqlQuery(RuneLingualPlugin plugin){
        this.plugin = plugin;
        this.english = null;
        this.translation = null;
        this.category = null;
        this.subCategory = null;
        this.source = null;
        this.color = null;
    }

    public String[] getMatching(SqlVariables column, boolean searchAlike) {
        // create query -> execute -> return result
        String query = getSearchQuery();
        query = query.replace("*", column.getColumnName());
        String[][] result = plugin.getSqlActions().executeSearchQuery(query);
        if(result.length == 0 && searchAlike){
            return new String[]{getPlaceholderMatches()};
        }
        String[] translations = new String[result.length];
        for (int i = 0; i < result.length; i++){
            translations[i] = result[i][0];
        }
        return translations;
    }

    public String[] getMatching(SqlVariables[] columns) {
        // create query -> execute -> return result
        String query = getSearchQuery();
        String[] translations = new String[columns.length];
        for (int i = 0; i < columns.length; i++){
            query = query.replace("*", columns[i].getColumnName());
            String[][] result = plugin.getSqlActions().executeSearchQuery(query);
            translations[i] = result[0][0];
        }
        return translations;
    }

    private String getPlaceholderMatches(){
        /*
        returns translation which includes placeholders at first that matches the english text,
        with the placeholders replaced with the corresponding english word/number.
        placeholders =  %s0, %s1,... for strings atleast 1 alphabet and 0 or more numbers/spaces
                        %d0, %d1,... for numbers (and only numbers)
        1. gets all records that contains placeholder values in English, and matches the query except for english
        if no matches with placeholders are found, returns the original english text
        2. returns the translation of the first match
        3. if no match is found, returns the original english text
        not tested for %s, nor tested throughly for %d
         */
        String[] placeholders = {"%s", "%d"};
        String query = getPlaceholderSearchQuery(placeholders);
        String[][] result = plugin.getSqlActions().executeSearchQuery(query);
        // returns a placeholder if no matches are found
        if (result == null || result.length == 0){
            return english;
        }
        for (String[] row : result){
            String englishWithPlaceholders = row[0];
            String translationWithPlaceholders = row[1];
            String replacedMatch = englishWithPlaceholders;
            // Replace placeholders
            // Replace placeholders for strings
            for (int i = 0; i < 100; i++) {
                String beforeReplace = replacedMatch;
                replacedMatch = replacedMatch.replace("%s" + i, "[ \\w]+");
                if (beforeReplace.equals(replacedMatch)){
                    break;
                }
            }

            // Replace placeholders for numbers
            for (int i = 0; i < 100; i++) {
                String beforeReplace = replacedMatch;
                replacedMatch = replacedMatch.replace("%d" + i, "\\d+");
                if (beforeReplace.equals(replacedMatch)){
                    break;
                }
            }

            replacedMatch = stringToRegex(replacedMatch);

            Pattern pattern = Pattern.compile(replacedMatch);
            Matcher matcher = pattern.matcher(this.english);
            if (!matcher.matches()){
                continue;
            }
            List<String> matchedStrings = new ArrayList<>();
            List<String> matchedNumbers = new ArrayList<>();
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group.matches("\\d+")) {
                    matchedStrings.add(group);
                } else if (group.matches("[ \\w]+")) {
                    matchedNumbers.add(group);
                }
            }

            // Replace placeholders in the translated text
            String translation = translationWithPlaceholders;
            for (int i = 0; i < matchedStrings.size(); i++) {
                translation = translation.replace("%s" + i, matchedStrings.get(i));
            }
            for (int i = 0; i < matchedNumbers.size(); i++) {
                translation = translation.replace("%d" + i, matchedNumbers.get(i));
            }
            return translation;
        }

        return english;
    }

    private String stringToRegex(String str){
        return str.replaceAll("([\\[\\](){}*+?^$.|])", "\\\\$1");
    }

    public String getSearchQuery() {
        // creates query that matches all non-empty fields
        // returns null if no fields are filled
        String query = "SELECT * FROM " + SqlActions.tableName + " WHERE ";
        if (english != null && !english.isEmpty()){
            query += SqlVariables.columnEnglish.getColumnName() + " = '" + english.replace("'","''") + "' AND ";
        }
        if (category != null && !category.isEmpty()){
            query += SqlVariables.columnCategory.getColumnName() + " = '" + category + "' AND ";
        }
        if (subCategory != null && !subCategory.isEmpty()){
            query += SqlVariables.columnSubCategory.getColumnName() + " = '" + subCategory + "' AND ";
        }
        if (source != null && !source.isEmpty()){
            query += SqlVariables.columnSource.getColumnName() + " = '" + source + "' AND ";
        }
        if (translation != null && !translation.isEmpty()){
            query += SqlVariables.columnTranslation.getColumnName() + " = '" + translation.replace("'","''") + "' AND ";
        } //todo: add more here if columns to be filtered are added

        if (query.endsWith("AND ")){
            query = query.substring(0, query.length() - 5);
            return query;
        }
        return null;
    }

    public String getPlaceholderSearchQuery(String[] placeholders) {
        // creates query that matches all non-empty fields
        // returns null if no fields are filled
        // return only english
        String query = "SELECT english, translation FROM " + SqlActions.tableName + " WHERE (english LIKE '%\\%s%' OR english LIKE '%\\%d%') AND ";
        if (category != null && !category.isEmpty()){
            query += SqlVariables.columnCategory.getColumnName() + " = '" + category + "' AND ";
        }
        if (subCategory != null && !subCategory.isEmpty()){
            query += SqlVariables.columnSubCategory.getColumnName() + " = '" + subCategory + "' AND ";
        }
        if (source != null && !source.isEmpty()){
            query += SqlVariables.columnSource.getColumnName() + " = '" + source + "' AND ";
        }
        if (query.endsWith("AND ")){
            query = query.substring(0, query.length() - 5);
            return query;
        }
        //todo: add more here if columns to be filtered are added
        return query;
    }

    public void setEnCatSubcat(String english, String category, String subCategory, Colors defaultColor){
        this.english = english;
        this.category = category;
        this.subCategory = subCategory;
        this.color = defaultColor;
    }

    public void setItemName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.nameInCategory.getValue();
        this.subCategory = SqlVariables.itemInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setNpcName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.nameInCategory.getValue();
        this.subCategory = SqlVariables.npcInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setObjectName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.nameInCategory.getValue();
        this.subCategory = SqlVariables.objInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setMenuName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.nameInCategory.getValue();
        this.subCategory = SqlVariables.menuInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }




    public void setInventoryItemActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.inventActionsInCategory.getValue();
        this.subCategory = SqlVariables.itemInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setGroundItemActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.actionsInCategory.getValue();
        this.subCategory = SqlVariables.itemInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setNpcActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.actionsInCategory.getValue();
        this.subCategory = SqlVariables.npcInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setObjectActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.actionsInCategory.getValue();
        this.subCategory = SqlVariables.objInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setMenuAcitons(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.actionsInCategory.getValue();
        this.subCategory = SqlVariables.menuInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setPlayerActions(String en, Colors defualtColor){
        this.english = en;
        this.category = SqlVariables.actionsInCategory.getValue();
        this.subCategory = SqlVariables.playerInSubCategory.getValue();
        this.color = defualtColor;
        this.source = null;
        this.translation = null;
    }
    public void setPlayerLevel() {
        this.english = "level";
        this.category = SqlVariables.nameInCategory.getValue();
        this.subCategory = SqlVariables.levelInSubCategory.getValue();
        this.source = null;
        this.translation = null;
    }

    public void setDialogue(String en, String npcTalkingTo, boolean speakerIsPlayer, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.dialogueInCategory.getValue();
        this.subCategory = npcTalkingTo;
        this.color = defaultColor;
        if(speakerIsPlayer){
            this.source = "Player";
        } else {
            this.source = npcTalkingTo;
        }
        this.translation = null;
    }

}
