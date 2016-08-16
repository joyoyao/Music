package com.way.downloadmanager.net.http;

import android.text.TextUtils;

import com.way.downloadmanager.net.exception.DataErrorEnum;
import com.way.downloadmanager.net.exception.DataException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class HttpConnectionBuilder {

    private String url;

    private String method;

    private int connectionTimeout;

    private int readTimeout;

    private boolean useCache;

    private boolean doOutput;

    private Map<String, String> properties;

    public HttpConnectionBuilder(String url, String method) {
        this.url = url;
        this.method = method;
        connectionTimeout = 5 * 1000;
        readTimeout = 10 * 1000;
        useCache = false;
        doOutput = false;

        properties = new HashMap<String, String>();
        if (TextUtils.equals(method, HttpConstant.POST)) {
            properties.put("Content-Type", "application/x-www-form-urlencoded");
            properties.put("Accept", "*/*");
            properties.put("Accept-Charset", "UTF8");
            properties.put("Connection", "Keep-Alive");
            properties.put("Cache-Control", "no-cache");
            doOutput = true;
        }
        System.setProperty("http.keepAlive", "false");
    }

    public HttpURLConnection build() throws DataException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url)
                    .openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setUseCaches(useCache);
            connection.setDoOutput(doOutput);

            if (TextUtils.equals(method, HttpConstant.POST)) {
                int contentLength = Integer.parseInt(properties
                        .get("Content-Length"));
                connection.setFixedLengthStreamingMode(contentLength);
            }

            Set<Entry<String, String>> set = properties.entrySet();
            if (set != null) {
                Iterator<Entry<String, String>> iterator = set.iterator();
                while (iterator.hasNext()) {
                    Entry<String, String> entry = iterator.next();
                    connection.addRequestProperty(entry.getKey(),
                            entry.getValue());
                }
            }

            return connection;
        } catch (Exception e) {
            throw new DataException(DataErrorEnum.DOWNLOAD_NET_FAILED);
        }
    }

    public HttpConnectionBuilder setUrl(String url) {
        this.url = url;
        return this;
    }

    public HttpConnectionBuilder setMethod(String method) {
        this.method = method;
        return this;
    }

    public HttpConnectionBuilder setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public HttpConnectionBuilder setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public HttpConnectionBuilder setUseCache(boolean useCache) {
        this.useCache = useCache;
        return this;
    }

    public HttpConnectionBuilder setDoOutput(boolean doOutput) {
        this.doOutput = doOutput;
        return this;
    }

    public HttpConnectionBuilder setProperty(String key, String value) {
        properties.put(key, value);
        return this;
    }

}
