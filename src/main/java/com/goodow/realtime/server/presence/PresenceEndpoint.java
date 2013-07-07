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
package com.goodow.realtime.server.presence;

import com.goodow.realtime.server.RealtimeApisModule;
import com.goodow.realtime.server.auth.AccountContext;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.prospectivesearch.ProspectiveSearchService;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.logging.Logger;

import javax.inject.Named;

@Api(name = "presence", version = RealtimeApisModule.DEFAULT_VERSION, defaultVersion = AnnotationBoolean.TRUE, namespace = @ApiNamespace(ownerDomain = "goodow.com", ownerName = "Goodow", packagePath = "api.services"))
public class PresenceEndpoint {
  private static final Logger log = Logger.getLogger(PresenceEndpoint.class.getName());
  private static final String PREFIX = "PS";
  // @Inject
  // private Cache cache;
  @Inject
  private Provider<AccountContext> ctx;
  @Inject
  private ProspectiveSearchService service;

  @ApiMethod(name = "connect")
  public void connect(@Named("sessionId") String sessionId) {
    // cache.put(sessionId, null);
  }

  @ApiMethod(name = "disconnect")
  public void disconnect(@Named("sessionId") String sessionId) {
    log.finer("disconnect: " + sessionId);
  }
}
