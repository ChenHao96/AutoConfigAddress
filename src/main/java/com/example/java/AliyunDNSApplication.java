package com.example.java;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.AddDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.DeleteDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class AliyunDNSApplication {

    private final Properties properties;

    public AliyunDNSApplication() throws IOException {

        this.properties = new Properties();
        InputStream inputStream = AliyunDNSApplication.class
                .getResourceAsStream("/application.properties");
        if (inputStream != null) {
            this.properties.load(inputStream);
            inputStream.close();
        }

        final String[] keys = new String[]{
                "aliyun.regionId", "aliyun.access.keyId", "aliyun.access.secret",
                "aliyun.domain.name", "aliyun.domain.name.sub"
        };
        for (String key : keys) {
            String value = System.getProperty(key);
            if (value == null || value.trim().length() == 0) {
                continue;
            }
            this.properties.put(key, value);
        }
    }

    public AliyunDNSApplication(Properties properties) {
        this.properties = new Properties();
        properties.forEach(properties::put);
    }

    private IAcsClient createIAcsClient() {
        String regionId = properties.getProperty("aliyun.regionId");
        if (StringUtils.isBlank(regionId)) {
            throw new IllegalArgumentException("aliyun.regionId is blank!");
        }
        String secret = properties.getProperty("aliyun.access.secret");
        if (StringUtils.isBlank(secret)) {
            throw new IllegalArgumentException("aliyun.access.secret is blank!");
        }
        String accessKeyId = properties.getProperty("aliyun.access.keyId");
        if (StringUtils.isBlank(accessKeyId)) {
            throw new IllegalArgumentException("aliyun.access.keyId is blank!");
        }
        return new DefaultAcsClient(DefaultProfile.getProfile(regionId, accessKeyId, secret));
    }

    public void updateDomainAddress(String ipv4, String ipv6) {

        String domainName = properties.getProperty("aliyun.domain.name");
        if (StringUtils.isBlank(domainName)) {
            throw new IllegalArgumentException("aliyun.domain.name is blank!");
        }
        String domainNameSub = properties.getProperty("aliyun.domain.name.sub");
        if (StringUtils.isBlank(domainNameSub)) {
            throw new IllegalArgumentException("aliyun.domain.name is blank!");
        }

        DescribeDomainRecordsRequest queryRequest = new DescribeDomainRecordsRequest();
        queryRequest.setDomainName(domainName);
        queryRequest.setKeyWord(domainNameSub);
        queryRequest.setSearchMode("LIKE");

        final String suffixIPv6 = "-v6";
        Set<String> rrSet = new HashSet<>(Arrays.asList(
                queryRequest.getKeyWord(), queryRequest.getKeyWord() + suffixIPv6));

        IAcsClient client = createIAcsClient();
        try {
            DescribeDomainRecordsResponse response = client.getAcsResponse(queryRequest);
            if (response.getDomainRecords() != null) {
                DeleteDomainRecordRequest request = new DeleteDomainRecordRequest();
                for (DescribeDomainRecordsResponse.Record domainRecord : response.getDomainRecords()) {
                    if (rrSet.contains(domainRecord.getRR())) {
                        request.setRecordId(domainRecord.getRecordId());
                        client.getAcsResponse(request);
                    }
                }
            }

            AddDomainRecordRequest request = new AddDomainRecordRequest();
            for (String rr : rrSet) {
                request.setDomainName(queryRequest.getDomainName());
                // 中国电信线路
                request.setLine("telecom");
                request.setRR(rr);
                if (rr.endsWith(suffixIPv6)) {
                    request.setValue(ipv6);
                    request.setType("AAAA");
                } else {
                    request.setType("A");
                    request.setValue(ipv4);
                }
                client.getAcsResponse(request);
            }
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            System.out.println("ErrCode: " + e.getErrCode());
            System.out.println("ErrMsg: " + e.getErrMsg());
            System.out.println("RequestId: " + e.getRequestId());
        }
    }
}
