package com.RuneLingual.nonLatin;

import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ChatIconManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;

@Slf4j
public class GeneralFunctions {
    @Inject
    private CharImageInit charImageInit;
    @Inject
    private RuneLingualPlugin runeLingualPlugin;


    public String StringToTags(String string, Colors colors) {
        /*
        This function takes a string + color and returns emojis that looks like letters
        But leave <img=??> tags as they are (they are already emojis)
        eg: こんにちは,<img=43>Lukyさん -> <img=43><img=1><img=2><img=3><img=4><img=5><img=6>

        order of conversion : String -> (for each char) -> char to codepoint -> get image name (eg: black--3021.png) ->
        -> get hash value of the char image from the hashmap
        -> get image Id in the chatIcon manager from the hash value
        -> create a tag with the image Id (eg: <img=1>)
        -> repeat to all characters in the string -> append all tags -> return
         */
        String pattern = "<img=[0-9]*>";
        String[] parts = string.split("(?=" + pattern + ")|(?<=" + pattern + ")");
        StringBuilder imgTagSb = new StringBuilder();
        for (String part : parts) {//check for img tags, such as the ironman icon
            if (part.matches(pattern)) {//if the part is an img tag, just append it
                imgTagSb.append(part);
                continue;
            }
            StringBuilder imgTagStrings = new StringBuilder();
            ChatIconManager chatIconManager = runeLingualPlugin.getChatIconManager();
            HashMap<String, Integer> map = runeLingualPlugin.getCharIds();
            for (int j = 0; j < part.length(); ) {//if the part is not an img tag, convert each letters to letter emojis

                int codePoint = part.codePointAt(j);
                if (Arrays.asList(32, 160, 8195, 8194, 8201, 8202, 8203, 12288).contains(codePoint)) {//if the char is a space, append a space
                    imgTagStrings.append(" ");
                    j += 1;
                    continue;
                }
                String imgName = colors.getName() + "--" + codePoint + ".png";
                int hash = map.getOrDefault(imgName, -99);
                if (hash == -99) {//if the char is not in the hashmap, append a question mark
                    imgTagStrings.append("?");
                    j += Character.isHighSurrogate(part.charAt(j)) ? 2 : 1;
                    log.info("Char not found in hashmap: " + part.charAt(j));
                }
                imgTagStrings.append("<img=");
                imgTagStrings.append(chatIconManager.chatIconIndex(hash));
                imgTagStrings.append(">");
                j += Character.isHighSurrogate(part.charAt(j)) ? 2 : 1;

            }
            imgTagSb.append(imgTagStrings);
        }
        return imgTagSb.toString();
    }

}
