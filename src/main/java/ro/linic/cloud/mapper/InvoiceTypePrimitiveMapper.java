package ro.linic.cloud.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.mapstruct.Condition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.helger.commons.datetime.XMLOffsetDate;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.BranchType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ItemIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.AdditionalStreetNameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.AllowanceChargeReasonCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.AllowanceChargeReasonType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.AmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.BaseAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.ChargeIndicatorType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.CityNameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.CompanyIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.CompanyLegalFormType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.CountrySubentityType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.CurrencyCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.DescriptionType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.DocumentCurrencyCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.DueDateType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.ElectronicMailType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.IDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.IdentificationCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.IssueDateType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.LineExtensionAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.MultiplierFactorNumericType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.NameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.PaymentIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.PaymentMeansCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.PostalZoneType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.PriceAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.RegistrationNameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.StreetNameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxCurrencyCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxExemptionReasonType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxableAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TelephoneType;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceTypePrimitiveMapper {
	InvoiceTypePrimitiveMapper INSTANCE = Mappers.getMapper(InvoiceTypePrimitiveMapper.class);
	
	IDType mapIDType(String value);
	IssueDateType mapIssueDateType(Instant value);
	DueDateType mapDueDateType(Instant value);
	DocumentCurrencyCodeType mapDocumentCurrencyCodeType(String value);
	TaxCurrencyCodeType mapTaxCurrencyCodeType(String value);
	PaymentMeansCodeType mapPaymentMeansCodeType(String value);
	NameType mapNameType(String value);
	CurrencyCodeType mapCurrencyCodeType(String value);
	@Mapping(target = "ID", source = "id")
	BranchType mapBranchType(String id);
	@Mapping(target = "name.value", source = "value")
	PartyNameType mapPartyNameType(String value);
	PaymentIDType mapPaymentIDType(String value);
	TaxExemptionReasonType mapTaxExemptionReasonType(String value);
	DescriptionType mapDescriptionType(String value);
	AllowanceChargeReasonType mapAllowanceChargeReasonType(String value);
	TaxableAmountType mapTaxableAmountType(BigDecimal value);
	TaxAmountType mapTaxAmountType(BigDecimal value);
	ChargeIndicatorType mapChargeIndicatorType(Boolean value);
	AllowanceChargeReasonCodeType mapAllowanceChargeReasonCodeType(String value);
	MultiplierFactorNumericType mapMultiplierFactorNumericType(BigDecimal value);
	AmountType mapAmountType(BigDecimal value);
	BaseAmountType mapBaseAmountType(BigDecimal value);
	RegistrationNameType mapRegistrationNameType(String value);
	CompanyIDType mapCompanyIDType(String value);
	CompanyLegalFormType mapCompanyLegalFormType(String value);
	IdentificationCodeType mapIdentificationCodeType(String value);
	CountrySubentityType mapCountrySubentityType(String value);
	CityNameType mapCityNameType(String value);
	PostalZoneType mapPostalZoneType(String value);
	StreetNameType mapStreetNameType(String value);
	AdditionalStreetNameType mapAdditionalStreetNameType(String value);
	TelephoneType mapTelephoneType(String value);
	ElectronicMailType mapElectronicMailType(String value);
	@Mapping(target = "ID", source = "id")
	ItemIdentificationType mapItemIdentificationType(String id);
	PriceAmountType mapPriceAmountType(BigDecimal value);
	LineExtensionAmountType mapLineExtensionAmountType(BigDecimal value);
	
	default XMLOffsetDate mapXMLOffsetDate(final Instant value) {
        return value == null ? null : XMLOffsetDate.of(LocalDate.ofInstant(value, ZoneId.of("Europe/Bucharest")));
    }
	
	@Condition
	default boolean isNotEmpty(final String value) {
		return value != null && !value.isBlank();
	}
}
