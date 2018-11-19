package fishing.lee.backend;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.plugins.EC2Plugin;
import com.amazonaws.xray.plugins.ElasticBeanstalkPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import java.io.IOException;

@Configuration
public class WebConfig {
    @Value("${amazon.xray.selfhosted}")
    private Boolean xrayEnabled = false;

    @Bean
    Boolean shouldCreateSegments() {
        return xrayEnabled;
    }

    @Bean
    public Filter TracingFilter() {
        if (xrayEnabled) {
            return new AWSXRayServletFilter("Backend");
        }

        // Return an empty filter
        return new Filter() {
            @Override
            public void init(FilterConfig filterConfig) {

            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                chain.doFilter(request, response);
            }

            @Override
            public void destroy() {

            }
        };
    }

    static {
        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
                .withPlugin(new EC2Plugin())
                .withPlugin(new ElasticBeanstalkPlugin());
        AWSXRay.setGlobalRecorder(builder.build());
    }
}
