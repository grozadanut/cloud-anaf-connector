package ro.linic.cloud.repository;

import java.time.LocalDate;
import java.util.List;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.repository.CrudRepository;

import ro.linic.cloud.entity.ReceivedCreditNote;

@JaversSpringDataAuditable
public interface ReceivedCreditNoteRepository extends CrudRepository<ReceivedCreditNote, Long> {
	List<ReceivedCreditNote> findAllByIssueDateBetween(LocalDate start, LocalDate end);
}
