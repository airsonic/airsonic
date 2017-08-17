package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InternetRadioStations", propOrder = {
        "internetRadioStation"
})
public class InternetRadioStations {

    protected List<InternetRadioStation> internetRadioStation;

    public List<InternetRadioStation> getInternetRadioStation() {
        if (internetRadioStation == null) {
            internetRadioStation = new ArrayList<>();
        }
        return this.internetRadioStation;
    }

}
