package com.eduardo.hocm3.world;

import com.eduardo.hocm3.block.ModBlocks;
import com.eduardo.hocm3.block.PortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraft.tags.BlockTags;

import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public class ModTeleporter implements ITeleporter {
    private static final int FALLBACK_Y = 100;
    private static final int DIM_PORTAL_X = 0;
    private static final int DIM_PORTAL_Z = 0;
    private static final int SEARCH_STEP = 16;      // distância entre tentativas (1 chunk)
    private static final int SEARCH_MAX_RING = 12;  // até ~190 blocos do ponto inicial


    private final BlockPos sourcePortal;
    private final boolean toModDimension;

    public ModTeleporter(BlockPos sourcePortal, boolean toModDimension) {
        this.sourcePortal = sourcePortal;
        this.toModDimension = toModDimension;
    }

    @Override
    public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                    Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        PortalLinkData data = PortalLinkData.get(destWorld.getServer());
        BlockPos portalPos;

        if (toModDimension) {
            data.setOverworldPortal(sourcePortal);
            portalPos = data.getDimensionPortal();
            if (portalPos == null) {
                portalPos = findPortalSpot(destWorld, DIM_PORTAL_X, DIM_PORTAL_Z);
                data.setDimensionPortal(portalPos);
            }
        } else {
            portalPos = data.getOverworldPortal();
            if (portalPos == null) {
                portalPos = findPortalSpot(destWorld,
                        entity.blockPosition().getX(), entity.blockPosition().getZ());
            }
        }

        ensurePortal(destWorld, portalPos);
        BlockPos standPos = findStandPos(destWorld, portalPos);

        Vec3 spawn = new Vec3(standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5);
        return new PortalInfo(spawn, Vec3.ZERO, entity.getYRot(), entity.getXRot());
    }

    // Gera o chunk e procura de cima para baixo o primeiro bloco sólido
    private BlockPos findPortalSpot(ServerLevel level, int x, int z) {
        for (int ring = 0; ring <= SEARCH_MAX_RING; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    // só percorre a borda do anel atual
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;

                    BlockPos found = findDryGround(level, x + dx * SEARCH_STEP, z + dz * SEARCH_STEP);
                    if (found != null) return found;
                }
            }
        }
        // Nada encontrado: usa altura fixa (ensurePortal cria o chão)
        return new BlockPos(x, FALLBACK_Y, z);
    }

    // Devolve a posição livre acima do chão seco, ou null se a coluna for água/árvore/vazia
    @Nullable
    private BlockPos findDryGround(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);

        int minY = level.getMinBuildHeight();
        for (int y = level.getMaxBuildHeight() - 3; y > minY + 5; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) continue;                              // ainda no ar

            if (!state.getFluidState().isEmpty()) return null;        // água/lava: rejeita
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) return null; // árvore: rejeita
            if (!state.blocksMotion()) continue;                      // grama alta, flores...

            return pos.above();                                       // chão sólido e seco
        }
        return null;
    }

    private void ensurePortal(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(ModBlocks.PORTAL.get())) return;

        ensureFloor(level, pos.below());
        BlockState lower = ModBlocks.PORTAL.get().defaultBlockState()
                .setValue(PortalBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(pos, lower, 3);
        level.setBlock(pos.above(), lower.setValue(PortalBlock.HALF, DoubleBlockHalf.UPPER), 3);
    }

    private BlockPos findStandPos(ServerLevel level, BlockPos portalPos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos p = portalPos.relative(dir);
            if (level.getBlockState(p).isAir()
                    && level.getBlockState(p.above()).isAir()
                    && level.getBlockState(p.below()).blocksMotion()) {
                return p;
            }
        }
        BlockPos p = portalPos.east();
        ensureFloor(level, p.below());
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(p.above(), Blocks.AIR.defaultBlockState(), 3);
        return p;
    }

    private void ensureFloor(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.isAir() || !s.getFluidState().isEmpty()) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        }
    }
}