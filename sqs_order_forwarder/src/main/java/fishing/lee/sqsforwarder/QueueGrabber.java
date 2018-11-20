package fishing.lee.sqsforwarder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageResult;

import java.util.HashMap;

@SuppressWarnings("unused")
public class QueueGrabber implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        final String sqsQueueName = System.getenv("SHOPBACKEND_SQS_QUEUE");
        LambdaLogger logger = context.getLogger();

        if (input.getHttpMethod().equals("OPTIONS")) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(new HashMap<String, String>()  {{
                        put("Access-Control-Allow-Origin", "*");
                        put("Access-Control-Allow-Methods", "GET,HEAD,POST");
                        put("Access-Control-Max-Age", "1800");
                        put("Allow", "GET, HEAD, POST, PUT, DELETE, OPTIONS, PATCH");
                    }})
                    .withStatusCode(200);
        }

        switch (input.getPath()) {
            case "/ping":
                return new APIGatewayProxyResponseEvent()
                        .withHeaders(new HashMap<String, String>()  {{
                            put("Content-Type", "application/json");
                        }})
                        .withStatusCode(200)
                        .withBody("PONG");
            case "/order":
                AmazonSQS amazonSQS = AmazonSQSClientBuilder.defaultClient();
                SendMessageResult result = amazonSQS.sendMessage(sqsQueueName, input.getBody());

                return new APIGatewayProxyResponseEvent()
                        .withHeaders(new HashMap<String, String>()  {{
                            put("Content-Type", "application/json");
                        }})
                        .withStatusCode(200)
                        .withBody(result.getMessageId());
            default:
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404);
        }


    }
}
