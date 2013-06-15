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
package com.goodow.realtime.server.device;

import com.goodow.realtime.server.RealtimeApisModule;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;
import javax.persistence.EntityManager;

/**
 * A simple Cloud Endpoint that receives notifications from a web client (<server url>/index.html),
 * and broadcasts them to all of the devices that were registered with this application (via
 * DeviceInfoEndpoint).
 * 
 * In order for this sample to work, you have to populate the API_KEY field with your key for server
 * apps. You can obtain this key from the API console (https://code.google.com/apis/console). You'll
 * first have to create a project and enable the Google Cloud Messaging Service for it, as described
 * in the javadoc for GCMIntentService.java (in your Android project).
 * 
 * After creating the project in the API console, browse to the "API Access" section, and select the
 * option to "Create a New Server Key". The generated key is what you'll enter into the API_KEY
 * field.
 * 
 * See the documentation at http://developers.google.com/eclipse/docs/cloud_endpoints for more
 * information.
 * 
 * NOTE: This endpoint does not use any form of authorization or authentication! If this app is
 * deployed, anyone can access this endpoint! If you'd like to add authentication, take a look at
 * the documentation.
 */
@Api(name = "device", root = RealtimeApisModule.FRONTEND_ROOT)
// NO AUTHENTICATION; OPEN ENDPOINT!
public class GoogleCloudMessaging {
  private static final Logger log = Logger.getLogger(GoogleCloudMessaging.class.getName());
  /*
   * Fill this in with the server key that you've obtained from the API Console
   * (https://code.google.com/apis/console). This is required for using Google Cloud Messaging from
   * your AppEngine application even if you are using a App Engine's local development server.
   */
  private static final String API_KEY = "AIzaSyCzZeDPyKkwhbOm9Qc2hC4fHFEkTLoyw6U";
  private static final String PLATFORM_ANDROID = "android";

  @Inject
  DeviceInfoEndpoint endpoint;

  @Inject
  Provider<EntityManager> em;

  /**
   * This accepts a message and persists it in the AppEngine datastore, it will also broadcast the
   * message to upto 10 registered android devices via Google Cloud Messaging
   * 
   * @param message the entity to be inserted.
   * @return
   * @throws IOException
   */
  @ApiMethod(name = "pushMessageToGcm")
  public void pushMessageToGcm(@Named("message") String message) {
    Sender sender = new Sender(API_KEY);
    // ping a max of 10 registered devices
    CollectionResponse<DeviceInfo> response = endpoint.listDeviceInfo(null, null, PLATFORM_ANDROID);
    Collection<DeviceInfo> items = response.getItems();
    if (items.isEmpty()) {
      return;
    }
    List<String> ids = new ArrayList<String>();
    for (DeviceInfo deviceInfo : items) {
      ids.add(deviceInfo.getDeviceRegistrationID());
    }

    // Trim message if needed.
    if (message.length() > 1000) {
      message = message.substring(0, 1000) + "[...]";
    }
    MulticastResult results = null;
    try {
      Message msg = new Message.Builder().addData("0", message).timeToLive(0).build();
      results = sender.sendNoRetry(msg, ids);
    } catch (IOException e) {
      log.log(Level.SEVERE, "Error when send message to Google Cloud Messaging Service", e);
      return;
    }
    if (results.getFailure() == 0 && results.getCanonicalIds() == 0) {
      return;
    }
    Iterator<DeviceInfo> iterator = items.iterator();
    for (Result result : results.getResults()) {
      DeviceInfo deviceInfo = iterator.next();
      if (result.getMessageId() != null) {
        String canonicalRegId = result.getCanonicalRegistrationId();
        if (canonicalRegId != null) {
          endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
          deviceInfo.setDeviceRegistrationID(canonicalRegId);
          endpoint.insertDeviceInfo(deviceInfo);
        }
      } else {
        String error = result.getErrorCodeName();
        if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
          endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
        } else {
          log.log(Level.WARNING, "Error occur when trying to send a message to android device:"
              + deviceInfo.getDeviceRegistrationID());
        }
      }
    }
  }
}
