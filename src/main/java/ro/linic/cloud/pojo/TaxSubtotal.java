package ro.linic.cloud.pojo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TaxSubtotal {
	/**
	 * VAT category taxable amount
	 * Sum of all taxable amounts subject to a specific VAT category code and VAT category rate (if the VAT category rate is applicable). 
	 * Must be rounded to maximum 2 decimals.
	 * Example value: 1945.00
	 */
	private BigDecimal taxableAmount;
	/**
	 * VAT category tax amount
	 * The total VAT amount for a given VAT category. Must be rounded to maximum 2 decimals.
	 * Example value: 486.25
	 */
	private BigDecimal taxAmount;
	private TaxCategory taxCategory;
}
