package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Shares", propOrder = {
        "share"
})
public class Shares {

    protected List<Share> share;

    public List<Share> getShare() {
        if (share == null) {
            share = new ArrayList<>();
        }
        return this.share;
    }

}
