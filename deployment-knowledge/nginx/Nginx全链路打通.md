# 0724：技术探索 NGINX 全链路打通

### 前言

Nginx（发音"engine X"） 是一个高性能的 开源 Web 服务器 和 反向代理服务器，以 高并发、低内存占用 著称。

#### 核心特点：

##### 高性能

- 事件驱动架构，单机可轻松处理 10 万 + 并发连接（C10K 问题解决方案）
- 资源消耗极低（静态请求仅需 2.5MB 内存/连接）

##### 核心功能

- Web 服务器：直接托管 HTML/CSS/JS 等静态资源
- 反向代理：隐藏真实服务器，实现负载均衡（如转发到 Tomcat/Python 后端）
- 缓存加速：减少后端压力，提升响应速度

##### 关键优势

- 热部署：无需停机即可更新配置或升级版本
- 模块化：可通过插件扩展功能（如 Lua 脚本支持）

##### 典型应用场景：

- 静态资源托管（图片/视频）
- API 网关（路由/限流）
- 负载均衡（轮询/IP Hash）
- SSL/TLS 终结（HTTPS 加密卸载）

##### 极简示例：

```nginx
# 用3行实现一个反向代理
server {
    listen 80;
    location / { proxy_pass http://your_backend; }
}
```

💡 对比 Apache：Nginx 更适合高并发场景，而 Apache 更适合需要动态模块频繁变更的环境。

### Nginx 部署后，一个请求会经历什么？

#### 请求如何到达 Nginx？

1. 如果通过域名请求，浏览器通过 DNS 将域名解析为服务器的 IP
2. 通过 IP 发送请求，并自动携带 Host 头，标识当前发送的目标地址
3. 请求达到 Nginx 监听的端口，开始后续操作，其一定会经过 Nginx 操作，基本无法绕过，除非 Nginx 宕机
4. Nginx 通过 Host 头匹配对应的 server 块

   - 如果 Host 头是域名，则匹配 server_name
   - 如果匹配不到 server_name 或者说没有该域名，则匹配第一个 server 或者 default_server 块
   - 如果是 ip:端口形式，则直接匹配 listen 块
5. 匹配到 server 块后，会进行后续的请求

#### Nginx 会对请求做什么？

1. 匹配 location 块

   - 匹配 location 的规则？
     - = /path → 精确匹配（最高优先级）。
     - ^~ /path → 前缀匹配（命中后停止搜索）。
     - ~ /path → 正则匹配（区分大小写，按配置顺序）。
     - ~* /path → 正则匹配（不区分大小写，按配置顺序）。
     - /path → 普通前缀匹配（最后检查）。
   - 例如：/api/xx 请求
     - /api/xx → 精确匹配（最高优，直接命中）。
     - ^~ /api/xx → 前缀匹配（若存在，跳过正则）。
     - /api/xx → 普通前缀匹配（按最长规则优先）。
     - /api → 普通前缀匹配（较短路径，次优先级）。
     - 注意：/api/ 比 /api 优先级更高，因为前者的前缀匹配更加精确
