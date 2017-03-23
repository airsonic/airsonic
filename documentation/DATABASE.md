# Database Configuration

*Before doing anything, make sure your database is properly backed up. Ensure your server is shutdown*

Libresonic has the capability to override the database settings. If you wish to
continue using the current hsql 1.8 database driver, no action is needed. When
upgrading to a new version of Libresonic powered by liquibase you may see some
liquibase logging to double check old migrations, but on subsequent startups it
will not execute them again.

For those that wish to change their database, instructions differ based on
whether you wish for your database connection to be managed by your container (tomcat),
or whether you wish Libresonic to manage it for you. The former may offer some performance 
gains in the case of many concurrent users with connection pooling while the latter is easiest.

We will refer to container managed configuration as jndi and libresonic managed configuration as embedded.

## Embedded
*Before doing anything, make sure your database is properly backed up. Ensure your server is shutdown*

In your libresonic.properties file, you will need to add the following settings (this is just an example):

```
DatabaseConfigType=embed
DatabaseConfigEmbedDriver=org.hsqldb.jdbcDriver
DatabaseConfigEmbedUrl=jdbc:hsqldb:file:/tmp/libre/db/libresonic
DatabaseConfigEmbedUsername=sa
DatabaseConfigEmbedPassword=
```

In addition, you will need to ensure that a jdbc driver suitable for your
database is on the
[classpath](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/classpath.html).

*Note adding to the classpath is currently pretty difficult for spring-boot. Tomcat is easy, just copy into tomcat home 
/lib. TODO: provide prebuilt artifacts with tested databases built in?*

## JNDI
*Before doing anything, make sure your database is properly backed up. Ensure your server is shutdown*

In your libresonic.properties file, you will need to add the following settings (this is just an example):

```
DatabaseConfigType=jndi
DatabaseConfigJNDIName=jdbc/libresonicDB
```

Then in your context.xml in your tomcat directory, add the jndi config:

```
    <Resource name="jdbc/libresonicDB" auth="Container"
        type="javax.sql.DataSource"
        maxActive="20"
        maxIdle="30"
        maxWait="10000"
        username="libresonic"
        password="REDACTED"
        driverClassName="com.mysql.jdbc.Driver"
        url="jdbc:mysql://hostname/libresonic?sessionVariables=sql_mode=ANSI_QUOTES"/>

```

Finally, copy the jdbc driver from the database vendor website to the `lib` directory in your tomcat folder.

## Database Vendor Specific Notes

### PostgreSQL

`stringtype=unspecified` on your jdbc url string is necessary.

You will also need to add `DatabaseUsertableQuote=\"` to your properties
file. This is due to the fact that our `user` table is a keyword for postgres.

## Troubleshooting

In the event that you change these settings, restart your server and it fails to start, you can remedy this by reverting
to the LEGACY config by removing all `Database*` settings from your `libresonic.properties` file.
