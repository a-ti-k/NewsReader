package com.example.newsreader

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF14233B)
private val Blue = Color(0xFF315CBA)
private val Surface = Color(0xFFF4F6FA)
private val categories = listOf("National", "World", "Politics", "Business", "Technology", "Sports", "Entertainment")

class MainActivity : ComponentActivity() {
    private val repository by lazy { NewsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Surface)) {
            NewsApp(repository) {
                isReady = true
            }
        } }
    }
}

@Composable
private fun NewsApp(repository: NewsRepository, onDataReady: () -> Unit) {
    var selectedCategory by remember { mutableStateOf("National") }
    var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }
    var articles by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }
    var state by remember { mutableStateOf(LoadState.LOADING) }
    var errorMessage by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }

    fun load() {
        state = LoadState.LOADING
        errorMessage = ""
    }
    LaunchedEffect(selectedCategory, refreshKey) {
        state = LoadState.LOADING
        try {
            articles = repository.fetch(selectedCategory)
            state = if (articles.isEmpty()) LoadState.EMPTY else LoadState.SUCCESS
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unable to load news."
            state = LoadState.ERROR
        } finally {
            if (selectedCategory == "National") {
                onDataReady()
            }
        }
    }
    BackHandler(enabled = selectedArticle != null) { selectedArticle = null }

    if (selectedArticle != null) {
        ArticleScreen(article = selectedArticle!!, onBack = { selectedArticle = null })
    } else {
        Scaffold(containerColor = Surface) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Header()
                Text("Your daily briefing", Modifier.padding(start = 20.dp, top = 18.dp),
                    color = Navy, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Headlines from trusted publishers", Modifier.padding(start = 20.dp, top = 4.dp, bottom = 14.dp),
                    color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                CategoryGrid(selectedCategory) { selectedCategory = it }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedCategory, color = Navy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${articles.size} headlines", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = { refreshKey++ }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
                }
                when (state) {
                    LoadState.LOADING -> LoadingState()
                    LoadState.EMPTY -> MessageState("No headlines available", "Feeds may be temporarily unavailable. Try refreshing.", "Refresh") { refreshKey++ }
                    LoadState.ERROR -> MessageState("Couldn't load headlines", errorMessage, "Try again") { refreshKey++ }
                    LoadState.SUCCESS -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(articles, key = { it.link }) { article ->
                            ArticleCard(article) { selectedArticle = article }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(Modifier.fillMaxWidth().background(Navy).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Color(0xFF315CBA), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            Text("DB", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text("DAILY BRIEF", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text("News that matters", color = Color(0xFFB9C7E0), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CategoryGrid(selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    val active = selected == category
                    Surface(
                        modifier = Modifier.weight(1f).height(42.dp).clickable { onSelect(category) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) Blue else Color.White,
                        shadowElevation = if (active) 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(category, color = if (active) Color.White else Navy,
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ArticleCard(article: NewsArticle, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(article.source.uppercase(), color = Blue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                if (article.published.isNotBlank()) Text(formatDate(article.published), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            Text(article.title, color = Navy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (article.excerpt.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(article.excerpt, color = Color(0xFF687386), style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Text("Read original article  →", color = Blue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth().fillMaxHeight(0.55f), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Loading the latest headlines…", color = Color.Gray)
        }
    }
}

@Composable
private fun MessageState(title: String, message: String, button: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().fillMaxHeight(0.6f).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Navy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onClick) { Text(button) }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleScreen(article: NewsArticle, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().background(Navy).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("Original article", color = Color.White, fontWeight = FontWeight.Bold)
                Text(article.source, color = Color(0xFFB9C7E0), style = MaterialTheme.typography.labelSmall)
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadUrl(article.link)
                }
            },
            update = { }
        )
    }
}

private fun formatDate(raw: String): String {
    return try {
        val formats = listOf("EEE, dd MMM yyyy HH:mm:ss Z", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'")
        val date = formats.firstNotNullOfOrNull { pattern ->
            runCatching { SimpleDateFormat(pattern, Locale.US).parse(raw) }.getOrNull()
        } ?: return raw.take(22)
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(date)
    } catch (_: Exception) { raw.take(22) }
}
