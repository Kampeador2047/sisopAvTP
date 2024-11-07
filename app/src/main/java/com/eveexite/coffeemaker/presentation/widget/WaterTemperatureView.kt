package com.eveexite.coffeemaker.presentation.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView


class WaterTemperatureView : SurfaceView, SurfaceHolder.Callback {

    private var recipientPaint: Paint? = null
    private var tempPaint: Paint? = null
    private var textPaint: Paint? = null

    private var rectContainer: RectF? = null

    private var textSize: Int = 0
    var waterLevel: Int = 0
        set(waterLevel) {
            field = waterLevel

            val c = surfaceHolder!!.lockCanvas()
            c?.let {
                draw(it)
                surfaceHolder!!.unlockCanvasAndPost(it)
            }
        }

    private var offsetStart: Float = 0.toFloat()

    private var surfaceHolder: SurfaceHolder? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    fun init() {

        offsetStart = 150f
        textSize = 30

        recipientPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeWidth = 5f
        }

        textPaint = Paint().apply {
            color = Color.WHITE
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            textSize = textSize.toFloat()
        }

        tempPaint = Paint().apply {
            color = Color.parseColor("#FF0000") // Color rojo para la temperatura
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        rectContainer = RectF()

        setZOrderOnTop(true)
        surfaceHolder = holder
        surfaceHolder!!.setFormat(PixelFormat.TRANSLUCENT)
        surfaceHolder!!.addCallback(this)

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val c = holder.lockCanvas()
        c?.let {
            draw(it)
            holder.unlockCanvasAndPost(it)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //Para limpiar el canvas anterior
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        rectContainer!!.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())

        drawRecipient(canvas)
        drawTemperature(canvas)
    }

    private fun drawRecipient(canvas: Canvas) {
        // Dibujar el recipiente
        canvas.drawRect(offsetStart, 0f, measuredWidth.toFloat() - offsetStart, measuredHeight.toFloat(), recipientPaint!!)

        // Dibujar etiquetas de temperatura
        canvas.drawText("0°C", offsetStart - 75, measuredHeight.toFloat() - 25, textPaint!!)
        canvas.drawText("100°C", offsetStart - 75, 25 + (measuredHeight.toFloat() - 50) * 0, textPaint!!)

    }

    private fun drawTemperature(canvas: Canvas) {

        val tempHeight = measuredHeight.toFloat() * (100 / 100) // la temperatura va de 0 a 100 // em vez del primer 100 tendria que ir la temperatura

        // Asegúrate de que la temperatura no exceda los límites
        val limitedTempHeight = tempHeight.coerceIn(0f, measuredHeight.toFloat() - 25)

        canvas.drawRect(offsetStart + 1, measuredHeight.toFloat() - limitedTempHeight,
            measuredWidth.toFloat() - 6, measuredHeight.toFloat() - 25, tempPaint!!)
    }

}
