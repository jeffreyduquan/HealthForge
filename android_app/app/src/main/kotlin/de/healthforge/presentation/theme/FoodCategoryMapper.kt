package de.healthforge.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps German food names to the appropriate [FoodIcon] (category + color).
 *
 * Uses keyword-based heuristics since [de.healthforge.data.network.IngredientDto]
 * does not carry a `category` field. Matching is case-insensitive and
 * substring-based — the first match wins.
 *
 * Supplement icons use a deterministic color variant based on `supplementId % 5`.
 */

data class FoodIcon(
    val icon: ImageVector,
    val tint: Color? = null, // null → use default fgSecondary tint
)

/**
 * Ordered list of (keyword regex, icon) pairs. First match wins.
 * Keywords are matched case-insensitively against `name_de.lowercase()`.
 */
private val CATEGORY_RULES: List<Pair<Regex, ImageVector>> = listOf(
    // ── Fleisch ──
    Regex("rind|kalb|rinder|steak|filet|roastbeef|hackfleisch.*rind") to FoodIcons.RIND,
    Regex("schwein|schnitzel|kotelett|kassler|schinken|speck|bauchfleisch|schweine") to FoodIcons.SCHWEIN,
    Regex("hähnchen|huhn|pute|truthahn|gans|ente|geflügel|chicken|turkey|hähnchenbrust|putenbrust|hühner") to FoodIcons.GEFLUEGEL,
    Regex("fisch|lachs|thunfisch|forelle|hering|kabeljau|scholle|seelachs|pangasius|dorsch|garnelen|krabben|muscheln|calamari|tintenfisch|fischstäbchen") to FoodIcons.FISCH,
    Regex("wurst|aufschnitt|salami|mortadella|leberwurst|mettwurst|bockwurst|wiener|frankfurter|bratwurst|currywurst|landjäger|schinkenwurst|jagdwurst|bierschinken|lyoner|fleischwurst|gelbwurst") to FoodIcons.WURST,

    // ── Milchprodukte ──
    Regex("\\bmilch\\b|vollmilch|fettarme.*milch|entrahmte.*milch|haltbare.*milch|frischmilch|rohmilch|vorzugsmilch|buttermilch|kefir|kumys") to FoodIcons.MILCH,
    Regex("käse|gouda|emmentaler|brie|camembert|mozzarella|parmesan|cheddar|feta|gorgonzola|ricotta|mascarpone|schafskäse|ziegenkäse|rahmkäse|schmelzkäse|frischkäse|hüttenkäse|cottage.*cheese|halloumi|grillkäse|edamer|tilsiter|butterkäse|blauschimmel") to FoodIcons.KAESE,
    Regex("joghurt|quark|skyr|buttermilch|kefir|schmand|saure.*sahne|crème.*fraîche|sauerrahm|schichtkäse|topfen|molke") to FoodIcons.JOGHURT,
    Regex("\\bei\\b|eier|hühnerei|wachtelei|entei|eigelb|eiweiß.*ei|eiklar|spiegelei|rührei|omelett") to FoodIcons.EIER,

    // ── Getreideprodukte ──
    Regex("\\bbrot\\b|brötchen|toast|vollkornbrot|roggenbrot|weißbrot|mischbrot|baguette|ciabatta|pumpernickel|knäckebrot|semmel|schrippe|wecken|laugenbrötchen|laugenstange|croissant|zopf|stuten|sandwich|fladenbrot|pitabrot|tortilla.*wrap|wrap") to FoodIcons.BROT,
    Regex("nudel|pasta|spaghetti|penne|fusilli|tagliatelle|rigatoni|tortellini|ravioli|macaroni|lasagne|spätzle|gnocchi|reis|risotto|basmati|jasmin.*reis|wildreis|milchreis|langkorn|parboiled") to FoodIcons.NUDELN,
    Regex("müsli|haferflocken|cornflakes|granola|porridge|grieß|getreide|dinkel|weizen|roggen|gerste|hirse|hafer|amarant|quinoa|buchweizen|bulgur|couscous|polenta|griebrei|semmelbrösel|paniermehl|mehl|vollkornmehl") to FoodIcons.MUESLI,

    // ── Obst & Gemüse ──
    Regex("gemüse|möhre|karotte|kartoffel|brokkoli|blumenkohl|zucchini|aubergine|paprika|tomate|gurke|kürbis|spargel|bohne|erbse|linse|mais|zwiebel|knoblauch|lauch|porree|sellerie|fenchel|kohl|wirsing|grünkohl|spinat|mangold|rote.*bete|radieschen|rettich|pastinake|topinambur|schwarzwurzel|artischocke|okra") to FoodIcons.GEMUESE,
    Regex("salat|feldsalat|rucola|eichblatt|lollo|kopfsalat|eissalat|chicorée|endivie|radicchio|spinat.*blatt|babyleaf|mesclun|blattspinat") to FoodIcons.SALAT,
    Regex("obst|apfel|birne|banane|orange|mandarine|clementine|zitrone|limette|grapefruit|ananas|mango|papaya|kiwi|erdbeere|himbeere|heidelbeere|brombeere|johannisbeere|stachelbeere|kirsche|pflaume|zwetschge|aprikose|pfirsich|nektarine|weintraube|melone|wassermelone|honigmelone|feige|dattel|granatapfel|kaki|sharon|lychee|maracuja|guave|traube|rosinen|korinthen|backobst|trockenobst|kompott|apfelmus") to FoodIcons.OBST,
    Regex("nuss|mandel|walnuss|haselnuss|cashew|paranuss|macadamia|pistazie|erdnuss|pekannuss|pinienkern|sonnenblumenkern|kürbiskern|sesam|leinsamen|chia|hanfsamen|mohn|kokosnuss|kokosraspel|marone|esskastanie") to FoodIcons.NUESSE,

    // ── Fette, Gewürze, Süßes ──
    Regex("öl|butterschmalz|schmalz|margarine|frittierfett|rapsöl|sonnenblumenöl|olivenöl|kokosöl|leinöl|walnussöl|sesamöl|erntegold") to FoodIcons.OELE,
    Regex("gewürz|pfeffer|salz|paprika.*pulver|curry|zimt|kümmel|anise|fenchel.*samen|koriander|muskat|vanille|chili|ingwer|kurkuma|oregano|thymian|rosmarin|basilikum|dille|petersilie|schnittlauch|estragon|majoran|bohnenkraut|lorbeer|wacholder|nelken|kardamom|piment|safran|senf|meerrettich|essig|balsamico|sojasoße|bratensoße|brühe|suppenwürze|gemüsebrühe|hühnerbrühe|fonds") to FoodIcons.GEWUERZE,
    Regex("schokolade|praline|bonbon|lutscher|kaugummi|gummibärchen|lakritz|marshmallow|zuckerwatte|schokoriegel|keks|plätzchen|waffel|zucker|puderzucker|brauner.*zucker|rohrzucker|süßstoff|stevia|honig|ahornsirup|agavendicksaft|marmelade|konfitüre|nuss.*nougat.*creme|nutella|schokocreme|kakao.*pulver") to FoodIcons.SUESSES,
    Regex("kuchen|torte|muffin|cupcake|donut|berliner|krapfen|strudel|apfelstrudel|brownie|stückchen|gebäck|teilchen|plunder|zimtschnecke|schnecke.*gebäck|hefezopf|stollen|kekse|plätzchen|makrone|baiser|tiramisu|pudding|creme.*dessert|mousse|quarkspeise|eis|eiscreme|sorbet|eistee.*pulver") to FoodIcons.KUCHEN,

    // ── Getränke ──
    Regex("getränk|wasser|mineralwasser|sprudel|limonade|cola|fanta|sprite|brause|energydrink|energy.*drink|kaffee|espresso|cappuccino|latte.*macchiato|tee|früchtetee|kräutertee|schwarztee|grüntee|mate|saft|orangensaft|apfelsaft|multivitamin|nektar|smoothie|milchshake|kakao.*getränk|trinkschokolade|alkohol|bier|wein|sekt|champagner|schnaps|likör|longdrink|gin|vodka|whisky|rum") to FoodIcons.GETRAENKE,

    // ── Fertiggerichte & Soßen ──
    Regex("fertiggericht|tiefkühl|tk.*gericht|mikrowelle|konserve|ravioli.*dose|erasco|dosenfraß|fix.*produkt|maggi.*fix|knorr.*fix|instant.*suppe|tütensuppe|dosen.*suppe|eintopf.*dose|chili.*con.*carne.*dose|curry.*dose") to FoodIcons.FERTIGGERICHTE,
    Regex("soße|dip|ketchup|mayonnaise|majo|aioli|remoulade|salsa|pesto|chutney|sambal|harissa|ajvar|guacamole|hummus|sour.*cream.*dip|barbecue.*sauce|bbq.*sauce|curry.*sauce|sate.*sauce|teriyaki|hollandaise|béchamel|demi.*glace|jus|gravy") to FoodIcons.SOSSEN,
)

