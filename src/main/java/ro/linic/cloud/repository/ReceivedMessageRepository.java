package ro.linic.cloud.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.repository.CrudRepository;

import ro.linic.cloud.entity.ReceivedMessage;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage.AnafReceivedMessageType;

@JaversSpringDataAuditable
public interface ReceivedMessageRepository extends CrudRepository<ReceivedMessage, Long> {
	List<ReceivedMessage> findAllByCreationDateBetweenAndMessageTypeIn(LocalDateTime start, LocalDateTime end, 
			Collection<AnafReceivedMessageType> messageType);
	ReceivedMessage findFirstByOrderByCreationDateDesc();
}
