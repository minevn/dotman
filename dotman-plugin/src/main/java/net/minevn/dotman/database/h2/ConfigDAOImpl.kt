package net.minevn.dotman.database.h2

import net.minevn.dotman.database.ConfigDAO

class ConfigDAOImpl : ConfigDAO() {
    override fun isTableExistsScript() =
        "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'dotman_config' and TABLE_SCHEMA = 'PUBLIC'"

    override fun getScript() = """SELECT * FROM "dotman_config" WHERE "key" = ?"""

    override fun setScript() = """MERGE INTO "dotman_config"("key", "value") KEY("key") VALUES (?, ?)"""

    override fun deleteScript() = """DELETE FROM "dotman_config" WHERE "key" = ?"""
}