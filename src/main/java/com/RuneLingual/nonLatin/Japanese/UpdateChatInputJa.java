package com.RuneLingual.nonLatin.Japanese;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.ChatMessages.PlayerMessage;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.commonFunctions.Transformer;
import com.RuneLingual.nonLatin.Japanese.Rom2hira.FourValues;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class UpdateChatInputJa {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private PlayerMessage playerMessage;
    @Inject @Getter
    private Rom2hira rom2Hira;
    @Getter
    private int inputCount = 0; //for counting number of words in chat input
    @Getter
    private String chatJpMsg = "";//whats written for chat input overlay
    @Getter
    private List<String> kanjKatCandidates = new ArrayList<>();//candidates of kanji or katakana from input, also used for candidates overlay
    @Getter
    private int instCandidateSelection = -1;
    private List<FourValues> japCharDS = new ArrayList<>();//store all japanese words's written form, how its read, type, rank
    private List<String> prevHiraList = new ArrayList<>();//stores the last updated words that are displayed
    private List<String> prevJPList = new ArrayList<>();
    private HashMap<String,String> char2char = new HashMap<>();
    private String notAvailable = "nan";

    @Inject
    public UpdateChatInputJa(RuneLingualPlugin plugin) {
        this.plugin = plugin;
        String wordsDir = FileNameAndPath.getLocalBaseFolder() + "/" +
                LangCodeSelectableList.日本語.getLangCode() +
                "/foreign2foreign_" + LangCodeSelectableList.日本語.getLangCode() + ".txt";
        putWordToHash(wordsDir);
        log.info("created hashmap for transform dict, for type : " + wordsDir);
    }

    private void putWordToHash(String dirName) {
        /*
        * these files are in the format of "written,read,type,rank"
        * written is the kanji or katakana.
        * read is the hiragana.
        * type is the type of word, eg. noun, verb, adj, etc.
        * rank is the rank of the word, the smaller the number, the more common the word is.
        *
        * The file itself is downloaded by Downloader, so are not jar files
        * */
        try (BufferedReader reader = new BufferedReader(new InputStreamReader
                (new FileInputStream(dirName), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    FourValues fourValues = new FourValues(parts[0],parts[1],parts[2],Integer.parseInt(parts[3]));
                    japCharDS.add(fourValues);
                } else {
                    log.info("rom to jap.csv : not enough elements : "+parts.length);
                }
            }
        } catch (IOException e) {
            log.info("error creating hashmap for transform dict, for type : " + dirName);
            e.printStackTrace();
        }
    }

    public void updateInput() {
        if (playerMessage.getTranslationOption().equals(Transformer.TransformOption.AS_IS)) {
            return;
        }

        String inputmsg = playerMessage.getChatInputString();
        if (inputmsg.isBlank()){
            inputCount = 0;
            chatJpMsg = "";
            return;
        }

        inputCount = inputmsg.length();
        String newMsg = romJpTransform(inputmsg, true);
        if (newMsg != null && !newMsg.isEmpty())
            chatJpMsg = newMsg;
    }

    public String romJpTransform(String romMsg, boolean chatInput) {
        String hiraMsg = this.rom2Hira.romToKat(romMsg);
        return hira2Jp(hiraMsg, chatInput);
    }


    private String hira2Jp(String hiraMsg, boolean chatInput) {//hiragana list to kanji sentence
        String[] wordList = getWakatiGaki(hiraMsg);//get katakana text split with symbols, space attached at the end of each string if its katakana or numbers
        //eg of wordList : "強烈はぴはぴ閾値" "、," "凄く"  "!/" "だよ" "、" "kaka" "クェスト00" "やり" "33" "," "ましょう"
        List<String> wordListList = Arrays.asList(getWakatiGaki(hiraMsg));

        if (compareLists(wordListList, prevHiraList) && chatInput)
            return null;
        int startIndex = searchFirstDif(wordList);
        if (startIndex < 1)
            startIndex = 0;
        List<String> changedList = new ArrayList<>();
        boolean last;
        for (int i = startIndex; i < wordList.length; i++) {
            String word = wordList[i];
            if (word.matches("^\\d+$") || word.matches("^を+$")
                    || word.matches("[^\\p{IsHiragana}]+\\s*")){
                changedList.add(word);
                kanjKatCandidates.clear();
            }
            else {
                last = i == wordList.length - 1;
                Rom2hira.FourValues FVword = getMatch(word, last, chatInput);
                changedList.add(FVword.getWritten());
            }
        }
        List<String> sublistPrevJPList = prevJPList.subList(0, startIndex);
        String[] notChanged = sublistPrevJPList.toArray(new String[0]);
        ArrayList<String> combindedList = new ArrayList<>(Arrays.asList(notChanged));
        combindedList.addAll(changedList);

        prevHiraList = wordListList;
        prevJPList = combindedList;
        return String.join("",combindedList);
    }

    private Rom2hira.FourValues getMatch(String word, boolean last, boolean chatInput){//the last word in wordList
        Rom2hira.FourValues FVofWord = new Rom2hira.FourValues(word.trim().replaceAll("\\d",""),word.trim().replaceAll("\\d",""),notAvailable,-1);
        List<Rom2hira.FourValues> newCandidates = new ArrayList<>();
        newCandidates.add(FVofWord);
        if (word.matches("[\\p{IsAlphabetic}\\p{IsHiragana}\\p{IsKatakana}]+\\d+$") ||//if theres a number at the end of strings, set candidate to that
                word.matches("[\\p{IsAlphabetic}\\p{IsHiragana}\\p{IsKatakana}]+ +$")){// if theres 1 or more space, #space - 1 = candidateN
            //show no candidates
            int candidateSelectionN;
            String wordPart;
            if (word.matches("[\\p{IsAlphabetic}\\p{IsHiragana}\\p{IsKatakana}]+\\d+$")) {
                String intPart = word.split("\\D+")[1].trim();
                candidateSelectionN = Integer.parseInt(intPart);
                wordPart = word.split("\\d+\\s*$")[0];
            } else {
                String spacePart = word.split("[\\p{IsAlphabetic}\\p{IsHiragana}\\p{IsKatakana}]+")[1];
                candidateSelectionN = spacePart.length() - 1;
                wordPart = word.split(" +$")[0];
            }
            if (chatInput)
                kanjKatCandidates.clear();
            newCandidates.addAll(getCandidates(wordPart));

            if (newCandidates.size() -1< candidateSelectionN) //if the selection is too large, return the last cand
                candidateSelectionN = newCandidates.size() - 1;

            if (last) {
                if (chatInput)
                    instCandidateSelection = candidateSelectionN;
                for (Rom2hira.FourValues fv : newCandidates)
                    if (chatInput)
                        kanjKatCandidates.add(fv.getWritten());
            } else if (chatInput)
                instCandidateSelection = -1;
            return newCandidates.get(candidateSelectionN);

        } else { //no japanese + number nor space at the end, add it as hiragana if its not the last word, if last word then look for candidates
            if (last) {//if its the last word on wordList, update candidates shown by overlay
                if (chatInput)
                    instCandidateSelection = -1;
                newCandidates.addAll(getCandidates(word));
                if (chatInput)
                    kanjKatCandidates.clear();
                for (Rom2hira.FourValues fv : newCandidates)
                    if (chatInput)
                        kanjKatCandidates.add(fv.getWritten());
                if (chatInput)
                    instCandidateSelection = -1;
            }
            return FVofWord;
        }
    }
    private List<Rom2hira.FourValues> getCandidates(String word){
        List<Rom2hira.FourValues> matches;

        matches = getAllMatches(word, 50);//get all exact matches
        List<FourValues> newCandidates = new ArrayList<>(matches);

        if (newCandidates.size() < 30) {//if not many candidates, get matches that begin with the last wordPart
            // (the last wordPart might be in the middle of being typed
            matches = getAllBeginningWith(word,Math.min(30-10-newCandidates.size(),10));
            newCandidates.addAll(matches);
        }
        newCandidates.sort(new compareFV());
        if (newCandidates.size() < 40) {//if still not many candidates
            // (the last wordPart might be in the middle of being typed
            int nWordsToAdd = 40 - newCandidates.size();
            List<Rom2hira.FourValues> containedAndExtra;
            containedAndExtra = getAllContaining(word, nWordsToAdd); // get all words that is contained within "wordPart"
            newCandidates.addAll(containedAndExtra);
        }

        //add katakana candidate, as 3rd option if it has more than 1 kanji candidate, 2nd if no kanji candidate
        if (newCandidates.size() > 1) {
            String kata = hira2kata(word);
            Rom2hira.FourValues kataFV = new Rom2hira.FourValues(kata,kata,notAvailable,0);
            newCandidates.add(2,kataFV);
        } else {
            String kata = hira2kata(word);
            Rom2hira.FourValues kataFV = new Rom2hira.FourValues(kata,kata,notAvailable,0);
            newCandidates.add(0,kataFV);
        }
        return  newCandidates;
    }
    private int searchFirstDif(String[] wordList){
        int upTo = 0;
        if (wordList.length > prevHiraList.size())
            upTo = prevHiraList.size();
        else
            upTo = wordList.length;
        for (int i = 0; i < upTo; i++)
            if (!Objects.equals(wordList[i], prevHiraList.get(i)))
                return i;
        return (upTo == 0 ? 0 : upTo-1);
    }
    public static <T> boolean compareLists(List<T> list1, List<T> list2) {
        // If lists have different sizes, they are not equal
        if (list1.size() != list2.size()) {
            return false;
        }

        // Compare elements of the lists
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }

        // If all elements match, lists are equal
        return true;
    }
    private List<Rom2hira.FourValues> getAllMatches(String kata, int nToAdd) {
        List<Rom2hira.FourValues> matches = new ArrayList<>();
        int count = 0;
        for (Rom2hira.FourValues entry: japCharDS) {
            if (count > nToAdd)
                break;
            if (entry.getRead().equals(kata)){
                if (matches.isEmpty()) {
                    count++;
                    matches.add(entry);
                }
                else if (matches.get(matches.size() - 1).getRank() + 9 < entry.getRank()) {//dont add if the word is only different tense of the previous
                    count++;
                    matches.add(entry);
                }
            }
        }
        return matches;
    }
    private List<Rom2hira.FourValues> getAllBeginningWith(String kata, int nToAdd) {
        List<Rom2hira.FourValues> matches = new ArrayList<>();
        int count = 0;
        for (Rom2hira.FourValues entry: japCharDS) {
            if (count > nToAdd)
                break;
            if (entry.getRead().startsWith(kata) && !entry.getRead().equals(kata)) {
                count++;
                matches.add(entry);
            }
        }
        return matches;
    }
    private List<Rom2hira.FourValues> getAllContaining(String kata, int nToAdd) {//returns a substring 0:x that is in japCharDS, and add it to substring x:
        List<Rom2hira.FourValues> matches = new ArrayList<>();
        int count = 0;
        for (int i = kata.length() - 1; i >= 0 && count < nToAdd; i--) {
            String substring = kata.substring(0,i);
            for (int j = 0; j < japCharDS.size(); j++) {
                Rom2hira.FourValues entry = japCharDS.get(j);
                if (entry.getRead().equals(substring)) {
                    Rom2hira.FourValues addingFV = new Rom2hira.FourValues(entry.getWritten() + kata.substring(i),
                            entry.getWritten() + kata.substring(i), notAvailable, entry.getRank());
                    if (matches.isEmpty()) {
                        matches.add(addingFV);
                        count++;
                    } else if (matches.get(matches.size() - 1).getRank() + 3 < addingFV.getRank()) {//only add few of the word is only different tense of the previous
                        count++;
                        if (count > nToAdd)
                            break;
                        matches.add(addingFV);
                    }
                }
            }
        }
        return matches;
    }
    private String[] getWakatiGaki (String text) {
        // Regular expression pattern for splitting
        String regexPattern =
                "(を+)|" + "([^\\p{IsAlphabetic}]+[a-zA-Z]+)|" +
                        "([a-zA-Z]+[^\\p{IsAlphabetic}]+)|" +
                        "([\\p{IsHiragana}\\p{IsKatakana}ー]*\\d+)|" +
                        "([\\p{IsHiragana}\\p{IsKatakana}ー]+ +)|" +
                        "([\\p{IsHiragana}\\p{IsKatakana}ー]+)|"+
                        "(\\s*[\\d]+\\s*)|" +
                        "([^\\p{IsHiragana}\\p{IsKatakana}ー\\d ]+)";

        Pattern patternCompiled = Pattern.compile(regexPattern);
        Matcher matcher = patternCompiled.matcher(text);
        List<String> segmentsAndPunctuation = new ArrayList<>();

        // Find and collect all matches
        while (matcher.find()) {
            String segment = matcher.group(0);
            if (segment != null) {
                segmentsAndPunctuation.add(segment);
            }
        }


        // Convert the list to an array
        return segmentsAndPunctuation.toArray(new String[0]);
    }



    private String hira2kata(String hira) {
        StringBuilder kata = new StringBuilder();
        for (int i = 0; i < hira.length(); i++) {
            char c = hira.charAt(i);
            if (c >= 0x3041 && c <= 0x3096) {
                kata.append((char) (c + 0x60));
            } else {
                kata.append(c);
            }
        }
        return kata.toString();
    }

}
