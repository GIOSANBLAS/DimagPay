package com.paycontrol.app.data.repository

import com.paycontrol.app.data.local.dao.SupplierDao
import com.paycontrol.app.data.local.entity.SupplierEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SupplierRepository(
    private val supplierDao: SupplierDao
) {

    fun observeSuppliers(): Flow<List<SupplierEntity>> =
        supplierDao.observeAll()
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    suspend fun createSupplier(name: String, phone: String = ""): Long =
        withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            require(trimmed.isNotBlank()) { "El nombre del proveedor es obligatorio" }
            require(trimmed.length <= 80) { "El nombre del proveedor es demasiado largo" }
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
            require(trimmed.isNotBlank()) { "El nombre del proveedor es obligatorio" }
            require(trimmed.length <= 80) { "El nombre del proveedor es demasiado largo" }
            val supplier = supplierDao.getById(supplierId)
                ?: error("Proveedor no encontrado")
            supplierDao.update(
                supplier.copy(
                    name = trimmed,
                    phone = phone.trim().take(32)
                )
            )
        }

    suspend fun delete(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        supplierDao.delete(supplier)
    }

    suspend fun deleteById(supplierId: Long) = withContext(Dispatchers.IO) {
        val supplier = supplierDao.getById(supplierId)
            ?: error("Proveedor no encontrado")
        supplierDao.delete(supplier)
    }
}
