package fishing.lee.infrastructure;

import software.amazon.awscdk.services.ec2.SecurityGroup;

import java.util.Objects;

public interface ShopBackendProps {
    String getInstanceType();
    int getMinSize();
    int getMaxSize();
    ShopVpc getShopVpc();
    SecurityGroup getBastionSecurityGroup();

    static ShopBackendProps.Builder builder() {
        return new ShopBackendProps.Builder();
    }

    final class Builder {
        private String _instanceType;
        private int _minSize;
        private int _maxSize;
        private ShopVpc _shopVpc;
        private SecurityGroup _bastionSecurityGroup;

        ShopBackendProps.Builder withInstanceType(String instanceType) {
            this._instanceType = instanceType;
            return this;
        }

        ShopBackendProps.Builder withShopVpc(ShopVpc shopVpc) {
            this._shopVpc = shopVpc;
            return this;
        }

        ShopBackendProps.Builder withMinSize(int minSize) {
            this._minSize = minSize;
            return this;
        }

        ShopBackendProps.Builder withMaxSize(int maxSize) {
            this._maxSize = maxSize;
            return this;
        }

        ShopBackendProps.Builder withBastionSecurityGroup(SecurityGroup securityGroup) {
            this._bastionSecurityGroup = securityGroup;
            return this;
        }

        ShopBackendProps build() {
            return new ShopBackendProps() {
                private String instanceType;
                private int minSize;
                private int maxSize;
                private ShopVpc shopVpc;
                private SecurityGroup bastionSecurityGroup;

                {
                    instanceType = Objects.nonNull(Builder.this._instanceType) ? Builder.this._instanceType : "t3.micro";
                    minSize = Builder.this._minSize > 0 ? Builder.this._minSize : 1;
                    maxSize = Builder.this._maxSize > 0 ? Builder.this._maxSize : 1;
                    shopVpc = Objects.requireNonNull(Builder.this._shopVpc, "shopVpc is required");
                    bastionSecurityGroup = Builder.this._bastionSecurityGroup;
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
                public SecurityGroup getBastionSecurityGroup() {
                    return bastionSecurityGroup;
                }
            };
        }
    }
}
