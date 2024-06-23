package ro.linic.cloud.pojo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TaxCategory {
	/**
	 * VAT category code
	 * Coded identification of a VAT category.
	 * Example value: S
	 * Duty or tax or fee category code (Subset of UNCL5305)
	 */
	private String code;
	/**
	 * VAT category rate
	 * The VAT rate, represented as percentage that applies for the relevant VAT category.
	 * Example value: 0.19
	 */
	private BigDecimal percent;
	/**
	 * VAT exemption reason text
	 * A textual statement of the reason why the amount is exempted from VAT or why no VAT is being charged.
	 * Example value: Exempt
	 */
	private String taxExemptionReason;
	/**
	 * Mandatory element. Use "VAT"
	 * Default value: VAT
	 */
	private String taxScheme;
}
