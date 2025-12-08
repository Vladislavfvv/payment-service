package com.innowise.paymentservice.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class LiquibaseConfig {

    private final MongoTemplate mongoTemplate;
    private final MongoClient mongoClient;

    @Value("${spring.liquibase.change-log:classpath:/db/changelog/master-changelog.yaml}")
    private String changeLogFile;

    @Value("${spring.liquibase.enabled:true}")
    private boolean liquibaseEnabled;

    public LiquibaseConfig(MongoTemplate mongoTemplate, MongoClient mongoClient) {
        this.mongoTemplate = mongoTemplate;
        this.mongoClient = mongoClient;
    }

    @PostConstruct
    public void runLiquibase() {
        if (!liquibaseEnabled) {
            log.info("Liquibase is disabled");
            return;
        }

        try {
            log.info("Starting Liquibase migration for MongoDB with changelog: {}", changeLogFile);
            
            String databaseName = mongoTemplate.getDb().getName();
            MongoDatabase mongoDatabase = mongoClient.getDatabase(databaseName);
            
            // Создаем Liquibase MongoDB connection
            MongoConnection mongoConnection = new MongoConnection();
            mongoConnection.setMongoDatabase(mongoDatabase);
            
            Database liquibaseDatabase = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(mongoConnection);
            
            liquibaseDatabase.setDefaultSchemaName(databaseName);
            
            // Создаем Liquibase и выполняем миграции
            try (Liquibase liquibase = new Liquibase(
                    changeLogFile,
                    new ClassLoaderResourceAccessor(),
                    liquibaseDatabase
            )) {
                liquibase.update("");
                log.info("Liquibase migration completed successfully for database: {}", databaseName);
            }
        } catch (LiquibaseException e) {
            log.error("Liquibase migration failed", e);
            throw new RuntimeException("Failed to run Liquibase migration", e);
        } catch (Exception e) {
            log.error("Unexpected error during Liquibase migration", e);
            throw new RuntimeException("Failed to run Liquibase migration", e);
        }
    }
}

