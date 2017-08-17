package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScanStatus")
public class ScanStatus {

    @XmlAttribute(name = "scanning", required = true)
    protected boolean scanning;
    @XmlAttribute(name = "count")
    protected Long count;

    public boolean isScanning() {
        return scanning;
    }

    public void setScanning(boolean value) {
        this.scanning = value;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long value) {
        this.count = value;
    }

}
