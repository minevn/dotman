package net.minevn.dotman.database.convert

import net.minevn.dotman.DotMan
import net.minevn.dotman.utils.BukkitBossBar
import net.minevn.dotman.utils.Utils.Companion.format
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.dotman.utils.Utils.Companion.runSync
import net.minevn.dotman.utils.Utils.Companion.send
import net.minevn.dotman.utils.Utils.Companion.severe
import net.minevn.dotman.utils.formatFloat
import net.minevn.dotman.utils.getTimeString
import net.minevn.libs.bukkit.color
import net.minevn.libs.bukkit.db.BukkitDBMigrator
import net.minevn.libs.db.pool.DatabasePool
import net.minevn.libs.db.pool.types.H2DBP
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Xử lý chuyển đổi database giữa H2 <-> MySQL/MariaDB
 */
class DbConversionService {
    companion object {
        private val running = AtomicBoolean(false)
        private const val BATCH_SIZE = 500
        private const val SQL_ONLY_CONFIG_HEAD = "dotman_config_head"

        fun isRunning() = running.get()

        /**
         * Bắt đầu chuyển đổi.
         */
        fun start(player: Player, source: DbConvertEngine, target: DbConvertEngine) {
            val current = DbConvertEngine.from(DotMan.instance.config.config.getString("database.engine"))
                ?: DbConvertEngine.H2
            val error = DbConvertEngine.validate(source, target, current)
            if (error != null) {
                player.send(error)
                return
            }

            if (!running.compareAndSet(false, true)) {
                player.send("§cĐang có một quá trình chuyển đổi database khác chạy.")
                return
            }

            val progress = ProgressView(player, source, target)
            runNotSync {
                try {
                    val result = DbConversionService().convert(source, target, progress)
                    progress.complete()
                    player.send("§aĐã chuyển đổi database từ §b${source.typeName} §asang §b${target.typeName}§a thành công.")
                    result.sendTo(player)
                    if (target == DbConvertEngine.H2) {
                        player.send("§ePlugin sẽ được tắt, hãy cập nhật sang database mới rồi restart server.")
                        runSync { Bukkit.getPluginManager().disablePlugin(DotMan.instance) }
                    }
                } catch (e: Exception) {
                    e.severe("DB convert failed: ${source.typeName} -> ${target.typeName}")
                    progress.fail()
                    player.send("§cChuyển đổi database thất bại: ${e.message}")
                    player.send("§cDữ liệu target đã được rollback nếu lỗi xảy ra trong transaction copy.")
                } finally {
                    running.set(false)
                }
            }
        }
    }

    /**
     * Chạy toàn bộ pipeline chuyển đổi.
     *
     * Các thao tác xóa/chèn dữ liệu ở target được bọc trong một transaction. Việc reseed identity của H2 được chạy sau
     * khi transaction commit vì DDL của H2 có thể tự commit transaction hiện tại.
     */
    private fun convert(sourceEngine: DbConvertEngine, targetEngine: DbConvertEngine, progress: ProgressView): ConversionResult {
        val startedAt = System.currentTimeMillis()
        val sourceConnection = openConnection(sourceEngine)
        sourceConnection.use { source ->
            val targetConnection = openConnection(targetEngine)
            targetConnection.use { target ->
                migrate(source.connection, sourceEngine)
                val targetTablesBeforeMigration = loadTables(target.connection).toNormalizedNameSet()
                val latestTargetVersion = migrate(target.connection, targetEngine)
                val createdTables = findCreatedTables(target.connection, targetTablesBeforeMigration)

                source.connection.isReadOnly = true
                source.connection.autoCommit = false
                target.connection.autoCommit = false

                try {
                    val sourceTables = loadTables(source.connection)
                    if (sourceTables.isEmpty()) {
                        throw IllegalStateException("Không tìm thấy bảng DotMan trong database nguồn")
                    }

                    val targetTables = loadTables(target.connection).toTableMap()
                    val tablesToCopy = resolveTablesToCopy(sourceTables, targetTables)
                    val totalRows = countSourceRows(source.connection, sourceEngine, tablesToCopy)
                    progress.start(totalRows)

                    val copiedTargetTables = mutableListOf<TableInfo>()
                    val copiedRowsByTable = mutableMapOf<String, Int>()
                    tablesToCopy.forEach { table ->
                        val copiedRows = copyTable(
                            source.connection,
                            target.connection,
                            sourceEngine,
                            targetEngine,
                            table.source,
                            table.target,
                            progress
                        )
                        copiedTargetTables.add(table.target)
                        copiedRowsByTable[table.target.name] = copiedRows
                    }

                    setMigrationVersion(target.connection, targetEngine, latestTargetVersion)
                    target.connection.commit()
                    if (targetEngine == DbConvertEngine.H2) {
                        target.connection.autoCommit = true
                        copiedTargetTables.forEach {
                            reseedIdentityColumns(target.connection, targetEngine, it)
                        }
                    }
                    source.connection.rollback()
                    return ConversionResult(
                        copiedRowsByTable.toSortedMap(),
                        createdTables,
                        System.currentTimeMillis() - startedAt)
                } catch (e: Exception) {
                    rollbackQuietly(target.connection)
                    rollbackQuietly(source.connection)
                    throw e
                } finally {
                    resetConnectionState(source.connection)
                    resetConnectionState(target.connection)
                }
            }
        }
    }

