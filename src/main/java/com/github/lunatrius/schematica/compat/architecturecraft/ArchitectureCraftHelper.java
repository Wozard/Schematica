package com.github.lunatrius.schematica.compat.architecturecraft;

import java.io.DataOutput;
import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameData;

public class ArchitectureCraftHelper {

    private static final String MOD_ID = "ArchitectureCraft";
    private static Boolean loaded = null;

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = Loader.isModLoaded(MOD_ID);
        }
        return loaded;
    }

    public static boolean isShapeBlock(Block block) {
        if (!isLoaded()) return false;
        String name = GameData.getBlockRegistry()
            .getNameForObject(block);
        return "ArchitectureCraft:shape".equals(name) || "ArchitectureCraft:shapeSE".equals(name);
    }

    /**
     * Compare two AC shape item stacks by their shape-defining NBT tags (Shape ID, base material).
     * Both stacks must already pass isItemEqual (same Item + damage).
     */
    public static boolean areShapeItemStacksEqual(ItemStack a, ItemStack b) {
        NBTTagCompound tagA = a.getTagCompound();
        NBTTagCompound tagB = b.getTagCompound();

        if (tagA == null && tagB == null) return true;
        if (tagA == null || tagB == null) return false;

        if (tagA.getInteger("Shape") != tagB.getInteger("Shape")) return false;

        String baseNameA = tagA.getString("BaseName");
        String baseNameB = tagB.getString("BaseName");
        if (!baseNameA.equals(baseNameB)) return false;

        if (tagA.getInteger("BaseData") != tagB.getInteger("BaseData")) return false;

        return true;
    }

    /**
     * Read orientation (side, turn) from a tile entity's NBT.
     * Returns a 2-element byte array: [side, turn].
     */
    public static byte[] getOrientationFromTE(TileEntity te) {
        NBTTagCompound nbt = new NBTTagCompound();
        te.writeToNBT(nbt);
        return new byte[] { nbt.getByte("side"), nbt.getByte("turn") };
    }

    /**
     * Count items in inventory that match the given AC shape item stack by NBT (Shape, BaseName, BaseData).
     */
    public static int countShapeInInventory(IInventory inventory, ItemStack target) {
        int count = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (slot != null && slot.isItemEqual(target) && areShapeItemStacksEqual(target, slot)) {
                count += slot.stackSize;
            }
        }
        return count;
    }

    /**
     * Send a SetOrientation message to the server via ArchitectureCraft's DataChannel.
     * Uses reflection to avoid a compile-time dependency on ArchitectureCraft.
     */
    public static void sendOrientationUpdate(int x, int y, int z, byte side, byte turn) {
        try {
            Class<?> acClass = Class.forName("gcewing.architecture.ArchitectureCraft");
            Object channel = acClass.getField("channel")
                .get(null);
            Method openServer = channel.getClass()
                .getMethod("openServer", String.class);
            Object out = openServer.invoke(channel, "SetOrientation");

            DataOutput dataOut = (DataOutput) out;
            dataOut.writeInt(x);
            dataOut.writeInt(y);
            dataOut.writeInt(z);
            dataOut.writeByte(side);
            dataOut.writeByte(turn);

            out.getClass()
                .getMethod("close")
                .invoke(out);
        } catch (Exception e) {
            Reference.logger.error("Failed to send AC orientation update", e);
        }
    }
}
