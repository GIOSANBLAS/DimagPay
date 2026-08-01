package com.paycontrol.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cuenta financiera (efectivo, banco, etc.).
 *
 * [balance] se guarda en centavos (unidades menores) para evitar
 * errores de precisión flotante en dinero.
 */
@Entity(
    tableName = "accounts",
    indices = [Index(value = ["name"], unique = true)]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val balance: Long = 0L,
    val type: String
)
