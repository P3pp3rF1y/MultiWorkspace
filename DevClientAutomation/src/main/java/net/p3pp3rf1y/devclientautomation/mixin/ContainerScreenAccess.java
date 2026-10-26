package net.p3pp3rf1y.devclientautomation.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
	@Accessor("hoveredSlot")
	void setVirtualHoveredSlot(@Nullable Slot slot);

	@Nullable
	@Invoker("getHoveredSlot")
	Slot findVirtualSlot(double x, double y);
}
