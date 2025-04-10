//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.3
// See https://eclipse-ee4j.github.io/jaxb-ri
// Any modifications to this file will be lost upon recompilation of the source schema.
//


package uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for ProsecutionStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="ProsecutionStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Advocate" type="{http://www.courtservice.gov.uk/schemas/courtservice}AdvocateStructure" maxOccurs="unbounded" minOccurs="0"/>
 *         <element name="ProsecutingReference" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="ProsecutingOrganisation" type="{http://www.courtservice.gov.uk/schemas/courtservice}OrganisationStructure" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="ProsecutingAuthority" type="{http://www.courtservice.gov.uk/schemas/courtservice}ProsecutingAuthorityType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProsecutionStructure", propOrder = {
    "advocate",
    "prosecutingReference",
    "prosecutingOrganisation"
})
public class ProsecutionStructure {

    @XmlElement(name = "Advocate")
    protected List<AdvocateStructure> advocate;
    @XmlElement(name = "ProsecutingReference")
    protected String prosecutingReference;
    @XmlElement(name = "ProsecutingOrganisation")
    protected OrganisationStructure prosecutingOrganisation;
    @XmlAttribute(name = "ProsecutingAuthority")
    protected ProsecutingAuthorityType prosecutingAuthority;

    /**
     * Gets the value of the advocate property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the advocate property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAdvocate().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AdvocateStructure }
     *
     *
     * @return
     *     The value of the advocate property.
     */
    public List<AdvocateStructure> getAdvocate() {
        if (advocate == null) {
            advocate = new ArrayList<>();
        }
        return this.advocate;
    }

    /**
     * Gets the value of the prosecutingReference property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getProsecutingReference() {
        return prosecutingReference;
    }

    /**
     * Sets the value of the prosecutingReference property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setProsecutingReference(String value) {
        this.prosecutingReference = value;
    }

    /**
     * Gets the value of the prosecutingOrganisation property.
     *
     * @return
     *     possible object is
     *     {@link OrganisationStructure }
     *
     */
    public OrganisationStructure getProsecutingOrganisation() {
        return prosecutingOrganisation;
    }

    /**
     * Sets the value of the prosecutingOrganisation property.
     *
     * @param value
     *     allowed object is
     *     {@link OrganisationStructure }
     *
     */
    public void setProsecutingOrganisation(OrganisationStructure value) {
        this.prosecutingOrganisation = value;
    }

    /**
     * Gets the value of the prosecutingAuthority property.
     *
     * @return
     *     possible object is
     *     {@link ProsecutingAuthorityType }
     *
     */
    public ProsecutingAuthorityType getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    /**
     * Sets the value of the prosecutingAuthority property.
     *
     * @param value
     *     allowed object is
     *     {@link ProsecutingAuthorityType }
     *
     */
    public void setProsecutingAuthority(ProsecutingAuthorityType value) {
        this.prosecutingAuthority = value;
    }

}
