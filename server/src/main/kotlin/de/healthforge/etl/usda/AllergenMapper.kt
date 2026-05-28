package de.healthforge.etl.usda

/**
 * P7.S2 / REQ-INGR-ALLERGEN-MAPPING-001 — Keyword-basierte Allergen-Erkennung
 * aus dem USDA-FDC `ingredients`-Freitext (Englisch).
 *
 * Codes folgen EU FIC-Allergen-Liste (14 Hauptallergene) + Histamin-relevante
 * Erweiterungen (TYRAMINE, ALCOHOL). Wird vom [UsdaFdcImporter] beim
 * Befüllen von `IngredientEntity.allergensJson` aufgerufen.
 *
 * Designprinzipien:
 *  - Reine Funktion, keine externen Abhängigkeiten (testbar).
 *  - Keywords-Liste konservativ: lieber False-Negative als False-Positive
 *    (User können in P5 Field-PR korrigieren).
 *  - Match auf Wortgrenzen (`\b`) um z.B. `wheat` nicht in `wheatgrass` zu
 *    triggern, falls Letzteres harmlos ist (es ist nicht — beide -> GLUTEN —
 *    aber Pattern ist konservativer Default).
 */
object AllergenMapper {

    /** Stabile Allergen-Codes (Spiegel zu Android `Allergen` Enum + EU FIC §14). */
    enum class Code {
        GLUTEN,
        CRUSTACEAN,
        EGG,
        FISH,
        PEANUT,
        SOY,
        LACTOSE,
        NUT,
        CELERY,
        MUSTARD,
        SESAME,
        SULFITE,
        LUPIN,
        MOLLUSC,
        // Histamin-/Intoleranz-relevant (kein EU-FIC):
        HISTAMINE,
        TYRAMINE,
        ALCOHOL,
    }

    /**
     * Keyword-Liste pro Code. Alle Keys werden gegen lowercased Text gematcht,
     * eingebettet in `\bKEYWORD\b` (Wortgrenze).
     */
    private val KEYWORDS: Map<Code, List<String>> = mapOf(
        Code.GLUTEN to listOf(
            "wheat", "barley", "rye", "spelt", "kamut", "triticale", "malt",
            "semolina", "durum", "farro", "bulgur", "couscous",
        ),
        Code.CRUSTACEAN to listOf(
            "shrimp", "prawn", "lobster", "crab", "crayfish", "langoustine",
        ),
        Code.EGG to listOf("egg", "eggs", "albumin", "ovalbumin", "egg white", "egg yolk"),
        Code.FISH to listOf(
            "salmon", "tuna", "cod", "anchovy", "anchovies", "sardine", "herring",
            "mackerel", "trout", "halibut", "bass", "haddock", "pollock",
        ),
        Code.PEANUT to listOf("peanut", "peanuts", "groundnut"),
        Code.SOY to listOf("soy", "soya", "soybean", "soybeans", "edamame", "tofu", "tempeh"),
        Code.LACTOSE to listOf(
            "milk", "cream", "butter", "cheese", "yogurt", "yoghurt", "whey",
            "casein", "lactose", "buttermilk", "ghee", "curd",
        ),
        Code.NUT to listOf(
            "almond", "almonds", "hazelnut", "walnut", "cashew", "pecan",
            "pistachio", "macadamia", "brazil nut",
        ),
        Code.CELERY to listOf("celery", "celeriac"),
        Code.MUSTARD to listOf("mustard"),
        Code.SESAME to listOf("sesame", "tahini"),
        Code.SULFITE to listOf("sulfite", "sulphite", "sulfur dioxide", "sulphur dioxide", "e220"),
        Code.LUPIN to listOf("lupin", "lupine"),
        Code.MOLLUSC to listOf(
            "mussel", "mussels", "oyster", "clam", "scallop", "squid", "octopus", "snail",
        ),
        Code.HISTAMINE to listOf(
            "fermented", "aged cheese", "sauerkraut", "kimchi", "miso", "soy sauce",
            "vinegar", "salami", "pepperoni", "prosciutto",
        ),
        Code.TYRAMINE to listOf("aged cheese", "cured meat", "salami", "tyramine"),
        Code.ALCOHOL to listOf("wine", "beer", "vodka", "whiskey", "rum", "liqueur", "alcohol", "ethanol"),
    )

    private val COMPILED: Map<Code, List<Regex>> = KEYWORDS.mapValues { (_, words) ->
        words.map { Regex("\\b" + Regex.escape(it) + "\\b", RegexOption.IGNORE_CASE) }
    }

    /**
     * Negativ-Liste (REQ-INGR-ALLERGEN-MAPPING-001 / ReqSpec §12) — Phrasen, die
     * aus dem Text entfernt werden BEVOR Keyword-Matching läuft. Verhindert
     * False-Positives wie:
     *  - `mustard-seed-oil` (hochraffiniert, kein EU-Allergen) → MUSTARD-Trigger.
     *  - `coconut` / `coconut oil` (botanisch keine Baumnuss; FDA listet, EU nicht).
     *  - `nutmeg` (Gewürz, keine Nuss).
     *
     * Reihenfolge: längere Phrasen zuerst, damit `mustard-seed-oil` vor `mustard`
     * matched wird (sonst würde nur `mustard` ersetzt und `-seed-oil` bliebe stehen).
     */
    private val NEGATIVE_LIST: List<Regex> = listOf(
        "mustard seed oil", "mustard-seed-oil", "mustard oil",
        "coconut oil", "coconut milk", "coconut water", "coconut cream", "coconut",
        "nutmeg",
    ).map { Regex("\\b" + Regex.escape(it) + "\\b", RegexOption.IGNORE_CASE) }

    /** Entfernt Negativ-Liste-Phrasen aus dem Text (case-insensitive, Wortgrenzen-stabil). */
    internal fun stripNegatives(text: String): String =
        NEGATIVE_LIST.fold(text) { acc, regex -> regex.replace(acc, " ") }

    /**
     * Extrahiert alle Allergen-Codes aus dem gegebenen Freitext.
     * Reihenfolge entspricht der [Code]-Enum-Reihenfolge (deterministisch).
     */
    fun extract(text: String?): List<Code> {
        if (text.isNullOrBlank()) return emptyList()
        val cleaned = stripNegatives(text)
        return COMPILED.mapNotNull { (code, regexes) ->
            if (regexes.any { it.containsMatchIn(cleaned) }) code else null
        }
    }

    /** Convenience: Codes als Strings (für JSON-Serialisierung). */
    fun extractAsStrings(text: String?): List<String> = extract(text).map { it.name }
}
