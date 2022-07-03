package com.example.java;

import java.io.IOException;

public class UpdateSecurityGroup {

    public static void main(String[] args) throws IOException {
        System.out.println("开始自动化配置...");
        TelecomIPApplication telecomIPApplication = new TelecomIPApplication();
        System.out.println("\u001B[33m------\t正在获取您的网关IP");
        TelecomIPApplication.IPTuple ipTuple = telecomIPApplication.loadWLanNATInfo();
        System.out.printf("\u001B[0m\u001B[33m------\tWLAN:\t%s\n", ipTuple.getWANIP());
        System.out.printf("------\tWLANv6:\t%s\n", ipTuple.getWANIPv6());
        System.out.println("\u001B[0m\u001B[32m------\t正在更新服务器安全组规则");
        AliyunSDKApplication aliyunSDKApplication = new AliyunSDKApplication();
        aliyunSDKApplication.updateSecurityGroup(ipTuple.getWANIP());
        System.out.println("\u001B[0m自动化配置完成.");
    }
}
