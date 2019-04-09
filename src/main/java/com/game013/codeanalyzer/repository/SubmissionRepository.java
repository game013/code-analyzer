package com.game013.codeanalyzer.repository;

import com.game013.codeanalyzer.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

	@Query("SELECT s FROM Submission s LEFT JOIN Metric m ON m.submissionId = s.id WHERE s.sourceCode IS NOT NULL AND" +
			" s.download = true AND s.programmingLanguage like '%C++%' AND s.problemIndex = ?1 AND m.submissionId IS " +
			"NULL ")
	List<Submission> findCppCode(String problemIndex);

	@Query("SELECT s FROM Submission s WHERE s.sourceCode IS NOT NULL AND" +
			" s.download = true AND s.programmingLanguage like '%C++%' ORDER BY s.id")
	List<Submission> findCppCode();

	@Query("SELECT s FROM Submission s LEFT JOIN Metric m ON m.submissionId = s.id WHERE s.sourceCode IS NOT NULL AND" +
			" s.download = true AND m.completed = true ORDER BY s.id")
	List<Submission> findCompletedCode();

}
