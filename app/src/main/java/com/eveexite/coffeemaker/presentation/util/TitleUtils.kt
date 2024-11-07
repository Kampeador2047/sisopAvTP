package com.eveexite.coffeemaker.presentation.util

import android.graphics.Paint



object TitleUtils {

    fun textHeight(paint: Paint): Float {
        return (paint.ascent() + paint.descent()) / 2
    }

}
