package br.com.casellisoftware.budgetmanager.configs;

import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class TransactionConfiguration {

    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    public SmartInitializingSingleton mongoTransactionsReplicaSetGuard(MongoClient mongoClient) {
        return () -> {
            Document hello = mongoClient.getDatabase("admin").runCommand(new Document("hello", 1));
            boolean isReplicaSet = hello.containsKey("setName");
            boolean isShardedOrCosmos = "isdbgrid".equals(hello.getString("msg"));
            if (!isReplicaSet && !isShardedOrCosmos) {
                throw new IllegalStateException(
                        "Mongo multi-document transactions require a replica set or sharded cluster. "
                                + "Current deployment does not advertise setName or isdbgrid."
                );
            }
        };
    }
}
