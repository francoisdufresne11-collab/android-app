package com.example.data

object M3uParser {
    fun parse(content: String, playlistId: Long): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.reader().readLines()
        
        var currentInfoLine: String? = null
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            
            if (trimmed.startsWith("#EXTM3U")) {
                continue
            } else if (trimmed.startsWith("#EXTINF")) {
                currentInfoLine = trimmed
            } else if (!trimmed.startsWith("#")) {
                // Il s'agit d'une URL de flux de streaming !
                if (currentInfoLine != null) {
                    channels.add(parseChannel(currentInfoLine, trimmed, playlistId))
                    currentInfoLine = null
                } else {
                    // URL sans métadonnées EXTINF (fall-back)
                    val urlName = trimmed.substringAfterLast("/").substringBefore("?")
                    channels.add(
                        Channel(
                            playlistId = playlistId,
                            name = if (urlName.length > 2) urlName else "Lien de streaming",
                            streamUrl = trimmed,
                            groupTitle = "Divers",
                            logoUrl = null
                        )
                    )
                }
            }
        }
        return channels
    }

    private fun parseChannel(infoLine: String, streamUrl: String, playlistId: Long): Channel {
        // Syntaxe typique : #EXTINF:-1 tvg-id="TF1" tvg-logo="https://..." group-title="Général",TF1 HD
        val commaIndex = infoLine.lastIndexOf(',')
        var name = if (commaIndex != -1 && commaIndex < infoLine.length - 1) {
            infoLine.substring(commaIndex + 1).trim()
        } else {
            ""
        }
        
        val logoUrl = findAttribute(infoLine, "tvg-logo") ?: findAttribute(infoLine, "logo")
        val groupTitle = findAttribute(infoLine, "group-title") 
            ?: findAttribute(infoLine, "group") 
            ?: "Général"
            
        if (name.isEmpty()) {
            name = findAttribute(infoLine, "tvg-name") 
                ?: findAttribute(infoLine, "tvg-id") 
                ?: "Chaîne sans nom"
        }
        
        return Channel(
            playlistId = playlistId,
            name = name,
            streamUrl = streamUrl,
            groupTitle = groupTitle,
            logoUrl = logoUrl
        )
    }

    private fun findAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"([^\"]*)\"".toRegex(RegexOption.IGNORE_CASE)
        return pattern.find(line)?.groupValues?.get(1)
    }
}
