package fishing.lee.infrastructure;

import software.amazon.awscdk.CloudFormationToken;
import software.amazon.awscdk.Construct;
import software.amazon.awscdk.Output;
import software.amazon.awscdk.OutputProps;
import software.amazon.awscdk.services.apigateway.cloudformation.RestApiResource;
import software.amazon.awscdk.services.apigateway.cloudformation.RestApiResourceProps;
import software.amazon.awscdk.services.apigateway.cloudformation.StageResource;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.cloudformation.EventSourceMappingResource;
import software.amazon.awscdk.services.lambda.cloudformation.EventSourceMappingResourceProps;
import software.amazon.awscdk.services.lambda.cloudformation.PermissionResource;
import software.amazon.awscdk.services.lambda.cloudformation.PermissionResourceProps;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketRef;
import software.amazon.awscdk.services.s3.BucketRefProps;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueProps;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

class QueueProxy extends Construct {
    QueueProxy(Construct parent, String id, QueueProxyProps properties) {
        super(parent, id);

        Queue queue = new Queue(this, "Queue", QueueProps.builder()
                .build());

        BucketRef bucket = Bucket.import_(this, "ImportedBucket", BucketRefProps.builder()
                .withBucketName("leepac-fishing-assets")
                .build());

        Role receiverRole = new Role(this, "QueueReceiverRole", RoleProps.builder()
                .withManagedPolicyArns(Arrays.asList(
                        "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole",
                        "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole",
                        "arn:aws:iam::aws:policy/AWSXrayFullAccess",
                        "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
                ))
                .withAssumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .build());

        SecurityGroup securityGroup = new SecurityGroup(this, "QueueReceiverSecurityGroup", SecurityGroupProps.builder()
                .withVpc(properties.getShopVpc().getVpc())
                .build());
        properties.getShopVpc().addSecurityGroupToApi(securityGroup);

        Function dummyQueueReceiverFunction = new Function(this, "QueueReceiver", FunctionProps.builder()
                .withRole(receiverRole)
                .withCode(Code.bucket(bucket, "dummy-1.0-SNAPSHOT-all.jar"))
                .withHandler("fishing.lee.sqsforwarder.OrderForwarder::handleRequest")
                .withMemorySize(512)
                .withTimeout(30)
                .withTracing(Tracing.Active)
                .withRuntime(Runtime.JAVA8)
                .withVpc(properties.getShopVpc().getVpc())
                .withVpcPlacement(VpcPlacementStrategy.builder()
                        .withSubnetsToUse(SubnetType.Private)
                        .build())
                .withSecurityGroup(securityGroup)
                .withEnvironment(new HashMap<String, Object>() {{
                    put("SHOPBACKEND_ORDER_URL", properties.getApiUrl());
                }})
                .withDescription("SQS to API")
                .build());

        new EventSourceMappingResource(this, "LambdaSQSMapping", EventSourceMappingResourceProps.builder()
                .withFunctionName(dummyQueueReceiverFunction.getFunctionName())
                .withEventSourceArn(queue.getQueueArn())
                .build());

        Role forwarderRole = new Role(this, "OrderToSQSRole", RoleProps.builder()
                .withManagedPolicyArns(Arrays.asList(
                        "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole",
                        "arn:aws:iam::aws:policy/AWSXrayFullAccess",
                        "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
                ))
                .withAssumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .build());

        queue.grantSendMessages(forwarderRole);

        Function dummyApiReceiverToSqs = new Function(this, "OrderToSQS", FunctionProps.builder()
                .withRole(forwarderRole)
                .withCode(Code.bucket(bucket, "dummy-1.0-SNAPSHOT-all.jar"))
                .withHandler("fishing.lee.sqsforwarder.QueueGrabber::handleRequest")
                .withMemorySize(512)
                .withTimeout(30)
                .withTracing(Tracing.Active)
                .withRuntime(Runtime.JAVA8)
                .withEnvironment(new HashMap<String, Object>() {{
                    put("SHOPBACKEND_SQS_QUEUE", queue.getQueueName());
                }})
                .withDescription("API To SQS")
                .build());

        new PermissionResource(this, "OrderToSQSPermission", PermissionResourceProps.builder()
                .withFunctionName(dummyApiReceiverToSqs.getFunctionName())
                .withPrincipal("sqs.amazonaws.com")
                .withSourceArn(queue.getQueueArn())
                .withAction("lambda:invokeFunction")
                .build());

        RestApiResource restApiResource = new RestApiResource(this, "RestApi", RestApiResourceProps.builder()
                .withName("SqsTroutApi")
                .withDescription("Artifishial Intelligence ordering API (via SQS)")
                .withEndpointConfiguration(RestApiResource.EndpointConfigurationProperty.builder()
                        .withTypes(Collections.singletonList("PRIVATE"))
                        .build())
                .withPolicy(new CloudFormationToken(new ApiGatewayVpcPolicyDocument(properties.getShopVpc().getApiEndpoint()).getPolicyDocument()))
                .build());

        RestApiResource edgeApiResource = new RestApiResource(this, "EdgeApi", RestApiResourceProps.builder()
                .withName("SqsTroutApiEdge")
                .withDescription("Artifishial Intelligence ordering API @ Edge (via SQS)")
                .withEndpointConfiguration(RestApiResource.EndpointConfigurationProperty.builder()
                        .withTypes(Collections.singletonList("EDGE"))
                        .build())
                .build());

        StageResource stageResource = ApiGatewayStageGenerator.configureLambda(restApiResource, dummyApiReceiverToSqs);
        StageResource edgeStageResource = ApiGatewayStageGenerator.configureLambda(edgeApiResource, dummyApiReceiverToSqs);

        new Output(this, "ToSQSFunctionArn", OutputProps.builder()
                .withValue(dummyApiReceiverToSqs.getFunctionArn())
                .build());

        new Output(this, "ToSQSFunctionName", OutputProps.builder()
                .withValue(dummyApiReceiverToSqs.getFunctionName())
                .build());

        new Output(this, "FromSQSFunctionArn", OutputProps.builder()
                .withValue(dummyQueueReceiverFunction.getFunctionArn())
                .build());

        new Output(this, "FromSQSFunctionName", OutputProps.builder()
                .withValue(dummyQueueReceiverFunction.getFunctionName())
                .build());

        new Output(this, "RestApiUrl", OutputProps.builder()
                .withValue(ApiGatewayStageGenerator.getLambdaApiUrl(restApiResource, stageResource))
                .build());

        new Output(this, "EdgeApiUrl", OutputProps.builder()
                .withValue(ApiGatewayStageGenerator.getLambdaApiUrl(edgeApiResource, edgeStageResource))
                .build());
    }
}
