package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public final class ResetRegionAction extends TypedRegressionAction<ResetRegionAction.Arguments> {
	private static final Set<MinecraftServer> PREPARED_SERVERS = Collections.newSetFromMap(new WeakHashMap<>());
	private static final AtomicLong NEXT_TICKET_OWNER = new AtomicLong();
	private static final int SUPPORT_X = 256;
	private static final int SUPPORT_Y = 72;
	private static final int SUPPORT_Z = 256;
	private static final int NORMALIZATION_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	private static final int TICKET_LIFESPAN_TICKS = 100;

	public record Arguments() {
	}

	public ResetRegionAction() {
		super("fixture.region.reset", "3", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final long ticketOwner = NEXT_TICKET_OWNER.incrementAndGet();
		@Nullable
		private CompletableFuture<Void> future;
		private boolean waitingForEntityChunks;
		private boolean ticketsHeld;

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				waitingForEntityChunks = false;
				future = context.onServer(player -> {
					ServerLevel level = player.serverLevel();
					MinecraftServer server = level.getServer();
					BlockPos support = new BlockPos(SUPPORT_X, SUPPORT_Y, SUPPORT_Z);
					boolean firstPreparation = !PREPARED_SERVERS.contains(server);
					boolean releaseTickets = true;
					try {
						if (!loadResetChunks(level)) {
							waitingForEntityChunks = true;
							releaseTickets = false;
							return;
						}

						if (firstPreparation) {
							normalize(level, support);
						}

						List<String> blockers = new ArrayList<>();
						for (Entity entity : level.getEntities((Entity) null, resetBounds(support), candidate -> !(candidate instanceof Player))) {
							blockers.add("entity " + entity.getType() + " at " + entity.blockPosition());
						}
						for (int x = -4; x <= 4; x++) {
							for (int z = -4; z <= 4; z++) {
								BlockPos pos = support.offset(x, 0, z);
								BlockState state = level.getBlockState(pos);
								if (!state.is(Blocks.STONE)) {
									blockers.add("support block " + pos + " is " + state);
								}
							}
						}
						for (int x = -3; x <= 3; x++) {
							for (int y = 1; y <= 6; y++) {
								for (int z = -3; z <= 3; z++) {
									BlockPos pos = support.offset(x, y, z);
									BlockState state = level.getBlockState(pos);
									if (!state.isAir()) {
										blockers.add("cell block " + pos + " is " + state);
									}
								}
							}
						}
						if (!blockers.isEmpty()) {
							String context = firstPreparation ? " after startup normalization" : " (no blocks or entities changed)";
							throw new IllegalStateException(
									"Reusable regression cell has " + blockers.size() + " blockers" + context + ": " + String.join(", ", blockers));
						}

						context.setSupport(support);
						PREPARED_SERVERS.add(server);
					} finally {
						if (releaseTickets) {
							releaseResetTickets(level);
						}
					}
				});
			}

			RegressionActionProgress progress = await(future);
			if (progress == RegressionActionProgress.PASSED && waitingForEntityChunks) {
				future = null;
				return RegressionActionProgress.WAITING;
			}
			return progress;
		}

		private boolean loadResetChunks(ServerLevel level) {
			boolean firstTicketPass = !ticketsHeld;
			ticketsHeld = true;
			forEachResetChunk((chunkX, chunkZ) -> level.getChunkSource().addRegionTicket(TicketHolder.RESET, new ChunkPos(chunkX, chunkZ), 0, ticketOwner));

			boolean entitiesLoaded = true;
			for (int chunkX = (SUPPORT_X - 4) >> 4; chunkX <= (SUPPORT_X + 4) >> 4; chunkX++) {
				for (int chunkZ = (SUPPORT_Z - 4) >> 4; chunkZ <= (SUPPORT_Z + 4) >> 4; chunkZ++) {
					LevelChunk chunk = level.getChunk(chunkX, chunkZ);
					if (!chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL) || !level.areEntitiesLoaded(ChunkPos.asLong(chunkX, chunkZ))) {
						entitiesLoaded = false;
					}
				}
			}
			return !firstTicketPass && entitiesLoaded;
		}

		private void releaseResetTickets(ServerLevel level) {
			if (!ticketsHeld) {
				return;
			}
			forEachResetChunk((chunkX, chunkZ) -> level.getChunkSource().removeRegionTicket(TicketHolder.RESET, new ChunkPos(chunkX, chunkZ), 0, ticketOwner));
			ticketsHeld = false;
		}

		private static void forEachResetChunk(ResetChunkConsumer consumer) {
			for (int chunkX = (SUPPORT_X - 4) >> 4; chunkX <= (SUPPORT_X + 4) >> 4; chunkX++) {
				for (int chunkZ = (SUPPORT_Z - 4) >> 4; chunkZ <= (SUPPORT_Z + 4) >> 4; chunkZ++) {
					consumer.accept(chunkX, chunkZ);
				}
			}
		}

		private static void normalize(ServerLevel level, BlockPos support) {
			discardEntities(level, support);
			for (int x = -3; x <= 3; x++) {
				for (int y = 1; y <= 6; y++) {
					for (int z = -3; z <= 3; z++) {
						BlockPos pos = support.offset(x, y, z);
						Clearable.tryClear(level.getBlockEntity(pos));
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), NORMALIZATION_FLAGS);
					}
				}
			}
			for (int x = -4; x <= 4; x++) {
				for (int z = -4; z <= 4; z++) {
					BlockPos pos = support.offset(x, 0, z);
					Clearable.tryClear(level.getBlockEntity(pos));
					level.setBlock(pos, Blocks.STONE.defaultBlockState(), NORMALIZATION_FLAGS);
				}
			}
			discardEntities(level, support);
		}

		private static void discardEntities(ServerLevel level, BlockPos support) {
			level.getEntities((Entity) null, resetBounds(support), entity -> !(entity instanceof Player)).forEach(Entity::discard);
		}

		private static AABB resetBounds(BlockPos support) {
			return new AABB(support.offset(-4, 0, -4)).expandTowards(9, 8, 9);
		}

		@FunctionalInterface
		private interface ResetChunkConsumer {
			void accept(int chunkX, int chunkZ);
		}

		private static final class TicketHolder {
			private static final TicketType<Long> RESET = TicketType.create("dev_client_regression_reset", Long::compare, TICKET_LIFESPAN_TICKS);
		}
	}
}
