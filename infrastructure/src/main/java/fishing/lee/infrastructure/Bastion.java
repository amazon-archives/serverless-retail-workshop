package fishing.lee.infrastructure;

import software.amazon.awscdk.Construct;
import software.amazon.awscdk.Output;
import software.amazon.awscdk.OutputProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroupProps;
import software.amazon.awscdk.services.ec2.*;

class Bastion extends Construct {
    private final SecurityGroup bastionSecurityGroup;

    Bastion(Construct parent, String id, BastionProps properties) {
        super(parent, id);

        AutoScalingGroup bastionAsg = new AutoScalingGroup(this, "AutoScalingGroup", AutoScalingGroupProps.builder()
                .withAllowAllOutbound(true)
                .withDesiredCapacity(1)
                .withMinSize(1)
                .withMaxSize(1)
                .withVpc(properties.getShopVpc().getVpc())
                .withVpcPlacement(VpcPlacementStrategy.builder()
                        .withSubnetsToUse(SubnetType.Public)
                        .build())
                .withInstanceType(new InstanceTypePair(InstanceClass.Burstable3, InstanceSize.Micro))
                .withMachineImage(new AmazonLinuxImage())
                .withKeyName(properties.getSshKeyPairName())
                .build());

        bastionSecurityGroup = new SecurityGroup(this, "SecurityGroup", SecurityGroupProps.builder()
                .withVpc(properties.getShopVpc().getVpc())
                .build());

        properties.getShopVpc().addSecurityGroupToApi(bastionSecurityGroup);

        bastionAsg.addSecurityGroup(bastionSecurityGroup);
        bastionAsg.addUserData(
                "yum update -y",
                "yum install -y jq"
        );

        bastionSecurityGroup.addIngressRule(new AnyIPv4(), new TcpPort(22));

        new Output(this, "AutoScalingGroupName", OutputProps.builder()
                .withValue(bastionAsg.getAutoScalingGroupName())
                .build());
    }

    SecurityGroup getBastionSecurityGroup() {
        return bastionSecurityGroup;
    }
}
