package com.RuneLingual.nonLatin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;

import com.RuneLingual.prepareResources.Downloader;
import com.RuneLingual.RuneLingualPlugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ChatIconManager;

import javax.imageio.ImageIO;
import javax.inject.Inject;

@Slf4j
public class CharImageInit {
    @Inject
    RuneLingualPlugin runeLingualPlugin;

    /*
    * Load character images from the local folder, and register them to the chatIconManager
    * The images are downloaded from the RuneLingual transcript website, which is done in the Downloader class.
     */
    public void loadCharImages()
    {
        //if the target language doesn't need character images, return
        if(!runeLingualPlugin.getTargetLanguage().needsCharImages()){
            return;
        }

        ChatIconManager chatIconManager = runeLingualPlugin.getChatIconManager();
        HashMap<String, Integer> charIds = runeLingualPlugin.getCharIds();

        Downloader downloader = runeLingualPlugin.getDownloader();
        String langCode = downloader.getLangCode();
        final String pathToChar = downloader.getLocalLangFolder().toString() + File.separator + "char_" + langCode;

        String[] charNameArray = getCharList(pathToChar); //list of all characters e.g.　black--3021.png

        for (String imageName : charNameArray) {//register all character images to chatIconManager
            try {
                String fullPath = pathToChar + File.separator + imageName;
                File externalCharImg = new File(fullPath);
                final BufferedImage image = ImageIO.read(externalCharImg);

                final int charID = chatIconManager.registerChatIcon(image);
                charIds.put(imageName, charID);
            } catch (Exception e){log.info(String.valueOf(e));}
        }
        log.info("end of making character image hashmap");
    }


    public String[] getCharList(String pathToChar) {//get list of names of all characters of every colours)
        FilenameFilter pngFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".png");
            }
        };
        File colorDir = new File(pathToChar + "/");
        File[] files = colorDir.listFiles(pngFilter); //list of files that end with ".png"

        if (files == null){return new String[]{};}

        String[] charImageNames = new String[files.length];
        for (int j = 0; j < files.length; j++) {
            charImageNames[j] = files[j].getName();
        }
        return charImageNames;
    }
}
