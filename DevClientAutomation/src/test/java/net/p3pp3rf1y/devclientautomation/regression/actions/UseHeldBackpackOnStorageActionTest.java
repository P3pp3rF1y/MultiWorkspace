package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class UseHeldBackpackOnStorageActionTest {

	@Test
	void pendingServerWorkDoesNotLatchFailureOrBeginSneakingCleanup() throws ReflectiveOperationException {
		for (String phase : new String[]{"enableSneaking", "snapshotTarget"}) {
			RegressionActionExecution execution = execution();
			set(execution, "enableSneaking", CompletableFuture.completedFuture(null));
			set(execution, "snapshotTarget", new CompletableFuture<Void>());
			set(execution, phase, new CompletableFuture<Void>());
			assertEquals(RegressionActionProgress.WAITING, execution.tick(), phase);
			assertEquals(RegressionActionProgress.WAITING, execution.tick(), phase);
			assertFalse((boolean) get(execution, "terminalFailureLatched"), phase);
			assertNull(get(execution, "disableSneaking"), phase);
			assertEquals("", execution.failure(), phase);
		}
	}

	@Test
	void failedServerWorkStillRestoresSneakingAndRetainsFailure() throws ReflectiveOperationException {
		for (String phase : new String[]{"enableSneaking", "snapshotTarget"}) {
			RegressionActionExecution execution = execution();
			set(execution, "enableSneaking", CompletableFuture.completedFuture(null));
			set(execution, "snapshotTarget", CompletableFuture.completedFuture(null));
			set(execution, "disableSneaking", CompletableFuture.completedFuture(null));
			set(execution, phase, CompletableFuture.failedFuture(new IllegalStateException("server-work-failed")));
			assertEquals(RegressionActionProgress.FAILED, execution.tick(), phase);
			assertEquals("server-work-failed", execution.failure(), phase);
		}
	}

	private RegressionActionExecution execution() throws ReflectiveOperationException {
		RegressionActionExecution execution = new UseHeldBackpackOnStorageAction().start(new RegressionExecutionContext("pending-server-work", new HashMap<>()),
				new UseHeldBackpackOnStorageAction.Arguments());
		// Start after deadline initialization so the future transitions need no live Minecraft player.
		set(execution, "deadline", 100);
		return execution;
	}

	private void set(RegressionActionExecution execution, String name, Object value) throws ReflectiveOperationException {
		Field field = execution.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(execution, value);
	}

	private Object get(RegressionActionExecution execution, String name) throws ReflectiveOperationException {
		Field field = execution.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(execution);
	}
}
