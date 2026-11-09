package com.cobalt.android.ui.games

/**
 * The web-game entry (Phase F6).
 *
 * VidMate ships a games hub; the honest, dependency-free equivalent here is a
 * curated list of portals that serve HTML5 games in a plain browser. There is
 * no bundled game runtime and no store — every entry opens in the app's own
 * browser (F1), which already handles the media sniffing and download hand-off,
 * so a game page and a video page behave the same way.
 */
object GamePortalCatalog {

    data class Portal(val name: String, val url: String, val blurb: String)

    val portals: List<Portal> = listOf(
        Portal("Poki", "https://poki.com/", "thousands of browser games"),
        Portal("CrazyGames", "https://www.crazygames.com/", "action, puzzle, io"),
        Portal("itch.io (web)", "https://itch.io/games/html5", "indie HTML5 games"),
        Portal("Chess", "https://www.chess.com/play/computer", "play the computer"),
        Portal("Sudoku", "https://sudoku.com/", "classic sudoku"),
        Portal("2048", "https://play2048.co/", "the sliding-tile puzzle")
    )
}
