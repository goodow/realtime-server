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
package com.goodow.realtime.server.model;

import com.goodow.realtime.DocumentBridge;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.TransformException;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.operation.util.Pair;

import com.google.inject.Inject;
import com.google.walkaround.slob.shared.InvalidSnapshot;
import com.google.walkaround.slob.shared.SlobModel;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonException;
import elemental.json.JsonValue;

public class RealtimeSlobAdapter implements SlobModel {
  class RealtimeSlob implements Slob {
    private final DocumentBridge bridge;

    RealtimeSlob(JsonValue json) {
      this.bridge = new DocumentBridge((JsonArray) json);
    }

    @Override
    public void apply(Delta<String> change) throws DeltaRejected {
      RealtimeOperation op;
      try {
        JsonValue serialized = Json.instance().parse(change.getPayload());
        op =
            transformer.createOperation(change.getSession().userId, change.getSession().sessionId,
                serialized);
      } catch (JsonException e) {
        throw new DeltaRejected("Malformed op: " + change, e);
      }
      try {
        bridge.consume(op);
      } catch (RuntimeException e) {
        throw new DeltaRejected("Invalid op: " + op, e);
      }
    }

    @Nullable
    @Override
    public String snapshot() {
      return bridge.toString();
    }
  }

  private final Transformer<RealtimeOperation> transformer;

  @Inject
  RealtimeSlobAdapter(Transformer<RealtimeOperation> transformer) {
    this.transformer = transformer;
  }

  @Override
  public Slob create(@Nullable String snapshot) throws InvalidSnapshot {
    if (snapshot == null) {
      return new RealtimeSlob(Json.createArray());
    } else {
      try {
        return new RealtimeSlob(Json.instance().parse(snapshot));
      } catch (JsonException e) {
        throw new InvalidSnapshot(e);
      }
    }
  }

  @Override
  public List<String> transform(List<Delta<String>> clientOps, List<Delta<String>> serverOps)
      throws DeltaRejected {
    try {
      Pair<List<RealtimeOperation>, List<RealtimeOperation>> pair =
          transformer.transform(deserializeOps(clientOps), deserializeOps(serverOps));
      List<RealtimeOperation> cOps = pair.first;
      ArrayList<String> toRtn = new ArrayList<String>(cOps.size());
      for (RealtimeOperation op : cOps) {
        toRtn.add(op.toString());
      }
      return toRtn;
    } catch (TransformException e) {
      throw new DeltaRejected(e);
    }
  }

  private List<RealtimeOperation> deserializeOps(List<Delta<String>> changes) throws DeltaRejected {
    List<RealtimeOperation> ops = new ArrayList<RealtimeOperation>();
    for (Delta<String> delta : changes) {
      RealtimeOperation op;
      try {
        JsonValue serialized = Json.instance().parse(delta.getPayload());
        op =
            transformer.createOperation(delta.getSession().userId, delta.getSession().sessionId,
                serialized);
      } catch (JsonException e) {
        throw new DeltaRejected(e);
      }
      ops.add(op);
    }
    return ops;
  }
}