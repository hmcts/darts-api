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

import javax.xml.datatype.Duration;


/**
 * <p>Java class for CreditOnRemandStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="CreditOnRemandStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="CreditPeriod" type="{http://www.w3.org/2001/XMLSchema}duration"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CreditOnRemandStructure", propOrder = {
    "creditPeriod"
})
public class CreditOnRemandStructure {

    @XmlElement(name = "CreditPeriod", required = true)
    protected Duration creditPeriod;

    /**
     * Gets the value of the creditPeriod property.
     *
     * @return
     *     possible object is
     *     {@link Duration }
     *
     */
    public Duration getCreditPeriod() {
        return creditPeriod;
    }

    /**
     * Sets the value of the creditPeriod property.
     *
     * @param value
     *     allowed object is
     *     {@link Duration }
     *
     */
    public void setCreditPeriod(Duration value) {
        this.creditPeriod = value;
    }

}
