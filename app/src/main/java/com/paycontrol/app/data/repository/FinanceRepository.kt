package com.paycontrol.app.data.repository

import androidx.room.withTransaction
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.local.dao.AccountDao
import com.paycontrol.app.data.local.dao.ClientDao
import com.paycontrol.app.data.local.dao.LedgerDao
import com.paycontrol.app.data.local.dao.SupplierDao
import com.paycontrol.app.data.local.dao.TransactionDao
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.domain.model.AccountType
import com.paycontrol.app.domain.model.DefaultCategories
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Punto de verdad financiero.
 * Mutaciones multi-tabla: RoomDatabase.withTransaction (atómicas entre DAOs).
 * Posteo movimiento+saldo: LedgerDao.@Transaction.
 * Todo en Dispatchers.IO.
 */
class FinanceRepository(
    private val database: AppDatabase,
    private val accountDao: AccountDao = database.accountDao(),
    private val transactionDao: TransactionDao = database.transactionDao(),
    private val clientDao: ClientDao = database.clientDao(),
    private val supplierDao: SupplierDao = database.supplierDao(),
    private val ledgerDao: LedgerDao = database.ledgerDao()
) {

    fun observeAccounts(): Flow<List<AccountEntity>> =
        accountDao.observeAll()
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    fun observeRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.observeRecent(limit.coerceIn(1, 100))
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    fun observeAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.observeAll()
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    fun observeTotalIncome(): Flow<Long> =
        transactionDao.observeTotalByType(TransactionType.INCOME)
            .catch { emit(0L) }
            .flowOn(Dispatchers.IO)

    fun observeTotalExpense(): Flow<Long> =
        transactionDao.observeTotalByType(TransactionType.EXPENSE)
            .catch { emit(0L) }
            .flowOn(Dispatchers.IO)

    fun observeConsolidatedBalance(): Flow<Long> =
        combine(observeTotalIncome(), observeTotalExpense()) { income, expense ->
            Money.subtract(income, expense)
        }.catch { emit(0L) }

    fun observeAccountsBalance(): Flow<Long> =
        accountDao.observeTotalBalance()
            .catch { emit(0L) }
            .flowOn(Dispatchers.IO)

    suspend fun getTransactionsFiltered(
        fromMs: Long?,
        toMs: Long?,
        accountId: Long? = null,
        type: String? = null,
        category: String? = null
    ): List<TransactionEntity> = withContext(Dispatchers.IO) {
        val trimmedCategory = category?.trim()?.takeIf { it.isNotEmpty() }
        transactionDao.getFiltered(
            fromMs = fromMs,
            toMs = toMs,
            accountId = accountId,
            type = type?.takeIf { it.isNotBlank() },
            category = trimmedCategory
        )
    }

    suspend fun ensureDefaultAccount(): Long = withContext(Dispatchers.IO) {
        val existing = accountDao.getAll()
        if (existing.isNotEmpty()) return@withContext existing.first().id
        createAccountInternal(
            name = "Efectivo principal",
            type = AccountType.CASH,
            initialBalanceCents = 0L
        )
    }

    suspend fun createAccount(
        name: String,
        type: String,
        initialBalanceCents: Long = 0L
    ): Long = withContext(Dispatchers.IO) {
        createAccountInternal(name, type, initialBalanceCents)
    }

    suspend fun updateAccount(
        id: Long,
        name: String,
        type: String
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = accountDao.getById(id)
                ?: error("Cuenta no encontrada")
            val trimmed = name.trim()
            require(trimmed.isNotBlank()) { "El nombre de la cuenta es obligatorio" }
            require(trimmed.length <= 80) { "El nombre de la cuenta es demasiado largo" }
            require(type in AccountType.all) { "Tipo de cuenta inválido" }
            accountDao.update(
                existing.copy(name = trimmed, type = type)
            )
        }
    }

    /**
     * Elimina la cuenta solo si no tiene movimientos.
     * Prefiere bloquear ante CASCADE para no perder historial.
     */
    suspend fun deleteAccount(id: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = accountDao.getById(id)
                ?: error("Cuenta no encontrada")
            val txCount = transactionDao.countByAccount(id)
            if (txCount > 0) {
                error(
                    "No se puede eliminar «${existing.name}»: tiene $txCount " +
                        if (txCount == 1) "movimiento registrado" else "movimientos registrados"
                )
            }
            accountDao.delete(existing)
        }
    }

    suspend fun registerTransaction(
        accountId: Long,
        amountCents: Long,
        type: String,
        category: String,
        date: Long,
        note: String = ""
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            applyTransactionLocked(
                accountId = accountId,
                amountCents = amountCents,
                type = type,
                category = category,
                date = date,
                note = note
            )
        }
    }

    /** Abono cliente + INGRESO en cuenta (atómico). */
    suspend fun receiveClientPayment(
        clientId: Long,
        accountId: Long,
        amountCents: Long
    ): Long = withContext(Dispatchers.IO) {
        require(amountCents > 0L) { "El abono debe ser mayor a cero" }
        database.withTransaction {
            val client = clientDao.getById(clientId)
                ?: error("Cliente no encontrado")
            require(amountCents <= client.totalDebt) {
                "El abono supera la deuda pendiente (${Money.format(client.totalDebt)})"
            }
            val txId = applyTransactionLocked(
                accountId = accountId,
                amountCents = amountCents,
                type = TransactionType.INCOME,
                category = DefaultCategories.CLIENT_PAYMENT,
                date = System.currentTimeMillis(),
                note = "Abono · ${client.name}"
            )
            clientDao.update(
                client.copy(totalDebt = Money.subtract(client.totalDebt, amountCents))
            )
            txId
        }
    }

    /** Pago proveedor + GASTO en cuenta (atómico). */
    suspend fun paySupplier(
        supplierId: Long,
        accountId: Long,
        amountCents: Long
    ): Long = withContext(Dispatchers.IO) {
        require(amountCents > 0L) { "El pago debe ser mayor a cero" }
        database.withTransaction {
            val supplier = supplierDao.getById(supplierId)
                ?: error("Proveedor no encontrado")
            val txId = applyTransactionLocked(
                accountId = accountId,
                amountCents = amountCents,
                type = TransactionType.EXPENSE,
                category = DefaultCategories.SUPPLIER_PAYMENT,
                date = System.currentTimeMillis(),
                note = "Pago · ${supplier.name}"
            )
            supplierDao.update(
                supplier.copy(totalPaid = Money.add(supplier.totalPaid, amountCents))
            )
            txId
        }
    }

    /**
     * Elimina un movimiento y revierte el saldo de la cuenta (atómico).
     * INGRESO → resta el monto; GASTO → suma el monto de vuelta.
     */
    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val tx = transactionDao.getById(id)
                ?: error("Movimiento no encontrado")
            val account = accountDao.getById(tx.accountId)
                ?: error("Cuenta no encontrada")
            val newBalance = when (tx.type) {
                TransactionType.INCOME -> Money.subtract(account.balance, tx.amount)
                TransactionType.EXPENSE -> Money.add(account.balance, tx.amount)
                else -> error("Tipo inválido")
            }
            accountDao.update(account.copy(balance = newBalance))
            transactionDao.delete(tx)
        }
    }

    private suspend fun createAccountInternal(
        name: String,
        type: String,
        initialBalanceCents: Long
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "El nombre de la cuenta es obligatorio" }
        require(trimmed.length <= 80) { "El nombre de la cuenta es demasiado largo" }
        require(type in AccountType.all) { "Tipo de cuenta inválido" }
        require(initialBalanceCents >= 0L) { "El saldo inicial no puede ser negativo" }
        return accountDao.insert(
            AccountEntity(
                name = trimmed,
                balance = initialBalanceCents,
                type = type
            )
        )
    }

    private suspend fun applyTransactionLocked(
        accountId: Long,
        amountCents: Long,
        type: String,
        category: String,
        date: Long,
        note: String
    ): Long {
        require(amountCents > 0L) { "El monto debe ser mayor a cero" }
        require(type == TransactionType.INCOME || type == TransactionType.EXPENSE) {
            "Tipo inválido"
        }
        val trimmedCategory = category.trim()
        require(trimmedCategory.isNotBlank()) { "La categoría es obligatoria" }

        val account = accountDao.getById(accountId)
            ?: error("Cuenta no encontrada")

        val newBalance = when (type) {
            TransactionType.INCOME -> Money.add(account.balance, amountCents)
            else -> {
                if (account.balance < amountCents) {
                    error("Saldo insuficiente en «${account.name}» (${Money.format(account.balance)})")
                }
                Money.subtract(account.balance, amountCents)
            }
        }

        val id = ledgerDao.postTransactionAndUpdateBalance(
            transaction = TransactionEntity(
                accountId = accountId,
                amount = amountCents,
                type = type,
                category = trimmedCategory,
                date = date,
                note = note.trim().take(500)
            ),
            newBalance = newBalance
        )
        return id
    }
}
