package net.minevn.dotman.database.dao

import net.minevn.dotman.database.DataAccess
import net.minevn.dotman.database.getInstance

interface TopDAO : DataAccess {
    companion object {
        fun getInstance() = TopDAO::class.getInstance()
    }
}