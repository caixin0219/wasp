/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wasp.zookeeper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.Abortable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.wasp.DeserializationException;
import org.apache.wasp.protobuf.generated.LoadBalancerProtos;
import org.apache.zookeeper.KeeperException;

import com.google.protobuf.InvalidProtocolBufferException;

public class LoadBalancerTracker extends ZooKeeperNodeTracker {
  private static final Log LOG = LogFactory.getLog(LoadBalancerTracker.class);

  public LoadBalancerTracker(ZooKeeperWatcher watcher, Abortable abortable) {
    super(watcher, watcher.balancerZNode, abortable);
  }

  /**
   * Return true if the balance switch is on, false otherwise
   */
  public boolean isBalancerOn() {
    byte[] upData = super.getData(false);
    try {
      // if data in ZK is null, use default of on.
      return upData == null || parseFrom(upData).getBalancerOn();
    } catch (DeserializationException dex) {
      LOG.error("ZK state for LoadBalancer could not be parsed "
          + Bytes.toStringBinary(upData));
      // return false to be safe.
      return false;
    }
  }

  /**
   * Set the balancer on/off
   * 
   * @param balancerOn
   * @throws KeeperException
   */
  public void setBalancerOn(boolean balancerOn) throws KeeperException {
    byte[] upData = toByteArray(balancerOn);
    try {
      ZKUtil.createAndWatch(watcher, watcher.balancerZNode, upData);
    } catch (KeeperException.NodeExistsException nee) {
      ZKUtil.setData(watcher, watcher.balancerZNode, upData);
    }
    super.nodeDataChanged(watcher.balancerZNode);
  }

  private byte[] toByteArray(boolean isBalancerOn) {
    LoadBalancerProtos.LoadBalancerState.Builder builder = LoadBalancerProtos.LoadBalancerState
        .newBuilder();
    builder.setBalancerOn(isBalancerOn);
    return builder.build().toByteArray();
  }

  private LoadBalancerProtos.LoadBalancerState parseFrom(byte[] pbBytes)
      throws DeserializationException {
    LoadBalancerProtos.LoadBalancerState.Builder builder = LoadBalancerProtos.LoadBalancerState
        .newBuilder();
    try {
      builder.mergeFrom(pbBytes, 0, pbBytes.length);
    } catch (InvalidProtocolBufferException e) {
      throw new DeserializationException(e);
    }
    return builder.build();
  }
}
