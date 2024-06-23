package ro.linic.cloud.integration;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import ro.linic.cloud.TestData;
import ro.linic.cloud.TestSecurityConfig;
import ro.linic.cloud.repository.CompanyOAuthTokenRepository;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration(classes = TestSecurityConfig.class)
public class RealCaseIntegrationTest {
	
	private MockRestServiceServer mockServer;
	@Autowired private RestTemplate restTemplate;
	
	@Autowired private MockMvc mockMvc;
	@Autowired private CompanyOAuthTokenRepository companyOAuthTokenRepos;
	@Autowired private OAuth2AuthorizedClientService authorizedClientService;
	
	@BeforeEach
	public void setup() throws Exception {
		mockServer = MockRestServiceServer.createServer(restTemplate);
		TestData.init();
		companyOAuthTokenRepos.save(TestData.companyToken);
		authorizedClientService.saveAuthorizedClient(TestData.authorizedClient, TestData.principal);
	}

	@Test
	public void givenRealDataCase1_whenUploadInvoice_thenNoValidationErrors() throws Exception {
		/**
		 * Real test data taken from an Invoice to Primaria Marghita
		 * The errors returned from Anaf when sending the Invoice from the attached json are:
		 * 1. The Buyer vat identifier shall have the country 2 digit code prefix
		 * 2. TaxTotal not permitted at InvoiceLine level
		 * 3. Issue date format error(yyyy-mm-dd): 2023-12-18+02:00
		 * 4. Due date format error(yyyy-mm-dd): 2023-12-22+02:00
		 * 5. Customer partyLegalEntity companyId: attribute present but void not allowed
		 * 6. Customer contact electronicMail: attribute present but void not allowed
		 * These errors are fixed if the Anaf xml request matches the expected xml below
		 */
		
		mockServer.expect(ExpectedCount.once(), 
				requestTo(new URI("http://localhost:8083/test/FCTEL/rest/upload?standard=UBL&cif=14998343")))
		.andExpect(method(HttpMethod.POST))
		.andExpect(content().xml("""
				<?xml version="1.0" encoding="UTF-8"?><Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:cec="urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"><cbc:CustomizationID>urn:cen.eu:en16931:2017#compliant#urn:efactura.mfinante.ro:CIUS-RO:1.0.1</cbc:CustomizationID><cbc:ID>LINDL1-1136</cbc:ID><cbc:IssueDate>2023-12-12</cbc:IssueDate><cbc:DueDate>2023-12-20</cbc:DueDate><cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode><cbc:DocumentCurrencyCode>RON</cbc:DocumentCurrencyCode><cbc:TaxCurrencyCode>RON</cbc:TaxCurrencyCode><cac:AccountingSupplierParty><cac:Party><cac:PostalAddress><cbc:StreetName>Str Principala nr 218A</cbc:StreetName><cbc:CityName>MARGINE</cbc:CityName><cbc:CountrySubentity>RO-BH</cbc:CountrySubentity><cac:Country><cbc:IdentificationCode>RO</cbc:IdentificationCode></cac:Country></cac:PostalAddress><cac:PartyTaxScheme><cbc:CompanyID>RO14998343</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme><cac:PartyLegalEntity><cbc:RegistrationName>SC LINIC SRL</cbc:RegistrationName><cbc:CompanyID>J05/1111/2002</cbc:CompanyID><cbc:CompanyLegalForm>Capital social 100,000.00 RON</cbc:CompanyLegalForm></cac:PartyLegalEntity><cac:Contact><cbc:Name>GROZA MIRCEA</cbc:Name><cbc:Telephone>Colibri - 0787577227, Linic - 0259362437</cbc:Telephone><cbc:ElectronicMail>colibridepot@gmail.com, sclinicsrl@gmail.com</cbc:ElectronicMail></cac:Contact></cac:Party></cac:AccountingSupplierParty><cac:AccountingCustomerParty><cac:Party><cac:PostalAddress><cbc:StreetName>STR REPUBLICII JUD</cbc:StreetName><cbc:CityName>Marghita</cbc:CityName><cbc:CountrySubentity>RO-BH</cbc:CountrySubentity><cac:Country><cbc:IdentificationCode>RO</cbc:IdentificationCode></cac:Country></cac:PostalAddress><cac:PartyTaxScheme><cbc:CompanyID>RO4348947</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme><cac:PartyLegalEntity><cbc:RegistrationName>PRIMARIA MARGHITA</cbc:RegistrationName></cac:PartyLegalEntity><cac:Contact><cbc:Name>HECZI ALEXANDRU</cbc:Name></cac:Contact></cac:Party></cac:AccountingCustomerParty><cac:PaymentMeans><cbc:PaymentMeansCode>30</cbc:PaymentMeansCode><cbc:PaymentID>FF_1136/2023-12-12</cbc:PaymentID><cac:PayeeFinancialAccount><cbc:ID>RO48BTRL00501202K65277XX-RON</cbc:ID><cbc:Name>SC LINIC SRL</cbc:Name></cac:PayeeFinancialAccount></cac:PaymentMeans><cac:TaxTotal><cbc:TaxAmount currencyID="RON">82.06</cbc:TaxAmount><cac:TaxSubtotal><cbc:TaxableAmount currencyID="RON">431.94</cbc:TaxableAmount><cbc:TaxAmount currencyID="RON">82.06</cbc:TaxAmount><cac:TaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:TaxCategory></cac:TaxSubtotal></cac:TaxTotal><cac:LegalMonetaryTotal><cbc:LineExtensionAmount currencyID="RON">431.94</cbc:LineExtensionAmount><cbc:TaxExclusiveAmount currencyID="RON">431.94</cbc:TaxExclusiveAmount><cbc:TaxInclusiveAmount currencyID="RON">514.00</cbc:TaxInclusiveAmount><cbc:PrepaidAmount currencyID="RON">0</cbc:PrepaidAmount><cbc:PayableAmount currencyID="RON">514.00</cbc:PayableAmount></cac:LegalMonetaryTotal><cac:InvoiceLine><cbc:ID>444744</cbc:ID><cbc:InvoicedQuantity unitCode="C62">1.0000</cbc:InvoicedQuantity><cbc:LineExtensionAmount currencyID="RON">74.79</cbc:LineExtensionAmount><cac:Item><cbc:Name>CAPAC WC K2 ALB SANOBI INCHIDERE LENTA</cbc:Name><cac:SellersItemIdentification><cbc:ID>3734</cbc:ID></cac:SellersItemIdentification><cac:ClassifiedTaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:ClassifiedTaxCategory></cac:Item><cac:Price><cbc:PriceAmount currencyID="RON">74.79</cbc:PriceAmount><cbc:BaseQuantity unitCode="C62">1</cbc:BaseQuantity></cac:Price></cac:InvoiceLine><cac:InvoiceLine><cbc:ID>444745</cbc:ID><cbc:InvoicedQuantity unitCode="C62">1.0000</cbc:InvoicedQuantity><cbc:LineExtensionAmount currencyID="RON">96.64</cbc:LineExtensionAmount><cac:Item><cbc:Name>REZERVOR WC BETA EXPORT</cbc:Name><cac:SellersItemIdentification><cbc:ID>5947041000291</cbc:ID></cac:SellersItemIdentification><cac:ClassifiedTaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:ClassifiedTaxCategory></cac:Item><cac:Price><cbc:PriceAmount currencyID="RON">96.64</cbc:PriceAmount><cbc:BaseQuantity unitCode="C62">1</cbc:BaseQuantity></cac:Price></cac:InvoiceLine><cac:InvoiceLine><cbc:ID>444748</cbc:ID><cbc:InvoicedQuantity unitCode="C62">1.0000</cbc:InvoicedQuantity><cbc:LineExtensionAmount currencyID="RON">243.70</cbc:LineExtensionAmount><cac:Item><cbc:Name>VAS WC CIV CERSANIT PREZIDENT</cbc:Name><cac:SellersItemIdentification><cbc:ID>2092</cbc:ID></cac:SellersItemIdentification><cac:ClassifiedTaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:ClassifiedTaxCategory></cac:Item><cac:Price><cbc:PriceAmount currencyID="RON">243.70</cbc:PriceAmount><cbc:BaseQuantity unitCode="C62">1</cbc:BaseQuantity></cac:Price></cac:InvoiceLine><cac:InvoiceLine><cbc:ID>444743</cbc:ID><cbc:InvoicedQuantity unitCode="C62">1.0000</cbc:InvoicedQuantity><cbc:LineExtensionAmount currencyID="RON">138.66</cbc:LineExtensionAmount><cac:Item><cbc:Name>CAPAC WC ALB AMORTIZARE GEHLER DUROPLAST</cbc:Name><cac:SellersItemIdentification><cbc:ID>5949052855150</cbc:ID></cac:SellersItemIdentification><cac:ClassifiedTaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:ClassifiedTaxCategory></cac:Item><cac:Price><cbc:PriceAmount currencyID="RON">138.66</cbc:PriceAmount><cbc:BaseQuantity unitCode="C62">1</cbc:BaseQuantity></cac:Price></cac:InvoiceLine><cac:InvoiceLine><cbc:ID>444746</cbc:ID><cbc:InvoicedQuantity unitCode="C62">1.0000</cbc:InvoicedQuantity><cbc:LineExtensionAmount currencyID="RON">21.85</cbc:LineExtensionAmount><cac:Item><cbc:Name>SOUDAL SILICON SANITAR TRANSP 310ML 160357</cbc:Name><cac:SellersItemIdentification><cbc:ID>5411183185364</cbc:ID></cac:SellersItemIdentification><cac:ClassifiedTaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:ClassifiedTaxCategory></cac:Item><cac:Price><cbc:PriceAmount currencyID="RON">21.85</cbc:PriceAmount><cbc:BaseQuantity unitCode="C62">1</cbc:BaseQuantity></cac:Price></cac:InvoiceLine><cac:InvoiceLine><cbc:ID>444749</cbc:ID><cbc:InvoicedQuantity unitCode="C62">-1.0000</cbc:InvoicedQuantity><cbc:LineExtensionAmount currencyID="RON">-164.71</cbc:LineExtensionAmount><cac:Item><cbc:Name>DISCOUNT IESIRI</cbc:Name><cac:SellersItemIdentification><cbc:ID>795</cbc:ID></cac:SellersItemIdentification><cac:ClassifiedTaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:ClassifiedTaxCategory></cac:Item><cac:Price><cbc:PriceAmount currencyID="RON">164.71</cbc:PriceAmount><cbc:BaseQuantity unitCode="C62">1</cbc:BaseQuantity></cac:Price></cac:InvoiceLine><cac:InvoiceLine><cbc:ID>444747</cbc:ID><cbc:InvoicedQuantity unitCode="C62">1.0000</cbc:InvoicedQuantity><cbc:LineExtensionAmount currencyID="RON">21.01</cbc:LineExtensionAmount><cac:Item><cbc:Name>SOUDAL SILICON UNIVERSAL ALB 310ML 160359</cbc:Name><cac:SellersItemIdentification><cbc:ID>5411183185357</cbc:ID></cac:SellersItemIdentification><cac:ClassifiedTaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>19.00</cbc:Percent><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:ClassifiedTaxCategory></cac:Item><cac:Price><cbc:PriceAmount currencyID="RON">21.01</cbc:PriceAmount><cbc:BaseQuantity unitCode="C62">1</cbc:BaseQuantity></cac:Price></cac:InvoiceLine></Invoice>
				"""))
		.andRespond(withStatus(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_XML)
				.body("""
						<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
						<header xmlns="mfp:anaf:dgti:spv:respUploadFisier:v1" dateResponse="202108051140" ExecutionStatus="0" index_incarcare="3828"/>
						""")
				);
		
		mockMvc.perform(put("/report").header("Content-Type", "application/json").param("companyId", "1").content("""
				{"id":284135,"lines":[{"id":444744,"sellersItemIdentification":"3734","buyersItemIdentification":null,"note":null,"description":null,"name":"CAPAC WC K2 ALB SANOBI INCHIDERE LENTA","quantity":1.0000,"uom":"C62","classifiedTaxCategory":{"code":"S","percent":0.19,"taxExemptionReason":null,"taxScheme":"VAT"},"price":74.79,"baseQuantity":1,"allowanceCharges":null,"lineExtensionAmount":74.79,"taxAmount":14.21},{"id":444745,"sellersItemIdentification":"5947041000291","buyersItemIdentification":null,"note":null,"description":null,"name":"REZERVOR WC BETA EXPORT","quantity":1.0000,"uom":"C62","classifiedTaxCategory":{"code":"S","percent":0.19,"taxExemptionReason":null,"taxScheme":"VAT"},"price":96.64,"baseQuantity":1,"allowanceCharges":null,"lineExtensionAmount":96.64,"taxAmount":18.36},{"id":444748,"sellersItemIdentification":"2092","buyersItemIdentification":null,"note":null,"description":null,"name":"VAS WC CIV CERSANIT PREZIDENT","quantity":1.0000,"uom":"C62","classifiedTaxCategory":{"code":"S","percent":0.19,"taxExemptionReason":null,"taxScheme":"VAT"},"price":243.70,"baseQuantity":1,"allowanceCharges":null,"lineExtensionAmount":243.70,"taxAmount":46.30},{"id":444743,"sellersItemIdentification":"5949052855150","buyersItemIdentification":null,"note":null,"description":null,"name":"CAPAC WC ALB AMORTIZARE GEHLER DUROPLAST","quantity":1.0000,"uom":"C62","classifiedTaxCategory":{"code":"S","percent":0.19,"taxExemptionReason":null,"taxScheme":"VAT"},"price":138.66,"baseQuantity":1,"allowanceCharges":null,"lineExtensionAmount":138.66,"taxAmount":26.34},{"id":444746,"sellersItemIdentification":"5411183185364","buyersItemIdentification":null,"note":null,"description":null,"name":"SOUDAL SILICON SANITAR TRANSP 310ML 160357","quantity":1.0000,"uom":"C62","classifiedTaxCategory":{"code":"S","percent":0.19,"taxExemptionReason":null,"taxScheme":"VAT"},"price":21.85,"baseQuantity":1,"allowanceCharges":null,"lineExtensionAmount":21.85,"taxAmount":4.15},{"id":444749,"sellersItemIdentification":"795","buyersItemIdentification":null,"note":null,"description":null,"name":"DISCOUNT IESIRI","quantity":-1.0000,"uom":"C62","classifiedTaxCategory":{"code":"S","percent":0.19,"taxExemptionReason":null,"taxScheme":"VAT"},"price":164.71,"baseQuantity":1,"allowanceCharges":null,"lineExtensionAmount":-164.71,"taxAmount":-31.29},{"id":444747,"sellersItemIdentification":"5411183185357","buyersItemIdentification":null,"note":null,"description":null,"name":"SOUDAL SILICON UNIVERSAL ALB 310ML 160359","quantity":1.0000,"uom":"C62","classifiedTaxCategory":{"code":"S","percent":0.19,"taxExemptionReason":null,"taxScheme":"VAT"},"price":21.01,"baseQuantity":1,"allowanceCharges":null,"lineExtensionAmount":21.01,"taxAmount":3.99}],"invoiceNumber":"LINDL1-1136","issueDate":"2023-12-12T13:58:10.615159Z","dueDate":"2023-12-19T22:00:00Z","accountingSupplier":{"taxId":"RO14998343","businessName":null,"registrationName":"SC LINIC SRL","registrationId":"J05/1111/2002","companyLegalForm":"Capital social 100,000.00 RON","postalAddress":{"country":"RO","countrySubentity":"RO-BH","city":"MARGINE","postalZone":null,"primaryLine":"Str Principala nr 218A","secondaryLine":null},"contactName":"GROZA MIRCEA","telephone":"Colibri - 0787577227, Linic - 0259362437","electronicMail":"colibridepot@gmail.com, sclinicsrl@gmail.com"},"accountingCustomer":{"taxId":"4348947","businessName":null,"registrationName":"PRIMARIA MARGHITA","registrationId":"","companyLegalForm":null,"postalAddress":{"country":"RO","countrySubentity":"RO-BH","city":"Marghita","postalZone":null,"primaryLine":"STR REPUBLICII JUD","secondaryLine":null},"contactName":"HECZI ALEXANDRU","telephone":"","electronicMail":""},"payeeParty":null,"documentCurrencyCode":"RON","paymentMeansCode":"30","paymentId":"FF_1136/2023-12-12","payeeFinancialAccount":{"id":"RO48BTRL00501202K65277XX-RON","name":"SC LINIC SRL","financialInstitutionBranch":null,"currency":null},"taxCurrencyCode":"RON","taxAmount":82.06,"taxSubtotals":[{"taxableAmount":431.94,"taxAmount":82.06,"taxCategory":{"code":"S","percent":0.19,"taxExemptionReason":null,"taxScheme":"VAT"}}],"allowanceCharges":null,"lineExtensionAmount":431.94,"taxExclusiveAmount":431.94,"taxInclusiveAmount":514.00,"allowanceTotalAmount":null,"chargeTotalAmount":null,"prepaidAmount":0,"payableRoundingAmount":null,"payableAmount":514.00,"note":null}
				"""))
		.andExpect(status().isOk());
		
		mockServer.verify();
	}
}
