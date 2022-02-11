package com.nis.common.config;

import io.micrometer.core.instrument.Tag;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTags;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Component
public class MyRestTemplateExchangeTagsProvider implements RestTemplateExchangeTagsProvider {

    @Override
    public Iterable<Tag> getTags(String urlTemplate, HttpRequest request, ClientHttpResponse response) {
        Tag uriTag;
        // 取 request.getURI().getPath() 作为 uri 的 value
        if (StringUtils.hasText(request.getURI().getPath())) {
            uriTag = Tag.of("uri", request.getURI().getPath());
        } else {
            uriTag = (StringUtils.hasText(urlTemplate) ? RestTemplateExchangeTags.uri(urlTemplate)
                    : RestTemplateExchangeTags.uri(request));
        }
        return Arrays.asList(RestTemplateExchangeTags.method(request), uriTag,
                RestTemplateExchangeTags.status(response), RestTemplateExchangeTags.clientName(request));
    }

}
