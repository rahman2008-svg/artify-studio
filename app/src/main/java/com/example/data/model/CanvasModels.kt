package com.example.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ElementType {
    TEXT, SHAPE, STICKER, IMAGE
}

sealed class CanvasElement {
    abstract val id: String
    abstract val type: ElementType
    abstract var x: Float
    abstract var y: Float
    abstract var scale: Float
    abstract var rotation: Float

    abstract fun copyElement(): CanvasElement
}

data class TextElement(
    override val id: String = UUID.randomUUID().toString(),
    override val type: ElementType = ElementType.TEXT,
    override var x: Float,
    override var y: Float,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    var text: String,
    var color: Int = 0xFFFFFFFF.toInt(),
    var size: Float = 32f,
    var fontStyle: String = "Sans-Serif", // Sans-Serif, Serif, Monospace, Cursive
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var alignment: String = "Center" // Left, Center, Right
) : CanvasElement() {
    override fun copyElement(): TextElement = copy()
}

data class ShapeElement(
    override val id: String = UUID.randomUUID().toString(),
    override val type: ElementType = ElementType.SHAPE,
    override var x: Float,
    override var y: Float,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    var shapeType: String, // Circle, Square, Star, Heart, SpeechBubble, Banner
    var color: Int = 0xFF6366F1.toInt(),
    var isFilled: Boolean = true,
    var strokeWidth: Float = 4f,
    var opacity: Float = 1f
) : CanvasElement() {
    override fun copyElement(): ShapeElement = copy()
}

data class StickerElement(
    override val id: String = UUID.randomUUID().toString(),
    override val type: ElementType = ElementType.STICKER,
    override var x: Float,
    override var y: Float,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    var stickerType: String, // Emoji, Frame, PaintSplat, Ribbon, Sparkle, Arrow
    var stickerValue: String, // Value / Unicode character or asset name
    var tintColor: Int? = null,
    var opacity: Float = 1f
) : CanvasElement() {
    override fun copyElement(): StickerElement = copy()
}

data class ImageElement(
    override val id: String = UUID.randomUUID().toString(),
    override val type: ElementType = ElementType.IMAGE,
    override var x: Float,
    override var y: Float,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    var imageUriString: String, // Device Gallery image content URI
    var filterType: String = "None", // None, Grayscale, Sepia, Invert, Vintage, Warm, Cool
    var opacity: Float = 1f
) : CanvasElement() {
    override fun copyElement(): ImageElement = copy()
}

data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

object CanvasSerializer {
    fun serializeElements(elements: List<CanvasElement>): String {
        val array = JSONArray()
        for (elem in elements) {
            val obj = JSONObject()
            obj.put("id", elem.id)
            obj.put("type", elem.type.name)
            obj.put("x", elem.x.toDouble())
            obj.put("y", elem.y.toDouble())
            obj.put("scale", elem.scale.toDouble())
            obj.put("rotation", elem.rotation.toDouble())

            when (elem) {
                is TextElement -> {
                    obj.put("text", elem.text)
                    obj.put("color", elem.color)
                    obj.put("size", elem.size.toDouble())
                    obj.put("fontStyle", elem.fontStyle)
                    obj.put("isBold", elem.isBold)
                    obj.put("isItalic", elem.isItalic)
                    obj.put("alignment", elem.alignment)
                }
                is ShapeElement -> {
                    obj.put("shapeType", elem.shapeType)
                    obj.put("color", elem.color)
                    obj.put("isFilled", elem.isFilled)
                    obj.put("strokeWidth", elem.strokeWidth.toDouble())
                    obj.put("opacity", elem.opacity.toDouble())
                }
                is StickerElement -> {
                    obj.put("stickerType", elem.stickerType)
                    obj.put("stickerValue", elem.stickerValue)
                    if (elem.tintColor != null) {
                        obj.put("tintColor", elem.tintColor!!)
                    }
                    obj.put("opacity", elem.opacity.toDouble())
                }
                is ImageElement -> {
                    obj.put("imageUriString", elem.imageUriString)
                    obj.put("filterType", elem.filterType)
                    obj.put("opacity", elem.opacity.toDouble())
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeElements(jsonStr: String): List<CanvasElement> {
        val list = mutableListOf<CanvasElement>()
        if (jsonStr.isEmpty()) return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val typeStr = obj.getString("type")
                val type = ElementType.valueOf(typeStr)
                val x = obj.getDouble("x").toFloat()
                val y = obj.getDouble("y").toFloat()
                val scale = obj.optDouble("scale", 1.0).toFloat()
                val rotation = obj.optDouble("rotation", 0.0).toFloat()

                when (type) {
                    ElementType.TEXT -> {
                        list.add(
                            TextElement(
                                id = id,
                                x = x,
                                y = y,
                                scale = scale,
                                rotation = rotation,
                                text = obj.getString("text"),
                                color = obj.getInt("color"),
                                size = obj.getDouble("size").toFloat(),
                                fontStyle = obj.optString("fontStyle", "Sans-Serif"),
                                isBold = obj.optBoolean("isBold", false),
                                isItalic = obj.optBoolean("isItalic", false),
                                alignment = obj.optString("alignment", "Center")
                            )
                        )
                    }
                    ElementType.SHAPE -> {
                        list.add(
                            ShapeElement(
                                id = id,
                                x = x,
                                y = y,
                                scale = scale,
                                rotation = rotation,
                                shapeType = obj.getString("shapeType"),
                                color = obj.getInt("color"),
                                isFilled = obj.optBoolean("isFilled", true),
                                strokeWidth = obj.optDouble("strokeWidth", 4.0).toFloat(),
                                opacity = obj.optDouble("opacity", 1.0).toFloat()
                            )
                        )
                    }
                    ElementType.STICKER -> {
                        val tintVal = if (obj.has("tintColor")) obj.getInt("tintColor") else null
                        list.add(
                            StickerElement(
                                id = id,
                                x = x,
                                y = y,
                                scale = scale,
                                rotation = rotation,
                                stickerType = obj.getString("stickerType"),
                                stickerValue = obj.getString("stickerValue"),
                                tintColor = tintVal,
                                opacity = obj.optDouble("opacity", 1.0).toFloat()
                            )
                        )
                    }
                    ElementType.IMAGE -> {
                        list.add(
                            ImageElement(
                                id = id,
                                x = x,
                                y = y,
                                scale = scale,
                                rotation = rotation,
                                imageUriString = obj.getString("imageUriString"),
                                filterType = obj.optString("filterType", "None"),
                                opacity = obj.optDouble("opacity", 1.0).toFloat()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
