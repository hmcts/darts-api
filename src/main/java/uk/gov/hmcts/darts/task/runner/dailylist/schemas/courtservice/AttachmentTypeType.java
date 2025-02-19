//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.3 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AttachmentTypeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="AttachmentTypeType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="previousConvictionList"/>
 *     <enumeration value="preSentenceReport"/>
 *     <enumeration value="medicalReport"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "AttachmentTypeType")
@XmlEnum
public enum AttachmentTypeType {

    @XmlEnumValue("previousConvictionList")
    PREVIOUS_CONVICTION_LIST("previousConvictionList"),
    @XmlEnumValue("preSentenceReport")
    PRE_SENTENCE_REPORT("preSentenceReport"),
    @XmlEnumValue("medicalReport")
    MEDICAL_REPORT("medicalReport");
    private final String value;

    AttachmentTypeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AttachmentTypeType fromValue(String v) {
        for (AttachmentTypeType c: AttachmentTypeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
