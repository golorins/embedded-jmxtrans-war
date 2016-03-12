/*
 * Copyright (c) 2016 the original author or authors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package golorins.jmxtrans.embedded.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.jmxtrans.embedded.config.KVStore;
import org.jmxtrans.embedded.config.KeyValue;

/**
 * JMXTrans configuration from a remote key value store. The configuration is stored in a key that
 * holds the references to the various elements that make up the final jmxtrans configuration
 *
 * @author Simone Zorzetti
 */
public class KVStoreComposedConfiguration {

  private static final Map<String, String> keyModifiedIndexes = new ConcurrentHashMap<String, String>();
  private static volatile String lastConfigKeyPath = null;
  private final KVStore keyValueStore;

  /**
   * Constructor
   *
   * @param store KVStore: implementation of the KVStore interface to use
   */
  public KVStoreComposedConfiguration(KVStore store) {
    keyValueStore = store;
  }

  /**
   * This method scans the KV store tree along the path of the key provided (bottom to top)
   * searching for the configuration key passed.<br>
   * Ex:<br>
   * etcd://127.0.0.1:123/root/level1/level2/config<br>
   * etcd://127.0.0.1:123/root/level1/config<br>
   * etcd://127.0.0.1:123/root/config<br>
   * etcd://127.0.0.1:123/config<br>
   * <p>
   * The configuration key is treated as a coma separated list of absolute paths in the kv store
   * which represent the elements that should be merged to obtain the final jmxtrans configuration.
   * <p>
   * Ex:<br>
   * /elements/jdk7, /elements/tomcat7, /elements/output<br>
   * <p>
   * Returns a coma separeted list of etcd URLs representing the keys that make up the jmxtrans
   * desired configuration<br>
   * If the all the keys didn't change since the last invocation returns null<br>
   * Ex:<br>
   * etcd://127.0.0.1:123/elements/jdk7, etcd://127.0.0.1:123/elements/tomcat7,
   * etcd://127.0.0.1:123/elements/output<br>
   *
   * @param configKeyUri the uri of the configuration key
   * @return
   * @throws EmbeddedJmxTransException
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
  public List<String> getConfigElementsKeys(String configKeyUri) throws EmbeddedJmxTransException {

    String etcdURI = configKeyUri.substring(0, configKeyUri.indexOf("/", 7));
    String path = configKeyUri.substring(configKeyUri.indexOf("/", 7), configKeyUri.lastIndexOf("/"));
    String key = configKeyUri.substring(configKeyUri.lastIndexOf("/"));

    String configValues = null;
    boolean configChanged = false;
    // Traverse the tree bottom to top searching key
    while (configValues == null && path.length() > 1) {
      configChanged = (getKeyValueIfModified(etcdURI + path + "/" + key) != null);
      configValues = getKeyValue(etcdURI + path + "/" + key);
      System.err.println("Path: " + path + " Configvalues: " + configValues);
      path = path.substring(0, path.lastIndexOf("/"));
    }

    if (configValues == null) {
      // Couldn't get the config keys from etcd
      return null;
    }

    if (!path.equals(lastConfigKeyPath)) {
      // configKeyUri found at a different level of the tree
      configChanged = true;
    }
    lastConfigKeyPath = path;

    StringTokenizer st = new StringTokenizer(configValues, ",");
    List<String> configurationUrls = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      String url = etcdURI + "/" + st.nextToken().trim();
      configurationUrls.add(url);
    }

    if (configChanged) {
      // First time or configKeyUri key changed
      return configurationUrls;
    }

    // Verify if the value of the config keys has changed since last time
    for (String keyUrl : configurationUrls) {
      configChanged = (getKeyValueIfModified(keyUrl) != null);

      if (configChanged) {
        return configurationUrls;
      }
    }

    // Config not changed
    return null;

  }

  /**
   * Retrieves a single key value from the kv store. Returns the value of the key or null if the key
   * doesn't exist. The onlyIfModified flag can force the method to return the value only if it has
   * been modified since last read
   *
   * @param KeyURI the uri of the the key to retrieve
   * @return the value of the key or null if the key doesn't exist or is unchanged
   * @throws EmbeddedJmxTransException
   */
  protected String getKeyValueIfModified(String KeyURI) throws EmbeddedJmxTransException {

    KeyValue keyVal = keyValueStore.getKeyValue(KeyURI);
    if (keyVal == null) {
      return null;
    }

    String keyValue = keyVal.getValue();

    if (keyModifiedIndexes.get(KeyURI) != null && keyVal.getVersion().equals(keyModifiedIndexes.get(KeyURI))) {
      keyVal = null;
    } else {
      keyModifiedIndexes.put(KeyURI, keyVal.getVersion());
    }

    return keyValue;

  }

  /**
   * Retrieves a single key value from the kv store. Returns the value of the key or null if the key
   * doesn't exist.
   *
   * @param KeyURI the uri of the key to retrieve
   * @return the value of the key or null if the key doesn't exist or is unchanged
   * @throws EmbeddedJmxTransException
   */
  protected String getKeyValue(String KeyURI) throws EmbeddedJmxTransException {

    KeyValue keyVal = keyValueStore.getKeyValue(KeyURI);
    if (keyVal != null) {
      return keyVal.getValue();
    }
    return null;
  }
}
