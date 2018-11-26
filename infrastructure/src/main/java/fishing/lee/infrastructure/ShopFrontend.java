package fishing.lee.infrastructure;

import org.apache.commons.text.StringSubstitutor;
import software.amazon.awscdk.Construct;
import software.amazon.awscdk.Output;
import software.amazon.awscdk.OutputProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.elasticbeanstalk.cloudformation.ApplicationResource;
import software.amazon.awscdk.services.elasticbeanstalk.cloudformation.ApplicationResourceProps;
import software.amazon.awscdk.services.elasticbeanstalk.cloudformation.EnvironmentResource;
import software.amazon.awscdk.services.elasticbeanstalk.cloudformation.EnvironmentResourceProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.iam.cloudformation.InstanceProfileResource;
import software.amazon.awscdk.services.iam.cloudformation.InstanceProfileResourceProps;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class ShopFrontend extends Construct {

    ShopFrontend(Construct parent, String id, ShopFrontendProps properties) {
        super(parent, id);

        ApplicationResource shopApplication = new ApplicationResource(this, "Application", ApplicationResourceProps.builder()
                .withApplicationName(id)
                .withDescription("Keeping it Reel!")
                .build());

        /*
            The instance needs a security group for the load balancer to connect to - EB will go
            ahead and add the Load Balancer security group to this without intervention, hence
            it being empty.
         */
        SecurityGroup instanceSecurityGroup = new SecurityGroup(this, "InstanceSecurityGroup", SecurityGroupProps.builder()
                .withVpc(properties.getShopVpc().getVpc())
                .build());

        properties.getShopBackend().addSecurityGroupIngress(instanceSecurityGroup);
        properties.getPostgres().addSecurityGroupIngress(instanceSecurityGroup);
        properties.getShopVpc().addSecurityGroupToApi(instanceSecurityGroup);

        /*
            The load balancer security group itself - we allow both 80 and 443
         */
        SecurityGroup loadBalancerSecurityGroup = new SecurityGroup(this, "LoadBalancerSecurityGroup", SecurityGroupProps.builder()
                .withVpc(properties.getShopVpc().getVpc())
                .build());

        loadBalancerSecurityGroup.addIngressRule(new CidrIPv4("0.0.0.0/0"), new TcpPort(80));

        Bucket mediaBucket = new Bucket(this, "Bucket", BucketProps.builder()
                .build());

        Map<String, String> databaseUrlVariables = new HashMap<String, String>() {{
            put("RDS_HOSTNAME", properties.getPostgres().getHostName());
            put("RDS_PORT", properties.getPostgres().getPort());
            put("RDS_DB_NAME", properties.getPostgres().getDbName());
            put("RDS_USERNAME", properties.getPostgres().getUsername());
            put("RDS_PASSWORD", properties.getPostgres().getPassword());
        }};

        String databaseUrl = new StringSubstitutor(databaseUrlVariables).replace(
                "postgres://${RDS_USERNAME}:${RDS_PASSWORD}@${RDS_HOSTNAME}:${RDS_PORT}/${RDS_DB_NAME}"
        );

        EnvironmentResource shopEnvironment = new EnvironmentResource(this, "Environment", EnvironmentResourceProps.builder()
                .withSolutionStackName("64bit Amazon Linux 2018.03 v2.7.6 running Python 3.6")
                .withApplicationName(shopApplication.getApplicationName())
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
                                        .getPublicSubnets()
                                        .stream()
                                        .map(VpcSubnetRef::getSubnetId)
                                        .collect(Collectors.joining(",")))
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
                                                        new Role(this, "InstanceRole", RoleProps.builder()
                                                                .withManagedPolicyArns(Arrays.asList(
                                                                        "arn:aws:iam::aws:policy/AWSElasticBeanstalkWebTier",
                                                                        "arn:aws:iam::aws:policy/AWSElasticBeanstalkMulticontainerDocker",
                                                                        "arn:aws:iam::aws:policy/AWSElasticBeanstalkWorkerTier",
                                                                        "arn:aws:iam::aws:policy/AmazonS3FullAccess",
                                                                        "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
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
                                .withOptionName("BACKEND_DOMAIN")
                                .withValue(properties.getShopBackend().getDomainName())
                                .build(),

                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:elasticbeanstalk:application:environment")
                                .withOptionName("DJANGO_AWS_STORAGE_BUCKET_NAME")
                                .withValue(mediaBucket.getBucketName())
                                .build(),
                        EnvironmentResource.OptionSettingProperty.builder()
                                .withNamespace("aws:elasticbeanstalk:application:environment")
                                .withOptionName("DJANGO_SECURE_SSL_REDIRECT")
                                .withValue("False")
                                .build()



                ))
                .build());

        new Output(this, "ShopUrl", OutputProps.builder()
                .withValue(shopEnvironment.getEnvironmentEndpointUrl())
                .build());

        new Output(this, "DjangoDatabaseUrl", OutputProps.builder()
                .withValue(databaseUrl)
                .build());
    }
}
