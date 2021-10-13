package com.nis.common.config;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Http resttemplate 配置
 **/
@Configuration
public class RestTemplateConfig {
	/**
	 * # 从连接池获取连接的timeout,不宜过大,ms
	 */
	@Value("${http-pool.connection-request-timeout:200}")
	private int connectionRequestTimeout;
	/**
	 * 指客户端和服务器建立连接的超时时间,ms , 最大约21秒,因为内部tcp在进行三次握手建立连接时,默认tcp超时时间是20秒
	 */
	@Value("${http-pool.connection-timeout:1000}")
	private int connectionTimeout;
	/**
	 * 指客户端从服务器读取数据包的间隔超时时间,不是总读取时间,也就是socket timeout,ms
	 */
	@Value("${http-pool.socket-timeout:5000}")
	private int socketTimeout;
	/**
	 * #每个路由的最大连接数,如果只调用一个地址,可以将其设置为最大连接数
	 */
	@Value("${http-pool.max-per-route:10000}")
	private int maxPerRoute;
	/**
	 * #连接池的最大连接数，0代表不限；如果取0，需要考虑连接泄露导致系统崩溃的后果
	 */
	@Value("${http-pool.max-total:1000}")
	private int maxTotal;

	/**
	 * 长连接保持时间 单位s,不宜过长
	 */
	@Value("${http-pool.keep-alive-time:60}")
	private int keepAliveTime;

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder, ClientHttpRequestFactory requestFactory) {
		RestTemplate restTemplate =  builder.build();
		restTemplate.setRequestFactory(requestFactory);
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				int rawStatusCode = response.getRawStatusCode();
				HttpStatus statusCode = HttpStatus.resolve(rawStatusCode);
				return (statusCode != null ? statusCode.isError() : hasError(rawStatusCode));
			}

			protected boolean hasError(int unknownStatusCode) {
				HttpStatus.Series series = HttpStatus.Series.resolve(unknownStatusCode);
				return (series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR);
			}

			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}
		});
		return restTemplate;
	}

	@Bean
	public ClientHttpRequestFactory httpRequestFactory(HttpClient client) {
		return new HttpComponentsClientHttpRequestFactory(client);
	}

	@Bean
	public HttpClient httpClient(ConnectionKeepAliveStrategy keepAliveStrategy) throws Exception {
		//配置不校验server 证书
		TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
	        @Override
	        public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
	            return true;
	        }
	    };
	    SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
	    SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", sslConnectionSocketFactory).build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
		// 设置整个连接池最大连接数 根据自己的场景决定
		connectionManager.setMaxTotal(maxTotal);
		// 路由是对maxTotal的细分
		connectionManager.setDefaultMaxPerRoute(maxPerRoute);
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)// 服务器返回数据(response)的时间，超过该时间抛出readtimeout
				.setConnectTimeout(connectionTimeout)// 连接上服务器(握手成功)的时间，超出该时间抛出connect timeout
				.setConnectionRequestTimeout(connectionRequestTimeout)// 从连接池中获取连接的超时时间，超过该时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException:Timeout
																		// waiting for connection from pool
				.build();
		return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setConnectionManager(connectionManager)
				.setKeepAliveStrategy(keepAliveStrategy).build();
	}

	/**
	 * 配置长连接保持策略
	 * 
	 * @return
	 */
	@Bean
	public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
		return new ConnectionKeepAliveStrategy() {
			@Override
			public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
				// Honor 'keep-alive' header
				HeaderElementIterator it = new BasicHeaderElementIterator(
						response.headerIterator(HTTP.CONN_KEEP_ALIVE));
				while (it.hasNext()) {
					HeaderElement he = it.nextElement();
					String param = he.getName();
					String value = he.getValue();
					if (value != null && "timeout".equalsIgnoreCase(param)) {
						try {
							return Long.parseLong(value) * 1000;
						} catch (NumberFormatException ignore) {
						}
					}
				}
				return keepAliveTime * 1000;
			}
		};
	}
}
