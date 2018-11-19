package fishing.lee.backend;

import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DynamoDBConfig {
    private static final Logger log = LoggerFactory.getLogger(DynamoDBConfig.class);

    @Value("${amazon.aws.mode")
    private String amazonMode = "local";

    private AWSCredentialsProvider amazonAWSCredentialsProvider() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials("nothing", "nothing"));
    }

    private AWSCredentialsProvider amazonEC2CredentialsProvider() {
        return InstanceProfileCredentialsProvider.getInstance();
    }

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        switch (amazonMode) {
            case "local":
                return AmazonDynamoDBClientBuilder.standard()
                        .withCredentials(amazonAWSCredentialsProvider())
                        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8001", "us-west-2"))
                        .build();
            case "beanstalk":
                return AmazonDynamoDBClientBuilder.standard()
                        .withCredentials(amazonEC2CredentialsProvider())
                        .build();
        }

        return AmazonDynamoDBClientBuilder.standard().build();
    }

    @Bean
    public DynamoDBMapper dynamoDBMapper() {
        return new DynamoDBMapper(amazonDynamoDB());
    }

    static void checkOrCreateTable(AmazonDynamoDB amazonDynamoDB, DynamoDBMapper mapper,
                                   Class<?> entityClass) {
        String tableName = entityClass.getAnnotation(DynamoDBTable.class).tableName();

        try {
            amazonDynamoDB.describeTable(tableName);

            log.info("Table {} found", tableName);
            return;
        } catch (ResourceNotFoundException rnfe) {
            log.warn("Table {} doesn't exist - Creating", tableName);
        }

        CreateTableRequest ctr = mapper.generateCreateTableRequest(entityClass);
        ProvisionedThroughput pt = new ProvisionedThroughput(1L, 1L);
        ctr.withProvisionedThroughput(pt);
        List<GlobalSecondaryIndex> gsi = ctr.getGlobalSecondaryIndexes();
        if (gsi != null) {
            gsi.forEach(aGsi -> aGsi.withProvisionedThroughput(pt));
        }

        amazonDynamoDB.createTable(ctr);
        waitForDynamoDBTable(amazonDynamoDB, tableName);
    }

    private static void waitForDynamoDBTable(AmazonDynamoDB amazonDynamoDB, String tableName) {
        do {
            try {
                Thread.sleep(5 * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException("Couldn't wait detect table " + tableName);
            }
        } while (!amazonDynamoDB.describeTable(tableName).getTable().getTableStatus()
                .equals(TableStatus.ACTIVE.name()));
    }
}
