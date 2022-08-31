package tech.pegasys.teku.api.response.v1.crawler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class SyncMessageData {
    @JsonProperty("slot")
    public final int slot;

    @JsonProperty("beacon_root")
    public final String beaconRoot;

    @JsonProperty("signature")
    public final String signature;

    @JsonProperty("bitlist")
    public final String bitlist;

    @JsonProperty("validator_index")
    public final Integer index;

    @JsonProperty("sub_committee_index")
    public final String subIndex;

    @JsonCreator
    public SyncMessageData(@JsonProperty("slot") final int slot,
                           @JsonProperty("beacon_root") final String beaconRoot,
                           @JsonProperty("signature") final String signature,
                           @JsonProperty("bitlist") final String bitlist,
                           @JsonProperty("validator_index") final Integer index,
                           @JsonProperty("sub_committee_index") final String subIndex) {
        this.slot = slot;
        this.beaconRoot = beaconRoot;
        this.signature = signature;
        this.bitlist = bitlist;
        this.index = index;
        this.subIndex = subIndex;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SyncMessageData that = (SyncMessageData) o;
        return this.slot == that.slot
                && Objects.equals(this.beaconRoot, that.beaconRoot)
                && Objects.equals(this.signature, that.signature)
                && Objects.equals(this.bitlist, that.bitlist)
                && Objects.equals(this.index, that.index)
                && Objects.equals(this.subIndex, that.subIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot, beaconRoot, signature, bitlist, index, subIndex);
    }
}
