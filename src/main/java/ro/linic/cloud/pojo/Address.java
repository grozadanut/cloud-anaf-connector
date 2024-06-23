package ro.linic.cloud.pojo;

import lombok.Data;

@Data
public class Address {
	/**
	 * ISO 3166-1:Alpha2 Country codes
	 * Example value: GB
	 */
	private String country;
	/**
	 * The subdivision of a country.
	 * Example value: RO-BH
	 * <a href="https://ro.wikipedia.org/wiki/ISO_3166-2:RO">https://ro.wikipedia.org/wiki/ISO_3166-2:RO<a>
	 */
	private String countrySubentity;
	private String city;
	private String postalZone;
	private String primaryLine;
	private String secondaryLine;
}
