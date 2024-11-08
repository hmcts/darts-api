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
 * <p>Java class for AssociatedOrderType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="AssociatedOrderType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="communityOrder"/>
 *     <enumeration value="suspendedSentenceOrder"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "AssociatedOrderType")
@XmlEnum
public enum AssociatedOrderType {

    @XmlEnumValue("communityOrder")
    COMMUNITY_ORDER("communityOrder"),
    @XmlEnumValue("suspendedSentenceOrder")
    SUSPENDED_SENTENCE_ORDER("suspendedSentenceOrder");
    private final String value;

    AssociatedOrderType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AssociatedOrderType fromValue(String v) {
        for (AssociatedOrderType c: AssociatedOrderType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
