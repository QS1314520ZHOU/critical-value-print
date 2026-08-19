package com.digixmed.icu.cvprint.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB 连接配置。
 *
 * 连接串在 application.yml 的 spring.data.mongodb.uri 中配置，例如：
 *   mongodb://192.168.5.154:27017/SmartCare
 *   mongodb://user:pwd@192.168.5.154:27017/SmartCare?authSource=admin
 *
 * 这里额外做了两件事：
 *  1. 设置连接池、超时等参数；
 *  2. 去掉写入文档时的 _class 字段，保持集合数据干净。
 */
@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(50)
                        .minSize(5)
                        .maxWaitTime(10, TimeUnit.SECONDS))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS))
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        String database = connectionString.getDatabase();
        if (database == null || database.isEmpty()) {
            database = "SmartCare";
        }
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory factory, MongoMappingContext context) {
        MappingMongoConverter converter =
                new MappingMongoConverter(new DefaultDbRefResolver(factory), context);
        // 不写入 _class 字段
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return new MongoTemplate(factory, converter);
    }
}
