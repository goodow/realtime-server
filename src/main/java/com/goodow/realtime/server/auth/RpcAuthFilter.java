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
package com.goodow.realtime.server.auth;

import com.goodow.realtime.channel.rpc.Constants.Params;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class RpcAuthFilter implements Filter {
  @Inject
  Provider<AccountContext> context;
  @Inject
  AccountEndpoint accountEndpoint;
  private static final String AUTHORIZATION_KEY = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
    String token = getToken(request);
    AccountInfo account = null;
    if (token != null) {
      account = accountEndpoint.findByToken(token);
    }
    if (account == null) {
      ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    context.get().setAccountInfo(account);
    filterChain.doFilter(request, response);
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {
  }

  private String getToken(ServletRequest request) {
    String token = request.getParameter(Params.ACCESS_TOKEN);
    if (token != null) {
      return token;
    }
    String rawToken = ((HttpServletRequest) request).getHeader(AUTHORIZATION_KEY);
    if (rawToken != null && rawToken.startsWith(BEARER_PREFIX)) {
      token = rawToken.substring(BEARER_PREFIX.length());
    }
    return token;
  }

}
