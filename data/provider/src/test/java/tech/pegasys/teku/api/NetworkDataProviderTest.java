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

package tech.pegasys.teku.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.pegasys.teku.api.NetworkDataProvider.createFinalBitlist;
import static tech.pegasys.teku.api.NetworkDataProvider.selectBitlistsWithoutIntersections;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.networking.eth2.Eth2P2PNetwork;
import tech.pegasys.teku.networking.eth2.gossip.topics.topichandlers.SyncCommitteeMessageData;
import tech.pegasys.teku.networking.eth2.peers.Eth2Peer;

public class NetworkDataProviderTest {

  @SuppressWarnings("unchecked")
  private final Eth2P2PNetwork p2pNetwork = mock(Eth2P2PNetwork.class);

  @Test
  void getPeerCount_shouldReturnTotalPeers() {
    final NetworkDataProvider network = new NetworkDataProvider(p2pNetwork);
    final Eth2Peer peer1 = mock(Eth2Peer.class);
    final Eth2Peer peer2 = mock(Eth2Peer.class);
    when(p2pNetwork.streamPeers()).thenReturn(Stream.of(peer1, peer2));

    assertThat(network.getPeerCount()).isEqualTo(2);
    verify(p2pNetwork).streamPeers();
  }

  @Test
  void getPeerCount_shouldReturnTotalPeersIfEmpty() {
    final NetworkDataProvider network = new NetworkDataProvider(p2pNetwork);
    when(p2pNetwork.streamPeers()).thenReturn(Stream.of());

    assertThat(network.getPeerCount()).isEqualTo(0);
    verify(p2pNetwork).streamPeers();
  }

  @Test
  void getListeningAddresses_shouldReturnAddressFromNetwork() {
    final NetworkDataProvider network = new NetworkDataProvider(p2pNetwork);
    final String nodeAddress = "/some/libp2p/addr";

    when(p2pNetwork.getNodeAddress()).thenReturn(nodeAddress);

    assertThat(network.getListeningAddresses()).isEqualTo(List.of(nodeAddress));
  }

  @Test
  void giveBitlists_shouldSelectWithMostBits() {

    Function<List<String>, String> processBitlists = bls -> createFinalBitlist(
            selectBitlistsWithoutIntersections(fromString(bls)))
            .stream()
            .map(String::valueOf)
            .collect(Collectors.joining()
            );

    assertThat(processBitlists.apply(
            List.of(
                    "00000001",
                    "11111110",
                    "00111111",
                    "00010000")
            )
    ).isEqualTo("11111111");

    assertThat(processBitlists.apply(
            List.of(
                  "01010101",
                  "10000000",
                  "00100000",
                  "00001000",
                  "00000010",
                  "11111110")
          )
    ).isEqualTo("11111111");
  }

  private static List<SyncCommitteeMessageData> fromString(List<String> bitlists) {
    return bitlists.stream()
            .map(s -> s.chars()
                    .map(Character::getNumericValue)
                    .boxed()
                    .collect(Collectors.toList()))
            .map(bl -> new SyncCommitteeMessageData(null, 1, bl, 1, null, 0))
            .collect(Collectors.toList());
  }
}
