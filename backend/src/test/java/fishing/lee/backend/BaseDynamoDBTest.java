package fishing.lee.backend;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.amazonaws.xray.AWSXRay;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static fishing.lee.backend.DynamoDBConfig.checkOrCreateTable;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "amazon.aws.mode=local"
})
public abstract class BaseDynamoDBTest {
    private static DynamoDBProxyServer serverRunner;

    @Autowired
    protected AmazonDynamoDB amazonDynamoDB;

    @Autowired
    protected BasketRepository basketRepository;

    @Autowired
    protected DynamoDBMapper dynamoDBMapper;

    @BeforeClass
    public static void runDynamoDB() {
        System.setProperty("sqlite4java.library.path", "./build/libs/");

        final String[] localArgs = { "-inMemory", "-port", "8001" };

        try {
            serverRunner = ServerRunner.createServerFromCommandLineArgs(localArgs);
            serverRunner.start();

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public static void shutdownDynamoDB() {
        if (serverRunner != null) {
            try {
                serverRunner.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Before
    public void setup() {
        AWSXRay.beginSegment("BasketRepositoryIntegrationTest");

        checkOrCreateTable(amazonDynamoDB, dynamoDBMapper, BasketModel.class);

        dynamoDBMapper.batchDelete(
                basketRepository.findAll()
        );
    }

    @After
    public void tearDown() {
        AWSXRay.endSegment();
    }

}
