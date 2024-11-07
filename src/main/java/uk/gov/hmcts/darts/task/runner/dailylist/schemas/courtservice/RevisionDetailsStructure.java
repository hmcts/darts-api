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
 * <p>Java class for RevisionDetailsStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="RevisionDetailsStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="PreviousOrderDate" type="{http://www.w3.org/2001/XMLSchema}date"/>
 *         <element name="RevisionNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RevisionDetailsStructure", propOrder = {
    "previousOrderDate",
    "revisionNumber"
})
public class RevisionDetailsStructure {

    @XmlElement(name = "PreviousOrderDate", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar previousOrderDate;
    @XmlElement(name = "RevisionNumber")
    protected int revisionNumber;

    /**
     * Gets the value of the previousOrderDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getPreviousOrderDate() {
        return previousOrderDate;
    }

    /**
     * Sets the value of the previousOrderDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setPreviousOrderDate(XMLGregorianCalendar value) {
        this.previousOrderDate = value;
    }

    /**
     * Gets the value of the revisionNumber property.
     *
     */
    public int getRevisionNumber() {
        return revisionNumber;
    }

    /**
     * Sets the value of the revisionNumber property.
     *
     */
    public void setRevisionNumber(int value) {
        this.revisionNumber = value;
    }

}