/**
 * Resolve the best icon for a German food name.
 * Returns [FoodIcons.FALLBACK] with null tint if no category matches.
 */
fun foodIconForName(nameDe: String): FoodIcon {
    val lower = nameDe.lowercase().trim()
    for ((regex, icon) in CATEGORY_RULES) {
        if (regex.containsMatchIn(lower)) {
            return FoodIcon(icon = icon, tint = null) // null = default tint
        }
    }
    return FoodIcon(icon = FoodIcons.FALLBACK, tint = null)
}

/**
 * Resolve a supplement icon variant based on supplement ID.
 * Uses `id % 5` for deterministic color assignment.
 */
fun supplementIconVariant(supplementId: Long): FoodIcon {
    val colors = listOf(
        null, // default fgSecondary
        androidx.compose.ui.graphics.Color(0xFF7C5CFF), // ambientViolet
        androidx.compose.ui.graphics.Color(0xFF4DD0E1), // ambientCyan
        androidx.compose.ui.graphics.Color(0xFF22D3A6), // statusGood
        androidx.compose.ui.graphics.Color(0xFFFFB454), // statusRelax
    )
    val tint = colors[(supplementId % colors.size).toInt()]
    return FoodIcon(icon = FoodIcons.SUPPLEMENT, tint = tint)
}
