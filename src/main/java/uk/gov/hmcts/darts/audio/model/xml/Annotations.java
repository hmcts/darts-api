package uk.gov.hmcts.darts.audio.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ViqAnnotation", propOrder = {
    "annotationItems"
})
public class Annotations {

    private static final String DEFAULT_ANNOTATION_COUNT = "0";
    @Serial
    private static final long serialVersionUID = -1L;
    @XmlAttribute(name = "count")
    protected String eventCount;
    @XmlElement(name = "a")
    @XmlJavaTypeAdapter(IncrementingElementNameAdapter.class)
    protected List<ViqAnnotationItem> annotationItems;
    @XmlTransient
    private String xmlElementName;

    public Annotations() {
        this.eventCount = DEFAULT_ANNOTATION_COUNT;
    }

    public String getEventCount() {
        return eventCount;
    }

    public void setEventCount(String eventCount) {
        this.eventCount = eventCount;
    }

    public List<ViqAnnotationItem> getAnnotationItems() {
        if (annotationItems == null) {
            annotationItems = new ArrayList<>();
        }
        return annotationItems;
    }

    public void setAnnotationItems(List<ViqAnnotationItem> annotationItems) {
        this.annotationItems = annotationItems;
    }
}
