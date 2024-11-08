//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.3
// See https://eclipse-ee4j.github.io/jaxb-ri
// Any modifications to this file will be lost upon recompilation of the source schema.
//


package uk.gov.hmcts.darts.task.runner.dailylist.govtalk.people.addressandpersonaldetails;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for CitizenNameStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="CitizenNameStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="CitizenNameTitle" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}CitizenNameTitleType" maxOccurs="unbounded" minOccurs="0"/>
 *         <element name="CitizenNameForename" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}CitizenNameForenameType" maxOccurs="unbounded" minOccurs="0"/>
 *         <element name="CitizenNameSurname" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}CitizenNameSurnameType"/>
 *         <element name="CitizenNameSuffix" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}CitizenNameSuffixType" maxOccurs="unbounded" minOccurs="0"/>
 *         <element name="CitizenNameRequestedName" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}CitizenNameRequestedNameType" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CitizenNameStructure", propOrder = {
    "citizenNameTitle",
    "citizenNameForename",
    "citizenNameSurname",
    "citizenNameSuffix",
    "citizenNameRequestedName"
})
@XmlSeeAlso({
    uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.JudiciaryStructure.Judge.class,
    uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.JudiciaryStructure.Justice.class
})
public class CitizenNameStructure {

    @XmlElement(name = "CitizenNameTitle")
    protected List<String> citizenNameTitle;
    @XmlElement(name = "CitizenNameForename")
    protected List<String> citizenNameForename;
    @XmlElement(name = "CitizenNameSurname", required = true)
    protected String citizenNameSurname;
    @XmlElement(name = "CitizenNameSuffix")
    protected List<String> citizenNameSuffix;
    @XmlElement(name = "CitizenNameRequestedName")
    protected String citizenNameRequestedName;

    /**
     * Gets the value of the citizenNameTitle property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the citizenNameTitle property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCitizenNameTitle().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     * @return
     *     The value of the citizenNameTitle property.
     */
    public List<String> getCitizenNameTitle() {
        if (citizenNameTitle == null) {
            citizenNameTitle = new ArrayList<>();
        }
        return this.citizenNameTitle;
    }

    /**
     * Gets the value of the citizenNameForename property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the citizenNameForename property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCitizenNameForename().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     * @return
     *     The value of the citizenNameForename property.
     */
    public List<String> getCitizenNameForename() {
        if (citizenNameForename == null) {
            citizenNameForename = new ArrayList<>();
        }
        return this.citizenNameForename;
    }

    /**
     * Gets the value of the citizenNameSurname property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCitizenNameSurname() {
        return citizenNameSurname;
    }

    /**
     * Sets the value of the citizenNameSurname property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCitizenNameSurname(String value) {
        this.citizenNameSurname = value;
    }

    /**
     * Gets the value of the citizenNameSuffix property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the citizenNameSuffix property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCitizenNameSuffix().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     * @return
     *     The value of the citizenNameSuffix property.
     */
    public List<String> getCitizenNameSuffix() {
        if (citizenNameSuffix == null) {
            citizenNameSuffix = new ArrayList<>();
        }
        return this.citizenNameSuffix;
    }

    /**
     * Gets the value of the citizenNameRequestedName property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCitizenNameRequestedName() {
        return citizenNameRequestedName;
    }

    /**
     * Sets the value of the citizenNameRequestedName property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCitizenNameRequestedName(String value) {
        this.citizenNameRequestedName = value;
    }

}
