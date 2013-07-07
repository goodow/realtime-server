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

import com.goodow.realtime.model.id.IdGenerator;
import com.goodow.realtime.server.RealtimeApisModule;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.datanucleus.query.JPACursorHelper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.walkaround.util.server.auth.DigestUtils2;
import com.google.walkaround.util.server.auth.DigestUtils2.Secret;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.Query;

@Api(name = "account", version = RealtimeApisModule.DEFAULT_VERSION, defaultVersion = AnnotationBoolean.TRUE, namespace = @ApiNamespace(ownerDomain = "goodow.com", ownerName = "Goodow", packagePath = "api.services"))
public class AccountEndpoint {
  @Inject
  Provider<EntityManager> em;
  @Inject
  Provider<Secret> secret;
  @Inject
  IdGenerator idGenerator;

  @ApiMethod(name = "findByName")
  public AccountInfo findByName(@Named("name") String name) {
    Query query = em.get().createQuery("select from AccountInfo as a where a.name = ?0");
    query.setParameter(0, name);
    return getSingleResult(query);
  }

  @ApiMethod(name = "getAccountInfo")
  public AccountInfo getAccountInfo(@Named("id") String id) {
    return em.get().find(AccountInfo.class, id);
  }

  @ApiMethod(name = "insertAccountInfo")
  public AccountInfo insertAccountInfo(AccountInfo account) {
    if (findByName(account.getName()) != null) {
      throw new EntityExistsException("Object already exists");
    }
    account.setUserId(idGenerator.nextNumbers(21));
    account.setToken(digest(account.getUserId() + " " + account.getToken()));
    em.get().persist(account);
    return account;
  }

  @ApiMethod(name = "listAccountInfo")
  public CollectionResponse<AccountInfo> listAccountInfo(
      @Nullable @Named("cursor") String cursorString, @Nullable @Named("limit") Integer limit) {

    Cursor cursor = null;

    StringBuilder q = new StringBuilder("select from AccountInfo as a");
    Query query = em.get().createQuery(q.toString());
    if (cursorString != null && cursorString != "") {
      cursor = Cursor.fromWebSafeString(cursorString);
      query.setHint(JPACursorHelper.CURSOR_HINT, cursor);
    }

    if (limit != null) {
      query.setFirstResult(0);
      query.setMaxResults(limit);
    }

    @SuppressWarnings("unchecked")
    List<AccountInfo> execute = query.getResultList();
    cursor = JPACursorHelper.getCursor(execute);
    if (cursor != null) {
      cursorString = cursor.toWebSafeString();
    }

    // Tight loop for fetching all entities from datastore and accomodate
    // for lazy fetch.
    for (AccountInfo obj : execute) {
      ;
    }

    return CollectionResponse.<AccountInfo> builder().setItems(execute).setNextPageToken(
        cursorString).build();
  }

  @ApiMethod(name = "login")
  public AccountInfo login(@Named("name") String name, @Named("pwd") String pwd) {
    AccountInfo account = findByName(name);
    if (account == null || !account.getToken().equals(digest(account.getUserId() + " " + pwd))) {
      return null;
    }
    return account;
  }

  @ApiMethod(name = "removeAccountInfo")
  public AccountInfo removeAccountInfo(@Named("id") String id) {
    AccountInfo account = null;
    account = em.get().find(AccountInfo.class, id);
    em.get().remove(account);
    return account;
  }

  @ApiMethod(name = "updateAccountInfo")
  public AccountInfo updateAccountInfo(AccountInfo account) {
    if (!containsAccountInfo(account)) {
      throw new EntityNotFoundException("Object does not exist");
    }
    if (account.getToken().length() != DigestUtils2.SHA1_BLOCK_SIZE * 2) {
      account.setToken(digest(account.getUserId() + " " + account.getToken()));
    }
    em.get().persist(account);
    return account;
  }

  AccountInfo findByToken(String token) {
    Query query = em.get().createQuery("select from AccountInfo as a where a.token = ?0");
    query.setParameter(0, token);
    return getSingleResult(query);
  }

  private boolean containsAccountInfo(AccountInfo account) {
    boolean contains = true;
    AccountInfo item = em.get().find(AccountInfo.class, account.getUserId());
    if (item == null) {
      contains = false;
    }
    return contains;
  }

  private String digest(String pwd) {
    return DigestUtils2.hexHmac(secret.get(), pwd);
  }

  private AccountInfo getSingleResult(Query query) {
    AccountInfo result;
    try {
      result = (AccountInfo) query.getSingleResult();
    } catch (NoResultException e) {
      result = null;
    }
    return result;
  }
}