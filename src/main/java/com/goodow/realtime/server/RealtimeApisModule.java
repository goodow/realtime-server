/*
 * Copyright 2012 Goodow.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.goodow.realtime.server;

import com.goodow.realtime.channel.http.HttpTransport;
import com.goodow.realtime.server.device.DeviceInfoEndpoint;
import com.goodow.realtime.server.persist.jpa.JpaFinderProxy;
import com.goodow.realtime.server.servlet.util.ProxyFilter;

import com.google.api.server.spi.guice.GuiceSystemServiceServletModule;
import com.google.inject.matcher.Matchers;
import com.google.inject.persist.PersistFilter;
import com.google.inject.persist.finder.Finder;
import com.google.inject.persist.jpa.JpaPersistModule;

import org.aopalliance.intercept.MethodInterceptor;

import java.util.HashSet;
import java.util.Set;

public class RealtimeApisModule extends GuiceSystemServiceServletModule {
  public static final String FRONTEND_ROOT = HttpTransport.ROOT + ProxyFilter.PROXY_PATH + "api";
  public static final String BACKENDROOT_ROOT = HttpTransport.ROOT + ProxyFilter.PROXY_PATH + "spi";

  @Override
  protected void configureServlets() {
    install(new JpaPersistModule("transactions-optional"));
    filter("/*").through(PersistFilter.class);
    MethodInterceptor finderInterceptor = new JpaFinderProxy();
    requestInjection(finderInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Finder.class), finderInterceptor);

    filter(ProxyFilter.PROXY_PATH + "*").through(ProxyFilter.class);

    Set<Class<?>> serviceClasses = new HashSet<Class<?>>();
    serviceClasses.add(DeviceInfoEndpoint.class);
    this.serveGuiceSystemServiceServlet("/_ah/spi/*", serviceClasses);
  }
}
