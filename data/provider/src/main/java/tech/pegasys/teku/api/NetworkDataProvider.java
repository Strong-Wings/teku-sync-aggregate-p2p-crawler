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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.identityconnectors.common.CollectionUtil;
import tech.pegasys.teku.api.response.v1.crawler.SyncMessageData;
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

import static tech.pegasys.teku.spec.constants.NetworkConstants.SYNC_COMMITTEE_SUBNET_COUNT;

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

    public Optional<List<SyncMessageData>> getSyncMessagesBySlot(UInt64 slot) {
        var value = keyValueStore.get(slot.toString());
        if (value.isEmpty()) {
            return Optional.empty();
        }
        var syncCommitteeMessageData = SyncMessageDataConverter.fromBytes(value.get());
        LOG.info("Sync committee message data list: {}", syncCommitteeMessageData);
        if (syncCommitteeMessageData == null) {
            return Optional.empty();
        }
        return Optional.of(syncCommitteeMessageData.stream()
                .map(d -> new SyncMessageData(d.getBeaconHeaderSlot(),
                        d.getBeaconHeaderRoot(),
                        d.getSyncAggregateSignature(),
                        d.getSyncAggregateBitlist() != null
                                ? d.getSyncAggregateBitlist().stream().map(String::valueOf).collect(Collectors.joining())
                                : null,
                        d.getIndex(),
                        Optional.ofNullable(d.getSubIndex()).map(String::valueOf).orElse(null)))
                .collect(Collectors.toList()));
    }

    public Optional<ValidatorsData> getValidatorsBySlot(UInt64 slot, List<Integer> allValidators) {
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

        syncCommitteeMessageData = syncCommitteeMessageData.stream()
                .filter(s -> s.getSyncAggregateBitlist() != null)
                .collect(Collectors.toList());
        var selectedSyncCommitteeData = selectBitlistsWithoutIntersections(syncCommitteeMessageData);
        var signature = BLS.aggregate(
                selectedSyncCommitteeData.stream()
                        .map(SyncCommitteeMessageData::getSyncAggregateSignature)
                        .map(s -> BLSSignature.fromBytesCompressed(Bytes.fromHexString(s)))
                        .collect(Collectors.toList())
        ).toString();
        var finalBitList = createFinalBitlist(selectedSyncCommitteeData);
        List<Integer> validatorsIndicies = new ArrayList<>();
        if (!CollectionUtil.isEmpty(allValidators)) {
            for (var bitIndex = 0; bitIndex < finalBitList.size(); bitIndex++) {
                if (finalBitList.get(bitIndex) == 1) {
                    validatorsIndicies.add(allValidators.get(bitIndex));
                }
            }
        } else {
            validatorsIndicies = syncCommitteeMessageData.stream()
                    .map(SyncCommitteeMessageData::getIndex)
                    .collect(Collectors.toList());
        }
        var count = (int) finalBitList.stream().filter(b -> b == 1).count();

        return Optional.of(new ValidatorsData(slot.intValue(),
                beaconRoot,
                signature,
                finalBitList.stream().map(String::valueOf).collect(Collectors.joining()),
                validatorsIndicies,
                count));
    }

    public static List<SyncCommitteeMessageData> selectBitlistsWithoutIntersections
            (List<SyncCommitteeMessageData> syncCommitteeMessageDataList) {
        List<SyncCommitteeMessageData> finalSyncCommitteeData = new ArrayList<>();

        for (var subIndex = 0; subIndex < SYNC_COMMITTEE_SUBNET_COUNT; subIndex++) {
            final int finalSubIndex = subIndex;
            var bitlists = syncCommitteeMessageDataList.stream()
                    .filter(s -> s.getSubIndex() ==  finalSubIndex)
                    .map(SyncCommitteeMessageData::getSyncAggregateBitlist)
                    .collect(Collectors.toList());

            var bitlistsWithOneBit = bitlists.stream()
                    .filter(bl -> Collections.frequency(bl, 1) == 1)
                    .collect(Collectors.toMap(bl -> bl.indexOf(1), Function.identity(), (b1, b2) -> b1));
            var otherBitlists = bitlists.stream()
                    .filter(bl -> Collections.frequency(bl, 1) > 1)
                    .collect(Collectors.toList());

            List<List<Integer>> selectedBitlists = new ArrayList<>();
            var maxCount = -1;
            for (var mainBitlist : otherBitlists) {
                List<List<Integer>> currentSelectedBitlists = new ArrayList<>();
                currentSelectedBitlists.add(mainBitlist);
                var tmpMainBitlist = new ArrayList<>(mainBitlist);
                for (var bitlistWithOneBitEntry : bitlistsWithOneBit.entrySet()) {
                    if (tmpMainBitlist.get(bitlistWithOneBitEntry.getKey()) == 0) {
                        tmpMainBitlist.set(bitlistWithOneBitEntry.getKey(), 1);
                        currentSelectedBitlists.add(bitlistWithOneBitEntry.getValue());
                    }
                }
                var bitsCount = Collections.frequency(tmpMainBitlist, 1);
                if (bitsCount > maxCount) {
                    maxCount = bitsCount;
                    selectedBitlists.clear();
                    selectedBitlists.addAll(currentSelectedBitlists);
                }
            }
            finalSyncCommitteeData.addAll(syncCommitteeMessageDataList.stream()
                    .filter(s -> selectedBitlists.contains(s.getSyncAggregateBitlist()))
                    .collect(Collectors.toList()));
        }
        return finalSyncCommitteeData;
    }

    public static List<Integer> createFinalBitlist(List<SyncCommitteeMessageData> selectedSyncCommitteeData) {
        List<Integer> finalBitList = new ArrayList<>();
        selectedSyncCommitteeData.stream()
                .map(SyncCommitteeMessageData::getSyncAggregateBitlist)
                .filter(Objects::nonNull)
                .forEach(bl -> {
                    if (finalBitList.isEmpty()) {
                        finalBitList.addAll(bl);
                    }
                    for (int i = 0; i < bl.size(); i++) {
                        if (bl.get(i) == 1) {
                            finalBitList.set(i, 1);
                        }
                    }
                });
        return finalBitList;
    }
}
