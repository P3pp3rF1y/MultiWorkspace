package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.BoundRegressionAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class MovingStorageActionRegistryTest {
	@Test
	void bindsBoundedMovingStorageActionsByRegisteredId() {
		RegressionActionRegistry registry = new RegressionActionRegistry();

		assertBound(registry, "moving.destroy", "{\"ref\":\"primary\"}");
		assertBound(registry, "fixture.moving.restoreDrop", "{\"ref\":\"primary\",\"saveAs\":\"restored\"}");
		assertBound(registry, "fixture.moving.reload", "{\"ref\":\"primary\"}");
		assertBound(registry, "fixture.moving.holdRecoveredInner", "{}");
		assertBound(registry, "moving.setSlot", "{\"item\":\"minecraft:diamond\",\"count\":1,\"ref\":\"primary\"}");
		assertBound(registry, "moving.setUpgrade", "{\"item\":\"minecraft:diamond\",\"ref\":\"primary\"}");
		assertBound(registry, "fixture.moving.setJukeboxDisc", "{\"ref\":\"primary\",\"item\":\"minecraft:music_disc_13\"}");
		assertBound(registry, "moving.setPacked", "{\"ref\":\"primary\",\"packed\":true}");
		assertBound(registry, "player.useMovingStorage", "{\"ref\":\"primary\"}");
		assertBound(registry, "assert.movingLinked", "{\"ref\":\"primary\"}");
		assertBound(registry, "menu.captureLinkedRevision", "{\"kind\":\"moving\",\"ref\":\"primary\",\"saveAs\":\"baseline\"}");
		assertBound(registry, "assert.linkedMenuDelta",
				"{\"kind\":\"moving\",\"ref\":\"primary\",\"baseline\":\"baseline\",\"item\":\"minecraft:diamond\",\"count\":1}");
		assertBound(registry, "assert.linkedMenuSettings", "{\"ref\":\"primary\",\"baseline\":\"baseline\"}");
		assertBound(registry, "menu.jukeboxPlayback", "{\"ref\":\"primary\",\"control\":\"play\"}");
		assertBound(registry, "assert.linkedJukeboxPlayback", "{\"ref\":\"primary\",\"baseline\":\"baseline\",\"playing\":true}");
		assertBound(registry, "assert.movingUnlinked", "{\"ref\":\"primary\"}");
		assertBound(registry, "moving.setPresentation", "{\"ref\":\"primary\"}");
		assertBound(registry, "assert.movingPresentation", "{\"primaryRef\":\"primary\",\"secondaryRef\":\"secondary\"}");
		assertBound(registry, "assert.movingMenuPresentation", "{\"ref\":\"primary\",\"role\":\"primary\"}");
		assertBound(registry, "assert.movingForgedRejected", "{\"ref\":\"primary\"}");
		assertBound(registry, "moving.probePickup", "{\"phase\":\"spawn\",\"ref\":\"secondary\",\"primaryRef\":\"primary\"}");
		assertBound(registry, "moving.probePackedPrimary", "{\"phase\":\"paused\",\"primaryRef\":\"primary\",\"secondaryRef\":\"secondary\"}");
		assertBound(registry, "assert.linkedStorageBlock", "{\"count\":7,\"presentation\":true}");
		assertProxyBound(registry, "fixture.create.mountLinkedStorage", "{\"saveAs\":\"mounted\"}");
		assertProxyBound(registry, "fixture.create.setMountedSlot", "{\"ref\":\"mounted\",\"item\":\"minecraft:diamond\",\"count\":1}");
		assertProxyBound(registry, "assert.createMountedLinkedStorage", "{\"ref\":\"mounted\",\"count\":1}");
		assertBound(registry, "fixture.moving.captureNestedLinkedCarrier", "{\"ref\":\"primary\",\"saveAs\":\"nested\"}");
		assertBound(registry, "assert.movingNestedLinkedIdentity", "{\"ref\":\"nested\"}");
		assertBound(registry, "assert.movingStorageCompatibilityProfile", "{}");
		assertProxyBound(registry, "assert.storageTierUpgradeRecipe", "{\"form\":\"normal\",\"recipe\":\"shaped\",\"linkage\":\"primary\"}");
		assertProxyBound(registry, "assert.movingStorageTierUpgradeRecipe", "{\"carrier\":\"boat\",\"recipe\":\"shapeless\",\"linkage\":\"secondary\"}");
	}

	private static void assertBound(RegressionActionRegistry registry, String id, String arguments) {
		assertInstanceOf(BoundRegressionAction.class, registry.require(id).bind(JsonParser.parseString(arguments).getAsJsonObject(), "test action"));
	}

	private static void assertProxyBound(RegressionActionRegistry registry, String id, String arguments) {
		assertSame(registry.require(id), registry.require(id).bind(JsonParser.parseString(arguments).getAsJsonObject(), "test action").action());
	}
}
