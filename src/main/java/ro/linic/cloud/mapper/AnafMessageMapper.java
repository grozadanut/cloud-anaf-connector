package ro.linic.cloud.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import ro.linic.cloud.entity.ReceivedMessage;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage;

@Mapper
public interface AnafMessageMapper {
	AnafMessageMapper INSTANCE = Mappers.getMapper(AnafMessageMapper.class);
	
	ReceivedMessage toEntity(AnafReceivedMessage message);
}
