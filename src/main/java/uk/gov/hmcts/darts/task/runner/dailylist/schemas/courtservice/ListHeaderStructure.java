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
 * <p>Java class for ListHeaderStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="ListHeaderStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="ListCategory">
 *           <simpleType>
 *             <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               <enumeration value="Combined"/>
 *               <enumeration value="Criminal"/>
 *             </restriction>
 *           </simpleType>
 *         </element>
 *         <element name="StartDate" type="{http://www.w3.org/2001/XMLSchema}date"/>
 *         <element name="EndDate" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         <element name="Version" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="CRESTprintRef" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="PublishedTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         <element name="CRESTlistID" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ListHeaderStructure", propOrder = {
    "listCategory",
    "startDate",
    "endDate",
    "version",
    "cresTprintRef",
    "publishedTime",
    "cresTlistID"
})
public class ListHeaderStructure {

    @XmlElement(name = "ListCategory", required = true, defaultValue = "Criminal")
    protected String listCategory;
    @XmlElement(name = "StartDate", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar startDate;
    @XmlElement(name = "EndDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar endDate;
    @XmlElement(name = "Version", required = true)
    protected String version;
    @XmlElement(name = "CRESTprintRef")
    protected String cresTprintRef;
    @XmlElement(name = "PublishedTime", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar publishedTime;
    @XmlElement(name = "CRESTlistID")
    protected Integer cresTlistID;

    /**
     * Gets the value of the listCategory property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getListCategory() {
        return listCategory;
    }

    /**
     * Sets the value of the listCategory property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setListCategory(String value) {
        this.listCategory = value;
    }

    /**
     * Gets the value of the startDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getStartDate() {
        return startDate;
    }

    /**
     * Sets the value of the startDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setStartDate(XMLGregorianCalendar value) {
        this.startDate = value;
    }

    /**
     * Gets the value of the endDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getEndDate() {
        return endDate;
    }

    /**
     * Sets the value of the endDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setEndDate(XMLGregorianCalendar value) {
        this.endDate = value;
    }

    /**
     * Gets the value of the version property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Gets the value of the cresTprintRef property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCRESTprintRef() {
        return cresTprintRef;
    }

    /**
     * Sets the value of the cresTprintRef property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCRESTprintRef(String value) {
        this.cresTprintRef = value;
    }

    /**
     * Gets the value of the publishedTime property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getPublishedTime() {
        return publishedTime;
    }

    /**
     * Sets the value of the publishedTime property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setPublishedTime(XMLGregorianCalendar value) {
        this.publishedTime = value;
    }

    /**
     * Gets the value of the cresTlistID property.
     *
     * @return
     *     possible object is
     *     {@link Integer }
     *
     */
    public Integer getCRESTlistID() {
        return cresTlistID;
    }

    /**
     * Sets the value of the cresTlistID property.
     *
     * @param value
     *     allowed object is
     *     {@link Integer }
     *
     */
    public void setCRESTlistID(Integer value) {
        this.cresTlistID = value;
    }

}
