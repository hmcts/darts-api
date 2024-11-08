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
 * <p>Java class for ResidenceStatusType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="ResidenceStatusType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="resident"/>
 *     <enumeration value="non-resident"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "ResidenceStatusType")
@XmlEnum
public enum ResidenceStatusType {

    @XmlEnumValue("resident")
    RESIDENT("resident"),
    @XmlEnumValue("non-resident")
    NON_RESIDENT("non-resident");
    private final String value;

    ResidenceStatusType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ResidenceStatusType fromValue(String v) {
        for (ResidenceStatusType c: ResidenceStatusType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}