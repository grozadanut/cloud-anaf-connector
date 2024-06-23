package ro.linic.cloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.helger.ubl21.codelist.EAllowanceChargeReasonCode21;
import com.helger.ubl21.codelist.ECountryIdentificationCode21;
import com.helger.ubl21.codelist.ECurrencyCode21;
import com.helger.ubl21.codelist.EPaymentMeansCode21;
import com.helger.ubl21.codelist.EUnitOfMeasureCode21;

import ro.linic.cloud.pojo.Address;
import ro.linic.cloud.pojo.AllowanceCharge;
import ro.linic.cloud.pojo.FinancialAccount;
import ro.linic.cloud.pojo.Invoice;
import ro.linic.cloud.pojo.InvoiceLine;
import ro.linic.cloud.pojo.Party;
import ro.linic.cloud.pojo.TaxCategory;
import ro.linic.cloud.pojo.TaxSubtotal;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage.AnafReceivedMessageType;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessages;
import ro.linic.cloud.pojo.anaf.AnafResponseError;
import ro.linic.cloud.pojo.anaf.AnafUploadResponseHeader;
import ro.linic.cloud.pojo.anaf.AnafUploadStateResponseHeader;

@SpringBootTest
@ActiveProfiles("test")
public class AnafApiIntegrationTest {
	@Autowired private AnafApi anafApi;
    @Autowired private RestTemplate restTemplate;
    
    private MockRestServiceServer mockServer;
    
