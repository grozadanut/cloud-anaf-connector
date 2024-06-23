package ro.linic.cloud.pojo;

import lombok.Data;

@Data
public class Party {
	/**
	 * Seller VAT identifier, Seller tax registration identifier
	 * The Seller's VAT identifier (also known as Seller VAT identification number) or the local identification 
	 * (defined by the Seller’s address) of the Seller for tax purposes or a reference that enables the Seller 
	 * to state his registered tax status. In order for the buyer to automatically identify a supplier, 
	 * the Seller identifier (BT-29), the Seller legal registration identifier (BT-30) and/or the Seller VAT identifier 
	 * (BT-31) shall be present
	 */
	private String taxId;
	/**
	 * Party trading name
	 * A name by which the Party is known, other than Party name (also known as Business name).
	 * Example value: Seller Business Name AS
	 */
	private String businessName;
	/**
	 * The full formal name by which the Party is registered in the national registry of legal entities 
	 * or as a Taxable person or otherwise trades as a person or persons.
	 * Example value: Full Formal Seller Name LTD.
	 */
	private String registrationName;
	/**
	 * Seller legal registration identifier
	 * An identifier issued by an official registrar that identifies the Seller as a legal entity or person. 
	 * In order for the buyer to automatically identify a supplier, the Seller identifier (BT-29), 
	 * the Seller legal registration identifier (BT-30) and/or the Seller VAT identifier (BT-31) shall be present
	 * Example value: J02/321/2010
	 */
	private String registrationId;
	/**
	 * Party additional legal information
	 * Additional legal information relevant for the Party.
	 * Example value: Share capital 200 Ron("Capital social 200 RON")
	 */
	private String companyLegalForm;
	private Address postalAddress;
	/**
	 * A contact point for a legal entity or person.
	 * Example value: John Wick
	 */
	private String contactName;
	private String telephone;
	private String electronicMail;
}
