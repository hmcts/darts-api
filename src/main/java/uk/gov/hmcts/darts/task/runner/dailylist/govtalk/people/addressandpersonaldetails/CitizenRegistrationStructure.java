//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.3 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package uk.gov.hmcts.darts.task.runner.dailylist.govtalk.people.addressandpersonaldetails;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CitizenRegistrationStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="CitizenRegistrationStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="NationalInsuranceNumber" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}NationalInsuranceNumberType" minOccurs="0"/>
 *         <element name="UniqueTaxReference" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}UniqueTaxReferenceType" minOccurs="0"/>
 *         <element name="DrivingLicenceNumber" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}DrivingLicenceNumberType" minOccurs="0"/>
 *         <element name="NHSNumber" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}NHSnumberStructure" minOccurs="0"/>
 *         <choice minOccurs="0">
 *           <element name="PassportNumber_Old" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}PassportNumber_OldType"/>
 *           <element name="PassportNumber_New" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}PassportNumber_NewType"/>
 *         </choice>
 *         <element name="ElectoralRollNumber" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}ElectoralRollNumberType" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CitizenRegistrationStructure", propOrder = {
    "nationalInsuranceNumber",
    "uniqueTaxReference",
    "drivingLicenceNumber",
    "nhsNumber",
    "passportNumberOld",
    "passportNumberNew",
    "electoralRollNumber"
})
public class CitizenRegistrationStructure {

    @XmlElement(name = "NationalInsuranceNumber")
    protected String nationalInsuranceNumber;
    @XmlElement(name = "UniqueTaxReference")
    protected String uniqueTaxReference;
    @XmlElement(name = "DrivingLicenceNumber")
    protected String drivingLicenceNumber;
    @XmlElement(name = "NHSNumber")
    protected NHSnumberStructure nhsNumber;
    @XmlElement(name = "PassportNumber_Old")
    protected String passportNumberOld;
    @XmlElement(name = "PassportNumber_New")
    protected Integer passportNumberNew;
    @XmlElement(name = "ElectoralRollNumber")
    protected String electoralRollNumber;

    /**
     * Gets the value of the nationalInsuranceNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    /**
     * Sets the value of the nationalInsuranceNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNationalInsuranceNumber(String value) {
        this.nationalInsuranceNumber = value;
    }

    /**
     * Gets the value of the uniqueTaxReference property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUniqueTaxReference() {
        return uniqueTaxReference;
    }

    /**
     * Sets the value of the uniqueTaxReference property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUniqueTaxReference(String value) {
        this.uniqueTaxReference = value;
    }

    /**
     * Gets the value of the drivingLicenceNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDrivingLicenceNumber() {
        return drivingLicenceNumber;
    }

    /**
     * Sets the value of the drivingLicenceNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDrivingLicenceNumber(String value) {
        this.drivingLicenceNumber = value;
    }

    /**
     * Gets the value of the nhsNumber property.
     * 
     * @return
     *     possible object is
     *     {@link NHSnumberStructure }
     *     
     */
    public NHSnumberStructure getNHSNumber() {
        return nhsNumber;
    }

    /**
     * Sets the value of the nhsNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link NHSnumberStructure }
     *     
     */
    public void setNHSNumber(NHSnumberStructure value) {
        this.nhsNumber = value;
    }

    /**
     * Gets the value of the passportNumberOld property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassportNumberOld() {
        return passportNumberOld;
    }

    /**
     * Sets the value of the passportNumberOld property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassportNumberOld(String value) {
        this.passportNumberOld = value;
    }

    /**
     * Gets the value of the passportNumberNew property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPassportNumberNew() {
        return passportNumberNew;
    }

    /**
     * Sets the value of the passportNumberNew property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPassportNumberNew(Integer value) {
        this.passportNumberNew = value;
    }

    /**
     * Gets the value of the electoralRollNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getElectoralRollNumber() {
        return electoralRollNumber;
    }

    /**
     * Sets the value of the electoralRollNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setElectoralRollNumber(String value) {
        this.electoralRollNumber = value;
    }

}
