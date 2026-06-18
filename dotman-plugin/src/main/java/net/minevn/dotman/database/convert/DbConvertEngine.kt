package net.minevn.dotman.database.convert

enum class DbConvertEngine(val migrationPath: String, val quote: String) {
    H2("h2", "\""),
    MYSQL("mysql", "`"),
    MARIADB("mysql", "`");

    val typeName = name

    fun isSql() = this == MYSQL || this == MARIADB

    fun quote(identifier: String) = quote + identifier.replace(quote, quote + quote) + quote

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.name.equals(value, true) }

        fun validate(source: DbConvertEngine, target: DbConvertEngine, current: DbConvertEngine): String? {
            if (source == target) {
                return "§cDatabase nguồn và đích không được giống nhau."
            }

            if (source != H2 && target != H2) {
                return "§cChỉ hỗ trợ chuyển đổi giữa H2 và MySQL/MariaDB."
            }

            val sqlEngine = if (source.isSql()) {
                source
            } else {
                target
            }

            if (!current.isSql()) {
                return "§cHãy đặt database.engine thành mysql hoặc mariadb và khởi động lại server trước khi chuyển đổi."
            }

            if (sqlEngine != current) {
                return "§cEngine SQL trong lệnh (${sqlEngine}) phải khớp database.engine hiện tại (${current.typeName})."
            }

            return null
        }
    }
}
