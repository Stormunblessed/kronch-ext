// use an integer for version numbers
version = 3


cloudstream {
    language = "de"
    // All of these properties are optional, you can safely remove them

    description = "Schauen Sie sich alle Crunchyroll-Anime über die Kamyroll-API an. Dieser Anbieter zeigt nur Anime mit deutschen Untertiteln/Synchronisationen."
    authors = listOf("Stormunblessed")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 0 // will be 3 if unspecified
    tvTypes = listOf(
        "Anime",
        "OVA",
    )

    iconUrl = "https://raw.githubusercontent.com/Stormunblessed/IPTV-CR-NIC/main/logos/K-14-11-2022.png"
}