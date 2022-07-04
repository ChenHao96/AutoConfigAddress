package com.example.java;

import java.io.IOException;

public class UpdateSecurityGroup {

    public static void main(String[] args) throws IOException {
        System.out.println("开始自动化配置...");
        TelecomGatewayApplication telecomGatewayApplication = new TelecomGatewayApplication();
        telecomGatewayApplication.loginGateway();
        System.out.println("\u001B[33m------\t正在获取您的网关IP");
        TelecomGatewayApplication.GatewayInfo gatewayInfo = telecomGatewayApplication.getGatewayInfo();
        System.out.printf("\u001B[0m\u001B[33m------\tWLAN:\t%s\n", gatewayInfo.getWANIP());
        System.out.println("\u001B[0m\u001B[32m------\t正在更新服务器安全组规则");
        AliyunSecurityGroupApplication aliyunSecurityGroupApplication = new AliyunSecurityGroupApplication();
        aliyunSecurityGroupApplication.updateSecurityGroup(gatewayInfo.getWANIP());
        System.out.println("\u001B[0m自动化配置完成.");
    }
}
