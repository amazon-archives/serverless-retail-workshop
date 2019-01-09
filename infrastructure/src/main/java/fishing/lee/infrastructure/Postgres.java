package fishing.lee.infrastructure;

import software.amazon.awscdk.Construct;
import software.amazon.awscdk.Output;
import software.amazon.awscdk.OutputProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.CfnDBInstance;
import software.amazon.awscdk.services.rds.CfnDBInstanceProps;
import software.amazon.awscdk.services.rds.CfnDBSubnetGroup;
import software.amazon.awscdk.services.rds.CfnDBSubnetGroupProps;

import java.util.Collections;
import java.util.stream.Collectors;

class Postgres extends Construct {
    private final SecurityGroup securityGroup;
    private final String hostName;
    private final String port;
    private final String password;
    private final String username;
    private final String dbName;

    Postgres(Construct parent, String id, PostgresProps properties) {
        super(parent, id);

        CfnDBSubnetGroup dbSubnetGroupResource = new CfnDBSubnetGroup(this, "PrivateSubnets", CfnDBSubnetGroupProps.builder()
                .withDbSubnetGroupDescription(id + "PrivateSubnets")
                .withDbSubnetGroupName(id + "PrivateSubnets")
                .withSubnetIds(properties.getShopVpc().getVpc()
                        .getPrivateSubnets()
                        .stream()
                        .map(VpcSubnetRef::getSubnetId)
                        .collect(Collectors.toList()))
                .build());

        securityGroup = new SecurityGroup(this, "SecurityGroup", SecurityGroupProps.builder()
                .withVpc(properties.getShopVpc().getVpc())
                .build());

        username = id.toLowerCase();
        password = id.toLowerCase() + "p4ssw0rd";
        dbName = id.toLowerCase();

        CfnDBInstance dbInstanceResource = new CfnDBInstance(this, "Database", CfnDBInstanceProps.builder()
                .withDbInstanceIdentifier(id)
                .withEngine("postgres")
                .withEngineVersion("10.4")
                .withAllocatedStorage("20")
                .withAllowMajorVersionUpgrade(false)
                .withAutoMinorVersionUpgrade(false)
                .withDbSubnetGroupName(dbSubnetGroupResource.getDbSubnetGroupName())
                .withDbInstanceClass("db.t2.micro")
                .withVpcSecurityGroups(Collections.singletonList(securityGroup.getSecurityGroupId()))
                .withMultiAz(false)
                .withStorageType("gp2")
                .withMasterUsername(username)
                .withMasterUserPassword(password)
                .withDbName(dbName)
                .build());

        port = dbInstanceResource.getDbInstanceEndpointPort();
        hostName = dbInstanceResource.getDbInstanceEndpointAddress();

        new Output(this, "Username", OutputProps.builder()
                .withValue(username)
                .build());
        new Output(this, "Password", OutputProps.builder()
                .withValue(password)
                .build());
        new Output(this, "DbName", OutputProps.builder()
                .withValue(dbName)
                .build());
        new Output(this, "Hostname", OutputProps.builder()
                .withValue(hostName)
                .build());
        new Output(this, "Port", OutputProps.builder()
                .withValue(port)
                .build());

    }

    void addSecurityGroupIngress(SecurityGroup securityGroup) {
        this.securityGroup.addIngressRule(securityGroup, new TcpPortFromAttribute(port));
    }

    String getPort() {
        return port;
    }

    String getHostName() {
        return hostName;
    }

    String getPassword() {
        return password;
    }

    String getUsername() {
        return username;
    }

    String getDbName() {
        return dbName;
    }
}
