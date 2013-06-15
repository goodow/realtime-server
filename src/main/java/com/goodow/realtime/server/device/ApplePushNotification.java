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

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.PayloadBuilder;

import java.util.logging.Logger;

import javax.inject.Named;
import javax.servlet.ServletContext;

@Singleton
@Api(name = "device", root = RealtimeApisModule.FRONTEND_ROOT)
public class ApplePushNotification {
  private static final Logger log = Logger.getLogger(ApplePushNotification.class.getName());
  private static final String PLATFORM_IOS = "ios";

  @Inject
  DeviceInfoEndpoint endpoint;
  @Inject
  ServletContext context;
  @Inject
  ApnsService service;
  @Inject
  GoogleCloudMessaging gcm;

  @ApiMethod(name = "pushMessageToApns")
  public void pushMessage(@Named("message") String message) {
    gcm.pushMessageToGcm(message);
    // ping a max of 10 registered devices
    CollectionResponse<DeviceInfo> response = endpoint.listDeviceInfo(null, null, PLATFORM_IOS);
    for (DeviceInfo deviceInfo : response.getItems()) {

      PayloadBuilder payload = APNS.newPayload().customField("0", message);
      log.info("payload length:" + payload.length());

      service.push(deviceInfo.getDeviceRegistrationID(), payload.build());
    }
    // Map<String, Date> inactiveDevices = service.getInactiveDevices();
    // inactiveDevices.toString();
  }
}
