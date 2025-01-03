//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.3
// See https://eclipse-ee4j.github.io/jaxb-ri
// Any modifications to this file will be lost upon recompilation of the source schema.
//


package uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for FixtureStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="FixtureStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="FixedDate" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         <element name="Notes" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="Cases" type="{http://www.courtservice.gov.uk/schemas/courtservice}CasesStructure"/>
 *         <element name="LinkedCases" type="{http://www.courtservice.gov.uk/schemas/courtservice}LinkedCasesStructure" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FixtureStructure", propOrder = {
    "fixedDate",
    "notes",
    "cases",
    "linkedCases"
})
public class FixtureStructure {

    @XmlElement(name = "FixedDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar fixedDate;
    @XmlElement(name = "Notes")
    protected String notes;
    @XmlElement(name = "Cases", required = true)
    protected CasesStructure cases;
    @XmlElement(name = "LinkedCases")
    protected LinkedCasesStructure linkedCases;

    /**
     * Gets the value of the fixedDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getFixedDate() {
        return fixedDate;
    }

    /**
     * Sets the value of the fixedDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setFixedDate(XMLGregorianCalendar value) {
        this.fixedDate = value;
    }

    /**
     * Gets the value of the notes property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets the value of the notes property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setNotes(String value) {
        this.notes = value;
    }

    /**
     * Gets the value of the cases property.
     *
     * @return
     *     possible object is
     *     {@link CasesStructure }
     *
     */
    public CasesStructure getCases() {
        return cases;
    }

    /**
     * Sets the value of the cases property.
     *
     * @param value
     *     allowed object is
     *     {@link CasesStructure }
     *
     */
    public void setCases(CasesStructure value) {
        this.cases = value;
    }

    /**
     * Gets the value of the linkedCases property.
     *
     * @return
     *     possible object is
     *     {@link LinkedCasesStructure }
     *
     */
    public LinkedCasesStructure getLinkedCases() {
        return linkedCases;
    }

    /**
     * Sets the value of the linkedCases property.
     *
     * @param value
     *     allowed object is
     *     {@link LinkedCasesStructure }
     *
     */
    public void setLinkedCases(LinkedCasesStructure value) {
        this.linkedCases = value;
    }

}
