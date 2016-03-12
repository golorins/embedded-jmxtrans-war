# embedded-jmxtrans-war
This is a simple Servlet web app that contains an embedded-jmxtrans instance optionally configured from a key-value store.
The idea is to have an autonomous application (a war file) to drop in servlet containers as a jmxtrans "agent" that can be started or stopped independently of the others. 
The embedded-jmxtrans instance is started on init(), stopped on destroy(). 
The configuration can be provided in the classic embedded-jmxtrans ways or retrieved from a remote etcd KV store structure.  

The KV store structure is accessed through the embedded-jmxtrans ConfigurationParser and it's refreshed at regular time intervals.

## Usage
There are two ways you can use this project:
* Configured in a classic embedded-jmxtrans way it's a ready to go "agent"
* Configured through an external etcd key value store it's a powerful centrally managed jmxtrans solution.

The reason to build this project in the first place was to build something that worked with the kv store, so that is the way you can get the most out of it (see documentation).

## Documentation

* [Documentation](https://github.com/golorins/embedded-jmxtrans-war/wiki)
