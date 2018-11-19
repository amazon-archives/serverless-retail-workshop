package fishing.lee.backend;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;

@SuppressWarnings("unused")
public class StreamLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("This is a sample responder - it'll only respond with DUMMY");

        return new APIGatewayProxyResponseEvent()
                .withHeaders(new HashMap<String, String>()  {{
                    put("Content-Type", "application/json");
                }})
                .withStatusCode(200)
                .withBody("DUMMY " + input.getPath());
    }
}
