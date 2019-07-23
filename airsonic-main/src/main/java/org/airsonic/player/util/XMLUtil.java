package org.airsonic.player.util;

import org.jdom2.input.SAXBuilder;

public class XMLUtil {

    public static SAXBuilder createSAXBuilder() {
        SAXBuilder builder = new SAXBuilder();
        builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return builder;
    }
}
