package com.game013.codeanalyzer.repository;

import com.game013.codeanalyzer.model.Metric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricRepository extends JpaRepository<Metric, Long> {

}
