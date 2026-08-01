package com.paycontrol.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Movimiento asociado a una [AccountEntity].
 *
 * [amount] siempre positivo en centavos; el sentido lo define [type].
 * Transferencias: dos filas con el mismo [transferGroupId];
 * [transferIsOutbound] = true en la cuenta de origen.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["transferGroupId"]),
        Index(value = ["relatedClientId"]),
        Index(value = ["relatedSupplierId"]),
        // date es epoch millis UTC (Instant); UI formatea en zona local.
        Index(value = ["date", "accountId", "type"]),
        Index(value = ["date", "type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val accountId: Long,
    val amount: Long,
    val type: String,
    val category: String,
    /** Epoch millis UTC ([java.time.Instant]). */
    val date: Long,
    val note: String = "",
    val transferGroupId: Long? = null,
    val transferIsOutbound: Boolean? = null,
    val relatedClientId: Long? = null,
    val relatedSupplierId: Long? = null
)