2. 执行指令

   1. 返回内容
      - root / alias: 指定静态文件路径
        - root: 将 location 路径拼接到目录后，例如：/api（请求路径） + /data（root 属性） = /data/api（对应的目录），相当于动态请求
        - alias: 替换 location 路径作为目录，例如：/api （请求路径）-> /data（alias 属性）

          ```nginx
          location /img/ {
            root /data;         # 访问 /img/cat.jpg → 文件路径：/data/img/cat.jpg  _
          }
          location /vedio/ {
            alias /v/;     # 访问/vedio/dog.mp4 -> 文件路径: /v/dog.mp4
            # 注意：/v 这个就代表着文件，也就是说，会直接返回 v 这个文件，如果没有，就会 404
          }
          ```

		- return：直接返回状态码或重定向（如 return 301 /new-path）
			- return的作用是：立刻终止处理，返回状态码或者重定向
			- return 404: 直接返回404状态码
			- return 301 /new-path
				- 30x都可以跳转，但是各有区别，301最常用，为永久重定向(浏览器缓存跳转)
				- return 302（临时重定向）
				- return 307（临时保持方法）
				- return 308（永久保持方法)
			- 重定向是什么？
				- 重定向是服务器告知浏览器**自动跳转**到**新地址**（如301/302状态码）
				- 区别：301 -> 告诉浏览器永久跳转，302 -> 告诉浏览器临时跳转
				- 例如: 我浏览器地址栏输入shuimuxy.com，浏览器会补全http://前缀，并且默认端口为80端口（一般不显示就是80），Nginx监听到端口，并匹配该服务名后，会进行return 301 https://shuimuxy.com的永久重定向，此时浏览器接收到301/302状态码，此时浏览器地址栏会变为https://shuimuxy.com，并成功发送一个地址栏相同的请求。
				- 那么区别在于：假如手动改掉地址栏为http://shuimuxy.com，301状态码，会让浏览器缓存这个跳转结果，地址栏依旧会变为https://shuimuxy.com, 但是这个状态是在浏览器改变的，不会再经过nginx，而302则是不缓存这个结果，会一直打到nginx上，进行又一次重定向，虽然结果一样，但是中间经过的地方是不一样的
	2. 代理转发
		- proxy_pass：转发请求到后端服务（如 proxy_pass http://backend）。
			- proxy_pass本质上是代理转发，注意，不仅仅是转发
			- 代理的核心逻辑是：**拦截 + 加强**
				- 拦截原请求 -> 请求到达Nginx后，被Nginx拦截
				- 增强行为 -> 可以修改请求/响应(例如：负载均衡、缓存、添加头、安全校验)
				- 在Nginx中，代理本质是**请求转发+功能扩展**，而不仅限于简单透传
			- 那么我们可以想一下，这个过程是什么？
				- 请求到达Nginx，被拦截（原始请求）
				- Nginx进行代理、增强，然后转发请求到后端（代理请求）
				- 后端返回响应体，Nginx获取，继续进行改造、增强（代理请求）
				- 请求返回（原始请求）
				- 也就是说，请求在Nginx以及其目的地的过程中，被代理、加强
			- 因此：浏览器始终认为自己在和Nginx交互，而不是后端，proxt_pass隐藏了后端的真实地址，仅暴露给浏览器一个可以访问的接口
		- proxy_set_header：修改转发时的请求头（如 Host）。
			- proxy_set_header属于代理的前置操作，其用来新增、覆盖原始的请求头
			- 注意：浏览器对此无感知，也就是说，我们可以通过一些操作，修改浏览器随后的行为
				- 修改Host头，会发生什么？
        ```nginx
            set $custom_host "api.xxx.com"; # 自定义值
            location / {
                proxy_pass http://backend; # 这里表示转发到后端的地址
                proxy_set_header Host $custom_host;  # 修改Host头为变量$custom_host，后端拿到的Host就会是这个地址，这个也是浏览器会自带的一个请求头，是不需要后端跨域添加的自定义头
            }
        ```
          什么是 $custom_host?
          $custom_host是自定义变量，需要我们手动设置变量
          $host则是Nginx的内置变量，我们只需要通过$host引用，就可以获取到原始的请求头Host了

          - 修改Origin头，会发生什么？
            - Origin头作为跨域的重要参考，其会影响到浏览器、后端框架的行为，因此比较重要
            - Origin头本质上是当前请求的源站，也就是说，其标识当前的请求是从哪里发过来的
            - Nginx在代理前置操作中，修改请求头Origin，表示修改请求发来的源站，后端框架（如SpringBoot）如果添加了CORS，也会进行比对校验，如果说直接修改Origin头为后端框架允许的Origin源站
            - 代码示例：
            ```nginx

            location / {
              proxy_pass http://backend;
              proxy_set_header Origin $http_origin;  # 保持原始 Origin（推荐）
              # 或强制修改（慎用）：
              # proxy_set_header Origin "https://xxxxx.com";
            }
            ```
          
            ```java
            @Override
            _public void _addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
            _            // 允许对应的域名访问_
            _            _.allowedOrigins(
                                "https://xxxxx.com", "https://xxxxxx.com"
                        )
                        _// 允许的请求方法_
            _            _.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        _// 允许的请求头_
            _            _.allowedHeaders("Content-Type", "x-auth-token", "Authorization")
                        _// 允许携带 Cookie_
            _            _.allowCredentials(_true_);
            }
            ```

        - 为什么说，不建议强制修改Origin头？因为在这个示例中，我们可以明显看到，如果修改了Origin头为后端框架允许的源站，则会直接绕过安全框架的预校验行为，相当于直接失去了一道防火墙，很容易被恶意请求攻击。因此更建议保持Origin头
			- 除了这些，还可以做什么？
				- 封装IP，让后端更轻易获取到用户的IP地址
					- 封装IP，有两种形式：$remote_addr，$proxy_add_x_forwarded_for
					- 这两种形式都代表什么？
					- $remote_addr: 表示的是前一个请求的IP地址，比如：浏览器请求到Nginx，那么$remote_addr就是当前请求的IP地址，也就是用户的IP地址
					- 注意：对于这个变量，记录的只是前一个请求的IP，**如果说后端也配置了Nginx，同时也加上了这个$remote_addr，**则会添加/覆盖掉原X-Real-IP，此时后端拿到的就不会是用户真实的IP，而是前端Nginx所在的IP
					- $proxy_add_x_forwarded_for：表示的是当前请求经过的IP链，会追加$remote_addr，而且会自动去重
          ```nginx
          location / {
            proxy_set_header X-Real-IP $remote_addr;          # 直接客户端 IP
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;  # 代理链 IP 列表
          }    
          ```
          - 注意：添加的这些Header，并不是浏览器自带的，因此，后端必须在Cors配置中声明该自定义头，如果不声明，就会出现跨域问题。
	4. FastCGI：
		- fastcgi_pass：转发到 PHP/Python 等动态服务。
		- 因为我们业务暂时用不到PHP或者Python进行Nginx代理，暂不做介绍
	5. 重写路径：
		- rewrite：修改请求 URI（如 rewrite ^/old/(.*)$ /new/$1）。
			- 和proxy_pass的核心区别：
				- proxy_pass: 路径转发，相当于从Nginx转发到其他的服务（后端/静态文件)
				- rewrite：重新触发路径请求，修改URI（仅针对URI，不会修改前缀，也就是说，不会触发server块的重新匹配）后，会再次匹配location请求，**注意：可能会导致死循环，需要用last和break控制。**
	6. 控制缓存/头：
		- add_header：添加响应头（如 Cache-Control）。
			- add_header是代理的后置请求，在后端返回响应到Nginx后，Nginx会进行代理后置增强（添加响应头），再返回到客户端（浏览器）
			- add_header有什么比较好玩的玩法？
				- 添加Origin头
        ```nginx
        add_header 'Access-Control-Allow-Origin' "$http_origin"; # 保留了这个请求的源站
        add_header 'Access-Control-Allow-Origin' 'shuimuxy.com'; # 这个响应头只允许返回一个源站，浏览器会严格匹配，匹配成功才可以放行
        ```
      - 添加基础CORS头
      ```nginx
  
      add_header 'Access-Control-Allow-Methods' 'GET, POST, OPTIONS'; # 允许 OPTIONS，这是必须的，不然浏览器的预检操作就不可能成功
      add_header 'Access-Control-Allow-Headers' 'Content-Type, Authorization';
      add_header 'Access-Control-Allow-Credentials' 'true';  # 允许携带 Cookie
      
      ```
      - 复杂操作示例
        ```nginx
        location / {
            if ($http_origin ~* (shuimuxy\.com|www\.shuimuxy\.com)) {
                add_header 'Access-Control-Allow-Origin' "$http_origin";
                add_header 'Access-Control-Allow-Methods' 'GET, POST, OPTIONS';
                add_header 'Access-Control-Allow-Headers' 'Content-Type, Authorization';
                add_header 'Vary' 'Origin';
            }
        }
        ```

			- 以上复杂操作，是根据不同的源站，选择返回不同的域名，但是这与Nginx的最佳实践违背，因此我们更推荐在后端手动配置好，而不是在网关层进行判断等比较复杂的逻辑。
