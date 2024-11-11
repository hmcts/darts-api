//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.3 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package uk.gov.hmcts.darts.task.runner.dailylist.govtalk.people.addressandpersonaldetails;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SexType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="SexType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="unknown"/>
 *     <enumeration value="male"/>
 *     <enumeration value="female"/>
 *     <enumeration value="indeterminate"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "SexType")
@XmlEnum
public enum SexType {

    @XmlEnumValue("unknown")
    UNKNOWN("unknown"),
    @XmlEnumValue("male")
    MALE("male"),
    @XmlEnumValue("female")
    FEMALE("female"),
    @XmlEnumValue("indeterminate")
    INDETERMINATE("indeterminate");
    private final String value;

    SexType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SexType fromValue(String v) {
        for (SexType c: SexType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
