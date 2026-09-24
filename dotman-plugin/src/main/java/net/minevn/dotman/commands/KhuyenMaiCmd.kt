package net.minevn.dotman.commands

import net.minevn.dotman.DotMan
import net.minevn.dotman.config.Language
import net.minevn.dotman.config.PlannedExtras
import net.minevn.dotman.extras.ExtraFormat
import net.minevn.dotman.extras.PlannedExtraEntry
import net.minevn.dotman.utils.DurationFormat
import net.minevn.dotman.utils.Pagination
import net.minevn.dotman.utils.replaceAllPlaceholders
import net.minevn.libs.bukkit.command
import java.time.Instant
import java.time.ZonedDateTime

class KhuyenMaiCmd {
    /**
     * Một dòng trong danh sách khuyến mãi, gộp planned và legacy về một kiểu
     *
     * @param from null với legacy
     * @param to null với lịch tuần vĩnh viễn
     * @param applied true nếu đây là khuyến mãi đang thực sự được áp dụng (tỉ lệ cao nhất trong các
     * khuyến mãi active cùng lúc)
     */
    class Entry(
        val name: String,
        val ratePercent: Int,
        val from: ZonedDateTime?,
        val to: ZonedDateTime?,
        val active: Boolean,
        val repeating: Boolean,
        val applied: Boolean,
    )

    companion object {
        const val PER_PAGE = 3

        fun init() {
            command {
                description("Xem danh sách khuyến mãi")

                tabComplete {
                    if (args.size != 1) {
                        return@tabComplete emptyList()
                    }
                    val main = DotMan.instance
                    val entries = currentEntries(main, ZonedDateTime.now())
                    val maxPage = Pagination(entries, PER_PAGE, 1).maxPage
                    (1..maxPage).map { it.toString() }.filter { it.startsWith(args.last()) }
                }

                action {
                    val main = DotMan.instance
                    val lang = main.language
                    val page = Pagination.parsePage(args.getOrNull(0)) ?: run {
                        sender.sendMessage(lang.khuyenmaiUsage)
                        return@action
                    }
                    val now = ZonedDateTime.now()
                    val entries = currentEntries(main, now)
                    if (entries.isEmpty()) {
                        sender.sendMessage(lang.khuyenmaiEmpty)
                        return@action
                    }
                    val pagination = Pagination(entries, PER_PAGE, page)

                    sender.sendMessage(
                        lang.khuyenmaiHeader
                            .replace("%PAGE%", pagination.page.toString())
                            .replace("%MAX_PAGE%", pagination.maxPage.toString())
                    )
                    pagination.pageItems.forEach { entry ->
                        lang.khuyenmaiEntry.forEach { sender.sendMessage(formatLine(it, entry, now, lang)) }
                    }
                    pagination.sendNav(sender, lang.pagination, "/khuyenmai")
                }

                register(DotMan.instance, "khuyenmai")
            }
        }

        private fun currentEntries(main: DotMan, now: ZonedDateTime): List<Entry> {
            val cfg = main.config
            return buildEntries(main.plannedExtras.getAll(), cfg.extraRate, cfg.extraUntil, main.plannedExtras.legacyName, now)
        }

        /**
         * Danh sách hiển thị: đang diễn ra trước (rate cao -> thấp, cùng rate giữ thứ tự file),
         * rồi sắp diễn ra (bắt đầu sớm -> muộn). Legacy config.yml đứng đầu khi đang được áp dụng.
         * Logic thuần, không đụng Bukkit/DotMan.
         */
        internal fun buildEntries(
            components: List<PlannedExtraEntry>, legacyRate: Double, legacyUntil: Long, legacyName: String,
            now: ZonedDateTime
        ): List<Entry> {
            // Khuyến mãi thực sự được áp dụng khi có nhiều khuyến mãi active cùng lúc, cùng logic với getCurrentExtra
            val appliedComponent = PlannedExtras.pickCurrent(components, now)
            val planned = components
                .mapNotNull { component ->
                    component.schedule.window(now)?.let { window ->
                        Entry(
                            component.name, component.getPercentage(), window.from, window.to,
                            window.active, component.schedule.isRepeating, component === appliedComponent
                        )
                    }
                }
                .sortedWith(
                    compareByDescending<Entry> { it.active }
                        .thenByDescending { if (it.active) it.ratePercent else 0 }
                        .thenBy { if (it.active) null else it.from }
                )

            val legacyApplied = appliedComponent == null
                && legacyRate > 0 && legacyUntil > now.toInstant().toEpochMilli()
            if (!legacyApplied) {
                return planned
            }
            val until = ZonedDateTime.ofInstant(Instant.ofEpochMilli(legacyUntil), now.zone)
            return listOf(Entry(legacyName, (legacyRate * 100).toInt(), null, until, true, false, true)) + planned
        }

        /**
         * Thay placeholder cho một dòng của khuyenmai-entry
         */
        internal fun formatLine(line: String, entry: Entry, now: ZonedDateTime, lang: Language): String {
            val nowMillis = now.toInstant().toEpochMilli()
            // Không có mốc tương ứng (legacy thiếu from, lịch vĩnh viễn thiếu to) thì bỏ trống ghi chú
            val timeNote = if (entry.active) {
                entry.to?.let {
                    lang.khuyenmaiNoteActive.replace(
                        "%DURATION%", DurationFormat.format(it.toInstant().toEpochMilli() - nowMillis, lang)
                    )
                } ?: ""
            } else {
                entry.from?.let {
                    lang.khuyenmaiNoteUpcoming.replace(
                        "%DURATION%", DurationFormat.format(it.toInstant().toEpochMilli() - nowMillis, lang)
                    )
                } ?: ""
            }
            val replacements = mapOf(
                "%STATUS%" to if (entry.active) lang.khuyenmaiStatusActive else lang.khuyenmaiStatusUpcoming,
                "%REPEAT%" to if (entry.repeating) lang.khuyenmaiRepeat else "",
                "%APPLIED_TAG%" to if (entry.applied) lang.khuyenmaiAppliedTag else "",
                "%TIME_NOTE%" to timeNote,
            )
            return replaceAllPlaceholders(
                ExtraFormat.replacePlaceholders(line, entry.name, entry.ratePercent, entry.from, entry.to, lang),
                replacements
            )
        }
    }
}