    /**
     * Mở connection từ SQL pool hiện tại hoặc một H2 pool tạm cho file H2 trong config.
     */
    private fun openConnection(engine: DbConvertEngine): ManagedConnection {
        if (engine.isSql()) {
            return ManagedConnection(DotMan.instance.dbPool!!.getConnection(), null)
        }

        val config = DotMan.instance.config.config
        val fileName = config.getString("database.h2.file", "dotman")!!
        val pool = H2DBP(
            DotMan.instance.dataFolder,
            fileName,
            { DotMan.instance.logger.info(it) },
            { level, message, throwable -> DotMan.instance.logger.log(level, message, throwable) },
            null
        )
        return ManagedConnection(pool.getConnection(), pool)
    }

    /**
     * Chạy migration cho engine tương ứng và lưu migration version mới nhất của chính engine đó.
     */
    private fun migrate(connection: Connection, engine: DbConvertEngine): Int {
        val currentVersion = readMigrationVersion(connection, engine)
        val latestVersion = BukkitDBMigrator(
            DotMan.instance,
            connection,
            "db/migrations/${engine.migrationPath}",
            currentVersion).migrate()
        if (latestVersion > currentVersion) {
            setMigrationVersion(connection, engine, latestVersion)
        }

        return latestVersion
    }

