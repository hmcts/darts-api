//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.3 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package uk.gov.hmcts.darts.task.runner.dailylist.govtalk.people.addressandpersonaldetails;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TelephoneStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="TelephoneStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="TelNationalNumber" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}TelephoneNumberType"/>
 *         <element name="TelExtensionNumber" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}TelephoneExtensionType" minOccurs="0"/>
 *         <element name="TelCountryCode" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}TelCountryCodeType" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="TelUse" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}WorkHomeType" />
 *       <attribute name="TelMobile" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}YesNoType" />
 *       <attribute name="TelPreferred" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}YesNoType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TelephoneStructure", propOrder = {
    "telNationalNumber",
    "telExtensionNumber",
    "telCountryCode"
})
public class TelephoneStructure {

    @XmlElement(name = "TelNationalNumber", required = true)
    protected String telNationalNumber;
    @XmlElement(name = "TelExtensionNumber")
    protected String telExtensionNumber;
    @XmlElement(name = "TelCountryCode")
    protected String telCountryCode;
    @XmlAttribute(name = "TelUse")
    protected WorkHomeType telUse;
    @XmlAttribute(name = "TelMobile")
    protected YesNoType telMobile;
    @XmlAttribute(name = "TelPreferred")
    protected YesNoType telPreferred;

    /**
     * Gets the value of the telNationalNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTelNationalNumber() {
        return telNationalNumber;
    }

    /**
     * Sets the value of the telNationalNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTelNationalNumber(String value) {
        this.telNationalNumber = value;
    }

    /**
     * Gets the value of the telExtensionNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTelExtensionNumber() {
        return telExtensionNumber;
    }

    /**
     * Sets the value of the telExtensionNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTelExtensionNumber(String value) {
        this.telExtensionNumber = value;
    }

    /**
     * Gets the value of the telCountryCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTelCountryCode() {
        return telCountryCode;
    }

    /**
     * Sets the value of the telCountryCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTelCountryCode(String value) {
        this.telCountryCode = value;
    }

    /**
     * Gets the value of the telUse property.
     * 
     * @return
     *     possible object is
     *     {@link WorkHomeType }
     *     
     */
    public WorkHomeType getTelUse() {
        return telUse;
    }

    /**
     * Sets the value of the telUse property.
     * 
     * @param value
     *     allowed object is
     *     {@link WorkHomeType }
     *     
     */
    public void setTelUse(WorkHomeType value) {
        this.telUse = value;
    }

    /**
     * Gets the value of the telMobile property.
     * 
     * @return
     *     possible object is
     *     {@link YesNoType }
     *     
     */
    public YesNoType getTelMobile() {
        return telMobile;
    }

    /**
     * Sets the value of the telMobile property.
     * 
     * @param value
     *     allowed object is
     *     {@link YesNoType }
     *     
     */
    public void setTelMobile(YesNoType value) {
        this.telMobile = value;
    }

    /**
     * Gets the value of the telPreferred property.
     * 
     * @return
     *     possible object is
     *     {@link YesNoType }
     *     
     */
    public YesNoType getTelPreferred() {
        return telPreferred;
    }

    /**
     * Sets the value of the telPreferred property.
     * 
     * @param value
     *     allowed object is
     *     {@link YesNoType }
     *     
     */
    public void setTelPreferred(YesNoType value) {
        this.telPreferred = value;
    }

}
