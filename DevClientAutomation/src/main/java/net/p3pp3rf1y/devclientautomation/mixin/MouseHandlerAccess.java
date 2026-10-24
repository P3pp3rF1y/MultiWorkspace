package net.p3pp3rf1y.devclientautomation.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccess {
	@Accessor("xpos")
	void setVirtualX(double x);

	@Accessor("ypos")
	void setVirtualY(double y);
}
