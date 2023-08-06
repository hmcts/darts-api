package uk.gov.hmcts.darts.audio.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.io.Serial;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ViqAnnotationMeta", propOrder = {
    "annotations"
})
@XmlRootElement(name = "cfMetaFile")
public class AnnotationMeta {
    @Serial
    private static final long serialVersionUID = -1L;
    @XmlElement(name = "annotations", required = true)
    protected Annotations annotations;

    public Annotations getAnnotations() {
        if (annotations == null) {
            annotations = new Annotations();
        }
        return annotations;
    }

    public void setAnnotations(Annotations annotations) {
        this.annotations = annotations;
    }
}
