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
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import uk.gov.hmcts.darts.task.runner.dailylist.govtalk.people.addressandpersonaldetails.UKPostalAddressStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.govtalk.people.addressandpersonaldetails.YesNoType;

import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for ChargeStructure complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType name="ChargeStructure">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="CRN" type="{http://www.courtservice.gov.uk/schemas/courtservice}CRnumberType" minOccurs="0"/>
 *         <element name="CRESTchargeID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="CaseNumber" type="{http://www.courtservice.gov.uk/schemas/courtservice}CaseNumberType" minOccurs="0"/>
 *         <element name="OffenceStatement" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="OffenceLocation" type="{http://www.govtalk.gov.uk/people/AddressAndPersonalDetails}UKPostalAddressStructure" minOccurs="0"/>
 *         <element name="ArrestingPoliceForceCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="CommittedOnBail" type="{http://www.courtservice.gov.uk/schemas/courtservice}YesNoType" minOccurs="0"/>
 *         <element name="OffenceStartDateTime" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         <element name="OffenceEndDateTime" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         <element name="ArraignmentDate" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         <element name="ConvictionDate" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         <element name="OffenceParticulars" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="CRESToffenceNumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="Plea" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="Verdict" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="Disposals" minOccurs="0">
 *           <complexType>
 *             <complexContent>
 *               <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 <sequence>
 *                   <element name="Disposal" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
 *                   <element name="CRESTDisposalData" type="{http://www.courtservice.gov.uk/schemas/courtservice}CRESTDisposalDataStructure" maxOccurs="unbounded" minOccurs="0"/>
 *                 </sequence>
 *               </restriction>
 *             </complexContent>
 *           </complexType>
 *         </element>
 *         <element name="SentenceTerm" type="{http://www.w3.org/2001/XMLSchema}duration" minOccurs="0"/>
 *         <element name="TermType" type="{http://www.courtservice.gov.uk/schemas/courtservice}TermTypeType" minOccurs="0"/>
 *         <element name="ForLife" type="{http://www.courtservice.gov.uk/schemas/courtservice}YesNoType" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="ChargeType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="IndictmentNumber" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       <attribute name="IndictmentCountNumber" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       <attribute name="CJSoffenceCode" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="PNCoffenceCode" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="HOoffenceCode" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="Life" type="{http://www.courtservice.gov.uk/schemas/courtservice}YesNoType" />
 *       <attribute name="BreachMultiple" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChargeStructure", propOrder = {
    "crn",
    "cresTchargeID",
    "caseNumber",
    "offenceStatement",
    "offenceLocation",
    "arrestingPoliceForceCode",
    "committedOnBail",
    "offenceStartDateTime",
    "offenceEndDateTime",
    "arraignmentDate",
    "convictionDate",
    "offenceParticulars",
    "cresToffenceNumber",
    "plea",
    "verdict",
    "disposals",
    "sentenceTerm",
    "termType",
    "forLife"
})
public class ChargeStructure {

    @XmlElement(name = "CRN")
    protected String crn;
    @XmlElement(name = "CRESTchargeID")
    protected String cresTchargeID;
    @XmlElement(name = "CaseNumber")
    protected String caseNumber;
    @XmlElement(name = "OffenceStatement", required = true)
    protected String offenceStatement;
    @XmlElement(name = "OffenceLocation")
    protected UKPostalAddressStructure offenceLocation;
    @XmlElement(name = "ArrestingPoliceForceCode")
    protected String arrestingPoliceForceCode;
    @XmlElement(name = "CommittedOnBail")
    @XmlSchemaType(name = "string")
    protected YesNoType committedOnBail;
    @XmlElement(name = "OffenceStartDateTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar offenceStartDateTime;
    @XmlElement(name = "OffenceEndDateTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar offenceEndDateTime;
    @XmlElement(name = "ArraignmentDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar arraignmentDate;
    @XmlElement(name = "ConvictionDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar convictionDate;
    @XmlElement(name = "OffenceParticulars")
    protected String offenceParticulars;
    @XmlElement(name = "CRESToffenceNumber")
    protected String cresToffenceNumber;
    @XmlElement(name = "Plea")
    protected String plea;
    @XmlElement(name = "Verdict")
    protected String verdict;
    @XmlElement(name = "Disposals")
    protected Disposals disposals;
    @XmlElement(name = "SentenceTerm")
    protected Duration sentenceTerm;
    @XmlElement(name = "TermType")
    @XmlSchemaType(name = "string")
    protected TermTypeType termType;
    @XmlElement(name = "ForLife", defaultValue = "yes")
    @XmlSchemaType(name = "string")
    protected YesNoType forLife;
    @XmlAttribute(name = "ChargeType")
    protected String chargeType;
    @XmlAttribute(name = "IndictmentNumber")
    protected Integer indictmentNumber;
    @XmlAttribute(name = "IndictmentCountNumber", required = true)
    protected int indictmentCountNumber;
    @XmlAttribute(name = "CJSoffenceCode")
    protected String cjSoffenceCode;
    @XmlAttribute(name = "PNCoffenceCode")
    protected String pnCoffenceCode;
    @XmlAttribute(name = "HOoffenceCode")
    protected String hOoffenceCode;
    @XmlAttribute(name = "Life")
    protected YesNoType life;
    @XmlAttribute(name = "BreachMultiple")
    protected Integer breachMultiple;

    /**
     * Gets the value of the crn property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCRN() {
        return crn;
    }

    /**
     * Sets the value of the crn property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCRN(String value) {
        this.crn = value;
    }

    /**
     * Gets the value of the cresTchargeID property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCRESTchargeID() {
        return cresTchargeID;
    }

    /**
     * Sets the value of the cresTchargeID property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCRESTchargeID(String value) {
        this.cresTchargeID = value;
    }

    /**
     * Gets the value of the caseNumber property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCaseNumber() {
        return caseNumber;
    }

    /**
     * Sets the value of the caseNumber property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCaseNumber(String value) {
        this.caseNumber = value;
    }

    /**
     * Gets the value of the offenceStatement property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getOffenceStatement() {
        return offenceStatement;
    }

    /**
     * Sets the value of the offenceStatement property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setOffenceStatement(String value) {
        this.offenceStatement = value;
    }

    /**
     * Gets the value of the offenceLocation property.
     *
     * @return
     *     possible object is
     *     {@link UKPostalAddressStructure }
     *
     */
    public UKPostalAddressStructure getOffenceLocation() {
        return offenceLocation;
    }

    /**
     * Sets the value of the offenceLocation property.
     *
     * @param value
     *     allowed object is
     *     {@link UKPostalAddressStructure }
     *
     */
    public void setOffenceLocation(UKPostalAddressStructure value) {
        this.offenceLocation = value;
    }

    /**
     * Gets the value of the arrestingPoliceForceCode property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getArrestingPoliceForceCode() {
        return arrestingPoliceForceCode;
    }

    /**
     * Sets the value of the arrestingPoliceForceCode property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setArrestingPoliceForceCode(String value) {
        this.arrestingPoliceForceCode = value;
    }

    /**
     * Gets the value of the committedOnBail property.
     *
     * @return
     *     possible object is
     *     {@link YesNoType }
     *
     */
    public YesNoType getCommittedOnBail() {
        return committedOnBail;
    }

    /**
     * Sets the value of the committedOnBail property.
     *
     * @param value
     *     allowed object is
     *     {@link YesNoType }
     *
     */
    public void setCommittedOnBail(YesNoType value) {
        this.committedOnBail = value;
    }

    /**
     * Gets the value of the offenceStartDateTime property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getOffenceStartDateTime() {
        return offenceStartDateTime;
    }

    /**
     * Sets the value of the offenceStartDateTime property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setOffenceStartDateTime(XMLGregorianCalendar value) {
        this.offenceStartDateTime = value;
    }

    /**
     * Gets the value of the offenceEndDateTime property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getOffenceEndDateTime() {
        return offenceEndDateTime;
    }

    /**
     * Sets the value of the offenceEndDateTime property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setOffenceEndDateTime(XMLGregorianCalendar value) {
        this.offenceEndDateTime = value;
    }

    /**
     * Gets the value of the arraignmentDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getArraignmentDate() {
        return arraignmentDate;
    }

    /**
     * Sets the value of the arraignmentDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setArraignmentDate(XMLGregorianCalendar value) {
        this.arraignmentDate = value;
    }

    /**
     * Gets the value of the convictionDate property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getConvictionDate() {
        return convictionDate;
    }

    /**
     * Sets the value of the convictionDate property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setConvictionDate(XMLGregorianCalendar value) {
        this.convictionDate = value;
    }

    /**
     * Gets the value of the offenceParticulars property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getOffenceParticulars() {
        return offenceParticulars;
    }

    /**
     * Sets the value of the offenceParticulars property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setOffenceParticulars(String value) {
        this.offenceParticulars = value;
    }

    /**
     * Gets the value of the cresToffenceNumber property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCRESToffenceNumber() {
        return cresToffenceNumber;
    }

    /**
     * Sets the value of the cresToffenceNumber property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCRESToffenceNumber(String value) {
        this.cresToffenceNumber = value;
    }

    /**
     * Gets the value of the plea property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPlea() {
        return plea;
    }

    /**
     * Sets the value of the plea property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setPlea(String value) {
        this.plea = value;
    }

    /**
     * Gets the value of the verdict property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getVerdict() {
        return verdict;
    }

    /**
     * Sets the value of the verdict property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setVerdict(String value) {
        this.verdict = value;
    }

    /**
     * Gets the value of the disposals property.
     *
     * @return
     *     possible object is
     *     {@link Disposals }
     *
     */
    public Disposals getDisposals() {
        return disposals;
    }

    /**
     * Sets the value of the disposals property.
     *
     * @param value
     *     allowed object is
     *     {@link Disposals }
     *
     */
    public void setDisposals(Disposals value) {
        this.disposals = value;
    }

    /**
     * Gets the value of the sentenceTerm property.
     *
     * @return
     *     possible object is
     *     {@link Duration }
     *
     */
    public Duration getSentenceTerm() {
        return sentenceTerm;
    }

    /**
     * Sets the value of the sentenceTerm property.
     *
     * @param value
     *     allowed object is
     *     {@link Duration }
     *
     */
    public void setSentenceTerm(Duration value) {
        this.sentenceTerm = value;
    }

    /**
     * Gets the value of the termType property.
     *
     * @return
     *     possible object is
     *     {@link TermTypeType }
     *
     */
    public TermTypeType getTermType() {
        return termType;
    }

    /**
     * Sets the value of the termType property.
     *
     * @param value
     *     allowed object is
     *     {@link TermTypeType }
     *
     */
    public void setTermType(TermTypeType value) {
        this.termType = value;
    }

    /**
     * Gets the value of the forLife property.
     *
     * @return
     *     possible object is
     *     {@link YesNoType }
     *
     */
    public YesNoType getForLife() {
        return forLife;
    }

    /**
     * Sets the value of the forLife property.
     *
     * @param value
     *     allowed object is
     *     {@link YesNoType }
     *
     */
    public void setForLife(YesNoType value) {
        this.forLife = value;
    }

    /**
     * Gets the value of the chargeType property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getChargeType() {
        return chargeType;
    }

    /**
     * Sets the value of the chargeType property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setChargeType(String value) {
        this.chargeType = value;
    }

    /**
     * Gets the value of the indictmentNumber property.
     *
     * @return
     *     possible object is
     *     {@link Integer }
     *
     */
    public Integer getIndictmentNumber() {
        return indictmentNumber;
    }

    /**
     * Sets the value of the indictmentNumber property.
     *
     * @param value
     *     allowed object is
     *     {@link Integer }
     *
     */
    public void setIndictmentNumber(Integer value) {
        this.indictmentNumber = value;
    }

    /**
     * Gets the value of the indictmentCountNumber property.
     *
     */
    public int getIndictmentCountNumber() {
        return indictmentCountNumber;
    }

    /**
     * Sets the value of the indictmentCountNumber property.
     *
     */
    public void setIndictmentCountNumber(int value) {
        this.indictmentCountNumber = value;
    }

    /**
     * Gets the value of the cjSoffenceCode property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCJSoffenceCode() {
        return cjSoffenceCode;
    }

    /**
     * Sets the value of the cjSoffenceCode property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCJSoffenceCode(String value) {
        this.cjSoffenceCode = value;
    }

    /**
     * Gets the value of the pnCoffenceCode property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPNCoffenceCode() {
        return pnCoffenceCode;
    }

    /**
     * Sets the value of the pnCoffenceCode property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setPNCoffenceCode(String value) {
        this.pnCoffenceCode = value;
    }

    /**
     * Gets the value of the hOoffenceCode property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getHOoffenceCode() {
        return hOoffenceCode;
    }

    /**
     * Sets the value of the hOoffenceCode property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setHOoffenceCode(String value) {
        this.hOoffenceCode = value;
    }

    /**
     * Gets the value of the life property.
     *
     * @return
     *     possible object is
     *     {@link YesNoType }
     *
     */
    public YesNoType getLife() {
        return life;
    }

    /**
     * Sets the value of the life property.
     *
     * @param value
     *     allowed object is
     *     {@link YesNoType }
     *
     */
    public void setLife(YesNoType value) {
        this.life = value;
    }

    /**
     * Gets the value of the breachMultiple property.
     *
     * @return
     *     possible object is
     *     {@link Integer }
     *
     */
    public Integer getBreachMultiple() {
        return breachMultiple;
    }

    /**
     * Sets the value of the breachMultiple property.
     *
     * @param value
     *     allowed object is
     *     {@link Integer }
     *
     */
    public void setBreachMultiple(Integer value) {
        this.breachMultiple = value;
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
     *         <element name="Disposal" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
     *         <element name="CRESTDisposalData" type="{http://www.courtservice.gov.uk/schemas/courtservice}CRESTDisposalDataStructure" maxOccurs="unbounded" minOccurs="0"/>
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
        "disposal",
        "crestDisposalData"
    })
    public static class Disposals {

        @XmlElement(name = "Disposal", required = true)
        protected List<String> disposal;
        @XmlElement(name = "CRESTDisposalData")
        protected List<CRESTDisposalDataStructure> crestDisposalData;

        /**
         * Gets the value of the disposal property.
         *
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a {@code set} method for the disposal property.
         *
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getDisposal().add(newItem);
         * </pre>
         *
         *
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         *
         *
         * @return
         *     The value of the disposal property.
         */
        public List<String> getDisposal() {
            if (disposal == null) {
                disposal = new ArrayList<>();
            }
            return this.disposal;
        }

        /**
         * Gets the value of the crestDisposalData property.
         *
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a {@code set} method for the crestDisposalData property.
         *
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getCRESTDisposalData().add(newItem);
         * </pre>
         *
         *
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link CRESTDisposalDataStructure }
         *
         *
         * @return
         *     The value of the crestDisposalData property.
         */
        public List<CRESTDisposalDataStructure> getCRESTDisposalData() {
            if (crestDisposalData == null) {
                crestDisposalData = new ArrayList<>();
            }
            return this.crestDisposalData;
        }

    }

}
