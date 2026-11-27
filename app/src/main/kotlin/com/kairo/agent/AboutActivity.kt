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
import com.kairo.agent.ui.components.KairoIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.kairo.agent.ui.components.KairoSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kairo.agent.R
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
                        contentDescription = stringResource(R.string.back),
                        tint = TextPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.about_title),
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
                        text = stringResource(R.string.about_app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.about_version, versionName),
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentBlue
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_summary),
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
                        text = stringResource(R.string.about_credits_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    CreditRow(
                        name = stringResource(R.string.about_credit_damanmikey),
                        role = stringResource(R.string.about_credit_damanmikey_role),
                        onClick = { openUrl("https://damanmikey.me/") }
                    )
                    CreditRow(
                        name = stringResource(R.string.about_credit_joshguy),
                        role = stringResource(R.string.about_credit_joshguy_role),
                        onClick = { openUrl("https://github.com/thatjoshguy67/oneui-icons") }
                    )
                    CreditRow(
                        name = stringResource(R.string.about_credit_tribalfs),
                        role = stringResource(R.string.about_credit_tribalfs_role),
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
                        text = stringResource(R.string.about_links_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    AboutLinkRow(Icons.Default.Language, stringResource(R.string.about_link_website), stringResource(R.string.about_link_website_sub)) {
                        openUrl("https://kairo.mikeyslabs.xyz/")
                    }
                    AboutLinkRow(Icons.Default.Code, stringResource(R.string.about_link_github), stringResource(R.string.about_link_github_sub)) {
                        openUrl("https://github.com/DaDevMikey/Kairo")
                    }
                    AboutLinkRow(Icons.Default.Download, stringResource(R.string.about_link_releases), stringResource(R.string.about_link_releases_sub)) {
                        openUrl("https://github.com/DaDevMikey/Kairo/releases")
                    }
                    AboutLinkRow(Icons.Default.Extension, stringResource(R.string.about_link_plugin_docs), stringResource(R.string.about_link_plugin_docs_sub)) {
                        openUrl("https://github.com/DaDevMikey/Kairo/blob/main/PLUGINS.md")
                    }
                    AboutLinkRow(Icons.Default.Policy, stringResource(R.string.about_link_privacy), stringResource(R.string.about_link_privacy_sub)) {
                        openUrl("https://kairo.mikeyslabs.xyz/privacy.html")
                    }
                    AboutLinkRow(Icons.Default.Policy, stringResource(R.string.about_link_terms), stringResource(R.string.about_link_terms_sub)) {
                        openUrl("https://kairo.mikeyslabs.xyz/terms.html")
                    }
                    AboutLinkRow(Icons.Default.Favorite, stringResource(R.string.about_link_support), stringResource(R.string.about_link_support_sub)) {
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
