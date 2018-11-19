package fishing.lee.backend;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.xray.AWSXRay;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static fishing.lee.backend.DynamoDBConfig.checkOrCreateTable;

@SpringBootApplication
@EnableDynamoDBRepositories
@Configuration
@Import(DynamoDBConfig.class)
@PropertySource("classpath:application-lambda.properties")
public class Application extends SpringBootServletInitializer {

    @Value("${logging.level.root:OFF}")
    String message = "";

    /**
     * Main entry point of the application
     *
     * @param args list of arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Bootstrap DynamoDB if required on startup
     *
     * @param ctx Application Context (unused here)
     * @param amazonDynamoDB Autowired DynamoDB instance
     * @param dynamoDBMapper Autowired DynamoDBMapper instance
     * @return CommandLineRunner inline function
     */
    @Bean
    public CommandLineRunner custom(ConfigurableApplicationContext ctx, AmazonDynamoDB amazonDynamoDB, DynamoDBMapper dynamoDBMapper, Boolean amazonAWSUseLocalDynamoDB) {
        return (args) -> {
            if (amazonAWSUseLocalDynamoDB)
                AWSXRay.beginSegment("custom");

            checkOrCreateTable(amazonDynamoDB, dynamoDBMapper, BasketModel.class);

            if (amazonAWSUseLocalDynamoDB)
                AWSXRay.endSegment();
        };
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*");
            }
        };
    }
}
