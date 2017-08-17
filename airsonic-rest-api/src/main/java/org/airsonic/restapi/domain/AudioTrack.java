package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AudioTrack")
public class AudioTrack {

    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "languageCode")
    protected String languageCode;

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String value) {
        this.languageCode = value;
    }

}
