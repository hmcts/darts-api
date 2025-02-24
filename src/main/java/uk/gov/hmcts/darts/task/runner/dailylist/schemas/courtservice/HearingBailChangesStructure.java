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
 * <p>Java class for HearingBailChangesStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="HearingBailChangesStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="BailChangeDate" type="{http://www.w3.org/2001/XMLSchema}date"/>
 *         <choice>
 *           <element name="HearingBailResult">
 *             <simpleType>
 *               <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                 <enumeration value="Granted"/>
 *                 <enumeration value="Refused"/>
 *               </restriction>
 *             </simpleType>
 *           </element>
 *           <element name="NewBailStatus" type="{http://www.courtservice.gov.uk/schemas/courtservice}BailStatusType"/>
 *         </choice>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HearingBailChangesStructure", propOrder = {
    "bailChangeDate",
    "hearingBailResult",
    "newBailStatus"
})
public class HearingBailChangesStructure {

    @XmlElement(name = "BailChangeDate", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar bailChangeDate;
    @XmlElement(name = "HearingBailResult")
    protected String hearingBailResult;
    @XmlElement(name = "NewBailStatus")
    @XmlSchemaType(name = "string")
    protected BailStatusType newBailStatus;

    /**
     * Gets the value of the bailChangeDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getBailChangeDate() {
        return bailChangeDate;
    }

    /**
     * Sets the value of the bailChangeDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setBailChangeDate(XMLGregorianCalendar value) {
        this.bailChangeDate = value;
    }

    /**
     * Gets the value of the hearingBailResult property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getHearingBailResult() {
        return hearingBailResult;
    }

    /**
     * Sets the value of the hearingBailResult property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setHearingBailResult(String value) {
        this.hearingBailResult = value;
    }

    /**
     * Gets the value of the newBailStatus property.
     *
     * @return
     *     possible object is
     *     {@link BailStatusType }
     *
     */
    public BailStatusType getNewBailStatus() {
        return newBailStatus;
    }

    /**
     * Sets the value of the newBailStatus property.
     *
     * @param value
     *     allowed object is
     *     {@link BailStatusType }
     *
     */
    public void setNewBailStatus(BailStatusType value) {
        this.newBailStatus = value;
    }

}
