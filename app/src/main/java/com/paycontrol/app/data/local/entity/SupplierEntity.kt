package com.paycontrol.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Proveedor con acumulado de pagos realizados.
 *
 * [totalPaid] en centavos.
 */
@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["name"], unique = true)]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val totalPaid: Long = 0L
)
