package br.srmatta.treino

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

const val MP = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** Paleta escura com detalhes em azul e roxo. */
object C {
    val bg = Color.parseColor("#0D0E1A")
    val card = Color.parseColor("#171929")
    val card2 = Color.parseColor("#22253D")
    val blue = Color.parseColor("#3B82F6")
    val blueLight = Color.parseColor("#7BA9FF")
    val purple = Color.parseColor("#8B5CF6")
    val purpleLight = Color.parseColor("#B79CFF")
    val text = Color.parseColor("#E8EAF2")
    val muted = Color.parseColor("#8F94AD")
    val danger = Color.parseColor("#F4577A")
    val ok = Color.parseColor("#34D399")
    val warn = Color.parseColor("#FBBF24")
}

private val PT = Locale("pt", "BR")

val DEFAULT_EXERCISES = listOf(
    "Supino reto", "Supino inclinado", "Crucifixo", "Flexão de braço",
    "Desenvolvimento", "Elevação lateral", "Tríceps pulley", "Tríceps testa",
    "Puxada frontal", "Remada curvada", "Remada baixa", "Barra fixa",
    "Rosca direta", "Rosca martelo", "Agachamento", "Leg press",
    "Cadeira extensora", "Mesa flexora", "Stiff", "Levantamento terra",
    "Avanço", "Elevação pélvica", "Panturrilha em pé", "Abdominal"
)

val REST_OPTIONS = listOf(45, 60, 90, 120, 180)

// ---------- Medidas e layout ----------

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

/** w/h em pixels (ou MP/WRAP/0); margens em dp. */
fun Context.params(
    w: Int = MP, h: Int = WRAP, weight: Float = 0f,
    l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0
): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(w, h, weight).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }

fun Context.rounded(color: Int, radiusDp: Int = 14): GradientDrawable =
    GradientDrawable().apply { setColor(color); cornerRadius = dp(radiusDp).toFloat() }

fun Context.gradientBg(radiusDp: Int = 14): GradientDrawable =
    GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(C.blue, C.purple))
        .apply { cornerRadius = dp(radiusDp).toFloat() }

// ---------- Componentes ----------

fun Context.label(text: String, size: Float = 14f, color: Int = C.text, bold: Boolean = false): TextView =
    TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

fun Context.card(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    background = rounded(C.card, 16)
    setPadding(dp(16), dp(14), dp(16), dp(14))
}

private fun View.pressable() {
    setOnTouchListener { v, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> v.alpha = 0.65f
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.alpha = 1f
        }
        false
    }
}

fun Context.primaryButton(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
    this.text = text
    textSize = 17f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Color.WHITE)
    gravity = Gravity.CENTER
    setPadding(dp(16), dp(15), dp(16), dp(15))
    background = gradientBg(14)
    pressable()
    setOnClickListener { onClick() }
}

fun Context.secondaryButton(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
    this.text = text
    textSize = 15f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(C.text)
    gravity = Gravity.CENTER
    setPadding(dp(16), dp(13), dp(16), dp(13))
    background = rounded(C.card2, 14)
    pressable()
    setOnClickListener { onClick() }
}

fun Context.smallButton(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
    this.text = text
    textSize = 13f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(C.text)
    gravity = Gravity.CENTER
    setPadding(dp(6), dp(11), dp(6), dp(11))
    background = rounded(C.card2, 12)
    pressable()
    setOnClickListener { onClick() }
}

fun Context.stepButton(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
    this.text = text
    textSize = 24f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(C.blueLight)
    gravity = Gravity.CENTER
    background = rounded(C.card2, 12)
    pressable()
    setOnClickListener { onClick() }
}

fun <T : EditText> Context.styled(e: T, hint: String): T {
    e.hint = hint
    e.setHintTextColor(C.muted)
    e.setTextColor(C.text)
    e.textSize = 18f
    e.background = rounded(C.card2, 12)
    e.setPadding(dp(14), dp(10), dp(14), dp(10))
    e.setSingleLine()
    return e
}

// ---------- Formatação ----------

/** Aceita "42,5" e "42.5". */
fun parseNum(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

/** Para exibir: "42,5" (vírgula, sem zeros sobrando). */
fun fmtNum(d: Double): String =
    String.format(PT, "%.2f", d).trimEnd('0').trimEnd(',')

/** Para preencher campos de digitação: "42.5" (ponto, sempre aceito pelo teclado numérico). */
fun fmtInput(d: Double): String =
    String.format(Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')

fun fmtThousands(d: Double): String = String.format(PT, "%,.0f", d)

fun signed(d: Double): String {
    val r = (d * 10).roundToLong() / 10.0
    return when {
        r > 0 -> "+" + fmtNum(r)
        r < 0 -> "−" + fmtNum(-r)
        else -> "0"
    }
}

fun plural(n: Int, one: String, many: String): String = "$n ${if (n == 1) one else many}"

fun fmtClock(totalSec: Int): String {
    val s = if (totalSec < 0) 0 else totalSec
    return if (s >= 3600) String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    else String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

fun fmtDuration(ms: Long): String {
    val m = (ms / 60000).toInt()
    return if (m < 60) "$m min" else "${m / 60}h${String.format(Locale.US, "%02d", m % 60)}"
}

fun fmtWeekday(ms: Long): String =
    SimpleDateFormat("EEEE, dd/MM", PT).format(Date(ms)).replaceFirstChar { it.uppercase() }

fun fmtDay(ms: Long): String = SimpleDateFormat("dd/MM", PT).format(Date(ms))

fun startOfDay(ms: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ms
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun startOfMonth(): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.DAY_OF_MONTH, 1)
    return startOfDay(c.timeInMillis)
}
