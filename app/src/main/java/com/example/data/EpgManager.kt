package com.example.data

import java.util.Calendar
import java.util.Random

data class EpgProgram(
    val title: String,
    val description: String,
    val startTime: Long, // Epoch milliseconds
    val endTime: Long,   // Epoch milliseconds
    val category: String
) {
    val durationMillis: Long get() = endTime - startTime
    
    // Calculates the percentage of the program that has already elapsed (0.0 to 1.0)
    val progress: Float
        get() {
            val now = System.currentTimeMillis()
            if (now <= startTime) return 0.0f
            if (now >= endTime) return 1.0f
            return (now - startTime).toFloat() / durationMillis.toFloat()
        }
        
    // Returns formatted times (e.g. "20:30 - 21:45")
    val formattedTimeRange: String
        get() {
            val startCal = Calendar.getInstance().apply { timeInMillis = startTime }
            val endCal = Calendar.getInstance().apply { timeInMillis = endTime }
            return String.format(
                "%02d:%02d - %02d:%02d",
                startCal.get(Calendar.HOUR_OF_DAY),
                startCal.get(Calendar.MINUTE),
                endCal.get(Calendar.HOUR_OF_DAY),
                endCal.get(Calendar.MINUTE)
            )
        }
}

object EpgManager {

    private val sportsTemplates = listOf(
        Pair("Direct: Ligue 1 Uber Eats", "Le choc du championnat de France de football à suivre en exclusivité avec toutes les analyses d'avant et d'après-match."),
        Pair("L'Équipe du Soir", "Débat animé, décryptage, analyses tactiques et avis tranchés des éditorialistes de la rédaction sportive."),
        Pair("Grand Prix de Formule 1", "Suivez la course en direct avec les commentaires de nos experts et les réactions à chaud dans les stands."),
        Pair("Légendes du Sport", "Documentaire immersif sur les plus grands champions qui ont marqué l'histoire moderne du sport mondial."),
        Pair("NBA Live: Warriors vs Lakers", "La nuit américaine en direct ! Une confrontation majeure entre deux franchises légendaires du basketball."),
        Pair("Direct: Tour de France", "Suivez l'étape reine de la journée de la plus grande course cycliste au monde avec nos consultants."),
        Pair("Rugby: Top 14", "Match crucial du championnat de France de rugby, une bataille physique et tactique d'exception."),
        Pair("Tennis: Roland Garros", "Suivez les rencontres phares sur le court central avec les meilleurs joueurs et joueuses mondiaux.")
    )

    private val moviesTemplates = listOf(
        Pair("Inception", "Un cambrioleur d'élite, spécialisé dans l'infiltration des rêves, se voit offrir une chance de rachat s'il réalise l'impossible : une conception."),
        Pair("Interstellar", "Un groupe d'explorateurs utilise une faille récemment découverte dans l'espace-temps pour repousser les limites humaines et sauver l'humanité."),
        Pair("La La Land", "Au cœur de Los Angeles, une actrice en devenir et un pianiste de jazz passionné naviguent entre amour et ambitions professionnelles."),
        Pair("Le Parrain", "L'histoire dramatique d'une famille mafieuse italo-américaine dirigée par le patriarche Don Vito Corleone, entre loyauté et trahison."),
        Pair("Dune: Deuxième Partie", "Paul Atréides s'allie aux Fremen pour mener la révolte contre les Harkonnen, tout en affrontant son destin messianique."),
        Pair("Ciné-Club Classique", "Rediffusion d'un chef-d'œuvre restauré du cinéma d'auteur avec présentation critique en préambule."),
        Pair("Le Journal des Sorties", "Toutes les actualités cinématographiques de la semaine, les bandes-annonces marquantes et des interviews exclusives.")
    )

    private val newsTemplates = listOf(
        Pair("Le Journal de 20 Heures", "Le grand rendez-vous quotidien de l'information nationale et internationale, reportages sur le terrain et invités spéciaux."),
        Pair("La Grande Matinale Info", "Réveil d'information en direct : l'essentiel de l'actualité, la revue de presse, l'invité politique et la météo."),
        Pair("Débat Public", "Une table ronde réunissant des experts de tous bords politiques et sociaux pour débattre du grand sujet de société de la semaine."),
        Pair("Édition Spéciale direct", "Flash info continu et décryptages en temps réel consacrés au grand événement politique de la journée."),
        Pair("L'Hebdo de l'Économie", "Magazine hebdomadaire décryptant les tendances des marchés, les décisions monétaires et les enjeux de la consommation."),
        Pair("Reportage Exclusif", "Une immersion journalistique approfondie de 52 minutes sur un sujet géopolitique majeur ou un fait de société."),
        Pair("Planète Terre", "Actualités environnementales, innovations écologiques et reportages scientifiques aux quatre coins du globe.")
    )

