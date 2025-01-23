package com.RuneLingual.constants;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Slf4j
public enum Colors
{
    black("000000","black"),
    black2("0","black"),
    blue("0000ff","blue"),
    blue2("ff","blue"),
    green("00ff00","green"),
    green2("ff00","green"),
    green3("c0ff00", "green"),
    green4("dc10d", "green"),
    lightblue("00ffff","lightblue"),
    lightblue2("ffff", "lightblue"),
    orange("ff7000","orange"),
    orange2("ff9040","orange"),
    orange3("ff981f","orange"),
    red("ff0000","red"),
    red2("800000","red"),
    red3("6800bf","red"),
    white("ffffff","white"),
    white2("9f9f9f","white"),
    yellow("ffff00", "yellow");

    private final String name;
    private final String hex;

    private Colors(String hex, String name)
    {
        this.hex = hex;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String getHex()
    {
        return hex;
    }

    public String getColorTag()
    {
        return "<col=" + this.getHex() + ">";
    }

    public static Colors getColorFromHex(String hex)
    {
        //number of colors
        int[] colorInts = new int[Colors.values().length];

        for (int i = 0; i < Colors.values().length; i++)
        {
            String colName = Colors.values()[i].hex;
            if (hex.equals(colName))
                return Colors.values()[i];
            colorInts[i] = hexToInt(Colors.values()[i].getHex());
        }

        int j = findClosest(hexToInt(hex),colorInts);

        // todo: maybe this should throw an exception
        return Colors.values()[j];
    }

    public static Colors fromInt(int intColor)
    {
        String hexString = IntToHex(intColor);
        return getColorFromHex(hexString);
    }

    private static int findClosest(int target, int[] numbers)
    {
        if (target == hexToInt("f9f9f9"))
        {
            //int for hex 9f9f9f, grey text in settings
            int i;

            for (i = 1; i < numbers.length; i++)
            {
                if (Colors.values()[i] == Colors.white)
                {
                    return i;
                }
            }

            return i;
        }
        else
        {
            int smallestDifference = Math.abs(numbers[0] - target);
            int closestI = 0;

            for(int i = 1; i < numbers.length; i++)
            {
                int currentDifference = Math.abs(numbers[i] - target);
                if (currentDifference < smallestDifference)
                {
                    smallestDifference = currentDifference;
                    closestI = i;
                }
            }
            return closestI;
        }
    }

    public static String IntToHex(int intColor)
    {
        String hexString = String.format("%06x",intColor);

        for (Colors Colors : values())
        {
            if (Colors.getHex().equalsIgnoreCase(hexString))
            {
                return Colors.getHex();
            }
        }

        // todo: maybe this should throw an exception
        return hexString;
    }

    public static int hexToInt(String hex)
    {
        if(hex.length() > 6)
        {
            hex = hex.substring(0,6);
        }
        return Integer.parseInt(hex, 16);
    }

    public Colors getSimpleColor()
    {
        if (this.getName().equals(Colors.green.getName()))
        {
            //if the color is green
            return Colors.green;
        }
        if (this.getName().equals(Colors.red.getName()))
        {
            //if the color is red
            return Colors.red;
        }
        if (this.getName().equals(Colors.blue.getName()))
        {
            //if the color is blue
            return Colors.blue;
        }
        if (this.getName().equals(Colors.orange.getName()))
        {
            //if the color is orange
            return Colors.orange;
        }
        if (this.getName().equals(Colors.yellow.getName()))
        {
            //if the color is yellow
            return Colors.yellow;
        }
        if (this.getName().equals(Colors.white.getName()))
        {
            //if the color is white
            return Colors.white;
        }
        if (this.getName().equals(Colors.black.getName()))
        {
            //if the color is black
            return Colors.black;
        }
        if (this.getName().equals(Colors.lightblue.getName()))
        {
            //if the color is lightblue
            return Colors.lightblue;
        }
        return this;
    }

    public static int countColorTagsAfterReformat(String wordAndColor)
    {
        // count number of color tags in a string
        wordAndColor = reformatColorWord(wordAndColor, Colors.white);
        Pattern re = Pattern.compile("(?<=\\d)>|(?<=\\p{IsAlphabetic})>");
        String[] splitResult = re.split(wordAndColor);
        if (splitResult.length == 0)
        {
            return 0;
        }

        if(splitResult.length == 1 && splitResult[0].isEmpty())
        {
            return 0;
        }
        return re.split(wordAndColor).length - 1;
    }

    public static Colors[] getColorArray(String strWithColor, Colors defaultColor)
    {
        /*
        *   This function takes a string with color tags and returns a list of color names
        *   eg: <col=ff0000>Nex<col=ffffff> (level-1) -> ["red", "white"]
        */

        // if there are no color tags, return defaultColor
        if(countColorTagsAfterReformat(strWithColor) == 0)
        {
            return new Colors[]{defaultColor};
        }

        strWithColor = reformatColorWord(strWithColor, defaultColor);

        // if there are color tags, return the color names
        String[] parts = strWithColor.split("<col=");
        Colors[] colorArray = new Colors[parts.length - 1];
        Pattern re = Pattern.compile("(?<=\\d)>|(?<=\\p{IsAlphabetic})>");
        for (int i = 0; i < parts.length - 1; i++)
        {
            Colors c = Colors.getColorFromHex(re.split(parts[i + 1])[0]);
            colorArray[i] = c;
            if (colorArray[i] == null || Objects.equals(colorArray[i], ""))
            {
                colorArray[i] = defaultColor;
            }
        }
        return colorArray;
    }

    public static String[] getWordArray(String strWithColor)
    {
        /*
        *   This function takes a string with color tags and returns a list of words
        *   eg: <img=3><colHIGHLIGHT>Nex<col=ffffff> (level-1) -> ["<img=3>", "Nex", " (level-1)"]
        */
        strWithColor = reformatColorWord(strWithColor, Colors.white);
        Pattern re = Pattern.compile("<col=[a-zA-Z0-9]*?>");
        String[] strArray = re.split(strWithColor);
        if (strArray.length == 0)
        {
            return new String[0];
        }
        if (strArray[0].isEmpty())
        {
            if (strArray.length == 1)
            {
                return new String[0];
            }
            else
            {
                return Arrays.copyOfRange(strArray, 1, strArray.length);
            }
        }
        return re.split(strWithColor);
    }

    private static String reformatColorWord(String colWord, Colors defaultColor)
    {
        // replace <colNORMAL> with <col=0>, <colHIGHLIGHT> with <col=ff0000>, etc.
        colWord = colWord.replace("<colNORMAL>", "<col=0>");
        colWord = colWord.replace("<colHIGHLIGHT>", "<col=ff0000>");
        // todo: if there are any color tags that are not in the enum
        // todo: add and replace them with <col=??> here like above

        // give words after </col> the default color
        // <col=ff0000>Nex</col> (level-1) -> <col=ff0000>Nex<col=ffffff> (level-1)
        colWord = colWord.replace("</col>",defaultColor.getColorTag());

        // give the beginning words without a color tag the default color
        // Nex <col=ffffff> (level-1) -> <col=ff0000>Nex <col=ffffff>(level-1)
        if (!colWord.startsWith("<col"))
        {
            colWord = defaultColor.getColorTag() + colWord;
        }

        // remove the color tag at the end of the word
        // <col=ff0000>Nex<col=ffffff> (level-1) <col=f0f0f0> -> <col=ff0000>Nex<col=ffffff> (level-1)
        colWord = colWord.replaceAll("<col=[a-zA-Z0-9]*?>$","");

        return colWord;
    }

    public static String removeColorTag(String str)
    {
        return str.replaceAll("<(?!img|>).*?>", "");
    }

    public static String removeAllTags(String str)
    {
        return str.replaceAll("<.*?>", "");
    }

    public static String enumerateColorsInColWord(String colWord)
    {
        Pattern re = Pattern.compile("<col[=a-zA-Z0-9]*?>");
        String[] parts = re.split(colWord);
        StringBuilder colorString = new StringBuilder();
        for (int i = 0; i < parts.length; i++)
        {
            colorString.append(parts[i]);
            if (i < parts.length - 1)
            {
                colorString.append("<colNum").append(i).append(">");
            }
        }
        return colorString.toString();
    }

    public static List<String> getColorTagsAsIs(String strWithColor)
    {
        // supports abnormal color tags such as <colHIGHLIGHT>, as long as its only numbers and alphabets, no symbols
        List<String> matches = new ArrayList<>();
        Pattern pattern = Pattern.compile("<col[=a-zA-Z0-9]*?>");
        Matcher matcher = pattern.matcher(strWithColor);

        while (matcher.find())
        {
            matches.add(matcher.group());
        }
        return matches;
    }
}