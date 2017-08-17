package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MusicFolders", propOrder = {
        "musicFolder"
})
public class MusicFolders {

    protected List<MusicFolder> musicFolder;

    public List<MusicFolder> getMusicFolder() {
        if (musicFolder == null) {
            musicFolder = new ArrayList<>();
        }
        return this.musicFolder;
    }

}
