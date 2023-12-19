package net.minevn.dotman.utils

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class ItemStackUtils {
    fun getEnchantment(item: ItemStack, type: Enchantment): Int {
        if (!checkNullorAir(item)) return 0
        if (!item.hasItemMeta()) return 0
        val meta = item.itemMeta
        return if (!meta.hasEnchant(type)) {
            0
        } else {
            meta.getEnchantLevel(type)
        }
    }

    fun addGlow(item: ItemStack): ItemStack {
        val meta = item.itemMeta
        meta.addEnchant(Enchantment.DURABILITY, 1, false)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        item.setItemMeta(meta)
        return item
    }

    fun createItemStack(type: Material, name: String?): ItemStack {
        val item = ItemStack(type)
        val meta = item.itemMeta
        meta.displayName = name
        item.setItemMeta(meta)
        return item
    }

    fun setDisplayName(item: ItemStack, displayName: String?): ItemStack {
        val meta = item.itemMeta
        meta.displayName = displayName
        item.setItemMeta(meta)
        return item
    }

    fun setLore(item: ItemStack, lore: List<String?>?): ItemStack {
        val meta = item.itemMeta
        meta.lore = lore
        item.setItemMeta(meta)
        return item
    }

    fun addLore(item: ItemStack, lore: List<String>?): ItemStack {
        val meta = item.itemMeta
        val lore1: MutableList<String> = ArrayList()
        if (meta.hasLore()) lore1.addAll(meta.lore!!)
        lore1.addAll(lore!!)
        meta.lore = lore1
        item.setItemMeta(meta)
        return item
    }

    fun checkNullorAir(item: ItemStack?) = !(item == null || item.type == Material.AIR)

    fun addItemFlag(item: ItemStack, flag: ItemFlag): ItemStack {
        val meta = item.itemMeta
        meta.addItemFlags(flag)
        item.setItemMeta(meta)
        return item
    }
}
