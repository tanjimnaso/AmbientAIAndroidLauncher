package com.ambient.launcher.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ambient.launcher.BriefingViewModel
import com.ambient.launcher.tts.TtsIconButton
import com.ambient.launcher.ui.theme.AmbientTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AnalysisScreen
 *
 * Full-screen in-app reading view for Gemini Pro deep analysis.
 * When analysis is null: shows "No Data" state with an Archives button
 * that reveals any previously cached analysis.
 *
 * Back / swipe-right to dismiss.
 */
@Composable
internal fun AnalysisScreen(
    briefingText: String,
    viewModel: BriefingViewModel,
    onDismiss: () -> Unit
) {
    val analysisState by viewModel.analysis.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAnalysisLoading.collectAsStateWithLifecycle()
    var showArchives by remember { mutableStateOf(false) }
    // Read once on open — SharedPreferences is fast enough for remember{}
    val cachedEntry = remember { viewModel.getCachedAnalysisEntry() }

    BackHandler {
        if (showArchives) showArchives = false else onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmbientTheme.palette.drawerBackground)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 100f) {
                        if (showArchives) showArchives = false else onDismiss()
                    }
                }
            }
    ) {
        if (showArchives && cachedEntry != null) {
            // ── Archives overlay ──────────────────────────────────────────────
            ArchivesView(
                entry = cachedEntry,
                onDismiss = { showArchives = false }
            )
        } else if (analysisState != null && !isLoading) {
            // ── Analysis content ──────────────────────────────────────────────
            val sections = parseAnalysisSections(analysisState!!)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item { AnalysisHeader(briefingText) }
                items(sections) { section ->
                    AnalysisSection(label = section.first, body = section.second)
                }
                if (cachedEntry != null) {
                    item {
                        Text(
                            text = "ARCHIVES",
                            style = ResponsiveTypography.t3.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 1.sp
                            ),
                            color = AmbientTheme.palette.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier
                                .clickable { showArchives = true }
                                .padding(top = 8.dp, bottom = 80.dp)
                        )
                    }
                } else {
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        } else {
            // ── No data / loading state ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = "NEWS ANALYSIS",
                    style = ResponsiveTypography.t3.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.5.sp
                    ),
                    color = AmbientTheme.palette.accentHigh.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(
                    color = AmbientTheme.palette.accentHigh.copy(alpha = 0.15f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                if (briefingText.isNotBlank()) {
                    Text(
                        text = briefingText,
                        style = ResponsiveTypography.t3.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        color = AmbientTheme.palette.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 28.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = if (isLoading) "AWAITING ANALYSIS..." else "NO DATA",
                    style = ResponsiveTypography.d3.copy(fontFamily = FontFamily.Monospace),
                    color = AmbientTheme.palette.textPrimary.copy(
                        alpha = if (isLoading) 0.3f else 0.2f
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isLoading)
                        "Gemini is generating the summary.\nThis may take up to 2 minutes."
                    else
                        "No summary has been generated for this signal.\nTap ANALYSE on the home screen to begin.",
                    style = ResponsiveTypography.t3,
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.4f),
                    lineHeight = 18.sp
                )

                if (cachedEntry != null) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "ARCHIVES",
                        style = ResponsiveTypography.t3.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        color = AmbientTheme.palette.accentHigh.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { showArchives = true }
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }

        // ── Top-right TTS button (drawn last so it stacks above content) ───
        val analysisText = analysisState
        if (!analysisText.isNullOrBlank() && !showArchives) {
            TtsIconButton(
                sessionId = "analysis:${briefingText.hashCode()}",
                title = "News Analysis",
                textProvider = { "$briefingText. $analysisText" },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 20.dp)
            )
        }
    }
}

@Composable
private fun AnalysisHeader(briefingText: String) {
    Column {
        Text(
            text = "NEWS ANALYSIS",
            style = ResponsiveTypography.t3.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.5.sp
            ),
            color = AmbientTheme.palette.accentHigh.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        HorizontalDivider(
            color = AmbientTheme.palette.accentHigh.copy(alpha = 0.15f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        if (briefingText.isNotBlank()) {
            Text(
                text = briefingText,
                style = ResponsiveTypography.t3.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                ),
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }
    }
}

@Composable
private fun ArchivesView(
    entry: Pair<String, Long>,
    onDismiss: () -> Unit
) {
    val (text, timestamp) = entry
    val dateLabel = remember(timestamp) {
        SimpleDateFormat("d MMM yyyy  h:mm a", Locale.ENGLISH).format(Date(timestamp))
    }
    val sections = remember(text) { parseAnalysisSections(text) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ARCHIVED ANALYSIS",
                    style = ResponsiveTypography.t3.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.5.sp
                    ),
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f)
                )
                Text(
                    text = "← BACK",
                    style = ResponsiveTypography.t3.copy(fontFamily = FontFamily.Monospace),
                    color = AmbientTheme.palette.accentHigh.copy(alpha = 0.5f),
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }
            Text(
                text = dateLabel,
                style = ResponsiveTypography.t3.copy(fontFamily = FontFamily.Monospace),
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 20.dp)
            )
            HorizontalDivider(
                color = AmbientTheme.palette.accentHigh.copy(alpha = 0.15f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
        items(sections) { section ->
            AnalysisSection(label = section.first, body = section.second)
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun AnalysisSection(label: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp)
    ) {
        HorizontalDivider(
            color = AmbientTheme.palette.textPrimary.copy(alpha = 0.06f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Text(
            text = label,
            style = ResponsiveTypography.t3.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp
            ),
            color = AmbientTheme.palette.accentHigh.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = body,
            style = ResponsiveTypography.t2.copy(lineHeight = 28.sp),
            color = AmbientTheme.palette.textPrimary.copy(alpha = 0.88f)
        )
    }
}

/**
 * Splits raw Gemini response into labeled section pairs.
 * Looks for numbered prefixes (1., 2., …). Falls back to a single unlabeled section.
 */
private val SECTION_LABELS = listOf(
    "01  SYSTEMIC EXPLANATION",
    "02  HISTORICAL PARALLEL",
    "03  LITERATURE & PHILOSOPHY",
    "04  TECH & ECONOMIC ANALYSIS",
    "05  PROBABILISTIC FORECAST"
)

private fun parseAnalysisSections(raw: String): List<Pair<String, String>> {
    val cleaned = raw
        .replace(Regex("\\*{1,3}(.+?)\\*{1,3}"), "$1")
        .replace(Regex("_{1,2}(.+?)_{1,2}"), "$1")
        .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
        .trim()

    val numbered = Regex("""(?m)^\s*(\d)\.\s+(.+)""")
    val matches = numbered.findAll(cleaned).toList()

    if (matches.size >= 2) {
        return matches.mapIndexed { index, match ->
            val label = SECTION_LABELS.getOrElse(index) { "SECTION ${index + 1}" }
            val start = match.range.last + 1
            val end = if (index + 1 < matches.size) matches[index + 1].range.first else cleaned.length
            val body = cleaned.substring(start, end).trim()
            label to body
        }
    }

    return listOf("NEWS BRIEF" to cleaned)
}
