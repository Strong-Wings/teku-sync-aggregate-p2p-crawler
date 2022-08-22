package tech.pegasys.teku.networking.eth2.gossip.topics.topichandlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

import java.io.IOException;
import java.util.List;

public class SyncMessageDataConverter {

    private static final Logger LOG = LogManager.getLogger();
    public static ObjectMapper objectMapper = new ObjectMapper();

    public static List<SyncCommitteeMessageData> fromBytes(Bytes bytes) {
        try {
            LOG.info("Try to convert from bytes");
            var res = objectMapper.readValue(bytes.toArray(), new TypeReference<List<SyncCommitteeMessageData>>() {});
            LOG.info("Successfully converted from bytes to object");
            return res;
        } catch (IOException e) {
            LOG.error("Failed to convert: {}", e.getMessage());
            return null;
        }
    }

    public static Bytes toBytes(List<SyncCommitteeMessageData> syncCommitteeMessageDataList) throws JsonProcessingException {
        return Bytes.of(
                objectMapper.writeValueAsBytes(syncCommitteeMessageDataList)
        );
    }
}
