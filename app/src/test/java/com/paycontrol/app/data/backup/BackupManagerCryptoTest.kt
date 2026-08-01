package com.paycontrol.app.data.backup

import com.paycontrol.app.domain.util.BackupPasswordPolicy
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import androidx.room.Room
import com.paycontrol.app.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream

/**
 * Pruebas de cifrado de respaldo (sin filtrar la contraseña en asserts de mensaje).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class BackupManagerCryptoTest {

    @Test
    fun wrongPasswordFailsDecrypt() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val manager = BackupManager(context, db)
        val password = "Correcta1!"
        require(BackupPasswordPolicy.validate(password) == null)

        val file = manager.export(password)
        val bytes = file.readBytes()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                manager.import(ByteArrayInputStream(bytes), "Incorrecta1!")
            }
        }
        db.close()
    }

    @Test
    fun roundTripExportImport() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val manager = BackupManager(context, db)
        val password = "Correcta1!"
        val file = manager.export(password)
        assertTrue(file.exists() && file.length() > 0)
        manager.import(file.inputStream(), password)
        db.close()
    }
}
