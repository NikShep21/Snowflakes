package com.example.snowflakes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.AsyncTask
import android.view.View
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
data class Snowflake(
    var x: Float,
    var y: Float,
    val velocity: Float,
    val radius: Float,
    val color: Int,
    var baseX: Float,
    var phase: Float
)

@Suppress("DEPRECATION")
class Snowflakes(ctx: Context) : View(ctx) {
    private var snow = emptyArray<Snowflake>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val snowLock = Any()
    private var moveTask: MoveTask? = null
    private val density = resources.displayMetrics.density

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        synchronized(snowLock) {
            snow = Array(60) {
                val x = Random.nextFloat() * w
                val shade = Random.nextInt(170, 256)
                Snowflake(
                    x = x, y = Random.nextFloat() * h,
                    velocity = (2 + 3 * Random.nextFloat()) * density,
                    radius = (3 + 5 * Random.nextFloat()) * density,
                    color = Color.rgb(shade, shade, 255),
                    baseX = x, phase = Random.nextFloat() * 6.28f
                )
            }
        }
        startSnowfall()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLUE)
        synchronized(snowLock) {
            for (s in snow) {
                paint.color = s.color
                canvas.drawCircle(s.x, s.y, s.radius, paint)
            }
        }
    }

    private fun moveSnowflakes() {
        synchronized(snowLock) {
            for (s in snow) {
                val remaining = (1f - s.y / height).coerceIn(0f, 1f)
                s.y = (s.y + s.velocity * sqrt(remaining)).coerceAtMost(height.toFloat())
                s.phase += 0.035f
                s.x = s.baseX + sin(s.phase) * 18 * density
                if (height - s.y < 1f) {
                    s.y = -s.radius
                    s.baseX = Random.nextFloat() * width
                }
            }
        }
    }

    private fun startSnowfall() {
        if (windowVisibility == VISIBLE && snow.isNotEmpty() && moveTask == null) {
            moveTask = MoveTask().also { it.execute(16) }
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) startSnowfall() else stopSnowfall()
    }

    override fun onDetachedFromWindow() {
        stopSnowfall()
        super.onDetachedFromWindow()
    }

    private fun stopSnowfall() {
        moveTask?.cancel(true)
        moveTask = null
    }

    private inner class MoveTask : AsyncTask<Int, Unit, Unit>() {
        override fun doInBackground(vararg params: Int?) {
            val delay = (params[0] ?: 16).toLong()
            while (!isCancelled) {
                moveSnowflakes()
                publishProgress()
                try {
                    Thread.sleep(delay)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }

        override fun onProgressUpdate(vararg values: Unit?) {
            invalidate()
        }
    }
}
