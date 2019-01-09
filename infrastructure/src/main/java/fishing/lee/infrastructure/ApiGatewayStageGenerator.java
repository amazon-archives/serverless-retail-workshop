package fishing.lee.infrastructure;

import software.amazon.awscdk.AwsAccountId;
import software.amazon.awscdk.AwsRegion;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.CfnPermission;
import software.amazon.awscdk.services.lambda.CfnPermissionProps;
import software.amazon.awscdk.services.lambda.Function;

class ApiGatewayStageGenerator {
    static CfnStage configureLambda(CfnRestApi restApiResource, Function function) {
        CfnResource resource = new CfnResource(restApiResource, "Resource", CfnResourceProps.builder()
                .withRestApiId(restApiResource.getRestApiId())
                .withParentId(restApiResource.getRestApiRootResourceId())
                .withPathPart("{proxy+}")
                .build());

        CfnMethod methodResource = new CfnMethod(restApiResource,"Method", CfnMethodProps.builder()
                .withAuthorizationType("NONE")
                .withResourceId(resource.getResourceId())
                .withRestApiId(restApiResource.getRestApiId())
                .withHttpMethod("ANY")
                .withIntegration(CfnMethod.IntegrationProperty.builder()
                        .withType("AWS_PROXY")
                        .withTimeoutInMillis(29000)
                        .withIntegrationHttpMethod("POST")
                        .withUri("arn:aws:apigateway:" + new AwsRegion() + ":lambda:path/2015-03-31/functions/" + function.getFunctionArn() + "/invocations")
                        .build())
                .build());

        CfnDeployment deploymentResource = new CfnDeployment(restApiResource, "Deployment", CfnDeploymentProps.builder()
                .withStageName("DummyStage")
                .withRestApiId(restApiResource.getRestApiId())
                .build());

        deploymentResource.addDependency(methodResource);

        CfnStage stageResource = new CfnStage(restApiResource, "Stage", CfnStageProps.builder()
                .withDeploymentId(deploymentResource.getDeploymentId())
                .withStageName("prod")
                .withRestApiId(restApiResource.getRestApiId())
                .build());

        new CfnPermission(restApiResource, "Permission", CfnPermissionProps.builder()
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

    static String getLambdaApiUrl(CfnRestApi restApiResource, CfnStage stageResource) {
        return "https://" + restApiResource.getRestApiId() + ".execute-api." + new AwsRegion() + ".amazonaws.com/" + stageResource.getStageName();
    }
}
