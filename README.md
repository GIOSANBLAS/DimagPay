# DimagPay

Aplicación Android nativa offline-first de control financiero personal/comercial.

**Producto:** Giosánblas · **Arquitectura:** Auto

## Stack

- Kotlin + Jetpack Compose (Material 3)
- MVVM + Room (SQLCipher) + StateFlow
- Montos en centavos (`Money`) para precisión exacta

## Módulos

- Dashboard, movimientos, proveedores, clientes (CxC)
- Cuentas + transferencias entre cuentas
- Reportes (filtros, gráfica, CSV)
- PIN / biometría, respaldo JSON, recordatorios, widget

## Compilar

```bash
.\gradlew.bat :app:installDebug
.\gradlew.bat :app:bundleRelease   # requiere keystore.properties
```

## Identidad

- Nombre visible: **DimagPay**
- `applicationId`: `com.paycontrol.app` (interno; no cambia instalaciones existentes)