6. 访问控制：
	- allow / deny：限制 IP 访问。
		- allow: 允许ip访问
		- deny：拒绝ip访问
		- allow/deny all：允许/拒绝所有IP访问
		- 按照顺序声明生效，非最佳实践方案，不建议使用if判断

3. 返回响应
   1. 当 Nginx 处理完请求（匹配 location、执行代理/重定向/静态文件等操作）后，最终会向客户端（浏览器/API 调用方）返回响应。以下是关键行为：
   2. 构造响应头
      - Nginx 自动生成基础头（如 Server、Content-Type），并支持自定义：

      ```nginx

      add_header 'X-Security-Policy' "default-src 'self'";  # 安全头
      add_header 'Cache-Control' 'no-store';                # 禁用缓存
      
      ```
		- 常见操作：
			- 动态 CORS 头：根据 $http_origin 返回允许的域名（前文示例）。
			- 隐藏敏感信息：移除 Server 头（server_tokens off;）。
	3.  返回响应体
		- 静态文件：直接发送文件内容（如 index.html）。
		- 代理响应：后端服务（如 Java/Python）返回的数据经 Nginx 透传/修改后返回。
		- 直接响应：通过 return 返回状态码或内容（如 return 200 'OK';）。
	4. 日志与监控
		- 记录日志：access.log 保存状态码、IP、耗时等信息。
		- 错误处理：error_page 自定义错误页（如 error_page 500 /50x.html）。
	5. 性能优化
		- 压缩响应：启用 gzip 减少传输体积。
		- 缓存控制：对静态资源设置 Expires 或 ETag。

