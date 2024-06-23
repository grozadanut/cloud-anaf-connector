package ro.linic.cloud.repository;

import java.util.List;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.repository.CrudRepository;

import ro.linic.cloud.entity.ReportedInvoice;
import ro.linic.cloud.entity.ReportedInvoice.ReportState;

@JaversSpringDataAuditable
public interface ReportedInvoiceRepository extends CrudRepository<ReportedInvoice, Long> {
	List<ReportedInvoice> findByState(ReportState state);
}
