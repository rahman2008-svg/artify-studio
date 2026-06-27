package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.database.DesignDraft
import com.example.data.database.DesignRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DesignerViewModel(private val repository: DesignRepository) : ViewModel() {

    // Saved draft lists from SQLite database
    val savedDrafts: StateFlow<List<DesignDraft>> = repository.allDrafts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current editing project states
    val currentDraftId = MutableStateFlow<Int?>(null)
    val currentDraftTitle = MutableStateFlow<String>("My Creative Art")
    val canvasWidth = MutableStateFlow<Int>(1080)
    val canvasHeight = MutableStateFlow<Int>(1080)
    val canvasBackgroundColor = MutableStateFlow<Int>(0xFFFFFFFF.toInt())
    
    // Graphic Layers on Canvas
    val canvasElements = MutableStateFlow<List<CanvasElement>>(emptyList())
    
    // Freeform Paintbrush drawing states
    val drawingPaths = MutableStateFlow<List<DrawPath>>(emptyList())
    val redoPaths = MutableStateFlow<List<DrawPath>>(emptyList())
    
    // Active Selected Layer element for dragging, scaling, rotating, editing, deleting
    val selectedElementId = MutableStateFlow<String?>(null)

    // AI prompt optimizer states
    val aiOptimizedPrompt = MutableStateFlow<String>("")
    val isAiLoading = MutableStateFlow<Boolean>(false)

    // Brand Kit palettes (user custom palettes)
    val brandPalettes = MutableStateFlow<List<List<Int>>>(
        listOf(
            listOf(0xFF6366F1.toInt(), 0xFFEC4899.toInt(), 0xFF14B8A6.toInt(), 0xFF0F172A.toInt()), // Cosmic Studio
            listOf(0xFFEF4444.toInt(), 0xFFF59E0B.toInt(), 0xFF10B981.toInt(), 0xFFF8FAFC.toInt()), // Pop Retro
            listOf(0xFF2563EB.toInt(), 0xFF38BDF8.toInt(), 0xFFF1F5F9.toInt(), 0xFF020617.toInt()), // Ocean Breeze
            listOf(0xFF16A34A.toInt(), 0xFF84CC16.toInt(), 0xFFFEF08A.toInt(), 0xFF14532D.toInt())  // Forest Glow
        )
    )

    fun createNewDesign(width: Int, height: Int, bgColor: Int = 0xFFFFFFFF.toInt()) {
        currentDraftId.value = null
        currentDraftTitle.value = "Artify Studio Design"
        canvasWidth.value = width
        canvasHeight.value = height
        canvasBackgroundColor.value = bgColor
        canvasElements.value = emptyList()
        drawingPaths.value = emptyList()
        redoPaths.value = emptyList()
        selectedElementId.value = null
    }

    fun addCustomPalette(colors: List<Int>) {
        val updated = brandPalettes.value.toMutableList()
        updated.add(0, colors)
        brandPalettes.value = updated
    }

    fun loadDraft(draftId: Int) {
        viewModelScope.launch {
            val draft = repository.getDraftById(draftId)
            if (draft != null) {
                currentDraftId.value = draft.id
                currentDraftTitle.value = draft.title
                canvasWidth.value = draft.canvasWidth
                canvasHeight.value = draft.canvasHeight
                canvasBackgroundColor.value = draft.backgroundColor
                canvasElements.value = CanvasSerializer.deserializeElements(draft.serializedElements)
                drawingPaths.value = emptyList() // Drawings are saved as flattened bitmap snapshots, or can be redrawn
                redoPaths.value = emptyList()
                selectedElementId.value = null
            }
        }
    }

    fun saveDraft(context: Context, thumbnailBitmap: Bitmap?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Save thumbnail to app private files for visual display in Dashboard
                var thumbPath: String? = null
                if (thumbnailBitmap != null) {
                    val file = File(context.filesDir, "thumb_${System.currentTimeMillis()}.png")
                    val out = FileOutputStream(file)
                    thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                    out.flush()
                    out.close()
                    thumbPath = file.absolutePath
                }

                val serialized = CanvasSerializer.serializeElements(canvasElements.value)
                val draft = DesignDraft(
                    id = currentDraftId.value ?: 0,
                    title = currentDraftTitle.value,
                    canvasWidth = canvasWidth.value,
                    canvasHeight = canvasHeight.value,
                    backgroundColor = canvasBackgroundColor.value,
                    serializedElements = serialized,
                    thumbnailPath = thumbPath ?: getActiveDraftThumbnailPath(currentDraftId.value),
                    lastUpdated = System.currentTimeMillis()
                )

                val id = repository.insertDraft(draft)
                if (currentDraftId.value == null) {
                    currentDraftId.value = id.toInt()
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    private suspend fun getActiveDraftThumbnailPath(id: Int?): String? {
        if (id == null) return null
        return repository.getDraftById(id)?.thumbnailPath
    }

    fun deleteDraft(draftId: Int) {
        viewModelScope.launch {
            repository.deleteDraftById(draftId)
        }
    }

    fun optimizePrompt(prompt: String) {
        if (prompt.isBlank()) return
        isAiLoading.value = true
        viewModelScope.launch {
            aiOptimizedPrompt.value = GeminiClient.optimizePrompt(prompt)
            isAiLoading.value = false
        }
    }

    // --- Canvas Elements Manipulations ---

    fun addTextElement(text: String, color: Int = 0xFF000000.toInt(), isBold: Boolean = false) {
        val newElem = TextElement(
            x = canvasWidth.value / 2f,
            y = canvasHeight.value / 2f,
            text = text,
            color = color,
            isBold = isBold
        )
        val currentList = canvasElements.value.toMutableList()
        currentList.add(newElem)
        canvasElements.value = currentList
        selectedElementId.value = newElem.id
    }

    fun addShapeElement(shapeType: String, color: Int = 0xFF6366F1.toInt()) {
        val newElem = ShapeElement(
            x = canvasWidth.value / 2f,
            y = canvasHeight.value / 2f,
            shapeType = shapeType,
            color = color
        )
        val currentList = canvasElements.value.toMutableList()
        currentList.add(newElem)
        canvasElements.value = currentList
        selectedElementId.value = newElem.id
    }

    fun addStickerElement(stickerType: String, stickerValue: String, tintColor: Int? = null) {
        val newElem = StickerElement(
            x = canvasWidth.value / 2f,
            y = canvasHeight.value / 2f,
            stickerType = stickerType,
            stickerValue = stickerValue,
            tintColor = tintColor
        )
        val currentList = canvasElements.value.toMutableList()
        currentList.add(newElem)
        canvasElements.value = currentList
        selectedElementId.value = newElem.id
    }

    fun addImageElement(uri: Uri) {
        val newElem = ImageElement(
            x = canvasWidth.value / 2f,
            y = canvasHeight.value / 2f,
            imageUriString = uri.toString()
        )
        val currentList = canvasElements.value.toMutableList()
        currentList.add(newElem)
        canvasElements.value = currentList
        selectedElementId.value = newElem.id
    }

    fun updateElementPosition(id: String, dx: Float, dy: Float) {
        canvasElements.value = canvasElements.value.map { elem ->
            if (elem.id == id) {
                val updated = elem.copyElement()
                updated.x += dx
                updated.y += dy
                updated
            } else {
                elem
            }
        }
    }

    fun updateElementScale(id: String, scaleFactor: Float) {
        canvasElements.value = canvasElements.value.map { elem ->
            if (elem.id == id) {
                val updated = elem.copyElement()
                updated.scale = (updated.scale * scaleFactor).coerceIn(0.1f, 10f)
                updated
            } else {
                elem
            }
        }
    }

    fun updateElementRotation(id: String, rotationDelta: Float) {
        canvasElements.value = canvasElements.value.map { elem ->
            if (elem.id == id) {
                val updated = elem.copyElement()
                updated.rotation = (updated.rotation + rotationDelta) % 360f
                updated
            } else {
                elem
            }
        }
    }

    fun updateElementProperties(id: String, block: (CanvasElement) -> Unit) {
        canvasElements.value = canvasElements.value.map { elem ->
            if (elem.id == id) {
                val updated = elem.copyElement()
                block(updated)
                updated
            } else {
                elem
            }
        }
    }

    fun deleteElement(id: String) {
        canvasElements.value = canvasElements.value.filter { it.id != id }
        if (selectedElementId.value == id) {
            selectedElementId.value = null
        }
    }

    fun bringToFront(id: String) {
        val list = canvasElements.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = list.removeAt(index)
            list.add(item)
            canvasElements.value = list
        }
    }

    fun sendToBack(id: String) {
        val list = canvasElements.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = list.removeAt(index)
            list.add(0, item)
            canvasElements.value = list
        }
    }

    // --- Freehand Sketching brush operations ---

    fun addDrawingPath(path: DrawPath) {
        drawingPaths.value = drawingPaths.value + path
        redoPaths.value = emptyList() // clear redo on new strokes
    }

    fun undoDrawing() {
        val current = drawingPaths.value
        if (current.isNotEmpty()) {
            val last = current.last()
            drawingPaths.value = current.dropLast(1)
            redoPaths.value = redoPaths.value + last
        }
    }

    fun redoDrawing() {
        val currentRedo = redoPaths.value
        if (currentRedo.isNotEmpty()) {
            val last = currentRedo.last()
            redoPaths.value = currentRedo.dropLast(1)
            drawingPaths.value = drawingPaths.value + last
        }
    }

    fun clearDrawing() {
        drawingPaths.value = emptyList()
        redoPaths.value = emptyList()
    }

    // --- Predefined Microsoft Designer Templates ---

    fun applyTemplate(templateName: String) {
        selectedElementId.value = null
        drawingPaths.value = emptyList()
        redoPaths.value = emptyList()

        when (templateName) {
            "Creative Quote" -> {
                canvasWidth.value = 1080
                canvasHeight.value = 1080
                canvasBackgroundColor.value = 0xFF0F172A.toInt() // Dark Slate

                canvasElements.value = listOf(
                    ShapeElement(
                        x = 300f, y = 300f, scale = 1.6f, rotation = -15f,
                        shapeType = "Circle", color = 0x3306B6D4.toInt() // Transparent cyan circle
                    ),
                    TextElement(
                        text = "ARTIFY STUDIO",
                        color = 0xFFEC4899.toInt(), // Pink accent
                        size = 30f, x = 540f, y = 320f,
                        isBold = true, fontStyle = "Monospace"
                    ),
                    TextElement(
                        text = "Where offline art\nmeets creative freedom.",
                        color = 0xFFFFFFFF.toInt(),
                        size = 46f, x = 540f, y = 520f,
                        isBold = true, fontStyle = "Serif"
                    ),
                    TextElement(
                        text = "Designed offline. Powered by you.",
                        color = 0xFF94A3B8.toInt(),
                        size = 24f, x = 540f, y = 740f,
                        isItalic = true, fontStyle = "Sans-Serif"
                    ),
                    ShapeElement(
                        x = 880f, y = 800f, scale = 1.0f, rotation = 45f,
                        shapeType = "Star", color = 0xFFF59E0B.toInt(), isFilled = false
                    )
                )
            }

            "Birthday Greetings" -> {
                canvasWidth.value = 1080
                canvasHeight.value = 1350 // Portrait (4:5)
                canvasBackgroundColor.value = 0xFFFEF2F2.toInt() // Warm peach white

                canvasElements.value = listOf(
                    StickerElement(
                        x = 540f, y = 240f, scale = 2.0f,
                        stickerType = "Emoji", stickerValue = "🎂"
                    ),
                    TextElement(
                        text = "Happy Birthday!",
                        color = 0xFFBE185D.toInt(), // Deep Pink
                        size = 52f, x = 540f, y = 480f,
                        isBold = true, fontStyle = "Cursive"
                    ),
                    TextElement(
                        text = "May your day be filled with vibrant paints, digital canvas magic, and beautiful memories.",
                        color = 0xFF475569.toInt(),
                        size = 30f, x = 540f, y = 680f,
                        fontStyle = "Sans-Serif"
                    ),
                    StickerElement(
                        x = 200f, y = 1000f, scale = 1.5f,
                        stickerType = "Emoji", stickerValue = "🎉"
                    ),
                    StickerElement(
                        x = 880f, y = 1000f, scale = 1.5f,
                        stickerType = "Emoji", stickerValue = "✨"
                    )
                )
            }

            "Minimalist Space" -> {
                canvasWidth.value = 1080
                canvasHeight.value = 1080
                canvasBackgroundColor.value = 0xFF020617.toInt() // Dark space

                canvasElements.value = listOf(
                    ShapeElement(
                        x = 540f, y = 540f, scale = 2.2f,
                        shapeType = "Circle", color = 0xFF1E1B4B.toInt()
                    ),
                    ShapeElement(
                        x = 540f, y = 540f, scale = 1.6f,
                        shapeType = "Circle", color = 0xFF311042.toInt()
                    ),
                    TextElement(
                        text = "L I M I T L E S S",
                        color = 0xFF22D3EE.toInt(), // cyan
                        size = 38f, x = 540f, y = 520f,
                        isBold = true, fontStyle = "Sans-Serif"
                    ),
                    TextElement(
                        text = "ARTIFY STUDIO CLONE",
                        color = 0x88FFFFFF.toInt(),
                        size = 18f, x = 540f, y = 580f,
                        fontStyle = "Monospace"
                    ),
                    StickerElement(
                        x = 540f, y = 840f, scale = 1.2f,
                        stickerType = "Emoji", stickerValue = "🚀"
                    )
                )
            }

            "Business Promo" -> {
                canvasWidth.value = 1200
                canvasHeight.value = 675 // Landscape (16:9)
                canvasBackgroundColor.value = 0xFFF1F5F9.toInt() // Very light gray

                canvasElements.value = listOf(
                    ShapeElement(
                        x = 1000f, y = 337f, scale = 2.5f, rotation = 10f,
                        shapeType = "Square", color = 0xFFEC4899.toInt()
                    ),
                    TextElement(
                        text = "LAUNCH OFFER",
                        color = 0xFF6366F1.toInt(), // Purple indigo
                        size = 28f, x = 350f, y = 180f,
                        isBold = true, fontStyle = "Monospace"
                    ),
                    TextElement(
                        text = "GRAND OPENING",
                        color = 0xFF0F172A.toInt(),
                        size = 54f, x = 350f, y = 290f,
                        isBold = true, fontStyle = "Sans-Serif"
                    ),
                    TextElement(
                        text = "Get 50% discount on all digital canvases.",
                        color = 0xFF475569.toInt(),
                        size = 24f, x = 350f, y = 410f,
                        fontStyle = "Sans-Serif"
                    ),
                    ShapeElement(
                        x = 220f, y = 520f, scale = 0.8f,
                        shapeType = "Square", color = 0xFF0F172A.toInt()
                    ),
                    TextElement(
                        text = "50% OFF",
                        color = 0xFFFFFFFF.toInt(),
                        size = 20f, x = 220f, y = 520f,
                        isBold = true, fontStyle = "Sans-Serif"
                    )
                )
            }
        }
    }
}

class DesignerViewModelFactory(private val repository: DesignRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DesignerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DesignerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
