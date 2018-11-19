package fishing.lee.infrastructure;

import software.amazon.awscdk.services.ec2.cloudformation.VPCEndpointResource;
import software.amazon.awscdk.services.iam.*;

import java.util.HashMap;

class ApiGatewayVpcPolicyDocument {
    private VPCEndpointResource vpcEndpointResource;

    ApiGatewayVpcPolicyDocument(VPCEndpointResource vpcEndpointResource) {
        this.vpcEndpointResource = vpcEndpointResource;
    }

    PolicyDocument getPolicyDocument() {
        PolicyStatement denyNotVpc = new PolicyStatement();
        denyNotVpc.deny();
        denyNotVpc.addAction("execute-api:Invoke");
        denyNotVpc.addResource("execute-api:/*");
        denyNotVpc.addPrincipal(new Anyone());
        denyNotVpc.addCondition("StringNotEquals", new HashMap<String, String>() {{
            put("aws:sourceVpce", vpcEndpointResource.getVpcEndpointId());
        }});

        PolicyStatement allow = new PolicyStatement();
        allow.allow();
        allow.addPrincipal(new Anyone());
        allow.addResource("execute-api:/*");
        allow.addAction("execute-api:Invoke");

        PolicyDocument policyDocument = new PolicyDocument();
        policyDocument.addStatement(denyNotVpc);
        policyDocument.addStatement(allow);
        return policyDocument;
    }
}
