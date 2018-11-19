package fishing.lee.infrastructure;

import java.util.Objects;

interface ApiGatewayXrayEnablerResourceProps {
    String getRestApiId();
    String getStageName();

    static ApiGatewayXrayEnablerResourceProps.Builder build() {
        return new ApiGatewayXrayEnablerResourceProps.Builder();
    }

    final class Builder {
        String _restApiId;
        String _stageName;

        ApiGatewayXrayEnablerResourceProps.Builder withRestApiId(String restApiId) {
            this._restApiId = restApiId;
            return this;
        }

        ApiGatewayXrayEnablerResourceProps.Builder withStageName(String stageName) {
            this._stageName = stageName;
            return this;
        }

        ApiGatewayXrayEnablerResourceProps build() {
            return new ApiGatewayXrayEnablerResourceProps() {
                private String restApiId;
                private String stageName;

                {
                    restApiId = Objects.requireNonNull(Builder.this._restApiId);
                    stageName = Objects.requireNonNull(Builder.this._stageName);
                }

                @Override
                public String getRestApiId() {
                    return restApiId;
                }

                @Override
                public String getStageName() {
                    return stageName;
                }
            };
        }
    }
}
