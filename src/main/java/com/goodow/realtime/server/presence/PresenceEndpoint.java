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

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.prospectivesearch.FieldType;
import com.google.appengine.api.prospectivesearch.ProspectiveSearchService;
import com.google.appengine.api.prospectivesearch.Subscription;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Named;

@Api(name = "presence", version = RealtimeApisModule.DEFAULT_VERSION, defaultVersion = AnnotationBoolean.TRUE, namespace = @ApiNamespace(ownerDomain = "goodow.com", ownerName = "Goodow", packagePath = "api.services"))
public class PresenceEndpoint {
  private static final Logger log = Logger.getLogger(PresenceEndpoint.class.getName());
  private static final int PresenceExpirationSeconds = 200;

  @Inject
  private ProspectiveSearchService service;

  @ApiMethod(name = "connect")
  public void connect(@Named("sessionId") String sessionId, @Named("documentIds") String... docIds) {
    if (docIds == null) {
      return;
    }
    for (String docId : docIds) {
      subscribe(sessionId, docId);
    }
  }

  @ApiMethod(name = "disconnect")
  public void disconnect(@Named("sessionId") String sessionId,
      @Named("documentIds") String... docIds) {
    if (docIds == null || docIds.length == 0) {
      List<Subscription> subscriptions;
      try {
        subscriptions = service.listSubscriptions(PresenceUtil.sidTopic(sessionId));
      } catch (IllegalArgumentException e) {
        return;
      }
      for (Subscription sub : subscriptions) {
        unsubscribe(sessionId, sub.getId());
      }
      return;
    }
    for (String docId : docIds) {
      unsubscribe(sessionId, docId);
    }
  }

  private void subscribe(String sessionId, String docId) {
    log.config("Subscribing " + sessionId + " to " + docId);
    String query = "a:true";
    Map<String, FieldType> schema = Collections.singletonMap("b", FieldType.BOOLEAN);
    service.subscribe(PresenceUtil.docIdTopic(docId, sessionId), sessionId,
        PresenceExpirationSeconds, query, schema);
    service.subscribe(PresenceUtil.sidTopic(sessionId), docId, PresenceExpirationSeconds, query,
        schema);
  }

  private void unsubscribe(String sessionId, String docId) {
    log.config("Unsubscribing " + sessionId + " to " + docId);
    service.unsubscribe(PresenceUtil.docIdTopic(docId, sessionId), sessionId);
    service.unsubscribe(PresenceUtil.sidTopic(sessionId), docId);
  }
}
