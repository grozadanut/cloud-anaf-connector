package ro.linic.cloud.mapper;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PriceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.AllowanceTotalAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.BaseQuantityType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.ChargeTotalAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.ElectronicMailType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.InvoicedQuantityType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.LineExtensionAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.NameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.PayableAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.PayableRoundingAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.PrepaidAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxExclusiveAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxInclusiveAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TelephoneType;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.AmountType;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.QuantityType;
import ro.linic.cloud.pojo.Invoice;
import ro.linic.cloud.pojo.InvoiceLine;
import ro.linic.cloud.pojo.Party;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceDeleteEmptyMapper {
	
	private static <T> void deleteIfEmpty(final AmountType amount, final Consumer<T> deleteCallback) {
		if (amount == null)
			return;
		
		if (amount.getValue() == null)
			deleteCallback.accept(null);
	}
	
	private static <T> void deleteIfEmpty(final QuantityType quantity, final Consumer<T> deleteCallback) {
		if (quantity == null)
			return;
		
		if (quantity.getValue() == null)
			deleteCallback.accept(null);
	}

	@AfterMapping
    default void mapTaxTotalType_deleteIfEmpty(final Invoice inv, @MappingTarget final TaxTotalType taxTotalType) {
        deleteIfEmpty(taxTotalType.getTaxAmount(), (Consumer<TaxAmountType>) taxTotalType::setTaxAmount);
	}
	
	@AfterMapping
    default void invoiceToMonetaryTotalType_deleteIfEmpty(final Invoice inv, @MappingTarget final MonetaryTotalType monetaryTotalType) {
		deleteIfEmpty(monetaryTotalType.getLineExtensionAmount(),
				(Consumer<LineExtensionAmountType>) monetaryTotalType::setLineExtensionAmount);
		deleteIfEmpty(monetaryTotalType.getTaxExclusiveAmount(),
				(Consumer<TaxExclusiveAmountType>) monetaryTotalType::setTaxExclusiveAmount);
		deleteIfEmpty(monetaryTotalType.getTaxInclusiveAmount(),
				(Consumer<TaxInclusiveAmountType>) monetaryTotalType::setTaxInclusiveAmount);
		deleteIfEmpty(monetaryTotalType.getAllowanceTotalAmount(),
				(Consumer<AllowanceTotalAmountType>) monetaryTotalType::setAllowanceTotalAmount);
		deleteIfEmpty(monetaryTotalType.getChargeTotalAmount(),
				(Consumer<ChargeTotalAmountType>) monetaryTotalType::setChargeTotalAmount);
		deleteIfEmpty(monetaryTotalType.getPrepaidAmount(),
				(Consumer<PrepaidAmountType>) monetaryTotalType::setPrepaidAmount);
		deleteIfEmpty(monetaryTotalType.getPayableRoundingAmount(),
				(Consumer<PayableRoundingAmountType>) monetaryTotalType::setPayableRoundingAmount);
		deleteIfEmpty(monetaryTotalType.getPayableAmount(),
				(Consumer<PayableAmountType>) monetaryTotalType::setPayableAmount);
	}
	
	@AfterMapping
    default void toInvoiceLine_deleteIfEmpty(final InvoiceLine line, @MappingTarget final InvoiceLineType invoiceLineType) {
		deleteIfEmpty(invoiceLineType.getInvoicedQuantity(),
				(Consumer<InvoicedQuantityType>) invoiceLineType::setInvoicedQuantity);
	}
	
	@AfterMapping
    default void invoiceLineToPriceType_deleteIfEmpty(final InvoiceLine line, @MappingTarget final PriceType priceType) {
		deleteIfEmpty(priceType.getBaseQuantity(),
				(Consumer<BaseQuantityType>) priceType::setBaseQuantity);
	}
	
	@AfterMapping
    default void toPartyType_deleteIfEmpty(final Party party, @MappingTarget final PartyType partyType) {
		if (partyType.getContact() != null)
		{
			if (partyType.getContact().getName() != null &&
					StringUtils.isBlank(partyType.getContact().getNameValue()))
				partyType.getContact().setName((NameType)null);
			
			if (partyType.getContact().getTelephone() != null &&
					StringUtils.isBlank(partyType.getContact().getTelephoneValue()))
				partyType.getContact().setTelephone((TelephoneType)null);
			
			if (partyType.getContact().getElectronicMail() != null &&
					StringUtils.isBlank(partyType.getContact().getElectronicMailValue()))
				partyType.getContact().setElectronicMail((ElectronicMailType)null);
		}
	}
}
