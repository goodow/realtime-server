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

import com.goodow.realtime.operation.util.Pair;
import com.goodow.realtime.server.device.DeviceEndpoint;
import com.goodow.realtime.server.device.DeviceInfo;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.sf.jsr107cache.Cache;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class PresenceUtil {
  private static final String PRESENCE = "PRE";
  private static final String S2T = "S2T";

  public static final String docIdTopic(String docId, String sid) {
    try {
      return URLEncoder.encode(docId + " " + sid.charAt(0) + PRESENCE, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static final String sidTopic(String sid) {
    return sid + " " + PRESENCE;
  }

  @Inject
  private Provider<DeviceEndpoint> device;

  @Inject
  Cache cache;

  public String channelTokenFor(String sessionId) {
    Pair<String, String> key = Pair.of(S2T, sessionId);
    String existing = (String) cache.get(key);
    if (existing != null) {
      return existing;
    }
    DeviceInfo deviceInfo = device.get().findBySessionId(sessionId);
    if (deviceInfo == null) {
      return null;
    }
    String subId = deviceInfo.getId();
    cache.put(key, subId);
    return subId;
  }
}
