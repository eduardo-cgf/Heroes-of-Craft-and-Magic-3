package com.eduardo.hocm3.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
    public static final ResourceKey<Level> HOCM3_DIMENSION_KEY = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation("hocm3", "hocm3dimension_overworld"));

    public static final ResourceKey<DimensionType> HOCM3_DIMENSION_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            new ResourceLocation("hocm3", "hocm3dimension_type_overworld"));
}