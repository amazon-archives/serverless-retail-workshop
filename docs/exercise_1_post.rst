Exercise #1 - Checklist for completion
======================================

We have now deployed our super basic shop. The architecture we're using
at this point has not changed since Exercise #0.

1. Get the hostname of the Backend order service

::

    ./tools get_value ShopBackendBackendUrl

2. In the Cloud9 terminal you can ssh to the Bastion host doing the following:

::

    ./tools ssh_to_bastion

3. Once SSH’d into the instance you can curl the backend by getting the
   hostname from ElasticBeanstalk and doing

::

    curl <hostname from step 1>/ping

You should see ‘PONG’ back.

To get back to the Cloud9 instance you can just type logout followed by
enter/return.

4. Let's go to our shop, run the following command in the Cloud9 terminal to
   get the URL to go to

::

   ./tools get_value CDNCloudFrontUrl

5. Take the output and enter into your browser, you should be greeted by the
   shop. If not, it might be cached, you can go to `/shop` and it'll likely
   work. If not, put your hand up for a Solutions Architect to come and give
   you a hand!

You have now tested that we have deployed our Shop Frontend and Backend
services successfully.

.. centered:: Exercise #1 is complete, go ahead and start Exercise #2
