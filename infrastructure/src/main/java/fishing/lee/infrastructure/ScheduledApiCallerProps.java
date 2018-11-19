package fishing.lee.infrastructure;

import java.util.Objects;

public interface ScheduledApiCallerProps {
    ShopVpc getShopVpc();
    String getApiToPing();

    static ScheduledApiCallerProps.Builder builder() {
        return new ScheduledApiCallerProps.Builder();
    }

    final class Builder {
        private ShopVpc _shopVpc;
        private String _apiToPing;

        ScheduledApiCallerProps.Builder withShopVpc(ShopVpc shopVpc) {
            this._shopVpc = shopVpc;
            return this;
        }

        ScheduledApiCallerProps.Builder withApiToPing(String apiToPing) {
            this._apiToPing = apiToPing;
            return this;
        }

        ScheduledApiCallerProps build() {
            return new ScheduledApiCallerProps() {
                private ShopVpc shopVpc;
                private String apiToPing;

                {
                    shopVpc = Objects.requireNonNull(ScheduledApiCallerProps.Builder.this._shopVpc, "shopVpc is required");
                    apiToPing = Objects.requireNonNull(ScheduledApiCallerProps.Builder.this._apiToPing);
                }

                @Override
                public ShopVpc getShopVpc() {
                    return shopVpc;
                }

                @Override
                public String getApiToPing() {
                    return apiToPing;
                }
            };
        }
    }
}
