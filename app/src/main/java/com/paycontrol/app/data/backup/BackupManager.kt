package com.paycontrol.app.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.ClientEntity
import com.paycontrol.app.data.local.entity.SupplierEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.domain.model.AccountType
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.BackupPasswordPolicy
import com.paycontrol.app.ui.widget.BalanceWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * Respaldo / restauración offline de DimagPay cifrado con AES-256-GCM.
 *
 * Montos en centavos ([Long]). La restauración **reemplaza** todos los datos.
 * Sobre: JSON `{ format, salt, iv, payload }` (Base64) para compartir vía FileProvider.
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val appContext = context.applicationContext

    data class Inventory(
        val accounts: Int,
        val transactions: Int,
        val clients: Int,
        val suppliers: Int
    )

    suspend fun currentInventory(): Inventory = withContext(Dispatchers.IO) {
        Inventory(
            accounts = database.accountDao().getAll().size,
            transactions = database.transactionDao().getAll().size,
            clients = database.clientDao().getAll().size,
            suppliers = database.supplierDao().getAll().size
        )
    }

    suspend fun export(password: String): File = withContext(Dispatchers.IO) {
        requirePassword(password)

        val accounts = database.accountDao().getAll()
        val transactions = database.transactionDao().getAll()
        val clients = database.clientDao().getAll()
        val suppliers = database.supplierDao().getAll()

        val root = JSONObject().apply {
            put(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
            put(KEY_APP, APP_NAME)
            put(KEY_EXPORTED_AT, System.currentTimeMillis())
            put(KEY_ACCOUNTS, accountsToJson(accounts))
            put(KEY_TRANSACTIONS, transactionsToJson(transactions))
            put(KEY_CLIENTS, clientsToJson(clients))
            put(KEY_SUPPLIERS, suppliersToJson(suppliers))
        }

        val plaintext = root.toString().toByteArray(Charsets.UTF_8)
        val envelope = encryptToEnvelope(plaintext, password)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "dimagpay-backup-$stamp.json")
        file.writeText(envelope.toString(2), Charsets.UTF_8)
        file
    }

    suspend fun import(inputStream: InputStream, password: String) = withContext(Dispatchers.IO) {
        requirePassword(password)
        val raw = readLimited(inputStream, MAX_BACKUP_BYTES)
        val text = String(raw, Charsets.UTF_8)
        val plaintext = decryptEnvelope(text, password)
        importJson(String(plaintext, Charsets.UTF_8))
    }

    suspend fun importJson(text: String) = withContext(Dispatchers.IO) {
        val root = try {
            JSONObject(text)
        } catch (_: Exception) {
            throw IllegalArgumentException("El archivo no es un respaldo JSON válido de DimagPay")
        }

        val app = root.optString(KEY_APP, "")
        if (app != APP_NAME) {
            throw IllegalArgumentException("El archivo no es un respaldo válido de DimagPay")
        }

        val version = root.optInt(KEY_SCHEMA_VERSION, -1)
        if (version != SCHEMA_VERSION) {
            throw IllegalArgumentException(
                "Versión de esquema no compatible ($version). Se espera $SCHEMA_VERSION."
            )
        }

        val accounts = parseAccounts(root.optJSONArray(KEY_ACCOUNTS) ?: JSONArray())
        val transactions = parseTransactions(root.optJSONArray(KEY_TRANSACTIONS) ?: JSONArray())
        val clients = parseClients(root.optJSONArray(KEY_CLIENTS) ?: JSONArray())
        val suppliers = parseSuppliers(root.optJSONArray(KEY_SUPPLIERS) ?: JSONArray())

        validateRestore(accounts, transactions, clients, suppliers)

        database.withTransaction {
            database.transactionDao().deleteAll()
            database.accountDao().deleteAll()
            database.clientDao().deleteAll()
            database.supplierDao().deleteAll()

            if (accounts.isNotEmpty()) {
                database.accountDao().insertAll(accounts)
            }
            if (transactions.isNotEmpty()) {
                database.transactionDao().insertAll(transactions)
            }
            if (clients.isNotEmpty()) {
                database.clientDao().insertAll(clients)
            }
            if (suppliers.isNotEmpty()) {
                database.supplierDao().insertAll(suppliers)
            }
        }
        BalanceWidgetProvider.requestUpdate(appContext)
    }

    private fun validateRestore(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        clients: List<ClientEntity>,
        suppliers: List<SupplierEntity>
    ) {
        if (accounts.any { it.id <= 0L }) {
            throw IllegalArgumentException("Hay cuentas con id inválido en el respaldo")
        }
        if (accounts.map { it.id }.toSet().size != accounts.size) {
            throw IllegalArgumentException("Hay ids de cuenta duplicados en el respaldo")
        }
        accounts.forEach { a ->
            if (a.type !in AccountType.all) {
                throw IllegalArgumentException("Hay tipos de cuenta inválidos en el respaldo")
            }
            requireMoneyInBounds(a.balance, "Hay saldos de cuenta fuera de rango en el respaldo")
        }

        if (transactions.any { it.id <= 0L }) {
            throw IllegalArgumentException("Hay movimientos con id inválido en el respaldo")
        }
        if (transactions.map { it.id }.toSet().size != transactions.size) {
            throw IllegalArgumentException("Hay ids de movimiento duplicados en el respaldo")
        }

        if (clients.any { it.id <= 0L }) {
            throw IllegalArgumentException("Hay clientes con id inválido en el respaldo")
        }
        if (clients.map { it.id }.toSet().size != clients.size) {
            throw IllegalArgumentException("Hay ids de cliente duplicados en el respaldo")
        }
        clients.forEach { c ->
            if (c.totalDebt < 0L) {
                throw IllegalArgumentException("Hay deudas de cliente inválidas en el respaldo")
            }
            requireMoneyInBounds(c.totalDebt, "Hay deudas de cliente fuera de rango en el respaldo")
        }

        if (suppliers.any { it.id <= 0L }) {
            throw IllegalArgumentException("Hay proveedores con id inválido en el respaldo")
        }
        if (suppliers.map { it.id }.toSet().size != suppliers.size) {
            throw IllegalArgumentException("Hay ids de proveedor duplicados en el respaldo")
        }
        suppliers.forEach { s ->
            if (s.totalPaid < 0L) {
                throw IllegalArgumentException("Hay totales pagados inválidos en el respaldo")
            }
            requireMoneyInBounds(s.totalPaid, "Hay totales pagados fuera de rango en el respaldo")
        }

        val accountIds = accounts.map { it.id }.toSet()
        val clientIds = clients.map { it.id }.toSet()
        val supplierIds = suppliers.map { it.id }.toSet()
        if (accounts.map { it.name.trim().lowercase() }.toSet().size != accounts.size) {
            throw IllegalArgumentException("Hay nombres de cuenta duplicados en el respaldo")
        }
        val validTxTypes = setOf(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
            TransactionType.TRANSFER
        )
        transactions.forEach { tx ->
            if (tx.accountId !in accountIds) {
                throw IllegalArgumentException(
                    "Respaldo inválido: un movimiento referencia una cuenta inexistente"
                )
            }
            if (tx.amount <= 0L) {
                throw IllegalArgumentException("Hay montos inválidos en el respaldo")
            }
            requireMoneyInBounds(tx.amount, "Hay montos fuera de rango en el respaldo")
            if (tx.type !in validTxTypes) {
                throw IllegalArgumentException("Hay tipos de movimiento inválidos en el respaldo")
            }
            tx.relatedClientId?.let { id ->
                if (id !in clientIds) {
                    throw IllegalArgumentException(
                        "Respaldo inválido: un movimiento referencia un cliente inexistente"
                    )
                }
            }
            tx.relatedSupplierId?.let { id ->
                if (id !in supplierIds) {
                    throw IllegalArgumentException(
                        "Respaldo inválido: un movimiento referencia un proveedor inexistente"
                    )
                }
            }
        }

        val transferGroups = transactions
            .filter { it.type == TransactionType.TRANSFER && it.transferGroupId != null }
            .groupBy { it.transferGroupId }
        transferGroups.forEach { (groupId, legs) ->
            if (legs.size != 2) {
                throw IllegalArgumentException(
                    "Respaldo inválido: transferencia incompleta (grupo $groupId)"
                )
            }
            val outbound = legs.count { it.transferIsOutbound == true }
            val inbound = legs.count { it.transferIsOutbound == false }
            if (outbound != 1 || inbound != 1) {
                throw IllegalArgumentException(
                    "Respaldo inválido: piernas de transferencia mal etiquetadas"
                )
            }
        }
    }

    private fun requireMoneyInBounds(cents: Long, message: String) {
        if (abs(cents) > MAX_ABS_CENTS) {
            throw IllegalArgumentException(message)
        }
    }

    private fun requirePassword(password: String) {
        BackupPasswordPolicy.validate(password)?.let { issue ->
            throw IllegalArgumentException(
                context.getString(
                    com.paycontrol.app.R.string.backup_password_invalid,
                    BackupPasswordPolicy.issueMessage(issue, context.resources)
                )
            )
        }
    }

    private fun encryptToEnvelope(plaintext: ByteArray, password: String): JSONObject {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return JSONObject().apply {
            put(KEY_FORMAT, ENC_FORMAT)
            put(KEY_SALT, Base64.getEncoder().encodeToString(salt))
            put(KEY_IV, Base64.getEncoder().encodeToString(iv))
            put(KEY_PAYLOAD, Base64.getEncoder().encodeToString(ciphertext))
        }
    }

    private fun decryptEnvelope(text: String, password: String): ByteArray {
        val envelope = try {
            JSONObject(text)
        } catch (_: Exception) {
            throw IllegalArgumentException("Contraseña incorrecta o archivo dañado")
        }
        if (envelope.optString(KEY_FORMAT) != ENC_FORMAT) {
            throw IllegalArgumentException("Contraseña incorrecta o archivo dañado")
        }
        val salt = decodeBase64Field(envelope, KEY_SALT)
        val iv = decodeBase64Field(envelope, KEY_IV)
        val payload = decodeBase64Field(envelope, KEY_PAYLOAD)
        if (salt.size != SALT_BYTES || iv.size != IV_BYTES || payload.isEmpty()) {
            throw IllegalArgumentException("Contraseña incorrecta o archivo dañado")
        }
        return try {
            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(payload)
        } catch (_: AEADBadTagException) {
            throw IllegalArgumentException("Contraseña incorrecta o archivo dañado")
        } catch (_: Exception) {
            throw IllegalArgumentException("Contraseña incorrecta o archivo dañado")
        }
    }

    private fun decodeBase64Field(envelope: JSONObject, key: String): ByteArray {
        val value = envelope.optString(key, "")
        if (value.isBlank()) {
            throw IllegalArgumentException("Contraseña incorrecta o archivo dañado")
        }
        return try {
            Base64.getDecoder().decode(value)
        } catch (_: Exception) {
            throw IllegalArgumentException("Contraseña incorrecta o archivo dañado")
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            val encoded = factory.generateSecret(spec).encoded
            SecretKeySpec(encoded, "AES")
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun readLimited(input: InputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(8_192)
        var total = 0
        while (true) {
            val n = input.read(chunk)
            if (n < 0) break
            total += n
            if (total > maxBytes) {
                throw IllegalArgumentException(
                    "El archivo de respaldo supera el tamaño máximo permitido"
                )
            }
            out.write(chunk, 0, n)
        }
        return out.toByteArray()
    }

    private fun accountsToJson(accounts: List<AccountEntity>): JSONArray =
        JSONArray().also { arr ->
            accounts.forEach { a ->
                arr.put(
                    JSONObject().apply {
                        put("id", a.id)
                        put("name", a.name)
                        put("balance", a.balance)
                        put("type", a.type)
                    }
                )
            }
        }

    private fun transactionsToJson(transactions: List<TransactionEntity>): JSONArray =
        JSONArray().also { arr ->
            transactions.forEach { t ->
                arr.put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("accountId", t.accountId)
                        put("amount", t.amount)
                        put("type", t.type)
                        put("category", t.category)
                        put("date", t.date)
                        put("note", t.note)
                        putNullableLong("transferGroupId", t.transferGroupId)
                        putNullableBoolean("transferIsOutbound", t.transferIsOutbound)
                        putNullableLong("relatedClientId", t.relatedClientId)
                        putNullableLong("relatedSupplierId", t.relatedSupplierId)
                    }
                )
            }
        }

    private fun clientsToJson(clients: List<ClientEntity>): JSONArray =
        JSONArray().also { arr ->
            clients.forEach { c ->
                arr.put(
                    JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("totalDebt", c.totalDebt)
                    }
                )
            }
        }

    private fun suppliersToJson(suppliers: List<SupplierEntity>): JSONArray =
        JSONArray().also { arr ->
            suppliers.forEach { s ->
                arr.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("phone", s.phone)
                        put("totalPaid", s.totalPaid)
                    }
                )
            }
        }

    private fun parseAccounts(arr: JSONArray): List<AccountEntity> =
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    AccountEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        balance = o.getLong("balance"),
                        type = o.getString("type")
                    )
                )
            }
        }

    private fun parseTransactions(arr: JSONArray): List<TransactionEntity> =
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    TransactionEntity(
                        id = o.getLong("id"),
                        accountId = o.getLong("accountId"),
                        amount = o.getLong("amount"),
                        type = o.getString("type"),
                        category = o.getString("category"),
                        date = o.getLong("date"),
                        note = o.optString("note", ""),
                        transferGroupId = o.nullableLong("transferGroupId"),
                        transferIsOutbound = o.nullableBoolean("transferIsOutbound"),
                        relatedClientId = o.nullableLong("relatedClientId"),
                        relatedSupplierId = o.nullableLong("relatedSupplierId")
                    )
                )
            }
        }

    private fun parseClients(arr: JSONArray): List<ClientEntity> =
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    ClientEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        phone = o.optString("phone", ""),
                        totalDebt = o.getLong("totalDebt")
                    )
                )
            }
        }

    private fun parseSuppliers(arr: JSONArray): List<SupplierEntity> =
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    SupplierEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        phone = o.optString("phone", ""),
                        totalPaid = o.getLong("totalPaid")
                    )
                )
            }
        }

    private fun JSONObject.putNullableLong(key: String, value: Long?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.putNullableBoolean(key: String, value: Boolean?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.nullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return getLong(key)
    }

    private fun JSONObject.nullableBoolean(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return getBoolean(key)
    }

    companion object {
        const val SCHEMA_VERSION = 2
        const val APP_NAME = "DimagPay"

        private const val ENC_FORMAT = "dimagpay-enc-1"
        private const val MAX_BACKUP_BYTES = 15 * 1024 * 1024
        private const val MAX_ABS_CENTS = 9_999_999_999_99L
        private const val PBKDF2_ITERATIONS = 120_000
        private const val KEY_BITS = 256
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

        private const val KEY_SCHEMA_VERSION = "schemaVersion"
        private const val KEY_APP = "app"
        private const val KEY_EXPORTED_AT = "exportedAt"
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_CLIENTS = "clients"
        private const val KEY_SUPPLIERS = "suppliers"
        private const val KEY_FORMAT = "format"
        private const val KEY_SALT = "salt"
        private const val KEY_IV = "iv"
        private const val KEY_PAYLOAD = "payload"
    }
}
