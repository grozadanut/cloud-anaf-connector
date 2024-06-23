package ro.linic.cloud.pojo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class InvoiceLine {
	/**
	 * Invoice line identifier
	 * A unique identifier for the individual line within the Invoice.
	 * Usually database persistence id.
	 * Example value: 12
	 */
	private Long id;
	/**
	 * Item Seller's identifier
	 * An identifier, assigned by the Seller, for the item.
	 * Example value: 9873242
	 */
	private String sellersItemIdentification;
	/**
	 * Item Buyer's identifier
	 * An identifier, assigned by the Buyer, for the item.
	 * Example value: 123455
	 */
	private String buyersItemIdentification;
	/**
	 * Invoice line note
	 * A textual note that gives unstructured information that is relevant to the Invoice line.
	 * Example value: New article number 12345
	 */
	private String note;
	/**
	 * Item description
	 * A description for an item.The item description allows for describing the item and its features in more detail than the Item name.
	 * Example value: Long description of the item on the invoice line
	 */
	private String description;
	/**
	 * Item name
	 * A name for an item.
	 * Example value: Item name
	 */
	private String name;
	
	/**
	 * Invoiced quantity
	 * The quantity of items (goods or services) that is charged in the Invoice line.
	 * Example value: 100
	 */
	private BigDecimal quantity;
	/**
	 * Invoiced quantity unit of measure
	 * The unit of measure that applies to the invoiced quantity. Codes for unit of packaging from UNECE Recommendation 
	 * No. 21 can be used in accordance with the descriptions in the "Intro" section of UN/ECE Recommendation 20, 
	 * Revision 11 (2015): The 2 character alphanumeric code values in UNECE Recommendation 21 shall be used. 
	 * To avoid duplication with existing code values in UNECE Recommendation No. 20, each code value from 
	 * UNECE Recommendation 21 shall be prefixed with an “X”, resulting in a 3 alphanumeric code when used as a unit of measure.
	 * Example value: C62
	 * <a href='https://docs.peppol.eu/poacc/billing/3.0/codelist/UNECERec20/'>https://docs.peppol.eu/poacc/billing/3.0/codelist/UNECERec20/<a>
	 */
	private String uom;
	
	/**
	 * A group of business terms providing information about the VAT applicable for the goods and services invoiced on the Invoice line.
	 */
	private TaxCategory classifiedTaxCategory;
	/**
	 * Item net price
	 * The price of an item, exclusive of VAT, after subtracting item price discount. 
	 * The Item net price has to be equal with the Item gross price less the Item price discount, 
	 * if they are both provided. Item price can not be negative.
	 * Example value: 23.45
	 */
	private BigDecimal price;
	
	/**
	 * Item price base quantity
	 * The number of item units to which the price applies.
	 * Example value: 1
	 */
	private BigDecimal baseQuantity;
	
	/**
	 * INVOICE LINE ALLOWANCES OR CHARGES
	 * A group of business terms providing information about allowances or charges applicable to the individual Invoice line.
	 */
	private List<AllowanceCharge> allowanceCharges;
	
	/**
	 * Invoice line net amount
	 * The total amount of the Invoice line. The amount is "net" without VAT, i.e. inclusive of line level allowances and 
	 * charges as well as other relevant taxes. Must be rounded to maximum 2 decimals.
	 * Example value: 2145.00
	 */
	private BigDecimal lineExtensionAmount;
	/**
	 * The total tax amount for a particular taxation scheme, e.g., VAT; the sum of the tax subtotals 
	 * for each tax category within the taxation scheme.
	 */
	private BigDecimal taxAmount;
}