    private fun readMigrationVersion(connection: Connection, engine: DbConvertEngine): Int {
        return try {
            val configTable = engine.quote("dotman_config")
            val keyColumn = engine.quote("key")
            val valueColumn = engine.quote("value")
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT $valueColumn FROM $configTable WHERE $keyColumn = 'migration_version'").use {
                    if (it.next()) {
                        it.getString(1).toIntOrNull() ?: 0
                    } else {
                        0
                    }
                }
            }
        } catch (_: SQLException) {
            0
        }
    }

    /**
     * Lấy toàn bộ table có prefix dotman từ database metadata.
     */
    private fun loadTables(connection: Connection): List<TableInfo> {
        val tables = loadTables(connection, connection.catalog)
        if (tables.isNotEmpty()) {
            return tables
        }

        return loadTables(connection, null)
    }

    private fun loadTables(connection: Connection, catalog: String?): List<TableInfo> {
        val tables = mutableListOf<TableInfo>()
        connection.metaData.getTables(catalog, null, "%", arrayOf("TABLE")).use { result ->
            while (result.next()) {
                val tableName = result.getString("TABLE_NAME") ?: continue
                if (!tableName.lowercase(Locale.ROOT).startsWith("dotman_")) {
                    continue
                }

                val schema = result.getString("TABLE_SCHEM")
                val columns = loadColumns(connection, schema, tableName)
                tables.add(TableInfo(tableName, columns))
            }
        }
        return tables.sortedBy { it.name }
    }

    private fun loadColumns(connection: Connection, schema: String?, table: String): List<ColumnInfo> {
        val columns = loadColumns(connection, connection.catalog, schema, table)
        if (columns.isNotEmpty()) {
            return columns
        }

        return loadColumns(connection, null, schema, table)
    }

    private fun loadColumns(connection: Connection, catalog: String?, schema: String?, table: String): List<ColumnInfo> {
        val columns = mutableListOf<ColumnInfo>()
        connection.metaData.getColumns(catalog, schema, table, "%").use { result ->
            while (result.next()) {
                columns.add(ColumnInfo(result.getString("COLUMN_NAME"), result.isAutoIncrementColumn()))
            }
        }
        return columns
    }

    private fun findCreatedTables(connection: Connection, oldTables: Set<String>): List<String> {
        return loadTables(connection)
            .map { it.name }
            .filter { !oldTables.contains(it.lowercase(Locale.ROOT)) }
            .sorted()
    }

    private fun List<TableInfo>.toNormalizedNameSet() = map { it.name.lowercase(Locale.ROOT) }.toSet()

    private fun List<TableInfo>.toTableMap() = associateBy { it.name.lowercase(Locale.ROOT) }

    /**
     * Kiểm tra target có đủ mọi table và column từ source sẽ được copy, rồi trả về danh sách table cần copy.
     */
    private fun resolveTablesToCopy(sourceTables: List<TableInfo>, targetTables: Map<String, TableInfo>): List<TablePair> {
        return sourceTables.mapNotNull { sourceTable ->
            val targetTable = targetTables[sourceTable.name.lowercase(Locale.ROOT)]
                ?: if (sourceTable.name.equals(SQL_ONLY_CONFIG_HEAD, true)) {
                    return@mapNotNull null
                } else {
                    throw IllegalStateException("Target thiếu bảng ${sourceTable.name}")
                }
            val targetColumns = targetTable.columns.map { it.name.lowercase(Locale.ROOT) }.toSet()
            sourceTable.columns.forEach { column ->
                if (!targetColumns.contains(column.name.lowercase(Locale.ROOT))) {
                    throw IllegalStateException("Target thiếu cột ${sourceTable.name}.${column.name}")
                }
            }

            TablePair(sourceTable, targetTable)
        }
    }

    private fun countSourceRows(connection: Connection, engine: DbConvertEngine, tables: List<TablePair>): Int {
        var totalRows = 0
        tables.forEach {
            totalRows += countRows(connection, engine, it.source)
        }
        return totalRows
    }

    private fun countRows(connection: Connection, engine: DbConvertEngine, table: TableInfo): Int {
        val tableName = engine.quote(table.name)
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $tableName").use {
                it.next()
                return it.getInt(1)
            }
        }
    }

    /**
     * Thay toàn bộ dữ liệu của một table target bằng dữ liệu đọc từ table source tương ứng.
     */
    private fun copyTable(
        source: Connection,
        target: Connection,
        sourceEngine: DbConvertEngine,
        targetEngine: DbConvertEngine,
        sourceTable: TableInfo,
        targetTable: TableInfo,
        progress: ProgressView
    ): Int {
        progress.table(sourceTable.name)

        val sourceTableName = sourceEngine.quote(sourceTable.name)
        val targetTableName = targetEngine.quote(targetTable.name)
        val sourceColumns = sourceTable.columns.joinToString(", ") { sourceEngine.quote(it.name) }
        val targetColumns = sourceTable.columns.joinToString(", ") { targetEngine.quote(it.name) }
        val placeholders = sourceTable.columns.joinToString(", ") { "?" }

        target.createStatement().use { statement ->
            statement.executeUpdate("DELETE FROM $targetTableName")
        }

        val query = "SELECT $sourceColumns FROM $sourceTableName"
        val insert = "INSERT INTO $targetTableName ($targetColumns) VALUES ($placeholders)"
        var copiedRows = 0
        source.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { select ->
            select.fetchSize = BATCH_SIZE
            select.executeQuery(query).use { result ->
                target.prepareStatement(insert).use { prepared ->
                    var batchRows = 0
                    while (result.next()) {
                        for (index in sourceTable.columns.indices) {
                            prepared.setObject(index + 1, result.getObject(index + 1))
                        }
                        prepared.addBatch()
                        batchRows++
                        copiedRows++
                        progress.row()

                        if (batchRows >= BATCH_SIZE) {
                            prepared.executeBatch()
                            batchRows = 0
                        }
                    }

                    if (batchRows > 0) {
                        prepared.executeBatch()
                    }
                }
            }
        }
        return copiedRows
    }

    /**
     * Tăng identity column của H2 để các insert sau này không dùng lại ID đã copy tường minh từ engine khác.
     */
    private fun reseedIdentityColumns(connection: Connection, engine: DbConvertEngine, table: TableInfo) {
        table.columns.filter { it.autoIncrement }.forEach { column ->
            val tableName = engine.quote(table.name)
            val columnName = engine.quote(column.name)
            val nextId = connection.createStatement().use { statement ->
                statement.executeQuery("SELECT MAX($columnName) FROM $tableName").use { result ->
                    if (result.next()) {
                        result.getLong(1) + 1
                    } else {
                        1L
                    }
                }
            }

            connection.createStatement().use { statement ->
                statement.executeUpdate("ALTER TABLE $tableName ALTER COLUMN $columnName RESTART WITH $nextId")
            }
        }
    }

    private fun setMigrationVersion(connection: Connection, engine: DbConvertEngine, version: Int) {
        val sql = if (engine == DbConvertEngine.H2) {
            "MERGE INTO ${engine.quote("dotman_config")}(${engine.quote("key")}" +
                    ", ${engine.quote("value")}) KEY(${engine.quote("key")}) VALUES (?, ?)"
        } else {
            "INSERT INTO ${engine.quote("dotman_config")} (${engine.quote("key")}, ${engine.quote("value")}) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE ${engine.quote("value")} = VALUES(${engine.quote("value")})"
        }

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, "migration_version")
            statement.setString(2, version.toString())
            statement.executeUpdate()
        }
    }

    private fun rollbackQuietly(connection: Connection) {
        try {
            connection.rollback()
        } catch (_: Exception) {
        }
    }

    private fun resetConnectionState(connection: Connection) {
        try {
            connection.isReadOnly = false
        } catch (_: Exception) {
        }

        try {
            connection.autoCommit = true
        } catch (_: Exception) {
        }
    }

    private data class ConversionResult(
        val copiedRowsByTable: Map<String, Int>,
        val createdTables: List<String>,
        val elapsedMillis: Long
    ) {
        fun sendTo(player: Player) {
            val copiedTableNames = if (copiedRowsByTable.isEmpty()) {
                "không có"
            } else {
                copiedRowsByTable.keys.joinToString(", ")
            }
            val createdTableNames = if (createdTables.isEmpty()) {
                "không có"
            } else {
                createdTables.joinToString(", ")
            }
            val totalRows = copiedRowsByTable.values.sum()

            player.send("""
                §aSummary:
                §7- §fTable đã copy (§b${copiedRowsByTable.size}§f): §a$copiedTableNames
                §7- §fTable tạo mới (§b${createdTables.size}§f): §a$createdTableNames
                §7- §fTổng số hàng đã copy: §a${totalRows.format()}
                §7- §fThời gian thực hiện: §a${getTimeString(elapsedMillis)}
            """.trimIndent())
        }
    }

    private class ProgressView(private val player: Player, private val source: DbConvertEngine, private val target: DbConvertEngine) {
        private var bossBar: BukkitBossBar? = null
        private var totalRows = 0
        private var copiedRows = 0
        private var currentTable = "-"
        private var startedAt = System.currentTimeMillis()
        private var lastUpdate = 0L

        init {
            runSync {
                bossBar = BukkitBossBar("§eChuẩn bị chuyển đổi database...", BarColor.YELLOW.name, BarStyle.SOLID.name).apply {
                    addPlayer(player)
                    progress = 0.0
                }
            }
        }

        fun start(totalRows: Int) {
            this.totalRows = totalRows
            copiedRows = 0
            startedAt = System.currentTimeMillis()
            runSync { bossBar?.setColor(BarColor.BLUE) }
            update(true)
        }

        fun table(name: String) {
            currentTable = name
            update(true)
        }

        fun row() {
            copiedRows++
            update(false)
        }

        /** TODO:
         * bổ sung runlater vào foliautils ở minevn-lib.
         *
         * mục đích: có hiển thị progress đến 100% (hiện tại bị skip luôn),
         * hiển thị chuyển đổi thành công/thất bại rồi 1 giây sau đó mới disable plugin
         * */
        fun complete() {
            runSync {
                bossBar?.color = BarColor.GREEN
                bossBar?.progress = 1.0
                bossBar?.setTitle("§aChuyển đổi database hoàn tất")
                bossBar?.removeAll()
            }
        }

        fun fail() {
            runSync {
                bossBar?.setColor(BarColor.RED)
                bossBar?.setTitle("§cChuyển đổi database thất bại")
                bossBar?.removeAll()
            }
        }

        private fun update(force: Boolean) {
            val now = System.currentTimeMillis()
            if (!force && now - lastUpdate < 500) {
                return
            }
            lastUpdate = now

            val percent = if (totalRows <= 0) {
                100.0
            } else {
                copiedRows * 100.0 / totalRows
            }
            val progress = if (totalRows <= 0) {
                1.0
            } else {
                (copiedRows.toDouble() / totalRows).coerceIn(0.0, 1.0)
            }
            val eta = estimateEta(now)
            val percentText = formatFloat(percent.toFloat())
            val message = "§eDB ${source.typeName} -> ${target.typeName}: §b$percentText% §7| §f${copiedRows.format()}/${totalRows.format()} §7| §a$currentTable §7| ETA: §f$eta"

            runSync {
                bossBar?.progress = progress
                bossBar?.setTitle(message.color())
            }
        }

        private fun estimateEta(now: Long): String {
            if (totalRows <= 0 || copiedRows <= 0) {
                return "0s"
            }

            val elapsed = now - startedAt
            val estimatedTotal = elapsed * totalRows / copiedRows
            val remaining = (estimatedTotal - elapsed).coerceAtLeast(0)
            return getTimeString(remaining)
        }
    }

    private class ManagedConnection(val connection: Connection, private val pool: DatabasePool?) : java.io.Closeable {
        override fun close() {
            connection.close()
            pool?.disconnect()
        }
    }

    private data class TablePair(val source: TableInfo, val target: TableInfo)

    private data class TableInfo(val name: String, val columns: List<ColumnInfo>)

    private data class ColumnInfo(val name: String, val autoIncrement: Boolean)

    private fun ResultSet.isAutoIncrementColumn(): Boolean {
        return try {
            getString("IS_AUTOINCREMENT").equals("YES", true)
        } catch (_: SQLException) {
            false
        }
    }
}
