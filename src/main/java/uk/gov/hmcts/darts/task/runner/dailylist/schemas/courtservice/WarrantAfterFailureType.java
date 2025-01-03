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
 * <p>Java class for WarrantAfterFailureType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="WarrantAfterFailureType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="failureToComply"/>
 *     <enumeration value="failureToAttend"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "WarrantAfterFailureType")
@XmlEnum
public enum WarrantAfterFailureType {

    @XmlEnumValue("failureToComply")
    FAILURE_TO_COMPLY("failureToComply"),
    @XmlEnumValue("failureToAttend")
    FAILURE_TO_ATTEND("failureToAttend");
    private final String value;

    WarrantAfterFailureType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static WarrantAfterFailureType fromValue(String v) {
        for (WarrantAfterFailureType c: WarrantAfterFailureType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
