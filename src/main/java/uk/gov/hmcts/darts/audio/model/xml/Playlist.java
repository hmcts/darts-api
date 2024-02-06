package uk.gov.hmcts.darts.audio.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ViqPlaylist", propOrder = {
      "playlistVersion",
      "items"
})
@XmlRootElement(name = "playlist")
public class Playlist implements Serializable {

    @Serial
    private static final long serialVersionUID = -1L;
    @XmlElement(name = "playlistversion", required = true, defaultValue = "1.0")
    protected String playlistVersion = "1.0";
    @XmlElement(name = "item")
    protected List<ViqPlayListItem> items;

    public String getPlaylistVersion() {
        return playlistVersion;
    }

    public void setPlaylistVersion(String value) {
        this.playlistVersion = value;
    }

    public List<ViqPlayListItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return this.items;
    }

}
