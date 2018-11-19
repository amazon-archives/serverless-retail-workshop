package fishing.lee.infrastructure;

import java.util.Objects;

interface PostgresProps {
    ShopVpc getShopVpc();

    static PostgresProps.Builder builder() {
        return new PostgresProps.Builder();
    }

    final class Builder {
        ShopVpc _shopVpc;

        PostgresProps.Builder withShopVpc(ShopVpc shopVpc) {
            this._shopVpc = shopVpc;
            return this;
        }

        PostgresProps build() {
            return new PostgresProps() {
                private ShopVpc shopVpc;

                {
                    shopVpc = Objects.requireNonNull(Builder.this._shopVpc, "shopVpc Required");
                }

                @Override
                public ShopVpc getShopVpc() {
                    return this.shopVpc;
                }
            };
        }
    }
}
