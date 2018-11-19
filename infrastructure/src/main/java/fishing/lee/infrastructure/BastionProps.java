package fishing.lee.infrastructure;

import java.util.Objects;

interface BastionProps {
    ShopVpc getShopVpc();
    String getSshKeyPairName();

    static BastionProps.Builder builder() {
        return new BastionProps.Builder();
    }

    final class Builder {
        ShopVpc _shopVpc;
        String _sshKeyPairName;

        BastionProps.Builder withShopVpc(ShopVpc shopVpc) {
            this._shopVpc = shopVpc;
            return this;
        }

        BastionProps.Builder withSshKeyPairName(@SuppressWarnings("SameParameterValue") String sshKeyPairName) {
            this._sshKeyPairName = sshKeyPairName;
            return this;
        }

        BastionProps build() {
            return new BastionProps() {
                private ShopVpc shopVpc;
                private String sshKeyPairName;

                {
                    shopVpc = Objects.requireNonNull(Builder.this._shopVpc, "shopVpc Required");
                    sshKeyPairName = Objects.requireNonNull(Builder.this._sshKeyPairName, "sshKeyPairName Required");
                }

                @Override
                public ShopVpc getShopVpc() {
                    return this.shopVpc;
                }

                @Override
                public String getSshKeyPairName() {
                    return this.sshKeyPairName;
                }
            };
        }
    }
}
