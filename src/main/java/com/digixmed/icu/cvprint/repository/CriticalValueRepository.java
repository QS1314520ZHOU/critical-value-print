package com.digixmed.icu.cvprint.repository;

import com.digixmed.icu.cvprint.entity.CriticalValue;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * SmartCare.criticalValue 只读仓储
 */
public interface CriticalValueRepository extends MongoRepository<CriticalValue, String> {
}
