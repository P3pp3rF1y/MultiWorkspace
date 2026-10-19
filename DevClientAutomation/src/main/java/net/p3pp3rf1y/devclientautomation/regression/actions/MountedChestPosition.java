package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

final class MountedChestPosition {
	private MountedChestPosition() {
	}

	static BlockPos normalize(AbstractContraptionEntity entity, BlockPos localPos) {
		StructureTemplate.StructureBlockInfo blockInfo = entity.getContraption().getBlocks().get(localPos);
		if (blockInfo == null) {
			throw new IllegalStateException("Create contraption has no block at mounted storage position " + localPos);
		}
		BlockState state = blockInfo.state();
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) == ChestType.LEFT) {
			BlockPos mainPos = localPos.relative(ChestBlock.getConnectedDirection(state));
			if (!entity.getContraption().getBlocks().containsKey(mainPos)) {
				throw new IllegalStateException("Create contraption has no main double-chest half at " + mainPos);
			}
			return mainPos;
		}
		return localPos;
	}
}
