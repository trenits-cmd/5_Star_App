package com.trenits.fivestar.domain

data class CatalogEntry(val title: String, val type: TaskType)

/** Startkatalog für neue Konten. Jede Person kann ihn danach frei ändern. */
val START_CATALOG: List<CatalogEntry> = listOf(
    CatalogEntry("Lieblingsessen", TaskType.REWARD),
    CatalogEntry("Filmabend", TaskType.REWARD),
    CatalogEntry("Massage (20 Min)", TaskType.REWARD),
    CatalogEntry("Abwasch eine Woche übernehmen", TaskType.PENALTY),
    CatalogEntry("Blumen kaufen", TaskType.PENALTY),
    CatalogEntry("Kochen für den Partner", TaskType.PENALTY),
)
