/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.walkaround.slob.server;

import com.goodow.realtime.channel.constant.MessageType;
import com.goodow.realtime.channel.constant.Platform;
import com.goodow.realtime.server.model.ObjectId;
import com.goodow.realtime.server.model.Session;
import com.goodow.realtime.server.presence.MessageRouter;
import com.goodow.realtime.server.presence.PresenceEndpoint;

import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.memcache.Expiration;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.walkaround.util.server.appengine.MemcacheTable;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Router that connects client channels as listeners to objects in an m:n fashion, and provides the
 * token required for channel set up.
 * 
 * <p>
 * Messages are not guaranteed to be delivered, nor are they guaranteed to be in order, nor is there
 * any guaranteed about lack of duplicate messages. The message contents should provide enough
 * information to allow clients to deal with these situations.
 */
public class SlobMessageRouter {
  private static final int ChannelExpirationSeconds = (int) (1.8 * 60 * 60);

  private static final Logger log = Logger.getLogger(SlobMessageRouter.class.getName());

  private static final String CLIENTS_MEMCACHE_TAG = "ORC";
  private final ChannelService channelService;
  private final Map<String, MessageRouter> messageRouters;
  private final Provider<PresenceEndpoint> presence;
  private final MemcacheTable<String, String> clientTokens;

  @Inject
  public SlobMessageRouter(MemcacheTable.Factory memcacheFactory, ChannelService channelService,
      Map<String, MessageRouter> messageRouters, Provider<PresenceEndpoint> presence) {
    this.clientTokens = memcacheFactory.create(CLIENTS_MEMCACHE_TAG);
    this.messageRouters = messageRouters;
    this.channelService = channelService;
    this.presence = presence;
  }

  /**
   * Connects a client as a listener to an object. A client may listen to more than one object.
   * 
   * <p>
   * Returns the token the client should use to set up its browser channel. A client will only use
   * one token, even if it is listening to multiple objects. The router keeps track of this, and
   * will return the client's existing token if it already has one.
   */
  public String connectListener(ObjectId objectId, Session sessionId) {
    presence.get().connect(sessionId.sessionId, Collections.singletonList(objectId.toString()));
    return tokenFor(sessionId);
  }

  /**
   * Publishes messages to clients listening on an object.
   */
  public void publishMessages(ObjectId object, String jsonString) {
    for (MessageRouter messageRouter : messageRouters.values()) {
      messageRouter.push(object.toString(), MessageType.REALTIME.name(), jsonString);
    }
    log.info("Publishing " + object + " " + jsonString);
  }

  private String tokenFor(Session session) {
    String sessionId = session.sessionId;
    if (sessionId.charAt(0) != Platform.WEB.prefix()) {
      return null;
    }
    String existing = clientTokens.get(sessionId);
    if (existing != null) {
      log.info("Got existing token for client " + session + ": " + existing);
      return existing;
    }

    // This might screw up a concurrent attempt to do the same thing but
    // doesn't really matter.
    String token = channelService.createChannel(sessionId);
    clientTokens.put(sessionId, token, Expiration.byDeltaSeconds(ChannelExpirationSeconds));

    log.info("Got new token for client " + session + ": " + token);
    return token;
  }
}