极简总结：

Nginx 的响应阶段是 组装头+发送数据+记录日志，核心目标是 **高效、安全、可观测**。

### 实战案例分析

#### 如何直接跳转到完整的HTTPS域名？

##### 业务场景

我们在浏览器地址栏通常只会直接输入对应的域名，并不会输入对应的协议(http:// | https://)，而浏览器通常会默认补充http://的前缀，那么我们如何让用户无感跳转到我们申请好的https://域名呢？

##### 实现思路

1. 我们之前有提到过重定向：return 30x，这一点可以帮助我们直接从http协议跳转到https协议

2. 同时不手动输入端口号，默认就是80

3. 我们访问网站，通常是需要保持https协议的跳转，因此我们需要使用301永久重定向，让浏览器缓存，这样既减轻Nginx负担，又提升响应效率，用户几乎无感知跳转

4. 跳转之后，我们还需要再监听对应的server_name，而https通常是在443端口，因此需要在这里进行监听，操作。这样，就可以配置好一个域名的全链路了

##### 配置示例

```nginx
server {
    listen 80;
    server_name shuimuxy.com www.shuimuxy.com;

    # 自动重定向到 HTTPS
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name shuimuxy.com www.shuimuxy.com;

    ssl_certificate     /home/ssl_cert/shuimuxy.com_certificate.pem; # 指定证书文件
    ssl_certificate_key /home/ssl_cert/shuimuxy.com_private.key; # 指定私钥文件

    ssl_protocols TLSv1.2 TLSv1.3; # 只允许 TLS 1.2/1.3（禁用旧的不安全协议）。
    ssl_ciphers HIGH:!aNULL:!MD5; # 指定加密套件（HIGH 表示高强度加密，排除不安全的 aNULL/MD5）。

    # 下面这几行可选，安全性更好
    ssl_session_cache shared:SSL:10m; # 缓存 SSL 会话，减少握手开销。
    ssl_session_timeout 10m; # 会话缓存有效期（10 分钟）。
    ssl_prefer_server_ciphers on; # 优先使用服务端的加密套件顺序。

    location / {
        add_header Cache-Control "no-store";
        proxy_pass http://frontend; # 这里是前端的地址，需要自己手动配置
        proxy_set_header Host $host; # 仅修改后端获取到的Host，浏览器始终按照原始Host添加
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; # 添加Ip链
        proxy_set_header X-Forwarded-Proto $scheme; # 获取用户当前的协议类型，比如在当前的servr块中，自动就是https类型
    }
}
```

#### 如何实现蓝绿发布/灰度发布

##### 介绍

蓝绿发布，就是部署两套相同的环境，一套作为生产环境（蓝环境），一套作为测试环境（绿环境）。项目迭代在绿环境迭代，然后二者可以随时切换，生产环境（当前稳定生产版本）又变为测试环境，测试环境（最新版本）变为生产环境。

灰度发布，其实是蓝绿发布的变种，在部署两套环境的基础上，取最新版本作为预发布试验版本，1% 的用户会跳转到新版本，而 99% 的用户则会跳转到原稳定发布的旧版本上。这样即使新版本会出现 bug，也会将影响最小化。（但我们面对灰度发布，仅仅调整负载均衡权重，是无法满足固定 1% 用户访问到新版本的，因此我们需要额外优化，这里不过多赘述）

##### 实现思路

1. Nginx 中的负载均衡可以完美匹配蓝绿发布，只需要进行适当的权重配对，即可完成蓝绿流量切换，用户几乎无感知
2. 蓝环境设置权重为 100，绿环境为 0，此时流量即可全部打在蓝环境上。
3. 切换流量，只需要在 Nginx 进行权重修改，然后再重新加载，即可达成蓝绿切换发布。

##### 配置参考

```nginx
upstream backend {
    server 127.0.0.1:25621; # 生产服务
    server 172.31.195.155:25621 down;  # 表示后端不接收流量，相当于测试使用服务器
}

upstream frontend {
    server 127.0.0.1:50701; # 生产服务 
    server 172.31.195.155:50701 down;  # 表示前端不接收流量，相当于测试使用服务器
}
# 因为当前Nginx版本不支持weight权重，因此如果可以，我更推荐使用weight=100, weight=0来配置蓝绿环境
```
