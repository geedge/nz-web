package com.nis.common.interceptor;

import com.nis.modules.metric.dto.NezhaMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class MetricsShiroFilter extends AccessControlFilter {

    @Autowired
    private NezhaMetrics nezhaMetrics;

    @Autowired
    private MeterRegistry meterRegistry;

    @Override
    protected boolean isAccessAllowed(ServletRequest servletRequest, ServletResponse servletResponse, Object o) throws Exception {
        nezhaMetrics.bindTo(meterRegistry);
        return true;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        return false;
    }
}
