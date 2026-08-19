package com.digixmed.icu.cvprint.config;

import com.digixmed.icu.cvprint.entity.CriticalValueReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * 启动时确保登记表集合 criticalValueReport 存在（不存在则新建），并建好索引。
 */
@Component
public class MongoIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    @Value("${critical-value.report-collection:criticalValueReport}")
    private String reportCollection;

    public MongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mongoTemplate.collectionExists(reportCollection)) {
            mongoTemplate.createCollection(reportCollection);
            log.info("已自动创建集合：{}", reportCollection);
        }
        mongoTemplate.indexOps(CriticalValueReport.class)
                .ensureIndex(new Index().on("sourceId", Sort.Direction.ASC).unique().named("uk_sourceId"));
        mongoTemplate.indexOps(CriticalValueReport.class)
                .ensureIndex(new Index().on("pid", Sort.Direction.ASC)
                        .on("publishTime", Sort.Direction.DESC).named("idx_pid_publishTime"));
        log.info("criticalValueReport 索引已就绪");
    }
}
