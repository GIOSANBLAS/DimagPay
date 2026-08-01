package com.paycontrol.app.data.repository

import androidx.room.withTransaction
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.local.dao.ClientDao
import com.paycontrol.app.data.local.dao.TransactionDao
import com.paycontrol.app.data.local.entity.ClientEntity
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.domain.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ClientRepository(
    private val database: AppDatabase,
    private val clientDao: ClientDao = database.clientDao(),
    private val transactionDao: TransactionDao = database.transactionDao()
) {

    fun observeClients(): Flow<List<ClientEntity>> =
        clientDao.observeAll()
            .catch { e ->
                AppLog.e(TAG, "Error observing clients", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)

    fun observeClientsWithDebt(): Flow<List<ClientEntity>> =
        clientDao.observeWithDebt()
            .catch { e ->
                AppLog.e(TAG, "Error observing clients with debt", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)

    fun pagingSource(): androidx.paging.PagingSource<Int, ClientEntity> =
        clientDao.pagingSource()

    fun observeTotalReceivables(): Flow<Long> =
        clientDao.observeTotalReceivables()
            .catch { e ->
                AppLog.e(TAG, "Error observing total receivables", e)
                emit(0L)
            }
            .flowOn(Dispatchers.IO)

    suspend fun createClient(
        name: String,
        phone: String = "",
        initialDebtCents: Long = 0L
    ): Long = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "El nombre del cliente es obligatorio" }
        require(trimmed.length <= 80) { "El nombre del cliente es demasiado largo" }
        require(initialDebtCents >= 0L) { "La deuda inicial no puede ser negativa" }
        clientDao.insert(
            ClientEntity(
                name = trimmed,
                phone = phone.trim().take(32),
                totalDebt = initialDebtCents
            )
        )
    }

    suspend fun addDebt(clientId: Long, amountCents: Long) = withContext(Dispatchers.IO) {
        require(amountCents > 0L) { "El monto debe ser mayor a cero" }
        val client = clientDao.getById(clientId)
            ?: error("Cliente no encontrado")
        clientDao.update(client.copy(totalDebt = Money.add(client.totalDebt, amountCents)))
    }

    suspend fun updateClient(clientId: Long, name: String, phone: String) =
        withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            require(trimmed.isNotBlank()) { "El nombre del cliente es obligatorio" }
            require(trimmed.length <= 80) { "El nombre del cliente es demasiado largo" }
            val client = clientDao.getById(clientId)
                ?: error("Cliente no encontrado")
            clientDao.update(
                client.copy(
                    name = trimmed,
                    phone = phone.trim().take(32)
                )
            )
        }

    suspend fun delete(client: ClientEntity) = withContext(Dispatchers.IO) {
        database.withTransaction {
            transactionDao.clearRelatedClient(client.id)
            clientDao.delete(client)
        }
    }

    suspend fun deleteById(clientId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val client = clientDao.getById(clientId)
                ?: error("Cliente no encontrado")
            transactionDao.clearRelatedClient(clientId)
            clientDao.delete(client)
        }
    }

    companion object {
        private const val TAG = "ClientRepo"
    }
}
