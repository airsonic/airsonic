package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "MediaType")
@XmlEnum
public enum MediaType {

    @XmlEnumValue("music")
    MUSIC("music"),
    @XmlEnumValue("podcast")
    PODCAST("podcast"),
    @XmlEnumValue("audiobook")
    AUDIOBOOK("audiobook"),
    @XmlEnumValue("video")
    VIDEO("video");
    private final String value;

    MediaType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MediaType fromValue(String v) {
        for (MediaType c : MediaType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
