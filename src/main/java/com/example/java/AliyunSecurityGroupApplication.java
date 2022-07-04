package com.example.java;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.AuthorizeSecurityGroupRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeSecurityGroupAttributeRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeSecurityGroupAttributeResponse;
import com.aliyuncs.ecs.model.v20140526.RevokeSecurityGroupRequest;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class AliyunSecurityGroupApplication {

    private final Properties properties;

    public AliyunSecurityGroupApplication() throws IOException {

        this.properties = new Properties();
        InputStream inputStream = AliyunSecurityGroupApplication.class
                .getResourceAsStream("/application.properties");
        if (inputStream != null) {
            this.properties.load(inputStream);
            inputStream.close();
        }

        final String[] keys = new String[]{
                "aliyun.regionId", "aliyun.access.keyId", "aliyun.access.secret",
                "aliyun.securityGroup.protocol", "aliyun.securityGroup.priority",
                "aliyun.securityGroup.portRange", "aliyun.securityGroup.id"
        };
        for (String key : keys) {
            String value = System.getProperty(key);
            if (value == null || value.trim().length() == 0) {
                continue;
            }
            this.properties.put(key, value);
        }
    }

    public AliyunSecurityGroupApplication(Properties properties) {
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

    public void updateSecurityGroup(String ipv4) {

        String securityGroupId = properties.getProperty("aliyun.securityGroup.id");
        if (StringUtils.isBlank(securityGroupId)) {
            throw new IllegalArgumentException("aliyun.securityGroup.id is blank!");
        }
        String securityGroupPriority = properties.getProperty("aliyun.securityGroup.priority");
        if (StringUtils.isBlank(securityGroupPriority)) {
            throw new IllegalArgumentException("aliyun.securityGroup.priority is blank!");
        }
        String securityGroupProtocol = properties.getProperty("aliyun.securityGroup.protocol");
        if (StringUtils.isBlank(securityGroupProtocol)) {
            throw new IllegalArgumentException("aliyun.securityGroup.protocol is blank!");
        }
        String securityGroupPortRange = properties.getProperty("aliyun.securityGroup.portRange");
        if (StringUtils.isBlank(securityGroupPortRange)) {
            throw new IllegalArgumentException("aliyun.securityGroup.portRange is blank!");
        }

        final String policy = "Accept", description = "SDK程序添加";
        DescribeSecurityGroupAttributeRequest request = new DescribeSecurityGroupAttributeRequest();
        request.setSecurityGroupId(securityGroupId);
        request.setDirection("ingress");
        request.setNicType("intranet");

        IAcsClient client = createIAcsClient();
        try {
            DescribeSecurityGroupAttributeResponse response = client.getAcsResponse(request);
            List<DescribeSecurityGroupAttributeResponse.Permission> permissionsList = response.getPermissions();
            if (permissionsList != null) {
                for (DescribeSecurityGroupAttributeResponse.Permission permission : permissionsList) {
                    boolean result = policy.equals(permission.getPolicy());
                    result = result && description.equals(permission.getDescription());
                    result = result && securityGroupPriority.equals(permission.getPriority());
                    result = result && securityGroupProtocol.equals(permission.getIpProtocol());
                    result = result && securityGroupPortRange.equals(permission.getPortRange());
                    if (result) {
                        RevokeSecurityGroupRequest revokeRequest = new RevokeSecurityGroupRequest();
                        revokeRequest.setSourceCidrIp(permission.getSourceCidrIp());
                        revokeRequest.setSecurityGroupId(securityGroupId);
                        revokeRequest.setIpProtocol(securityGroupProtocol);
                        revokeRequest.setPortRange(securityGroupPortRange);
                        revokeRequest.setPriority(securityGroupPriority);
                        client.getAcsResponse(revokeRequest);
                    }
                }
            }

            AuthorizeSecurityGroupRequest authorizeRequest = new AuthorizeSecurityGroupRequest();
            authorizeRequest.setSecurityGroupId(securityGroupId);
            authorizeRequest.setIpProtocol(securityGroupProtocol);
            authorizeRequest.setPortRange(securityGroupPortRange);
            authorizeRequest.setPriority(securityGroupPriority);
            authorizeRequest.setDescription(description);
            authorizeRequest.setSourceCidrIp(ipv4);
            authorizeRequest.setPolicy(policy);
            client.getAcsResponse(authorizeRequest);
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            System.out.println("ErrCode: " + e.getErrCode());
            System.out.println("ErrMsg: " + e.getErrMsg());
            System.out.println("RequestId: " + e.getRequestId());
        }
    }
}
