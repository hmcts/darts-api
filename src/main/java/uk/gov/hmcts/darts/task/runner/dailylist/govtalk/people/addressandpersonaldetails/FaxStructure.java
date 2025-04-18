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
 * <p>Java class for FaxStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="FaxStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="FaxNationalNumber" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}TelephoneNumberType"/>
 *         <element name="FaxExtensionNumber" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}TelephoneExtensionType" minOccurs="0"/>
 *         <element name="FaxCountryCode" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}TelCountryCodeType" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="FaxUse" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}WorkHomeType" />
 *       <attribute name="FaxMobile" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}YesNoType" />
 *       <attribute name="FaxPreferred" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}YesNoType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FaxStructure", propOrder = {
    "faxNationalNumber",
    "faxExtensionNumber",
    "faxCountryCode"
})
public class FaxStructure {

    @XmlElement(name = "FaxNationalNumber", required = true)
    protected String faxNationalNumber;
    @XmlElement(name = "FaxExtensionNumber")
    protected String faxExtensionNumber;
    @XmlElement(name = "FaxCountryCode")
    protected String faxCountryCode;
    @XmlAttribute(name = "FaxUse")
    protected WorkHomeType faxUse;
    @XmlAttribute(name = "FaxMobile")
    protected YesNoType faxMobile;
    @XmlAttribute(name = "FaxPreferred")
    protected YesNoType faxPreferred;

    /**
     * Gets the value of the faxNationalNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFaxNationalNumber() {
        return faxNationalNumber;
    }

    /**
     * Sets the value of the faxNationalNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFaxNationalNumber(String value) {
        this.faxNationalNumber = value;
    }

    /**
     * Gets the value of the faxExtensionNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFaxExtensionNumber() {
        return faxExtensionNumber;
    }

    /**
     * Sets the value of the faxExtensionNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFaxExtensionNumber(String value) {
        this.faxExtensionNumber = value;
    }

    /**
     * Gets the value of the faxCountryCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFaxCountryCode() {
        return faxCountryCode;
    }

    /**
     * Sets the value of the faxCountryCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFaxCountryCode(String value) {
        this.faxCountryCode = value;
    }

    /**
     * Gets the value of the faxUse property.
     * 
     * @return
     *     possible object is
     *     {@link WorkHomeType }
     *     
     */
    public WorkHomeType getFaxUse() {
        return faxUse;
    }

    /**
     * Sets the value of the faxUse property.
     * 
     * @param value
     *     allowed object is
     *     {@link WorkHomeType }
     *     
     */
    public void setFaxUse(WorkHomeType value) {
        this.faxUse = value;
    }

    /**
     * Gets the value of the faxMobile property.
     * 
     * @return
     *     possible object is
     *     {@link YesNoType }
     *     
     */
    public YesNoType getFaxMobile() {
        return faxMobile;
    }

    /**
     * Sets the value of the faxMobile property.
     * 
     * @param value
     *     allowed object is
     *     {@link YesNoType }
     *     
     */
    public void setFaxMobile(YesNoType value) {
        this.faxMobile = value;
    }

    /**
     * Gets the value of the faxPreferred property.
     * 
     * @return
     *     possible object is
     *     {@link YesNoType }
     *     
     */
    public YesNoType getFaxPreferred() {
        return faxPreferred;
    }

    /**
     * Sets the value of the faxPreferred property.
     * 
     * @param value
     *     allowed object is
     *     {@link YesNoType }
     *     
     */
    public void setFaxPreferred(YesNoType value) {
        this.faxPreferred = value;
    }

}
