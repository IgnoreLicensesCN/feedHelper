package com.linearity.feedhelper.client.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.Objects;
import java.util.function.Function;

public class InventoryUtils {
    public static void swapSlotToHand(MinecraftClient mc, int slotNumber, Hand hand)
    {
        //all tries failed
        PlayerEntity player = mc.player;
        if (player == null) return;
        ScreenHandler container = player.currentScreenHandler;

        if (slotNumber != -1 && container == player.playerScreenHandler)
        {
            PlayerInventory inventory = player.getInventory();

            if (hand == Hand.MAIN_HAND)
            {
                int currentHotbarSlot = inventory.getSelectedSlot();

                if (isHotbarSlot(slotNumber))
                {
                    inventory.setSelectedSlot(slotNumber - 36);;
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(inventory.getSelectedSlot()));
                }
                else
                {
                    mc.interactionManager.clickSlot(container.syncId, slotNumber, currentHotbarSlot, SlotActionType.SWAP, mc.player);
                }
//                System.out.println("swapped to main hand");
            }
            else if (hand == Hand.OFF_HAND)
            {
                mc.interactionManager.clickSlot(container.syncId, slotNumber, 40, SlotActionType.SWAP, mc.player);
//                if (player.getOffHandStack() != ItemStack.EMPTY){
//                    System.out.println("swapped to offHand"
//                            + container.syncId
//                            + String.valueOf(slotNumber)
//                            + 40 + SlotActionType.SWAP
//                            + mc.player);
                    return;
//                }
//                System.out.println("swapped to off hand failed");
            }
        }
    }

    public static void swapSlotToOffhand(MinecraftClient mc, int slotNumber)
    {
        swapSlotToHand(mc, slotNumber, Hand.OFF_HAND);
    }

    private static boolean isHotbarSlot(Slot slot)
    {
        return slot.getIndex() >= 36 && slot.getIndex() <= 44;
    }

    private static boolean isHotbarSlot(int slotNumber)
    {
        return slotNumber >= 36 && slotNumber <= 44;
    }

    public static int findSlotWithItem(ScreenHandler container, ItemStack stackReference, boolean allowHotbar, boolean reverse)
    {
        final int startSlot = reverse ? container.slots.size() - 1 : 0;
        final int endSlot = reverse ? -1 : container.slots.size();
        final int increment = reverse ? -1 : 1;
        final boolean isPlayerInv = container instanceof PlayerScreenHandler;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment)
        {
            Slot slot = container.slots.get(slotNum);

            if ((!isPlayerInv || isRegularInventorySlot(slot.id, false)) &&
                    (allowHotbar || !isHotbarSlot(slot)) &&
                    areStacksEqualIgnoreDurability(slot.getStack(), stackReference))
            {
                return slot.id;
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

        if (ref.isDamageable() && ref.isDamaged())
        {
            ref.setDamage(0);
        }
        if (check.isDamageable() && check.isDamaged())
        {
            check.setDamage(0);
        }

        return ItemStack.areItemsAndComponentsEqual(ref, check);
    }

    public static boolean equipToHandIf(Function<ItemStack,Boolean> ifToEquip, MinecraftClient client,Hand hand){
        if (client == null) return false;
        var player = client.player;
        if (player == null) return false;

        for (ItemStack stack:player.currentScreenHandler.getStacks()){
            if (Objects.equals(ifToEquip.apply(stack),true)) {
                int slot = InventoryUtils.findSlotWithItem(player.currentScreenHandler,stack,true,false);
                InventoryUtils.swapSlotToHand(client, slot, hand);
                return true;
            }
        }
        return false;
    }
}
