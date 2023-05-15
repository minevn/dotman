package net.minevn.dotman.test.db

import net.minevn.dotman.database.connections.DatabaseConnection
import net.minevn.dotman.database.connections.types.H2DBC
import net.minevn.dotman.database.dao.LogDAO
import net.minevn.dotman.test.utils.mockDotMan
import net.minevn.dotman.test.utils.setDatabaseConnectionInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class H2DBCTest {
    @BeforeEach
    fun setUp() {
        mockDotMan()
        setDatabaseConnectionInstance(H2DBC("test"))
    }

    @Test
    fun testDAOInit() {
        val logDao = LogDAO.getInstance()
        assertEquals(net.minevn.dotman.database.dao.h2.LogDAOImpl::class.java, logDao.javaClass)
    }

    @Test
    fun testMigrating() {
        DatabaseConnection.instance.migrate()
    }
}