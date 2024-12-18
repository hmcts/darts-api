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
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for OriginatingCourtStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="OriginatingCourtStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Date" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         <element name="CourtHouse" type="{http://www.courtservice.gov.uk/schemas/courtservice}CourtHouseStructure"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OriginatingCourtStructure", propOrder = {
    "date",
    "courtHouse"
})
@XmlSeeAlso({
    BreachStructure.OriginatingCourt.class,
    BreachStructure.BreachCourt.class
})
public class OriginatingCourtStructure {

    @XmlElement(name = "Date")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar date;
    @XmlElement(name = "CourtHouse", required = true)
    protected CourtHouseStructure courtHouse;

    /**
     * Gets the value of the date property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setDate(XMLGregorianCalendar value) {
        this.date = value;
    }

    /**
     * Gets the value of the courtHouse property.
     *
     * @return
     *     possible object is
     *     {@link CourtHouseStructure }
     *
     */
    public CourtHouseStructure getCourtHouse() {
        return courtHouse;
    }

    /**
     * Sets the value of the courtHouse property.
     *
     * @param value
     *     allowed object is
     *     {@link CourtHouseStructure }
     *
     */
    public void setCourtHouse(CourtHouseStructure value) {
        this.courtHouse = value;
    }

}
