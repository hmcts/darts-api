package uk.gov.hmcts.darts.audio.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ViqPlayListItem", propOrder = {
      "value"
})
@Getter
@Setter
@NoArgsConstructor
public class ViqPlayListItem implements Serializable {

    @Serial
    private static final long serialVersionUID = -1L;
    @XmlValue
    protected String value;
    @XmlAttribute(name = "N", required = true)
    protected String caseNumber;
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


}
