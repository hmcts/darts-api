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
 * <p>Java class for WorkHomeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="WorkHomeType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="work"/>
 *     <enumeration value="home"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "WorkHomeType")
@XmlEnum
public enum WorkHomeType {

    @XmlEnumValue("work")
    WORK("work"),
    @XmlEnumValue("home")
    HOME("home");
    private final String value;

    WorkHomeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static WorkHomeType fromValue(String v) {
        for (WorkHomeType c: WorkHomeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}