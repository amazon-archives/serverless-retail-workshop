package fishing.lee.infrastructure;

import software.amazon.awscdk.Construct;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.TopicProps;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueProps;

class Decoupling extends Construct {
    Decoupling(Construct parent, String id) {
        super(parent, id);

        new Queue(this, "Queue", QueueProps.builder()
                .build());

        new Topic(this, "Topic", TopicProps.builder()
                .build());
    }
}
