package com.adhanapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adhanapp.R

private val FAQ = listOf(
    R.string.faq_q1 to R.string.faq_a1, R.string.faq_q2 to R.string.faq_a2, R.string.faq_q3 to R.string.faq_a3,
    R.string.faq_q4 to R.string.faq_a4, R.string.faq_q5 to R.string.faq_a5, R.string.faq_q6 to R.string.faq_a6,
    R.string.faq_q7 to R.string.faq_a7,
)

private val PRIVACY = listOf(
    R.string.privacy_t1 to R.string.privacy_b1, R.string.privacy_t2 to R.string.privacy_b2,
    R.string.privacy_t3 to R.string.privacy_b3, R.string.privacy_t4 to R.string.privacy_b4,
    R.string.privacy_t5 to R.string.privacy_b5,
)

@Composable
fun FaqScreen(onBack: () -> Unit) {
    InfoScaffold(stringResource(R.string.more_faq), stringResource(R.string.more_faq_sub), onBack) { InfoCards(FAQ) }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    InfoScaffold(stringResource(R.string.more_privacy), stringResource(R.string.more_privacy_sub), onBack) { InfoCards(PRIVACY) }
}

@Composable
private fun InfoCards(items: List<Pair<Int, Int>>) {
    items.forEach { (title, body) ->
        GlassCard(Modifier.fillMaxWidth(), radius = 18.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = AppColors.Gold)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = AppColors.Text)
            }
        }
    }
}

@Composable
private fun InfoScaffold(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackRow(onBack)
        ScreenTitle(title, subtitle)
        Spacer(Modifier.height(10.dp))
        GoldOrnament()
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        Spacer(Modifier.height(30.dp))
    }
}
