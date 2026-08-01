package com.paycontrol.app.data.repository

import androidx.room.withTransaction
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.local.dao.SupplierDao
import com.paycontrol.app.data.local.dao.TransactionDao
import com.paycontrol.app.data.local.entity.SupplierEntity
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.domain.util.DomainStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SupplierRepository(
    private val database: AppDatabase,
    private val messages: DomainStrings,
    private val supplierDao: SupplierDao = database.supplierDao(),
    private val transactionDao: TransactionDao = database.transactionDao()
) {

    fun observeSuppliers(): Flow<List<SupplierEntity>> =
        supplierDao.observeAll()
            .catch { e ->
                AppLog.e(TAG, "Error observing suppliers", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)

    fun pagingSource(): androidx.paging.PagingSource<Int, SupplierEntity> =
        supplierDao.pagingSource()

    suspend fun createSupplier(name: String, phone: String = ""): Long =
        withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            require(trimmed.isNotBlank()) { messages.supplierNameRequired() }
            require(trimmed.length <= 80) { messages.supplierNameTooLong() }
            supplierDao.insert(
                SupplierEntity(
                    name = trimmed,
                    phone = phone.trim().take(32)
                )
            )
        }

    suspend fun updateSupplier(supplierId: Long, name: String, phone: String) =
        withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            require(trimmed.isNotBlank()) { messages.supplierNameRequired() }
            require(trimmed.length <= 80) { messages.supplierNameTooLong() }
            val supplier = supplierDao.getById(supplierId)
                ?: error(messages.supplierNotFound())
            supplierDao.update(
                supplier.copy(
                    name = trimmed,
                    phone = phone.trim().take(32)
                )
            )
        }

    suspend fun delete(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        database.withTransaction {
            transactionDao.clearRelatedSupplier(supplier.id)
            supplierDao.delete(supplier)
        }
    }

    suspend fun deleteById(supplierId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val supplier = supplierDao.getById(supplierId)
                ?: error(messages.supplierNotFound())
            transactionDao.clearRelatedSupplier(supplierId)
            supplierDao.delete(supplier)
        }
    }

    companion object {
        private const val TAG = "SupplierRepo"
    }
}
