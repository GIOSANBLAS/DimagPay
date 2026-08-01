# Arquitectura DimagPay

**Build:** `1.3.0-Atlas` (`BuildConfig.BUILD_CODENAME = Atlas`)

## Capas

```
UI (Compose Screens + ViewModels)
        ↓ StateFlow / PagingData
Domain (Money, DateTimeUtils, políticas, UiErrorMapper)
        ↓
Data (Repositories → Room/SQLCipher DAOs, Preferences, BackupManager)
```

- **UI:** pantallas Jetpack Compose; estado efímero (diálogos, drafts, mensajes) vive en ViewModels (`StateFlow`) para sobrevivir rotaciones.
- **Domain:** utilidades puras sin Android (salvo logging Timber). Montos en centavos (`Long`).
- **Data:** `FinanceRepository` / `ClientRepository` / `SupplierRepository` encapsulan transacciones Room atómicas; `BackupManager` cifra AES-GCM; `UserPreferencesRepository` usa EncryptedSharedPreferences.

## Flujo de datos

1. DAOs exponen `Flow` / `PagingSource`.
2. Repositorios aplican reglas de negocio (`require` / `error`) y notifican el widget.
3. ViewModels (Hilt `@HiltViewModel`) exponen `StateFlow` / `PagingData` a la UI.
4. La UI observa con `collectAsStateWithLifecycle` / `collectAsLazyPagingItems`.

## Seguridad

| Área | Enfoque |
|------|---------|
| Base de datos | SQLCipher + passphrase en Android Keystore (`SecureStore`) |
| Preferencias / PIN | EncryptedSharedPreferences; PIN con PBKDF2 (`PinHasher`) |
| Backup | AES-256-GCM + PBKDF2 (120k); contraseña ≥8 con letra/número/símbolo |
| UI | `FLAG_SECURE`; widget oculta saldo si hay PIN |
| Backup sistema | `allowBackup=false` |
| Logs | Timber; nunca contraseñas/PIN/passphrase |

## DI

Dagger Hilt (`@HiltAndroidApp`, `@Module` en `di/AppModule.kt`). `AppViewModelFactory` se mantiene temporalmente como puente; pantallas migran a `hiltViewModel()`.

## Fechas

Almacenadas como **epoch millis UTC** (`Instant`). Visualización y filtros de día usan `DateTimeUtils` + `ZoneId.systemDefault()`.

## Paginación

Listas largas (movimientos, clientes, proveedores, reportes) usan **Paging 3** + `room-paging`. El dashboard conserva un “recientes” acotado.
