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
 * <p>Java class for MethodOfInstigationType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="MethodOfInstigationType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="Committal"/>
 *     <enumeration value="Sending"/>
 *     <enumeration value="Execution"/>
 *     <enumeration value="Transfer"/>
 *     <enumeration value="Transfer certificated"/>
 *     <enumeration value="Voluntary bill"/>
 *     <enumeration value="Rehearing ordered"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "MethodOfInstigationType")
@XmlEnum
public enum MethodOfInstigationType {

    @XmlEnumValue("Committal")
    COMMITTAL("Committal"),
    @XmlEnumValue("Sending")
    SENDING("Sending"),
    @XmlEnumValue("Execution")
    EXECUTION("Execution"),
    @XmlEnumValue("Transfer")
    TRANSFER("Transfer"),
    @XmlEnumValue("Transfer certificated")
    TRANSFER_CERTIFICATED("Transfer certificated"),
    @XmlEnumValue("Voluntary bill")
    VOLUNTARY_BILL("Voluntary bill"),
    @XmlEnumValue("Rehearing ordered")
    REHEARING_ORDERED("Rehearing ordered");
    private final String value;

    MethodOfInstigationType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MethodOfInstigationType fromValue(String v) {
        for (MethodOfInstigationType c: MethodOfInstigationType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
