package tech.pegasys.teku.api.response.v1.crawler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class ValidatorsData {
    @JsonProperty("slot")
    public final int slot;

    @JsonProperty("beacon_root")
    public final String beaconRoot;

    @JsonProperty("signature")
    public final String signature;

    @JsonProperty("bitlist")
    public final String bitlist;

    @JsonProperty("validators_indicies")
    public final List<Integer> validatorsIndicies;

    @JsonProperty("count")
    public final int count;

    @JsonCreator
    public ValidatorsData(@JsonProperty("slot") final int slot,
                          @JsonProperty("beacon_root") final String beaconRoot,
                          @JsonProperty("signature") final String signature,
                          @JsonProperty("bitlist") final String bitlist,
                          @JsonProperty("validators_indicies") final List<Integer> validatorsIndicies,
                          @JsonProperty("count") final int count) {
        this.slot = slot;
        this.beaconRoot = beaconRoot;
        this.signature = signature;
        this.bitlist = bitlist;
        this.validatorsIndicies = validatorsIndicies;
        this.count = count;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ValidatorsData that = (ValidatorsData) o;
        return this.slot == that.slot
                && Objects.equals(this.beaconRoot, that.beaconRoot)
                && Objects.equals(this.signature, that.signature)
                && Objects.equals(this.bitlist, that.bitlist)
                && Objects.equals(this.validatorsIndicies, that.validatorsIndicies)
                && this.count == that.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot, beaconRoot, signature, bitlist, validatorsIndicies, count);
    }
}
