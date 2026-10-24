package net.p3pp3rf1y.devclientautomation.regression;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in, read-only diagnostics for the reusable regression cell; never changes isolation policy. */
final class RegressionIsolationTrace {
	private static final Logger LOGGER = LoggerFactory.getLogger(RegressionIsolationTrace.class);
	private static final boolean ENABLED = Boolean.getBoolean("devclientautomation.traceIsolation");
	private static volatile String action;
	private static int events;
	private static final String[] SUPPORT = new String[81];

	private RegressionIsolationTrace() {
	}

	static void init() {
		if (ENABLED) {
			MinecraftForge.EVENT_BUS.addListener(RegressionIsolationTrace::levelTick);
			MinecraftForge.EVENT_BUS.addListener(RegressionIsolationTrace::breakBlock);
			MinecraftForge.EVENT_BUS.addListener(RegressionIsolationTrace::joinEntity);
			LOGGER.info("Isolation trace enabled (max 160 cell events)");
		}
	}

	static void action(String description) {
		if (ENABLED) {
			action = description;
		}
	}

	static boolean enabled() {
		return ENABLED;
	}

	private static boolean cell(BlockPos pos) {
		return pos.getX() >= 252 && pos.getX() <= 260 && pos.getZ() >= 252 && pos.getZ() <= 260 && pos.getY() >= 72 && pos.getY() <= 80;
	}

	private static void levelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD) || action == null
				|| events >= 160 || !level.hasChunkAt(new BlockPos(256, 72, 256))) {
			return;
		}
		for (int x = 252; x <= 260; x++) {
			for (int z = 252; z <= 260; z++) {
				BlockPos pos = new BlockPos(x, 72, z);
				if (!level.hasChunkAt(pos)) {
					continue;
				}
				int index = (x - 252) * 9 + z - 252;
				String state = level.getBlockState(pos).toString();
				String previous = SUPPORT[index];
				SUPPORT[index] = state;
				if (previous != null && previous.equals(Blocks.STONE.defaultBlockState().toString()) && !previous.equals(state) && events++ < 160) {
					LOGGER.warn("Isolation support transition tick={} pos={} {} -> {} action={}", level.getGameTime(), pos, previous, state, action);
				}
			}
		}
	}

	private static void breakBlock(BlockEvent.BreakEvent event) {
		if (action != null && event.getLevel() instanceof ServerLevel level && level.dimension().equals(Level.OVERWORLD) && cell(event.getPos())
				&& events++ < 160) {
			LOGGER.warn("Isolation break pos={} state={} player={} creative={} action={} stack=", event.getPos(), event.getState(), event.getPlayer().getUUID(),
					event.getPlayer().isCreative(), action, new Throwable("Break origin"));
		}
	}

	private static void joinEntity(EntityJoinLevelEvent event) {
		if (action != null && event.getLevel() instanceof ServerLevel level && level.dimension().equals(Level.OVERWORLD)
				&& event.getEntity() instanceof ItemEntity item && cell(item.blockPosition()) && events++ < 160) {
			// Forge warns that this event may precede a FULL chunk: inspect the event and entity only.
			LOGGER.warn("Isolation item join tick={} uuid={} pos={} stack={} loadedFromDisk={} action={} origin=", level.getGameTime(), item.getUUID(),
					item.blockPosition(), item.getItem(), event.loadedFromDisk(), action, new Throwable("Item join origin"));
		}
	}
}
