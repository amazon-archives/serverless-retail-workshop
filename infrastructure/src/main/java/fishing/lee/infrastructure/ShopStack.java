package fishing.lee.infrastructure;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

class ShopStack extends Stack {
    ShopStack(final App parent, final String name) {
        this(parent, name, null);
    }

    private ShopStack(final App parent, final String name, final StackProps props) {
        super(parent, name, props);

        ShopVpc shopVpc = new ShopVpc(this, "Vpc");

        Postgres postgres = new Postgres(this, "ShopDatabase", PostgresProps.builder()
                .withShopVpc(shopVpc)
                .build());

        Bastion bastion = new Bastion(this, "Bastion", BastionProps.builder()
                .withShopVpc(shopVpc)
                .withSshKeyPairName("FishingKey")
                .build());

        ShopBackend shopBackend = new ShopBackend(this, "ShopBackend", ShopBackendProps.builder()
                .withShopVpc(shopVpc)
                .withBastionSecurityGroup(bastion.getBastionSecurityGroup())
                .build());

        new ScheduledApiCaller(this, "ApiPing", ScheduledApiCallerProps.builder()
                .withShopVpc(shopVpc)
                .withApiToPing(shopBackend.getLambdaApiUrl())
                .build());

        new ShopFrontend(this, "ShopFrontend", ShopFrontendProps.builder()
                .withShopVpc(shopVpc)
                .withShopBackend(shopBackend)
                .withPostgres(postgres)
                .build());

        new QueueProxy(this, "QueueProxy", QueueProxyProps.builder()
                .withShopVpc(shopVpc)
                .withApiUrl(shopBackend.getLambdaApiUrl())
                .build());

        new Decoupling(this, "Decoupling");

        new DeploymentAssets(this, "DeploymentAssets");

        new ContentDistribution(this, "CDN");
    }
}
