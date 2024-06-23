package ro.linic.cloud.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.repository.CrudRepository;

import ro.linic.cloud.entity.CompanyOAuthToken;

@JaversSpringDataAuditable
public interface CompanyOAuthTokenRepository extends CrudRepository<CompanyOAuthToken, Integer> {

}
