package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.DesignDraft
import com.example.ui.viewmodel.DesignerViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DesignerViewModel,
    onNavigateToCanvas: () -> Unit
) {
    val savedDrafts by viewModel.savedDrafts.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var isCreatingCustomPalette by remember { mutableStateOf(false) }
    var inputPromptText by remember { mutableStateOf("") }
    val aiOptimizedResult by viewModel.aiOptimizedPrompt.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo icon with gradient background
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF4F46E5), Color(0xFFA855F7), Color(0xFFEC4899))
                                    )
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_icon_1782540900984),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Artify Studio",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            )
                            Text(
                                text = "PRO SUITE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5),
                                    letterSpacing = 1.5.sp,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Soft round Notification Button
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "No new notifications", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // User Avatar with "JS"
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E7FF))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JS",
                            color = Color(0xFF4338CA),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDFBFF)
                )
            )
        },
        containerColor = Color(0xFFFDFBFF)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Interactive Prompt Creation Card (from Clean Minimalism Design HTML)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Glow effect background
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 2.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF4F46E5).copy(alpha = 0.15f),
                                        Color(0xFFA855F7).copy(alpha = 0.15f)
                                    )
                                )
                            )
                    )

                    // Card container
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "What would you like to create?",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // Clean, white custom text input field
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                OutlinedTextField(
                                    value = inputPromptText,
                                    onValueChange = { inputPromptText = it },
                                    placeholder = {
                                        Text(
                                            "A futuristic cyberpunk cat...",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF1E293B),
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    )
                                )

                                // Circle button to enhance
                                IconButton(
                                    onClick = { viewModel.optimizePrompt(inputPromptText) },
                                    enabled = inputPromptText.isNotBlank() && !isAiLoading,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (inputPromptText.isNotBlank()) Color(0xFF4F46E5) else Color(0xFFCBD5E1))
                                ) {
                                    if (isAiLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Enhance",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Horizontal Quick Action suggestion chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SuggestionChipButton(
                                    label = "✨ Image Creator",
                                    selected = true
                                ) {
                                    inputPromptText = "A realistic pastel painting of a mystical lake in summer"
                                }
                                SuggestionChipButton(
                                    label = "🖼️ Brand Kits",
                                    selected = false
                                ) {
                                    inputPromptText = "Minimalist corporate identity package with sleek abstract lines"
                                }
                                SuggestionChipButton(
                                    label = "🎨 Restyle",
                                    selected = false
                                ) {
                                    inputPromptText = "Vibrant high-contrast pop-art portrait with bold strokes"
                                }
                            }

                            // AI Results visibility block
                            AnimatedVisibility(
                                visible = aiOptimizedResult.isNotEmpty(),
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Optimized Design Prompt:",
                                        color = Color(0xFF4F46E5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = aiOptimizedResult,
                                        color = Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(aiOptimizedResult))
                                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy", color = Color(0xFF4F46E5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(
                                            onClick = {
                                                viewModel.createNewDesign(1080, 1080)
                                                viewModel.addTextElement(aiOptimizedResult, color = 0xFF0F172A.toInt())
                                                onNavigateToCanvas()
                                            }
                                        ) {
                                            Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Use in Design", color = Color(0xFF4F46E5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Design Power Tools (Start a Blank Design) - Pastel custom cells from HTML
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Design Power Tools",
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    // 2x2 Clean grid mapping HTML tool styling
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            // Square Instagram Post Button
                            DesignToolGridCard(
                                title = "Square (1:1)",
                                subtitle = "Instagram Post",
                                icon = Icons.Default.Square,
                                bgColor = Color(0xFFE3F2FD),
                                borderColor = Color(0xFFBBDEFB),
                                textColor = Color(0xFF1E3A8A),
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.createNewDesign(1080, 1080)
                                onNavigateToCanvas()
                            }

                            // Portrait Stories Button
                            DesignToolGridCard(
                                title = "Portrait (4:5)",
                                subtitle = "Stories / Cards",
                                icon = Icons.Default.Portrait,
                                bgColor = Color(0xFFF3E5F5),
                                borderColor = Color(0xFFE1BEE7),
                                textColor = Color(0xFF581C87),
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.createNewDesign(1080, 1350)
                                onNavigateToCanvas()
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            // Landscape Cover Button
                            DesignToolGridCard(
                                title = "Landscape (16:9)",
                                subtitle = "Cover / YouTube",
                                icon = Icons.Default.CropLandscape,
                                bgColor = Color(0xFFE8F5E9),
                                borderColor = Color(0xFFC8E6C9),
                                textColor = Color(0xFF065F46),
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.createNewDesign(1200, 675)
                                onNavigateToCanvas()
                            }

                            // Brand Kits & Colors Toggle button
                            DesignToolGridCard(
                                title = "Brand Kits",
                                subtitle = "Color Palettes",
                                icon = Icons.Default.Style,
                                bgColor = Color(0xFFFFF3E0),
                                borderColor = Color(0xFFFFE0B2),
                                textColor = Color(0xFF9A3412),
                                modifier = Modifier.weight(1f)
                            ) {
                                isCreatingCustomPalette = !isCreatingCustomPalette
                            }
                        }
                    }
                }
            }

            // 3. Brand Kit Palette Maker Drawer
            item {
                AnimatedVisibility(
                    visible = isCreatingCustomPalette,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    BrandKitCreator { colors ->
                        viewModel.addCustomPalette(colors)
                        isCreatingCustomPalette = false
                        Toast.makeText(context, "Brand palette added successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // 4. Saved Drafts Section ("LOCAL PROJECTS") with green offline badge from HTML
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "LOCAL PROJECTS",
                                color = Color(0xFF1E293B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OFFLINE",
                                    color = Color(0xFF15803D),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        TextButton(onClick = {}) {
                            Text("View All", color = Color(0xFF4F46E5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (savedDrafts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(28.dp))
                                Text(
                                    text = "No saved drafts yet",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Create a blank project and click Save Draft!",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(savedDrafts) { draft ->
                                SavedDraftCard(
                                    draft = draft,
                                    context = context,
                                    onEdit = {
                                        viewModel.loadDraft(draft.id)
                                        onNavigateToCanvas()
                                    },
                                    onDelete = {
                                        viewModel.deleteDraft(draft.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Instant Graphic Design Templates
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Instant Graphic Design Templates",
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            TemplateCard(
                                title = "Creative Quote",
                                tag = "Inspire",
                                gradient = listOf(Color(0xFFE0E7FF), Color(0xFFEEF2F6)),
                                textColor = Color(0xFF1E1B4B),
                                accentColor = Color(0xFF4F46E5)
                            ) {
                                viewModel.applyTemplate("Creative Quote")
                                onNavigateToCanvas()
                            }
                        }
                        item {
                            TemplateCard(
                                title = "Birthday Wishes",
                                tag = "Celebration",
                                gradient = listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6)),
                                textColor = Color(0xFF9F1239),
                                accentColor = Color(0xFFF43F5E)
                            ) {
                                viewModel.applyTemplate("Birthday Greetings")
                                onNavigateToCanvas()
                            }
                        }
                        item {
                            TemplateCard(
                                title = "Minimalist Space",
                                tag = "Aesthetic",
                                gradient = listOf(Color(0xFFF0FDFA), Color(0xFFCCFBF1)),
                                textColor = Color(0xFF115E59),
                                accentColor = Color(0xFF0D9488)
                            ) {
                                viewModel.applyTemplate("Minimalist Space")
                                onNavigateToCanvas()
                            }
                        }
                        item {
                            TemplateCard(
                                title = "Business Flier",
                                tag = "Marketing",
                                gradient = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)),
                                textColor = Color(0xFF0F172A),
                                accentColor = Color(0xFF4F46E5)
                            ) {
                                viewModel.applyTemplate("Business Promo")
                                onNavigateToCanvas()
                            }
                        }
                    }
                }
            }

            // 6. Color Palettes List (Brand Kits & Colors)
            item {
                val brandPalettesState by viewModel.brandPalettes.collectAsState()
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Style, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                        Text(
                            text = "Brand Kit & Colors",
                            color = Color(0xFF1E293B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Visual Color Palettes you can use instantly for texts, shapes, and canvases:",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        brandPalettesState.forEachIndexed { idx, palette ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                palette.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(color))
                                            .clickable {
                                                val hexCode = String.format("#%06X", 0xFFFFFF and color)
                                                clipboardManager.setText(AnnotatedString(hexCode))
                                                Toast.makeText(context, "Copied $hexCode to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer Spacer
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun SuggestionChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) Color(0xFFEEF2F6) else Color(0xFFF8FAFC))
            .border(
                1.dp,
                if (selected) Color(0xFFE2E8F0) else Color(0xFFF1F5F9),
                RoundedCornerShape(50.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF4F46E5) else Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DesignToolGridCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(108.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(26.dp)
            )
            Column {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TemplateCard(
    title: String,
    tag: String,
    gradient: List<Color>,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(135.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(tag, color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SavedDraftCard(
    draft: DesignDraft,
    context: Context,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(155.dp)
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color(draft.backgroundColor))
            ) {
                val thumbnailFile = draft.thumbnailPath?.let { File(it) }
                if (thumbnailFile != null && thumbnailFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(thumbnailFile.absolutePath)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Project preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = draft.title.take(1).uppercase(),
                            color = Color.Black.copy(alpha = 0.15f),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Delete button in corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable { onDelete() }
                        .border(1.dp, Color(0xFFF1F5F9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = draft.title,
                    color = Color(0xFF1E293B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${draft.canvasWidth}x${draft.canvasHeight}",
                    color = Color(0xFF64748B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BrandKitCreator(onSave: (List<Int>) -> Unit) {
    var c1 by remember { mutableStateOf(0xFF6366F1.toInt()) }
    var c2 by remember { mutableStateOf(0xFFEC4899.toInt()) }
    var c3 by remember { mutableStateOf(0xFF14B8A6.toInt()) }
    var c4 by remember { mutableStateOf(0xFF1E293B.toInt()) }

    val presetColors = listOf(
        0xFFEF4444.toInt(), 0xFFF59E0B.toInt(), 0xFF10B981.toInt(), 0xFF3B82F6.toInt(),
        0xFF6366F1.toInt(), 0xFFEC4899.toInt(), 0xFF14B8A6.toInt(), 0xFF1E293B.toInt(),
        0xFFFFFFFF.toInt(), 0xFF84CC16.toInt(), 0xFFD946EF.toInt(), 0xFFF43F5E.toInt()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Create Custom Brand Palette", color = Color(0xFF1E293B), fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(c1, c2, c3, c4).forEachIndexed { index, selectedColor ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(selectedColor))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("#%06X", 0xFFFFFF and selectedColor),
                        color = if (selectedColor == 0xFFFFFFFF.toInt()) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text("Select colors to assign below:", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Medium)

        // Color circles selector grid
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presetColors) { col ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(col))
                        .clickable {
                            c4 = c3
                            c3 = c2
                            c2 = c1
                            c1 = col
                        }
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                )
            }
        }

        Button(
            onClick = { onSave(listOf(c1, c2, c3, c4)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Palette to Brand Kit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
