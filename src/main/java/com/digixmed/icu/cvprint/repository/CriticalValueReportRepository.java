package com.digixmed.icu.cvprint.repository;

import com.digixmed.icu.cvprint.entity.CriticalValueReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * SmartCare.criticalValueReport 登记表编辑结果仓储
 */
public interface CriticalValueReportRepository extends MongoRepository<CriticalValueReport, String> {

    List<CriticalValueReport> findBySourceIdIn(List<String> sourceIds);
}
