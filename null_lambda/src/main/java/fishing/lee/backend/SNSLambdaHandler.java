package fishing.lee.backend;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

@SuppressWarnings("unused")
public class SNSLambdaHandler implements RequestHandler<SNSEvent, Object> {
    private LambdaLogger logger;

    private void callApiPing() {
        final String url = System.getenv("SHOPBACKEND_API_URL");

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(url);
            logger.log("Executing request " + httpget.getRequestLine());

            CloseableHttpResponse response = httpclient.execute(httpget);

            logger.log(String.valueOf(response.getStatusLine()));
            logger.log(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            // We're cool and we'll ignore it
            e.printStackTrace();
        }
    }

    @Override
    public Object handleRequest(SNSEvent input, Context context) {
        logger = context.getLogger();

        input.getRecords().forEach(snsRecord -> callApiPing());
        return null;
    }
}
