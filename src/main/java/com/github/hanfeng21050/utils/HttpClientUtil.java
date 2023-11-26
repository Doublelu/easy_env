/*
 * ϵͳ����: ҵ������Ӫƽ̨
 * ģ������:
 * �� �� ��: HttpClientUtil.java
 * �����Ȩ: ���ݺ������ӹɷ����޹�˾
 * ����ĵ�:
 * �޸ļ�¼:
 * �޸����� �޸���Ա �޸�˵��<BR>
 * ======== ====== ============================================
 * ======== ====== ============================================
 * �����¼��
 * ������Ա��
 * �������ڣ�
 * �������⣺
 */

package com.github.hanfeng21050.utils;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.DefaultHttpResponseParserFactory;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * http�ͻ��˹�����
 * httpclient
 *
 * @author niusw40398
 * @date 2022/12/26
 */
public class HttpClientUtil {

    private final static Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    /**
     * httpclient���ӳ�
     */
    private static PoolingHttpClientConnectionManager manager = null;

    /**
     * http�ͻ���
     */
    private static volatile CloseableHttpClient httpClient = null;

    /**
     * Ĭ����������
     */
    private static RequestConfig defaultRequestConfig = null;

    private static CookieStore cookieStore = new BasicCookieStore();

    /**
     * ��ʼ��
     */
    private static void init() {
        // Ĭ����������
        defaultRequestConfig = RequestConfig.custom()
                // HttpClient�е�Ҫ������ʱ���Դ����ӳ��л�ȡ�������ڵȴ���һ����ʱ���û�л�ȡ���������ӣ��������ӳ���û�п��������ˣ�����׳���ȡ���ӳ�ʱ�쳣��
                .setConnectTimeout(2000)
                // ָ��������Ŀ��url�����ӳ�ʱʱ�䣬���ͷ��˷���������Ŀ��url���������ӵ����ʱ�䡣����ڸ�ʱ�䷶Χ�ڻ�û�н��������ӣ�����׳�connectionTimeOut�쳣��
                .setSocketTimeout(3 * 60 * 1000)
                // ������һ��url�󣬻�ȡresponse�ķ��صȴ�ʱ�� ��������Ŀ��url�������Ӻ󣬵ȴ��Ż�response�����ʱ�䣬�ڹ涨ʱ����û�з�����Ӧ�Ļ����׳�SocketTimeout��
                .setConnectionRequestTimeout(60 * 1000)
                .build();

        // httpclietn �ع�����
        SSLConnectionSocketFactory scsf = null;
        try {
            /// ���httpClient����https���������
            scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);

            // ע�����Э����ص� socket ����
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", scsf).build();
            // HttpConnection����������д����/������Ӧ������
            HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = new ManagedHttpClientConnectionFactory(DefaultHttpRequestWriterFactory.INSTANCE,
                    DefaultHttpResponseParserFactory.INSTANCE);
            // DNS ������
            DnsResolver dnsResolver = SystemDefaultDnsResolver.INSTANCE;
            // �����ػ����ӹ�����
            manager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, connFactory, dnsResolver);
            // Ĭ��ΪSocket ����
            SocketConfig defaultSocketConfig = SocketConfig.custom().setTcpNoDelay(true).build();
            manager.setDefaultSocketConfig(defaultSocketConfig);
            // �����������ӳص���������� Ĭ��20
            manager.setMaxTotal(200);
            // ÿ��·����������� Ĭ��2
            manager.setDefaultMaxPerRoute(20);
            // �ڴ����ӳػ�ȡ����ʱ�����Ӳ���Ծ�೤ʱ�����Ҫ����һ����֤��Ĭ��Ϊ 2s
            manager.setValidateAfterInactivity(5 * 1000);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

