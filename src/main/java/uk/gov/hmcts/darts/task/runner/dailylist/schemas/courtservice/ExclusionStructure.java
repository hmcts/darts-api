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


/**
 * <p>Java class for ExclusionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="ExclusionStructure">
 *   <complexContent>
 *     <extension base="{http://www.courtservice.gov.uk/schemas/courtservice}BaseOrderRequirementStructure">
 *       <sequence>
 *         <element name="Location" type="{http://www.courtservice.gov.uk/schemas/courtservice}LocationStructure"/>
 *         <element name="Between" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       </sequence>
 *     </extension>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExclusionStructure", propOrder = {
    "location",
    "between"
})
public class ExclusionStructure
    extends BaseOrderRequirementStructure
{

    @XmlElement(name = "Location", required = true)
    protected LocationStructure location;
    @XmlElement(name = "Between")
    protected String between;

    /**
     * Gets the value of the location property.
     * 
     * @return
     *     possible object is
     *     {@link LocationStructure }
     *     
     */
    public LocationStructure getLocation() {
        return location;
    }

    /**
     * Sets the value of the location property.
     * 
     * @param value
     *     allowed object is
     *     {@link LocationStructure }
     *     
     */
    public void setLocation(LocationStructure value) {
        this.location = value;
    }

    /**
     * Gets the value of the between property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBetween() {
        return between;
    }

    /**
     * Sets the value of the between property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBetween(String value) {
        this.between = value;
    }

}
