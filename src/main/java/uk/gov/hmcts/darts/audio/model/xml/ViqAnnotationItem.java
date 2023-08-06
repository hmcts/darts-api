package uk.gov.hmcts.darts.audio.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ViqAnnotationItem", propOrder = {
    "eventName"
})
@Getter
@Setter
public class ViqAnnotationItem implements Serializable {

    private static final String DEFAULT_ANNOTATION_LABEL = "operator";
    private static final String DEFAULT_ANNOTATION_RESTRICTION = "0";

    @Serial
    private static final long serialVersionUID = -1L;
    @XmlValue
    protected String eventName;
    @XmlAttribute(name = "L", required = true)
    protected String label;
    @XmlAttribute(name = "N", required = true)
    protected String eventText;
    @XmlAttribute(name = "T", required = true)
    protected String startTimeInMillis;
    @XmlAttribute(name = "Y", required = true)
    protected String startTimeYear;
    @XmlAttribute(name = "M", required = true)
    protected String startTimeMonth;
    @XmlAttribute(name = "D", required = true)
    protected String startTimeDate;
    @XmlAttribute(name = "H", required = true)
    protected String startTimeHour;
    @XmlAttribute(name = "MIN", required = true)
    protected String startTimeMinutes;
    @XmlAttribute(name = "S", required = true)
    protected String startTimeSeconds;
    @XmlAttribute(name = "R", required = true)
    protected String restricted;
    @XmlAttribute(name = "P", required = true)
    protected String lapsed;
    @XmlTransient
    protected String elementName;

    public ViqAnnotationItem() {
        this.label = DEFAULT_ANNOTATION_LABEL;
        this.restricted = DEFAULT_ANNOTATION_RESTRICTION;
    }

    public void setXmlElementName(String incrementedElementName) {
        this.elementName = incrementedElementName;
    }
}
