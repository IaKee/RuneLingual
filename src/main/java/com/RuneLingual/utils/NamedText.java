package com.RuneLingual.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class NamedText
{
    // default language code used from startup
    private static final String DEFAULT_LANGUAGE = "en";
    private static final Charset defaultCharSet = StandardCharsets.UTF_8;

    private static String currentLanguage = DEFAULT_LANGUAGE;
    private static Charset currentCharset = defaultCharSet;

    private static final Map<String, String> translations = new HashMap<>();
    private static final Gson GSON = new Gson();

    private static boolean ok = false;

    public void setLanguage(
        String newLanguage,
        Charset newCharSet)
    {
        currentLanguage = newLanguage;
        currentCharset = newCharSet;
        updateInternalTranslations();
    }

    public String getLanguage()
    {
        return currentLanguage;
    }

    public static String get(String key)
    {
        if(!ok)
        {
            log.warn("NamedText instance was not loaded correctly, returning key: {}", key);
            return key;
        }

        // returns corresponding named string
        return translations.getOrDefault(key, key);
    }

    public static void updateInternalTranslations()
    {
        // resets all loaded translations
        translations.clear();

        String path = "/lang/" + currentLanguage + ".json";

        try (InputStreamReader reader = new InputStreamReader(
            Objects.requireNonNull(NamedText.class.getResourceAsStream(path)),
            currentCharset))
        {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            translations.putAll(GSON.fromJson(reader, type));
            ok = true;
        }
        catch (Exception e)
        {
            log.error("Could not read translated texts from file: {}", e.getMessage());
            ok = false;
        }
    }

    static
    {
        updateInternalTranslations();
    }
}
