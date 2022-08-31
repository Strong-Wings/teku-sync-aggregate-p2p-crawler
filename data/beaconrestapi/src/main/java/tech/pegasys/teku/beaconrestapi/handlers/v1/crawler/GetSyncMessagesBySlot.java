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

package tech.pegasys.teku.beaconrestapi.handlers.v1.crawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.pegasys.teku.api.DataProvider;
import tech.pegasys.teku.api.NetworkDataProvider;
import tech.pegasys.teku.api.response.v1.crawler.SyncMessageData;
import tech.pegasys.teku.api.response.v1.crawler.SyncMessageResponse;
import tech.pegasys.teku.beaconrestapi.MigratingEndpointAdapter;
import tech.pegasys.teku.infrastructure.json.types.SerializableTypeDefinition;
import tech.pegasys.teku.infrastructure.restapi.endpoints.EndpointMetadata;
import tech.pegasys.teku.infrastructure.restapi.endpoints.RestApiRequest;

import java.util.List;
import java.util.Objects;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static tech.pegasys.teku.beaconrestapi.BeaconRestApiTypes.SLOT_PARAMETER;
import static tech.pegasys.teku.infrastructure.http.HttpStatusCodes.SC_OK;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.CACHE_NONE;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.RES_INTERNAL_ERROR;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.RES_NOT_FOUND;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.RES_OK;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.SLOT;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.SLOT_PATH_DESCRIPTION;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.TAG_NODE;
import static tech.pegasys.teku.infrastructure.json.types.CoreTypes.RAW_INTEGER_TYPE;
import static tech.pegasys.teku.infrastructure.json.types.CoreTypes.STRING_TYPE;
import static tech.pegasys.teku.infrastructure.json.types.SerializableTypeDefinition.listOf;

public class GetSyncMessagesBySlot extends MigratingEndpointAdapter {

  private static final Logger LOG = LogManager.getLogger();

  public static final String ROUTE = "/eth/v1/crawler/messages/{slot}";

  private final NetworkDataProvider network;

  public GetSyncMessagesBySlot(final DataProvider provider) {
    this(provider.getNetworkDataProvider());
  }

  GetSyncMessagesBySlot(final NetworkDataProvider network) {
    super(
        EndpointMetadata.get(ROUTE)
            .operationId("getSyncCommitteeMessagesBySlot")
            .summary("Get messages")
            .description("Retrieves sync committee messages for the given slot.")
            .pathParam(SLOT_PARAMETER)
            .tags(TAG_NODE)
            .response(SC_OK, "Request successful", SYNC_MESSAGE_RESPONSE_TYPE)
            .withNotFoundResponse()
            .build());
    this.network = network;
  }

  @OpenApi(
      path = ROUTE,
      method = HttpMethod.GET,
      summary = "Get messages",
      tags = {TAG_NODE},
      description = "Retrieves sync committee messages for the given slot.",
      pathParams = {@OpenApiParam(name = SLOT, description = SLOT_PATH_DESCRIPTION)},
      responses = {
        @OpenApiResponse(status = RES_OK, content = @OpenApiContent(from = SyncMessageResponse.class)),
        @OpenApiResponse(status = RES_NOT_FOUND, description = "Slot not found"),
        @OpenApiResponse(status = RES_INTERNAL_ERROR)
      })
  @Override
  public void handle(final Context ctx) throws Exception {
    adapt(ctx);
  }

  @Override
  public void handleRequest(RestApiRequest request) throws JsonProcessingException {
    request.header(Header.CACHE_CONTROL, CACHE_NONE);
    var messagesBySlot = network.getSyncMessagesBySlot(request.getPathParameter(SLOT_PARAMETER));
    LOG.info("Found messages list: {}", messagesBySlot);
    if (messagesBySlot.isEmpty()) {
      request.respondError(SC_NOT_FOUND, "Slot not found");
    } else {
      LOG.info("Return {} messages", messagesBySlot.get().size());
      request.respondOk(new SyncMessagesWithData(messagesBySlot.get()));
    }
  }

  private static final SerializableTypeDefinition<SyncMessageData> SYNC_MESSAGE_DATA_TYPE =
          SerializableTypeDefinition.<SyncMessageData>object()
                  .name("SyncMessageData")
                  .withField("slot", RAW_INTEGER_TYPE, v -> v.slot)
                  .withField("beacon_root", STRING_TYPE, v -> v.beaconRoot)
                  .withField("signature", STRING_TYPE, v -> v.signature)
                  .withField("bitlist", STRING_TYPE, v -> v.bitlist)
                  .withField("validator_index", RAW_INTEGER_TYPE, v -> v.index)
                  .withField("sub_committee_index", STRING_TYPE, v -> v.subIndex)
                  .build();

  private static final SerializableTypeDefinition<SyncMessagesWithData> SYNC_MESSAGE_RESPONSE_TYPE =
          SerializableTypeDefinition.<SyncMessagesWithData>object()
                  .name("SyncMessageResponse")
                  .withField("data", listOf(SYNC_MESSAGE_DATA_TYPE), SyncMessagesWithData::getData)
                  .build();

  private static class SyncMessagesWithData {
    public final List<SyncMessageData> data;

    public SyncMessagesWithData(final List<SyncMessageData> data) {
      this.data = data;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final SyncMessagesWithData that = (SyncMessagesWithData) o;
      return Objects.equals(this.data, that.data);
    }

    public List<SyncMessageData> getData() {
      return this.data;
    }

    @Override
    public int hashCode() {
      return Objects.hash(data);
    }
  }
}
