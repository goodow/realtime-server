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

import com.goodow.realtime.channel.constant.Constants.Params;
import com.goodow.realtime.channel.constant.Constants.Services;
import com.goodow.realtime.server.rpc.RpcUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class PresenceHandler extends HttpServlet {

  @Inject
  private Provider<PresenceEndpoint> presenceEndpoint;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    boolean isConnect;
    if (req.getRequestURI().endsWith(Services.PRESENCE_CONNECT)) {
      isConnect = true;
    } else if (req.getRequestURI().endsWith(Services.PRESENCE_DISCONNECT)) {
      isConnect = false;
    } else {
      throw new IllegalArgumentException(
          "Can't determine the type of channel presence from the path: " + req.getRequestURI());
    }
    String sessionId = req.getParameter(Params.SESSION_ID);
    String json = RpcUtil.readRequestBody(req);
    if (json == null || json.isEmpty()) {
      assert !isConnect;
      presenceEndpoint.get().disconnect(sessionId, null);
      return;
    }
    JsonObject payload = new JsonParser().parse(json).getAsJsonObject();
    JsonArray ids = payload.get(Params.IDS).getAsJsonArray();
    List<String> docIds = new ArrayList<String>(ids.size());
    int i = 0;
    for (JsonElement e : ids) {
      docIds.add(i++, e.getAsString());
    }
    if (isConnect) {
      presenceEndpoint.get().connect(sessionId, docIds);
    } else {
      presenceEndpoint.get().disconnect(sessionId, docIds);
    }
    super.doPost(req, resp);
  }
}
