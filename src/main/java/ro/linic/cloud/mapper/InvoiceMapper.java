package ro.linic.cloud.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.helger.ubl21.codelist.ECountryIdentificationCode21;
import com.nimbusds.oauth2.sdk.util.StringUtils;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ItemType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyLegalEntityType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyTaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PaymentMeansType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PriceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.CompanyIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.NoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.AmountType;
import ro.linic.cloud.pojo.Address;
import ro.linic.cloud.pojo.AllowanceCharge;
import ro.linic.cloud.pojo.Invoice;
import ro.linic.cloud.pojo.InvoiceLine;
import ro.linic.cloud.pojo.Party;
import ro.linic.cloud.pojo.TaxCategory;
import ro.linic.cloud.pojo.TaxSubtotal;

@Mapper(uses = {InvoiceTypePrimitiveMapper.class, InvoiceDeleteEmptyMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {
	InvoiceMapper INSTANCE = Mappers.getMapper(InvoiceMapper.class);
	
	static void removeCurrencyIfAmountNull(final AmountType amountType) {
		Optional.ofNullable(amountType)
		.filter(at -> at.getValue() == null)
		.ifPresent(at -> at.setCurrencyID(null));
	}
	
	@Mapping(target = "customizationID.value", constant = "urn:cen.eu:en16931:2017#compliant#urn:efactura.mfinante.ro:CIUS-RO:1.0.1")
	@Mapping(target = "invoiceTypeCode.value", constant = "380")
	@Mapping(target = "ID", source = "inv.invoiceNumber")
	@Mapping(target = "issueDate", source = "inv.issueDate")
	@Mapping(target = "dueDate", source = "inv.dueDate")
	@Mapping(target = "accountingSupplierParty", source = "inv.accountingSupplier")
	@Mapping(target = "accountingCustomerParty", source = "inv.accountingCustomer")
	@Mapping(target = "payeeParty", source = "inv.payeeParty")
	@Mapping(target = "documentCurrencyCode", source = "inv.documentCurrencyCode")
	@Mapping(target = "paymentMeans", expression = "java(listOf(mapPaymentMeansType(inv)))")
	@Mapping(target = "taxCurrencyCode", source = "inv.taxCurrencyCode")
	@Mapping(target = "allowanceCharge", source = "inv.allowanceCharges")
	@Mapping(target = "taxTotal", expression = "java(listOf(mapTaxTotalType(inv)))")
	@Mapping(target = "legalMonetaryTotal.lineExtensionAmount.value", source = "inv.lineExtensionAmount")
	@Mapping(target = "legalMonetaryTotal.lineExtensionAmount.currencyID", source = "inv.documentCurrencyCode")
	@Mapping(target = "legalMonetaryTotal.taxExclusiveAmount.value", source = "inv.taxExclusiveAmount")
	@Mapping(target = "legalMonetaryTotal.taxExclusiveAmount.currencyID", source = "inv.documentCurrencyCode")
	@Mapping(target = "legalMonetaryTotal.taxInclusiveAmount.value", source = "inv.taxInclusiveAmount")
	@Mapping(target = "legalMonetaryTotal.taxInclusiveAmount.currencyID", source = "inv.documentCurrencyCode")
	@Mapping(target = "legalMonetaryTotal.allowanceTotalAmount.value", source = "inv.allowanceTotalAmount")
	@Mapping(target = "legalMonetaryTotal.allowanceTotalAmount.currencyID", source = "inv.documentCurrencyCode")
	@Mapping(target = "legalMonetaryTotal.chargeTotalAmount.value", source = "inv.chargeTotalAmount")
	@Mapping(target = "legalMonetaryTotal.chargeTotalAmount.currencyID", source = "inv.documentCurrencyCode")
	@Mapping(target = "legalMonetaryTotal.prepaidAmount.value", source = "inv.prepaidAmount")
	@Mapping(target = "legalMonetaryTotal.prepaidAmount.currencyID", source = "inv.documentCurrencyCode")
	@Mapping(target = "legalMonetaryTotal.payableRoundingAmount.value", source = "inv.payableRoundingAmount")
	@Mapping(target = "legalMonetaryTotal.payableRoundingAmount.currencyID", source = "inv.documentCurrencyCode")
	@Mapping(target = "legalMonetaryTotal.payableAmount.value", source = "inv.payableAmount")
	@Mapping(target = "legalMonetaryTotal.payableAmount.currencyID", source = "inv.documentCurrencyCode")
	@Mapping(target = "note", source = "inv.note", qualifiedByName = "mapNoteTypes")
	@Mapping(target = "invoiceLine", source = "inv.lines")
	InvoiceType toUblInvoice(Invoice inv);
	
	@Mapping(target = "ID", source = "line.id")
	@Mapping(target = "note", source = "line.note", qualifiedByName = "mapNoteTypes")
	@Mapping(target = "invoicedQuantity.value", source = "line.quantity")
	@Mapping(target = "invoicedQuantity.unitCode", source = "line.uom")
	@Mapping(target = "lineExtensionAmount", source = "line.lineExtensionAmount")
	@Mapping(target = "item", source = "line")
	@Mapping(target = "allowanceCharge", source = "line.allowanceCharges")
	@Mapping(target = "price.priceAmount", source = "line.price")
	@Mapping(target = "price.baseQuantity.value", source = "line.baseQuantity")
	@Mapping(target = "price.baseQuantity.unitCode", source = "line.uom")
	InvoiceLineType toInvoiceLine(InvoiceLine line);
	
	@Mapping(target = "description", expression = "java(listOf(invoiceTypePrimitiveMapper.mapDescriptionType(line.getDescription())))")
	@Mapping(target = "name", source = "line.name")
	@Mapping(target = "buyersItemIdentification", source = "line.buyersItemIdentification")
	@Mapping(target = "sellersItemIdentification", source = "line.sellersItemIdentification")
	@Mapping(target = "classifiedTaxCategory", expression = "java(listOf(mapTaxCategoryType(line.getClassifiedTaxCategory())))")
	ItemType mapItemType(InvoiceLine line);
	
	@Mapping(target = "chargeIndicator", source = "allCh.chargeIndicator")
	@Mapping(target = "allowanceChargeReasonCode", source = "allCh.allowanceChargeReasonCode")
	@Mapping(target = "allowanceChargeReason", expression = "java(listOf(invoiceTypePrimitiveMapper.mapAllowanceChargeReasonType(allCh.getAllowanceChargeReason())))")
	@Mapping(target = "multiplierFactorNumeric", source = "allCh.multiplierFactorNumeric")
	@Mapping(target = "amount", source = "allCh.amount")
	@Mapping(target = "baseAmount", source = "allCh.baseAmount")
	AllowanceChargeType toAllowanceCharge(AllowanceCharge allCh);
	
	@Mapping(target = "party", source = "party")
	SupplierPartyType toSupplierParty(Party party);
	
	@Mapping(target = "party", source = "party")
	CustomerPartyType toCustomerParty(Party party);
	
	@Mapping(target = "partyTaxScheme", expression = "java(listOf(mapPartyTaxSchemeType(party.getTaxId())))")
	@Mapping(target = "partyName", expression = "java(listOf(invoiceTypePrimitiveMapper.mapPartyNameType(party.getBusinessName())))")
	@Mapping(target = "partyLegalEntity", expression = "java(listOf(mapPartyLegalEntityType(party)))")
	@Mapping(target = "postalAddress", source = "party.postalAddress")
	@Mapping(target = "contact.name", source = "party.contactName")
	@Mapping(target = "contact.telephone", source = "party.telephone")
	@Mapping(target = "contact.electronicMail", source = "party.electronicMail")
	PartyType toPartyType(Party party);
	
	@Mapping(target = "country.identificationCode", source = "address.country")
	@Mapping(target = "countrySubentity", source = "address.countrySubentity")
	@Mapping(target = "cityName", source = "address.city")
	@Mapping(target = "postalZone", source = "address.postalZone")
	@Mapping(target = "streetName", source = "address.primaryLine")
	@Mapping(target = "additionalStreetName", source = "address.secondaryLine")
	AddressType toAddressType(Address address);
	
	@Mapping(target = "registrationName", source = "party.registrationName")
	@Mapping(target = "companyID", source = "party.registrationId")
	@Mapping(target = "companyLegalForm", source = "party.companyLegalForm")
	PartyLegalEntityType mapPartyLegalEntityType(Party party);
	
	@Mapping(target = "companyID.value", source = "value")
	@Mapping(target = "taxScheme.ID", constant = "VAT")
	PartyTaxSchemeType mapPartyTaxSchemeType(String value);
	
	@Mapping(target = "paymentMeansCode", source = "inv.paymentMeansCode")
	@Mapping(target = "paymentID", expression = "java(listOf(invoiceTypePrimitiveMapper.mapPaymentIDType(inv.getPaymentId())))")
	@Mapping(target = "payeeFinancialAccount.ID", source = "inv.payeeFinancialAccount.id")
	@Mapping(target = "payeeFinancialAccount.name", source = "inv.payeeFinancialAccount.name")
	@Mapping(target = "payeeFinancialAccount.financialInstitutionBranch", source = "inv.payeeFinancialAccount.financialInstitutionBranch")
	@Mapping(target = "payeeFinancialAccount.currencyCode", source = "inv.payeeFinancialAccount.currency")
	PaymentMeansType mapPaymentMeansType(Invoice inv);
	
	@Mapping(target = "taxAmount.value", source = "inv.taxAmount")
	@Mapping(target = "taxAmount.currencyID", source = "inv.taxCurrencyCode")
	@Mapping(target = "taxSubtotal", source = "inv.taxSubtotals")
	TaxTotalType mapTaxTotalType(Invoice inv);
	
	@Mapping(target = "taxableAmount", source = "subtotal.taxableAmount")
	@Mapping(target = "taxAmount", source = "subtotal.taxAmount")
	@Mapping(target = "taxCategory", source = "subtotal.taxCategory")
	TaxSubtotalType mapTaxSubtotalType(TaxSubtotal subtotal);
	
	@Mapping(target = "ID", source = "taxCat.code")
	@Mapping(target = "percent.value", source = "taxCat.percent", qualifiedByName = "displayPercentage")
	@Mapping(target = "taxExemptionReason", expression = "java(listOf(invoiceTypePrimitiveMapper.mapTaxExemptionReasonType(taxCat.getTaxExemptionReason())))")
	@Mapping(target = "taxScheme.ID", source = "taxCat.taxScheme")
	TaxCategoryType mapTaxCategoryType(TaxCategory taxCat);
	
	@Named("displayPercentage")
	default BigDecimal displayPercentage(final BigDecimal percentage) {
        return percentage == null ? null : percentage.multiply(new BigDecimal("100"));
    }
	
	@Named("mapNoteTypes")
	default List<NoteType> mapNoteTypes(final String value) {
        return value == null ? null : List.of(new NoteType(value));
    }
	
	default <T> List<T> listOf(final T t) {
		if (t == null)
			return List.of();
		return List.of(t);
	}

	@AfterMapping
    default void enrichInvoiceTypeWithCurrency(final Invoice inv, @MappingTarget final InvoiceType invoiceType) {
        if (invoiceType == null)
        	return;
        
        // InvoiceType toUblInvoice(Invoice inv);
        // @Mapping(target = "allowanceCharge.amount.currencyID", source = "inv.documentCurrencyCode")
        invoiceType.getAllowanceCharge().stream()
        .map(AllowanceChargeType::getAmount)
        .filter(Objects::nonNull)
        .filter(t -> t.getValue() != null)
        .forEach(amount -> amount.setCurrencyID(inv.getDocumentCurrencyCode()));
        // InvoiceType toUblInvoice(Invoice inv);
        // @Mapping(target = "allowanceCharge.baseAmount.currencyID", source = "inv.documentCurrencyCode")
        invoiceType.getAllowanceCharge().stream()
        .map(AllowanceChargeType::getBaseAmount)
        .filter(Objects::nonNull)
        .filter(t -> t.getValue() != null)
        .forEach(amount -> amount.setCurrencyID(inv.getDocumentCurrencyCode()));
        
        // InvoiceLineType toInvoiceLine(InvoiceLine line);
        // @Mapping(target = "lineExtensionAmount.currencyID", source = "inv.documentCurrencyCode")
        invoiceType.getInvoiceLine().stream()
        .map(InvoiceLineType::getLineExtensionAmount)
        .filter(Objects::nonNull)
        .filter(t -> t.getValue() != null)
        .forEach(amount -> amount.setCurrencyID(inv.getDocumentCurrencyCode()));
        // InvoiceLineType toInvoiceLine(InvoiceLine line);
        // @Mapping(target = "price.priceAmount.currencyID", source = "inv.documentCurrencyCode")
        invoiceType.getInvoiceLine().stream()
        .map(InvoiceLineType::getPrice)
        .filter(Objects::nonNull)
        .map(PriceType::getPriceAmount)
        .filter(Objects::nonNull)
        .filter(t -> t.getValue() != null)
        .forEach(amount -> amount.setCurrencyID(inv.getDocumentCurrencyCode()));
        // InvoiceLineType toInvoiceLine(InvoiceLine line);
        // @Mapping(target = "taxTotal.taxAmount.currencyID", source = "inv.taxCurrencyCode")
        invoiceType.getInvoiceLine().stream()
        .flatMap(ilt -> ilt.getTaxTotal().stream())
        .filter(Objects::nonNull)
        .map(TaxTotalType::getTaxAmount)
        .filter(Objects::nonNull)
        .filter(t -> t.getValue() != null)
        .forEach(amount -> amount.setCurrencyID(inv.getTaxCurrencyCode()));
        // InvoiceLineType toInvoiceLine(InvoiceLine line);
        // @Mapping(target = "allowanceCharge.amount.currencyID", source = "inv.documentCurrencyCode")
        invoiceType.getInvoiceLine().stream()
        .flatMap(ilt -> ilt.getAllowanceCharge().stream())
        .filter(Objects::nonNull)
        .map(AllowanceChargeType::getAmount)
        .filter(Objects::nonNull)
        .filter(t -> t.getValue() != null)
        .forEach(amount -> amount.setCurrencyID(inv.getDocumentCurrencyCode()));
        // InvoiceLineType toInvoiceLine(InvoiceLine line);
        // @Mapping(target = "allowanceCharge.baseAmount.currencyID", source = "inv.documentCurrencyCode")
        invoiceType.getInvoiceLine().stream()
        .flatMap(ilt -> ilt.getAllowanceCharge().stream())
        .filter(Objects::nonNull)
        .map(AllowanceChargeType::getBaseAmount)
        .filter(Objects::nonNull)
        .filter(t -> t.getValue() != null)
        .forEach(amount -> amount.setCurrencyID(inv.getDocumentCurrencyCode()));
        
        // fix for legalMonetaryTotal amounts
        // if the amount is null, the currency should also be null
        if (invoiceType != null && invoiceType.getLegalMonetaryTotal() != null)
        {
        	removeCurrencyIfAmountNull(invoiceType.getLegalMonetaryTotal().getLineExtensionAmount());
        	removeCurrencyIfAmountNull(invoiceType.getLegalMonetaryTotal().getTaxExclusiveAmount());
        	removeCurrencyIfAmountNull(invoiceType.getLegalMonetaryTotal().getTaxInclusiveAmount());
        	removeCurrencyIfAmountNull(invoiceType.getLegalMonetaryTotal().getAllowanceTotalAmount());
        	removeCurrencyIfAmountNull(invoiceType.getLegalMonetaryTotal().getChargeTotalAmount());
        	removeCurrencyIfAmountNull(invoiceType.getLegalMonetaryTotal().getPrepaidAmount());
        	removeCurrencyIfAmountNull(invoiceType.getLegalMonetaryTotal().getPayableRoundingAmount());
        	removeCurrencyIfAmountNull(invoiceType.getLegalMonetaryTotal().getPayableAmount());
        }
    }
	
	@AfterMapping
    default void enrichTaxSubtotalTypeWithCurrency(final Invoice inv, @MappingTarget final TaxTotalType taxTotalType) {
        if (taxTotalType == null)
        	return;
        // TaxSubtotalType mapTaxSubtotalType(TaxSubtotal subtotal);
        // @Mapping(target = "taxableAmount.currencyID", source = "inv.taxCurrencyCode")
        taxTotalType.getTaxSubtotal().stream()
        .map(TaxSubtotalType::getTaxableAmount)
        .filter(Objects::nonNull)
        .filter(t -> t.getValue() != null)
        .forEach(taxableAmount -> taxableAmount.setCurrencyID(inv.getTaxCurrencyCode()));
        // @Mapping(target = "taxAmount.currencyID", source = "inv.taxCurrencyCode")
        taxTotalType.getTaxSubtotal().stream()
        .map(TaxSubtotalType::getTaxAmount)
        .filter(Objects::nonNull)
        .filter(t -> t.getValue() != null)
        .forEach(taxAmount -> taxAmount.setCurrencyID(inv.getTaxCurrencyCode()));
    }
	
	@AfterMapping
    default void enrichPartyTaxIdWithCountryCode(@MappingTarget final PartyTaxSchemeType partyTaxSchemeType) {
        if (partyTaxSchemeType == null)
        	return;
        if (partyTaxSchemeType.getCompanyID() == null)
            return;

        final CompanyIDType companyIDType = partyTaxSchemeType.getCompanyID();
        final String companyIDValue = companyIDType.getValue();
        
        if (StringUtils.isNotBlank(companyIDValue))
        	companyIDType.setValue(Character.isDigit(companyIDValue.charAt(0)) ? ECountryIdentificationCode21.RO.getID()+companyIDValue : companyIDValue);
	}
}
