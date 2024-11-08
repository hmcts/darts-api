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


/**
 * <p>Java class for OfficerStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="OfficerStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="OfficerName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
@XmlType(name = "OfficerStructure", propOrder = {
    "officerName",
    "contactDetails"
})
public class OfficerStructure {

    @XmlElement(name = "OfficerName")
    protected String officerName;
    @XmlElement(name = "ContactDetails")
    protected CitizenContactDetailsStructure contactDetails;

    /**
     * Gets the value of the officerName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOfficerName() {
        return officerName;
    }

    /**
     * Sets the value of the officerName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOfficerName(String value) {
        this.officerName = value;
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
