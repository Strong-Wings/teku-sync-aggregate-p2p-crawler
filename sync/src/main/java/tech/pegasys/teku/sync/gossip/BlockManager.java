/*
 * Copyright 2019 ConsenSys AG.
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

package tech.pegasys.teku.sync.gossip;

import static tech.pegasys.teku.infrastructure.logging.LogFormatter.formatBlock;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.core.results.BlockImportResult;
import tech.pegasys.teku.core.results.BlockImportResult.FailureReason;
import tech.pegasys.teku.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.collections.LimitedSet;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.networking.eth2.gossip.events.GossipedBlockEvent;
import tech.pegasys.teku.service.serviceutils.Service;
import tech.pegasys.teku.statetransition.blockimport.BlockImportChannel;
import tech.pegasys.teku.statetransition.blockimport.BlockImporter;
import tech.pegasys.teku.statetransition.events.block.ImportedBlockEvent;
import tech.pegasys.teku.statetransition.util.FutureItems;
import tech.pegasys.teku.statetransition.util.PendingPool;
import tech.pegasys.teku.storage.client.RecentChainData;
import tech.pegasys.teku.util.time.channels.SlotEventsChannel;

public class BlockManager extends Service implements SlotEventsChannel, BlockImportChannel {
  private static final Logger LOG = LogManager.getLogger();

  private final EventBus eventBus;
  private final RecentChainData recentChainData;
  private final BlockImporter blockImporter;
  private final PendingPool<SignedBeaconBlock> pendingBlocks;

  private final FutureItems<SignedBeaconBlock> futureBlocks;
  private final RecentBlockFetcher recentBlockFetcher;
  private final Set<Bytes32> invalidBlockRoots = LimitedSet.create(500);

  BlockManager(
      final EventBus eventBus,
      final RecentChainData recentChainData,
      final BlockImporter blockImporter,
      final PendingPool<SignedBeaconBlock> pendingBlocks,
      final FutureItems<SignedBeaconBlock> futureBlocks,
      final RecentBlockFetcher recentBlockFetcher) {
    this.eventBus = eventBus;
    this.recentChainData = recentChainData;
    this.blockImporter = blockImporter;
    this.pendingBlocks = pendingBlocks;
    this.futureBlocks = futureBlocks;
    this.recentBlockFetcher = recentBlockFetcher;
  }

  public static BlockManager create(
      final EventBus eventBus,
      final PendingPool<SignedBeaconBlock> pendingBlocks,
      final FutureItems<SignedBeaconBlock> futureBlocks,
      final RecentBlockFetcher recentBlockFetcher,
      final RecentChainData recentChainData,
      final BlockImporter blockImporter) {
    return new BlockManager(
        eventBus, recentChainData, blockImporter, pendingBlocks, futureBlocks, recentBlockFetcher);
  }

  @Override
  public SafeFuture<?> doStart() {
    this.eventBus.register(this);
    recentBlockFetcher.subscribeBlockFetched(this::importBlockIgnoringResult);
    return recentBlockFetcher.start();
  }

  @Override
  protected SafeFuture<?> doStop() {
    eventBus.unregister(this);
    return recentBlockFetcher.stop();
  }

  @Subscribe
  @SuppressWarnings("unused")
  void onGossipedBlock(GossipedBlockEvent gossipedBlockEvent) {
    importBlockIgnoringResult(gossipedBlockEvent.getBlock());
  }

  @Override
  public SafeFuture<BlockImportResult> importBlock(final SignedBeaconBlock block) {
    LOG.trace("Preparing to import block: {}", () -> formatBlock(block.getSlot(), block.getRoot()));
    return doImportBlock(block);
  }

  @Override
  public void onSlot(final UInt64 slot) {
    pendingBlocks.onSlot(slot);
    futureBlocks.onSlot(slot);
    futureBlocks.prune(slot).forEach(this::importBlockIgnoringResult);
  }

  @Subscribe
  @SuppressWarnings("unused")
  void onBlockImported(ImportedBlockEvent blockImportedEvent) {
    // Check if any pending blocks can now be imported
    final SignedBeaconBlock block = blockImportedEvent.getBlock();
    final Bytes32 blockRoot = block.getRoot();
    pendingBlocks.remove(block);
    final List<SignedBeaconBlock> children = pendingBlocks.getItemsDependingOn(blockRoot, false);
    children.forEach(pendingBlocks::remove);
    children.forEach(this::importBlockIgnoringResult);
  }

  private void importBlockIgnoringResult(final SignedBeaconBlock block) {
    doImportBlock(block).reportExceptions();
  }

  private SafeFuture<BlockImportResult> doImportBlock(final SignedBeaconBlock block) {
    recentBlockFetcher.cancelRecentBlockRequest(block.getRoot());
    if (!shouldImportBlock(block)) {
      return SafeFuture.completedFuture(BlockImportResult.knownBlock(block));
    }

    return blockImporter
        .importBlock(block)
        .thenPeek(
            result -> {
              if (result.isSuccessful()) {
                LOG.trace("Imported block: {}", block);
              } else if (result.getFailureReason() == FailureReason.UNKNOWN_PARENT) {
                // Add to the pending pool so it is triggered once the parent is imported
                pendingBlocks.add(block);
                // Check if the parent was imported while we were trying to import this block and if
                // so, remove from the pendingPool again and process now We must add the block to
                // the pending pool before this check happens to avoid race conditions between
                // performing the check and the parent importing.
                if (recentChainData.containsBlock(block.getParent_root())) {
                  pendingBlocks.remove(block);
                  importBlockIgnoringResult(block);
                }
              } else if (result.getFailureReason() == FailureReason.BLOCK_IS_FROM_FUTURE) {
                futureBlocks.add(block);
              } else {
                LOG.trace(
                    "Unable to import block for reason {}: {}", result.getFailureReason(), block);
                dropInvalidBlock(block);
              }
            });
  }

  private boolean shouldImportBlock(final SignedBeaconBlock block) {
    if (blockIsKnown(block)) {
      return false;
    }
    if (blockIsInvalid(block)) {
      dropInvalidBlock(block);
      return false;
    }
    return true;
  }

  private boolean blockIsKnown(final SignedBeaconBlock block) {
    return pendingBlocks.contains(block)
        || futureBlocks.contains(block)
        || recentChainData.containsBlock(block.getRoot());
  }

  private boolean blockIsInvalid(final SignedBeaconBlock block) {
    return invalidBlockRoots.contains(block.getRoot())
        || invalidBlockRoots.contains(block.getParent_root());
  }

  private void dropInvalidBlock(final SignedBeaconBlock block) {
    final Bytes32 blockRoot = block.getRoot();
    final Set<SignedBeaconBlock> blocksToDrop = new HashSet<>();
    blocksToDrop.add(block);
    blocksToDrop.addAll(pendingBlocks.getItemsDependingOn(blockRoot, true));

    blocksToDrop.forEach(
        blockToDrop -> {
          invalidBlockRoots.add(blockToDrop.getMessage().hash_tree_root());
          pendingBlocks.remove(blockToDrop);
        });
  }
}
