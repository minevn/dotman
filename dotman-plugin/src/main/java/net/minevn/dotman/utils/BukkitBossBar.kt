package net.minevn.dotman.utils

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar

/**
 * Vì 1.8 không có BossBar nên phải tạo class này
 */
class BukkitBossBar(title: String, color: String, style: String) : BossBar
by Bukkit.createBossBar(title, BarColor.valueOf(color), BarStyle.valueOf(style))
