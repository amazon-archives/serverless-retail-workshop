Exercise #5 (Optional)
======================

.. warning:: Draft content

We have a reasonable set up so far with our service being served by AWS Lambda.

Now, although the service is running with full scaling capabilities on AWS
Lambda, there are potentially downstream services - databases, job runners,
or physical warehouse limitations - that might limit the speed with which
our overall infrastructure can respond to higher demand and scale.

Ideally, these downstream services would be re-architected to handle the
new scaling requirements, but in the meantime, we can build a small queuing
system in front of our service to handle this. To accomplish this, we'll
use an `Amazon Simple Queue Service`_ queue.

Amazon Simple Queue Service (SQS) is a fully managed message queuing
service that enables you to decouple and scale micro-services, distributed
systems, and serverless applications. SQS eliminates the complexity and
overhead associated with managing and operating message oriented
middleware, and empowers developers to focus on differentiating work.
Using SQS, you can send, store, and receive messages between software
components at any volume, without losing messages or requiring other
services to be available.

Leveraging SQS, we can build a small service to emulate our backend
service which just forwards the request to Amazon SQS and then have a
receiver which forwards those requests onto the original backend
API Gateway.

To do this we have one .jar file (or, Java project) with two different
AWS Lambda functions to be called.

The first of these is `QueueGrabber.java`. Let's examine the file here to see
how this works as we don't use any framework here to do this. Part of the
reason for this is that we get to avoid time-intensive things like Dependency
Injection.

.. Note:: Although you can speed this up using things like Dagger, it's just
          easier in this case not use a Framework at all.

.. code-block:: java

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

Here we quickly check if the verb is OPTIONS, if so, we go ahead and pretend
that all the things are allowed and CORS is okay from any origin.

.. Note:: In a real world scenario, you'd likely limit this to the specific
          domain that the site is hosted.

We then have a `switch` statement around the path requested rather than
a full blown router. Let's compare the two paths:

.. code-block:: java

    case "/ping":
        return new APIGatewayProxyResponseEvent()
                .withHeaders(new HashMap<String, String>()  {{
                    put("Content-Type", "application/json");
                }})
                .withStatusCode(200)
                .withBody("PONG");

Above, we just return a 'PONG' which emulates the existing service.

.. code-block:: java
    :emphasize-lines: 3

    case "/order":
        AmazonSQS amazonSQS = AmazonSQSClientBuilder.defaultClient();
        SendMessageResult result = amazonSQS.sendMessage(sqsQueueName, input.getBody());

        return new APIGatewayProxyResponseEvent()
                .withHeaders(new HashMap<String, String>()  {{
                    put("Content-Type", "application/json");
                }})
                .withStatusCode(200)
                .withBody(result.getMessageId());

The important line is emphasized above. It sends the body of the request into
Amazon SQS. There's no validation here, it could easily be improved with some
order validation before sending on.

We then return the message ID back to the client.

.. Note:: This is, obviously, a slight difference to the existing API and
          the client would normally need to be updated to cope with this.

**The receiver from the Queue**

Now, let's take a look at `OrderForwarder.java` which receives items off of
the queue and sends them to the original service.

.. code-block:: java

    @Override
    public String handleRequest(SQSEvent input, Context context) {
        logger = context.getLogger();

        input.getRecords().forEach(sqsMessage -> {
            String orderDetail = sqsMessage.getBody();
            sendOrder(orderDetail);
        });

        return "";
    }

All we do in the Lambda itself is call sendOrder() with the body of each
message received.

.. code-block:: java

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

We blindly take the body and then POST it to the original Order URL.

**Let's go ahead and deploy it all!**

1. Head over the Cloud9 Console and build our SQS Forwarder. We use Gradle
   for this to build a Fat JAR file which includes all the dependencies
   needed.

   .. code-block:: bash

        cd ~/environment/sqs_order_forwarder
        ./gradlew shadowJar

   .. Note:: This last command can take 1-2 minutes to execute.

2. Now we will need to upload this created JAR file.

   .. tabs::

        .. group-tab:: Tools Script

            .. code-block:: bash

                cd ~/environment
                ./tools upload_sqs_lambda sqs_order_forwarder/build/libs/sqsforward-1.0-SNAPSHOT-all.jar v1

        .. group-tab:: AWS CLI

            .. code-block:: bash

                cd ~/environment
                aws s3 cp sqs_order_forwarder/build/libs/sqsforward-1.0-SNAPSHOT-all.jar s3://`./tools get_value DeploymentAssetsDeploymentBucket`/v1_sqsforwarder_lambda.jar

3. Because we have one JAR file we can use for both the sender and receiver,
   we just upload it to both Lambda functions used for this. During the
   bootstrap phase in Exercise #0 we created dummy functions for us to
   populate now.

   .. tabs::

        .. group-tab:: Tools Script

            .. code-block:: bash

                cd ~/environment
                ./tools deploy_sqs_receiver v1
                ./tools deploy_sqs_forwarder v1

        .. group-tab:: AWS CLI

            .. code-block:: bash

                cd ~/environment

            .. note:: The following block has to be copy/pasted in one go
                      as we are executing multi-line commands.

            .. code-block:: bash

                aws lambda update-function-code \
                    --function-name `./tools get_value QueueProxyToSQSFunctionName` \
                    --s3-bucket `./tools get_value DeploymentAssetsDeploymentBucket` \
                    --s3-key v1_sqsforwarder_lambda.jar \
                    --publish
                aws lambda update-function-code \
                    --function-name `./tools get_value QueueProxyFromSQSFunctionName` \
                    --s3-bucket `./tools get_value DeploymentAssetsDeploymentBucket` \
                    --s3-key v1_sqsforwarder_lambda.jar \
                    --publish

4. With the Lambda function updated we should now test it. To do this
   we can post a dummy item to the /order endpoint of an API Gateway which
   sends requests to the `QueueProxyToSQSFunctionName` Lambda Function and
   then check the CloudWatch logs to see if it worked. To start, let's get the
   URL we're interested in.

   .. code-block:: bash

        ./tools get_value QueueProxyRestApiUrl

   Copy that variable into your clipboard ready for the next part.

5. Now we'll use the URL we grabbed above to test our new queueing endpoint.

   .. code-block:: bash

        ./tools ssh_to_bastion

   Once connected to the Bastion

   .. code-block:: bash

        curl <url from above>/ping

   You should see `PONG` come back.

6. Testing an order is a bit more complex, we need to POST to order. We can
   still do this with cURL.

   .. code-block:: bash

        curl -X POST -H "Content-Type: application/json" -d "{}" <url from above>/order

7. Load up the `CloudWatch Logs <https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logs:>`_
   console.

8. Search for `/aws/lambda/TheFishingShopWorkshop-QueueProxyQueueReceiver` in
   the Filter box and click the Log Group which appears here.

9. Click the most recent Log Stream (there's likely only one).

   .. image:: images/cwl_processing.png

   You can see here the at the queue processor received our order (denoted
   by the `PROCESSING {}` where the {} matches what we sent above. You can then
   see a line that says `$argon...` that is the response from our order
   processing API.

We have successfully made a nearly compatible end point that can be used to
take pressure off our backend services.

.. _Amazon Simple Queue Service : https://aws.amazon.com/sqs/
