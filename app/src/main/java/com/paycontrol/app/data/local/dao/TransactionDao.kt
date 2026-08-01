package com.paycontrol.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paycontrol.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun pagingSource(): androidx.paging.PagingSource<Int, TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        ORDER BY date DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    fun observeByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun countByAccount(accountId: Long): Int

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transferGroupId = :groupId")
    suspend fun getByTransferGroup(groupId: Long): List<TransactionEntity>

    @Query("UPDATE transactions SET relatedClientId = NULL WHERE relatedClientId = :clientId")
    suspend fun clearRelatedClient(clientId: Long)

    @Query("UPDATE transactions SET relatedSupplierId = NULL WHERE relatedSupplierId = :supplierId")
    suspend fun clearRelatedSupplier(supplierId: Long)

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = :type
        """
    )
    fun observeTotalByType(type: String): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = :type
        """
    )
    suspend fun getTotalByType(type: String): Long

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:fromMs IS NULL OR date >= :fromMs)
          AND (:toMs IS NULL OR date <= :toMs)
          AND (:accountId IS NULL OR accountId = :accountId)
          AND (:type IS NULL OR type = :type)
          AND (:category IS NULL OR category LIKE '%' || :category || '%')
        ORDER BY date DESC, id DESC
        """
    )
    fun pagingFiltered(
        fromMs: Long?,
        toMs: Long?,
        accountId: Long?,
        type: String?,
        category: String?
    ): androidx.paging.PagingSource<Int, TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:fromMs IS NULL OR date >= :fromMs)
          AND (:toMs IS NULL OR date <= :toMs)
          AND (:accountId IS NULL OR accountId = :accountId)
          AND (:type IS NULL OR type = :type)
          AND (:category IS NULL OR category LIKE '%' || :category || '%')
        ORDER BY date DESC, id DESC
        """
    )
    suspend fun getFiltered(
        fromMs: Long?,
        toMs: Long?,
        accountId: Long?,
        type: String?,
        category: String?
    ): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
