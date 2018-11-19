package fishing.lee.infrastructure;

import java.util.Objects;

public interface ShopFrontendProps {
    String getInstanceType();
    int getMinSize();
    int getMaxSize();
    ShopVpc getShopVpc();
    ShopBackend getShopBackend();
    Postgres getPostgres();

    static ShopFrontendProps.Builder builder() {
        return new ShopFrontendProps.Builder();
    }

    final class Builder {
        private String _instanceType;
        private int _minSize;
        private int _maxSize;
        private ShopVpc _shopVpc;
        private ShopBackend _shopBackend;
        private Postgres _postgres;

        ShopFrontendProps.Builder withInstanceType(String instanceType) {
            this._instanceType = instanceType;
            return this;
        }

        ShopFrontendProps.Builder withShopVpc(ShopVpc shopVpc) {
            this._shopVpc = shopVpc;
            return this;
        }

        ShopFrontendProps.Builder withMinSize(int minSize) {
            this._minSize = minSize;
            return this;
        }

        ShopFrontendProps.Builder withMaxSize(int maxSize) {
            this._maxSize = maxSize;
            return this;
        }

        ShopFrontendProps.Builder withShopBackend(ShopBackend shopBackend) {
            this._shopBackend = shopBackend;
            return this;
        }

        ShopFrontendProps.Builder withPostgres(Postgres postgres) {
            this._postgres = postgres;
            return this;
        }

        ShopFrontendProps build() {
            return new ShopFrontendProps() {
                private String instanceType;
                private int minSize;
                private int maxSize;
                private ShopVpc shopVpc;
                private ShopBackend shopBackend;
                private Postgres postgres;

                {
                    instanceType = Objects.nonNull(Builder.this._instanceType) ? Builder.this._instanceType : "t3.micro";
                    minSize = Builder.this._minSize > 0 ? Builder.this._minSize : 1;
                    maxSize = Builder.this._maxSize > 0 ? Builder.this._maxSize : 1;
                    shopVpc = Objects.requireNonNull(Builder.this._shopVpc, "shopVpc is required");
                    shopBackend = Objects.requireNonNull(Builder.this._shopBackend, "shopBackend is required");
                    postgres = Objects.requireNonNull(Builder.this._postgres, "postgres instance required");
                }

                @Override
                public String getInstanceType() {
                    return instanceType;
                }

                @Override
                public int getMinSize() {
                    return minSize;
                }

                @Override
                public int getMaxSize() {
                    return maxSize;
                }

                @Override
                public ShopVpc getShopVpc() {
                    return shopVpc;
                }

                @Override
                public ShopBackend getShopBackend() {
                    return shopBackend;
                }

                @Override
                public Postgres getPostgres() {
                    return postgres;
                }
            };
        }
    }
}
