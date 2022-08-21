package tech.pegasys.teku.networking.eth2.gossip.topics.topichandlers;

import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.infrastructure.ssz.collections.SszBitvector;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBit;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import java.util.List;
import java.util.stream.Collectors;

public class SyncCommitteeMessageData {

    private final Bytes32 beaconHeaderRoot;
    private final UInt64 beaconHeaderSlot;
    private final List<String> syncAggregateBitlist;
    private final UInt64 index;
    private final BLSSignature syncAggregateSignature;

    public SyncCommitteeMessageData(Bytes32 beaconHeaderRoot,
                                    UInt64 beaconHeaderSlot,
                                    SszBitvector syncAggregateBitlist,
                                    UInt64 index,
                                    BLSSignature syncAggregateSignature) {
        this.beaconHeaderRoot = beaconHeaderRoot;
        this.beaconHeaderSlot = beaconHeaderSlot;
        this.syncAggregateBitlist = syncAggregateBitlist == null ? null
                : syncAggregateBitlist.asList()
                .stream()
                .map(SszBit::toString)
                .collect(Collectors.toList());
        this.index = index;
        this.syncAggregateSignature = syncAggregateSignature;
    }

    public BLSSignature getSyncAggregateSignature() {
        return syncAggregateSignature;
    }

    public UInt64 getBeaconHeaderSlot() {
        return beaconHeaderSlot;
    }

    public List<String> getSyncAggregateBitlist() {
        return syncAggregateBitlist;
    }

    public Bytes32 getBeaconHeaderRoot() {
        return beaconHeaderRoot;
    }

    public UInt64 getIndex() {
        return index;
    }
}