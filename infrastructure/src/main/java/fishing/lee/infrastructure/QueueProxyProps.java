package fishing.lee.infrastructure;

import java.util.Objects;

interface QueueProxyProps {
    ShopVpc getShopVpc();
    String getApiUrl();

    static QueueProxyProps.Builder builder() {
        return new QueueProxyProps.Builder();
    }

    final class Builder {
        ShopVpc _shopVpc;
        String _apiUrl;

        QueueProxyProps.Builder withShopVpc(ShopVpc shopVpc) {
            this._shopVpc = shopVpc;
            return this;
        }

        QueueProxyProps.Builder withApiUrl(String apiUrl) {
            this._apiUrl = apiUrl;
            return this;
        }

        QueueProxyProps build() {
            return new QueueProxyProps() {
                private ShopVpc shopVpc;
                private String apiUrl;

                {
                    shopVpc = Objects.requireNonNull(Builder.this._shopVpc, "shopVpc Required");
                    apiUrl = Objects.requireNonNull(Builder.this._apiUrl);
                }

                @Override
                public ShopVpc getShopVpc() {
                    return this.shopVpc;
                }

                @Override
                public String getApiUrl() {
                    return this.apiUrl;
                }
            };
        }
    }

}
