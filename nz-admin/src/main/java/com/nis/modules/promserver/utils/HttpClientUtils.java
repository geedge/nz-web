package com.nis.modules.promserver.utils;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * HttpClient4.3工具类
 *
 * @author hang.luo
 */
public class HttpClientUtils {
    private static Log logger = Log.get(); // 日志记录

    private static RequestConfig requestConfig = null;

    static {
        // 设置请求和传输超时时间
        requestConfig = RequestConfig.custom().setSocketTimeout(10000).setConnectTimeout(10000).build();
    }

    /**
     * post请求传输json参数
     *
     * @param url  url地址
     * @param jsonParam 参数
     * @return
     */
    public static JSONObject httpPost(String url, JSONObject jsonParam) {
        // post请求返回结果
        CloseableHttpClient httpClient = HttpClients.createDefault();
        JSONObject jsonResult = null;
        HttpPost httpPost = new HttpPost(url);
        // 设置请求和传输超时时间
        httpPost.setConfig(requestConfig);
        try {
            if (null != jsonParam) {
                // 解决中文乱码问题
                StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/json");
                httpPost.setEntity(entity);
            }
            CloseableHttpResponse result = httpClient.execute(httpPost);
            // 请求发送成功，并得到响应
            String str = "";
            try {
            	// 防止有的响应没有返回值报异常情况
            	if(ObjectUtils.isNotEmpty(result.getEntity())) {
            		// 读取服务器返回过来的json字符串数据
                    str = EntityUtils.toString(result.getEntity(), "utf-8");
            	}
                // 把json字符串转换成json对象
                jsonResult = JSONObject.parseObject(str);
            } catch (Exception e) {
                logger.error("submit post request faild:" + url,e);
            }
        } catch (IOException e) {
            logger.error("submit post request faild:" + url,e);
        } finally {
            httpPost.releaseConnection();
            IOUtils.closeQuietly(httpClient);
        }
        return jsonResult;
    }

    /**
     * post请求传输String参数 例如：name=Jack&sex=1&type=2
     * Content-type:application/x-www-form-urlencoded
     *
     * @param url      url地址
     * @param strParam 参数
     * @return
     */
    public static JSONObject httpPost(String url, String strParam) {
        // post请求返回结果
        CloseableHttpClient httpClient = HttpClients.createDefault();
        JSONObject jsonResult = null;
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);
        try {
            if (null != strParam && !"".equals(strParam)) {
                // 解决中文乱码问题
                StringEntity entity = new StringEntity(strParam, "utf-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/x-www-form-urlencoded");
                httpPost.setEntity(entity);
            }
            CloseableHttpResponse result = httpClient.execute(httpPost);
            // 请求发送成功，并得到响应
            String str = "";
            try {
            	// 防止有的响应没有返回值报异常情况
            	if(ObjectUtils.isNotEmpty(result.getEntity())) {
            		// 读取服务器返回过来的json字符串数据
                    str = EntityUtils.toString(result.getEntity(), "utf-8");
            	}
                // 把json字符串转换成json对象
                jsonResult = JSONObject.parseObject(str);
            } catch (Exception e) {
                logger.error("submit post request faild:" + url,e);
            }
        } catch (IOException e) {
            logger.error("submit post request faild:" + url,e);
        } finally {
            httpPost.releaseConnection();
            try {
				httpClient.close();
			} catch (IOException e) {
				logger.error(e);
			}
        }
        return jsonResult;
    }

    /**
     * 发送get请求
     *
     * @param url 路径
     * @return
     */
    public static JSONObject httpGet(String url) {
        // get请求返回结果
        JSONObject jsonResult = null;
        CloseableHttpClient client = HttpClients.createDefault();
        // 发送get请求
        HttpGet request = new HttpGet(url);
        request.setConfig(requestConfig);
        try {
            CloseableHttpResponse response = client.execute(request);

            // 读取服务器返回过来的json字符串数据
            HttpEntity entity = response.getEntity();
            String strResult = EntityUtils.toString(entity, "utf-8");
            // 把json字符串转换成json对象
            jsonResult = JSONObject.parseObject(strResult);
        } catch (IOException e) {
            logger.error("submit get request faild:" + url,e);
        } finally {
            request.releaseConnection();
            try {
				client.close();
			} catch (IOException e) {
				logger.error(e);
			}
        }
        return jsonResult;
    }

    /**
     * 发送get请求
     *
     * @param url 路径
     * @return
     */
    public static Integer httpGetForCode(String url) {
        // get请求返回结果
    	Integer statusCode  = null;
        CloseableHttpClient client = HttpClients.createDefault();
        // 发送get请求
        HttpGet request = new HttpGet(url);
        request.setConfig(requestConfig);
        try {
            CloseableHttpResponse response = client.execute(request);
            statusCode = response.getStatusLine().getStatusCode();
            return statusCode;
   
        } catch (IOException e) {
            logger.error("submit get request faild:" + url,e);
        } finally {
            request.releaseConnection();
            try {
				client.close();
			} catch (IOException e) {
				logger.error(e);
			}
        }
        return statusCode;
    }
}