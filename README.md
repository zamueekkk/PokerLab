# Poker Lab · Android (Kotlin + Jetpack Compose)

Port nativo de `poker-lab.html`: analizador de torneos CoinPoker que corre 100% en el teléfono.

## Cómo generar el APK
1. Abre la carpeta `PokerLab` con **Android Studio** (Koala 2024.1 o más nuevo; usa JDK 17, ya incluido).
2. Deja que sincronice Gradle (descarga Gradle 8.7 y las dependencias; necesita internet la primera vez).
3. Menú **Build ▸ Build Bundle(s) / APK(s) ▸ Build APK(s)**.
   El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`.
   (Para un APK firmado: **Build ▸ Generate Signed App Bundle / APK**.)
4. Instálalo en el teléfono (activa "instalar apps desconocidas") o ejecútalo directo con ▶.

**O usa GitHub Actions** (recomendado): ve a la pestaña *Actions* del repositorio, ejecuta el workflow **Build APK** y descarga el artefacto.

Requisitos del dispositivo: Android 8.0 (API 26) o superior.

## Uso
- Botón **＋** (barra superior): elige uno o varios `.txt` de historial de CoinPoker. Se guardan en el
  almacenamiento privado de la app y se recargan solos al abrirla (menú ⋮ ▸ Borrar historial para limpiarlos).
- Menú ☰: las 20 secciones (Resumen, Nivel, Perfil, Preflop, Postflop, All-ins & EV, Solver, Fugas, ...).
- Filtros arriba: torneo (todos / freerolls / con buy-in / uno concreto) y rango de fechas.
- Toca cualquier mano para repetirla en la mesa (⏮ ◀ ▶ ⏭, reproducción automática, deslizador).
  En una calle con 2 jugadores aparece "Solver CFR HU · revisar spot".
- ⋮ ▸ Exportar CSV del filtro actual.

## Estructura (app/src/main/java/com/pokerlab)
- `Parser.kt`      lee el historial y calcula los contadores de cada mano
- `Stats.kt`       tabla de estadísticas con rangos de referencia, Wilson, nivel estimado, alertas
- `Equity.kt`      evaluador de 7 cartas, equity multiway con side pots, EV de all-ins
- `Solver.kt`      solver CFR+ heads-up nativo (reemplaza al TexasSolver externo del HTML)
- `Replay.kt`      reconstrucción paso a paso de la mano
- `Charts.kt`      gráficas en Canvas (líneas, barras, donut, radar, heatmaps, mesa)
- `Components.kt`  componentes (secciones, KPIs, tablas, cartas, repetidor)
- `Tabs1/2/3.kt`   las pestañas
- `Vm.kt`, `MainActivity.kt`  estado, navegación, importación
