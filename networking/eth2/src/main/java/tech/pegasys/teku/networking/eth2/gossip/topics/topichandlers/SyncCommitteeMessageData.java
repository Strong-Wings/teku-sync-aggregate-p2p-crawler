package tech.pegasys.teku.networking.eth2.gossip.topics.topichandlers;

import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.infrastructure.ssz.collections.SszBitvector;
import tech.pegasys.teku.infrastructure.ssz.impl.AbstractSszPrimitive;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBit;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SyncCommitteeMessageData {

    private String beaconHeaderRoot;
    private int beaconHeaderSlot;
    private List<Integer> syncAggregateBitlist;
    private int index;
    private String syncAggregateSignature;
    private Integer subIndex;

    public SyncCommitteeMessageData() {
    }

    public SyncCommitteeMessageData(String beaconHeaderRoot,
                                    int beaconHeaderSlot,
                                    List<Integer> syncAggregateBitlist,
                                    int index,
                                    String syncAggregateSignature,
                                    int subIndex) {
        this.beaconHeaderRoot = beaconHeaderRoot;
        this.beaconHeaderSlot = beaconHeaderSlot;
        this.syncAggregateBitlist = syncAggregateBitlist;
        this.index = index;
        this.syncAggregateSignature = syncAggregateSignature;
        this.subIndex = subIndex;
    }

    public SyncCommitteeMessageData(Bytes32 beaconHeaderRoot,
                                    UInt64 beaconHeaderSlot,
                                    SszBitvector syncAggregateBitlist,
                                    UInt64 index,
                                    BLSSignature syncAggregateSignature,
                                    UInt64 subIndex) {
        this.beaconHeaderRoot = beaconHeaderRoot.toString();
        this.beaconHeaderSlot = beaconHeaderSlot.intValue();
        if (subIndex != null) {
            this.subIndex = subIndex.intValue();
        }
        if (syncAggregateBitlist != null) {
            this.syncAggregateBitlist = new ArrayList<>();
            for (int i = 0; i < syncAggregateBitlist.size() * 4; i++) {
                this.syncAggregateBitlist.add(0);
            }
            for (int i = 0; i < syncAggregateBitlist.size(); i++) {
                this.syncAggregateBitlist.set(i + syncAggregateBitlist.size() * this.subIndex,
                        syncAggregateBitlist.getBit(i) ? 1 : 0);
            }
        }
        this.index = index.intValue();
        this.syncAggregateSignature = syncAggregateSignature.toString();
    }

    public String getSyncAggregateSignature() {
        return syncAggregateSignature;
    }

    public int getBeaconHeaderSlot() {
        return beaconHeaderSlot;
    }

    public List<Integer> getSyncAggregateBitlist() {
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
        return String.format("root: %s, slot: %s, bitlist: %s, signature: %s, index: %s, subIndex: %s",
                this.beaconHeaderRoot,
                this.beaconHeaderSlot,
                this.syncAggregateBitlist,
                this.syncAggregateSignature,
                this.index,
                this.subIndex);
    }
}