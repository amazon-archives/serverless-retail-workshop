package fishing.lee.infrastructure;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import software.amazon.awscdk.Construct;
import software.amazon.awscdk.IDependable;
import software.amazon.awscdk.services.cloudformation.CustomResource;
import software.amazon.awscdk.services.cloudformation.CustomResourceProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.SingletonFunction;
import software.amazon.awscdk.services.lambda.SingletonFunctionProps;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

class ApiGatewayXrayEnablerResource extends Construct {
    private CustomResource customResource;

    ApiGatewayXrayEnablerResource(Construct parent, String id, ApiGatewayXrayEnablerResourceProps properties) {
        super(parent, id);

        String inlineFunction = null;
        try {
            //noinspection UnstableApiUsage
            inlineFunction = Resources.toString(Resources.getResource("xray_enabler.py"), Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Role role = new Role(this, "Role", RoleProps.builder()
                .withManagedPolicyArns(Arrays.asList(
                        "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole",
                        "arn:aws:iam::aws:policy/AmazonAPIGatewayAdministrator"
                ))
                .withAssumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .build()
        );

        role.addToPolicy(new PolicyStatement()
            .addAllResources()
            .addAction("iam:CreateServiceLinkedRole")
            .allow()
        );

        customResource = new CustomResource(this, "CustomResource", CustomResourceProps.builder()
                .withLambdaProvider(new SingletonFunction(this, "Singleton", SingletonFunctionProps.builder()
                        .withCode(Code.inline(Objects.requireNonNull(inlineFunction)))
                        .withRuntime(Runtime.PYTHON36)
                        .withTimeout(300)
                        .withRole(role)
                        .withHandler("index.lambda_handler")
                        .withUuid("7814aca5-9512-4c11-a941-516cc6261a45")
                        .build()))
                .withProperties(new HashMap<String, Object>() {{
                    put("RestApiId", properties.getRestApiId());
                    put("StageName", properties.getStageName());
                }})
                .build()
        );
    }

    void addDependency(IDependable other) {
        customResource.addDependency(other);
    }
}
