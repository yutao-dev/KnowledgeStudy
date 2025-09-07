# `0716——技术沉淀：C` ORS 跨域请求问题详解

### 什么是跨域

跨域（CORSs-Origin）是浏览器为了防止正常网页被恶意网站随意访问、请求，而搞出来的安全策略。本意上是为了隔绝网站与网站之间不安全的请求。组织当前网页向不同源（协议、域名、端口任一不同）的服务器发送请求。

### 发出请求时候，浏览器会做什么？

#### 检查请求是否符合同源策略

- 发出一个跨域请求（fetch、XMLHttpRequest）
- 浏览器会先判断请求的协议（HTTP/HTTPS）、域名（host 或者 xxx.com）、端口是否与当前页面一致
- 如果符合同源——> 直接发送请求，不需要后续校验
- 如果是跨域，就会进入跨域安全策略流程

#### 检查是否为简单请求

- 当不符合跨域请求时候，浏览器会进一步检查，看其是否符合简单请求

#### 简单请求

- 简单请求满足条件：同时满足
  - 方法为 GET、POST 或者 HEAD
  - 请求头仅包含： Accept、Accept-Language、Content-Language、Content-Type（仅限 text/plain、multipart/form-data、application/x-www-form-urlencoded）

###### 为什么要这么设计？

- 一是 GET 与 POST 请求限制了操作类型，同时请求头限制只能安全读取。
- 这类请求是类似于“只读操作”，其本身不会对服务器造成严重危害。因此浏览器对这种请求放宽操作，但是依旧走跨域流程
- 浏览器对于这种简单请求，会自动添加 Origin 头，标明当前页的域名，例如：

```http
Origin: https://your-site.com
```

- 同时服务器必须响应：返回 Access-Control-Allow-Origin，明确允许该来源（或通配符 *）

```http
Access-Control-Allow-Origin: https://your-site.com  // 或 *
```

- 如果服务器未授权，浏览器会隐藏响应结果，并因为同源问题，该请求失败

###### 这些请求头都是什么含义？

- Accept: 客户端可接受的响应内容类型（如 JSON、HTML）。

```http
Accept: text/html, application/json;q=0.9, */*;q=0.8
```

