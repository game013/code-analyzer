package com.game013.codeanalyzer.repository;

import com.game013.codeanalyzer.model.NgramMeaning;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NgramMeaningRepository extends JpaRepository<NgramMeaning, String> {

}
