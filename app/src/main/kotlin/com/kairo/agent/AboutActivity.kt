package com.kairo.agent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kairo.agent.ui.components.GemmASparkleIcon
import com.kairo.agent.ui.components.ReactiveAuroraBackground
import com.kairo.agent.ui.theme.GemmATheme

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.3.0"
        }.getOrDefault("1.3.0")

        setContent {
            GemmATheme {
                AboutScreen(
                    versionName = versionName,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
private fun AboutScreen(
    versionName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val AccentBlue = MaterialTheme.colorScheme.primary
    val FrostedBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val FrostedCardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    val TextPrimary = MaterialTheme.colorScheme.onSurface
    val TextSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ReactiveAuroraBackground(isGenerating = false, isListening = false)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "About Kairo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Surface(
                color = FrostedCardBg,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FrostedBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GemmASparkleIcon(size = 54.dp, isAnimating = true)
                    Text(
                        text = "Kairo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "v$versionName • 100% On-Device • Open Source (MIT)",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentBlue
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Kairo is a private, local Android AI agent powered by Gemma 4 via Android AICore. It runs on-device with zero cloud inference, offering a floating assistant overlay, screen context, voice wake-up, real Android tool calling, and standalone APK plugins.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                color = FrostedCardBg,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FrostedBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Credits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    CreditRow(
                        name = "DaManMikey",
                        role = "Creator & Lead Developer (damanmikey.me)",
                        onClick = { openUrl("https://damanmikey.me/") }
                    )
                    CreditRow(
                        name = "ThatJoshGuy (thatjoshguy67)",
                        role = "Twidget setup animations, One UI widget core & oneui-icons",
                        onClick = { openUrl("https://github.com/thatjoshguy67/oneui-icons") }
                    )
                    CreditRow(
                        name = "tribalfs",
                        role = "One UI design system reference (oneui-design)",
                        onClick = { openUrl("https://github.com/tribalfs/oneui-design") }
                    )
                }
            }

            Surface(
                color = FrostedCardBg,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FrostedBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Links",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    AboutLinkRow(Icons.Default.Language, "Website & Direct Downloads", "kairo.mikeyslabs.xyz") {
                        openUrl("https://kairo.mikeyslabs.xyz/")
                    }
                    AboutLinkRow(Icons.Default.Code, "GitHub Source Code (MIT)", "github.com/DaDevMikey/Kairo") {
                        openUrl("https://github.com/DaDevMikey/Kairo")
                    }
                    AboutLinkRow(Icons.Default.Download, "Releases & Plugins", "github.com/DaDevMikey/Kairo/releases") {
                        openUrl("https://github.com/DaDevMikey/Kairo/releases")
                    }
                    AboutLinkRow(Icons.Default.Extension, "Plugin SDK Guide", "PLUGINS.md") {
                        openUrl("https://github.com/DaDevMikey/Kairo/blob/main/PLUGINS.md")
                    }
                    AboutLinkRow(Icons.Default.Policy, "Privacy Policy", "kairo.mikeyslabs.xyz/privacy.html") {
                        openUrl("https://kairo.mikeyslabs.xyz/privacy.html")
                    }
                    AboutLinkRow(Icons.Default.Policy, "Terms of Service", "kairo.mikeyslabs.xyz/terms.html") {
                        openUrl("https://kairo.mikeyslabs.xyz/terms.html")
                    }
                    AboutLinkRow(Icons.Default.Favorite, "Support Project (PayPal)", "paypal.me/DaManMikey") {
                        openUrl("https://paypal.me/DaManMikey")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CreditRow(name: String, role: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            Text(role, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AboutLinkRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}
