package com.paycontrol.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cliente / cuenta por cobrar.
 *
 * [totalDebt] en centavos. Un valor > 0 indica saldo pendiente.
 */
@Entity(
    tableName = "clients",
    indices = [Index(value = ["name"], unique = true)]
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val totalDebt: Long = 0L
)
