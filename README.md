# DimagPay

Aplicación Android nativa offline-first de control financiero personal y comercial.

**Producto:** Giosánblas · **Arquitectura:** Auto · **Build:** 1.3.0-Atlas

© Giosánblas. Todos los derechos reservados.

DimagPay es una herramienta de registro. No constituye asesoría financiera, contable ni fiscal.
El uso de la app y la exactitud de los datos son responsabilidad del usuario.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- MVVM + Room (SQLCipher) + StateFlow + Paging 3
- Hilt, Timber (`AppLog`), WorkManager
- Montos en centavos (`Money`) para precisión exacta

## Módulos

- Dashboard, movimientos, proveedores, clientes (CxC)
- Cuentas + transferencias entre cuentas
- Reportes (filtros, gráfica, CSV, Paging)
- PIN / biometría, respaldo cifrado, recordatorios, widget

## Compilar (debug)

```bash
.\gradlew.bat :app:installDebug
```

## Firma opcional (APK/AAB local)

No se publica en Play Store por defecto. Si quieres un release firmado en tu máquina:

1. Copia `keystore.properties.example` → `keystore.properties` (gitignored).
2. Genera tu propio keystore (no subas `.jks` ni contraseñas al repo).
3. Ejecuta:

```bash
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:bundleRelease
```

## Identidad

- Nombre visible: **DimagPay**
- `applicationId`: `com.paycontrol.app` (interno; no cambia instalaciones existentes)

## Documentación

Ver [ARCHITECTURE.md](ARCHITECTURE.md).
