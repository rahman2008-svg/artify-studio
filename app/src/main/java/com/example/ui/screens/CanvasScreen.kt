package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.applyCanvas
import coil.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin
import com.example.data.model.*
import com.example.ui.canvas.CanvasExporter
import com.example.ui.viewmodel.DesignerViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: DesignerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val elements by viewModel.canvasElements.collectAsState()
    val drawingPaths by viewModel.drawingPaths.collectAsState()
    val selectedId by viewModel.selectedElementId.collectAsState()
    val width by viewModel.canvasWidth.collectAsState()
    val height by viewModel.canvasHeight.collectAsState()
    val bgColor by viewModel.canvasBackgroundColor.collectAsState()
    val title by viewModel.currentDraftTitle.collectAsState()

    // Editor tab states
    var activeTab by remember { mutableStateOf("Layers") } // Layers, Text, Shapes, Stickers, Image, Brush, CanvasBG
    
    // Brush tools states
    var brushColor by remember { mutableStateOf(Color.Red) }
    var brushWidth by remember { mutableStateOf(10f) }
    var isEraserActive by remember { mutableStateOf(false) }

    // Text adder inputs
    var inputAddText by remember { mutableStateOf("New Text") }
    var textSelectedColor by remember { mutableStateOf(0xFFFFFFFF.toInt()) }
    var textSelectedFont by remember { mutableStateOf("Sans-Serif") }

    // Shape creator states
    var shapeSelectedColor by remember { mutableStateOf(0xFF6366F1.toInt()) }
    var isShapeFilled by remember { mutableStateOf(true) }

    // Sticker adder inputs
    var stickerTypeTab by remember { mutableStateOf("Emojis") }

    // Dialog state for export success
    var showSuccessDialog by remember { mutableStateOf(false) }
    var savedFilePath by remember { mutableStateOf("") }
    var isSavingInProgress by remember { mutableStateOf(false) }

    // System Image Pick Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addImageElement(uri)
            Toast.makeText(context, "Image added to design canvas!", Toast.LENGTH_SHORT).show()
        }
    }

    // Background Photo Launcher
    val bgPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Set as image background layer or customize
            viewModel.addImageElement(uri)
            viewModel.sendToBack(elements.lastOrNull()?.id ?: "")
            Toast.makeText(context, "Photo set as canvas background layer!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.currentDraftTitle.value = it },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(52.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF1E293B), fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF64748B)
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
                    }
                },
                actions = {
                    // Save draft locally
                    IconButton(
                        onClick = {
                            viewModel.saveDraft(context, null) { success ->
                                if (success) {
                                    Toast.makeText(context, "Draft Saved Offline!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to save draft", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Draft", tint = Color(0xFF1E293B))
                    }

                    // EXPORT TO GALLERY (MOST IMPORTANT REQ!)
                    Button(
                        onClick = {
                            isSavingInProgress = true
                            CanvasExporter.exportToStorage(
                                context = context,
                                title = title,
                                width = width,
                                height = height,
                                bgColor = bgColor,
                                elements = elements,
                                drawingPaths = drawingPaths
                            ) { path, err ->
                                isSavingInProgress = false
                                if (path != null) {
                                    savedFilePath = path
                                    showSuccessDialog = true
                                } else {
                                    Toast.makeText(context, "Export failed: $err", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFDFBFF))
            )
        },
        containerColor = Color(0xFFFDFBFF)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Loading Overlay for saving
            if (isSavingInProgress) {
                LinearProgressIndicator(
                    color = Color(0xFF4F46E5),
                    trackColor = Color(0xFFE2E8F0),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 1. Drawing Canvas Area
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .background(Color(0xFFF1F5F9))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Calculate display size dynamically to preserve original project aspect ratio
                val containerWidth = maxWidth.value
                val containerHeight = maxHeight.value
                val projectRatio = width.toFloat() / height

                val displayWidth: Float
                val displayHeight: Float

                if (containerWidth / containerHeight > projectRatio) {
                    displayHeight = containerHeight - 20
                    displayWidth = displayHeight * projectRatio
                } else {
                    displayWidth = containerWidth - 20
                    displayHeight = displayWidth / projectRatio
                }

                // Scaling factors between native high-definition coordinates and interactive UI screen pixels
                val scaleX = displayWidth / width
                val scaleY = displayHeight / height

                // The Interactive Canvas Board
                Box(
                    modifier = Modifier
                        .size(displayWidth.dp, displayHeight.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(bgColor))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        .pointerInput(activeTab) {
                            if (activeTab == "Brush") {
                                // Draw paths gestures
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        // Translate display offset back to project coordinates
                                        val actualPt = Offset(offset.x / scaleX, offset.y / scaleY)
                                        viewModel.addDrawingPath(
                                            DrawPath(
                                                points = listOf(actualPt),
                                                color = if (isEraserActive) Color.Transparent else brushColor,
                                                strokeWidth = brushWidth,
                                                isEraser = isEraserActive
                                            )
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        val currentPaths = viewModel.drawingPaths.value
                                        if (currentPaths.isNotEmpty()) {
                                            val lastPath = currentPaths.last()
                                            val nextPt = change.position
                                            val actualPt =
                                                Offset(nextPt.x / scaleX, nextPt.y / scaleY)
                                            val updatedPoints = lastPath.points + actualPt
                                            
                                            // replace last path
                                            viewModel.drawingPaths.value = currentPaths.dropLast(1) + lastPath.copy(points = updatedPoints)
                                        }
                                    }
                                )
                            }
                        }
                        .clickable(enabled = activeTab != "Brush") {
                            // deselect elements on background tap
                            viewModel.selectedElementId.value = null
                        }
                ) {
                    // Draw Canvas Elements (Texts, Shapes, Stickers, Images)
                    elements.forEach { elem ->
                        val isSelected = elem.id == selectedId

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (elem.x * scaleX - (60 * elem.scale * scaleX)).dp,
                                    y = (elem.y * scaleY - (60 * elem.scale * scaleY)).dp
                                )
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            viewModel.selectedElementId.value = elem.id
                                        },
                                        onDrag = { change, dragAmount ->
                                            // Scale back touch increments to native layout dimensions
                                            viewModel.updateElementPosition(
                                                elem.id,
                                                dragAmount.x / scaleX,
                                                dragAmount.y / scaleY
                                            )
                                        }
                                    )
                                }
                                .clickable {
                                    viewModel.selectedElementId.value = elem.id
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) Color(0xFF4F46E5) else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = (120 * elem.scale).dp,
                                        height = (120 * elem.scale).dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when (elem) {
                                    is ImageElement -> {
                                        AsyncImage(
                                            model = elem.imageUriString,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit,
                                            alpha = elem.opacity
                                        )
                                    }
                                    is ShapeElement -> {
                                        RenderShape(elem)
                                    }
                                    is StickerElement -> {
                                        Text(
                                            text = elem.stickerValue,
                                            fontSize = 42.sp,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                    is TextElement -> {
                                        RenderText(elem)
                                    }
                                }
                            }
                        }
                    }

                    // Freehand Brush Sketch Layers Overlay
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        for (drawPath in drawingPaths) {
                            if (drawPath.points.size < 2) continue
                            val path = androidx.compose.ui.graphics.Path()
                            val start = drawPath.points.first()
                            path.moveTo(start.x * scaleX, start.y * scaleY)
                            for (i in 1 until drawPath.points.size) {
                                val pt = drawPath.points[i]
                                path.lineTo(pt.x * scaleX, pt.y * scaleY)
                            }
                            drawPath(
                                path = path,
                                color = drawPath.color,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = drawPath.strokeWidth * scaleX,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }

            // 2. Selected Element Manipulator Handles (Scale/Rotate/Forward/Delete)
            AnimatedVisibility(
                visible = selectedId != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedId?.let { selId ->
                    val elem = elements.find { it.id == selId }
                    if (elem != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Scale Down
                                IconButton(onClick = { viewModel.updateElementScale(selId, 0.9f) }) {
                                    Icon(Icons.Default.ZoomOut, contentDescription = "Shrink", tint = Color(0xFF1E293B))
                                }
                                // Scale Up
                                IconButton(onClick = { viewModel.updateElementScale(selId, 1.1f) }) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Enlarge", tint = Color(0xFF1E293B))
                                }
                                // Rotate Left
                                IconButton(onClick = { viewModel.updateElementRotation(selId, -15f) }) {
                                    Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left", tint = Color(0xFF1E293B))
                                }
                                // Rotate Right
                                IconButton(onClick = { viewModel.updateElementRotation(selId, 15f) }) {
                                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right", tint = Color(0xFF1E293B))
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Layers forward
                                TextButton(onClick = { viewModel.bringToFront(selId) }) {
                                    Text("Front", color = Color(0xFF4F46E5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                // Layers backward
                                TextButton(onClick = { viewModel.sendToBack(selId) }) {
                                    Text("Back", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                // Delete
                                IconButton(onClick = { viewModel.deleteElement(selId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Layer", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Bottom Action Workstations / Tab Editor Panels
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    "Layers" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Canvas Graphic Layers", color = Color(0xFF1E293B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${elements.size} items", color = Color(0xFF64748B), fontSize = 11.sp)
                            }

                            if (elements.isEmpty()) {
                                Text(
                                    text = "Your canvas is empty. Tap any tools below (Text, Shapes, Stickers, Photos) to design!",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(elements) { elem ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (elem.id == selectedId) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                                .clickable { viewModel.selectedElementId.value = elem.id }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = when (elem) {
                                                    is TextElement -> "Text: \"${elem.text.take(10)}\""
                                                    is ShapeElement -> "${elem.shapeType} Shape"
                                                    is StickerElement -> "Sticker ${elem.stickerValue}"
                                                    is ImageElement -> "Imported Photo"
                                                },
                                                color = if (elem.id == selectedId) Color.White else Color(0xFF475569),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Text" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = inputAddText,
                                    onValueChange = { inputAddText = it },
                                    placeholder = { Text("Enter text...") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4F46E5),
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedTextColor = Color(0xFF1E293B),
                                        unfocusedTextColor = Color(0xFF1E293B),
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC)
                                    )
                                )
                                Button(
                                    onClick = {
                                        viewModel.addTextElement(inputAddText, textSelectedColor, isBold = true)
                                        inputAddText = "New Text"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                                ) {
                                    Text("Add")
                                }
                            }

                            // Properties tweak if selected is Text
                            val selElem = elements.find { it.id == selectedId }
                            if (selElem is TextElement) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Alignments
                                    Text("Align:", color = Color(0xFF1E293B), fontSize = 11.sp)
                                    listOf("Left", "Center", "Right").forEach { align ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (selElem.alignment == align) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                                .clickable {
                                                    viewModel.updateElementProperties(selElem.id) { elem ->
                                                        (elem as TextElement).alignment = align
                                                    }
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(align, color = if (selElem.alignment == align) Color.White else Color(0xFF475569), fontSize = 9.sp)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    // Color selector
                                    listOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFEF4444.toInt(), 0xFFF59E0B.toInt(), 0xFF10B981.toInt(), 0xFF3B82F6.toInt()).forEach { col ->
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(col))
                                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                                .clickable {
                                                    viewModel.updateElementProperties(selElem.id) { elem ->
                                                        (elem as TextElement).color = col
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "Shapes" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Draw Vector Shapes", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("Circle", "Square", "Star", "Heart", "SpeechBubble").forEach { shape ->
                                    Button(
                                        onClick = { viewModel.addShapeElement(shape, shapeSelectedColor) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(shape, fontSize = 10.sp, color = Color(0xFF1E293B))
                                    }
                                }
                            }

                            // Customizing if selected is shape
                            val selElem = elements.find { it.id == selectedId }
                            if (selElem is ShapeElement) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Color:", color = Color(0xFF1E293B), fontSize = 11.sp)
                                    listOf(0xFF6366F1.toInt(), 0xFF4F46E5.toInt(), 0xFF14B8A6.toInt(), 0xFFEF4444.toInt(), 0xFFF59E0B.toInt()).forEach { col ->
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(col))
                                                .clickable {
                                                    viewModel.updateElementProperties(selElem.id) { elem ->
                                                        (elem as ShapeElement).color = col
                                                    }
                                                }
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Fill vs stroke
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (selElem.isFilled) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                            .clickable {
                                                viewModel.updateElementProperties(selElem.id) { elem ->
                                                    (elem as ShapeElement).isFilled = true
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Fill", color = if (selElem.isFilled) Color.White else Color(0xFF475569), fontSize = 10.sp)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (!selElem.isFilled) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                            .clickable {
                                                viewModel.updateElementProperties(selElem.id) { elem ->
                                                    (elem as ShapeElement).isFilled = false
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Stroke", color = if (!selElem.isFilled) Color.White else Color(0xFF475569), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    "Stickers" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("Emojis", "Badges", "Sparkles").forEach { tab ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (stickerTypeTab == tab) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                            .clickable { stickerTypeTab = tab }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(tab, color = if (stickerTypeTab == tab) Color.White else Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            val stickersList = when (stickerTypeTab) {
                                "Emojis" -> listOf("😃", "🚀", "🌟", "🔥", "🎨", "👑", "🍕", "🎸", "💡", "💖", "🎉", "🎈")
                                "Badges" -> listOf("💯", "✅", "⚠️", "🌀", "💠", "💮", "🔰", "🔱", "🆒", "🆕", "🆗", "🆙")
                                else -> listOf("✨", "💫", "⭐", "💥", "🌈", "☀️", "🌙", "☁️", "⚡", "🍀", "🌸", "🍁")
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                items(stickersList) { sticker ->
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .clickable { viewModel.addStickerElement("Emoji", sticker) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(sticker, fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                    }

                    "Photos" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Local Photo")
                                }
                            }

                            // Color Filter Controls for Selected Image
                            val selElem = elements.find { it.id == selectedId }
                            if (selElem is ImageElement) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Apply Photo Filter Effect:", color = Color(0xFF1E293B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listOf("None", "Grayscale", "Sepia", "Invert", "Vintage", "Warm", "Cool")) { filter ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (selElem.filterType == filter) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                                    .clickable {
                                                        viewModel.updateElementProperties(selElem.id) { elem ->
                                                            (elem as ImageElement).filterType = filter
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(filter, color = if (selElem.filterType == filter) Color.White else Color(0xFF475569), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Brush" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Brush, contentDescription = null, tint = Color(0xFF4F46E5))
                                    Text("Paint Brush Board", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Undo
                                    IconButton(onClick = { viewModel.undoDrawing() }) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color(0xFF1E293B))
                                    }
                                    // Redo
                                    IconButton(onClick = { viewModel.redoDrawing() }) {
                                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = Color(0xFF1E293B))
                                    }
                                    // Clear Brush sketches
                                    TextButton(onClick = { viewModel.clearDrawing() }) {
                                        Text("Clear Brush", color = Color.Red, fontSize = 11.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Eraser:", color = Color(0xFF1E293B), fontSize = 11.sp)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isEraserActive) Color.Red else Color(0xFFF1F5F9))
                                        .clickable { isEraserActive = !isEraserActive }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(if (isEraserActive) "Eraser ON" else "Eraser OFF", color = if (isEraserActive) Color.White else Color(0xFF475569), fontSize = 10.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text("Color:", color = Color(0xFF1E293B), fontSize = 11.sp)
                                listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.White, Color.Black).forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (brushColor == col && !isEraserActive) 1.5.dp else 0.dp,
                                                color = Color(0xFF94A3B8),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                brushColor = col
                                                isEraserActive = false
                                            }
                                    )
                                }
                            }
                        }
                    }

                    "CanvasBG" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Canvas Background Customizer", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Presets:", color = Color(0xFF1E293B), fontSize = 11.sp)
                                listOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF0F172A.toInt(), 0xFFFEF2F2.toInt(), 0xFFFFFBEB.toInt(), 0xFFECFDF5.toInt()).forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(col))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                                            .clickable { viewModel.canvasBackgroundColor.value = col }
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Button(
                                    onClick = { bgPhotoPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("BG Photo", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Main Nav Footer Tabs / Buttons for Graphic Workstations
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                WorkstationTabButton(
                    title = "Layers",
                    icon = Icons.Default.Layers,
                    isActive = activeTab == "Layers"
                ) { activeTab = "Layers" }

                WorkstationTabButton(
                    title = "Text",
                    icon = Icons.Default.TextFields,
                    isActive = activeTab == "Text"
                ) { activeTab = "Text" }

                WorkstationTabButton(
                    title = "Shapes",
                    icon = Icons.Default.Category,
                    isActive = activeTab == "Shapes"
                ) { activeTab = "Shapes" }

                WorkstationTabButton(
                    title = "Stickers",
                    icon = Icons.Default.EmojiEmotions,
                    isActive = activeTab == "Stickers"
                ) { activeTab = "Stickers" }

                WorkstationTabButton(
                    title = "Photos",
                    icon = Icons.Default.AddPhotoAlternate,
                    isActive = activeTab == "Photos"
                ) { activeTab = "Photos" }

                WorkstationTabButton(
                    title = "Brush",
                    icon = Icons.Default.Brush,
                    isActive = activeTab == "Brush"
                ) { activeTab = "Brush" }

                WorkstationTabButton(
                    title = "BG",
                    icon = Icons.Default.AspectRatio,
                    isActive = activeTab == "CanvasBG"
                ) { activeTab = "CanvasBG" }
            }
        }
    }

    // Gorgeous Success Dialog when saved to storage (User requirement!)
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(28.dp))
                    Text("Saved to Phone Storage!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your design has been rendered in high definition and successfully saved into your phone's public gallery!",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Destination folder:\n$savedFilePath",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Awesome")
                }
            }
        )
    }
}

@Composable
fun RenderShape(elem: ShapeElement) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val size = size.minDimension
        
        when (elem.shapeType) {
            "Circle" -> {
                drawCircle(
                    color = Color(elem.color),
                    radius = size / 2f,
                    center = center,
                    style = if (elem.isFilled) androidx.compose.ui.graphics.drawscope.Fill else androidx.compose.ui.graphics.drawscope.Stroke(elem.strokeWidth)
                )
            }
            "Square" -> {
                drawRect(
                    color = Color(elem.color),
                    topLeft = Offset(center.x - size / 2, center.y - size / 2),
                    size = androidx.compose.ui.geometry.Size(size, size),
                    style = if (elem.isFilled) androidx.compose.ui.graphics.drawscope.Fill else androidx.compose.ui.graphics.drawscope.Stroke(elem.strokeWidth)
                )
            }
            "Star" -> {
                val path = androidx.compose.ui.graphics.Path()
                val outerRadius = size / 2f
                val innerRadius = size / 4f
                val pointsCount = 5
                var angle = -Math.PI / 2

                for (i in 0 until pointsCount * 2) {
                    val r = if (i % 2 == 0) outerRadius else innerRadius
                    val currX = (center.x + r * cos(angle)).toFloat()
                    val currY = (center.y + r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(currX, currY) else path.lineTo(currX, currY)
                    angle += Math.PI / pointsCount
                }
                path.close()
                drawPath(
                    path = path,
                    color = Color(elem.color),
                    style = if (elem.isFilled) androidx.compose.ui.graphics.drawscope.Fill else androidx.compose.ui.graphics.drawscope.Stroke(elem.strokeWidth)
                )
            }
            "Heart" -> {
                val path = androidx.compose.ui.graphics.Path()
                val hSize = size * 0.8f
                val cx = center.x
                val cy = center.y
                path.moveTo(cx, cy - hSize / 4f)
                path.cubicTo(cx - hSize / 2f, cy - hSize * 0.8f, cx - hSize, cy - hSize / 3f, cx, cy + hSize * 0.7f)
                path.cubicTo(cx + hSize, cy - hSize / 3f, cx + hSize / 2f, cy - hSize * 0.8f, cx, cy - hSize / 4f)
                path.close()
                drawPath(
                    path = path,
                    color = Color(elem.color),
                    style = if (elem.isFilled) androidx.compose.ui.graphics.drawscope.Fill else androidx.compose.ui.graphics.drawscope.Stroke(elem.strokeWidth)
                )
            }
            "SpeechBubble" -> {
                val path = androidx.compose.ui.graphics.Path()
                val cx = center.x
                val cy = center.y
                val w = size * 0.8f
                val h = size * 0.6f
                path.addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = cx - w/2,
                        top = cy - h/2,
                        right = cx + w/2,
                        bottom = cy + h/2,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )
                )
                // draw bubble tail
                path.moveTo(cx - 10f, cy + h/2)
                path.lineTo(cx - 20f, cy + h/2 + 20f)
                path.lineTo(cx + 10f, cy + h/2)
                drawPath(
                    path = path,
                    color = Color(elem.color),
                    style = if (elem.isFilled) androidx.compose.ui.graphics.drawscope.Fill else androidx.compose.ui.graphics.drawscope.Stroke(elem.strokeWidth)
                )
            }
        }
    }
}

@Composable
fun RenderText(elem: TextElement) {
    val style = when {
        elem.isBold && elem.isItalic -> androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        elem.isBold -> androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold)
        elem.isItalic -> androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        else -> androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Normal)
    }

    val fFamily = when (elem.fontStyle) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }

    Text(
        text = elem.text,
        color = Color(elem.color),
        fontSize = (elem.size * 0.75f).sp,
        style = style,
        fontFamily = fFamily,
        textAlign = when (elem.alignment) {
            "Left" -> TextAlign.Left
            "Right" -> TextAlign.Right
            else -> TextAlign.Center
        }
    )
}

@Composable
fun WorkstationTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = if (isActive) Color(0xFF4F46E5) else Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            color = if (isActive) Color(0xFF4F46E5) else Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}
