package fishing.lee.infrastructure;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import software.amazon.awscdk.Construct;
import software.amazon.awscdk.Output;
import software.amazon.awscdk.OutputProps;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.s3.CfnBucketPolicy;
import software.amazon.awscdk.services.s3.CfnBucketPolicyProps;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

class ContentDistribution extends Construct {

    ContentDistribution(Construct parent, String id) {
        super(parent, id);

        Bucket bucket = new Bucket(this, "Bucket", BucketProps.builder()
                .build());

        CfnCloudFrontOriginAccessIdentity identityResource = new CfnCloudFrontOriginAccessIdentity(this, "OAI", CfnCloudFrontOriginAccessIdentityProps.builder()
                .withCloudFrontOriginAccessIdentityConfig(new CfnCloudFrontOriginAccessIdentity.CloudFrontOriginAccessIdentityConfigProperty.Builder()
                        .withComment("Static Content Distribution")
                        .build())
                .build());

        CloudFrontWebDistribution webDistribution = new CloudFrontWebDistribution(this, "CloudFront", CloudFrontWebDistributionProps.builder()
                .withViewerProtocolPolicy(ViewerProtocolPolicy.RedirectToHTTPS)
                .withPriceClass(PriceClass.PriceClass100)
                .withHttpVersion(HttpVersion.HTTP2)
                .withDefaultRootObject("")
                .withOriginConfigs(Collections.singletonList(
                        SourceConfiguration.builder()
                                .withBehaviors(Collections.singletonList(
                                        Behavior.builder()
                                                .withAllowedMethods(CloudFrontAllowedMethods.ALL)
                                                .withDefaultTtlSeconds(60)
                                                .withIsDefaultBehavior(true)
                                                .withForwardedValues(CfnDistribution.ForwardedValuesProperty.builder()
                                                        .withCookies(CfnDistribution.CookiesProperty.builder()
                                                                .withWhitelistedNames(Arrays.asList(
                                                                        "csrftoken",
                                                                        "sessionid",
                                                                        "messages"
                                                                ))
                                                                .withForward("whitelist")
                                                                .build())
                                                        .withQueryString(true)
                                                        .build())
                                                .build()
                                ))
                                .withS3OriginSource(S3OriginConfig.builder()
                                        .withS3BucketSource(bucket)
                                        .withOriginAccessIdentity(identityResource)
                                        .build())
                                .withOriginHeaders(new HashMap<String, String>() {{
                                    put("X-CloudFront-Forwarded-Proto", "https");
                                }})
                                .build()
                ))
                .build());

        PolicyDocument document = new PolicyDocument();
        document.addStatement(new PolicyStatement()
                .addPrincipal(new CanonicalUserPrincipal(identityResource.getCloudFrontOriginAccessIdentityS3CanonicalUserId()))
                .addAction("s3:GetObject")
                .addResource("arn:aws:s3:::" + bucket.getBucketName() + "/*")
        );

        String inlineFunction = null;
        try {
            //noinspection UnstableApiUsage
            inlineFunction = Resources.toString(Resources.getResource("dummy_edge.js"), Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Function edgeLambda = new Function(this, "LambdaBackend", FunctionProps.builder()
                .withCode(Code.inline(Objects.requireNonNull(inlineFunction)))
                .withHandler("index.index_rewrite")
                .withMemorySize(256)
                .withTimeout(5)
                .withTracing(Tracing.Active)
                .withRuntime(Runtime.NODE_J_S610)
                .withDescription("Lambda@Edge Function")
                .build());

        Objects.requireNonNull(Objects.requireNonNull(edgeLambda.getRole()).getAssumeRolePolicy()).addStatement(new PolicyStatement()
                .addServicePrincipal("edgelambda.amazonaws.com")
                .addAction("sts:AssumeRole")
                .allow());

        edgeLambda.addVersion("1");

        new CfnBucketPolicy(this, "BucketPolicy", CfnBucketPolicyProps.builder()
                .withBucket(bucket.getBucketName())
                .withPolicyDocument(document)
                .build());

        new Output(this, "CloudFrontDistribution", OutputProps.builder()
                .withValue(webDistribution.getDistributionId())
                .build());

        new Output(this, "CloudFrontUrl", OutputProps.builder()
                .withValue(webDistribution.getDomainName())
                .build());

        new Output(this, "StaticBucket", OutputProps.builder()
                .withValue(bucket.getBucketName())
                .build());

        new Output(this, "EdgeFunctionName", OutputProps.builder()
                .withValue(edgeLambda.getFunctionName())
                .build());

        new Output(this, "EdgeFunctionArn", OutputProps.builder()
                .withValue(edgeLambda.getFunctionArn())
                .build());
    }
}
