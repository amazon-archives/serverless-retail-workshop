package fishing.lee.infrastructure;

import software.amazon.awscdk.App;

public class InfrastructureApp {
    public static void main(final String argv[]) {
        App app = new App();

        new ShopStack(app, "TheFishingShopWorkshop");

        app.run();
    }
}
