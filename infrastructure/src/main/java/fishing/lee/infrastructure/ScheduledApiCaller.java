package fishing.lee.infrastructure;

import software.amazon.awscdk.Construct;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.events.EventRule;
import software.amazon.awscdk.services.events.EventRuleProps;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;

import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketRef;
import software.amazon.awscdk.services.s3.BucketRefProps;
import software.amazon.awscdk.services.sns.*;

import java.util.Collections;
import java.util.HashMap;

class ScheduledApiCaller extends Construct {
    private SecurityGroup securityGroup;

    ScheduledApiCaller(Construct parent, String id, ScheduledApiCallerProps properties) {
        super(parent, id);

        BucketRef bucket = Bucket.import_(this, "Bucket", BucketRefProps.builder()
                .withBucketName("leepac-fishing-assets")
                .build());

        securityGroup = new SecurityGroup(this, "SecurityGroup", SecurityGroupProps.builder()
                .withVpc(properties.getShopVpc().getVpc())
                .build());

        properties.getShopVpc().addSecurityGroupToApi(securityGroup);

        Function function = new Function(this, "Function", FunctionProps.builder()
                .withCode(Code.bucket(bucket, "dummy-1.0-SNAPSHOT-all.jar"))
                .withHandler("fishing.lee.backend.SNSLambdaHandler::handleRequest")
                .withMemorySize(1024)
                .withTimeout(60)
                .withTracing(Tracing.Active)
                .withRuntime(Runtime.JAVA8)
                .withVpc(properties.getShopVpc().getVpc())
                .withVpcPlacement(VpcPlacementStrategy.builder()
                        .withSubnetsToUse(SubnetType.Private)
                        .build())
                .withSecurityGroup(securityGroup)
                .withEnvironment(new HashMap<String, Object>() {{
                    put("SHOPBACKEND_API_URL", properties.getApiToPing() + "/ping");
                }})
                .build());

        Topic topic = new Topic(this, "Topic", TopicProps.builder()
                .build());

        topic.subscribeLambda(function);

        new EventRule(this, "EventRule", EventRuleProps.builder()
                .withTargets(Collections.singletonList(topic))
                .withScheduleExpression("rate(2 minutes)")
                .build());
    }

    public SecurityGroup getSecurityGroup() {
        return securityGroup;
    }
}
