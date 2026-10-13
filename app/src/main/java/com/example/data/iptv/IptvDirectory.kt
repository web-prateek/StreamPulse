package com.example.data.iptv

data class IptvPreset(
    val name: String,
    val type: String, // Category, Language, Country, Region
    val url: String,
    val channelEstimate: String,
    val description: String = ""
)

object IptvDirectory {

    const val DEFAULT_M3U_URL = "https://iptv-org.github.io/iptv/index.category.m3u"
    const val DEFAULT_M3U_NAME = "IPTV Master (Grouped by Category)"

    const val DEFAULT_LANGUAGE_M3U_URL = "https://iptv-org.github.io/iptv/index.language.m3u"
    const val DEFAULT_COUNTRY_M3U_URL = "https://iptv-org.github.io/iptv/index.country.m3u"

    // Real categories directly from user's provided list
    val CATEGORIES = listOf(
        IptvPreset("Animation", "Category", "https://iptv-org.github.io/iptv/categories/animation.m3u", "169 channels", "Anime & animated cartoons"),
        IptvPreset("Auto", "Category", "https://iptv-org.github.io/iptv/categories/auto.m3u", "28 channels", "Motorsports & automotive"),
        IptvPreset("Business", "Category", "https://iptv-org.github.io/iptv/categories/business.m3u", "74 channels", "Finance, markets & business"),
        IptvPreset("Classic", "Category", "https://iptv-org.github.io/iptv/categories/classic.m3u", "108 channels", "Vintage cinema & retro TV"),
        IptvPreset("Comedy", "Category", "https://iptv-org.github.io/iptv/categories/comedy.m3u", "216 channels", "Standup & comedy series"),
        IptvPreset("Cooking", "Category", "https://iptv-org.github.io/iptv/categories/cooking.m3u", "58 channels", "Culinary shows & recipes"),
        IptvPreset("Culture", "Category", "https://iptv-org.github.io/iptv/categories/culture.m3u", "192 channels", "Arts, theater & culture"),
        IptvPreset("Documentary", "Category", "https://iptv-org.github.io/iptv/categories/documentary.m3u", "264 channels", "History, nature & science"),
        IptvPreset("Education", "Category", "https://iptv-org.github.io/iptv/categories/education.m3u", "215 channels", "Academic & learning"),
        IptvPreset("Entertainment", "Category", "https://iptv-org.github.io/iptv/categories/entertainment.m3u", "902 channels", "General entertainment & drama"),
        IptvPreset("Family", "Category", "https://iptv-org.github.io/iptv/categories/family.m3u", "62 channels", "Family-safe broadcasts"),
        IptvPreset("General", "Category", "https://iptv-org.github.io/iptv/categories/general.m3u", "2,744 channels", "General broadcast channels"),
        IptvPreset("Kids", "Category", "https://iptv-org.github.io/iptv/categories/kids.m3u", "420 channels", "Children's channels"),
        IptvPreset("Lifestyle", "Category", "https://iptv-org.github.io/iptv/categories/lifestyle.m3u", "140 channels", "Home, design & travel"),
        IptvPreset("Movies", "Category", "https://iptv-org.github.io/iptv/categories/movies.m3u", "781 channels", "Feature films & cinema"),
        IptvPreset("Music", "Category", "https://iptv-org.github.io/iptv/categories/music.m3u", "773 channels", "Music videos, concerts & radio"),
        IptvPreset("News", "Category", "https://iptv-org.github.io/iptv/categories/news.m3u", "1,016 channels", "Live news broadcasts worldwide"),
        IptvPreset("Outdoor", "Category", "https://iptv-org.github.io/iptv/categories/outdoor.m3u", "77 channels", "Wild, hunting & fishing"),
        IptvPreset("Relax", "Category", "https://iptv-org.github.io/iptv/categories/relax.m3u", "9 channels", "Ambient & relaxation streams"),
        IptvPreset("Science", "Category", "https://iptv-org.github.io/iptv/categories/science.m3u", "22 channels", "Space, nature & tech"),
        IptvPreset("Series", "Category", "https://iptv-org.github.io/iptv/categories/series.m3u", "521 channels", "Television series & dramas"),
        IptvPreset("Sports", "Category", "https://iptv-org.github.io/iptv/categories/sports.m3u", "454 channels", "Live sports & games"),
        IptvPreset("Travel", "Category", "https://iptv-org.github.io/iptv/categories/travel.m3u", "60 channels", "Tourism & exploration"),
        IptvPreset("Weather", "Category", "https://iptv-org.github.io/iptv/categories/weather.m3u", "16 channels", "Live radar & meteorology")
    )

