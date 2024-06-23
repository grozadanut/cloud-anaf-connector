package ro.linic.cloud.pojo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class AllowanceCharge {
	/**
	 * Use "true" when informing about Charges and "false" when informing about Allowances.
	 * Example value: false
	 */
	private Boolean chargeIndicator;
	/**
	 * Document level allowance or charge reason code
	 * The reason for the document level allowance or charge, expressed as a code. 
	 * A subset of codelist EAllowanceChargeReasonCode21 is to be used. 
	 * The Document level allowance reason code and the Document level allowance reason shall indicate the same allowance reason
	 * Example value: 19
	 */
	private String allowanceChargeReasonCode;
	/**
	 * Document level allowance or charge reason
	 * The reason for the document level allowance or charge, expressed as text. 
	 * The Document level allowance reason code and the Document level allowance reason shall indicate the same allowance reason
	 * Example value: Trade discount
	 */
	private String allowanceChargeReason;
	/**
	 * Document level allowance or charge percentage
	 * The percentage that may be used, in conjunction with the document level allowance base amount, 
	 * to calculate the document level allowance or charge amount. To state 20%, use value 0.2
	 * Example value: 0.20
	 */
	private BigDecimal multiplierFactorNumeric;
	/**
	 * Document level allowance or charge amount
	 * The amount of an allowance or a charge, without VAT. Must be rounded to maximum 2 decimals
	 * Example value: 200
	 */
	private BigDecimal amount;
	/**
	 * Document level allowance or charge base amount
	 * The base amount that may be used, in conjunction with the document level allowance or charge percentage, 
	 * to calculate the document level allowance or charge amount. Must be rounded to maximum 2 decimals
	 * Example value: 1000
	 */
	private BigDecimal baseAmount;
}
