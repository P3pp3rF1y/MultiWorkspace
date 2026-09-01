package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import java.util.UUID;

final class BackpackRegressionFixture {
	private BackpackRegressionFixture() {
	}

	static Fixture create(BackpackItem backpackItem, int mainColor, int accentColor) {
		ItemStack backpack = new ItemStack(backpackItem);
		UUID storageUuid = UUID.randomUUID();
		backpack.set(ModCoreDataComponents.STORAGE_UUID, storageUuid);
		BackpackItem.setColors(backpack, mainColor, accentColor);
		backpack.set(ModCoreDataComponents.OPEN_TAB_ID, 1);
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
		wrapper.getInventoryHandler().saveInventory();
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_STARTER_TIER.get()));
		wrapper.getUpgradeHandler().saveInventory();
		return new Fixture(backpack, storageUuid, BackpackItem.getMainColor(backpack), BackpackItem.getAccentColor(backpack));
	}

	static String dataError(ItemStack backpack, BackpackItem backpackItem, Fixture fixture) {
		if (!backpack.is(backpackItem)) {
			return "backpack item changed";
		}
		if (!fixture.storageUuid().equals(backpack.get(ModCoreDataComponents.STORAGE_UUID))) {
			return "storage UUID changed";
		}
		if (BackpackItem.getMainColor(backpack) != fixture.mainColor() || BackpackItem.getAccentColor(backpack) != fixture.accentColor()) {
			return "backpack colors changed";
		}
		if (!Integer.valueOf(1).equals(backpack.get(ModCoreDataComponents.OPEN_TAB_ID))) {
			return "custom component changed";
		}
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		ItemStack contents = wrapper.getInventoryHandler().getStackInSlot(0);
		if (!contents.is(Items.DIAMOND) || contents.getCount() != 5) {
			return "contents changed";
		}
		if (!wrapper.getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_STARTER_TIER.get())) {
			return "upgrade changed";
		}
		return null;
	}

	record Fixture(ItemStack backpack, UUID storageUuid, int mainColor, int accentColor) {
	}
}
