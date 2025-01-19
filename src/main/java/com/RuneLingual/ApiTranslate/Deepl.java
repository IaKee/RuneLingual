package com.RuneLingual.ApiTranslate;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.TranslatingServiceSelectableList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

@Slf4j
public class Deepl {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private RuneLingualConfig config;
    private String deeplKey;
    @Getter @Setter
    private int deeplLimit = 500000;
    @Getter @Setter
    private int deeplCount = deeplLimit;
    @Getter @Setter
    private boolean keyValid = true;

    @Getter @Setter
    private PastTranslationManager deeplPastTranslationManager;


    // add texts that has already been attempted to be translated.
    // this avoids translating same texts multiple times when ran in a thread, which will waste limited or paid word count
    @Getter
    private List<String> translationAttempt = new ArrayList<>();

    @Inject
    public Deepl(RuneLingualPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        setUsageAndLimitInThread();
        deeplPastTranslationManager = new PastTranslationManager(this, plugin);
    }

    /**
     * Translates the given text from the source language to the target language.
     * Sets deeplCount the number of characters translated using the API
     *
     * @param text the text to be translated
     * @param sourceLang the source language
     * @param targetLang the target language
     * @return the translated text, or the original text if the translation fails
     */
    public String translate(String text, LangCodeSelectableList sourceLang, LangCodeSelectableList targetLang) {

        // if the text is already translated, return the past translation
        String pastTranslation = deeplPastTranslationManager.getPastTranslation(text);
        if (pastTranslation != null) {
            return pastTranslation;
        }

        // don't translate if text is empty, or has been attempted to translate
        if (text.isEmpty() || translationAttempt.contains(text)) {
            return text;
        }

        //if the character count is close to the limit, return the original text
        if(deeplCount > deeplLimit - text.length() - 1000){
            return text;
        }

        // from here, attempt to translate the text
        translationAttempt.add(text);
        deeplKey = plugin.getConfig().getAPIKey();

        String url = getTranslatorUrl();
        if(url.isEmpty()){// if selected service is not deepl, return as is
            return text;
        }

        String urlParameters = getUrlParameters(sourceLang, targetLang, text);
        String response = getResponse(url, urlParameters);
        if (response.isEmpty()) { // if response is empty, return as is
            setKeyValid(false);
            return text;
        }

        String translation = getTranslationInResponse(response);

        setKeyValid(true);
        setUsageAndLimit();
        // add the new translation to the past translations and its file
        deeplPastTranslationManager.addToPastTranslations(text, translation);

        return translation;
    }

    private String getUrlParameters(LangCodeSelectableList sourceLang, LangCodeSelectableList targetLang, String text) {

        String key = config.getAPIKey();
        String sourceLangCode = sourceLang.getDeeplLangCodeSource();
        String targetLangCode = targetLang.getDeeplLangCodeTarget();
        return "auth_key=" + key + "&text=" + text + "&source_lang=" + sourceLangCode + "&target_lang=" + targetLangCode;
    }

    private String getTranslatorUrl() {
        String baseUrl = getBaseUrl();
        if(baseUrl.isEmpty()){
            return "";
        }
        return baseUrl + "translate";
    }

    private String getBaseUrl() {
        if (Objects.equals(config.getApiServiceConfig().getServiceName(), TranslatingServiceSelectableList.DeepL.getServiceName())) {
            return "https://api-free.deepl.com/v2/";
        } else if (Objects.equals(config.getApiServiceConfig().getServiceName(), TranslatingServiceSelectableList.DeepL_PRO.getServiceName())) {
            return "https://api.deepl.com/v2/";
        } else {
            return "";
        }
    }

    private String getResponse(String url, String urlParameters) {
        try {
            URL deeplUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) deeplUrl.openConnection();
            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Enable input and output streams
            connection.setDoOutput(true);

            // Set the content type to indicate UTF-8 encoding
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            // Write the parameters to the request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = connection.getResponseCode();

            // If the response code is 200 (HTTP_OK), read the response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Use UTF-8 encoding when reading the response
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                setKeyValid(false);
                System.out.println("POST request not worked. Response Code: " + responseCode);
            }
        } catch (Exception e) {
            setKeyValid(false);
            e.printStackTrace();
        }
        setKeyValid(false);
        return "";
    }

    private String getTranslationInResponse(String response){
        JSONObject jsonObject = new JSONObject(response);
        JSONArray translationsArray = jsonObject.getJSONArray("translations");
        JSONObject firstTranslation = translationsArray.getJSONObject(0);
        return firstTranslation.getString("text");
    }

    // function to set usage of the API
    private void setUsageAndLimit() {
        String usage = getUsage();
        //log.info("usage: " + usage);
        if(usage.isEmpty()){
            return;
        }
        JSONObject jsonObject = new JSONObject(usage);
        deeplCount = jsonObject.getInt("character_count");
        deeplLimit = jsonObject.getInt("character_limit");
        //log.info("updated deepl count:" + deeplCount+"\nupdated deepl limit" + deeplLimit);
    }
    private String getUsage() {
        // URL of the DeepL API
        String url = getUsageUrl();
        if(url.isEmpty()){
            setKeyValid(false);
            return "";
        }
        String paramUrl = "auth_key=" + config.getAPIKey();
        return getResponse(url, paramUrl);
    }

    private String getUsageUrl() {
        String baseUrl = getBaseUrl();
        if (baseUrl.isEmpty()) {
            setKeyValid(false);
            return "";
        }
        return getBaseUrl() + "usage";
    }

    private void setUsageAndLimitInThread(){
        Thread thread = new Thread(this::setUsageAndLimit);
        thread.start();
    }
}
