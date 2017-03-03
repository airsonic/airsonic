# Setting up a reverse proxy

A reverse proxy is a public-facing web server sitting in front of an internal
server such as Libresonic. The Libresonic server never communicates with the
outside ; instead, the reverse proxy handles all HTTP(S) requests and forwards
them to Libresonic.

This is useful in many ways, such as gathering all web configuration in the
same place. It also handles some options (HTTPS) much better than the bundled
Libresonic server or a servlet container such as Tomcat.

This guide assumes you already have a working Libresonic installation after
following the [installation guide](documentation/INSTALL.md).

## Getting a TLS certificate

This guide assumes you already have a TLS certificate. [Let's
Encrypt](https://letsencrypt.org) currently provides such certificates for
free.

## Libresonic configuration

A few settings should be tweaked via Spring Boot or Tomcat
configuration:

  - Set the context path to `/libresonic`
  - Set the correct address to listen to
  - Set the correct port to listen to

#### Spring Boot

Add the following java args:

```java -Dserver.port=4040 -Dserver.address=127.0.0.1 -Dserver.contextPath=/libresonic -jar libresonic.war```

#### Tomcat
Modify your `<Connector>` with the proper address and port:

```
<Connector 
    port="4040" 
    address="127.0.0.1"
    ...
```
See [HTTP Connector](https://tomcat.apache.org/tomcat-7.0-doc/config/http.html) for further detail.

For the context path, tomcat will automatically deploy to a context path matching your war name. So if you're using 
libresonic.war, you do not need to change anything.

## Reverse proxy configuration

#### How it works
Libresonic expects proxies to provide information about their incoming URL so that Libresonic can craft it when needed.
To do so, Libresonic looks for the following HTTP headers:

 - `X-Forwarded-Host`
   - Provides server name and optionally port in the case that the proxy is on a non-standard port
 - `X-Forwarded-Proto`
   - Tells Libresonic whether to craft an HTTP or HTTPS url
 - `X-Forwarded-Server`
   - This is only a fallback in the case that `X-Forwarded-Host` is not available

Currently this is used wherever, `NetworkService#getBaseUrl` is called. A couple notable places include:

- Stream urls
- Share urls
- Coverart urls

### Nginx

The following configuration works for Nginx (HTTPS with HTTP redirection):

```nginx
# Redirect HTTP to HTTPS
server {
    listen      80;
    server_name example.com;
    return      301 https://$server_name$request_uri;
}

server {

    # Setup HTTPS certificates
    listen       443 default ssl;
    server_name  example.com;
    ssl_certificate      cert.pem;
    ssl_certificate_key  key.pem;

    # Proxy to the Libresonic server
    location /libresonic {
      proxy_set_header X-Real-IP         $remote_addr;
      proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header X-Forwarded-Host  $http_host;
      proxy_set_header Host              $http_host;
      proxy_max_temp_file_size           0;
      proxy_pass                         http://127.0.0.1:4040;
      proxy_redirect                     http:// https://;
    }
}
```

### Apache

The following configuration works for Apache (without HTTPS):

```apache
<VirtualHost *:80>
    ServerName        example.com
    ErrorDocument 404 /404.html
    DocumentRoot      /var/www
    ProxyPass         /libresonic http://localhost:4040/libresonic
    ProxyPassReverse  /libresonic http://localhost:4040/libresonic
</VirtualHost>
```

### HAProxy

**OUTDATED since `X-Forwarded-Host`/`X-FORWARDED-PROTO` change**

The following configuration works for HAProxy (HTTP and HTTPS):

```haproxy

defaults
    
    # Use HTTP protocole
    mode http
    
frontend https
    
    # Listen on the HTTPS and HTTP ports
    bind :80
    bind :443 ssl crt /etc/haproxy/certs/cert_key.pem

    # Bind URL with the right backend
    acl is_libresonic  path_beg -i /libresonic
    use_backend libresonic-backend if is_libresonic

backend libresonic-backend

    # Rewrite all redirects to use HTTPS, similar to what Nginx does in the
    # proxy_redirect directive.
    http-response replace-value Location ^http://(.*)$ https://\1

    # (OPTIONAL) Force HTTPS
    redirect scheme https if !{ ssl_fc }

    # Forward requests to Libresonic running on localhost on port 4040
    server libresonic 127.0.0.1:4040 check
```
