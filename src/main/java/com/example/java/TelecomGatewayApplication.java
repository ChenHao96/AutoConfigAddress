package com.example.java;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelecomGatewayApplication {

    public static void main(String[] args) throws IOException {
        TelecomGatewayApplication telecomGatewayApplication = new TelecomGatewayApplication();
        System.out.println("------\t正在登录网关");
        telecomGatewayApplication.loginGateway();
        System.out.print("\u001B[33m------\t正在获取您的网关IP");
        System.out.println("\u001B[0m\u001B[31m(暂时仅支持电信)\u001B[0m");
        System.out.println(new Gson().toJson(telecomGatewayApplication.getGatewayInfo()));
        System.out.println(new Gson().toJson(telecomGatewayApplication.getPortMappingDisplay()));
    }

    private final Properties properties;

    private String gatewayUrl;

    private String loginToken;

    private CloseableHttpClient httpclient;

    public TelecomGatewayApplication() throws IOException {

        this.properties = new Properties();
        InputStream inputStream = AliyunDNSApplication.class
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

    public TelecomGatewayApplication(Properties properties) {
        this.properties = new Properties();
        properties.forEach(properties::put);
    }

    public TelecomGatewayApplication(String gateway, String username, String password) {
        this.properties = new Properties();
        this.properties.put("wlan.nat.ip", gateway);
        this.properties.put("wlan.nat.username", username);
        this.properties.put("wlan.nat.password", password);
    }

    public void loginGateway() throws IOException {

        String address = this.properties.getProperty("wlan.nat.ip");
        if (StringUtils.isBlank(address)) {
            throw new IllegalArgumentException("wlan.nat.ip is blank!");
        }
        String username = this.properties.getProperty("wlan.nat.username");
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("wlan.nat.username is blank!");
        }
        String password = this.properties.getProperty("wlan.nat.password");
        if (StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("wlan.nat.password is blank!");
        }

        this.gatewayUrl = "http://" + address + "/cgi-bin/luci";
        this.httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(this.gatewayUrl);
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("username", username));
        nameValuePairs.add(new BasicNameValuePair("psd", password));
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        Pattern findToken = Pattern.compile("\\{ *token *: *'\\w+' *}");
        try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            response.getEntity().getContent()));
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = findToken.matcher(line);
                if (matcher.find()) {
                    LoginToken loginToken = new Gson().fromJson(matcher.group(), LoginToken.class);
                    this.loginToken = loginToken.getToken();
                    break;
                }
            }
            EntityUtils.consume(response.getEntity());
        }
    }

    public PortMappingDisplay getPortMappingDisplay() throws IOException {

        if (StringUtils.isBlank(this.gatewayUrl)) {
            throw new IllegalArgumentException("gateway must be login!");
        }
        if (StringUtils.isBlank(this.loginToken)) {
            throw new IllegalArgumentException("gateway must be login!");
        }

        final String url = this.gatewayUrl + "/admin/settings/pmDisplay";
        try (CloseableHttpResponse response = httpclient.execute(new HttpGet(url))) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            response.getEntity().getContent()));

            JsonElement jsonElement = JsonParser.parseReader(reader);
            EntityUtils.consume(response.getEntity());
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String lanIp = jsonObject.getAsJsonPrimitive("lanIp").getAsString();
            String mask = jsonObject.getAsJsonPrimitive("mask").getAsString();
            int count = jsonObject.getAsJsonPrimitive("count").getAsInt();
            PortMappingDisplay result = new PortMappingDisplay();
            result.setLanIp(lanIp);
            result.setMask(mask);
            if (count > 0 && result.getPmRule() == null) {
                Gson gson = new Gson();
                result.setPmRule(new ArrayList<>());
                for (int i = 0; i < count; i++) {
                    result.getPmRule().add(gson.fromJson(jsonObject.get(
                            "pmRule" + (i + 1)), PortMappingRule.class));
                }
            }
            return result;
        }
    }

    public GatewayInfo getGatewayInfo() throws IOException {

        if (StringUtils.isBlank(this.gatewayUrl)) {
            throw new IllegalArgumentException("gateway must be login!");
        }

        final String url = this.gatewayUrl + "/admin/settings/gwinfo?get=part";
        try (CloseableHttpResponse response = httpclient.execute(new HttpGet(url))) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            response.getEntity().getContent()));
            GatewayInfo result = new Gson().fromJson(reader, GatewayInfo.class);
            EntityUtils.consume(response.getEntity());
            int index = result.getWANIPv6().indexOf("/");
            if (index > 0) {
                result.setWANIPv6(result.getWANIPv6().substring(0, index));
            }
            if (!result.getMAC().contains(":")) {
                StringBuilder macStr = new StringBuilder(result.getMAC());
                int[] indexArray = new int[]{2, 5, 8, 11, 14};
                for (int i : indexArray) {
                    macStr.insert(i, ":");
                }
                result.setMAC(macStr.toString());
            }
            return result;
        }
    }

    static class LoginToken implements Serializable {

        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    static class PortMappingDisplay implements Serializable {

        private String lanIp;

        private String mask;

        private List<PortMappingRule> pmRule;

        public String getLanIp() {
            return lanIp;
        }

        public void setLanIp(String lanIp) {
            this.lanIp = lanIp;
        }

        public String getMask() {
            return mask;
        }

        public void setMask(String mask) {
            this.mask = mask;
        }

        public List<PortMappingRule> getPmRule() {
            return pmRule;
        }

        public void setPmRule(List<PortMappingRule> pmRule) {
            this.pmRule = pmRule;
        }
    }

    static abstract class PortMapping implements Serializable {

        private String protocol;

        private String client;

        private int inPort;

        private int exPort;

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getClient() {
            return client;
        }

        public void setClient(String client) {
            this.client = client;
        }

        public int getInPort() {
            return inPort;
        }

        public void setInPort(int inPort) {
            this.inPort = inPort;
        }

        public int getExPort() {
            return exPort;
        }

        public void setExPort(int exPort) {
            this.exPort = exPort;
        }
    }

    static class PortMappingSet extends PortMapping {

        private String op;

        private String srvname;

        public String getOp() {
            return op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        public String getSrvname() {
            return srvname;
        }

        public void setSrvname(String srvname) {
            this.srvname = srvname;
        }
    }

    static class PortMappingRule extends PortMapping {

        private String desp;

        private int enable;

        public String getDesp() {
            return desp;
        }

        public void setDesp(String desp) {
            this.desp = desp;
        }

        public int getEnable() {
            return enable;
        }

        public void setEnable(int enable) {
            this.enable = enable;
        }
    }

    static class GatewayInfo implements Serializable {

        private String DevType;

        private String LANIP;

        private String LANIPv6;

        private String MAC;

        private String ProductCls;

        private String ProductSN;

        private String SWVer;

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

        public String getDevType() {
            return DevType;
        }

        public void setDevType(String devType) {
            DevType = devType;
        }

        public String getLANIP() {
            return LANIP;
        }

        public void setLANIP(String LANIP) {
            this.LANIP = LANIP;
        }

        public String getLANIPv6() {
            return LANIPv6;
        }

        public void setLANIPv6(String LANIPv6) {
            this.LANIPv6 = LANIPv6;
        }

        public String getMAC() {
            return MAC;
        }

        public void setMAC(String MAC) {
            this.MAC = MAC;
        }

        public String getProductCls() {
            return ProductCls;
        }

        public void setProductCls(String productCls) {
            ProductCls = productCls;
        }

        public String getProductSN() {
            return ProductSN;
        }

        public void setProductSN(String productSN) {
            ProductSN = productSN;
        }

        public String getSWVer() {
            return SWVer;
        }

        public void setSWVer(String SWVer) {
            this.SWVer = SWVer;
        }
    }
}
