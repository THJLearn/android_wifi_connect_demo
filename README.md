# 无屏联网
 1.扫描WIFI

    AT^HELLO

    AT^SCAN
    由APP发送，系统收到后回传WIFI列表
    
 2.回传WIFI列表
    
    AT^WIFI:123,456,789
    123,456,789 WiFi列表 用逗号分隔
 
 3.连接WiFi
 
    AT^CONNECT:123,456
    123:wifi名称
    456:wifi密码
    由APP发送，系统收到后获取指定WIFI并连接
 