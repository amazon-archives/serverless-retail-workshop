package fishing.lee.infrastructure;

import software.amazon.awscdk.AwsAccountId;
import software.amazon.awscdk.AwsRegion;
import software.amazon.awscdk.services.apigateway.cloudformation.*;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.cloudformation.PermissionResource;
import software.amazon.awscdk.services.lambda.cloudformation.PermissionResourceProps;

class ApiGatewayStageGenerator {
    static StageResource configureLambda(RestApiResource restApiResource, Function function) {
        Resource resource = new Resource(restApiResource, "Resource", ResourceProps.builder()
                .withRestApiId(restApiResource.getRestApiId())
                .withParentId(restApiResource.getRestApiRootResourceId())
                .withPathPart("{proxy+}")
                .build());

        MethodResource methodResource = new MethodResource(restApiResource,"Method", MethodResourceProps.builder()
                .withAuthorizationType("NONE")
                .withResourceId(resource.getResourceId())
                .withRestApiId(restApiResource.getRestApiId())
                .withHttpMethod("ANY")
                .withIntegration(MethodResource.IntegrationProperty.builder()
                        .withType("AWS_PROXY")
                        .withTimeoutInMillis(29000)
                        .withIntegrationHttpMethod("POST")
                        .withUri("arn:aws:apigateway:" + new AwsRegion() + ":lambda:path/2015-03-31/functions/" + function.getFunctionArn() + "/invocations")
                        .build())
                .build());

        DeploymentResource deploymentResource = new DeploymentResource(restApiResource, "Deployment", DeploymentResourceProps.builder()
                .withStageName("DummyStage")
                .withRestApiId(restApiResource.getRestApiId())
                .build());

        deploymentResource.addDependency(methodResource);

        StageResource stageResource = new StageResource(restApiResource, "Stage", StageResourceProps.builder()
                .withDeploymentId(deploymentResource.getDeploymentId())
                .withStageName("prod")
                .withRestApiId(restApiResource.getRestApiId())
                .build());

        new PermissionResource(restApiResource, "Permission", PermissionResourceProps.builder()
                .withFunctionName(function.getFunctionName())
                .withPrincipal("apigateway.amazonaws.com")
                .withSourceArn("arn:aws:execute-api:" + new AwsRegion() + ":" + new AwsAccountId() + ":" + restApiResource.getRestApiId() + "/*")
                .withAction("lambda:invokeFunction")
                .build());

        new ApiGatewayXrayEnablerResource(restApiResource, "ProdXray", ApiGatewayXrayEnablerResourceProps.build()
                .withRestApiId(restApiResource.getRestApiId())
                .withStageName(stageResource.getStageName())
                .build());

        new ApiGatewayXrayEnablerResource(restApiResource, "DummyStageXray", ApiGatewayXrayEnablerResourceProps.build()
                .withRestApiId(restApiResource.getRestApiId())
                .withStageName("DummyStage")
                .build()).addDependency(deploymentResource);

        return stageResource;
    }

    static String getLambdaApiUrl(RestApiResource restApiResource, StageResource stageResource) {
        return "https://" + restApiResource.getRestApiId() + ".execute-api." + new AwsRegion() + ".amazonaws.com/" + stageResource.getStageName();
    }
}
