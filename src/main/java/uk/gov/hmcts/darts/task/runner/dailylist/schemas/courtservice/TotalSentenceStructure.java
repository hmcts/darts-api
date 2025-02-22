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

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for TotalSentenceStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="TotalSentenceStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Term" type="{http://www.courtservice.gov.uk/schemas/courtservice}TermStructure" minOccurs="0"/>
 *         <element name="OtherOrders" minOccurs="0">
 *           <complexType>
 *             <complexContent>
 *               <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 <sequence>
 *                   <element name="Order" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
 *                   <element name="CRESTOrderData" type="{http://www.courtservice.gov.uk/schemas/courtservice}CRESTDisposalDataStructure" maxOccurs="unbounded"/>
 *                 </sequence>
 *               </restriction>
 *             </complexContent>
 *           </complexType>
 *         </element>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TotalSentenceStructure", propOrder = {
    "term",
    "otherOrders"
})
public class TotalSentenceStructure {

    @XmlElement(name = "Term")
    protected TermStructure term;
    @XmlElement(name = "OtherOrders")
    protected OtherOrders otherOrders;

    /**
     * Gets the value of the term property.
     *
     * @return
     *     possible object is
     *     {@link TermStructure }
     *
     */
    public TermStructure getTerm() {
        return term;
    }

    /**
     * Sets the value of the term property.
     *
     * @param value
     *     allowed object is
     *     {@link TermStructure }
     *
     */
    public void setTerm(TermStructure value) {
        this.term = value;
    }

    /**
     * Gets the value of the otherOrders property.
     *
     * @return
     *     possible object is
     *     {@link OtherOrders }
     *
     */
    public OtherOrders getOtherOrders() {
        return otherOrders;
    }

    /**
     * Sets the value of the otherOrders property.
     *
     * @param value
     *     allowed object is
     *     {@link OtherOrders }
     *
     */
    public void setOtherOrders(OtherOrders value) {
        this.otherOrders = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     *
     * <p>The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>{@code
     * <complexType>
     *   <complexContent>
     *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       <sequence>
     *         <element name="Order" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
     *         <element name="CRESTOrderData" type="{http://www.courtservice.gov.uk/schemas/courtservice}CRESTDisposalDataStructure" maxOccurs="unbounded"/>
     *       </sequence>
     *     </restriction>
     *   </complexContent>
     * </complexType>
     * }</pre>
     *
     *
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "order",
        "crestOrderData"
    })
    public static class OtherOrders {

        @XmlElement(name = "Order", required = true)
        protected List<String> order;
        @XmlElement(name = "CRESTOrderData", required = true)
        protected List<CRESTDisposalDataStructure> crestOrderData;

        /**
         * Gets the value of the order property.
         *
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a {@code set} method for the order property.
         *
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getOrder().add(newItem);
         * </pre>
         *
         *
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         *
         *
         * @return
         *     The value of the order property.
         */
        public List<String> getOrder() {
            if (order == null) {
                order = new ArrayList<>();
            }
            return this.order;
        }

        /**
         * Gets the value of the crestOrderData property.
         *
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a {@code set} method for the crestOrderData property.
         *
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getCRESTOrderData().add(newItem);
         * </pre>
         *
         *
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link CRESTDisposalDataStructure }
         *
         *
         * @return
         *     The value of the crestOrderData property.
         */
        public List<CRESTDisposalDataStructure> getCRESTOrderData() {
            if (crestOrderData == null) {
                crestOrderData = new ArrayList<>();
            }
            return this.crestOrderData;
        }

    }

}
