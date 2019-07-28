package io.penguinstats.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@Document(collection = "zone")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Zone {

	@Id
	@JsonIgnore
	private ObjectId id;
	@Indexed
	private String zoneId;
	private Integer zoneIndex;
	private String type;
	private String zoneName;
	private Long openTime;
	private Long closeTime;
	private List<String> stages;

	public Zone(String zoneId, Integer zoneIndex, String type, String zoneName, Long openTime, Long closeTime,
			List<String> stages) {
		this.zoneId = zoneId;
		this.zoneIndex = zoneIndex;
		this.type = type;
		this.zoneName = zoneName;
		this.openTime = openTime;
		this.closeTime = closeTime;
		this.stages = stages;
	}
}
