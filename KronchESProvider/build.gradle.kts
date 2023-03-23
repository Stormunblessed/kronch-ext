// use an integer for version numbers
version = 18


cloudstream {
    language = "es"
    // All of these properties are optional, you can safely remove them

    description = "Con esta extensión podrás ver anime subtitulado/doblado disponible en el catálogo de Estados Unidos de ese sitio."
    authors = listOf("Stormunblessed")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Anime",
        "OVA",
    )

    iconUrl = "https://raw.githubusercontent.com/Stormunblessed/IPTV-CR-NIC/main/logos/kronch.png"
}
