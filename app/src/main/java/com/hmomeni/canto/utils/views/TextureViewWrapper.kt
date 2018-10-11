package com.hmomeni.canto.utils.views

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import android.view.View

class TextureViewWrapper : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    var textureView: TextureView? = null
}