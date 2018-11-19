package fishing.lee.infrastructure;

import software.amazon.awscdk.Construct;
import software.amazon.awscdk.Output;
import software.amazon.awscdk.OutputProps;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;

class DeploymentAssets extends Construct {
    DeploymentAssets(Construct parent, String id) {
        super(parent, id);

        Bucket bucket = new Bucket(this, "Bucket", BucketProps.builder()
                .build());

        new Output(this, "DeploymentBucket", OutputProps.builder()
                .withValue(bucket.getBucketName())
                .build());
    }
}