    private val kidsTemplates = listOf(
        Pair("Les Simpson", "Les péripéties quotidiennes et hilarantes d'une famille américaine moyenne vivant dans la ville dysfonctionnelle de Springfield."),
        Pair("Bob l'Éponge", "Plongez dans les profondeurs de l'océan à Bikini Bottom pour suivre les aventures absurdes d'une éponge de mer et de son ami Patrick."),
        Pair("Miraculous, les aventures de Ladybug", "Deux collégiens parisiens ordinaires se transforment en super-héros pour défendre la ville des super-vilains."),
        Pair("Pokémon: Les Horizons", "Partez à la découverte de nouvelles régions fantastiques aux côtés d'entraîneurs de Pokémon déterminés à devenir champions."),
        Pair("L'Atelier Créatif des Juniors", "Une émission interactive apprenant aux plus jeunes à réaliser des activités manuelles et artistiques amusantes chez eux.")
    )

    private val musicTemplates = listOf(
        Pair("Top 50 Hits", "Le classement de référence des 50 morceaux les plus écoutés de la semaine avec diffusion des vidéoclips officiels."),
        Pair("Hit Story francophone", "Redécouvrez l'histoire des plus grands tubes de la variété française à travers des anecdotes et archives exclusives."),
        Pair("Morning Electro-Pop Mix", "La playlist parfaite pour démarrer la journée du bon pied : des sonorités pop dynamiques et des nouveautés rythmées."),
        Pair("Concert Privé Live", "Enregistrement d'une performance live acoustique exclusive d'un artiste ou groupe international de premier plan."),
        Pair("Club DJ Set", "Mix exclusif réalisé par des DJ de renom mondial pour animer votre soirée avec le meilleur de la musique de club.")
    )

    private val generalTemplates = listOf(
        Pair("Le Bureau des Légendes", "Au cœur de la DGSE, les agents formés pour infiltrer des milieux hostiles doivent gérer la frontière ténue entre leur identité fictive et réelle."),
        Pair("Des Racines et des Ailes", "Une exploration historique, géographique et culturelle des magnifiques régions de France et d'ailleurs, à la rencontre de passionnés."),
        Pair("Questions pour un Champion", "Le célèbre jeu télévisé de culture générale où quatre candidats s'affrontent sur des questions de rapidité et d'érudition."),
        Pair("Koh-Lanta: Les Aventuriers", "Vingt candidats abandonnés sur une île tropicale doivent faire preuve de stratégie et d'endurance pour remporter l'aventure ultime."),
        Pair("La Vie Secrète des Félins", "Un documentaire animalier captivant suivant le quotidien de plusieurs espèces de félins sauvages dans leur habitat naturel."),
        Pair("Le Grand Jeu de la Science", "Émission de vulgarisation scientifique pour toute la famille avec expériences ludiques et explications fascinantes."),
        Pair("Série Suspense: L'Affaire Noire", "Série dramatique haletante où une inspectrice déterminée tente de dénouer les secrets d'une affaire policière complexe.")
    )

    private fun getChannelSeed(channelName: String): Long {
        var hash = 1125899906842597L
        for (char in channelName) {
            hash = 31 * hash + char.code.toLong()
        }
        return Math.abs(hash)
    }

