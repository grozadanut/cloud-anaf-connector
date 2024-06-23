package ro.linic.cloud.pojo;

import lombok.Data;

@Data
// alias BankAccount
public class FinancialAccount {
	/**
	 * Payment account identifier
	 * A unique identifier of the financial payment account, at a payment service provider, to which payment should be made. 
	 * Such as IBAN or BBAN.
	 * Example value: NO99991122222
	 */
	private String id;
	/**
	 * Payment account name
	 * The name of the payment account, at a payment service provider, to which payment should be made.
	 * Example value: Company SRL
	 */
	private String name;
	/**
	 * Payment service provider identifier
	 * An identifier for the payment service provider where a payment account is located. 
	 * Such as a BIC or a national clearing code where required. No identification scheme Identifier to be used.
	 * Example value: 9999
	 */
	private String financialInstitutionBranch;
	/**
	 * A code signifying the currency in which this financial account is held.
	 * Example: RON
	 */
	private String currency;
}
