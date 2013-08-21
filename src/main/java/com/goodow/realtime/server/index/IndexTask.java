/*
 * Copyright 2013 Goodow.com
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
package com.goodow.realtime.server.index;

import com.goodow.realtime.server.model.ObjectId;

import com.google.inject.Inject;
import com.google.walkaround.slob.server.PostCommitAction;
import com.google.walkaround.slob.shared.SlobModel.ReadableSlob;

import java.util.logging.Logger;

public class IndexTask implements PostCommitAction {
  @Inject
  private RealtimeIndexer indexer;

  private static final Logger log = Logger.getLogger(IndexTask.class.getName());

  @Override
  public void reliableDelayedPostCommit(final ObjectId slobId) {
    // try {
    // new RetryHelper().run(new RetryHelper.VoidBody() {
    // @Override
    // public void run() throws RetryableFailure, PermanentFailure {
    // try {
    // indexer.index(slobId);
    // } catch (WaveletLockedException e) {
    // log.log(Level.SEVERE, "Post-commit on locked conv wavelet " + slobId, e);
    // }
    // }
    // });
    // } catch (PermanentFailure e) {
    // throw new RuntimeException(e);
    // }
  }

  @Override
  public void unreliableImmediatePostCommit(ObjectId slobId, long resultingVersion,
      ReadableSlob resultingState) {
    // nothing
  }
}