    private fun getGenreTemplates(channelName: String, groupTitle: String): List<Pair<String, String>> {
        val nameLower = channelName.lowercase()
        val groupLower = groupTitle.lowercase()
        
        return when {
            nameLower.contains("sport") || nameLower.contains("foot") || nameLower.contains("bein") ||
            nameLower.contains("espn") || nameLower.contains("lequipe") || nameLower.contains("rmc") ||
            nameLower.contains("moto") || nameLower.contains("f1") || nameLower.contains("tennis") ||
            groupLower.contains("sport") -> sportsTemplates
            
            nameLower.contains("film") || nameLower.contains("cine") || nameLower.contains("movie") ||
            nameLower.contains("hbo") || nameLower.contains("action") || nameLower.contains("ocs") ||
            groupLower.contains("cine") || groupLower.contains("movie") || groupLower.contains("film") -> moviesTemplates
            
            nameLower.contains("news") || nameLower.contains("info") || nameLower.contains("bfm") ||
            nameLower.contains("lci") || nameLower.contains("cnews") || nameLower.contains("cnn") ||
            nameLower.contains("bbc") || groupLower.contains("news") || groupLower.contains("info") -> newsTemplates
            
            nameLower.contains("kids") || nameLower.contains("enfant") || nameLower.contains("disney") ||
            nameLower.contains("cartoon") || nameLower.contains("gulli") || nameLower.contains("junior") ||
            groupLower.contains("kid") || groupLower.contains("jeunesse") || groupLower.contains("enfant") -> kidsTemplates
            
            nameLower.contains("music") || nameLower.contains("musique") || nameLower.contains("mtv") ||
            nameLower.contains("m6 music") || nameLower.contains("nrj") || nameLower.contains("hit") ||
            groupLower.contains("music") || groupLower.contains("musique") -> musicTemplates
            
            else -> generalTemplates
        }
    }

    /**
     * Generates a list of EpgProgram for a channel for the current day.
     * The list covers 24 hours from the start of the current day.
     */
    fun generateEpgForChannel(channelName: String, groupTitle: String): List<EpgProgram> {
        val seed = getChannelSeed(channelName)
        val calendar = Calendar.getInstance()
        
        // Use year and day-of-year to make the seed change daily, but stay constant all day
        val daySeed = calendar.get(Calendar.YEAR) * 1000L + calendar.get(Calendar.DAY_OF_YEAR)
        val random = Random(seed + daySeed)
        
        // Start of today (midnight)
        val startOfDayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var currentTimeCursor = startOfDayCal.timeInMillis
        
        // End of today (midnight tomorrow)
        val endOfDayCal = Calendar.getInstance().apply {
            timeInMillis = startOfDayCal.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val endOfDay = endOfDayCal.timeInMillis
        
        val templates = getGenreTemplates(channelName, groupTitle)
        val category = when (templates) {
            sportsTemplates -> "Sports"
            moviesTemplates -> "Cinéma"
            newsTemplates -> "Actualités"
            kidsTemplates -> "Jeunesse"
            musicTemplates -> "Musique"
            else -> "Général"
        }
        
        val programs = mutableListOf<EpgProgram>()
        
        // Shuffle templates deterministically to create a varied schedule
        val shuffledTemplates = templates.shuffled(random)
        var templateIndex = 0
        
        while (currentTimeCursor < endOfDay) {
            val template = shuffledTemplates[templateIndex % shuffledTemplates.size]
            templateIndex++
            
            // Program durations are 30, 45, 60, 90, or 120 minutes
            val durationOptions = listOf(30, 45, 60, 90, 120)
            val durationMins = durationOptions[random.nextInt(durationOptions.size)]
            val durationMillis = durationMins * 60 * 1000L
            
            val programEndTime = currentTimeCursor + durationMillis
            
            programs.add(
                EpgProgram(
                    title = template.first,
                    description = template.second,
                    startTime = currentTimeCursor,
                    endTime = programEndTime,
                    category = category
                )
            )
            
            currentTimeCursor = programEndTime
        }
        
        return programs
    }

    /**
     * Gets the active program playing *right now* for a channel.
     */
    fun getCurrentProgram(channelName: String, groupTitle: String): EpgProgram {
        val now = System.currentTimeMillis()
        val epg = generateEpgForChannel(channelName, groupTitle)
        
        // Find the program covering 'now'
        val active = epg.firstOrNull { now >= it.startTime && now < it.endTime }
        if (active != null) return active
        
        // Fallback in case time went beyond midnight of the generated range
        return EpgProgram(
            title = "Direct TV",
            description = "Profitez de votre flux en direct sur votre application.",
            startTime = now - 30 * 60 * 1000L,
            endTime = now + 60 * 60 * 1000L,
            category = "Général"
        )
    }

    /**
     * Gets the list of next programs (up to 'count' items) after the current one.
     */
    fun getUpcomingPrograms(channelName: String, groupTitle: String, count: Int = 3): List<EpgProgram> {
        val now = System.currentTimeMillis()
        val epg = generateEpgForChannel(channelName, groupTitle)
        
        // Filter programs that start after the current program's start time (or now)
        return epg.filter { it.startTime > now }.take(count)
    }
}
