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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelecomGatewayApplication {

    private final Properties properties;

    private final Set<String> serviceNames = new LinkedHashSet<>();

    private String gatewayUrl;

    private String loginToken;

    private String cidr;

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
            throw new IllegalArgumentException("Gateway Address Is Blank!");
        }
        String username = this.properties.getProperty("wlan.nat.username");
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Gateway Username Is Blank!");
        }
        String password = this.properties.getProperty("wlan.nat.password");
        if (StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("Gateway Password Is Blank!");
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

    public GatewayInfo getGatewayInfo() throws IOException {

        if (StringUtils.isBlank(this.gatewayUrl)) {
            throw new IllegalArgumentException("Gateway Must Be Login First!");
        }
        if (StringUtils.isBlank(this.loginToken)) {
            throw new IllegalArgumentException("Gateway Login Fail!");
        }

        final String url = this.gatewayUrl + "/admin/settings/gwinfo?get=part";
        try (CloseableHttpResponse response = httpclient.execute(new HttpGet(url))) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));
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

    public PortMappingDisplay getPortMappingDisplay() throws IOException {

        if (StringUtils.isBlank(this.gatewayUrl)) {
            throw new IllegalArgumentException("Gateway Must Be Login First!");
        }
        if (StringUtils.isBlank(this.loginToken)) {
            throw new IllegalArgumentException("Gateway Login Fail!");
        }

        final String url = this.gatewayUrl + "/admin/settings/pmDisplay";
        try (CloseableHttpResponse response = httpclient.execute(new HttpGet(url))) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));
            JsonElement jsonElement = JsonParser.parseReader(reader);
            EntityUtils.consume(response.getEntity());
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            PortMappingDisplay result = new PortMappingDisplay();
            result.setLanIp(jsonObject.getAsJsonPrimitive("lanIp").getAsString());
            result.setMask(jsonObject.getAsJsonPrimitive("mask").getAsString());
            result.setCount(jsonObject.getAsJsonPrimitive("count").getAsInt());
            this.cidr = String.format("%s/%s", result.getLanIp(), result.getMask());
            if (result.getCount() > 0) {
                if (result.getPmRule() == null) {
                    Gson gson = new Gson();
                    result.setPmRule(new ArrayList<>(result.getCount()));
                    for (int i = 0; i < result.getCount(); i++) {
                        PortMappingRule rule = gson.fromJson(jsonObject.get(
                                String.format("pmRule%d", i + 1)), PortMappingRule.class);
                        result.getPmRule().add(rule);
                        this.serviceNames.add(rule.getDesp());
                    }
                }
            } else {
                result.setPmRule(Collections.emptyList());
            }
            return result;
        }
    }

    public PortMappingSetResponse portMappingSetSingle(PortMappingSet portMappingSet) throws IOException {

        if (StringUtils.isBlank(this.gatewayUrl)) {
            throw new IllegalArgumentException("Gateway Must Be Login First!");
        }
        if (StringUtils.isBlank(this.loginToken)) {
            throw new IllegalArgumentException("Gateway Login Fail!");
        }
        if (StringUtils.isBlank(this.cidr)) {
            throw new IllegalArgumentException("Must Be Request PortMappingDisplay First!");
        }
        if (StringUtils.isBlank(portMappingSet.getSrvname())) {
            throw new IllegalArgumentException("Virtual Service Name Cannot Be Empty!");
        }
        if (portMappingSet.getOp() == null) {
            throw new IllegalArgumentException("PortMapping Option Is Null!");
        }

        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("token", this.loginToken));
        nameValuePairs.add(new BasicNameValuePair("op", portMappingSet.getOp().name()));
        nameValuePairs.add(new BasicNameValuePair("srvname", portMappingSet.getSrvname()));
        if (PortMappingSetOption.add.equals(portMappingSet.getOp())) {

            if (this.serviceNames.contains(portMappingSet.getSrvname())) {
                throw new IllegalArgumentException("Service Name Already Exists!");
            }
            if (StringUtils.isBlank(portMappingSet.getClient())) {
                throw new IllegalArgumentException("Client Param Is Blank!");
            } else if (!portMappingSet.getClient().matches(ipRegex)) {
                throw new IllegalArgumentException("Invalid LAN IP!");
            } else if (!checkIpInRange(portMappingSet.getClient(), this.cidr)) {
                throw new IllegalArgumentException("The LAN IP is not the same network segment as the current LAN!");
            }
            if (portMappingSet.getExPort() < 0 || portMappingSet.getExPort() > 65535) {
                throw new IllegalArgumentException("Invalid External Port Value!");
            }
            if (portMappingSet.getInPort() < 0 || portMappingSet.getInPort() > 65535) {
                throw new IllegalArgumentException("Invalid Internal Port Value!");
            }
            if (portMappingSet.getProtocol() == null) {
                throw new IllegalArgumentException("PortMapping Protocol Is Null!");
            }

            nameValuePairs.add(new BasicNameValuePair("client", portMappingSet.getClient()));
            nameValuePairs.add(new BasicNameValuePair("exPort", portMappingSet.getExPort().toString()));
            nameValuePairs.add(new BasicNameValuePair("inPort", portMappingSet.getInPort().toString()));
            nameValuePairs.add(new BasicNameValuePair("protocol", portMappingSet.getProtocol().name()));
        }

        HttpPost httpPost = new HttpPost(this.gatewayUrl + "/admin/settings/pmSetSingle");
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        try (CloseableHttpResponse httpResponse = httpclient.execute(httpPost)) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(httpResponse.getEntity().getContent()));
            PortMappingSetResponse response = new Gson().fromJson(reader, PortMappingSetResponse.class);
            EntityUtils.consume(httpResponse.getEntity());
            response.setSrvname(portMappingSet.getSrvname());
            response.setOp(portMappingSet.getOp());
            response.setProtocol(null);
            if (PortMappingSetOption.del.equals(response.getOp())) {
                this.serviceNames.remove(response.getSrvname());
            } else if (PortMappingSetOption.add.equals(response.getOp())) {
                response.setOp(portMappingSet.getOp());
                response.setClient(portMappingSet.getClient());
                response.setInPort(portMappingSet.getInPort());
                response.setExPort(portMappingSet.getExPort());
                response.setSrvname(portMappingSet.getSrvname());
                response.setProtocol(portMappingSet.getProtocol());
            }
            return response;
        }
    }

    private static final String ipRegex = "((1\\d{2}|2([0-4]\\d|5[0-5])|[1-9]\\d|\\d)\\.){3}(1\\d{2}|2([0-4]\\d|5[0-5])|[1-9]\\d|\\d)";

    private static int parseToInt(String ip) {
        String[] ips = ip.split("\\.");
        return (Integer.parseInt(ips[0]) << 24) | (Integer.parseInt(ips[1]) << 16)
                | (Integer.parseInt(ips[2]) << 8) | (Integer.parseInt(ips[3]));
    }

    public static boolean checkIpInRange(String ip, String cidr) {

        String[] lanMask = cidr.split("/");
        String lan = lanMask[0], maskStr = lanMask[1];

        int mask;
        if (maskStr.matches(ipRegex)) {
            mask = parseToInt(maskStr);
        } else {
            mask = 0xFFFFFFFF << (32 - Integer.parseInt(maskStr));
        }

        int ipAddr = parseToInt(ip);
        int netAddr = parseToInt(lan);
        return (ipAddr & mask) == (netAddr & mask);
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

    static class PortMappingDisplay implements Serializable {

        private String lanIp;

        private String mask;

        private int count;

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

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    enum PortMappingProtocol {
        TCP, UDP, BOTH
    }

    static abstract class PortMapping implements Serializable {

        private PortMappingProtocol protocol;

        private String client;

        private Integer inPort;

        private Integer exPort;

        public PortMappingProtocol getProtocol() {
            return protocol;
        }

        public void setProtocol(PortMappingProtocol protocol) {
            this.protocol = protocol;
        }

        public String getClient() {
            return client;
        }

        public void setClient(String client) {
            this.client = client;
        }

        public Integer getInPort() {
            return inPort;
        }

        public void setInPort(Integer inPort) {
            this.inPort = inPort;
        }

        public Integer getExPort() {
            return exPort;
        }

        public void setExPort(Integer exPort) {
            this.exPort = exPort;
        }
    }

    enum PortMappingSetOption {
        add, del, enable, disable;
    }

    static class PortMappingSet extends PortMapping {

        private PortMappingSetOption op;

        private String srvname;

        public PortMappingSet() {
            super.setProtocol(PortMappingProtocol.TCP);
            this.op = PortMappingSetOption.add;
        }

        public PortMappingSetOption getOp() {
            return op;
        }

        public void setOp(PortMappingSetOption op) {
            this.op = op;
        }

        public String getSrvname() {
            return srvname;
        }

        public void setSrvname(String srvname) {
            this.srvname = srvname;
        }
    }

    static class PortMappingSetResponse extends PortMappingSet {

        private int retVal;

        public int getRetVal() {
            return retVal;
        }

        public void setRetVal(int retVal) {
            this.retVal = retVal;
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
}
