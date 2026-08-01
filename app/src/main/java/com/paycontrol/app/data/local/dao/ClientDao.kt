package com.paycontrol.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paycontrol.app.data.local.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    @Query("SELECT * FROM clients ORDER BY totalDebt DESC, name ASC")
    fun observeAll(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY totalDebt DESC, name ASC")
    fun pagingSource(): androidx.paging.PagingSource<Int, ClientEntity>

    @Query("SELECT * FROM clients ORDER BY totalDebt DESC, name ASC")
    suspend fun getAll(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE totalDebt > 0 ORDER BY totalDebt DESC, name ASC")
    fun observeWithDebt(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: Long): ClientEntity?

    @Query("SELECT COALESCE(SUM(totalDebt), 0) FROM clients")
    fun observeTotalReceivables(): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalDebt), 0) FROM clients WHERE totalDebt > 0")
    suspend fun getTotalReceivables(): Long

    @Query("SELECT COUNT(*) FROM clients WHERE totalDebt > 0")
    suspend fun countWithDebt(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(client: ClientEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(clients: List<ClientEntity>)

    @Update
    suspend fun update(client: ClientEntity)

    @Delete
    suspend fun delete(client: ClientEntity)

    @Query("DELETE FROM clients")
    suspend fun deleteAll()
}
