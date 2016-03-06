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

import org.jmxtrans.embedded.config.EtcdKVStore;
import org.jmxtrans.embedded.config.KVStore;

/**
 * This is an EmbeddedJmxTransServlet which uses anetcd server as key value store for the
 * configuration
 */
public class EmbeddedJmxTransEtcdServlet extends EmbeddedJmxTransServlet {

  private static final long serialVersionUID = 1L;

  /**
   * Constructor
   */
  public EmbeddedJmxTransEtcdServlet() {
    super();
  }

  /**
   * Builds and returns an instance of KVStore interface based on etcd
   *
   * @return
   *
   * @see golorins.jmxtrans.embedded.servlet.EmbeddedJmxTransServlet#getKVStoreInstance()
   */
  protected KVStore getKVStoreInstance() {
    KVStore kvs = new EtcdKVStore();
    return kvs;
  }

}