    // Real language playlists directly from user's provided list
    val LANGUAGES = listOf(
        IptvPreset("English", "Language", "https://iptv-org.github.io/iptv/languages/eng.m3u", "2,984 channels", "English worldwide"),
        IptvPreset("Spanish", "Language", "https://iptv-org.github.io/iptv/languages/spa.m3u", "2,384 channels", "Spanish worldwide"),
        IptvPreset("Russian", "Language", "https://iptv-org.github.io/iptv/languages/rus.m3u", "666 channels", "Russian worldwide"),
        IptvPreset("Portuguese", "Language", "https://iptv-org.github.io/iptv/languages/por.m3u", "477 channels", "Portuguese worldwide"),
        IptvPreset("French", "Language", "https://iptv-org.github.io/iptv/languages/fra.m3u", "459 channels", "French worldwide"),
        IptvPreset("Hindi", "Language", "https://iptv-org.github.io/iptv/languages/hin.m3u", "349 channels", "Hindi worldwide"),
        IptvPreset("Arabic", "Language", "https://iptv-org.github.io/iptv/languages/ara.m3u", "346 channels", "Arabic worldwide"),
        IptvPreset("German", "Language", "https://iptv-org.github.io/iptv/languages/deu.m3u", "326 channels", "German worldwide"),
        IptvPreset("Italian", "Language", "https://iptv-org.github.io/iptv/languages/ita.m3u", "322 channels", "Italian worldwide"),
        IptvPreset("Chinese", "Language", "https://iptv-org.github.io/iptv/languages/zho.m3u", "216 channels", "Chinese worldwide"),
        IptvPreset("Dutch", "Language", "https://iptv-org.github.io/iptv/languages/nld.m3u", "207 channels", "Dutch worldwide"),
        IptvPreset("Indonesian", "Language", "https://iptv-org.github.io/iptv/languages/ind.m3u", "179 channels", "Indonesian worldwide"),
        IptvPreset("Turkish", "Language", "https://iptv-org.github.io/iptv/languages/tur.m3u", "177 channels", "Turkish worldwide"),
        IptvPreset("Ukrainian", "Language", "https://iptv-org.github.io/iptv/languages/ukr.m3u", "177 channels", "Ukrainian worldwide"),
        IptvPreset("Polish", "Language", "https://iptv-org.github.io/iptv/languages/pol.m3u", "136 channels", "Polish worldwide"),
        IptvPreset("Japanese", "Language", "https://iptv-org.github.io/iptv/languages/jpn.m3u", "13 channels", "Japanese worldwide"),
        IptvPreset("Korean", "Language", "https://iptv-org.github.io/iptv/languages/kor.m3u", "81 channels", "Korean worldwide")
    )

    // Real country playlists directly from user's provided list
    val COUNTRIES = listOf(
        IptvPreset("United States", "Country", "https://iptv-org.github.io/iptv/countries/us.m3u", "National & States", "US TV broadcasts"),
        IptvPreset("United Kingdom", "Country", "https://iptv-org.github.io/iptv/countries/uk.m3u", "National & BBC", "UK TV broadcasts"),
        IptvPreset("Canada", "Country", "https://iptv-org.github.io/iptv/countries/ca.m3u", "Provinces & Networks", "Canadian TV broadcasts"),
        IptvPreset("Australia", "Country", "https://iptv-org.github.io/iptv/countries/au.m3u", "National & States", "Australian TV broadcasts"),
        IptvPreset("India", "Country", "https://iptv-org.github.io/iptv/countries/in.m3u", "National & Regional", "Indian TV broadcasts"),
        IptvPreset("Germany", "Country", "https://iptv-org.github.io/iptv/countries/de.m3u", "Public & Private", "German TV broadcasts"),
        IptvPreset("France", "Country", "https://iptv-org.github.io/iptv/countries/fr.m3u", "National Networks", "French TV broadcasts"),
        IptvPreset("Spain", "Country", "https://iptv-org.github.io/iptv/countries/es.m3u", "Autonomous Regions", "Spanish TV broadcasts"),
        IptvPreset("Italy", "Country", "https://iptv-org.github.io/iptv/countries/it.m3u", "RAI & Networks", "Italian TV broadcasts"),
        IptvPreset("Brazil", "Country", "https://iptv-org.github.io/iptv/countries/br.m3u", "National & States", "Brazilian TV broadcasts"),
        IptvPreset("Mexico", "Country", "https://iptv-org.github.io/iptv/countries/mx.m3u", "National & States", "Mexican TV broadcasts"),
        IptvPreset("Argentina", "Country", "https://iptv-org.github.io/iptv/countries/ar.m3u", "Provinces & Buenos Aires", "Argentine TV broadcasts"),
        IptvPreset("Japan", "Country", "https://iptv-org.github.io/iptv/countries/jp.m3u", "National", "Japanese TV broadcasts")
    )

    // Real regional playlists from user's list
    val REGIONS = listOf(
        IptvPreset("Worldwide", "Region", "https://iptv-org.github.io/iptv/regions/ww.m3u", "Global", "Worldwide streams"),
        IptvPreset("North America", "Region", "https://iptv-org.github.io/iptv/regions/noram.m3u", "Regional", "North American TV"),
        IptvPreset("Europe", "Region", "https://iptv-org.github.io/iptv/regions/eur.m3u", "Regional", "European TV"),
        IptvPreset("Asia", "Region", "https://iptv-org.github.io/iptv/regions/asia.m3u", "Regional", "Asian TV"),
        IptvPreset("Latin America", "Region", "https://iptv-org.github.io/iptv/regions/latam.m3u", "Regional", "Latin American TV"),
        IptvPreset("Arab World / MENA", "Region", "https://iptv-org.github.io/iptv/regions/mena.m3u", "Regional", "Middle East & North Africa")
    )
}
