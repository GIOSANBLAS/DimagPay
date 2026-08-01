# PayControl

Aplicación Android nativa offline-first de control financiero personal/comercial.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Clean Architecture / MVVM
- Room (fuente de verdad local)
- StateFlow en ViewModels

## Estructura de paquetes

```
com.paycontrol.app
├── data
│   ├── local
│   │   ├── dao
│   │   ├── entity
│   │   └── AppDatabase.kt
│   └── repository
├── domain
│   ├── model
│   └── util (Money — montos en centavos)
├── di
└── ui
    ├── navigation
    ├── screens (dashboard, transactions, suppliers, clients)
    └── theme
```

## Dinero sin errores de precisión

Los montos se almacenan en Room como `Long` (centavos). El helper `Money` convierte, formatea y opera con `BigDecimal` / aritmética exacta.

## Compilar

```bash
./gradlew :app:assembleDebug
```
