package com.example.java;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class TelecomGateway {

    public static void main(String[] args) throws IOException {

        List<String> localLanIPList = new ArrayList<>();
        Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
            if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
                if (inetAddressEnumeration.hasMoreElements()) {
                    InetAddress address = inetAddressEnumeration.nextElement();
                    localLanIPList.add(address.getHostAddress());
                }
            }
        }

        TelecomGatewayApplication telecomGatewayApplication = new TelecomGatewayApplication();
        System.out.println("------\t正在登录网关");
        telecomGatewayApplication.loginGateway();
        System.out.print("\u001B[33m------\t正在获取您的网关IP");
        System.out.println("\u001B[0m\u001B[31m(暂时仅支持天翼网关 >=3.0)\u001B[0m");

        System.out.println(new Gson().toJson(telecomGatewayApplication.getGatewayInfo()));
        TelecomGatewayApplication.PortMappingDisplay display = telecomGatewayApplication.getPortMappingDisplay();
        System.out.println(new Gson().toJson(display));

        localLanIPList.removeIf(s -> !TelecomGatewayApplication.checkIpInRange(s, String.format("%s/%s", display.getLanIp(), display.getMask())));
        if (localLanIPList.size() > 0) {
            TelecomGatewayApplication.PortMappingSet portMappingSet = new TelecomGatewayApplication.PortMappingSet();
            portMappingSet.setSrvname("test");
            portMappingSet.setOp(TelecomGatewayApplication.PortMappingSetOption.add);
            if (display.getPmRule() != null && display.getPmRule().size() > 0) {
                for (TelecomGatewayApplication.PortMappingRule rule : display.getPmRule()) {
                    if (rule.getDesp().equals(portMappingSet.getSrvname())) {
                        portMappingSet.setOp(TelecomGatewayApplication.PortMappingSetOption.del);
                        break;
                    }
                }
            }
            if (TelecomGatewayApplication.PortMappingSetOption.add.equals(portMappingSet.getOp())) {
                portMappingSet.setClient(localLanIPList.get(0));
                portMappingSet.setExPort(8080);
                portMappingSet.setInPort(8080);
            }
            System.out.println(new Gson().toJson(telecomGatewayApplication.portMappingSetSingle(portMappingSet)));
        }
    }
}
