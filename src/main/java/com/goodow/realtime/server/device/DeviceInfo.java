package com.goodow.realtime.server.device;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * An entity for Android device information.
 * 
 * Its associated endpoint, DeviceEndpoint.java, was directly generated from this class - the Google
 * Plugin for Eclipse allows you to generate endpoints directly from entities!
 * 
 * DeviceEndpoint.java will be used for registering devices with this App Engine application.
 * Registered devices will receive messages broadcast by this application over Google Cloud
 * Messaging (GCM). If you'd like to take a look at the broadcasting code, check out
 * GoogleCloudMessaging.java.
 * 
 * For more information, see http://developers.google.com/eclipse/docs/cloud_endpoints.
 * 
 * NOTE: This DeviceEndpoint.java does not use any form of authorization or authentication! If this
 * app is deployed, anyone can access this endpoint! If you'd like to add authentication, take a
 * look at the documentation.
 */
@Entity
// DeviceEndpoint has NO AUTHENTICATION - it is an OPEN ENDPOINT!
public class DeviceInfo {

  /*
   * The Google Cloud Messaging registration token for the device. This token indicates that the
   * device is able to receive messages sent via GCM.
   */
  @Id
  private String id;

  /*
   * Some identifying information about the device, such as its manufacturer and product name.
   */
  private String information;

  /*
   * Timestamp indicating when this device registered with the application.
   */
  private long timestamp;

  private String sessionId;

  public String getId() {
    return id;
  }

  public String getInformation() {
    return information;
  }

  public String getSessionId() {
    return sessionId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setInformation(String information) {
    this.information = information;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
}