- 优先返回 text/html，其次是 application/json，最后是任意类型（*/*）。q 表示优先级，默认 q 为 1
- Accept-Language：客户端希望接收的自然语言（如 en 英语，zh-CN 简体中文）

```http
Accept-Language: en-US, en;q=0.9, zh-CN;q=0.8, zh;q=0.7
```

- 优先返回美式英语（en-US），其次是通用英语（en），然后是简体中文（zh-CN）。
- Content-Language: 服务器/客户端声明当前内容的语言（通常用于响应头）

```http
Content-Language: zh-CN
```

- 表示返回的内容是简体中文。
- Content-Type: 声明请求/响应体的数据格式：

```http
Content-Type: text/plain; charset=utf-8 # 表示纯文本
Hello, world! # 请求示例

Content-Type: application/x-www-form-urlencoded # 表单键值对
username=admin&password=123456 # 请求体示例

Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryABC123 #文件上传
# 请求体示例
------WebKitFormBoundaryABC123
Content-Disposition: form-data; name="username"

admin
------WebKitFormBoundaryABC123
Content-Disposition: form-data; name="avatar"; filename="photo.jpg"
Content-Type: image/jpeg

<二进制文件数据>
------WebKitFormBoundaryABC123--
```

- 完整 HTTP 请求示例

```http
POST /login HTTP/1.1
Host: example.com
Accept: application/json
Accept-Language: en-US, en;q=0.9
Content-Type: application/x-www-form-urlencoded
Content-Language: en-US

username=admin&password=123456
```

```http
POST /login HTTP/1.1  # 使用 POST 方法请求 /login 接口，HTTP 版本 1.1
Host: example.com     # 目标服务器域名

# 客户端希望服务器返回 JSON 格式的响应（优先级最高）
Accept: application/json  

# 客户端优先接收美式英语（en-US），其次是通用英语（en），q 值表示权重（0.9）
Accept-Language: en-US, en;q=0.9  

# 请求体的数据格式为 URL 编码的表单数据（键值对）
Content-Type: application/x-www-form-urlencoded  

# 请求体的内容语言为美式英语（通常用于描述数据内容的语言）
Content-Language: en-US  

# 空行分隔请求头和请求体

# 请求体：URL 编码的表单数据（username=admin，password=123456）
username=admin&password=123456
```

###### 总结

浏览器本质上只是对简单请求进行了放宽，不再进行预检，只需要服务器提供 Access-Control-Allow-Origin 表明当前网页被允许即可。

#### 非简单请求

- 非简单请求（此时需要预检请求）： 满足以下任意一条
  - 方法为 PUT、DELETE、CONNECT 等
  - 自定义请求头（如 Authorization、X-Custom-Header）
  - Content-Type 为 application/json

###### 为什么要这么设计？

这类方法通常不是“只读操作”，而是对服务器数据有影响的操作，并且如果服务器对特定的请求头进行特定操作也可能会出现危险操作。以及 json 格式的请求体也允许进行更复杂的操作，因此对于这类请求需要更加精细的控制

###### 浏览器行为

- 先发送 OPTIONS 预检请求，携带：
- Origin（来源）
- Access-Control-Request-Method（实际请求方法）
- Access-Control-Request-Headers（自定义头）

###### 服务器需响应预检，返回

- Access-Control-Allow-Origin（允许的源）
- Access-Control-Allow-Methods（允许的方法）
- Access-Control-Allow-Headers（允许的头）

只有通过比对并通过后，浏览器才允许发送真实请求

###### 特殊行为（如 Cookie 跨域）

- 默认跨域请求 不携带 Cookie。
- 若需携带，需满足：

  - 前端设置 credentials: 'include'（Fetch）或 withCredentials: true（XHR）。
  - 服务器响应头设置 Access-Control-Allow-Credentials: true，且 Access-Control-Allow-Origin 不能为 *。

### 了解跨域流程后，解决思路

#### 跨域，本质上是什么？

- 面对跨域问题，浏览器本质上是想通过多加一步校验流程，从而保护不同源服务器之间的资源安全。
- 面对简单请求，浏览器进行放宽，自动添加 Origin 头，只需要服务器提供一个允许访问的白名单（指定或者*，但不推荐*）
- 面对非简单请求，浏览器会额外多一次预检请求，浏览器发送 Origin 头，标明源站，需要服务器返回响应头，标明源站白名单、方法白名单、请求头白名单

#### 跨域最主流的 CORS 方案

- 其实 Cors 方案，就是在服务器返回的响应头中多加几个，就是允许的源站、允许的请求方法、允许的自定义头，十分简单！
- 因此当我们有使用任何自定义头的时候，都需要注意添加到 Cors 中，如果我们不知道这些原理，就可能一直被困在跨域问题中!
- 浏览器会十分严格的比对，只有全部允许了才放行（注意：对于固定的简单请求的请求头，浏览器会自行处理，只需要我们在 Cors 中添加对应的自定义请求头即可）

说白了：Cors 就是添加响应头，浏览器校验通过就自动放行。

### 如何添加 Cors 头？

#### 后端自主添加

- SpringBoot 提供的 @CORSsOrigin，标注在方法上，或者 Controller 类上

```java
@CORSsOrigin(origins = "*")  // 加在Controller类或方法上
```

- 全局配置（推荐）

```typescript
@Configuration
public class CorsConfig implements WebMvcConfigurer {
   @Override
   public void addCorsMappings(CorsRegistry registry) {
       registry.addMapping("/**") // 添加对应的路径通配符，这里表示所有路径
               .allowedOrigins("*") // 添加允许的源站，这里允许所有
               .allowedMethods("GET", "POST"); // 允许的方法
               // 注意：如果不允许"OPTIONS"方法，那么意味着浏览器的预检请求不被允许，也会被跨域拦住，因此此时只允许的是简单方法经过
   }
}
```

- 手动响应头（灵活但麻烦）：

```java
response.setHeader("Access-Control-Allow-Origin", "*");
```

#### Nginx 代理添加

- Nginx 作为反向代理的网关层，其更像是一个加强器
- 请求打到 Nginx，Nginx 会对该请求进行一些操作，进行加强，比如路由到不同的后端服务器，或者说在服务器返回的响应头中添加 Cors 头，示例如下（这里不是加 Cors 头，是在请求到后端的过程中加上的一些头）

```nginx
# API 代理配置
location /api/ {
    proxy_pass http://127.0.0.1:25621/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
# proxy_set_header：这里添加的是代理头，是nginx自主添加返回给后端的，而浏览器打在nginx上的请求头并不会携带，因此后续后端返回的Cors头（不需要对这些请求头额外放行），浏览器进行比对的时候，是不会被拦截的
# 为啥要加这些Header？这些Header相当于是Nginx将原始请求的一些信息传给后端，比如$host是用户的主机号、$remote_addr是用户的ip地址，$proxy_add_x_forwarded_for相当于经过的ip的链
示例：
用户IP是 1.1.1.1 → 首层代理收到后头为：X-Forwarded-For: 1.1.1.1
经过IP 2.2.2.2 的Nginx时 → 自动追加为：X-Forwarded-For: 1.1.1.1, 2.2.2.2
```

如果不经过 Nginx，直接打到后端，是否也可以获取这些信息？

能！不过更加麻烦一些，Nginx 可以很方便的加到请求头上

- 加 Cors 头示例如下

```nginx
location / {
        # 处理 OPTIONS 预检请求
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' 'https://smxy.com';
            add_header 'Access-Control-Allow-Methods' 'GET, POST, OPTIONS';
            add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type, x-auth-token'; # 标注允许Authorization, Content-Type, x-auth-token自定义头
            add_header 'Access-Control-Max-Age' 1728000; # 预检缓存20天
            return 204;
        }
        
        # 正常请求的 CORS 头
        add_header 'Access-Control-Allow-Origin' 'https://smxy.com';
        add_header 'Access-Control-Allow-Credentials' 'true';
        add_header 'Access-Control-Expose-Headers' 'Content-Length, Content-Range';
        
        add_header Cache-Control "no-store";
        proxy_pass http://127.0.0.1:25621;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
```

说明：

1. 当浏览器发送预检验请求时候，操作方式是 OPTIONS，此时 Nginx 代理，会走 if 逻辑
2. 此时会经过 add_header，在后端返回到 Nginx，Nginx 返回到浏览器时候会多加 Cors 头，并在此返回 204 状态码
3. 通过添加 Cors 头后，其包括 OPTIONS 请求方法，浏览器比对后会直接放行，再次发送原始请求
4. 这时候就不走 if 逻辑了，因此在这里的 add_header 不会再多加方法、自定义头的允许，但是注意：简单请求会绕过预检验，因此依旧需要添加允许的源站，不然简单请求的返回体就会被隐藏。
5. proxy_set_header：向后端传递特定的信息，方便通过 Header 头获取

### 总结

Nginx 配置更像是骗浏览器，其本质上就是通过帮服务端添加 Cors 请求头，莱蒙混过关，但是对于我来说，我更推荐直接在后端加上跨域请求，同时**严格注意允许源站、允许请求方法一定、至少包含 OPTIONS、以及允许的自定义头，一定要全部包含项目中有的，不然我们就会在这些跨域请求的问题中一直绕弯路！**

### 一些案例

#### 这是同源请求还是跨域请求？

![](static/RpnCbjENnoWBRSxwhEgcXmA6nzd.png)

请看**请求标头**的 **Host** 和 **Origin**，其中 Host 表示请求的目标地址，而 Origin 表示当前的源地址，那么这是跨域请求还是同源请求？

答：同源请求，协议（Https）、域名（[www.smxy.com](http://www.smxy.com)）、端口(443)全部一致，因此这是同源请求。

**为什么会发生这样的情况？**

因为当前的前端页面就是 [www.smxy.com](http://www.smxy.com) 而对于前端应用，采用的是相对地址请求，浏览器会自动拼接地址成为 [www.smxy.com/api/xxx](http://www.smxy.com/api/xxx)，这时候请求地址 Host 就是 [www.smxy.com](http://www.smxy.com)，而页面源地址就是 [www.smxy.com](http://www.smxy.com)，符合同源请求，因此不会进行跨域流程。

**那么为什么敢这么笃定不是跨域流程呢？**

1. 因为当时后端配置的 CORS 头，没有允许自定义 Header：Authorization，但是其依旧放行了需要预检的请求
2. 并且在浏览器的网络抓包中，并没有发现任何预检请求的痕迹，因此我们敢确定，浏览器认为该请求是同源请求，而不是跨域请求

#### 是同源请求没错了，但这又是什么情况？

上一张展示的图片，是经过后端 CORS 配置，允许域名为 [https://www.smxy.com](https://www.smxy.com) 的请求放行，才解决的，在解决之前，报错是 403，那么**为什么明明是同源请求，但是还会出现看似是跨域的错误呢？**

首先排查 Nginx，nginx 配置的代码如下

```nginx
server {
    listen 80;
    server_name smxy.com www.smxy.com;

    # 自动重定向到 HTTPS
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name smxy.com www.smxy.com;

    ssl_certificate     /home/ssl_cert/smxy.com_certificate.pem;
    ssl_certificate_key /home/ssl_cert/smxy.com_private.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 下面这几行可选，安全性更好
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_prefer_server_ciphers on;
    
    location /api/ {
        proxy_pass http://127.0.0.1:25621;  # 关键：指向后端服务端口
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2|ttf|svg)$ {
        expires 1h;
        add_header Cache-Control "public";
        proxy_pass http://127.0.0.1:50701;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        add_header Cache-Control "no-store";
        proxy_pass http://127.0.0.1:50701;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
server {
    listen 443 ssl;
    server_name api.voice.smxy.com;

    ssl_certificate     /home/ssl_cert/api_voice/api.voice.smxy.com_certificate.pem;
    ssl_certificate_key /home/ssl_cert/api_voice/api.voice.smxy.com_private.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_prefer_server_ciphers on;

     location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2|ttf|svg)$ {
        expires 1h;
        add_header Cache-Control "public";
        proxy_pass http://127.0.0.1:25621;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        # 这里不再让nginx进行代理添加CORS头
        add_header Cache-Control "no-store";
        proxy_pass http://127.0.0.1:25621;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

1. 可以很清楚的看到，这里是有对 [www.smxy.com](http://www.smxy.com) 做反向代理的，因此请求会正常打到本地 25621 端口（后端部署地址），因此不是 nginx 的问题
2. 再看后端 CORS 配置

```typescript
@Override
    _public void _addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
_//                .allowedOrigins("https://smxy.com")_
_                // 允许对应的域名访问_
_                _.allowedOrigins("https://smxy.com")
                _// 允许的请求方法_
_                _.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                _// 允许的请求头_
_                _.allowedHeaders("Content-Type", "x-auth-token", "Authorization")
                _// 允许携带 Cookie_
_                _.allowCredentials(_true_);
    }
```

这里放行 [https://smxy.com](https://smxy.com)，可以进行访问，但是 [https://www.smxy.com](https://www.smxy.com) 就不可以了，是不是因为这里出现了问题？添加之后就发现确实如此，那么为什么浏览器分明是同源请求，还是被拦住了呢？

AI 解释：这是 SpringBoot 框架的安全策略，当配置 CORS 时候，框架也会自主进行匹对，如果 Origin（源站地址）不在允许的范围内，则会自动返回 403 错误，这也就导致了同源请求却发生类似跨域错误的问题。

然后让我来解释一下这些代码都是什么含义

```nginx
server {
    listen 80;
    server_name smxy.com www.smxy.com;

    # 自动重定向到 HTTPS
    return 301 https://$host$request_uri;
}
```

这里表示监听 80 端口，服务名为 smxy.com、[www.smxy.com](http://www.smxy.com)，通过直接输入 smxy.com，在浏览器实际上就是 [smxy.com](http://smxy.com)，nginx 进行匹配，随后就会直接 return 301，重定向到 https 版本的 smxy.com，因此我们就达到了正常输入 smxy.com 就自动到 [smxy.com](https://smxy.com) 的效果了，让用户无感知跳转，而随后的所有相对地址也都是通过 https 版本进行请求，**因为 nginx 的代理是隐蔽的，浏览器暴露的反而是原始接口**

```nginx
server {
    listen 443 ssl;
    server_name smxy.com www.smxy.com;

    ssl_certificate     /home/ssl_cert/smxy.com_certificate.pem;
    ssl_certificate_key /home/ssl_cert/smxy.com_private.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 下面这几行可选，安全性更好
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_prefer_server_ciphers on;
    
    location /api/ {
        proxy_pass http://127.0.0.1:25621;  # 关键：指向后端服务端口
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2|ttf|svg)$ {
        expires 1h;
        add_header Cache-Control "public";
        proxy_pass http://127.0.0.1:50701;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        add_header Cache-Control "no-store";
        proxy_pass http://127.0.0.1:50701;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

1. 这里就是表示监听 443 端口了，匹配的依旧是 smxy.com，www.smxy.com;但这次是 https 版本的，也就是说，经过 80 端口重定向后，实际上已经到达了 443 端口
2. 在这里配置了一些重定向匹配，通过 location 请求进行，对于/api 前缀请求，会走本地的 25621 端口，注意：这里不代表浏览器行为，不需要考虑跨域的问题，因此请求会直接打到后端
3. 但是后端因为框架安全策略，如果配置了 CORS，框架会自行进行匹配，因此如果没有放行 [www.smxy.com](http://www.smxy.com)，框架也会拒绝请求，导致出现 403，看似是跨域请求的问题，实则并不是，框架无视同源/跨域请求，如果不被允许，就直接禁止通行。
4. 其他的不再做解释，其实就是匹配的根路径直接跳转到前端部署页面，而对于静态文件则是正常进行请求。
5. 后续的 api.voice.smxy.com 也是同理，不再做解释
