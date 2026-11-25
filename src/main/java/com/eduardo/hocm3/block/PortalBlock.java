package com.eduardo.hocm3.block;

import com.eduardo.hocm3.world.ModDimensions;
import com.eduardo.hocm3.world.ModTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

public class PortalBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public PortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    // Só deixa colocar se houver espaço livre em cima
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1
                && level.getBlockState(pos.above()).canBeReplaced(ctx)) {
            return defaultBlockState();
        }
        return null;
    }

    // Depois de colocar a base, cria o topo
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    // Se uma metade sumir, a outra some junto
    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean isLower = half == DoubleBlockHalf.LOWER;
        boolean checkingOtherHalf = dir.getAxis() == Direction.Axis.Y && isLower == (dir == Direction.UP);

        if (checkingOtherHalf) {
            boolean otherHalfOk = neighbor.is(this) && neighbor.getValue(HALF) != half;
            return otherHalfOk ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    // No criativo, evita dropar o item ao quebrar o topo
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, below, Block.getId(belowState));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    // Clique direito = teleporte
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // 1) No cliente, só confirma o clique e para aqui
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isPassenger() || player.isVehicle()) {
            return InteractionResult.PASS;
        }

        // 2) Daqui para baixo roda só no servidor
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        boolean toMod = level.dimension() != ModDimensions.HOCM3_DIMENSION_KEY;

        ResourceKey<Level> target = toMod ? ModDimensions.HOCM3_DIMENSION_KEY : Level.OVERWORLD;
        ServerLevel destination = level.getServer().getLevel(target);
        if (destination == null) {
            return InteractionResult.PASS;
        }

        player.changeDimension(destination, new ModTeleporter(lowerPos, toMod));
        return InteractionResult.SUCCESS;
    }
}