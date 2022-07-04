# AutoConfigAddress
这是一个自动化配置本地网关IP到DNS的程序

## 本地网关信息
获取网关IP信息,暂时仅支持电信天翼网关(>=3.0)
|参数名|描述|
|-|:------:|
|wlan.nat.ip|本地网关IP(192.168.1.1)|
|wlan.nat.username|本地网关登录用户名|
|wlan.nat.password|本地网关登录密码|

登录到天翼网关并获取当前网络信息
``` java
public static void main(String[] args) throws IOException {
    TelecomGatewayApplication telecomGatewayApplication = new TelecomGatewayApplication();
    telecomGatewayApplication.loginGateway();
    System.out.println(new Gson().toJson(telecomGatewayApplication.getGatewayInfo()));
}
```

登录到天翼网关并获取端口转发列表
``` java
public static void main(String[] args) throws IOException {
    TelecomGatewayApplication telecomGatewayApplication = new TelecomGatewayApplication();
    telecomGatewayApplication.loginGateway();
    System.out.println(new Gson().toJson(telecomGatewayApplication.getPortMappingDisplay()));
}
```

登录到天翼网关并获取端口转发列表
``` java
public static void main(String[] args) throws IOException {
    TelecomGatewayApplication telecomGatewayApplication = new TelecomGatewayApplication();
    telecomGatewayApplication.loginGateway();
    System.out.println(new Gson().toJson(telecomGatewayApplication.getPortMappingDisplay()));

    TelecomGatewayApplication.PortMappingSet portMappingSet = new TelecomGatewayApplication.PortMappingSet();
    portMappingSet.setSrvname("test");
    // 添加端口转发
    portMappingSet.setOp(TelecomGatewayApplication.PortMappingSetOption.add);
    // 删除端口转发
    // portMappingSet.setOp(TelecomGatewayApplication.PortMappingSetOption.del);
    // 删除端口开启
    // portMappingSet.setOp(TelecomGatewayApplication.PortMappingSetOption.enable);
    // 删除端口关闭
    // portMappingSet.setOp(TelecomGatewayApplication.PortMappingSetOption.disable);
    // 只有添加时需要以下信息
    portMappingSet.setClient("被转发的IP地址(192.168.0.100)");
    portMappingSet.setExPort(8080);
    portMappingSet.setInPort(8080);
    System.out.println(new Gson().toJson(telecomGatewayApplication.portMappingSetSingle(portMappingSet)));
}
```

## Aliyun SDK 参数
|参数名|描述|
|-|:------:|
|aliyun.regionId|阿里云地区ID|
|aliyun.access.keyId|阿里云 SDK Key|
|aliyun.access.secret|阿里云 SDK Key对应的秘钥|

## Aliyun DNS解析域名
|参数名|描述|
|-|:------:|
|aliyun.domain.name|域名地址(aliyun.com)|
|aliyun.domain.name.sub|二级域名(www)|

将本地的网络IP添加(修改)至域名
``` java
public static void main(String[] args) throws IOException {
    TelecomGatewayApplication telecomGatewayApplication = new TelecomGatewayApplication();
    telecomGatewayApplication.loginGateway();
    TelecomGatewayApplication.GatewayInfo gatewayInfo = telecomGatewayApplication.getGatewayInfo();
    AliyunDNSApplication aliyunDNSApplication = new AliyunDNSApplication();
    aliyunDNSApplication.updateDomainAddress(gatewayInfo.getWANIP(), gatewayInfo.getWANIPv6());
}
```

## Aliyun 安全组入站规则
|参数名|描述|
|-|:------:|
|aliyun.securityGroup.id|阿里云安全组ID|
|aliyun.securityGroup.protocol|入站规则协议(TCP \| UDP \| ALL)|
|aliyun.securityGroup.priority|入站规则权重(1-50)|
|aliyun.securityGroup.portRange|入站规则端口范围(1-65535 \| 8080/8090)|

如果您有服务器在阿里云并设置了端口(固定IP)访问规则可以使用以下代码更新您安全组的入站端口IP
``` java
public static void main(String[] args) throws IOException {
    TelecomGatewayApplication telecomGatewayApplication = new TelecomGatewayApplication();
    telecomGatewayApplication.loginGateway();
    TelecomGatewayApplication.GatewayInfo gatewayInfo = telecomGatewayApplication.getGatewayInfo();
    AliyunSecurityGroupApplication aliyunSecurityGroupApplication = new AliyunSecurityGroupApplication();
    aliyunSecurityGroupApplication.updateSecurityGroup(gatewayInfo.getWANIP());
}
```
