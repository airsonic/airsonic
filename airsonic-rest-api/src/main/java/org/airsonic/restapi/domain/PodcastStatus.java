package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "PodcastStatus")
@XmlEnum
public enum PodcastStatus {

    @XmlEnumValue("new")
    NEW("new"),
    @XmlEnumValue("downloading")
    DOWNLOADING("downloading"),
    @XmlEnumValue("completed")
    COMPLETED("completed"),
    @XmlEnumValue("error")
    ERROR("error"),
    @XmlEnumValue("deleted")
    DELETED("deleted"),
    @XmlEnumValue("skipped")
    SKIPPED("skipped");
    private final String value;

    PodcastStatus(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static PodcastStatus fromValue(String v) {
        for (PodcastStatus c : PodcastStatus.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
