package com.game013.codeanalyzer.model;

import com.game013.codeanalyzer.constant.ParticipantType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

	private static final String JAVA = "Java";

	private static final String CPP = "C++";

	@Id
	private Long id;

	@NotNull
	private Integer contestId;

	@NotNull
	private String handle;

	@NotNull
	@Enumerated(EnumType.STRING)
	private ParticipantType participantType;

	@NotNull
	private LocalDateTime deliveryDate;

	@NotNull
	private String programmingLanguage;

	@NotNull
	private String problemIndex;

	@NotNull
	private Integer rating;

	private String sourceCode;

	@NotNull
	private Boolean download;

}
