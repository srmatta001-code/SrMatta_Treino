package br.srmatta.treino

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View

/** Gráfico de linha desenhado direto no Canvas: sem biblioteca, quase nenhum custo de memória. */
class ChartView(ctx: Context) : View(ctx) {

    /** Pares (data em ms, valor). Os pontos são espaçados igualmente. */
    var points: List<Pair<Long, Double>> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ctx.dp(3).toFloat()
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint = Paint().apply {
        color = C.card2
        strokeWidth = ctx.dp(1).toFloat()
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C.muted
        textSize = ctx.dp(11).toFloat()
    }

    private fun px(v: Int): Float = context.dp(v).toFloat()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val pts = points

        if (pts.isEmpty()) {
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText("Sem dados ainda", w / 2, h / 2, textPaint)
            return
        }

        val vals = pts.map { it.second }
        var lo = vals.minOrNull() ?: 0.0
        var hi = vals.maxOrNull() ?: 0.0
        if (hi - lo < 0.001) {
            lo -= 1.0
            hi += 1.0
        }

        val x0 = px(8)
        val x1 = w - px(16)
        val y0 = px(20)
        val y1 = h - px(24)
        val n = pts.size

        // Linhas de grade: máximo, meio e mínimo (valores reais da série).
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = C.muted
        for (i in 0..2) {
            val y = y0 + (y1 - y0) * i / 2f
            canvas.drawLine(x0, y, x1, y, gridPaint)
            val v = hi - (hi - lo) * i / 2.0
            canvas.drawText(fmtNum(v), x0 + px(2), y - px(3), textPaint)
        }

        fun xAt(i: Int): Float = if (n == 1) (x0 + x1) / 2f else x0 + (x1 - x0) * i / (n - 1).toFloat()
        fun yAt(v: Double): Float = (y1 - (y1 - y0) * ((v - lo) / (hi - lo))).toFloat()

        if (n >= 2) {
            val path = Path()
            for (i in 0 until n) {
                val x = xAt(i)
                val y = yAt(pts[i].second)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            linePaint.shader = LinearGradient(x0, 0f, x1, 0f, C.blue, C.purple, Shader.TileMode.CLAMP)
            canvas.drawPath(path, linePaint)
        }

        if (n <= 40) {
            dotPaint.color = C.purpleLight
            for (i in 0 until n) canvas.drawCircle(xAt(i), yAt(pts[i].second), px(3), dotPaint)
        }

        // Destaque do último ponto, com o valor.
        val lx = xAt(n - 1)
        val ly = yAt(pts[n - 1].second)
        dotPaint.color = C.blueLight
        canvas.drawCircle(lx, ly, px(5), dotPaint)
        textPaint.color = C.text
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textAlign = if (n > 1) Paint.Align.RIGHT else Paint.Align.CENTER
        canvas.drawText(fmtNum(pts[n - 1].second), if (n > 1) lx + px(4) else lx, ly - px(9), textPaint)

        // Datas do primeiro e do último ponto.
        textPaint.color = C.muted
        textPaint.typeface = Typeface.DEFAULT
        if (n == 1) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(fmtDay(pts[0].first), (x0 + x1) / 2f, h - px(6), textPaint)
        } else {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(fmtDay(pts[0].first), x0, h - px(6), textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(fmtDay(pts[n - 1].first), x1, h - px(6), textPaint)
        }
    }
}
