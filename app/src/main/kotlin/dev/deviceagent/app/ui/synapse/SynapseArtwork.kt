package dev.deviceagent.app.ui.synapse

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.R

/** Only isolated artwork extracted from approved references with user authorization. */
@Composable
internal fun ReferenceArtwork(@DrawableRes drawable: Int, description: String?, modifier: Modifier = Modifier) {
    Image(painterResource(drawable), description, modifier, contentScale = ContentScale.Fit)
}

@DrawableRes
internal fun providerArtwork(provider: String): Int? = when {
    provider.contains("Gemini", true) || provider.contains("Google", true) -> R.drawable.synapse_art_provider_google
    provider.contains("OpenAI", true) -> R.drawable.synapse_art_provider_openai
    provider.contains("Claude", true) || provider.contains("Anthropic", true) -> R.drawable.synapse_art_provider_anthropic
    else -> null
}

@DrawableRes
internal fun packageArtwork(name: String): Int? = when (name.lowercase()) {
    "python" -> R.drawable.synapse_art_package_python
    "node.js", "nodejs" -> R.drawable.synapse_art_package_nodejs
    "git" -> R.drawable.synapse_art_package_git
    "curl" -> R.drawable.synapse_art_package_curl
    "ffmpeg" -> R.drawable.synapse_art_package_ffmpeg
    "neofetch" -> R.drawable.synapse_art_package_neofetch
    "htop" -> R.drawable.synapse_art_package_htop
    "vim" -> R.drawable.synapse_art_package_vim
    "wget" -> R.drawable.synapse_art_package_wget
    "build-essential" -> R.drawable.synapse_art_package_build
    else -> null
}

@Composable
internal fun ProviderArtwork(provider: String, size: Int = 24) {
    val drawable = providerArtwork(provider)
    if (drawable != null) ReferenceArtwork(drawable, null, Modifier.size(size.dp).clip(RoundedCornerShape(5.dp)))
    else AssetIcon(SynapseIcon.MODELS, size = size.dp)
}

@Composable
internal fun VideoThumbnail(index: Int, modifier: Modifier = Modifier) {
    val drawable = listOf(R.drawable.synapse_art_video_1, R.drawable.synapse_art_video_2, R.drawable.synapse_art_video_3)[index.coerceIn(0,2)]
    ReferenceArtwork(drawable, "Sample video ${index+1} preview", modifier.clip(RoundedCornerShape(5.dp)))
}

@Composable
internal fun DemoVideoRow(index: Int, onOpen: () -> Unit) {
    val video = demoVideos[index]
    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp).clickable(onClick = onOpen).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VideoThumbnail(index, Modifier.width(48.dp).height(32.dp))
        Column(Modifier.weight(1f)) {
            Text("${video.name}_converted.mp4", style = MaterialTheme.typography.titleSmall.copy(fontSize = 10.sp, lineHeight = 13.sp))
            Text("MP4  ·  ${video.resolution}  ·  ${video.size}  ·  ${video.duration}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp,lineHeight = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onOpen, Modifier.size(25.dp)) { AssetIcon(SynapseIcon.PLAY, "Preview ${video.name}", size = 15.dp) }
        IconButton(onOpen, Modifier.size(23.dp)) { AssetIcon(SynapseIcon.FILE, "Details ${video.name}", size = 14.dp) }
        Text("⋮", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ReferenceMetricCards(vararg metrics: Triple<SynapseIcon,String,String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        metrics.forEachIndexed { index,(icon,value,label) ->
            val tint = listOf(Color(0xFF00A85B),MaterialTheme.colorScheme.primary,Color(0xFF596580),Color(0xFF8B45E8))[index % 4]
            Row(Modifier.weight(1f).background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)).padding(horizontal=5.dp,vertical=5.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                AssetIcon(icon,modifier=Modifier.background(tint.copy(alpha=.08f),RoundedCornerShape(6.dp)).padding(4.dp),size=17.dp,tint=tint)
                Column {
                    Text(value,style=MaterialTheme.typography.titleSmall.copy(fontSize=9.sp,lineHeight=12.sp))
                    Text(label,style=MaterialTheme.typography.bodySmall.copy(fontSize=8.sp,lineHeight=11.sp),color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