    private Invoice invoice;
    private OAuth2AccessToken token;
    
    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        token = new OAuth2AccessToken(TokenType.BEARER, "tokenValue", Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS));
        
        invoice = new Invoice();
		invoice.setId(1L);
		invoice.setInvoiceNumber("LIND100");
		invoice.setIssueDate(LocalDate.of(2023, 10, 20).atStartOfDay().toInstant(ZoneOffset.UTC));
		invoice.setDueDate(LocalDate.of(2023, 11, 19).atStartOfDay().toInstant(ZoneOffset.UTC));
		invoice.setDocumentCurrencyCode(ECurrencyCode21.RON.getID());
		invoice.setPaymentMeansCode(EPaymentMeansCode21._30.getID());
		invoice.setPaymentId("FF_LIND100/2023-10-20");
		invoice.setNote("Textual note");
		
		final FinancialAccount payeeFinancialAccount = new FinancialAccount();
		payeeFinancialAccount.setId("RO0123456xxx");
		payeeFinancialAccount.setName("LINIC SRL");
		payeeFinancialAccount.setFinancialInstitutionBranch("BT");
		payeeFinancialAccount.setCurrency(ECurrencyCode21.RON.getID());
		invoice.setPayeeFinancialAccount(payeeFinancialAccount );
		
		invoice.setTaxCurrencyCode(ECurrencyCode21.RON.getID());
		invoice.setTaxAmount(new BigDecimal("19"));
		
		final TaxSubtotal taxSubtotal = new TaxSubtotal();
		taxSubtotal.setTaxableAmount(new BigDecimal("100"));
		taxSubtotal.setTaxAmount(new BigDecimal("19"));
		final TaxCategory taxCategory = new TaxCategory();
		taxCategory.setCode("S");
		taxCategory.setPercent(new BigDecimal("0.19"));
		taxCategory.setTaxScheme("VAT");
		taxSubtotal.setTaxCategory(taxCategory);
		invoice.setTaxSubtotals(List.of(taxSubtotal));
		
		final AllowanceCharge charge = new AllowanceCharge();
		charge.setChargeIndicator(true);
		charge.setAllowanceChargeReasonCode(EAllowanceChargeReasonCode21.ZZZ.getID());
		charge.setAllowanceChargeReason(EAllowanceChargeReasonCode21.ZZZ.getDisplayName());
		charge.setMultiplierFactorNumeric(new BigDecimal("0.1"));
		charge.setBaseAmount(new BigDecimal("100"));
		charge.setAmount(new BigDecimal("10"));
		final AllowanceCharge allowance = new AllowanceCharge();
		allowance.setChargeIndicator(false);
		allowance.setAllowanceChargeReasonCode(EAllowanceChargeReasonCode21._19.getID());
		allowance.setAllowanceChargeReason(EAllowanceChargeReasonCode21._19.getDisplayName());
		allowance.setMultiplierFactorNumeric(new BigDecimal("0.1"));
		allowance.setBaseAmount(new BigDecimal("100"));
		allowance.setAmount(new BigDecimal("10"));
		invoice.setAllowanceCharges(List.of(charge, allowance));
		
		invoice.setLineExtensionAmount(new BigDecimal("100"));
		invoice.setTaxExclusiveAmount(new BigDecimal("100"));
		invoice.setTaxInclusiveAmount(new BigDecimal("119"));
		invoice.setAllowanceTotalAmount(new BigDecimal("10"));
		invoice.setChargeTotalAmount(new BigDecimal("10"));
		invoice.setPrepaidAmount(new BigDecimal("50"));
		invoice.setPayableAmount(new BigDecimal("79"));
		
		final Party supplier = new Party();
		supplier.setTaxId("RO1485236");
		supplier.setBusinessName("Colibri - Linic");
		supplier.setRegistrationName("LINIC SRL");
		supplier.setRegistrationId("J05/1001/2002");
		supplier.setCompanyLegalForm("Capital social 200 RON");
		supplier.setContactName("Groza Danut");
		supplier.setTelephone("0259xxx");
		supplier.setElectronicMail("linic@gmail.com");
		final Address supplierAddress = new Address();
		supplierAddress.setCountry(ECountryIdentificationCode21.RO.getID());
		supplierAddress.setCountrySubentity("RO-BH");
		supplierAddress.setCity("MARGHITA");
		supplierAddress.setPostalZone("415300");
		supplierAddress.setPrimaryLine("Balcescu 51");
		supplierAddress.setSecondaryLine("Vladimirescu 59");
		supplier.setPostalAddress(supplierAddress);
		invoice.setAccountingSupplier(supplier);
		invoice.setPayeeParty(supplier);

		final Party customer = new Party();
		customer.setTaxId("RO148");
		customer.setRegistrationName("CLIENT SRL");
		customer.setRegistrationId("J05/10/2023");
		customer.setContactName("Delegat");
		customer.setTelephone("0745154xxx");
		final Address customerAddress = new Address();
		customerAddress.setCountry(ECountryIdentificationCode21.RO.getID());
		customerAddress.setCountrySubentity("RO-BH");
		customerAddress.setCity("MARGHITA");
		customerAddress.setPostalZone("415300");
		customerAddress.setPrimaryLine("Revolutiei");
		customer.setPostalAddress(customerAddress);
		invoice.setAccountingCustomer(customer);
		
		final InvoiceLine invoiceLine = new InvoiceLine();
		invoiceLine.setId(1L);
		invoiceLine.setSellersItemIdentification("399");
		invoiceLine.setBuyersItemIdentification("598751225812");
		invoiceLine.setNote("Test note");
		invoiceLine.setDescription("Super fluffly paper");
		invoiceLine.setName("Toilet paper 3ply");
		invoiceLine.setQuantity(new BigDecimal("11"));
		invoiceLine.setUom(EUnitOfMeasureCode21.C62.getID());
		invoiceLine.setClassifiedTaxCategory(taxCategory);
		invoiceLine.setPrice(new BigDecimal("10"));
		invoiceLine.setBaseQuantity(new BigDecimal("1"));
		invoiceLine.setLineExtensionAmount(new BigDecimal("100"));
		invoiceLine.setTaxAmount(new BigDecimal("19"));
		invoiceLine.setAllowanceCharges(List.of(allowance));
		invoice.setLines(List.of(invoiceLine));
    }
    
    @Test
	public void givenValidInvoice_whenUpload_thenReturnUploadIndex() throws URISyntaxException {
    	// given
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/upload?standard=UBL&cif=1485236")))
		.andExpect(method(HttpMethod.POST))
		.andExpect(content().xml("""
				<?xml version="1.0" encoding="UTF-8"?><Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:cec="urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"><cbc:CustomizationID>urn:cen.eu:en16931:2017#compliant#urn:efactura.mfinante.ro:CIUS-RO:1.0.1</cbc:CustomizationID><cbc:ID>LIND100</cbc:ID><cbc:IssueDate>2023-10-20</cbc:IssueDate><cbc:DueDate>2023-11-19</cbc:DueDate><cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode><cbc:Note>Textual note</cbc:Note><cbc:DocumentCurrencyCode>RON</cbc:DocumentCurrencyCode><cbc:TaxCurrencyCode>RON</cbc:TaxCurrencyCode><cac:AccountingSupplierParty><cac:Party><cac:PartyName><cbc:Name>Colibri - Linic</cbc:Name></cac:PartyName><cac:PostalAddress><cbc:StreetName>Balcescu 51</cbc:StreetName><cbc:AdditionalStreetName>Vladimirescu 59</cbc:AdditionalStreetName><cbc:CityName>MARGHITA</cbc:CityName><cbc:PostalZone>415300</cbc:PostalZone><cbc:CountrySubentity>RO-BH</cbc:CountrySubentity><cac:Country><cbc:IdentificationCode>RO</cbc:IdentificationCode></cac:Country></cac:PostalAddress><cac:PartyTaxScheme><cbc:CompanyID>RO1485236</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme><cac:PartyLegalEntity><cbc:RegistrationName>LINIC SRL</cbc:RegistrationName><cbc:CompanyID>J05/1001/2002</cbc:CompanyID><cbc:CompanyLegalForm>Capital social 200 RON</cbc:CompanyLegalForm></cac:PartyLegalEntity><cac:Contact><cbc:Name>Groza Danut</cbc:Name><cbc:Telephone>0259xxx</cbc:Telephone><cbc:ElectronicMail>linic@gmail.com</cbc:ElectronicMail></cac:Contact></cac:Party></cac:AccountingSupplierParty><cac:AccountingCustomerParty><cac:Party><cac:PostalAddress><cbc:StreetName>Revolutiei</cbc:StreetName><cbc:CityName>MARGHITA</cbc:CityName><cbc:PostalZone>415300</cbc:PostalZone><cbc:CountrySubentity>RO-BH</cbc:CountrySubentity><cac:Country><cbc:IdentificationCode>RO</cbc:IdentificationCode></cac:Country></cac:PostalAddress><cac:PartyTaxScheme><cbc:CompanyID>RO148</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme><cac:PartyLegalEntity><cbc:RegistrationName>CLIENT SRL</cbc:RegistrationName><cbc:CompanyID>J05/10/2023</cbc:CompanyID></cac:PartyLegalEntity><cac:Contact><cbc:Name>Delegat</cbc:Name><cbc:Telephone>0745154xxx</cbc:Telephone></cac:Contact></cac:Party></cac:AccountingCustomerParty><cac:PayeeParty><cac:PartyName><cbc:Name>Colibri - Linic</cbc:Name></cac:PartyName><cac:PostalAddress><cbc:StreetName>Balcescu 51</cbc:StreetName><cbc:AdditionalStreetName>Vladimirescu 59</cbc:AdditionalStreetName><cbc:CityName>MARGHITA</cbc:CityName><cbc:PostalZone>415300</cbc:PostalZone><cbc:CountrySubentity>RO-BH</cbc:CountrySubentity><cac:Country><cbc:IdentificationCode>RO</cbc:IdentificationCode></cac:Country></cac:PostalAddress><cac:PartyTaxScheme><cbc:CompanyID>RO1485236</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme><cac:PartyLegalEntity><cbc:RegistrationName>LINIC SRL</cbc:RegistrationName><cbc:CompanyID>J05/1001/2002</cbc:CompanyID><cbc:CompanyLegalForm>Capital social 200 RON</cbc:CompanyLegalForm></cac:PartyLegalEntity><cac:Contact><cbc:Name>Groza Danut</cbc:Name><cbc:Telephone>0259xxx</cbc:Telephone><cbc:ElectronicMail>linic@gmail.com</cbc:ElectronicMail></cac:Contact></cac:PayeeParty><cac:PaymentMeans><cbc:PaymentMeansCode>30</cbc:PaymentMeansCode><cbc:PaymentID>FF_LIND100/2023-10-20</cbc:PaymentID><cac:PayeeFinancialAccount><cbc:ID>RO0123456xxx</cbc:ID><cbc:Name>LINIC SRL</cbc:Name><cbc:CurrencyCode>RON</cbc:CurrencyCode><cac:FinancialInstitutionBranch><cbc:ID>BT</cbc:ID></cac:FinancialInstitutionBranch></cac:PayeeFinancialAccount></cac:PaymentMeans><cac:AllowanceCharge><cbc:ChargeIndicator>true</cbc:ChargeIndicator><cbc:AllowanceChargeReasonCode>ZZZ</cbc:AllowanceChargeReasonCode><cbc:AllowanceChargeReason>Mutually defined</cbc:AllowanceChargeReason><cbc:MultiplierFactorNumeric>0.1</cbc:MultiplierFactorNumeric><cbc:Amount currencyID="RON">10</cbc:Amount><cbc:BaseAmount currencyID="RON">100</cbc:BaseAmount></cac:AllowanceCharge><cac:AllowanceCharge><cbc:ChargeIndicator>false</cbc:ChargeIndicator><cbc:AllowanceChargeReasonCode>19</cbc:AllowanceChargeReasonCode><cbc:AllowanceChargeReason>Trade discount</cbc:AllowanceChargeReason><cbc:MultiplierFactorNumeric>0.1</cbc:MultiplierFactorNumeric><cbc:Amount currencyID="RON">10</cbc:Amount><cbc:BaseAmount currencyID="RON">100</cbc:BaseAmount></cac:AllowanceCharge><cac:TaxTotal><cbc:TaxAmount currencyID="RON">19</cbc:TaxAmount><cac:TaxSubtotal><cbc:TaxableAmount currencyID="RON">100</cbc:TaxableAmount><cbc:TaxAmount currencyID="RON">19</cbc:TaxAmount><cac:TaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:TaxCategory></cac:TaxSubtotal></cac:TaxTotal><cac:LegalMonetaryTotal><cbc:LineExtensionAmount currencyID="RON">100</cbc:LineExtensionAmount><cbc:TaxExclusiveAmount currencyID="RON">100</cbc:TaxExclusiveAmount><cbc:TaxInclusiveAmount currencyID="RON">119</cbc:TaxInclusiveAmount><cbc:AllowanceTotalAmount currencyID="RON">10</cbc:AllowanceTotalAmount><cbc:ChargeTotalAmount currencyID="RON">10</cbc:ChargeTotalAmount><cbc:PrepaidAmount currencyID="RON">50</cbc:PrepaidAmount><cbc:PayableAmount currencyID="RON">79</cbc:PayableAmount></cac:LegalMonetaryTotal><cac:InvoiceLine><cbc:ID>1</cbc:ID><cbc:Note>Test note</cbc:Note><cbc:InvoicedQuantity unitCode="C62">11</cbc:InvoicedQuantity><cbc:LineExtensionAmount currencyID="RON">100</cbc:LineExtensionAmount><cac:AllowanceCharge><cbc:ChargeIndicator>false</cbc:ChargeIndicator><cbc:AllowanceChargeReasonCode>19</cbc:AllowanceChargeReasonCode><cbc:AllowanceChargeReason>Trade discount</cbc:AllowanceChargeReason><cbc:MultiplierFactorNumeric>0.1</cbc:MultiplierFactorNumeric><cbc:Amount currencyID="RON">10</cbc:Amount><cbc:BaseAmount currencyID="RON">100</cbc:BaseAmount></cac:AllowanceCharge><cac:Item><cbc:Description>Super fluffly paper</cbc:Description><cbc:Name>Toilet paper 3ply</cbc:Name><cac:BuyersItemIdentification><cbc:ID>598751225812</cbc:ID></cac:BuyersItemIdentification><cac:SellersItemIdentification><cbc:ID>399</cbc:ID></cac:SellersItemIdentification><cac:ClassifiedTaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:ClassifiedTaxCategory></cac:Item><cac:Price><cbc:PriceAmount currencyID="RON">10</cbc:PriceAmount><cbc:BaseQuantity unitCode="C62">1</cbc:BaseQuantity></cac:Price></cac:InvoiceLine></Invoice>
				"""))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:spv:respUploadFisier:v1" dateResponse="202108051140" ExecutionStatus="0" index_incarcare="3828"/>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadResponseHeader> response = anafApi.uploadInvoice(token, invoice);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getUploadIndex()).isEqualTo("3828");
    	assertThat(response.getBody().isExecutionStatusOk()).isTrue();
    	assertThat(response.getBody().getErrors()).isEmpty();
    }
    
    @Test
	public void givenTokenIsNull_whenUpload_thenThrowNPE() {
    	// given
		// when
    	// then
    	assertThrows(NullPointerException.class, () -> anafApi.uploadInvoice(null , invoice));
    }
    
    @Test
    public void givenInvoiceIsNull_whenUpload_thenThrowNPE() {
    	// given
    	// when
    	// then
    	assertThrows(NullPointerException.class, () -> anafApi.uploadInvoice(token , null));
    }
    
    @Test
   	public void givenInvalidAccessPermission_whenUpload_thenReturnError() throws URISyntaxException {
    	// given
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/upload?standard=UBL&cif=1485236")))
		.andExpect(method(HttpMethod.POST))
		.andExpect(content().xml("""
				<?xml version="1.0" encoding="UTF-8"?><Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:cec="urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"><cbc:CustomizationID>urn:cen.eu:en16931:2017#compliant#urn:efactura.mfinante.ro:CIUS-RO:1.0.1</cbc:CustomizationID><cbc:ID>LIND100</cbc:ID><cbc:IssueDate>2023-10-20</cbc:IssueDate><cbc:DueDate>2023-11-19</cbc:DueDate><cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode><cbc:Note>Textual note</cbc:Note><cbc:DocumentCurrencyCode>RON</cbc:DocumentCurrencyCode><cbc:TaxCurrencyCode>RON</cbc:TaxCurrencyCode><cac:AccountingSupplierParty><cac:Party><cac:PartyName><cbc:Name>Colibri - Linic</cbc:Name></cac:PartyName><cac:PostalAddress><cbc:StreetName>Balcescu 51</cbc:StreetName><cbc:AdditionalStreetName>Vladimirescu 59</cbc:AdditionalStreetName><cbc:CityName>MARGHITA</cbc:CityName><cbc:PostalZone>415300</cbc:PostalZone><cbc:CountrySubentity>RO-BH</cbc:CountrySubentity><cac:Country><cbc:IdentificationCode>RO</cbc:IdentificationCode></cac:Country></cac:PostalAddress><cac:PartyTaxScheme><cbc:CompanyID>RO1485236</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme><cac:PartyLegalEntity><cbc:RegistrationName>LINIC SRL</cbc:RegistrationName><cbc:CompanyID>J05/1001/2002</cbc:CompanyID><cbc:CompanyLegalForm>Capital social 200 RON</cbc:CompanyLegalForm></cac:PartyLegalEntity><cac:Contact><cbc:Name>Groza Danut</cbc:Name><cbc:Telephone>0259xxx</cbc:Telephone><cbc:ElectronicMail>linic@gmail.com</cbc:ElectronicMail></cac:Contact></cac:Party></cac:AccountingSupplierParty><cac:AccountingCustomerParty><cac:Party><cac:PostalAddress><cbc:StreetName>Revolutiei</cbc:StreetName><cbc:CityName>MARGHITA</cbc:CityName><cbc:PostalZone>415300</cbc:PostalZone><cbc:CountrySubentity>RO-BH</cbc:CountrySubentity><cac:Country><cbc:IdentificationCode>RO</cbc:IdentificationCode></cac:Country></cac:PostalAddress><cac:PartyTaxScheme><cbc:CompanyID>RO148</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme><cac:PartyLegalEntity><cbc:RegistrationName>CLIENT SRL</cbc:RegistrationName><cbc:CompanyID>J05/10/2023</cbc:CompanyID></cac:PartyLegalEntity><cac:Contact><cbc:Name>Delegat</cbc:Name><cbc:Telephone>0745154xxx</cbc:Telephone></cac:Contact></cac:Party></cac:AccountingCustomerParty><cac:PayeeParty><cac:PartyName><cbc:Name>Colibri - Linic</cbc:Name></cac:PartyName><cac:PostalAddress><cbc:StreetName>Balcescu 51</cbc:StreetName><cbc:AdditionalStreetName>Vladimirescu 59</cbc:AdditionalStreetName><cbc:CityName>MARGHITA</cbc:CityName><cbc:PostalZone>415300</cbc:PostalZone><cbc:CountrySubentity>RO-BH</cbc:CountrySubentity><cac:Country><cbc:IdentificationCode>RO</cbc:IdentificationCode></cac:Country></cac:PostalAddress><cac:PartyTaxScheme><cbc:CompanyID>RO1485236</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme><cac:PartyLegalEntity><cbc:RegistrationName>LINIC SRL</cbc:RegistrationName><cbc:CompanyID>J05/1001/2002</cbc:CompanyID><cbc:CompanyLegalForm>Capital social 200 RON</cbc:CompanyLegalForm></cac:PartyLegalEntity><cac:Contact><cbc:Name>Groza Danut</cbc:Name><cbc:Telephone>0259xxx</cbc:Telephone><cbc:ElectronicMail>linic@gmail.com</cbc:ElectronicMail></cac:Contact></cac:PayeeParty><cac:PaymentMeans><cbc:PaymentMeansCode>30</cbc:PaymentMeansCode><cbc:PaymentID>FF_LIND100/2023-10-20</cbc:PaymentID><cac:PayeeFinancialAccount><cbc:ID>RO0123456xxx</cbc:ID><cbc:Name>LINIC SRL</cbc:Name><cbc:CurrencyCode>RON</cbc:CurrencyCode><cac:FinancialInstitutionBranch><cbc:ID>BT</cbc:ID></cac:FinancialInstitutionBranch></cac:PayeeFinancialAccount></cac:PaymentMeans><cac:AllowanceCharge><cbc:ChargeIndicator>true</cbc:ChargeIndicator><cbc:AllowanceChargeReasonCode>ZZZ</cbc:AllowanceChargeReasonCode><cbc:AllowanceChargeReason>Mutually defined</cbc:AllowanceChargeReason><cbc:MultiplierFactorNumeric>0.1</cbc:MultiplierFactorNumeric><cbc:Amount currencyID="RON">10</cbc:Amount><cbc:BaseAmount currencyID="RON">100</cbc:BaseAmount></cac:AllowanceCharge><cac:AllowanceCharge><cbc:ChargeIndicator>false</cbc:ChargeIndicator><cbc:AllowanceChargeReasonCode>19</cbc:AllowanceChargeReasonCode><cbc:AllowanceChargeReason>Trade discount</cbc:AllowanceChargeReason><cbc:MultiplierFactorNumeric>0.1</cbc:MultiplierFactorNumeric><cbc:Amount currencyID="RON">10</cbc:Amount><cbc:BaseAmount currencyID="RON">100</cbc:BaseAmount></cac:AllowanceCharge><cac:TaxTotal><cbc:TaxAmount currencyID="RON">19</cbc:TaxAmount><cac:TaxSubtotal><cbc:TaxableAmount currencyID="RON">100</cbc:TaxableAmount><cbc:TaxAmount currencyID="RON">19</cbc:TaxAmount><cac:TaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:TaxCategory></cac:TaxSubtotal></cac:TaxTotal><cac:LegalMonetaryTotal><cbc:LineExtensionAmount currencyID="RON">100</cbc:LineExtensionAmount><cbc:TaxExclusiveAmount currencyID="RON">100</cbc:TaxExclusiveAmount><cbc:TaxInclusiveAmount currencyID="RON">119</cbc:TaxInclusiveAmount><cbc:AllowanceTotalAmount currencyID="RON">10</cbc:AllowanceTotalAmount><cbc:ChargeTotalAmount currencyID="RON">10</cbc:ChargeTotalAmount><cbc:PrepaidAmount currencyID="RON">50</cbc:PrepaidAmount><cbc:PayableAmount currencyID="RON">79</cbc:PayableAmount></cac:LegalMonetaryTotal><cac:InvoiceLine><cbc:ID>1</cbc:ID><cbc:Note>Test note</cbc:Note><cbc:InvoicedQuantity unitCode="C62">11</cbc:InvoicedQuantity><cbc:LineExtensionAmount currencyID="RON">100</cbc:LineExtensionAmount><cac:AllowanceCharge><cbc:ChargeIndicator>false</cbc:ChargeIndicator><cbc:AllowanceChargeReasonCode>19</cbc:AllowanceChargeReasonCode><cbc:AllowanceChargeReason>Trade discount</cbc:AllowanceChargeReason><cbc:MultiplierFactorNumeric>0.1</cbc:MultiplierFactorNumeric><cbc:Amount currencyID="RON">10</cbc:Amount><cbc:BaseAmount currencyID="RON">100</cbc:BaseAmount></cac:AllowanceCharge><cac:Item><cbc:Description>Super fluffly paper</cbc:Description><cbc:Name>Toilet paper 3ply</cbc:Name><cac:BuyersItemIdentification><cbc:ID>598751225812</cbc:ID></cac:BuyersItemIdentification><cac:SellersItemIdentification><cbc:ID>399</cbc:ID></cac:SellersItemIdentification><cac:ClassifiedTaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:ClassifiedTaxCategory></cac:Item><cac:Price><cbc:PriceAmount currencyID="RON">10</cbc:PriceAmount><cbc:BaseQuantity unitCode="C62">1</cbc:BaseQuantity></cac:Price></cac:InvoiceLine></Invoice>
				"""))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
				<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
				<header xmlns="mfp:anaf:dgti:spv:respUploadFisier:v1" dateResponse="202210121036" ExecutionStatus="1">
				    <Errors errorMessage="Nu exista niciun CIF petru care sa aveti drept in SPV"/>
				</header>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadResponseHeader> response = anafApi.uploadInvoice(token, invoice);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getUploadIndex()).isNull();
    	assertThat(response.getBody().isExecutionStatusOk()).isFalse();
    	assertThat(response.getBody().getErrors()).singleElement().extracting(AnafResponseError::getMessage)
    	.isEqualTo("Nu exista niciun CIF petru care sa aveti drept in SPV");
    }
    
    @Test
   	public void givenInvalidInvoice_whenUpload_thenThrowException() throws URISyntaxException {
    	// given
    	final Invoice invoice = new Invoice();
		invoice.setId(1L);
		invoice.setInvoiceNumber("LIND100");
		invoice.setIssueDate(LocalDate.of(2023, 10, 20).atStartOfDay().toInstant(ZoneOffset.UTC));
		
    	// when
		// then
		assertThatThrownBy(() -> anafApi.uploadInvoice(token , invoice))
		.isInstanceOf(ResponseStatusException.class)
		.hasMessageContaining("cvc-complex-type.2.4.a: Invalid content was found starting with element");
    }
    
    @Test
	public void givenValidInvoice_whenCheckInvoiceState_thenReturnInvoiceDownloadId() throws URISyntaxException {
    	// given
    	final String uploadIndex = "3828";
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/stareMesaj?id_incarcare="+uploadIndex)))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:efactura:stareMesajFactura:v1" stare="ok" id_descarcare="1234"/>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadStateResponseHeader> response = anafApi.checkInvoiceState(token, uploadIndex);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getDownloadId()).isEqualTo("1234");
    	assertThat(response.getBody().isStateOk()).isTrue();
    	assertThat(response.getBody().isStateNok()).isFalse();
    	assertThat(response.getBody().isStatePending()).isFalse();
    	assertThat(response.getBody().getErrors()).isEmpty();
    	assertThat(response.getBody().prettyErrorMessage()).isNullOrEmpty();
    }
    
    @Test
	public void givenInvalidInvoice_whenCheckInvoiceState_thenReturnErrorDownloadId() throws URISyntaxException {
    	// given
    	final String uploadIndex = "3828";
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/stareMesaj?id_incarcare="+uploadIndex)))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:efactura:stareMesajFactura:v1" stare="nok" id_descarcare="123"/>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadStateResponseHeader> response = anafApi.checkInvoiceState(token, uploadIndex);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getDownloadId()).isEqualTo("123");
    	assertThat(response.getBody().isStateOk()).isFalse();
    	assertThat(response.getBody().isStateNok()).isTrue();
    	assertThat(response.getBody().isStatePending()).isFalse();
    	assertThat(response.getBody().getErrors()).isEmpty();
    	assertThat(response.getBody().prettyErrorMessage()).isEqualTo("nok");
    }
    
    @Test
	public void givenValidationIsPending_whenCheckInvoiceState_thenReturnResponse() throws URISyntaxException {
    	// given
    	final String uploadIndex = "3828";
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/stareMesaj?id_incarcare="+uploadIndex)))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:efactura:stareMesajFactura:v1" stare="in prelucrare"/>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadStateResponseHeader> response = anafApi.checkInvoiceState(token, uploadIndex);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getDownloadId()).isNull();
    	assertThat(response.getBody().isStateOk()).isFalse();
    	assertThat(response.getBody().isStateNok()).isFalse();
    	assertThat(response.getBody().isStatePending()).isTrue();
    	assertThat(response.getBody().getErrors()).isEmpty();
    	assertThat(response.getBody().prettyErrorMessage()).isEqualTo("in prelucrare");
    }
    
    @Test
	public void givenXmlInvalid_whenCheckInvoiceState_thenReturnResponse() throws URISyntaxException {
    	// given
    	final String uploadIndex = "3828";
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/stareMesaj?id_incarcare="+uploadIndex)))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:efactura:stareMesajFactura:v1" stare="XML cu erori nepreluat de sistem"/>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadStateResponseHeader> response = anafApi.checkInvoiceState(token, uploadIndex);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getDownloadId()).isNull();
    	assertThat(response.getBody().isStateOk()).isFalse();
    	assertThat(response.getBody().isStateNok()).isFalse();
    	assertThat(response.getBody().isStatePending()).isFalse();
    	assertThat(response.getBody().getErrors()).isEmpty();
    	assertThat(response.getBody().prettyErrorMessage()).isEqualTo("XML cu erori nepreluat de sistem");
    }
    
    @Test
	public void givenUnauthorizedIndex_whenCheckInvoiceState_thenReturnResponse() throws URISyntaxException {
    	// given
    	final String uploadIndex = "3828";
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/stareMesaj?id_incarcare="+uploadIndex)))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:efactura:stareMesajFactura:v1">
						    <Errors errorMessage="Nu aveti dreptul de inteorgare pentru id_incarcare=3828"/>
						</header>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadStateResponseHeader> response = anafApi.checkInvoiceState(token, uploadIndex);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getDownloadId()).isNull();
    	assertThat(response.getBody().isStateOk()).isFalse();
    	assertThat(response.getBody().isStateNok()).isFalse();
    	assertThat(response.getBody().isStatePending()).isFalse();
    	assertThat(response.getBody().getErrors()).singleElement().extracting(AnafResponseError::getMessage)
    	.isEqualTo("Nu aveti dreptul de inteorgare pentru id_incarcare=3828");
    	assertThat(response.getBody().prettyErrorMessage()).isEqualTo("Nu aveti dreptul de inteorgare pentru id_incarcare=3828");
    }
    
    @Test
	public void givenUnauthorizedTaxId_whenCheckInvoiceState_thenReturnResponse() throws URISyntaxException {
    	// given
    	final String uploadIndex = "3828";
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/stareMesaj?id_incarcare="+uploadIndex)))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:efactura:stareMesajFactura:v1">
						    <Errors errorMessage="Nu exista niciun CIF petru care sa aveti drept"/>
						</header>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadStateResponseHeader> response = anafApi.checkInvoiceState(token, uploadIndex);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getDownloadId()).isNull();
    	assertThat(response.getBody().isStateOk()).isFalse();
    	assertThat(response.getBody().isStateNok()).isFalse();
    	assertThat(response.getBody().isStatePending()).isFalse();
    	assertThat(response.getBody().getErrors()).singleElement().extracting(AnafResponseError::getMessage)
    	.isEqualTo("Nu exista niciun CIF petru care sa aveti drept");
    	assertThat(response.getBody().prettyErrorMessage()).isEqualTo("Nu exista niciun CIF petru care sa aveti drept");
    }
    
    @Test
	public void givenInvalidUploadIndex_whenCheckInvoiceState_thenReturnResponse() throws URISyntaxException {
    	// given
    	final String uploadIndex = "aaa";
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/stareMesaj?id_incarcare="+uploadIndex)))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:efactura:stareMesajFactura:v1">
						    <Errors errorMessage="Id_incarcare introdus= aaa nu este un numar intreg"/>
						</header>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadStateResponseHeader> response = anafApi.checkInvoiceState(token, uploadIndex);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getDownloadId()).isNull();
    	assertThat(response.getBody().isStateOk()).isFalse();
    	assertThat(response.getBody().isStateNok()).isFalse();
    	assertThat(response.getBody().isStatePending()).isFalse();
    	assertThat(response.getBody().getErrors()).singleElement().extracting(AnafResponseError::getMessage)
    	.isEqualTo("Id_incarcare introdus= aaa nu este un numar intreg");
    	assertThat(response.getBody().prettyErrorMessage()).isEqualTo("Id_incarcare introdus= aaa nu este un numar intreg");
    }
    
    @Test
	public void givenInvoiceMissing_whenCheckInvoiceState_thenReturnResponse() throws URISyntaxException {
    	// given
    	final String uploadIndex = "15000";
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/stareMesaj?id_incarcare="+uploadIndex)))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:efactura:stareMesajFactura:v1">
						    <Errors errorMessage="Nu exista factura cu id_incarcare= 15000"/>
						</header>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadStateResponseHeader> response = anafApi.checkInvoiceState(token, uploadIndex);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getDownloadId()).isNull();
    	assertThat(response.getBody().isStateOk()).isFalse();
    	assertThat(response.getBody().isStateNok()).isFalse();
    	assertThat(response.getBody().isStatePending()).isFalse();
    	assertThat(response.getBody().getErrors()).singleElement().extracting(AnafResponseError::getMessage)
    	.isEqualTo("Nu exista factura cu id_incarcare= 15000");
    	assertThat(response.getBody().prettyErrorMessage()).isEqualTo("Nu exista factura cu id_incarcare= 15000");
    }
    
    @Test
	public void givenQuotaExceeded_whenCheckInvoiceState_thenReturnResponse() throws URISyntaxException {
    	// given
    	final String uploadIndex = "3828";
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/stareMesaj?id_incarcare="+uploadIndex)))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:efactura:stareMesajFactura:v1">
						    <Errors errorMessage="S-au facut deja 20 descarcari de mesaj in cursul zilei"/>
						</header>
						""")
				);
    	// when
    	final ResponseEntity<AnafUploadStateResponseHeader> response = anafApi.checkInvoiceState(token, uploadIndex);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getDownloadId()).isNull();
    	assertThat(response.getBody().isStateOk()).isFalse();
    	assertThat(response.getBody().isStateNok()).isFalse();
    	assertThat(response.getBody().isStatePending()).isFalse();
    	assertThat(response.getBody().getErrors()).singleElement().extracting(AnafResponseError::getMessage)
    	.isEqualTo("S-au facut deja 20 descarcari de mesaj in cursul zilei");
    	assertThat(response.getBody().prettyErrorMessage()).isEqualTo("S-au facut deja 20 descarcari de mesaj in cursul zilei");
    }
    
    @Test
	public void givenValidCall_whenReceivedMessages_thenReturnParsedMessages() throws URISyntaxException {
    	// given
    	final String cif = "1485236";
		final int days = 60;
		
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/listaMesajeFactura?zile="+days+"&cif="+cif+"")))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{
						  "mesaje": [
						    {
						      "data_creare": "202211011415",
						      "cif": "1485236",
						      "id_solicitare": "5001130147",
						      "detalii": "Erori de validare identificate la factura primita cu id_incarcare=5001130147",
						      "tip": "ERORI FACTURA",
						      "id": "3001293434"
						    },
						    {
						      "data_creare": "202211011336",
						      "cif": "1485236",
						      "id_solicitare": "5001131297",
						      "detalii": "Factura cu id_incarcare=5001131297 emisa de cif_emitent=1485236 pentru cif_beneficiar=3",
						      "tip": "FACTURA TRIMISA",
						      "id": "3001503294"
						    },
						    {
						      "data_creare": "202401251436",
						      "cif": "1485236",
						      "id_solicitare": "5006514680",
						      "detalii": "Factura cu id_incarcare=5006514680 emisa de cif_emitent=1485236 pentru cif_beneficiar=1485236",
						      "tip": "FACTURA PRIMITA",
						      "id": "3009239535"
						    }
						  ],
						  "serial": "1234AA456",
						  "cui": "1485236",
						  "titlu": "Lista Mesaje disponibile din ultimele 60 zile"
						}
						""")
				);
    	
		// when
    	final ResponseEntity<AnafReceivedMessages> response = anafApi.receivedMessages(token, cif, days);
    	
    	// then
    	final AnafReceivedMessage billSent = new AnafReceivedMessage("3001503294", LocalDateTime.of(2022, 11, 1, 13, 36),
    			"1485236", "5001131297", "Factura cu id_incarcare=5001131297 emisa de cif_emitent=1485236 pentru cif_beneficiar=3",
    			AnafReceivedMessageType.BILL_SENT);
    	final AnafReceivedMessage billReceived = new AnafReceivedMessage("3009239535", LocalDateTime.of(2024, 1, 25, 14, 36),
    			"1485236", "5006514680", "Factura cu id_incarcare=5006514680 emisa de cif_emitent=1485236 pentru cif_beneficiar=1485236",
    			AnafReceivedMessageType.BILL_RECEIVED);
    	final AnafReceivedMessage validationErrors = new AnafReceivedMessage("3001293434", LocalDateTime.of(2022, 11, 1, 14, 15),
    			"1485236", "5001130147", "Erori de validare identificate la factura primita cu id_incarcare=5001130147",
    			AnafReceivedMessageType.BILL_ERRORS);
    	
    	mockServer.verify();
    	assertThat(response.getBody().getError()).isNullOrEmpty();
    	assertThat(response.getBody().getMessages()).hasSize(3);
		assertThat(response.getBody().getMessages()).containsExactlyInAnyOrder(billSent, billReceived, validationErrors);
    }
    
    @Test
	public void givenTaxIdIsNonNumeric_whenReceivedMessages_thenReturnError() throws URISyntaxException {
    	// given
    	final String cif = "aaa";
		final int days = 60;
		
    	mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/listaMesajeFactura?zile="+days+"&cif="+cif+"")))
		.andExpect(method(HttpMethod.GET))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{
						  "eroare": "CIF introdus= aaa nu este un numar",
						  "titlu": "Lista Mesaje"
						}
						""")
				);
    	
		// when
    	final ResponseEntity<AnafReceivedMessages> response = anafApi.receivedMessages(token, cif, days);
    	
    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getError()).isEqualTo("CIF introdus= aaa nu este un numar");
    	assertThat(response.getBody().getMessages()).isEmpty();
    }
    
    @Test
    public void givenDayGreaterThan60_whenReceivedMessages_thenThrowException() throws URISyntaxException {
    	// given
    	final String cif = "1485236";
    	final int days = 61;

    	// when
    	// then
    	assertThrows(ResponseStatusException.class, () -> anafApi.receivedMessages(token, cif, days));
    }

    @Test
    public void givenDaySmallerThan1_whenReceivedMessages_thenThrowException() throws URISyntaxException {
    	// given
    	final String cif = "1485236";
    	final int days = 0;

    	// when
    	// then
    	assertThrows(ResponseStatusException.class, () -> anafApi.receivedMessages(token, cif, days));
    }
    
    @Test
    public void givenTaxIdNotAllowed_whenReceivedMessages_thenReturnError() throws URISyntaxException {
    	// given
    	final String cif = "8000000000";
    	final int days = 60;

    	mockServer.expect(ExpectedCount.once(), 
    			requestTo(new URI("http://localhost:8083/test/FCTEL/rest/listaMesajeFactura?zile="+days+"&cif="+cif+"")))
    	.andExpect(method(HttpMethod.GET))
    	.andRespond(withStatus(HttpStatus.OK)
    			.contentType(MediaType.APPLICATION_JSON)
    			.body("""
    					{
						  "eroare": "Nu aveti drept in SPV pentru CIF=8000000000",
						  "titlu": "Lista Mesaje"
						}
    					""")
    			);

    	// when
    	final ResponseEntity<AnafReceivedMessages> response = anafApi.receivedMessages(token, cif, days);

    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getError()).isEqualTo("Nu aveti drept in SPV pentru CIF=8000000000");
    	assertThat(response.getBody().getMessages()).isEmpty();
    }
    
    @Test
    public void givenNotAllowed_whenReceivedMessages_thenReturnError() throws URISyntaxException {
    	// given
    	final String cif = "1485236";
    	final int days = 60;

    	mockServer.expect(ExpectedCount.once(), 
    			requestTo(new URI("http://localhost:8083/test/FCTEL/rest/listaMesajeFactura?zile="+days+"&cif="+cif+"")))
    	.andExpect(method(HttpMethod.GET))
    	.andRespond(withStatus(HttpStatus.OK)
    			.contentType(MediaType.APPLICATION_JSON)
    			.body("""
    					{
    					  "eroare": "Nu exista niciun CIF petru care sa aveti drept in SPV",
    					  "titlu": "Lista Mesaje"
    					}
    					""")
    			);

    	// when
    	final ResponseEntity<AnafReceivedMessages> response = anafApi.receivedMessages(token, cif, days);

    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getError()).isEqualTo("Nu exista niciun CIF petru care sa aveti drept in SPV");
    	assertThat(response.getBody().getMessages()).isEmpty();
    }
    
    @Test
    public void givenNoMessages_whenReceivedMessages_thenReturnSpecialErrorCode() throws URISyntaxException {
    	// given
    	final String cif = "1485236";
    	final int days = 60;

    	mockServer.expect(ExpectedCount.once(), 
    			requestTo(new URI("http://localhost:8083/test/FCTEL/rest/listaMesajeFactura?zile="+days+"&cif="+cif+"")))
    	.andExpect(method(HttpMethod.GET))
    	.andRespond(withStatus(HttpStatus.OK)
    			.contentType(MediaType.APPLICATION_JSON)
    			.body("""
    					{
						  "eroare": "Nu exista mesaje in ultimele 60 zile",
						  "titlu": "Lista Mesaje"
						}
    					""")
    			);

    	// when
    	final ResponseEntity<AnafReceivedMessages> response = anafApi.receivedMessages(token, cif, days);

    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getError()).isEqualTo(AnafReceivedMessages.NO_MESSAGES_ERROR);
    	assertThat(response.getBody().getMessages()).isEmpty();
    }
    
    @Test
    public void givenTooManyMessages_whenReceivedMessages_thenReturnSpecialErrorCode() throws URISyntaxException {
    	// given
    	final String cif = "1485236";
    	final int days = 60;

    	mockServer.expect(ExpectedCount.once(), 
    			requestTo(new URI("http://localhost:8083/test/FCTEL/rest/listaMesajeFactura?zile="+days+"&cif="+cif+"")))
    	.andExpect(method(HttpMethod.GET))
    	.andRespond(withStatus(HttpStatus.OK)
    			.contentType(MediaType.APPLICATION_JSON)
    			.body("""
    					{
						  "eroare": "Lista de mesaje este mai mare decat numarul de 500 elemente permise in pagina. Folositi endpoint-ul cu paginatie.",
						  "titlu": "Lista Mesaje"
						}
    					""")
    			);

    	// when
    	final ResponseEntity<AnafReceivedMessages> response = anafApi.receivedMessages(token, cif, days);

    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getError()).isEqualTo(AnafReceivedMessages.TOO_MANY_MESSAGES_ERROR);
    	assertThat(response.getBody().getMessages()).isEmpty();
    }
    
    @Test
    public void givenDailyQuotaReached_whenReceivedMessages_thenReturnError() throws URISyntaxException {
    	// given
    	final String cif = "1485236";
    	final int days = 60;

    	mockServer.expect(ExpectedCount.once(), 
    			requestTo(new URI("http://localhost:8083/test/FCTEL/rest/listaMesajeFactura?zile="+days+"&cif="+cif+"")))
    	.andExpect(method(HttpMethod.GET))
    	.andRespond(withStatus(HttpStatus.OK)
    			.contentType(MediaType.APPLICATION_JSON)
    			.body("""
    					{
						  "eroare": "S-au facut deja 1000 interogari de lista mesaje de catre utilizator in cursul zilei",
						  "titlu": "Lista Mesaje"
						}
    					""")
    			);

    	// when
    	final ResponseEntity<AnafReceivedMessages> response = anafApi.receivedMessages(token, cif, days);

    	// then
    	mockServer.verify();
    	assertThat(response.getBody().getError()).isEqualTo("S-au facut deja 1000 interogari de lista mesaje de catre utilizator in cursul zilei");
    	assertThat(response.getBody().getMessages()).isEmpty();
    }
}
