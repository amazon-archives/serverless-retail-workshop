package fishing.lee.sqsforwarder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

@SuppressWarnings("unused")
public class OrderForwarder implements RequestHandler<SQSEvent, String> {
    private LambdaLogger logger;

    @Override
    public String handleRequest(SQSEvent input, Context context) {
        logger = context.getLogger();

        input.getRecords().forEach(sqsMessage -> {
            String orderDetail = sqsMessage.getBody();
            sendOrder(orderDetail);
        });

        return "";
    }

    private void sendOrder(String orderDetail) {
        logger.log("PROCESSING " + orderDetail);

        final String url = System.getenv("SHOPBACKEND_ORDER_URL") + "/order";

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(orderDetail));

            CloseableHttpResponse response = httpclient.execute(httpPost);
            logger.log(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            // We're cool and we'll ignore it
            e.printStackTrace();
        }
    }
}
