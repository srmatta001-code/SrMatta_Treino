package br.srmatta.treino

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.Vibrator
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import kotlin.math.ceil
import kotlin.math.max

class MainActivity : Activity() {

    private lateinit var db: Db
    private lateinit var content: FrameLayout
    private lateinit var navTabs: List<TextView>
    private val handler = Handler(Looper.getMainLooper())

    // Cronômetros (o de descanso usa o relógio do sistema, então não "anda" errado).
    private var restEnd = 0L
    private var restDone = false
    private var restView: TextView? = null
    private var elapsedView: TextView? = null
    private var sessionStart = 0L
    private var selectedEx: String? = null

    private val prefs by lazy { getSharedPreferences("srmatta", MODE_PRIVATE) }
    private var restSecs: Int
        get() = prefs.getInt("rest", 90)
        set(v) {
            prefs.edit().putInt("rest", v).apply()
        }

    private val ticker = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(C.bg)
        }
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(MP, 0, 1f))
        root.addView(buildNav(), LinearLayout.LayoutParams(MP, WRAP))
        setContentView(root)
        show(0)
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    // ---------- Navegação ----------

    private fun buildNav(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(C.card)
        }
        val names = listOf("Treino", "Histórico", "Evolução")
        navTabs = names.mapIndexed { i, n ->
            TextView(this).apply {
                text = n
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dp(14), 0, dp(14))
                setOnClickListener { show(i) }
            }
        }
        navTabs.forEach { bar.addView(it, LinearLayout.LayoutParams(0, WRAP, 1f)) }
        return bar
    }

    private fun show(i: Int) {
        restView = null
        elapsedView = null
        content.removeAllViews()
        val v = when (i) {
            0 -> trainPage()
            1 -> historyPage()
            else -> progressPage()
        }
        content.addView(v, FrameLayout.LayoutParams(MP, MP))
        navTabs.forEachIndexed { k, t ->
            t.setTextColor(if (k == i) C.blueLight else C.muted)
            t.typeface = if (k == i) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        // Tela acesa enquanto há um treino aberto (para ver o descanso na academia).
        if (db.openSession() != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        tick()
    }

    private fun page(title: String, body: LinearLayout.() -> Unit): View {
        val sv = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        col.addView(label(title, 26f, C.text, true))
        col.body()
        sv.addView(col, FrameLayout.LayoutParams(MP, WRAP))
        return sv
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ---------- Cronômetros ----------

    private fun startRest() {
        restEnd = SystemClock.elapsedRealtime() + restSecs * 1000L
        restDone = false
        tick()
    }

    private fun adjustRest(deltaSec: Int) {
        if (restEnd == 0L) return
        restEnd += deltaSec * 1000L
        tick()
    }

    private fun stopRest() {
        restEnd = 0L
        restDone = false
        tick()
    }

    @Suppress("DEPRECATION")
    private fun buzz() {
        try {
            val vib = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vib.vibrate(longArrayOf(0, 250, 120, 250), -1)
        } catch (_: Exception) {
        }
    }

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        if (restEnd != 0L && now >= restEnd) {
            restEnd = 0L
            restDone = true
            buzz()
        }
        restView?.let { v ->
            when {
                restEnd != 0L -> {
                    val s = ceil((restEnd - now) / 1000.0).toInt()
                    v.text = fmtClock(s)
                    v.setTextColor(if (s <= 10) C.warn else C.purpleLight)
                }
                restDone -> {
                    v.text = "Pronto!"
                    v.setTextColor(C.ok)
                }
                else -> {
                    v.text = "—"
                    v.setTextColor(C.muted)
                }
            }
        }
        elapsedView?.text = fmtClock(((System.currentTimeMillis() - sessionStart) / 1000).toInt())
    }

    // ---------- Aba Treino ----------

    private fun trainPage(): View = page("SrMatta Treino") {
        val s = db.openSession()
        if (s == null) idleSection() else activeSection(s)
    }

    private fun LinearLayout.idleSection() {
        val month = db.countSince(startOfMonth())
        val last = db.history(1).firstOrNull()
        addView(label("Pronto para treinar?", 16f, C.muted), params(t = 2, b = 16))
        addView(card().apply {
            addView(label("Neste mês", 13f, C.muted))
            addView(label(plural(month, "treino", "treinos"), 28f, C.text, true))
            if (last != null) {
                addView(
                    label("Último: ${fmtWeekday(last.started)} · ${plural(last.sets, "série", "séries")}", 13f, C.muted),
                    params(t = 4)
                )
            }
        }, params(b = 16))
        addView(primaryButton("Iniciar treino") {
            db.startSession()
            show(0)
        }, params())
    }

    private fun LinearLayout.activeSection(s: Session) {
        sessionStart = s.started
        val sets = db.setsOf(s.id)

        // Cronômetros: tempo de treino e descanso.
        val timers = card().apply { orientation = LinearLayout.HORIZONTAL }
        val left = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        left.addView(label("Tempo de treino", 12f, C.muted))
        val ev = label("00:00", 26f, C.text, true)
        elapsedView = ev
        left.addView(ev)
        val right = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        right.addView(label("Descanso", 12f, C.muted))
        val rv = label("—", 26f, C.muted, true)
        restView = rv
        right.addView(rv)
        timers.addView(left, params(w = 0, weight = 1f))
        timers.addView(right, params(w = 0, weight = 1f))
        addView(timers, params(t = 10, b = 8))

        val controls = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(smallButton("−15 s") { adjustRest(-15) }, params(w = 0, weight = 1f, r = 6))
        controls.addView(smallButton("+15 s") { adjustRest(15) }, params(w = 0, weight = 1f, r = 6))
        controls.addView(smallButton("Parar") { stopRest() }, params(w = 0, weight = 1f, r = 6))
        var preset: TextView? = null
        preset = smallButton("Tempo: ${restSecs}s") {
            val i = REST_OPTIONS.indexOf(restSecs)
            restSecs = REST_OPTIONS[(i + 1) % REST_OPTIONS.size]
            preset?.text = "Tempo: ${restSecs}s"
        }
        controls.addView(preset, params(w = 0, weight = 1.3f))
        addView(controls, params(b = 12))

        // Formulário da série.
        val names = (db.recentExercises() + DEFAULT_EXERCISES).distinctBy { it.lowercase() }
        val ex = styled(AutoCompleteTextView(context), "Nome do exercício")
        ex.setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, names))
        ex.threshold = 1
        ex.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        ex.setOnClickListener { ex.showDropDown() }

        val kg = styled(EditText(context), "kg")
        kg.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        kg.gravity = Gravity.CENTER
        val reps = styled(EditText(context), "reps")
        reps.inputType = InputType.TYPE_CLASS_NUMBER
        reps.gravity = Gravity.CENTER

        val lastSet = sets.lastOrNull()
        if (lastSet != null) {
            ex.setText(lastSet.exercise)
            kg.setText(fmtInput(lastSet.weight))
            reps.setText(lastSet.reps.toString())
        }
        ex.setOnItemClickListener { _, _, pos, _ ->
            val name = ex.adapter.getItem(pos) as String
            db.lastSetFor(name)?.let {
                kg.setText(fmtInput(it.weight))
                reps.setText(it.reps.toString())
            }
        }

        addView(card().apply {
            addView(label("Exercício", 13f, C.muted))
            addView(ex, params(t = 6, h = dp(48)))
            addView(label("Carga (kg)", 13f, C.muted), params(t = 14))
            addView(stepper(kg, 2.5, true), params(t = 6))
            addView(label("Repetições", 13f, C.muted), params(t = 14))
            addView(stepper(reps, 1.0, false), params(t = 6))
            addView(primaryButton("Registrar série") { register(s.id, ex, kg, reps) }, params(t = 18))
        }, params(b = 12))

        // Séries já feitas hoje, agrupadas por exercício.
        if (sets.isNotEmpty()) {
            addView(label("Séries de hoje", 16f, C.text, true), params(t = 8, b = 8))
            for ((name, list) in sets.groupBy { it.exercise }) {
                addView(card().apply {
                    addView(label(name, 15f, C.blueLight, true))
                    list.forEachIndexed { i, st ->
                        val row = LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        row.addView(label("${i + 1}ª série", 14f, C.muted), params(w = dp(84)))
                        row.addView(label("${fmtNum(st.weight)} kg × ${st.reps}", 16f, C.text, true), params(w = 0, weight = 1f))
                        row.addView(label("×", 24f, C.danger, true).apply {
                            setPadding(dp(14), dp(2), dp(4), dp(2))
                            setOnClickListener {
                                db.deleteSet(st.id)
                                show(0)
                            }
                        })
                        addView(row, params(t = 8))
                    }
                }, params(b = 8))
            }
        }

        addView(secondaryButton("Finalizar treino") { confirmFinish(s.id, sets.size) }, params(t = 16))
    }

    private fun stepper(field: EditText, step: Double, decimal: Boolean): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fun bump(delta: Double) {
            val cur = parseNum(field.text.toString()) ?: 0.0
            val nv = max(0.0, cur + delta)
            field.setText(if (decimal) fmtInput(nv) else nv.toInt().toString())
        }
        row.addView(stepButton("−") { bump(-step) }, params(w = dp(54), h = dp(48)))
        row.addView(field, params(w = 0, h = dp(48), weight = 1f, l = 8, r = 8))
        row.addView(stepButton("+") { bump(step) }, params(w = dp(54), h = dp(48)))
        return row
    }

    /** Usa a grafia já existente do exercício (ignora maiúsculas/minúsculas e espaços extras). */
    private fun canon(raw: String): String {
        val clean = raw.trim().replace(Regex("\\s+"), " ")
        if (clean.isEmpty()) return clean
        val known = db.recentExercises() + DEFAULT_EXERCISES
        return known.firstOrNull { it.equals(clean, ignoreCase = true) }
            ?: clean.replaceFirstChar { it.uppercase() }
    }

    private fun register(sessionId: Long, ex: AutoCompleteTextView, kg: EditText, reps: EditText) {
        val name = canon(ex.text.toString())
        val w = parseNum(kg.text.toString())
        val r = reps.text.toString().trim().toIntOrNull()
        when {
            name.isEmpty() -> toast("Informe o exercício")
            w == null || w < 0 -> toast("Informe a carga (use 0 para peso do corpo)")
            r == null || r <= 0 -> toast("Informe as repetições")
            else -> {
                db.addSet(sessionId, name, r, w)
                startRest()
                show(0)
            }
        }
    }

    private fun confirmFinish(id: Long, nSets: Int) {
        val msg = if (nSets == 0) "Nenhuma série registrada. O treino será descartado."
        else "Salvar este treino com ${plural(nSets, "série", "séries")}?"
        AlertDialog.Builder(this)
            .setTitle("Finalizar treino")
            .setMessage(msg)
            .setPositiveButton(if (nSets == 0) "Descartar" else "Finalizar") { _, _ ->
                db.endSession(id)
                stopRest()
                show(1)
            }
            .setNegativeButton("Continuar", null)
            .show()
    }

    // ---------- Aba Histórico ----------

    private fun historyPage(): View = page("Histórico") {
        val list = db.history()
        if (list.isEmpty()) {
            addView(label("Nenhum treino finalizado ainda.\nComece pela aba Treino.", 15f, C.muted), params(t = 12))
        }
        for (h in list) {
            addView(card().apply {
                addView(label(fmtWeekday(h.started), 17f, C.text, true))
                addView(
                    label(
                        "${fmtDuration(h.ended - h.started)} · ${plural(h.exercises, "exercício", "exercícios")} · ${plural(h.sets, "série", "séries")}",
                        13f, C.muted
                    ), params(t = 2)
                )
                addView(label("Volume: ${fmtThousands(h.volume)} kg", 13f, C.purpleLight), params(t = 6))
                addView(label(h.names.replace(",", ", "), 12f, C.muted).apply {
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                }, params(t = 6))
                setOnClickListener { showSessionDialog(h) }
            }, params(t = 10))
        }
    }

    private fun showSessionDialog(h: Summary) {
        val sb = StringBuilder()
        for ((name, list) in db.setsOf(h.id).groupBy { it.exercise }) {
            sb.append(name).append('\n')
            list.forEachIndexed { i, s -> sb.append("  ${i + 1}. ${fmtNum(s.weight)} kg × ${s.reps}\n") }
            sb.append('\n')
        }
        AlertDialog.Builder(this)
            .setTitle(fmtWeekday(h.started))
            .setMessage(sb.toString().trim())
            .setPositiveButton("Fechar", null)
            .setNegativeButton("Excluir") { _, _ -> confirmDelete(h) }
            .show()
    }

    private fun confirmDelete(h: Summary) {
        AlertDialog.Builder(this)
            .setTitle("Excluir treino?")
            .setMessage("O treino de ${fmtWeekday(h.started)} será apagado. Isso não pode ser desfeito.")
            .setPositiveButton("Excluir") { _, _ ->
                db.deleteSession(h.id)
                show(1)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------- Aba Evolução ----------

    private fun progressPage(): View = page("Evolução") {
        // ----- Peso corporal -----
        addView(label("Peso corporal", 18f, C.text, true), params(t = 16, b = 8))
        val body = db.bodyAll()
        val bodyChart = ChartView(context)
        bodyChart.points = body.map { it.day to it.kg }
        val bodySummary = if (body.isEmpty()) {
            "Sem registros ainda."
        } else {
            val first = body.first()
            val last = body.last()
            var t = "Atual: ${fmtNum(last.kg)} kg"
            if (body.size > 1) t += " · ${signed(last.kg - first.kg)} kg desde ${fmtDay(first.day)}"
            t
        }
        addView(card().apply {
            addView(bodyChart, params(h = dp(180)))
            addView(label(bodySummary, 13f, C.muted), params(t = 8))
        }, params())

        val kgIn = styled(EditText(context), "Seu peso hoje (kg)")
        kgIn.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val addRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        addRow.addView(kgIn, params(w = 0, h = dp(48), weight = 1f, r = 8))
        addRow.addView(smallButton("Salvar") {
            val v = parseNum(kgIn.text.toString())
            if (v == null || v < 20 || v > 400) {
                toast("Informe um peso válido")
            } else {
                db.addBody(v)
                show(2)
            }
        }, params(w = dp(92), h = dp(48)))
        addView(addRow, params(t = 10))

        for (row in body.takeLast(5).reversed()) {
            val line = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            line.addView(label(fmtDay(row.day), 14f, C.muted), params(w = dp(70)))
            line.addView(label("${fmtNum(row.kg)} kg", 15f, C.text, true), params(w = 0, weight = 1f))
            line.addView(label("×", 24f, C.danger, true).apply {
                setPadding(dp(14), dp(2), dp(4), dp(2))
                setOnClickListener {
                    db.deleteBody(row.id)
                    show(2)
                }
            })
            addView(line, params(t = 8))
        }

        // ----- Cargas por exercício -----
        addView(label("Cargas por exercício", 18f, C.text, true), params(t = 28, b = 8))
        val exs = db.exerciseNames()
        if (exs.isEmpty()) {
            addView(label("Registre séries na aba Treino para acompanhar a evolução das cargas.", 14f, C.muted))
            return@page
        }
        if (selectedEx == null || selectedEx !in exs) selectedEx = exs.first()

        val loadChart = ChartView(context)
        val loadSummary = label("", 13f, C.muted)
        fun refresh() {
            val name = selectedEx ?: return
            val pts = db.loadSeries(name)
            loadChart.points = pts
            loadSummary.text = if (pts.isEmpty()) "" else
                "Melhor: ${fmtNum(pts.maxOf { it.second })} kg · Última: ${fmtNum(pts.last().second)} kg · ${plural(pts.size, "treino", "treinos")}"
        }

        val spinner = Spinner(context)
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, exs)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(exs.indexOf(selectedEx), false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (exs[position] != selectedEx) {
                    selectedEx = exs[position]
                    refresh()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        refresh()

        addView(card().apply {
            addView(spinner, params())
            addView(loadChart, params(h = dp(180), t = 8))
            addView(loadSummary, params(t = 8))
        }, params())
    }
}
