package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegressionValueStoreTest {
	@Test
	void reserveAllRejectsTheWholeBatchWithoutReservingAnEarlierOutput() {
		RegressionValueStore values = new RegressionValueStore();
		values.reserve("existing", UUID.class);

		assertThrows(IllegalStateException.class, () -> values.reserveAll(List.of(new RegressionValueContract.Value("new-output", String.class.getName()),
				new RegressionValueContract.Value("existing", UUID.class.getName()))));
		values.reserve("new-output", String.class);
		values.put("new-output", String.class, "published");

		assertEquals("published", values.require("new-output", String.class));
	}

	@Test
	void reserveRejectsCollisionsBeforeProducerMutationAndChecksPublicationType() {
		RegressionValueStore values = new RegressionValueStore();
		values.reserve("target", UUID.class);

		assertThrows(IllegalStateException.class, () -> values.require("target", UUID.class));
		assertThrows(IllegalStateException.class, () -> values.reserve("target", UUID.class));
		assertThrows(IllegalStateException.class, () -> values.put("target", String.class, "wrong"));
		UUID id = UUID.randomUUID();
		values.put("target", UUID.class, id);
		assertEquals(id, values.require("target", UUID.class));
		assertThrows(IllegalStateException.class, () -> values.put("target", UUID.class, id));
	}

	private record EntityIdentity(UUID id, String dimension) {
	}

	@Test
	void putStoresDifferentTypesWithoutChangingTheFramework() {
		RegressionValueStore values = new RegressionValueStore();
		EntityIdentity entity = new EntityIdentity(UUID.randomUUID(), "minecraft:overworld");
		values.put("target-entity", EntityIdentity.class, entity);
		values.put("expected-count", Integer.class, 3);

		assertEquals(entity, values.require("target-entity", EntityIdentity.class));
		assertEquals(3, values.require("expected-count", Integer.class));
	}

	@Test
	void putRejectsDuplicateBindingsAndInvalidNames() {
		RegressionValueStore values = new RegressionValueStore();
		values.put("target", String.class, "first");

		assertEquals("Regression value target is already bound",
				assertThrows(IllegalStateException.class, () -> values.put("target", String.class, "second")).getMessage());
		assertThrows(IllegalArgumentException.class, () -> values.put("Invalid Name", String.class, "value"));
		assertThrows(IllegalArgumentException.class, () -> values.put("untyped", Object.class, "value"));
	}

	@Test
	void requireReportsMissingAndMismatchedValuesPrecisely() {
		RegressionValueStore values = new RegressionValueStore();
		values.put("target", UUID.class, UUID.randomUUID());

		assertEquals("Regression value unknown has not been bound",
				assertThrows(IllegalStateException.class, () -> values.require("unknown", UUID.class)).getMessage());
		assertEquals("Regression value target has type java.util.UUID; expected java.lang.String",
				assertThrows(IllegalStateException.class, () -> values.require("target", String.class)).getMessage());
		assertThrows(IllegalArgumentException.class, () -> values.require("target", Object.class));
	}

	@Test
	void requireDoesNotLeakValuesBetweenTestScopes() {
		RegressionValueStore conformance = new RegressionValueStore();
		conformance.put("target", UUID.class, UUID.randomUUID());
		RegressionValueStore product = new RegressionValueStore();

		assertThrows(IllegalStateException.class, () -> product.require("target", UUID.class));
		product.put("target", String.class, "product");
		assertEquals("product", product.require("target", String.class));
		assertThrows(IllegalStateException.class, () -> conformance.require("target", String.class));
	}
}
