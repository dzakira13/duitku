package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AvatarPresets
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SlateViolet
import com.example.util.ImageEncodingUtil

@Composable
fun ProfileAvatar(
    avatarData: String,
    displayName: String,
    size: Dp = 64.dp,
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(avatarData) {
        if (avatarData.startsWith("data:image/")) {
            ImageEncodingUtil.decodeDataUriToImageBitmap(avatarData)
        } else {
            null
        }
    }

    val clickableMod = if (onClick != null) {
        modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    } else {
        modifier.size(size)
    }

    Box(modifier = clickableMod, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                decodedBitmap != null -> {
                    Image(
                        bitmap = decodedBitmap,
                        contentDescription = "Profile photo of $displayName",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                avatarData.startsWith("https://") || avatarData.startsWith("http://") -> {
                    AsyncImage(
                        model = avatarData,
                        contentDescription = "Profile photo of $displayName",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                avatarData.startsWith("preset:") -> {
                    PresetAvatarGraphic(presetKey = avatarData, size = size)
                }
                else -> {
                    InitialAvatarFallback(displayName = displayName, size = size)
                }
            }
        }

        if (showEditBadge) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.34f).dp.coerceAtLeast(24.dp))
                    .testTag("profile_avatar_edit_badge")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Change profile photo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(5.dp)
                )
            }
        }
    }
}

@Composable
fun PresetAvatarGraphic(presetKey: String, size: Dp) {
    val (colors, icon) = presetStyle(presetKey)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Avatar preset",
            tint = Color.White,
            modifier = Modifier.size((size.value * 0.48f).dp)
        )
    }
}

@Composable
private fun InitialAvatarFallback(displayName: String, size: Dp) {
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "D"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(EmeraldPrimary, EmeraldBright))
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}

private fun presetStyle(presetKey: String): Pair<List<Color>, ImageVector> = when (presetKey) {
    AvatarPresets.GOLD -> listOf(GoldSecondary, Color(0xFFF59E0B)) to Icons.Rounded.Savings
    AvatarPresets.OCEAN -> listOf(OceanBlue, Color(0xFF38BDF8)) to Icons.Rounded.Diamond
    AvatarPresets.CORAL -> listOf(ExpenseCoral, Color(0xFFFB7185)) to Icons.Rounded.AutoAwesome
    AvatarPresets.SLATE -> listOf(SlateViolet, Color(0xFF64748B)) to Icons.Rounded.AccountBalanceWallet
    else -> listOf(EmeraldDeep, EmeraldBright) to Icons.Rounded.Eco
}
