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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import tech.pegasys.teku.api.response.v1.crawler.ValidatorsData;
import tech.pegasys.teku.api.response.v1.node.Direction;
import tech.pegasys.teku.api.response.v1.node.Peer;
import tech.pegasys.teku.api.response.v1.node.State;
import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.networking.eth2.Eth2P2PNetwork;
import tech.pegasys.teku.networking.eth2.gossip.topics.topichandlers.SyncCommitteeMessageData;
import tech.pegasys.teku.networking.eth2.gossip.topics.topichandlers.SyncMessageDataConverter;
import tech.pegasys.teku.networking.eth2.peers.Eth2Peer;
import tech.pegasys.teku.networking.p2p.peer.NodeId;
import tech.pegasys.teku.spec.datastructures.networking.libp2p.rpc.metadata.MetadataMessage;
import tech.pegasys.teku.storage.store.FileKeyValueStoreFactory;
import tech.pegasys.teku.storage.store.KeyValueStore;

public class NetworkDataProvider {

    private static final Logger LOG = LogManager.getLogger();
    private final Eth2P2PNetwork network;
    private final KeyValueStore<String, Bytes> keyValueStore;

    public NetworkDataProvider(final Eth2P2PNetwork network) {
        this.network = network;
        this.keyValueStore = FileKeyValueStoreFactory.getStore();
    }

    /**
     * get the Ethereum Node Record of the node
     *
     * @return if discovery is in use, returns the Ethereum Node Record (base64).
     */
    public Optional<String> getEnr() {
        return network.getEnr();
    }

    /**
     * Get the current node
     *
     * @return the node id (base58)
     */
    public String getNodeIdAsBase58() {
        return network.getNodeId().toBase58();
    }

    /**
     * Get the number of Peers
     *
     * @return the the number of peers currently connected to the client
     */
    public long getPeerCount() {
        return network.streamPeers().count();
    }

    /**
     * Get the listen port
     *
     * @return the port this client is listening on
     */
    public int getListenPort() {
        return network.getListenPort();
    }

    public List<String> getListeningAddresses() {
        return List.of(network.getNodeAddress());
    }

    public List<String> getDiscoveryAddresses() {
        Optional<String> discoveryAddressOptional = network.getDiscoveryAddress();
        return discoveryAddressOptional.map(List::of).orElseGet(List::of);
    }

    public MetadataMessage getMetadata() {
        return network.getMetadata();
    }

    public List<Peer> getPeers() {
        return network.streamPeers().map(this::toPeer).collect(Collectors.toList());
    }

    public List<Eth2Peer> getEth2Peers() {
        return network.streamPeers().collect(Collectors.toList());
    }

    public List<Eth2Peer> getPeerScores() {
        return network.streamPeers().collect(Collectors.toList());
    }

    public Optional<Peer> getPeerById(final String peerId) {
        final NodeId nodeId = network.parseNodeId(peerId);
        return network.getPeer(nodeId).map(this::toPeer);
    }

    public Optional<Eth2Peer> getEth2PeerById(final String peerId) {
        final NodeId nodeId = network.parseNodeId(peerId);
        return network.getPeer(nodeId);
    }

    private <R> Peer toPeer(final Eth2Peer eth2Peer) {
        final String peerId = eth2Peer.getId().toBase58();
        final String address = eth2Peer.getAddress().toExternalForm();
        final State state = eth2Peer.isConnected() ? State.connected : State.disconnected;
        final Direction direction =
                eth2Peer.connectionInitiatedLocally() ? Direction.outbound : Direction.inbound;

        return new Peer(peerId, null, address, state, direction);
    }

    public Optional<ValidatorsData> getValidatorsBySlot(UInt64 slot) {
        var value = keyValueStore.get(slot.toString());
        if (value.isEmpty()) {
            return Optional.empty();
        }
        var syncCommitteeMessageData = SyncMessageDataConverter.fromBytes(value.get());
        LOG.info("Sync committee message data list: {}", syncCommitteeMessageData);
        if (syncCommitteeMessageData == null) {
            return Optional.empty();
        }

        var beaconRoot = syncCommitteeMessageData.stream()
                .findFirst().
                map(SyncCommitteeMessageData::getBeaconHeaderRoot)
                .orElse(null);

        // todo select without intersections
        var signature = BLS.aggregate(
                syncCommitteeMessageData.stream()
                        .map(SyncCommitteeMessageData::getSyncAggregateSignature)
                        .map(s -> BLSSignature.fromBytesCompressed(Bytes.fromHexString(s)))
                        .collect(Collectors.toList())
        ).toString();
        List<String> bitList = new ArrayList<>();
        syncCommitteeMessageData.stream()
                .map(SyncCommitteeMessageData::getSyncAggregateBitlist)
                .filter(Objects::nonNull)
                .forEach(bl -> {
                    if (bitList.isEmpty()) {
                        bitList.addAll(bl);
                    }
                    for (int i = 0; i < bl.size(); i++) {
                        if (Objects.equals(bl.get(i), "1")) {
                            bitList.set(i, "1");
                        }
                    }
                });
        var validatorsIndicies = syncCommitteeMessageData.stream()
                .map(SyncCommitteeMessageData::getIndex)
                .collect(Collectors.toList());
        var count = validatorsIndicies.size();

        return Optional.of(new ValidatorsData(slot.intValue(),
                beaconRoot,
                signature,
                bitList,
                validatorsIndicies,
                count));
    }
}
