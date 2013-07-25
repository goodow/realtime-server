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
package com.goodow.realtime.server.rpc;

import com.goodow.realtime.channel.constant.Constants;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utilities for the servlets
 */
// TODO: This is near-empty now, should eliminate altogether, or give it
// a more specific name.
public class RpcUtil {

  public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  public static final String ORIGIN = "origin";

  /**
   * Reads the given HTTP request's input stream into a string.
   * 
   * @param req the HTTP request to be read.
   * @return a string representation of the given HTTP request's body.
   * 
   * @throws IOException if there is a problem reading the body.
   */
  public static String readRequestBody(HttpServletRequest req) throws IOException {
    StringBuilder json = new StringBuilder();
    BufferedReader reader = req.getReader();
    String line;
    while ((line = reader.readLine()) != null) {
      json.append(line);
    }
    return json.toString();
  }

  /**
   * Writes the string to the print writer according to the protocol the client expects.
   * 
   * @param resp
   * @param str must be valid JSON
   * @throws IOException
   */
  static void writeJsonResult(HttpServletRequest req, HttpServletResponse resp, String str)
      throws IOException {
    try {
      assert new JsonParser().parse(str) != null;
    } catch (JsonParseException e) {
      throw new IllegalArgumentException("Bad JSON: " + str, e);
    }
    resp.setContentType("application/json");
    // resp.setHeader("Access-Control-Allow-Origin", req.getHeader(ORIGIN));
    resp.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    resp.setHeader("Access-Control-Expose-Headers", "Content-Length,Content-Type,X-Restart");
    resp.getWriter().print(Constants.XSSI_PREFIX + str);
  }
}