package tech.pegasys.teku.networking.eth2.gossip.topics.topichandlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.infrastructure.ssz.SszData;
import tech.pegasys.teku.infrastructure.ssz.collections.SszBitvector;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.operations.versions.altair.SignedContributionAndProof;
import tech.pegasys.teku.spec.datastructures.operations.versions.altair.SyncCommitteeMessage;
import tech.pegasys.teku.storage.store.FileKeyValueStoreFactory;
import tech.pegasys.teku.storage.store.KeyValueStore;

import java.util.ArrayList;

public class SyncAggregateCrawler<MessageT extends SszData> {

    private static final Logger LOG = LogManager.getLogger();
    private final KeyValueStore<String, Bytes> keyValueStore;

    public SyncAggregateCrawler() {
        this.keyValueStore = FileKeyValueStoreFactory.getStore();
    }

    public void saveMessage(MessageT messageT) {
        if (messageT instanceof SignedContributionAndProof) {
            var signedContributionAndProof = (SignedContributionAndProof) messageT;
            LOG.info("SignedContributionAndProof: {}", signedContributionAndProof);

            var message = signedContributionAndProof.getMessage();
            var slot = message.getContribution().getSlot();
            var beaconRoot = message.getContribution().getBeaconBlockRoot();
            var aggregationBits = message.getContribution().getAggregationBits();
            var index = signedContributionAndProof.getMessage().getAggregatorIndex();
            var signature = signedContributionAndProof.getSignature();
            saveSyncData(slot, beaconRoot, aggregationBits, index, signature);
        } else if (messageT instanceof SyncCommitteeMessage) {
            var syncCommitteeMessage = (SyncCommitteeMessage) messageT;
            LOG.info("SyncCommitteeMessage: {}", syncCommitteeMessage);

            var slot = syncCommitteeMessage.getSlot();
            var beaconRoot = syncCommitteeMessage.getBeaconBlockRoot();
            var index = syncCommitteeMessage.getValidatorIndex();
            var signature = syncCommitteeMessage.getSignature();
            saveSyncData(slot, beaconRoot,null, index, signature);
        }
    }

    private void saveSyncData(UInt64 slot, Bytes32 beaconRoot, SszBitvector aggregationBits, UInt64 index, BLSSignature signature) {
        var syncCommitteeMessageData = new SyncCommitteeMessageData(beaconRoot, slot, aggregationBits, index, signature);
        try {
            addNewValue(slot, syncCommitteeMessageData);
            LOG.info("Successfully saved {} in db", syncCommitteeMessageData);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to update KV store with value {} {}", syncCommitteeMessageData, e);
        }
    }

    private void addNewValue(UInt64 slot, SyncCommitteeMessageData syncCommitteeMessageData) throws JsonProcessingException {
        var key = slot.toString();
        var existingValues = keyValueStore.get(key)
                .map(SyncMessageDataConverter::fromBytes)
                .orElse(new ArrayList<>());
        existingValues.add(syncCommitteeMessageData);
        keyValueStore.put(slot.toString(), SyncMessageDataConverter.toBytes(existingValues));
        LOG.info("New values for slot {}: {}", slot, Strings.join(existingValues, ';'));
    }
}
