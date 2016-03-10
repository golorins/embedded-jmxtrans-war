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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.jmxtrans.embedded.EmbeddedJmxTrans;
import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.jmxtrans.embedded.config.ConfigurationParser;
import org.jmxtrans.embedded.config.KVStore;

/**
 * This is the servlet
 */
public abstract class EmbeddedJmxTransServlet extends GenericServlet {

  public static final long DEFAULT_REFRESH_INTERVAL = 120000l;
  public static final String JMXTRANS_KV_REFRESH_INTERVAL = "jmxtrans.kv.refresh";
  public static final String JMXTRANS_KV_CONFIG = "jmxtrans.kv.config";

  private static final long serialVersionUID = 1L;
  private transient final Timer configCheckTimer = new Timer(true);

  private transient EmbeddedJmxTrans embeddedJmxTrans;
  private ObjectName objectName;
  private transient MBeanServer mbeanServer;
  private boolean isRunning = false;
  private String configuration;
  private List<String> configurationUrls = null;
  private long refreshInterval;

  /**
   *
   */
  public EmbeddedJmxTransServlet() {

    configuration = System.getProperty(JMXTRANS_KV_CONFIG);
    try {
      refreshInterval = Integer.parseInt(System.getProperty(JMXTRANS_KV_REFRESH_INTERVAL, "0"));
    } catch (NumberFormatException e) {
      refreshInterval = DEFAULT_REFRESH_INTERVAL;
    }
  }

  /**
   * This method is implemented just to satisfy the Servlet requirements.
   *
   * @param arg0
   * @param arg1
   * @throws ServletException
   * @throws IOException
   *
   * @see javax.servlet.GenericServlet#service(javax.servlet.ServletRequest,
   *      javax.servlet.ServletResponse)
   */
  public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
    // nothing to do
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    if (configuration == null) {
      configuration = config.getInitParameter(JMXTRANS_KV_CONFIG);
    }
    if (refreshInterval == 0) {
      try {
         refreshInterval = Integer.parseInt(config.getInitParameter(JMXTRANS_KV_REFRESH_INTERVAL));
      } catch (NumberFormatException e) {
         refreshInterval = DEFAULT_REFRESH_INTERVAL;
      }
    }

    if (configuration == null || configuration.length() == 0) {
      throw new EmbeddedJmxTransException("Missing '" + JMXTRANS_KV_CONFIG + "' init param or system property");
    }

    startJmxTrans();

    TimerTask configCheckTask = new TimerTask() {

      public void run() {

        List<String> configurationUrls = null;
        try {
          // Get from KV config keys if changed
          configurationUrls = readConfigurationUrls();
        } catch (EmbeddedJmxTransException e1) {
          // Cannot read KV, skip check cycle
        }
        if (configurationUrls == null)
          return;

        setConfigurationUrls(configurationUrls);
        stopJmxTrans();
        try {
          Thread.sleep(2000);
          startJmxTrans();
        } catch (InterruptedException e) {
          // nothing to do
        } catch (EmbeddedJmxTransException e1) {
          // Cannot restart jmxtrans
          getServletContext().log("Can't restart embedded-jmxtrans: config error ? " + configuration);
        }
      }
    };

    getServletContext().log("Embedded-jmxtrans refresh interval: " + refreshInterval);
    configCheckTimer.schedule(configCheckTask, refreshInterval, refreshInterval);

  }

  public void destroy() {
    stopJmxTrans();
    super.destroy();
  }

  protected void startJmxTrans() {

    if (configuration == null || configuration.isEmpty() || isRunning)
      return;

    getServletContext().log("Start embedded-jmxtrans config: " + configuration);

    mbeanServer = ManagementFactory.getPlatformMBeanServer();
    ConfigurationParser configurationParser = new ConfigurationParser();

    if (configurationUrls == null) {
      configurationUrls = readConfigurationUrls();
    }
    if (configurationUrls == null) {
      getServletContext().log("Empty config urls: no jmxtrans! ");
      return;
    }

    embeddedJmxTrans = configurationParser.newEmbeddedJmxTrans(configurationUrls);
    String on = "org.jmxtrans.embedded:type=EmbeddedJmxTrans,name=jmxtrans,path=" + getServletContext().getContextPath();
    try {
      objectName = mbeanServer.registerMBean(embeddedJmxTrans, new ObjectName(on)).getObjectName();
    } catch (Exception e) {
      throw new EmbeddedJmxTransException("Exception registering '" + objectName + "'", e);
    }
    try {
      embeddedJmxTrans.start();
    } catch (Exception e) {
      String message = "Exception starting jmxtrans, application '" + getServletContext().getContextPath() + "'";
      getServletContext().log(message, e);
      throw new EmbeddedJmxTransException(message, e);
    }
    isRunning = true;

  }


  protected void stopJmxTrans() {

    if (isRunning == false)
      return;

    getServletContext().log("Stop embedded-jmxtrans ...");

    try {
      mbeanServer.unregisterMBean(objectName);
    } catch (Exception e) {
      getServletContext().log("Silently skip exception unregistering mbean '" + objectName + "'");
    }
    try {
      embeddedJmxTrans.stop();
    } catch (Exception e) {
      getServletContext().log("Silently skip exception stopping '" + objectName + "'", e);
    }
    isRunning = false;

  }

  protected List<String> getConfigurationUrls() {
    return configurationUrls;
  }

  protected void setConfigurationUrls(List<String> configurationUrls) {
    this.configurationUrls = configurationUrls;
  }

  /**
   * Read configuration URLS from the key value store
   *
   * @return
   */
  protected List<String> readConfigurationUrls() {

    KVStore kvs = getKVStoreInstance();
    KVStoreComposedConfiguration kvsConf = new KVStoreComposedConfiguration(kvs);

    return kvsConf.getConfigElementsKeys(configuration);
  }

  protected abstract KVStore getKVStoreInstance();


}
