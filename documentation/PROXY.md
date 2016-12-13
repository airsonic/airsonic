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

A few settings can be tweaked in Libresonic's startup script or Tomcat
configuration.

The reverse proxy will handle HTTPS connections, so there is no need for
Libresonic to handle them, which is why we set `httpsPort` to 0:

    libresonic.httpsPort=0

Furthermore, the internal Libresonic server should only be accessible from the
inside of the reverse proxy : we tell Libresonic to listen on the local IP
only:

    libresonic.host=127.0.0.1
    libresonic.port=4040

Finally, if Libresonic should be accessible from a subdirectory, the context
path must be set correctly:

    libresonic.contextPath=/libresonic

## Reverse proxy configuration

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

The following configuration works for HAProxy (HTTPS only):

```haproxy
frontend https
    bind $server_public_ip$:443 ssl crt /etc/haproxy/ssl/$server_ssl_keys$.pem

    # Let Libresonic handle all requests under /libresonic
    acl url_libresonic path_beg -i /libresonic
    use_backend libresonic-backend if url_libresonic

    # Change default backend to libresonic backend if you don't have a web backend
    default_backend web-backend

backend libresonic-backend
  server libresonic 127.0.0.1:4040 check
```
