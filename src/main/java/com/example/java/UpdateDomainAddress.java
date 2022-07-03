package com.example.java;

import java.io.IOException;

public class UpdateDomainAddress {

    public static void main(String[] args) throws IOException {
        System.out.println("开始自动化配置...");
        TelecomIPApplication telecomIPApplication = new TelecomIPApplication();
        System.out.println("\u001B[33m------\t正在获取您的网关IP");
        TelecomIPApplication.IPTuple ipTuple = telecomIPApplication.loadWLanNATInfo();
        System.out.printf("\u001B[0m\u001B[33m------\tWLAN:\t%s\n", ipTuple.getWANIP());
        System.out.printf("------\tWLANv6:\t%s\n", ipTuple.getWANIPv6());
        System.out.println("\u001B[0m\u001B[30m------\t正在更新域名地址");
        AliyunSDKApplication aliyunSDKApplication = new AliyunSDKApplication();
        aliyunSDKApplication.updateDomainAddress(ipTuple.getWANIP(), ipTuple.getWANIPv6());
        System.out.println("\u001B[0m自动化配置完成.");
    }
}
