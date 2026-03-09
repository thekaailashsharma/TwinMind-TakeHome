package com.takehome.twinmind.feature.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.takehome.twinmind.core.designsystem.R as DesignR
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmLabeledTextField
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationScreen(
    onCancelClick: () -> Unit,
    onSaveClick: (name: String, role: String, language: String, translateTranscripts: Boolean, additionalInfo: String) -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    initialRole: String = "",
    initialLanguage: String = "English",
    initialTranslate: Boolean = false,
    initialAdditionalInfo: String = "",
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var role by rememberSaveable { mutableStateOf(initialRole) }
    var language by rememberSaveable { mutableStateOf(initialLanguage) }
    var translateTranscripts by rememberSaveable { mutableStateOf(initialTranslate) }
    var additionalInfo by rememberSaveable { mutableStateOf(initialAdditionalInfo) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Personalize",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TwinMindDarkNavy,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onCancelClick) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            color = TwinMindDarkNavy,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSaveClick(name, role, language, translateTranscripts, additionalInfo)
                        },
                    ) {
                        Text(
                            text = "Save",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TwinMindTeal,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                Image(
                    painter = painterResource(id = DesignR.drawable.plant),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PersonalizationChip("I can be more:")
                    PersonalizationChip("Relevant")
                    PersonalizationChip("Personal")
                    PersonalizationChip("Useful")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your name, role, and language help us personalize responses. The more you share, the more useful TwinMind becomes.",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            TmLabeledTextField(
                label = "What should TwinMind call you?",
                value = name,
                onValueChange = { name = it },
                placeholder = "Enter your name",
            )

            Spacer(modifier = Modifier.height(20.dp))

            TmLabeledTextField(
                label = "What do you do?",
                value = role,
                onValueChange = { role = it },
                placeholder = "Student, engineer, etc",
            )

            Spacer(modifier = Modifier.height(20.dp))

            TmLabeledTextField(
                label = "Preferred language for summaries and responses",
                value = language,
                onValueChange = { language = it },
                placeholder = "English",
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Translate Transcripts toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Translate Transcripts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TwinMindDarkNavy,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = translateTranscripts,
                    onCheckedChange = { translateTranscripts = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TwinMindWhite,
                        checkedTrackColor = TwinMindTeal,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            TmLabeledTextField(
                label = "Anything else TwinMind should know about you?",
                value = additionalInfo,
                onValueChange = { additionalInfo = it },
                placeholder = "e.g. I am a product designer at TwinMind. I like concise summaries that cut right to the chases of the answer I am looking for.",
                singleLine = false,
                maxLines = 4,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PersonalizationChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = TwinMindDarkNavy.copy(alpha = 0.7f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TwinMindWhite,
        )
    }
}
