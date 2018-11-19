Exercise #0 - Environment Setup
===============================

There are a few services to deploy to kick off and because this is an empty
account they can take a few minutes to deploy. The engineers we had hired have
put together some AWS Cloud Development Kit stacks for us.

.. Hint:: The `AWS Cloud Development Kit`_ (or, CDK) is in Developer Preview on
          GitHub. We are using the Java version here and you can see all the
          code within the infrastructure folder within AWS Cloud9.

The first thing we need to do is setup our IDE. For this workshop, we'll use
the AWS Cloud9 IDE. AWS Cloud9 is a cloud-based integrated development
environment (IDE) that lets you write, run, and debug your code with just a
browser. It includes a code editor, debugger, and terminal. Cloud9 comes
prepackaged with essential tools for popular programming languages, including
JavaScript, Python, PHP, and more, so you don’t need to install files or
configure your development machine to start new projects.

.. Attention:: This workshop uses AWS Cloud9 and we have only tested this
               workshop with Google Chrome and Mozilla Firefox. Please do not
               use Microsoft Edge, Internet Explorer, Safari, Opera or any of
               the numerous forks of browsers that exist.

Steps:

1. Log into your AWS Account Console. You'll need an account with
   Administrator Access to proceed with the workshop. Although this workshop
   will fit within default limits of a new AWS account, be mindful that if
   you are using an existing account you may hit your limits. Try and use an
   empty account, if possible.
2. When logged in, select N. Virginia as the region (us-east-1).
3. Type in `Cloud9` into the Services box and click the suggested service name.
   You can also find it under All Services, within Developer Tools.
4. Click the `Create environment` button
5. Put in a suitable name, we suggest `Fishing`. Description is optional, but
   we suggest "I just fish we had more time".
6. Click `Next Step`
7. For Instance Type, select `m4.large` – we’ll be doing a lot of Java. Be
   aware that this costs $0.1 per hour in the us-east-1 region.
8. Cost-saving setting – set to After 4 hours. What’s cool here is the instance
   will turn itself off, if you forget to delete it after the workshop, after 4
   hours.
9. Click `Next Step`
10. Review the changes and click `Create environment`
11.	After a while the IDE will appear!

    .. image:: images/cloud9.png

12. You’ll be living in the Terminal for a lot of this so for now either expand
    it or click the button shown below to make it full browser size:

    .. image:: images/maxiterminal.png

Now that we've completed setup of our IDE, we're going to download the files
provided by our engineers to configure our Cloud9 IDE with the packages and
tools required for launching the new fishing shop website.

13.	Run the following commands - each line is a separate command so

    .. Hint:: Although it's tempting to copy all this at once - don't. Copy
              each line one by one and it'll be a lot more likely to be
              successful.

              This is due to the way pasting new lines into a terminal can
              cause buffering issues.

    You can either do this using the pre-bundled Tools Script or Manually. We
    recommend you using the Tools Script and look at the Manual tab for what
    it's doing behind the scenes.

    .. tabs::

        .. group-tab:: Tools Script

            .. code-block:: bash
                :linenos:

                curl -O https://fishing.serverlessretail.com/files/student-files.zip
                unzip student-files.zip
                ./tools install
                ./tools create_ssh_key
                nvm install

            At this point you might see something like
            `v8.12.0 is already installed.`, it is fine to ignore, it means
            you're on the right track.

        .. group-tab:: Manual

            .. code-block:: bash
                :linenos:

                curl -O https://fishing.serverlessretail.com/files/student-files.zip
                unzip student-files.zip
                sudo yum remove -y java-1.7.0-openjdk java-1.7.0-openjdk-devel
                sudo yum install -y jq java-1.8.0-openjdk-devel
                npm i -g aws-cdk@0.17.0
                nvm install v8.12.0
                curl --silent --location https://dl.yarnpkg.com/rpm/yarn.repo | sudo tee /etc/yum.repos.d/yarn.repo
                sudo yum install -y yarn

14. With the general setup done, we now need to use the
    `AWS Cloud Development Kit`_ to deploy our AWS CloudFormation stacks.
    First, we compile the Java code that defines our Stacks and then run the
    `cdk` commands to deploy it. The longest part to run is the `cdk deploy` so
    make sure you get to run that command before pausing to get coffee or watch
    the talks.

    .. code-block:: bash
        :linenos:

        cd infrastructure
        ./mvnw package
        cdk bootstrap
        cdk deploy

    .. Attention:: This step can take around 15-20 minutes. Pause here and watch
                the talk or read through the Overview. Also, if you're curious,
                you can take a look through the CDK code. The code is managed in
                infrastructure/src/main/java/fishing/lee/infrastructure/.
                The CDK app is located in InfrastructureApp.java, which
                references the construct in ShopStack.java. For more information,
                see `AWS Cloud Development Kit`_.

    .. Note:: The counter on the left of each line will go over the max number.
            This is due to an open issue with the `AWS Cloud Development Kit`_
            and is nothing to worry about. It will finish! It's because the
            stacks are optimised to reduce the deployment time to allow maximum
            time to work through the exercises.

    .. Note:: You might see something like the following during the output and
            you can safely ignore it!

    .. image:: images/yumwarning.png

    .. Note:: You might also see this at the top of the screen and it is also
            something to safely ignore.

    .. image:: images/cloud9_warning.png

14. At the end of the process there's some output variables and you can
    continue on with the exercises when instructed.

.. centered:: **Exercise #0 is complete, please wait to be told to start Exercise #1**

.. _AWS Cloud Development Kit : https://github.com/awslabs/aws-cdk
