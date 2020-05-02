package org.airsonic.player;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

public class MissingTranslations {

    public static void main(String[] args) throws IOException {
        String[] locales = {"da", "de", "es", "pt", "fi", "fr", "is", "it", "ja_JP", "mk", "nl", "no", "pl", "ru", "sl", "sv", "zh_CN", "zh_TW"};

        for (String locale : locales) {
            diff(locale, "en");
//            diff("en", locale);
        }
    }

    private static void diff(String locale1, String locale2) throws IOException {
        Properties en = new Properties();
        en.load(MissingTranslations.class.getResourceAsStream("/org/airsonic/player/i18n/ResourceBundle_" + locale1 + ".properties"));
        SortedMap<Object,Object> enSorted = new TreeMap<Object, Object>(en);

        Properties mk = new Properties();
        mk.load(MissingTranslations.class.getResourceAsStream("/org/airsonic/player/i18n/ResourceBundle_" + locale2 + ".properties"));

        System.out.println("\nMessages present in locale " + locale1 + " and missing in locale " + locale2 + ":");
        int count = 0;
        for (Map.Entry<Object, Object> entry : enSorted.entrySet()) {
            if (!mk.containsKey(entry.getKey())) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
                count++;
            }
        }

        System.out.println("\nTotal: " + count);
    }
}
