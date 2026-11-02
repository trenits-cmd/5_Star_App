package com.trenits.fivestar.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trenits.fivestar.R
import com.trenits.fivestar.domain.MAX_STARS
import com.trenits.fivestar.domain.RelationshipAspect
import com.trenits.fivestar.domain.coupleRelationshipScorePercent
import com.trenits.fivestar.domain.relationshipScorePercent
import com.trenits.fivestar.domain.relationshipStarsOf
import com.trenits.fivestar.ui.theme.Amber

@Composable
fun RelationshipScreen(vm: AppViewModel) {
    val myToday by vm.myToday.collectAsState()
    val partnerToday by vm.partnerToday.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val myName by vm.myName.collectAsState()
    val partnerName by vm.partnerName.collectAsState()
    val myPhoto by vm.myPhoto.collectAsState()
    val partnerPhoto by vm.partnerPhoto.collectAsState()
    val weekDays by vm.weekDays.collectAsState()
    val days by vm.days.collectAsState()

    val myLabel = myName ?: stringResource(R.string.who_you)
    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)

    val myStars = relationshipStarsOf(myToday)
    val partnerStars = relationshipStarsOf(partnerToday)
    val myScore = relationshipScorePercent(myStars)
    val partnerScore = relationshipScorePercent(partnerStars)
    val coupleScore = coupleRelationshipScorePercent(myStars, partnerStars)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.cat_relationship),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.relationship_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        stringResource(R.string.relationship_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Gemeinsamer Beziehungs-Score Hero
        item {
            val progress = ((coupleScore ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.relationship_score_couple),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        formatPercent(coupleScore),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                        color = Amber,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(photoBase64 = myPhoto, name = myLabel, size = 24.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.relationship_score_you, myLabel, formatPercent(myScore)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        if (partnerUid != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(photoBase64 = partnerPhoto, name = partnerLabel, size = 24.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.relationship_score_partner, partnerLabel, formatPercent(partnerScore)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Die 5 Bewertungs-Dimensionen
        items(RelationshipAspect.entries, key = { it.name }) { aspect ->
            RelationshipAspectCard(
                aspect = aspect,
                myValue = myStars[aspect] ?: 0,
                partnerValue = partnerStars[aspect] ?: 0,
                partnerLabel = partnerLabel,
                showPartner = partnerUid != null,
                onRate = { stars -> vm.setRelationshipStars(aspect, stars) },
            )
        }

        // 7-Tage-Verlauf der Beziehung
        item {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text(
                stringResource(R.string.relationship_history_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    weekDays.reversed().forEach { dayKey ->
                        val myRec = days.items.firstOrNull { it.ownerUid == vm.uid && it.dayKey == dayKey }
                        val pRec = partnerUid?.let { p ->
                            days.items.firstOrNull { it.ownerUid == p && it.dayKey == dayKey }
                        }
                        val mScore = relationshipScorePercent(relationshipStarsOf(myRec))
                        val pScore = relationshipScorePercent(relationshipStarsOf(pRec))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(formatDay(dayKey), style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    stringResource(R.string.history_you, myLabel, formatPercent(mScore)),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (partnerUid != null) {
                                    Text(
                                        stringResource(R.string.history_partner, partnerLabel, formatPercent(pScore)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kompakte Beziehungstracker-Karte für den Heute-Tab, damit beide Partner alle 5 Dimensionen
 * auch direkt auf der Tagesübersicht bewerten und vergleichen können.
 */
@Composable
fun RelationshipTodaySection(vm: AppViewModel) {
    val myToday by vm.myToday.collectAsState()
    val partnerToday by vm.partnerToday.collectAsState()
    val partnerUid by vm.partnerUid.collectAsState()
    val partnerName by vm.partnerName.collectAsState()

    val partnerLabel = partnerName ?: stringResource(R.string.who_partner)
    val myStars = relationshipStarsOf(myToday)
    val partnerStars = relationshipStarsOf(partnerToday)
    val coupleScore = coupleRelationshipScorePercent(myStars, partnerStars)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(R.drawable.cat_relationship),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            stringResource(R.string.relationship_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.relationship_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    formatPercent(coupleScore),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            RelationshipAspect.entries.forEach { aspect ->
                val myVal = myStars[aspect] ?: 0
                val pVal = partnerStars[aspect] ?: 0
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                relationshipAspectLabel(aspect),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            if (partnerUid != null) {
                                Text(
                                    if (pVal > 0) {
                                        stringResource(
                                            R.string.relationship_partner_stars,
                                            partnerLabel,
                                            "★".repeat(pVal) + "☆".repeat(MAX_STARS - pVal),
                                        )
                                    } else {
                                        stringResource(R.string.relationship_partner_not_rated, partnerLabel)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        RelationshipStarRow(value = myVal) { vm.setRelationshipStars(aspect, it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipAspectCard(
    aspect: RelationshipAspect,
    myValue: Int,
    partnerValue: Int,
    partnerLabel: String,
    showPartner: Boolean,
    onRate: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Image(
                painter = painterResource(R.drawable.cat_relationship),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    relationshipAspectLabel(aspect),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (showPartner) {
                    Text(
                        if (partnerValue > 0) {
                            stringResource(
                                R.string.relationship_partner_stars,
                                partnerLabel,
                                "★".repeat(partnerValue) + "☆".repeat(MAX_STARS - partnerValue),
                            )
                        } else {
                            stringResource(R.string.relationship_partner_not_rated, partnerLabel)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (partnerValue > 0) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            RelationshipStarRow(value = myValue, onChange = onRate)
        }
    }
}

@Composable
private fun RelationshipStarRow(value: Int, onChange: (Int) -> Unit) {
    Row {
        for (n in 1..MAX_STARS) {
            val filled = n <= value
            Text(
                "★",
                fontSize = 26.sp,
                color = if (filled) Amber else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clickable { onChange(if (value == n) 0 else n) }
                    .padding(horizontal = 2.dp),
            )
        }
    }
}
