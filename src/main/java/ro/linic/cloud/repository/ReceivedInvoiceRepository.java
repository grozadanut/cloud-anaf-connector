package ro.linic.cloud.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.repository.CrudRepository;

import ro.linic.cloud.entity.ReceivedInvoice;

@JaversSpringDataAuditable
public interface ReceivedInvoiceRepository extends CrudRepository<ReceivedInvoice, Long> {
	List<ReceivedInvoice> findAllByIssueDateBetween(LocalDate start, LocalDate end);
	List<ReceivedInvoice> findByInvoiceIdIn(Collection<Long> invoiceIds);
}
