package com.example.java;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TelecomIPApplication {

    public static void main(String[] args) throws IOException {
        TelecomIPApplication telecomIPApplication = new TelecomIPApplication();
        System.out.print("\u001B[33m------\t正在获取您的网关IP");
        System.out.println("\u001B[0m\u001B[31m(暂时仅支持电信)");
        TelecomIPApplication.IPTuple ipTuple = telecomIPApplication.loadWLanNATInfo();
        System.out.printf("\u001B[0m\u001B[33m------\tWLAN:\t%s\n", ipTuple.getWANIP());
        System.out.printf("------\tWLANv6:\t%s\n", ipTuple.getWANIPv6());
    }

    private final Properties properties;

    public TelecomIPApplication() throws IOException {

        this.properties = new Properties();
        InputStream inputStream = AliyunSDKApplication.class
                .getResourceAsStream("/application.properties");
        if (inputStream != null) {
            this.properties.load(inputStream);
            inputStream.close();
        }

        final String[] keys = new String[]{"wlan.nat.ip", "wlan.nat.username", "wlan.nat.password"};
        for (String key : keys) {
            String value = System.getProperty(key);
            if (value == null || value.trim().length() == 0) {
                continue;
            }
            this.properties.put(key, value);
        }
    }

    public TelecomIPApplication(Properties properties) {
        this.properties = new Properties();
        properties.forEach(properties::put);
    }

    public IPTuple loadWLanNATInfo() throws IOException {

        String address = properties.getProperty("wlan.nat.ip");
        if (StringUtils.isBlank(address)) {
            throw new IllegalArgumentException("wlan.nat.ip is blank!");
        }
        String username = properties.getProperty("wlan.nat.username");
        if (StringUtils.isBlank(address)) {
            throw new IllegalArgumentException("wlan.nat.username is blank!");
        }
        String password = properties.getProperty("wlan.nat.password");
        if (StringUtils.isBlank(address)) {
            throw new IllegalArgumentException("wlan.nat.password is blank!");
        }

        final String loginUrl = "http://" + address + "/cgi-bin/luci";
        final String ipGetUrl = loginUrl + "/admin/settings/gwinfo?get=part";
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(loginUrl);
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("username", username));
            nameValuePairs.add(new BasicNameValuePair("psd", password));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                EntityUtils.consume(response.getEntity());
            }

            try (CloseableHttpResponse response = httpclient.execute(
                    new HttpGet(ipGetUrl))) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                response.getEntity().getContent()));
                IPTuple result = new Gson().fromJson(reader, IPTuple.class);
                EntityUtils.consume(response.getEntity());
                return result;
            }
        }
    }

    static class IPTuple implements Serializable {

        private String WANIP;

        private String WANIPv6;

        public String getWANIP() {
            return WANIP;
        }

        public void setWANIP(String WANIP) {
            this.WANIP = WANIP;
        }

        public String getWANIPv6() {
            return WANIPv6;
        }

        public void setWANIPv6(String WANIPv6) {
            this.WANIPv6 = WANIPv6;
        }
    }
}
