package net.minevn.dotman.utils

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import net.minevn.dotman.config.FileConfig
import net.minevn.dotman.utils.Utils.Companion.color
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.math.ceil
import kotlin.math.max

/**
 * Text của thanh điều hướng phân trang, đọc từ messages.yml
 */
class PaginationStyle(
    val template: String,
    val first: String,
    val prev: String,
    val next: String,
    val last: String,
    val firstDisabled: String,
    val prevDisabled: String,
    val nextDisabled: String,
    val lastDisabled: String,
    val firstHover: String,
    val prevHover: String,
    val nextHover: String,
    val lastHover: String,
) {
    companion object {
        /**
         * Đọc từ FileConfig theo tiền tố key.
         * prefix "pagination" -> pagination-template, pagination-first, pagination-first-disabled, ...
         */
        fun fromConfig(config: FileConfig, prefix: String) = PaginationStyle(
            template = config.get("$prefix-template"),
            first = config.get("$prefix-first"),
            prev = config.get("$prefix-prev"),
            next = config.get("$prefix-next"),
            last = config.get("$prefix-last"),
            firstDisabled = config.get("$prefix-first-disabled"),
            prevDisabled = config.get("$prefix-prev-disabled"),
            nextDisabled = config.get("$prefix-next-disabled"),
            lastDisabled = config.get("$prefix-last-disabled"),
            firstHover = config.get("$prefix-first-hover"),
            prevHover = config.get("$prefix-prev-hover"),
            nextHover = config.get("$prefix-next-hover"),
            lastHover = config.get("$prefix-last-hover"),
        )
    }
}

/**
 * Cắt trang và dựng thanh điều hướng cho lệnh người chơi.
 * Không giữ state, tạo mới mỗi lần gọi lệnh.
 *
 * @param items danh sách đầy đủ
 * @param perPage số mục mỗi trang, phải >= 1
 * @param requestedPage trang yêu cầu, sẽ được clamp về 1..maxPage
 */
class Pagination<T>(items: List<T>, val perPage: Int, requestedPage: Int) {
    init {
        require(perPage >= 1) { "perPage phải >= 1, nhận $perPage" }
    }

    val maxPage: Int = max(1, ceil(items.size / perPage.toDouble()).toInt())
    val page: Int = requestedPage.coerceIn(1, maxPage)
    val pageItems: List<T> = items.drop((page - 1) * perPage).take(perPage)
    val hasPrev: Boolean = page > 1
    val hasNext: Boolean = page < maxPage

    /**
     * Thanh điều hướng cho player.
     *
     * @param command lệnh gốc không có số trang, ví dụ "/khuyenmai"
     */
    fun buildNav(style: PaginationStyle, command: String): Array<BaseComponent> {
        val template = style.template
            .replace("%PAGE%", page.toString())
            .replace("%MAX_PAGE%", maxPage.toString())
        val components = mutableListOf<BaseComponent>()
        var lastIndex = 0

        PLACEHOLDER_REGEX.findAll(template).forEach { match ->
            val text = template.substring(lastIndex, match.range.first)
            if (text.isNotEmpty()) {
                components.add(TextComponent(*TextComponent.fromLegacyText(text.color())))
            }
            components.add(buildButton(match.groupValues[1], style, command))
            lastIndex = match.range.last + 1
        }

        val tail = template.substring(lastIndex)
        if (tail.isNotEmpty()) {
            components.add(TextComponent(*TextComponent.fromLegacyText(tail.color())))
        }

        return components.toTypedArray()
    }

    private fun buildButton(name: String, style: PaginationStyle, command: String): TextComponent {
        val target: Int?
        val text: String
        val disabled: String
        val hover: String
        when (name) {
            "FIRST" -> {
                target = 1.takeIf { hasPrev }
                text = style.first
                disabled = style.firstDisabled
                hover = style.firstHover
            }
            "PREV" -> {
                target = (page - 1).takeIf { hasPrev }
                text = style.prev
                disabled = style.prevDisabled
                hover = style.prevHover
            }
            "NEXT" -> {
                target = (page + 1).takeIf { hasNext }
                text = style.next
                disabled = style.nextDisabled
                hover = style.nextHover
            }
            else -> {
                target = maxPage.takeIf { hasNext }
                text = style.last
                disabled = style.lastDisabled
                hover = style.lastHover
            }
        }

        if (target == null) {
            return TextComponent(*TextComponent.fromLegacyText(disabled.color()))
        }

        return TextComponent(*TextComponent.fromLegacyText(text.color())).apply {
            clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "$command $target")
            hoverEvent = HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                Text(TextComponent.fromLegacyText(hover.replace("%PAGE%", target.toString()).color()))
            )
        }
    }

    /**
     * Dòng điều hướng cho console, không đọc config
     */
    fun buildConsoleNav() = "§eTrang $page/$maxPage"

    /**
     * Gửi nav phù hợp: Player nhận component click được, còn lại nhận text thường
     */
    fun sendNav(sender: CommandSender, style: PaginationStyle, command: String) {
        if (sender is Player) {
            sender.spigot().sendMessage(*buildNav(style, command))
        } else {
            sender.sendMessage(buildConsoleNav())
        }
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("%(FIRST|PREV|NEXT|LAST)%")

        /**
         * Parse số trang từ argument.
         * null/rỗng -> 1; số nguyên >= 1 -> số đó; còn lại -> null (caller gửi usage)
         */
        fun parsePage(arg: String?): Int? {
            if (arg.isNullOrEmpty()) {
                return 1
            }
            return arg.toIntOrNull()?.takeIf { it >= 1 }
        }
    }
}
