package com.way.downloadmanager.net.http;

import com.way.downloadmanager.net.exception.DataErrorEnum;
import com.way.downloadmanager.net.exception.DataException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public final class HttpProxy {

    public byte[] doGet(String url, String[] cookieKeys) throws DataException {

        HttpURLConnection connection = new HttpConnectionBuilder(url,
                HttpConstant.GET).build();

        if (connection == null) {
            throw new DataException(DataErrorEnum.DOWNLOAD_NET_FAILED);
        }

        CookieHolder cookieHolder = CookieHolder.getInstance();
        cookieHolder.setCookie(connection);

        // connection.connect();

        InputStream in = null;

        try {
            in = connection.getInputStream();
            cookieHolder.resolveCookie(connection, cookieKeys);
            if (connection.getResponseCode() != 200) {
                throw new DataException(DataErrorEnum.DOWNLOAD_NET_FAILED);
            }
            return readStream(in);
        } catch (IOException e) {
            throw new DataException(DataErrorEnum.DOWNLOAD_NET_FAILED);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                throw new DataException(DataErrorEnum.DOWNLOAD_NET_FAILED);
            }
            connection.disconnect();
        }

    }

    public byte[] doPost(String url, byte[] data, String[] cookieKeys)
            throws DataException {

        HttpURLConnection connection = new HttpConnectionBuilder(url,
                HttpConstant.POST).setProperty("Content-Length",
                String.valueOf(data.length)).build();

        if (connection == null) {
            throw new DataException(DataErrorEnum.DOWNLOAD_NET_FAILED);
        }

        CookieHolder cookieHolder = CookieHolder.getInstance();
        cookieHolder.setCookie(connection);

        // connection.connect();

        OutputStream out = null;
        InputStream in = null;

        try {
            out = new DataOutputStream(connection.getOutputStream());

            out.write(data);
            out.flush();
            in = connection.getInputStream();
            cookieHolder.resolveCookie(connection, cookieKeys);
            return readStream(in);
        } catch (IOException e) {
            throw new DataException(DataErrorEnum.DOWNLOAD_NET_FAILED);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                throw new DataException(DataErrorEnum.DOWNLOAD_NET_FAILED);
            }

            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                throw new DataException(DataErrorEnum.DOWNLOAD_NET_FAILED);
            }
            connection.disconnect();
        }

    }

    private byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(in);
            int size;
            byte[] buffer = new byte[1024];
            do {
                size = inputStream.read(buffer, 0, buffer.length);
                if (size != -1) {
                    byteStream.write(buffer, 0, size);
                }
            } while (size > -1);
            return byteStream.toByteArray();
        } finally {
            if (in != null) {
                in.close();
            }
            if (byteStream != null) {
                byteStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }

    }

}
