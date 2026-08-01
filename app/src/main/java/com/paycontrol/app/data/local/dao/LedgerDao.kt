package com.paycontrol.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity

/**
 * Operaciones atómicas (@Transaction) sobre movimiento + saldo.
 * Las operaciones multi-DAO usan RoomDatabase.withTransaction en FinanceRepository.
 */
@Dao
abstract class LedgerDao {

    @Query("SELECT * FROM accounts WHERE id = :id")
    abstract suspend fun getAccount(id: Long): AccountEntity?

    @Query("UPDATE accounts SET balance = :balance WHERE id = :id")
    abstract suspend fun setAccountBalance(id: Long, balance: Long)

    @Insert
    abstract suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Transaction
    open suspend fun postTransactionAndUpdateBalance(
        transaction: TransactionEntity,
        newBalance: Long
    ): Long {
        val id = insertTransaction(transaction)
        setAccountBalance(transaction.accountId, newBalance)
        return id
    }
}
