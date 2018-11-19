package fishing.lee.infrastructure;

import software.amazon.awscdk.AwsRegion;
import software.amazon.awscdk.Construct;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.cloudformation.VPCEndpointResource;
import software.amazon.awscdk.services.ec2.cloudformation.VPCEndpointResourceProps;

import java.util.Collections;
import java.util.stream.Collectors;

class ShopVpc extends Construct {
    private final VpcNetwork vpc;
    private final SecurityGroup apiEndpointSecurityGroup;
    private final VPCEndpointResource apiEndpoint;

    ShopVpc(Construct parent, String id) {
        super(parent, id);

        vpc = new VpcNetwork(this, "Fishing", VpcNetworkProps.builder()
                .withMaxAZs(2)
                .withCidr("10.0.0.0/16")
                .withEnableDnsSupport(true)
                .withEnableDnsHostnames(true)
                .withNatGateways(1)
                .build());

        apiEndpointSecurityGroup = new SecurityGroup(this, "APISecurityGroup", SecurityGroupProps.builder()
                .withVpc(vpc)
                .build());

        apiEndpoint = new VPCEndpointResource(this, "APIEndpoint", VPCEndpointResourceProps.builder()
                .withVpcId(vpc.getVpcId())
                .withServiceName("com.amazonaws." + new AwsRegion() + ".execute-api")
                .withPrivateDnsEnabled(true)
                .withSecurityGroupIds(Collections.singletonList(apiEndpointSecurityGroup.getSecurityGroupId()))
                .withVpcEndpointType("Interface")
                .withSubnetIds(vpc.getPrivateSubnets()
                        .stream()
                        .map(VpcSubnetRef::getSubnetId)
                        .collect(Collectors.toList()))
                .build());
    }

    VpcNetwork getVpc() {
        return vpc;
    }

    void addSecurityGroupToApi(SecurityGroup securityGroup) {
        apiEndpointSecurityGroup.addIngressRule(securityGroup, new TcpPort(443));
    }

    VPCEndpointResource getApiEndpoint() {
        return apiEndpoint;
    }
}
