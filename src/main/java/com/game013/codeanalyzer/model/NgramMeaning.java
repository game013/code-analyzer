package com.game013.codeanalyzer.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NgramMeaning {

	@Id
	private String id;

	private String biGram;

	private String triGram;

	private String fourGram;

	private String fiveGram;

}
