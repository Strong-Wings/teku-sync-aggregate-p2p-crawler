/*
 * Copyright ConsenSys Software Inc., 2022
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.networking.eth2.gossip.encoding;

import org.apache.tuweni.bytes.Bytes;
import tech.pegasys.teku.infrastructure.crypto.Hash;

abstract class MessageIdCalculator {
  // 4-byte domain for gossip message-id isolation of *invalid* snappy messages
  protected static final Bytes MESSAGE_DOMAIN_INVALID_SNAPPY = Bytes.fromHexString("0x00000000");
  // 4-byte domain for gossip message-id isolation of *valid* snappy messages
  protected static final Bytes MESSAGE_DOMAIN_VALID_SNAPPY = Bytes.fromHexString("0x01000000");

  protected abstract Bytes validMessageIdData(final Bytes uncompressedData);

  protected abstract Bytes invalidMessageIdData();

  protected Bytes hashMessageIdData(final Bytes idData) {
    return Hash.sha256(idData).slice(0, 20);
  }

  public Bytes getValidMessageId(final Bytes uncompressedData) {
    return hashMessageIdData(validMessageIdData(uncompressedData));
  }

  public Bytes getInvalidMessageId() {
    return hashMessageIdData(invalidMessageIdData());
  }
}
