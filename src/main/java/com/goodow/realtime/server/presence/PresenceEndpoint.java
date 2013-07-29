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

import com.goodow.realtime.channel.constant.Platform;
import com.goodow.realtime.server.RealtimeApisModule;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.prospectivesearch.ProspectiveSearchService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.walkaround.util.server.appengine.MemcacheTable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;

@Singleton
@Api(name = "presence", version = RealtimeApisModule.DEFAULT_VERSION, defaultVersion = AnnotationBoolean.TRUE, namespace = @ApiNamespace(ownerDomain = "goodow.com", ownerName = "Goodow", packagePath = "api.services"))
public class PresenceEndpoint {
  private static final Logger log = Logger.getLogger(PresenceEndpoint.class.getName());
  private static final int PresenceExpirationSeconds = 200;
  private static final String SESSIONS_MEMCACHE_TAG = "S2D";
  private static final String DOC_WEB_MEMCACHE_TAG = "DSW";
  private static final String DOC_ANDROID_MEMCACHE_TAG = "DSA";
  private static final String DOC_IOS_MEMCACHE_TAG = "DSI";

  @Inject
  private ProspectiveSearchService service;
  // @Inject
  // private SlobStore store;
  private final MemcacheTable<String, HashSet<String>> sessionDocs;
  private final MemcacheTable<String, HashSet<String>> docWebSessions;
  private final MemcacheTable<String, HashSet<String>> docAndroidSessions;
  private final MemcacheTable<String, HashSet<String>> docIosSessions;

  @Inject
  PresenceEndpoint(MemcacheTable.Factory memcacheFactory) {
    this.sessionDocs = memcacheFactory.create(SESSIONS_MEMCACHE_TAG);
    this.docWebSessions = memcacheFactory.create(DOC_WEB_MEMCACHE_TAG);
    this.docAndroidSessions = memcacheFactory.create(DOC_ANDROID_MEMCACHE_TAG);
    this.docIosSessions = memcacheFactory.create(DOC_IOS_MEMCACHE_TAG);
  }

  @ApiMethod(name = "connect")
  public void connect(@Named("sessionId") String sessionId,
      @Nullable @Named("documentIds") List<String> docIds) {
    if (docIds == null) {
      return;
    }
    for (String docId : docIds) {
      subscribe(sessionId, docId);
    }
  }

  @ApiMethod(name = "disconnect")
  public void disconnect(@Named("sessionId") String sessionId,
      @Nullable @Named("documentIds") List<String> docIds) {
    if (docIds == null || docIds.isEmpty()) {
      Set<String> docs = listSessionSubscriptions(sessionId);
      for (String docId : docs) {
        unsubscribe(sessionId, docId);
      }
      return;
    }
    for (String docId : docIds) {
      unsubscribe(sessionId, docId);
    }
  }

  @ApiMethod(path = "listDocumentSubscriptions")
  public Set<String> listDocumentSubscriptions(@Named("documentId") String documentId,
      @Named("platformName") String platformName) {
    Platform platform = Platform.valueOf(platformName);
    if (platform == null) {
      return null;
    }
    MemcacheTable<String, HashSet<String>> sessions = getCachedSessions(platform.name());
    return sessions.get(documentId);
  }

  @ApiMethod(path = "listSessionSubscriptions")
  public Set<String> listSessionSubscriptions(@Named("sessionId") String sessionId) {
    return sessionDocs.get(sessionId);
  }

  private MemcacheTable<String, HashSet<String>> getCachedSessions(String platform) {
    switch (Platform.valueOf(platform)) {
      case WEB:
        return docWebSessions;
      case ANDROID:
        return docAndroidSessions;
      case IOS:
        return docIosSessions;
      default:
        throw new UnsupportedOperationException();
    }
  }

  // private void presenceOperation(ObjectId id) {
  // String snapshot = null;
  // try {
  // snapshot = store.loadAtVersion(id, null);
  // } catch (SlobNotFoundException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // } catch (IOException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // } catch (AccessDeniedException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // }
  // DocumentBridge bridge = new DocumentBridge((JsonArray) Json.instance().parse(snapshot));
  // bridge.setOutputSink(new OutputSink() {
  //
  // @Override
  // public void close() {
  // }
  //
  // @Override
  // public void consume(RealtimeOperation<?> op) {
  // }
  // });
  // Model model = bridge.getDocument().getModel();
  // CollaborativeList list = model.getRoot().get(Constants.PRESENCE_KEY);
  // }

  private void subscribe(String sessionId, String docId) {
    log.config("Subscribing " + sessionId + " to " + docId);
    HashSet<String> docs = (HashSet<String>) listSessionSubscriptions(sessionId);
    if (docs == null) {
      docs = new HashSet<String>();
    }
    if (!docs.contains(docId)) {
      docs.add(docId);
      sessionDocs.put(sessionId, docs, Expiration.byDeltaSeconds(PresenceExpirationSeconds));
    }
    Platform platform = Platform.fromPrefix(sessionId.charAt(0));
    HashSet<String> sessions = (HashSet<String>) listDocumentSubscriptions(docId, platform.name());
    if (sessions == null) {
      sessions = new HashSet<String>();
    }
    if (!sessions.contains(sessionId)) {
      sessions.add(sessionId);
      MemcacheTable<String, HashSet<String>> docSessions = getCachedSessions(platform.name());
      docSessions.put(docId, sessions, Expiration.byDeltaSeconds(PresenceExpirationSeconds));
    }
  }

  private void unsubscribe(String sessionId, String docId) {
    log.config("Unsubscribing " + sessionId + " to " + docId);
    HashSet<String> docs = (HashSet<String>) listSessionSubscriptions(sessionId);
    if (docs != null && docs.contains(docId)) {
      docs.remove(docId);
      sessionDocs.put(sessionId, docs, Expiration.byDeltaSeconds(PresenceExpirationSeconds));
    }
    String platformName = Platform.fromPrefix(sessionId.charAt(0)).name();
    HashSet<String> sessions = (HashSet<String>) listDocumentSubscriptions(docId, platformName);
    if (sessions != null && sessions.contains(sessionId)) {
      sessions.remove(sessionId);
      MemcacheTable<String, HashSet<String>> docSessions = getCachedSessions(platformName);
      docSessions.put(docId, sessions, Expiration.byDeltaSeconds(PresenceExpirationSeconds));
    }
  }
}
