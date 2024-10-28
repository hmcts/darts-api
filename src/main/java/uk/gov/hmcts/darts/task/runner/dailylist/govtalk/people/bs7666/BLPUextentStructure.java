//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.3
// See https://eclipse-ee4j.github.io/jaxb-ri
// Any modifications to this file will be lost upon recompilation of the source schema.
//


package uk.gov.hmcts.darts.task.runner.dailylist.govtalk.people.bs7666;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for BLPUextentStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="BLPUextentStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="SourceDescription">
 *           <simpleType>
 *             <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               <minLength value="1"/>
 *               <maxLength value="50"/>
 *             </restriction>
 *           </simpleType>
 *         </element>
 *         <element name="ExtentEntryDate" type="{http://www.w3.org/2001/XMLSchema}date"/>
 *         <element name="ExtentSourceDate" type="{http://www.w3.org/2001/XMLSchema}date"/>
 *         <element name="ExtentStartDate" type="{http://www.w3.org/2001/XMLSchema}date"/>
 *         <element name="ExtentEndDate" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         <element name="ExtentLastUpdateDate" type="{http://www.w3.org/2001/XMLSchema}date"/>
 *         <element name="ExtentDefinition" type="{http://www.govtalk.gov.uk/people/bs7666}BLPUpolygonStructure" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BLPUextentStructure", propOrder = {
    "sourceDescription",
    "extentEntryDate",
    "extentSourceDate",
    "extentStartDate",
    "extentEndDate",
    "extentLastUpdateDate",
    "extentDefinition"
})
public class BLPUextentStructure {

    @XmlElement(name = "SourceDescription", required = true)
    protected String sourceDescription;
    @XmlElement(name = "ExtentEntryDate", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar extentEntryDate;
    @XmlElement(name = "ExtentSourceDate", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar extentSourceDate;
    @XmlElement(name = "ExtentStartDate", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar extentStartDate;
    @XmlElement(name = "ExtentEndDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar extentEndDate;
    @XmlElement(name = "ExtentLastUpdateDate", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar extentLastUpdateDate;
    @XmlElement(name = "ExtentDefinition", required = true)
    protected List<BLPUpolygonStructure> extentDefinition;

    /**
     * Gets the value of the sourceDescription property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSourceDescription() {
        return sourceDescription;
    }

    /**
     * Sets the value of the sourceDescription property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSourceDescription(String value) {
        this.sourceDescription = value;
    }

    /**
     * Gets the value of the extentEntryDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getExtentEntryDate() {
        return extentEntryDate;
    }

    /**
     * Sets the value of the extentEntryDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setExtentEntryDate(XMLGregorianCalendar value) {
        this.extentEntryDate = value;
    }

    /**
     * Gets the value of the extentSourceDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getExtentSourceDate() {
        return extentSourceDate;
    }

    /**
     * Sets the value of the extentSourceDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setExtentSourceDate(XMLGregorianCalendar value) {
        this.extentSourceDate = value;
    }

    /**
     * Gets the value of the extentStartDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getExtentStartDate() {
        return extentStartDate;
    }

    /**
     * Sets the value of the extentStartDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setExtentStartDate(XMLGregorianCalendar value) {
        this.extentStartDate = value;
    }

    /**
     * Gets the value of the extentEndDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getExtentEndDate() {
        return extentEndDate;
    }

    /**
     * Sets the value of the extentEndDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setExtentEndDate(XMLGregorianCalendar value) {
        this.extentEndDate = value;
    }

    /**
     * Gets the value of the extentLastUpdateDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getExtentLastUpdateDate() {
        return extentLastUpdateDate;
    }

    /**
     * Sets the value of the extentLastUpdateDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setExtentLastUpdateDate(XMLGregorianCalendar value) {
        this.extentLastUpdateDate = value;
    }

    /**
     * Gets the value of the extentDefinition property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the extentDefinition property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExtentDefinition().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BLPUpolygonStructure }
     *
     *
     * @return
     *     The value of the extentDefinition property.
     */
    public List<BLPUpolygonStructure> getExtentDefinition() {
        if (extentDefinition == null) {
            extentDefinition = new ArrayList<>();
        }
        return this.extentDefinition;
    }

}
