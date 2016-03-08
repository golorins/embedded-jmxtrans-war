# embedded-jmxtrans-war
Simple Servlet web app that contains an embedded jmxtrans configured from a KV store.
The embedded-jmxtrans instance is started on init() and stopped on destroy(). 
The configuration is retrieved from a remote pluggable KV store. At now an implementation for etcd is provided.

# Architecture
The purpose of the KV store is to be able to manage in a central location the jmxtrans configurations for a lot of application servers.
In order to do that in a powerful and simple way, the configuration is built with one level of indirection.
We have a configuration key which contains the references to the various elements that once merged make up the final JMXTrans configuration. The references are absolute paths in the KV store.

The value of the configuration key is retrieved using a scan in depth first of the path provided. The first occurrence present is used.
```
Ex.:
 etcd://127.0.0.1:123/jdk7/tomcat/service1/customer1/family1/instance1/config<br>
 etcd://127.0.0.1:123/jdk7/tomcat/service1/customer1/family1/config<br>
 etcd://127.0.0.1:123/jdk7/tomcat/service1/customer1/config
 etc..
```

The idea is that the hierarchy is built from the most general down to the more specific. 
Every application server contains an instance of the embedded-jmxtrans-war and is given a specific config key based on the type and purpose of the container.
With this key the embedded-jmxtrans-war can retrieve the right configuration and we can tune it centrally at any level of the hierarchy.

A configuration key is a coma separated list of elements that should be merged to obtain the final JMXTrans configuration. 
For example a string like this:

```
"/elements/jdk7, /elements/tomcat6, /elements/output-statsd, /elements/interval10"
```

In the KV store we could have a subtree (eg. /elements/) containing all the elements that can be combined. The elements are JSON excerpt of JMXTrans configuration.

To manage the monitoring output we can change a configuration key, or we can create/delete one at a more specific level, or change the content of an element.

At a customizable time interval the configuration is refreshed and in case it was changed (down to the elements) the current embedded-jmxtrans is stopped, and a new one is restarted.

# Configuration

In order to configure the servlet you can define two system properties:

```
jmxtrans.kv.config: URI of the configuration key
jmxtrans.kv.refresh: refresh time interval in milliseconds: default 120sec
```

The value of jmxtrans.kv.config is also retrieved from the init-param of the servlet as defined in the web.xml file. The system property value overrides the init-param one.

To specify a cluster of etcd servers you can use the syntax ```etcd://[127.0.0.1:4001,127.0.0.1:4002]/path```. Servers are used in the given order
Ex.
```
-Djmxtrans.kv.config=etcd://192.168.10.100:4001/jmxtrans/config  -Djmxtrans.kv.refresh=300000
```

#### Note
This application uses my fork of embedded-jmxtrans which has the ability to read configuration url from etcd and can substitute values from system properties in the result aliases.
