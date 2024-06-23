package ro.linic.cloud.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
public class CompanyOAuthToken {
	@Id private Integer companyId;
	@NotNull private String taxId;
	@NotNull private String clientRegistrationId;
	@NotNull private String principalName;
}
