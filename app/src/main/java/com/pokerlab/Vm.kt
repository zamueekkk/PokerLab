package com.pokerlab

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Vm(app: Application) : AndroidViewModel(app) {
    private val dir = File(app.filesDir, "histories").also { it.mkdirs() }
    private val df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

    var hands by mutableStateOf<List<Hand>>(emptyList())
        private set
    var filtered by mutableStateOf<List<Hand>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var selectedTour by mutableStateOf("all")
    var dateFrom by mutableStateOf<Long?>(null)
    var dateTo by mutableStateOf<Long?>(null)
    var section by mutableIntStateOf(0)
    var replayIdx by mutableIntStateOf(-1)
    var rpStep by mutableIntStateOf(0)
    var rpData by mutableStateOf<RpData?>(null)
    var sBusy by mutableStateOf(false)
    var sError by mutableStateOf<String?>(null)
    var sResult by mutableStateOf<SolveResult?>(null)
    private var cancelFlag = false

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val list = withContext(Dispatchers.IO) {
                    dir.listFiles()?.filter { it.extension == "txt" }?.flatMap { parseFile(it.readText()) } ?: emptyList()
                }
                hands = list.sortedByDescending { it.ts }
                applyFilter()
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun importUris(uris: List<Uri>) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                withContext(Dispatchers.IO) {
                    for (u in uris) {
                        val name = u.lastPathSegment?.substringAfterLast('/') ?: "hist_${System.currentTimeMillis()}.txt"
                        val f = File(dir, name)
                        app.contentResolver.openInputStream(u)?.use { inp ->
                            f.outputStream().use { out -> inp.copyTo(out) }
                        }
                    }
                }
                reload()
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dir.listFiles()?.forEach { it.delete() } }
            hands = emptyList()
            filtered = emptyList()
        }
    }

    fun applyFilter() {
        var list = hands
        if (selectedTour != "all") {
            list = when (selectedTour) {
                "freeroll" -> list.filter { it.buyIn <= 0 }
                "buyin" -> list.filter { it.buyIn > 0 }
                else -> list.filter { it.tour == selectedTour }
            }
        }
        if (dateFrom != null) list = list.filter { it.ts >= dateFrom!! }
        if (dateTo != null) list = list.filter { it.ts <= dateTo!! }
        filtered = list
    }

    fun openReplay(i: Int) {
        if (i < 0 || i >= filtered.size) return
        replayIdx = i
        rpData = buildReplay(filtered[i])
        rpStep = 0
    }

    fun closeReplay() {
        replayIdx = -1
        rpData = null
        rpStep = 0
        sResult = null
        sError = null
    }

    fun setStep(s: Int) {
        val d = rpData ?: return
        rpStep = s.coerceIn(0, d.steps.lastIndex)
    }

    fun solveCurrent() {
        val d = rpData ?: return
        val step = d.steps.getOrNull(rpStep) ?: return
        if (!rpCanSolve(step)) {
            sError = "Solo spots HU en calle nueva sin apuestas pendientes"
            return
        }
        viewModelScope.launch {
            sBusy = true
            sError = null
            sResult = null
            cancelFlag = false
            try {
                val res = withContext(Dispatchers.Default) {
                    solveCFR(step, cancel = { cancelFlag })
                }
                withContext(Dispatchers.Main) { sResult = res }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { sError = e.message ?: "Error en el solver." }
            } finally {
                withContext(Dispatchers.Main) { sBusy = false }
            }
        }
    }

    fun cancelSolve() { cancelFlag = true }
}
