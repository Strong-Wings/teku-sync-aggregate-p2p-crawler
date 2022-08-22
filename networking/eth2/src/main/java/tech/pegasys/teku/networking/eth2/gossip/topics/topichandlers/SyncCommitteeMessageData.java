package tech.pegasys.teku.networking.eth2.gossip.topics.topichandlers;

import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.infrastructure.ssz.collections.SszBitvector;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBit;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import java.util.List;
import java.util.stream.Collectors;

public class SyncCommitteeMessageData {

    private String beaconHeaderRoot;
    private int beaconHeaderSlot;
    private List<String> syncAggregateBitlist;
    private int index;
    private String syncAggregateSignature;

    public SyncCommitteeMessageData() {
    }

    public SyncCommitteeMessageData(String beaconHeaderRoot,
                                    int beaconHeaderSlot,
                                    List<String> syncAggregateBitlist,
                                    int index,
                                    String syncAggregateSignature) {
        this.beaconHeaderRoot = beaconHeaderRoot;
        this.beaconHeaderSlot = beaconHeaderSlot;
        this.syncAggregateBitlist = syncAggregateBitlist;
        this.index = index;
        this.syncAggregateSignature = syncAggregateSignature;
    }

    public SyncCommitteeMessageData(Bytes32 beaconHeaderRoot,
                                    UInt64 beaconHeaderSlot,
                                    SszBitvector syncAggregateBitlist,
                                    UInt64 index,
                                    BLSSignature syncAggregateSignature) {
        this.beaconHeaderRoot = beaconHeaderRoot.toString();
        this.beaconHeaderSlot = beaconHeaderSlot.intValue();
        this.syncAggregateBitlist = syncAggregateBitlist == null ? null
                : syncAggregateBitlist.asList()
                .stream()
                .map(SszBit::toString)
                .collect(Collectors.toList());
        this.index = index.intValue();
        this.syncAggregateSignature = syncAggregateSignature.toString();
    }

    public String getSyncAggregateSignature() {
        return syncAggregateSignature;
    }

    public int getBeaconHeaderSlot() {
        return beaconHeaderSlot;
    }

    public List<String> getSyncAggregateBitlist() {
        return syncAggregateBitlist;
    }

    public String getBeaconHeaderRoot() {
        return beaconHeaderRoot;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return String.format("root: %s, slot: %s, bitlist: %s, signature: %s, index: %s",
                this.beaconHeaderRoot,
                this.beaconHeaderSlot,
                this.syncAggregateBitlist,
                this.syncAggregateSignature,
                this.index);
    }
}