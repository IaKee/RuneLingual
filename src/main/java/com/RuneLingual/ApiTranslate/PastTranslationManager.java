package com.RuneLingual.ApiTranslate;

import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.FileActions;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuEntry;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

@Slf4j
public class PastTranslationManager {
    private final String pastTranslationFile;
    @Inject
    private Deepl deepl;
    private RuneLingualPlugin plugin;
    private HashMap<String, String> pastTranslations = new HashMap<>();


    @Inject
    public PastTranslationManager(Deepl deepl, RuneLingualPlugin plugin) {
        this.plugin = plugin;
        this.deepl = deepl;
        pastTranslationFile = FileNameAndPath.getLocalBaseFolder().getPath() + File.separator +
                plugin.getConfig().getSelectedLanguage().getLangCode() + File.separator + "pastTranslations.txt";
        setPastTranslationsFromFile();
    }

    public void setPastTranslationsFromFile() {
        // if pastTranslationFile exists, read from it
        if (FileActions.fileExists(pastTranslationFile)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(pastTranslationFile), StandardCharsets.UTF_8))) {
                // Skip BOM if present
                reader.mark(1);
                if (reader.read() != 0xFEFF) {
                    reader.reset();
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        pastTranslations.put(key, value);
                    }
                }

            } catch (IOException e) {
                log.error("Error reading file: " + e.getMessage(), e);
            }
        } else {
            // if pastTranslationFile doesn't exist, create an empty one
            try {
                Files.createFile(Paths.get(pastTranslationFile));
                log.info("Created empty file: " + pastTranslationFile);
            } catch (IOException e) {
                log.error("Error creating file: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Get past translation from hashmap
     *
     * @param text the text to get past translation for
     * @return the past translation if it exists, null otherwise
     */
    public String getPastTranslation(String text) {
        return pastTranslations.getOrDefault(text, null);
    }

    public void addToPastTranslations(String text, String translation) {
        // add to the hashmap
        pastTranslations.put(text, translation);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(pastTranslationFile, true), StandardCharsets.UTF_8))) {
            String content = text + "|" + translation + "\n";
            writer.write(content);
            writer.flush(); // Ensure the content is written immediately
        } catch (IOException e) {
            log.error("Error writing to file: " + e.getMessage(), e);
        }
    }

    public boolean haveTranslatedMenuBefore(String option, String target, MenuEntry menuEntry) {
        String[] optionWordArray = Colors.getWordArray(option);
        String[] targetWordArray = Colors.getWordArray(target);

        // if option is set to be translated with API, check if all elements have been translated before
        if (plugin.getConfig().getMenuOptionConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)) {
            if (!checkAllElementExistInPastTranslation(optionWordArray)) {
                return false;
            }
        }

        // if target is item name and that is set to be translated with API,
        // check if all elements have been translated before
        if (plugin.getConfig().getItemNamesConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)
                && (plugin.getMenuCapture().isItemInWidget(menuEntry) || plugin.getMenuCapture().isItemOnGround(menuEntry.getType()))) {
            if (!checkAllElementExistInPastTranslation(targetWordArray)) {
                return false;
            }
        }

        // if target is object name, check if all elements have been translated before
        if (plugin.getConfig().getObjectNamesConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)
                && plugin.getMenuCapture().isObjectMenu(menuEntry.getType())) {
            if (!checkAllElementExistInPastTranslation(targetWordArray)) {
                return false;
            }
        }

        // if target is npc name, check if all elements have been translated before
        if (plugin.getConfig().getNPCNamesConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)
                && plugin.getMenuCapture().isNpcMenu(menuEntry.getType())) {
            if (!checkAllElementExistInPastTranslation(targetWordArray)) {
                return false;
            }
        }


        // if other target (general menu, walk here, player, etc) is set to be translated with API,
        // check if all elements have been translated before
        if (!target.isEmpty() &&
                plugin.getConfig().getMenuOptionConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)
                && !plugin.getMenuCapture().isItemInWidget(menuEntry)
                && !plugin.getMenuCapture().isItemOnGround(menuEntry.getType())
                && !plugin.getMenuCapture().isObjectMenu(menuEntry.getType())
                && !plugin.getMenuCapture().isNpcMenu(menuEntry.getType())) {
            if (!checkAllElementExistInPastTranslation(targetWordArray)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkAllElementExistInPastTranslation(String[] wordArray) {
        for (String word : wordArray) {
            if (plugin.getDeepl().getDeeplPastTranslationManager().getPastTranslation(word) == null) {
                return false;
            }
        }
        return true;
    }
}
