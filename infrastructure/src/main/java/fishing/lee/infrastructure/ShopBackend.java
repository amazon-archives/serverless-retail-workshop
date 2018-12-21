package fishing.lee.infrastructure;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.cloudformation.*;
import software.amazon.awscdk.services.apigateway.cloudformation.Resource;
import software.amazon.awscdk.services.apigateway.cloudformation.ResourceProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.elasticbeanstalk.cloudformation.ApplicationResource;
import software.amazon.awscdk.services.elasticbeanstalk.cloudformation.ApplicationResourceProps;
import software.amazon.awscdk.services.elasticbeanstalk.cloudformation.EnvironmentResource;
import software.amazon.awscdk.services.elasticbeanstalk.cloudformation.EnvironmentResourceProps;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.iam.cloudformation.InstanceProfileResource;
import software.amazon.awscdk.services.iam.cloudformation.InstanceProfileResourceProps;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.cloudformation.PermissionResource;
import software.amazon.awscdk.services.lambda.cloudformation.PermissionResourceProps;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketRef;
import software.amazon.awscdk.services.s3.BucketRefProps;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

class ShopBackend extends Construct {
    private final String domainName;
    private final SecurityGroup loadBalancerSecurityGroup;
    private final String lambdaApiUrl;

    ShopBackend(Construct parent, String id, ShopBackendProps properties) {
        super(parent, id);

        ApplicationResource applicationResource = new ApplicationResource(this, "Application", ApplicationResourceProps.builder()
                .withApplicationName(id)
                .withDescription("Keeping it Reel!")
                .build());

        SecurityGroup instanceSecurityGroup = new SecurityGroup(this, "SecurityGroup", SecurityGroupProps.builder()
                .withVpc(properties.getShopVpc().getVpc())
                .build());

        loadBalancerSecurityGroup = new SecurityGroup(this, "LoadBalancer", SecurityGroupProps.builder()
                .withVpc(properties.getShopVpc().getVpc())
                .build());

        if (Objects.nonNull(properties.getBastionSecurityGroup())) {
            loadBalancerSecurityGroup.addIngressRule(properties.getBastionSecurityGroup(), new TcpAllPorts(), "Bastion");
        }

        EnvironmentResource environmentResource = new EnvironmentResource(this, "Environment", EnvironmentResourceProps.builder()
                .withSolutionStackName("64bit Amazon Linux 2018.03 v2.7.8 running Java 8")
                .withApplicationName(applicationResource.getApplicationName())
                .withEnvironmentName(id)
                .withOptionSettings(Arrays.asList(
                        /*
                            Networking
                         */
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:ec2:vpc")
                                .withOptionName("VPCId")
                                .withValue(properties.getShopVpc().getVpc().getVpcId())
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:ec2:vpc")
                                .withOptionName("Subnets")
                                .withValue(properties.getShopVpc().getVpc()
                                        .getPrivateSubnets()
                                        .stream()
                                        .map(VpcSubnetRef::getSubnetId)
                                        .collect(Collectors.joining(",")))
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:ec2:vpc")
                                .withOptionName("ELBSubnets")
                                .withValue(properties.getShopVpc().getVpc()
                                        .getPrivateSubnets()
                                        .stream()
                                        .map(VpcSubnetRef::getSubnetId)
                                        .collect(Collectors.joining(",")))
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:ec2:vpc")
                                .withOptionName("ELBScheme")
                                .withValue("internal")
                                .build(),

                        /*
                          IAM things - instance profile and the EB Service Role
                         */
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:autoscaling:launchconfiguration")
                                .withOptionName("IamInstanceProfile")
                                .withValue(
                                        new InstanceProfileResource(this, "InstanceProfile", InstanceProfileResourceProps.builder()
                                                .withRoles(Collections.singletonList(
                                                        new Role(this, "ShopInstanceRole", RoleProps.builder()
                                                                .withManagedPolicyArns(Arrays.asList(
                                                                        "arn:aws:iam::aws:policy/AWSElasticBeanstalkWebTier",
                                                                        "arn:aws:iam::aws:policy/AWSElasticBeanstalkMulticontainerDocker",
                                                                        "arn:aws:iam::aws:policy/AWSElasticBeanstalkWorkerTier",
                                                                        "arn:aws:iam::aws:policy/AmazonS3FullAccess",
                                                                        "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM",
                                                                        "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
                                                                ))
                                                                .withAssumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                                                                .build()).getRoleName()
                                                ))
                                                .build()).getInstanceProfileName()
                                )
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:elasticbeanstalk:environment")
                                .withOptionName("ServiceRole")
                                .withValue(new Role(this, "ServiceRole", RoleProps.builder()
                                        .withAssumedBy(new ServicePrincipal("elasticbeanstalk.amazonaws.com"))
                                        .withManagedPolicyArns(Arrays.asList(
                                                "arn:aws:iam::aws:policy/service-role/AWSElasticBeanstalkService",
                                                "arn:aws:iam::aws:policy/service-role/AWSElasticBeanstalkEnhancedHealth"
                                        ))
                                        .build()).getRoleName())
                                .build(),

                        /*
                            Auto Scaling options (instance type etc.)
                         */
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:autoscaling:launchconfiguration")
                                .withOptionName("InstanceType")
                                .withValue(properties.getInstanceType())
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:autoscaling:launchconfiguration")
                                .withOptionName("SecurityGroups")
                                .withValue(instanceSecurityGroup.getGroupName())
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:autoscaling:asg")
                                .withOptionName("MinSize")
                                .withValue(String.valueOf(properties.getMinSize()))
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:autoscaling:asg")
                                .withOptionName("MaxSize")
                                .withValue(String.valueOf(properties.getMaxSize()))
                                .build(),

                        /*
                            Monitoring
                         */
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:elasticbeanstalk:xray")
                                .withOptionName("XRayEnabled")
                                .withValue("true")
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:elasticbeanstalk:healthreporting:system")
                                .withOptionName("SystemType")
                                .withValue("enhanced")
                                .build(),

                        /*
                            Load Balancer (use ALB rather than ELB) - we use port 80 because, we don't really
                            care about encryption for this, plus you'd need a Certificate ARN and that's a bit
                            more complex in a demo.
                         */
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:elasticbeanstalk:environment")
                                .withOptionName("LoadBalancerType")
                                .withValue("application")
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:elbv2:loadbalancer")
                                .withOptionName("SecurityGroups")
                                .withValue(loadBalancerSecurityGroup.getGroupName())
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:elbv2:loadbalancer")
                                .withOptionName("ManagedSecurityGroup")
                                .withValue(loadBalancerSecurityGroup.getGroupName())
                                .build(),

                        /*
                            Settings
                         */
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:elasticbeanstalk:application:environment")
                                .withOptionName("SERVER_PORT")
                                .withValue("5000")
                                .build()
                ))
                .build());

        domainName = environmentResource.getEnvironmentEndpointUrl();

        PolicyDocument policyDocument = new ApiGatewayVpcPolicyDocument(properties.getShopVpc().getApiEndpoint()).getPolicyDocument();

        /*
            We import the bucket needed to get our dummy lambda JAR file
         */
        BucketRef bucket = Bucket.import_(this, "ImportedBucket", BucketRefProps.builder()
                .withBucketName("leepac-fishing-assets")
                .build());

        Role backendLambdaRole = new Role(this, "Role", RoleProps.builder()
                .withManagedPolicyArns(Arrays.asList(
                        "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole",
                        "arn:aws:iam::aws:policy/AWSXrayFullAccess",
                        "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
                ))
                .withAssumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .build()
        );

        Function dummyLambda = new Function(this, "Lambda", FunctionProps.builder()
                .withCode(Code.bucket(bucket, "dummy-1.0-SNAPSHOT-all.jar"))
                .withHandler("fishing.lee.backend.StreamLambdaHandler::handleRequest")
                .withMemorySize(3008)
                .withTimeout(60)
                .withTracing(Tracing.Active)
                .withRuntime(Runtime.JAVA8)
                .withRole(backendLambdaRole)
                .withEnvironment(new HashMap<String, Object>() {{
                    put("SPRING_PROFILES_ACTIVE", "lambda");
                }})
                .withDescription("Backend API Function")
                .build());

        RestApiResource restApiResource = new RestApiResource(this, "RestApi", RestApiResourceProps.builder()
                .withName("TroutApi")
                .withDescription("Artifishial Intelligence ordering API")
                .withEndpointConfiguration(RestApiResource.EndpointConfigurationProperty.builder()
                        .withTypes(Collections.singletonList("PRIVATE"))
                        .build())
                .withPolicy(new CloudFormationToken(policyDocument))
                .build());

        RestApiResource edgeApiResource = new RestApiResource(this, "EdgeApi", RestApiResourceProps.builder()
                .withName("TroutApiEdge")
                .withDescription("Artifishial Intelligence ordering API @ Edge")
                .withEndpointConfiguration(RestApiResource.EndpointConfigurationProperty.builder()
                        .withTypes(Collections.singletonList("EDGE"))
                        .build())
                .build());

        StageResource stageResource = ApiGatewayStageGenerator.configureLambda(restApiResource, dummyLambda);
        StageResource edgeStageResource = ApiGatewayStageGenerator.configureLambda(edgeApiResource, dummyLambda);

        new Output(this, "LambdaFunctionArn", OutputProps.builder()
                .withValue(dummyLambda.getFunctionArn())
                .build());

        new Output(this, "LambdaFunctionName", OutputProps.builder()
                .withValue(dummyLambda.getFunctionName())
                .build());

        lambdaApiUrl = ApiGatewayStageGenerator.getLambdaApiUrl(restApiResource, stageResource);

        new Output(this, "RestApiUrl", OutputProps.builder()
                .withValue(lambdaApiUrl)
                .build());

        new Output(this, "EdgeApiUrl", OutputProps.builder()
                .withValue(ApiGatewayStageGenerator.getLambdaApiUrl(edgeApiResource, edgeStageResource))
                .build());

        new Output(this, "BackendUrl", OutputProps.builder()
                .withValue(environmentResource.getEnvironmentEndpointUrl())
                .build());
    }

    String getDomainName() {
        return domainName;
    }

    void addSecurityGroupIngress(SecurityGroup securityGroup) {
        loadBalancerSecurityGroup.addIngressRule(securityGroup, new TcpPort(80));
    }

    String getLambdaApiUrl() {
        return lambdaApiUrl;
    }
}
