# AutoConfigAddress
这是一个自动化配置本地网关IP的程序

## Aliyun SDK 参数
|参数名|描述|
|-|:------:|
|aliyun.regionId|阿里云地区ID|
|aliyun.access.keyId|阿里云 SDK Key|
|aliyun.access.secret|阿里云 SDK Key对应的秘钥|

## Aliyun 安全组入站规则
|参数名|描述|
|-|:------:|
|aliyun.securityGroup.id|阿里云安全组ID|
|aliyun.securityGroup.protocol|入站规则协议(TCP\| UDP\| ALL)|
|aliyun.securityGroup.priority|入站规则权重(1-50)|
|aliyun.securityGroup.portRange|入站规则端口范围(1-65535 \| 8080:8090)|

## Aliyun DNS解析域名
|参数名|描述|
|-|:------:|
|aliyun.domain.name|域名地址(aliyun.com)|
|aliyun.domain.name.sub|二级域名(www)|

## 本地网关信息
### 获取网关IP信息暂时仅支持电信宽带网关
|参数名|描述|
|-|:------:|
|wlan.nat.ip|本地网关IP(192.168.1.1)|
|wlan.nat.username|本地网关登录用户名|
|wlan.nat.password|本地网关登录密码|
