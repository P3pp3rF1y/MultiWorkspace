package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.TankItemStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.TankStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.TankScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.TankContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertTankAction extends TypedRegressionAction<AssertTankAction.Arguments> {
	private static final String OFFSET_ARGUMENT = "offset";
	private static final String PRESENT_KEY = "present";
	private static final String AMOUNT_KEY = "amount";
	private static final String CAPACITY_KEY = "capacity";
	private static final String HEIGHT_KEY = "height";
	private static final String UPGRADES_KEY = "upgrades";
	private static final String TIER_KEY = "tier";
	private static final String TANK_DROPS_KEY = "tankDrops";
	private static final String DROPPED_FLUID_KEY = "droppedFluid";
	private static final String DROPPED_FLUID_TYPE_KEY = "droppedFluidType";
	private static final String DROPPED_HEIGHT_KEY = "droppedHeight";
	private static final String DROPPED_UPGRADES_KEY = "droppedUpgrades";
	private static final String MENU_KEY = "menu";
	private static final String EMISSION_KEY = "emission";
	private static final String LIGHT_KEY = "light";
	private static final String TABS_KEY = "tabs";
	private static final String SWITCHES_KEY = "switches";
	private static final String CURSOR_ITEM_KEY = "cursorItem";
	private static final String CURSOR_COUNT_KEY = "cursorCount";
	private static final String CONTAINER_SLOT_KEY = "containerSlot";
	private static final String CONTAINER_ITEM_KEY = "containerItem";
	private static final String CONTAINER_COUNT_KEY = "containerCount";

	public record Arguments(int x, int y, int z, boolean present, Map<String, Integer> numbers, String tier, boolean menu, String cursorItem, int cursorCount,
			int containerSlot, String containerItem, int containerCount, String droppedFluidType) {
	}

	public AssertTankAction() {
		super("assert.tank", "1", Set.of(CLIENT_CAPABILITY, SophisticatedStorage.MOD_ID));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args,
				Set.of(OFFSET_ARGUMENT, PRESENT_KEY, AMOUNT_KEY, CAPACITY_KEY, HEIGHT_KEY, UPGRADES_KEY, TIER_KEY, TANK_DROPS_KEY, DROPPED_FLUID_KEY,
						DROPPED_HEIGHT_KEY, DROPPED_UPGRADES_KEY, MENU_KEY, EMISSION_KEY, LIGHT_KEY, TABS_KEY, SWITCHES_KEY, CURSOR_ITEM_KEY, CURSOR_COUNT_KEY,
						CONTAINER_SLOT_KEY, CONTAINER_ITEM_KEY, CONTAINER_COUNT_KEY, DROPPED_FLUID_TYPE_KEY),
				description);
		JsonObject position = new JsonObject();
		if (args.has(OFFSET_ARGUMENT))
			position.add(OFFSET_ARGUMENT, args.get(OFFSET_ARGUMENT));
		PlaceHeldBlockAction.Arguments offset = new PlaceHeldBlockAction().parseArguments(position, description);
		Map<String, Integer> numbers = new LinkedHashMap<>();
		for (String name : Set.of(AMOUNT_KEY, CAPACITY_KEY, HEIGHT_KEY, UPGRADES_KEY, TANK_DROPS_KEY, DROPPED_FLUID_KEY, DROPPED_HEIGHT_KEY,
				DROPPED_UPGRADES_KEY, EMISSION_KEY, LIGHT_KEY, TABS_KEY, SWITCHES_KEY)) {
			if (args.has(name))
				numbers.put(name, requireNonNegativeInt(args, name, description));
		}
		int containerSlot = args.has(CONTAINER_SLOT_KEY) ? requireNonNegativeInt(args, CONTAINER_SLOT_KEY, description) : -1;
		if (containerSlot > 3)
			throw new IllegalArgumentException("Tank container slot must be from 0 through 3");
		return new Arguments(offset.x(), offset.y(), offset.z(), optionalBoolean(args, PRESENT_KEY, true, description), Map.copyOf(numbers),
				optionalString(args, TIER_KEY, "", description), optionalBoolean(args, MENU_KEY, false, description),
				optionalString(args, CURSOR_ITEM_KEY, "", description), optionalNonNegativeInt(args, CURSOR_COUNT_KEY, 1, description), containerSlot,
				optionalString(args, CONTAINER_ITEM_KEY, "minecraft:air", description), optionalNonNegativeInt(args, CONTAINER_COUNT_KEY, 1, description),
				optionalString(args, DROPPED_FLUID_TYPE_KEY, "", description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments args) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			private int deadline = -1;
			private boolean matches;
			private final Map<String, Object> actual = new LinkedHashMap<>();
			private final Map<String, Object> clientDrops = new LinkedHashMap<>();

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0)
					deadline = context.minecraft().player.tickCount + 100;
				BlockPos position = context.target().offset(args.x(), args.y(), args.z());
				if (future == null) {
					future = context.onServer(player -> matches = matchesServerState(player, position));
				}
				RegressionActionProgress progress = await(future);
				if (progress != RegressionActionProgress.PASSED)
					return progress;
				future = null;
				boolean clientMatches = matchesClientState(position);
				observe(args, Map.of("server", Map.copyOf(actual), "clientMatches", clientMatches, "clientDrops", Map.copyOf(clientDrops)));
				if (matches && clientMatches)
					return RegressionActionProgress.PASSED;
				return context.minecraft().player.tickCount >= deadline
						? failAssertion("Tank did not converge: " + expectedActual())
						: RegressionActionProgress.WAITING;
			}

			private boolean matchesServerState(ServerPlayer player, BlockPos position) {
				actual.clear();
				BlockEntity blockEntity = player.level().getBlockEntity(position);
				boolean present = blockEntity instanceof TankBlockEntity;
				actual.put(PRESENT_KEY, present);
				boolean serverMatches = present == args.present();
				if (blockEntity instanceof TankBlockEntity tank) {
					recordTankState(player, tank, position);
					if (args.containerSlot() >= 0)
						serverMatches &= matchesStack(containerStack(tank.getStorageWrapper().getContainerInventory(), args.containerSlot()),
								args.containerItem(), args.containerCount());
					if (!args.tier().isEmpty())
						serverMatches &= args.tier().equals(actual.get(TIER_KEY));
				}
				if (!args.droppedFluidType().isEmpty()
						|| args.numbers().keySet().stream().anyMatch(name -> name.equals(TANK_DROPS_KEY) || name.startsWith("dropped")))
					serverMatches &= recordDroppedTanks(player);
				for (Map.Entry<String, Integer> entry : args.numbers().entrySet())
					serverMatches &= entry.getValue().equals(actual.get(entry.getKey()));
				if (!args.cursorItem().isEmpty())
					serverMatches &= matchesStack(player.containerMenu.getCarried(), args.cursorItem(), args.cursorCount());
				return serverMatches;
			}

			private void recordTankState(ServerPlayer player, TankBlockEntity tank, BlockPos position) {
				actual.put(AMOUNT_KEY, tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
				actual.put(CAPACITY_KEY, tank.getStorageWrapper().getCapacity());
				actual.put(HEIGHT_KEY, tank.getHeight());
				actual.put(EMISSION_KEY, tank.getBlockState().getLightEmission(player.level(), position));
				actual.put(LIGHT_KEY, player.level().getBrightness(LightLayer.BLOCK, position));
				actual.put(TIER_KEY, tank.getTier().name().toLowerCase(Locale.ROOT));
				actual.put(UPGRADES_KEY, countUpgrades(tank.getStorageWrapper()));
				if (player.containerMenu instanceof TankContainerMenu menu) {
					actual.put(TABS_KEY, menu.getUpgradeContainers().size());
					int switches = 0;
					for (int slot = 0; slot < tank.getStorageWrapper().getUpgradeHandler().size(); slot++)
						if (menu.canDisableUpgrade(slot))
							switches++;
					actual.put(SWITCHES_KEY, switches);
				}
			}

			private boolean recordDroppedTanks(ServerPlayer player) {
				boolean fluidMatches = true;
				int count = 0;
				int amount = 0;
				int droppedHeight = 0;
				int droppedUpgrades = 0;
				for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, new AABB(context.target()).inflate(6))) {
					if (item.getItem().getItem() instanceof TankBlockItem) {
						count += item.getItem().getCount();
						TankItemStorageWrapper wrapper = new TankItemStorageWrapper(item.getItem());
						amount += wrapper.getTankFluidHandler().getFluidInTank(0).getAmount();
						if (!args.droppedFluidType().isEmpty())
							fluidMatches &= args.droppedFluidType()
									.equals(BuiltInRegistries.FLUID.getKey(wrapper.getTankFluidHandler().getFluidInTank(0).getFluid()).toString());
						droppedHeight += item.getItem().getOrDefault(ModDataComponents.TANK_HEIGHT, 1);
						droppedUpgrades += countUpgrades(wrapper);
					}
				}
				actual.put(TANK_DROPS_KEY, count);
				actual.put(DROPPED_FLUID_KEY, amount);
				actual.put(DROPPED_HEIGHT_KEY, droppedHeight);
				actual.put(DROPPED_UPGRADES_KEY, droppedUpgrades);
				return fluidMatches;
			}

			private boolean matchesClientState(BlockPos position) {
				BlockEntity clientEntity = context.minecraft().level.getBlockEntity(position);
				boolean tankMatches = true;
				if (clientEntity instanceof TankBlockEntity client && args.present())
					tankMatches = matchesClientTank(client, position);
				boolean cursorMatches = args.cursorItem().isEmpty()
						|| matchesStack(context.minecraft().player.containerMenu.getCarried(), args.cursorItem(), args.cursorCount());
				boolean menuMatches = !args.menu() || matchesClientMenu(position);
				boolean controlsMatches = !(args.numbers().containsKey(TABS_KEY) || args.numbers().containsKey(SWITCHES_KEY)) || matchesClientControls();
				boolean dropsMatch = matchesClientDroppedTanks();
				return (clientEntity instanceof TankBlockEntity) == args.present() && tankMatches && cursorMatches && menuMatches && controlsMatches
						&& dropsMatch;
			}

			private boolean matchesClientDroppedTanks() {
				if (args.droppedFluidType().isEmpty())
					return true;
				int count = 0;
				int amount = 0;
				boolean fluidMatches = true;
				for (ItemEntity item : context.minecraft().level.getEntitiesOfClass(ItemEntity.class, new AABB(context.target()).inflate(6))) {
					if (item.getItem().getItem() instanceof TankBlockItem) {
						count += item.getItem().getCount();
						var wrapper = new TankItemStorageWrapper(item.getItem());
						amount += wrapper.getTankFluidHandler().getFluidInTank(0).getAmount();
						String fluid = BuiltInRegistries.FLUID.getKey(wrapper.getTankFluidHandler().getFluidInTank(0).getFluid()).toString();
						clientDrops.put(DROPPED_FLUID_TYPE_KEY, fluid);
						fluidMatches &= args.droppedFluidType().equals(fluid);
					}
				}
				clientDrops.put(TANK_DROPS_KEY, count);
				clientDrops.put(DROPPED_FLUID_KEY, amount);
				return count > 0 && fluidMatches && (!args.numbers().containsKey(TANK_DROPS_KEY) || count == args.numbers().get(TANK_DROPS_KEY))
						&& (!args.numbers().containsKey(DROPPED_FLUID_KEY) || amount == args.numbers().get(DROPPED_FLUID_KEY));
			}

			private boolean matchesClientTank(TankBlockEntity client, BlockPos position) {
				boolean clientMatches = true;
				if (args.numbers().containsKey(EMISSION_KEY))
					clientMatches &= client.getBlockState().getLightEmission(context.minecraft().level, position) == args.numbers().get(EMISSION_KEY);
				if (args.numbers().containsKey(LIGHT_KEY))
					clientMatches &= context.minecraft().level.getBrightness(LightLayer.BLOCK, position) == args.numbers().get(LIGHT_KEY);
				if (args.numbers().containsKey(HEIGHT_KEY))
					clientMatches &= client.getHeight() == args.numbers().get(HEIGHT_KEY);
				if (args.numbers().containsKey(CAPACITY_KEY))
					clientMatches &= client.getStorageWrapper().getCapacity() == args.numbers().get(CAPACITY_KEY);
				if (args.numbers().containsKey(AMOUNT_KEY)) {
					int expected = args.numbers().get(AMOUNT_KEY);
					int amount = client.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount();
					int quantum = Math.max(1, (int) Math.ceil(client.getStorageWrapper().getCapacity() / 256D));
					clientMatches &= expected == 0 ? amount == 0 : amount > 0 && Math.abs((long) amount - expected) < quantum;
				}
				if (!args.tier().isEmpty())
					clientMatches &= client.getTier().name().equalsIgnoreCase(args.tier());
				if (args.containerSlot() >= 0)
					clientMatches &= matchesStack(containerStack(client.getStorageWrapper().getContainerInventory(), args.containerSlot()),
							args.containerItem(), args.containerCount());
				return clientMatches;
			}

			private boolean matchesClientMenu(BlockPos position) {
				if (!(context.minecraft().player.containerMenu instanceof TankContainerMenu menu))
					return false;
				boolean clientMatches = menu.getBlockPosition().filter(position::equals).isPresent();
				if (args.numbers().containsKey(AMOUNT_KEY))
					clientMatches &= menu.getFluid().getAmount() == args.numbers().get(AMOUNT_KEY);
				if (args.numbers().containsKey(CAPACITY_KEY))
					clientMatches &= menu.getCapacity() == args.numbers().get(CAPACITY_KEY);
				return clientMatches;
			}

			private boolean matchesClientControls() {
				if (!(context.minecraft().gui.screen() instanceof TankScreen screen))
					return false;
				boolean clientMatches = true;
				if (args.numbers().containsKey(TABS_KEY))
					clientMatches &= screen.getUpgradeSettingsControl().children().size() == args.numbers().get(TABS_KEY);
				if (args.numbers().containsKey(SWITCHES_KEY))
					clientMatches &= screen.children().stream()
							.filter(child -> child instanceof ToggleButton<?> button && button.getX() == screen.getGuiLeft() - 22)
							.count() == args.numbers().get(SWITCHES_KEY);
				return clientMatches;
			}
		};
	}

	private static ItemStack containerStack(ItemStacksResourceHandler inventory, int slot) {
		return inventory.getResource(slot).toStack(inventory.getAmountAsInt(slot));
	}

	private static int countUpgrades(TankStorageWrapper storage) {
		int count = 0;
		for (int slot = 0; slot < storage.getUpgradeHandler().size(); slot++)
			if (!storage.getUpgradeHandler().getStackInSlot(slot).isEmpty())
				count++;
		return count;
	}

	private static boolean matchesStack(ItemStack stack, String item, int count) {
		return count == 0 ? stack.isEmpty() : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item) && stack.getCount() == count;
	}
}
