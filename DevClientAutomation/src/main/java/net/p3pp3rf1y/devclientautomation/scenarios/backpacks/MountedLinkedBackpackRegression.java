package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssembleRailType;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.LinkedStorageBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpackscreateintegration.backpack.MountedSophisticatedBackpack;
import net.p3pp3rf1y.sophisticatedbackpackscreateintegration.common.MountedBackpackContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;

public final class MountedLinkedBackpackRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);

	private MountedLinkedBackpackRegression() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, MountedLinkedBackpackRegression::run);
	}

	private static String run() {
		Fixture fixture = AutomationRuntime.runOnServer(MountedLinkedBackpackRegression::setup);
		waitForClientEndpoint(fixture);
		AutomationRuntime.runOnServer(player -> {
			Entity entity = player.serverLevel().getEntity(fixture.contraptionEntityId());
			assertTrue(
					entity instanceof AbstractContraptionEntity contraption
							&& contraption.handlePlayerInteraction(player, fixture.localPos(), Direction.UP, InteractionHand.MAIN_HAND),
					"Mounted Backpack interaction did not open its menu");
			assertTrue(player.containerMenu instanceof MountedBackpackContainerMenu menu && menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper,
					"Mounted Backpack menu did not use the linked facade");
			return true;
		});
		AutomationRuntime.runOnServer(player -> {
			BackpackBlockEntity peer = WorldHelper.getBlockEntity(player.serverLevel(), fixture.peerPos(), BackpackBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Linked peer Backpack is missing"));
			IBackpackWrapper wrapper = BackpackLinkedStorageResolver.resolveOrCreate(player.serverLevel(), peer.getBackpackWrapper().getBackpack());
			try {
				wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.EMERALD));
				wrapper.getInventoryHandler().saveInventory();
			} finally {
				if (wrapper instanceof LinkedStorageBackpackWrapper linkedStorageBackpackWrapper) {
					linkedStorageBackpackWrapper.close();
				}
			}
			MountedSophisticatedBackpack mounted = getMountedBackpack(player.serverLevel(), fixture);
			mounted.tick();
			assertTrue(mounted.getStorageWrapper() instanceof LinkedStorageBackpackWrapper, "Mounted linked facade was lost after remote update");
			return true;
		});
		return "{\"ok\":true,\"groupId\":\"" + fixture.groupId() + "\",\"linkInPlace\":true,\"mountedMenu\":true,\"remoteProjection\":true}";
	}

	private static Fixture setup(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos origin = player.blockPosition().relative(player.getDirection(), 6);
		BlockPos peerPos = origin.east(4);
		clear(level, origin);
		player.closeContainer();
		player.getInventory().clearContent();

		ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get());
		ItemStack peer = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		IBackpackWrapper peerWrapper = new BackpackWrapper(peer);
		peerWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
		peerWrapper.getInventoryHandler().saveInventory();
		assertTrue(LinkedStorageService.link(level, player.getUUID(), linker, peer), "Could not create mounted linked Backpack group");
		LinkedStorageEndpointData endpoint = requireEndpoint(peer, "linked peer");
		level.setBlockAndUpdate(peerPos, ModBlocks.DIAMOND_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, Direction.NORTH));
		WorldHelper.getBlockEntity(level, peerPos, BackpackBlockEntity.class).orElseThrow(() -> new IllegalStateException("Linked peer Backpack is missing"))
				.setBackpack(peer);

		BlockPos assemblerPos = origin;
		BlockPos backpackPos = origin.above();
		level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
		level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		CartAssemblerBlock assemblerBlock = (CartAssemblerBlock) ForgeRegistries.BLOCKS
				.getValue(new net.minecraft.resources.ResourceLocation("create", "cart_assembler"));
		level.setBlock(assemblerPos, assemblerBlock.defaultBlockState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
				.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
		level.setBlock(backpackPos, ModBlocks.DIAMOND_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, Direction.NORTH), 3);
		BackpackBlockEntity backpack = WorldHelper.getBlockEntity(level, backpackPos, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Mounted Backpack is missing"));
		backpack.setBackpack(new ItemStack(ModItems.DIAMOND_BACKPACK.get()));

		Minecart cart = new Minecart(level, assemblerPos.getX() + 0.5D, assemblerPos.getY(), assemblerPos.getZ() + 0.5D);
		level.addFreshEntity(cart);
		WorldHelper.getBlockEntity(level, assemblerPos, CartAssemblerBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Cart assembler is missing")).tryAssemble(cart);
		AbstractContraptionEntity contraption = cart.getPassengers().stream().filter(AbstractContraptionEntity.class::isInstance)
				.map(AbstractContraptionEntity.class::cast).findFirst()
				.orElseThrow(() -> new IllegalStateException("Cart assembly did not create a contraption"));
		BlockPos localPos = ContraptionHelper.getMountedItemStorages(contraption).keySet().stream()
				.filter(pos -> ContraptionHelper.getMountedStorage(contraption, pos) instanceof MountedSophisticatedBackpack).findFirst()
				.orElseThrow(() -> new IllegalStateException("Contraption did not contain a mounted Backpack"));
		MountedSophisticatedBackpack mounted = getMountedBackpack(contraption, localPos);
		mounted.initEntityLevelAndPositions(contraption, localPos, level, contraption.position());
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		assertTrue(contraption.handlePlayerInteraction(player, localPos, Direction.UP, InteractionHand.MAIN_HAND), "Ender Linker interaction was not handled");
		assertTrue(endpoint.groupId().equals(requireEndpoint(mounted.getStorageStack(), "mounted Backpack").groupId()),
				"Mounted Backpack did not join the linked group");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return new Fixture(peerPos, contraption.getId(), localPos, endpoint.groupId());
	}

	private static boolean hasClientEndpoint(Fixture fixture) {
		if (!(Minecraft.getInstance().level != null
				&& Minecraft.getInstance().level.getEntity(fixture.contraptionEntityId()) instanceof AbstractContraptionEntity contraption)) {
			return false;
		}
		if (!(ContraptionHelper.getMountedStorage(contraption, fixture.localPos()) instanceof MountedSophisticatedBackpack mounted)) {
			return false;
		}
		LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(mounted.getStorageStack());
		return endpoint != null && fixture.groupId().equals(endpoint.groupId());
	}

	private static void waitForClientEndpoint(Fixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
		do {
			if (AutomationRuntime.runOnClient(() -> hasClientEndpoint(fixture))) {
				return;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while waiting for mounted Backpack synchronization", e);
			}
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Mounted linked Backpack was not synchronized to the client");
	}

	private static MountedSophisticatedBackpack getMountedBackpack(ServerLevel level, Fixture fixture) {
		Entity entity = level.getEntity(fixture.contraptionEntityId());
		if (entity instanceof AbstractContraptionEntity contraption) {
			return getMountedBackpack(contraption, fixture.localPos());
		}
		throw new IllegalStateException("Mounted Backpack contraption is unavailable");
	}

	private static MountedSophisticatedBackpack getMountedBackpack(AbstractContraptionEntity contraption, BlockPos localPos) {
		if (ContraptionHelper.getMountedStorage(contraption, localPos) instanceof MountedSophisticatedBackpack mounted) {
			return mounted;
		}
		throw new IllegalStateException("Contraption does not contain a mounted Backpack");
	}

	private static LinkedStorageEndpointData requireEndpoint(ItemStack stack, String name) {
		LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(stack);
		if (endpoint == null) {
			throw new IllegalStateException(name + " is not a linked storage endpoint");
		}
		return endpoint;
	}

	private static void clear(ServerLevel level, BlockPos origin) {
		level.getEntitiesOfClass(Entity.class, new AABB(origin).inflate(10), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
		for (int x = -2; x <= 8; x++) {
			for (int y = -2; y <= 4; y++) {
				for (int z = -3; z <= 3; z++) {
					level.setBlock(origin.offset(x, y, z), y == -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static void assertTrue(boolean value, String message) {
		if (!value) {
			throw new IllegalStateException(message);
		}
	}

	private record Fixture(BlockPos peerPos, int contraptionEntityId, BlockPos localPos, UUID groupId) {
	}
}
