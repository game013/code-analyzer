package com.game013.codeanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metric {

	@Id
	@NotNull
	private Long submissionId;

	@NotNull
	@Builder.Default
	private Boolean completed = Boolean.TRUE;

	@NotNull
	private Boolean requirePreProcessing;

	private Double astNodeCount;

	private Double avgFunctionLength;

	private Double avgFunctionParameterCount;

	private Double avgIdentifierCount;

	private Double avgIdentifierLength;

	private Double avgLineLength;

	private Double avgPeriodCount;

	private Double avgParenthesisCount;

	private Double branchFactor;

	private Double cyclomaticComplexity;

	private Double entropy;

	private Double halsteadDifficulty;

	private Double halsteadEffort;

	private Double halsteadVolume;

	private Double keywordDistribution;

	private Double linesOfCode;

	private Double lnKeywordFileLength;

	private Double lnLiteralCountFileLength;

	private Double maxDepthNestedBlocks;

	private Double maxIdentifierCount;

	private Double maxIdentifierLength;

	private Double maxLineLength;

	private Double posnettReadabilityMetric;

	private String biGram;

	private String triGram;

	private String quadriGram;

	private String quinqueGram;

	public boolean isValid() {

		boolean result = true;
		for (Field field : Metric.class.getDeclaredFields()) {
			try {
				Object value = field.get(this);
				if (value instanceof Double) {
					if (Double.isInfinite((Double) value) || Double.isNaN((Double) value)) {
						result = false;
						break;
					}
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

}
