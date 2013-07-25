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

import com.goodow.realtime.channel.constant.MessageType;
import com.goodow.realtime.channel.constant.Platform;
import com.goodow.realtime.server.RealtimeApisModule;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.PayloadBuilder;

import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Named;

@Singleton
@Api(name = "presence", version = RealtimeApisModule.DEFAULT_VERSION, defaultVersion = AnnotationBoolean.TRUE)
public class ApplePushNotification implements MessageRouter {
  private static final Logger log = Logger.getLogger(ApplePushNotification.class.getName());
  @Inject
  private Provider<ApnsService> apnsService;
  @Inject
  private Provider<PresenceUtil> util;
  @Inject
  private PresenceEndpoint presence;

  @Override
  @ApiMethod(path = "pushToApns")
  public void push(@Named("documentId") String docId, @Named("messageType") String messageType,
      @Named("message") String message) {
    // ping a max of 10 registered devices
    Set<String> subscriptions = presence.listDocumentSubscriptions(docId, Platform.IOS.name());
    if (subscriptions == null || subscriptions.isEmpty()) {
      return;
    }
    PayloadBuilder payloadBuilder =
        APNS.newPayload().customField(MessageType.valueOf(messageType).key(), message);
    log.info("payload length:" + payloadBuilder.length());
    String payload = payloadBuilder.build();
    for (String subscription : subscriptions) {
      String id = subscription;
      String token = util.get().channelTokenFor(id);
      if (token == null) {
        continue;
      }
      apnsService.get().push(token, payload);
    }
    // Map<String, Date> inactiveDevices = service.getInactiveDevices();
    // inactiveDevices.toString();
  }
}
