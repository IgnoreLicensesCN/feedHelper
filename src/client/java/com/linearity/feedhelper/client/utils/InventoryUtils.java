package com.linearity.feedhelper.client.utils;

import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class InventoryUtils {
    public static void swapSlotToHand(Minecraft mc, int slotNumber, InteractionHand hand)
    {
        Player player = mc.player;
        if (player == null) return;
        var connection = mc.getConnection();
        if (connection == null) return;
        var gameMode = mc.gameMode;
        if (gameMode == null) return;
        AbstractContainerMenu container = player.containerMenu;

        if (slotNumber != -1 && container == player.inventoryMenu)
        {
            Inventory inventory = player.getInventory();

            if (hand == InteractionHand.MAIN_HAND)
            {
                int currentHotbarSlot = inventory.getSelectedSlot();

                if (isHotbarSlot(slotNumber))
                {
                    inventory.setSelectedSlot(slotNumber - 36);
                    connection.send(new ServerboundSetCarriedItemPacket(inventory.getSelectedSlot()));
                }
                else
                {
                    gameMode.handleInventoryMouseClick(container.containerId, slotNumber, currentHotbarSlot, ClickType.SWAP, mc.player);
                }
//                System.out.println("swapped to main hand");
            }
            else if (hand == InteractionHand.OFF_HAND)
            {
                gameMode.handleInventoryMouseClick(container.containerId, slotNumber, 40, ClickType.SWAP, mc.player);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void swapSlotToOffhand(Minecraft mc, int slotNumber)
    {
        swapSlotToHand(mc, slotNumber, InteractionHand.OFF_HAND);
    }

    private static boolean isHotbarSlot(Slot slot)
    {
        return slot.getContainerSlot() >= 36 && slot.getContainerSlot() <= 44;
    }

    private static boolean isHotbarSlot(int slotNumber)
    {
        return slotNumber >= 36 && slotNumber <= 44;
    }

    public static int findSlotWithItem(AbstractContainerMenu container, ItemStack stackReference, boolean allowHotbar, boolean reverse)
    {
        final int startSlot = reverse ? container.slots.size() - 1 : 0;
        final int endSlot = reverse ? -1 : container.slots.size();
        final int increment = reverse ? -1 : 1;
        final boolean isPlayerInv = container instanceof InventoryMenu;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment)
        {
            Slot slot = container.slots.get(slotNum);

            if ((!isPlayerInv || isRegularInventorySlot(slot.index, false)) &&
                    (allowHotbar || !isHotbarSlot(slot)) &&
                    areStacksEqualIgnoreDurability(slot.getItem(), stackReference))
            {
                return slot.index;
            }
        }

        return -1;
    }

    public static boolean isRegularInventorySlot(int slotNumber, boolean allowOffhand)
    {
        return slotNumber > 8 && (allowOffhand || slotNumber < 45);
    }

    public static boolean areStacksEqualIgnoreDurability(ItemStack stack1, ItemStack stack2)
    {
        ItemStack ref = stack1.copy();
        ItemStack check = stack2.copy();

        // It's a little hacky, but it works.
        ref.setCount(1);
        check.setCount(1);

        if (ref.isDamageableItem() && ref.isDamaged())
        {
            ref.setDamageValue(0);
        }
        if (check.isDamageableItem() && check.isDamaged())
        {
            check.setDamageValue(0);
        }

        return ItemStack.isSameItemSameComponents(ref, check);
    }

    public static boolean equipToHandIf(Function<ItemStack,Boolean> ifToEquip, Minecraft client,InteractionHand hand){
        if (client == null) return false;
        var player = client.player;
        if (player == null) return false;

        for (ItemStack stack:player.containerMenu.getItems()){
            if (Objects.equals(ifToEquip.apply(stack),true)) {
                int slot = InventoryUtils.findSlotWithItem(player.containerMenu,stack,true,false);
                InventoryUtils.swapSlotToHand(client, slot, hand);
                return true;
            }
        }
        return false;
    }
}
