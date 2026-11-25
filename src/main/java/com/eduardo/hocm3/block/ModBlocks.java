package com.eduardo.hocm3.block;

import com.eduardo.hocm3.Hocm3;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    // Registers
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Hocm3.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Hocm3.MODID);

    // Simple blocks
    public static final RegistryObject<Block> TEST_STONE = registerBlock("test_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)));

    // Portal blocks
    public static final RegistryObject<Block> PORTAL = registerBlock("portal_block",
            () -> new PortalBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)
                    .strength(5.0f, 1200.0f)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 11)));

    // Items

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(toReturn.get(), new Item.Properties()));
        return toReturn;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}