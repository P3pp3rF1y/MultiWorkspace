package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceLifecycleActionBindingTest {
	@Test
	void resourceCapabilitiesBindSourceArgumentsAndRejectInvalidInputs() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionAction cache = registry.require("capability.storageResourceCache");
		RegressionAction transfer = registry.require("capability.transferStorageResource");
		RegressionAction resources = registry.require("assert.storageResources");
		assertEquals(new StorageResourceCacheAction.Arguments("capture", "probe", 0, false),
				((BoundRegressionAction<?>) RegressionActionTestSupport.bind(cache, "{\"operation\":\"capture\",\"ref\":\"probe\"}")).arguments());
		assertEquals(new StorageResourceCacheAction.Arguments("assert", "probe", 2, true), ((BoundRegressionAction<?>) RegressionActionTestSupport.bind(cache,
				"{\"operation\":\"assert\",\"ref\":\"probe\",\"invalidations\":2,\"replaced\":true}")).arguments());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(cache, "{\"operation\":\"inspect\",\"ref\":\"probe\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(cache, "{\"operation\":\"capture\",\"ref\":\"probe\",\"replaced\":1}"));
		assertEquals(new TransferStorageResourceAction.Arguments("", "fill", 1000, 1000, "water", false, -1),
				((BoundRegressionAction<?>) RegressionActionTestSupport.bind(transfer, "{\"operation\":\"fill\",\"amount\":1000}")).arguments());
		assertEquals(new TransferStorageResourceAction.Arguments("moving", "extract", 200, 0, "lava", true, -1),
				((BoundRegressionAction<?>) RegressionActionTestSupport.bind(transfer,
						"{\"ref\":\"moving\",\"operation\":\"extract\",\"amount\":200,\"expected\":0,\"fluid\":\"lava\",\"simulate\":true}")).arguments());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(transfer, "{\"operation\":\"fill\",\"amount\":0}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(transfer, "{\"operation\":\"fill\",\"amount\":1,\"fluid\":\"oil\"}"));
		assertEquals(new AssertStorageResourcesAction.Arguments("", 0, 0, true),
				((BoundRegressionAction<?>) RegressionActionTestSupport.bind(resources, "{}")).arguments());
		assertEquals(new AssertStorageResourcesAction.Arguments("moving", 1000, 300, false),
				((BoundRegressionAction<?>) RegressionActionTestSupport.bind(resources, "{\"ref\":\"moving\",\"fluid\":1000,\"energy\":300,\"present\":false}"))
						.arguments());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(resources, "{\"fluid\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(resources, "{\"present\":0}"));
		RegressionActionTestSupport.assertMetadata(cache, "capability.storageResourceCache", Set.of("minecraft:client", "sophisticatedstorage"));
		RegressionActionTestSupport.assertMetadata(transfer, "capability.transferStorageResource",
				Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"));
		RegressionActionTestSupport.assertMetadata(resources, "assert.storageResources",
				Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"));
	}

	@Test
	void replacementAndUpgradeArgumentsMatchSource() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionAction replace = registry.require("fixture.linked.replaceRoot");
		RegressionAction upgrade = registry.require("fixture.backpack.setUpgrade");
		assertSame(replace, RegressionActionTestSupport.bind(replace, "{}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(replace, "{\"extra\":1}"));
		assertEquals(new SetHeldBackpackUpgradeAction.Arguments(0, "sophisticatedbackpacks:tank_upgrade", 0, true),
				((BoundRegressionAction<?>) RegressionActionTestSupport.bind(upgrade, "{\"item\":\"sophisticatedbackpacks:tank_upgrade\"}")).arguments());
		assertEquals(new SetHeldBackpackUpgradeAction.Arguments(2, "sophisticatedbackpacks:battery_upgrade", 35, false),
				((BoundRegressionAction<?>) RegressionActionTestSupport.bind(upgrade,
						"{\"item\":\"sophisticatedbackpacks:battery_upgrade\",\"slot\":2,\"inventorySlot\":35,\"enabled\":false}")).arguments());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(upgrade, "{\"item\":\"x\",\"inventorySlot\":36}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(upgrade, "{\"item\":\"x\",\"enabled\":1}"));
		RegressionActionTestSupport.assertMetadata(replace, "fixture.linked.replaceRoot", Set.of("minecraft:client", "sophisticatedcore"));
		RegressionActionTestSupport.assertMetadata(upgrade, "fixture.backpack.setUpgrade", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Test
	void controllerReloadAndSharedGroupBindStrictly() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionAction reload = registry.require("fixture.controller.reload");
		RegressionAction shared = registry.require("assert.controllerSharedGroup");
		assertEquals(new ReloadFixtureControllerAction.Arguments(1), ((BoundRegressionAction<?>) RegressionActionTestSupport.bind(reload, "{}")).arguments());
		assertEquals(new ReloadFixtureControllerAction.Arguments(0),
				((BoundRegressionAction<?>) RegressionActionTestSupport.bind(reload, "{\"offsetX\":0}")).arguments());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(reload, "{\"offsetX\":-1}"));
		assertEquals(new AssertControllerSharedGroupAction.Arguments(2, 27, 0),
				((BoundRegressionAction<?>) RegressionActionTestSupport.bind(shared, "{}")).arguments());
		assertEquals(new AssertControllerSharedGroupAction.Arguments(3, 54, 1),
				((BoundRegressionAction<?>) RegressionActionTestSupport.bind(shared, "{\"members\":3,\"slots\":54,\"offsetZ\":1}")).arguments());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(shared, "{\"members\":0}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(shared, "{\"extra\":true}"));
		RegressionActionTestSupport.assertMetadata(reload, "fixture.controller.reload", Set.of("minecraft:client", "sophisticatedstorage"));
		RegressionActionTestSupport.assertMetadata(shared, "assert.controllerSharedGroup", Set.of("minecraft:client", "sophisticatedstorage"));
	}
}
