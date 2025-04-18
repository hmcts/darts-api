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
 * <p>Java class for VerifiedByType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="VerifiedByType">
 *   <restriction base="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}RestrictedStringType">
 *     <enumeration value="not verified"/>
 *     <enumeration value="accepted on balance of probabilities"/>
 *     <enumeration value="secondary certificate"/>
 *     <enumeration value="certified copy of birth certificate"/>
 *     <enumeration value="short form birth certificate or certificate of registration of birth"/>
 *     <enumeration value="birth certificate"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "VerifiedByType")
@XmlEnum
public enum VerifiedByType {

    @XmlEnumValue("not verified")
    NOT_VERIFIED("not verified"),
    @XmlEnumValue("accepted on balance of probabilities")
    ACCEPTED_ON_BALANCE_OF_PROBABILITIES("accepted on balance of probabilities"),
    @XmlEnumValue("secondary certificate")
    SECONDARY_CERTIFICATE("secondary certificate"),
    @XmlEnumValue("certified copy of birth certificate")
    CERTIFIED_COPY_OF_BIRTH_CERTIFICATE("certified copy of birth certificate"),
    @XmlEnumValue("short form birth certificate or certificate of registration of birth")
    SHORT_FORM_BIRTH_CERTIFICATE_OR_CERTIFICATE_OF_REGISTRATION_OF_BIRTH("short form birth certificate or certificate of registration of birth"),
    @XmlEnumValue("birth certificate")
    BIRTH_CERTIFICATE("birth certificate");
    private final String value;

    VerifiedByType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static VerifiedByType fromValue(String v) {
        for (VerifiedByType c: VerifiedByType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
