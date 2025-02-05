//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.3 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import uk.gov.hmcts.darts.task.runner.dailylist.govtalk.people.addressandpersonaldetails.CitizenContactDetailsStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.govtalk.people.addressandpersonaldetails.UKPostalAddressStructure;


/**
 * <p>Java class for OrganisationStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="OrganisationStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="OrganisationCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="OrganisationName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="OrganisationAddress" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}UKPostalAddressStructure" minOccurs="0"/>
 *         <element name="OrganisationDX" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="ContactDetails" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}CitizenContactDetailsStructure" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OrganisationStructure", propOrder = {
    "organisationCode",
    "organisationName",
    "organisationAddress",
    "organisationDX",
    "contactDetails"
})
public class OrganisationStructure {

    @XmlElement(name = "OrganisationCode")
    protected String organisationCode;
    @XmlElement(name = "OrganisationName", required = true)
    protected String organisationName;
    @XmlElement(name = "OrganisationAddress")
    protected UKPostalAddressStructure organisationAddress;
    @XmlElement(name = "OrganisationDX")
    protected String organisationDX;
    @XmlElement(name = "ContactDetails")
    protected CitizenContactDetailsStructure contactDetails;

    /**
     * Gets the value of the organisationCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrganisationCode() {
        return organisationCode;
    }

    /**
     * Sets the value of the organisationCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrganisationCode(String value) {
        this.organisationCode = value;
    }

    /**
     * Gets the value of the organisationName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrganisationName() {
        return organisationName;
    }

    /**
     * Sets the value of the organisationName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrganisationName(String value) {
        this.organisationName = value;
    }

    /**
     * Gets the value of the organisationAddress property.
     * 
     * @return
     *     possible object is
     *     {@link UKPostalAddressStructure }
     *     
     */
    public UKPostalAddressStructure getOrganisationAddress() {
        return organisationAddress;
    }

    /**
     * Sets the value of the organisationAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link UKPostalAddressStructure }
     *     
     */
    public void setOrganisationAddress(UKPostalAddressStructure value) {
        this.organisationAddress = value;
    }

    /**
     * Gets the value of the organisationDX property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrganisationDX() {
        return organisationDX;
    }

    /**
     * Sets the value of the organisationDX property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrganisationDX(String value) {
        this.organisationDX = value;
    }

    /**
     * Gets the value of the contactDetails property.
     * 
     * @return
     *     possible object is
     *     {@link CitizenContactDetailsStructure }
     *     
     */
    public CitizenContactDetailsStructure getContactDetails() {
        return contactDetails;
    }

    /**
     * Sets the value of the contactDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link CitizenContactDetailsStructure }
     *     
     */
    public void setContactDetails(CitizenContactDetailsStructure value) {
        this.contactDetails = value;
    }

}
