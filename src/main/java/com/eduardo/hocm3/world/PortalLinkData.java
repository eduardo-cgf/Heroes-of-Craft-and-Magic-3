package com.eduardo.hocm3.world;

import com.eduardo.hocm3.Hocm3;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class PortalLinkData extends SavedData {
    private static final String NAME = Hocm3.MODID + "_portal_links";

    private BlockPos overworldPortal;
    private BlockPos dimensionPortal;

    public static PortalLinkData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(PortalLinkData::load, PortalLinkData::new, NAME);
    }

    @Nullable public BlockPos getOverworldPortal() { return overworldPortal; }
    @Nullable public BlockPos getDimensionPortal() { return dimensionPortal; }

    public void setOverworldPortal(BlockPos pos) {
        this.overworldPortal = pos.immutable();
        setDirty();
    }

    public void setDimensionPortal(BlockPos pos) {
        this.dimensionPortal = pos.immutable();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (overworldPortal != null) tag.put("overworld", NbtUtils.writeBlockPos(overworldPortal));
        if (dimensionPortal != null) tag.put("dimension", NbtUtils.writeBlockPos(dimensionPortal));
        return tag;
    }

    public static PortalLinkData load(CompoundTag tag) {
        PortalLinkData data = new PortalLinkData();
        if (tag.contains("overworld")) data.overworldPortal = NbtUtils.readBlockPos(tag.getCompound("overworld"));
        if (tag.contains("dimension")) data.dimensionPortal = NbtUtils.readBlockPos(tag.getCompound("dimension"));
        return data;
    }
}