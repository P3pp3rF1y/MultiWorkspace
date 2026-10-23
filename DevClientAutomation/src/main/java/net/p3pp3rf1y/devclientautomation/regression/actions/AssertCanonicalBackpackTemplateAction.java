package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackTemplates;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertCanonicalBackpackTemplateAction extends TypedRegressionAction<AssertCanonicalBackpackTemplateAction.Arguments> {
	public record Arguments(String template, String expectation) {
	}

	public AssertCanonicalBackpackTemplateAction() {
		super("assert.canonicalBackpackTemplate", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("template", "expectation"), description);
		String template = requireString(args, "template", description);
		if (!template.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) {
			throw new IllegalArgumentException("Template must be a namespaced resource location");
		}
		String expectation = optionalString(args, "expectation", "canonical", description);
		if (!Set.of("canonical", "absent").contains(expectation)) {
			throw new IllegalArgumentException("Template expectation must be canonical or absent");
		}
		return new Arguments(template, expectation);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						Optional<CompoundTag> template = BackpackTemplates.getBackpackTemplate(ResourceLocation.parse(arguments.template()));
						if (arguments.expectation().equals("absent")) {
							if (template.isPresent()) {
								throw RegressionActionFailure.assertion("Unsupported native host unexpectedly exported a template");
							}
							return;
						}
						IBackpackWrapper wrapper = BackpackWrapper.fromStack(player.getMainHandItem());
						LinkedStorageEndpointData endpoint = player.getMainHandItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
						if (endpoint == null) {
							throw RegressionActionFailure.assertion("Held Backpack lost its linked endpoint");
						}
						CompoundTag contents = wrapper.getBackpackContents().orElseThrow(
								() -> RegressionActionFailure.assertion("Canonical Backpack contents unavailable from " + wrapper.getClass().getName()));
						if (template.isEmpty() || !template.orElseThrow().getCompound("backpackContents").equals(contents)) {
							throw RegressionActionFailure.assertion("Template did not capture the canonical Backpack root");
						}
						if (BackpackStorage.get().getBackpackContents(endpoint.groupId()).isPresent()) {
							throw RegressionActionFailure.assertion("Group UUID leaked into ordinary BackpackStorage");
						}
					});
				}
				return await(future);
			}
		};
	}
}