    /**
     * �õ�http�ͻ���
     *
     * @return {@link CloseableHttpClient}
     */
    public static synchronized CloseableHttpClient getHttpClient() {
        if (manager == null) {
            init();
        }
        // ˫����, ��ֻ֤��һ��ʵ��
        if (httpClient == null) {
            synchronized (HttpClientUtil.class) {
                if (httpClient == null) {
                    httpClient = HttpClients.custom().setConnectionManager(manager)
                            // ���ӳز��ǹ���ģʽ
                            .setConnectionManagerShared(false)
                            // ���ڻ��չ�������
                            .evictExpiredConnections()
                            // ���Ӵ��ʱ�䣬��������ã�����ݳ�������Ϣ����
                            .setConnectionTimeToLive(60, TimeUnit.SECONDS)
                            // ����Ĭ����������
                            .setDefaultRequestConfig(defaultRequestConfig)
                            // �������ò���
                            .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
                            // ���������ã�����ó����������೤ʱ��
                            .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                            .setRetryHandler(DefaultHttpRequestRetryHandler.INSTANCE)
                            .setDefaultCookieStore(cookieStore)
                            .build();
                }
            }

        }

        // JVMֹͣ������ʱ �ر����ӳ��ͷŵ�����
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        });

        return httpClient;
    }

    /**
     * ��������ͷ
     *
     * @param httpRequestBase ����
     * @param header          ����ͷ
     */
    private static void setRequestHeader(HttpRequestBase httpRequestBase, Map<String, String> header) {
        if (header != null && header.size() > 0) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                httpRequestBase.addHeader(key, value);
            }
        }
    }

    /**
     * post����
     *
     * @param url    �����ַ
     * @param body   ������
     * @param header ����ͷ
     * @return {@link String}
     */
    public static String httpPost(String url, Map<String, String> body, Map<String, String> header) throws IOException {
        List<NameValuePair> valuePairs = new LinkedList<NameValuePair>();
        for (Map.Entry<String, String> entry : body.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            valuePairs.add(new BasicNameValuePair(key, value));
        }
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new UrlEncodedFormEntity(valuePairs, Consts.UTF_8));
        setRequestHeader(httpPost, header);
        CloseableHttpResponse response = null;
        try {
            response = request(httpPost);
            return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        } finally {
            if (null != response) {
                EntityUtils.consume(response.getEntity());
            }
        }
    }

    /**
     * post����
     *
     * @param url    �����ַ
     * @param body   ������
     * @param header ����ͷ
     * @return {@link String}
     */
    public static String httpPost(String url, String body, Map<String, String> header) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(body, "UTF-8"));
        setRequestHeader(httpPost, header);
        CloseableHttpResponse response = null;
        try {
            response = request(httpPost);
            return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        } finally {
            if (null != response) {
                EntityUtils.consume(response.getEntity());
            }
        }
    }


    /**
     * post����
     *
     * @param url        �����ַ
     * @param httpEntity ������
     * @param header     ����ͷ
     * @return {@link String}
     * @throws IOException ioexception
     */
    public static String httpPost(String url, HttpEntity httpEntity, Map<String, String> header) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(httpEntity);
        setRequestHeader(httpPost, header);
        CloseableHttpResponse response = null;
        try {
            response = request(httpPost);
            return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        } finally {
            if (null != response) {
                EntityUtils.consume(response.getEntity());
            }
        }
    }


    /**
     * http post
     *
     * @param url  �����ַ
     * @param body ������
     * @return {@link String}
     */
    public static String httpPost(String url, String body) throws IOException {
        return httpPost(url, body, null);
    }

    /**
     * post����
     *
     * @param url  �����ַ
     * @param body ������
     * @return {@link String}
     */
    public static String httpPost(String url, Map<String, String> body) throws IOException {
        return httpPost(url, body, null);
    }

    /**
     * post����
     *
     * @param url        �����ַ
     * @param httpEntity ������
     * @return {@link String}
     * @throws IOException ioexception
     */
    public static String httpPost(String url, HttpEntity httpEntity) throws IOException {
        return httpPost(url, httpEntity, null);
    }


    /**
     * http get����
     *
     * @param url �����ַ
     * @return {@link String}
     */
    public static String httpGet(String url) throws URISyntaxException, IOException {
        return httpGet(url, null, null);
    }

    /**
     * http get����
     *
     * @param url    �����ַ
     * @param params �������
     * @return {@link String}
     */
    public static String httpGet(String url, Map<String, String> params) throws URISyntaxException, IOException {
        return httpGet(url, params, null);
    }

    /**
     * http get����
     *
     * @param url    �����ַ
     * @param params �������
     * @param header ����ͷ
     * @return {@link String}
     */
    public static String httpGet(String url, Map<String, String> params, Map<String, String> header) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(url);
        // �����������
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue());
            }
        }
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.getParams().setParameter("http.protocol.allow-circular-redirects", true);
        // ��������ͷ
        setRequestHeader(httpGet, header);

        CloseableHttpResponse response = null;
        try {
            response = request(httpGet);
            return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        } finally {
            if (null != response) {
                EntityUtils.consume(response.getEntity());
            }
        }
    }


    /**
     * post
     *
     * @param httpRequest http post����
     * @return {@link String}
     */
    public static CloseableHttpResponse request(HttpRequestBase httpRequest) throws IOException {
        CloseableHttpClient closeableHttpClient = getHttpClient();
        return closeableHttpClient.execute(httpRequest);
    }

    /**
     * ���cookie
     */
    public static void clearCookie() {
        cookieStore.clear();
    }
}
