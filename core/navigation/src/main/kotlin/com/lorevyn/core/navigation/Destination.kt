package com.lorevyn.core.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

/**
 * All navigable destinations in the Lorevyn app.
 * Source: D-03 Navigation Architecture v1.8 — Section 3.
 *
 * Rules:
 * - Full-screen destinations push onto the back stack.
 * - Bottom sheets (BS-01 through BS-12) are NOT Destinations.
 *   They are managed via Compose state in their parent screens.
 * - Nested graph objects (ReadingGraph, LibraryGraph, etc.) wrap each
 *   tab's sub-graph so Compose Navigation can save/restore tab state.
 *
 * Back-stack rule: tab root screens never push a duplicate onto the
 * back stack (launchSingleTop = true, restoreState = true in nav options).
 */
sealed class Destination(val route: String) {

    // ── Nested graph routes (one per tab + onboarding) ────────────────────────
    // These are the route strings passed to navigation(route = ...) blocks.
    // They are never rendered as screens — only their children are.
    object OnboardingGraph : Destination("graph/onboarding")
    object ReadingGraph    : Destination("graph/reading")
    object LibraryGraph    : Destination("graph/library")
    object DiscoverGraph   : Destination("graph/discover")
    object JourneyGraph    : Destination("graph/journey")

    // ── Onboarding flow — :feature:onboarding (SCR-01 → SCR-05) ─────────────
    // Shown once. DataStore flag onboardingComplete gates entry.
    // Back gesture disabled on SCR-01 (app exits to home).
    // Re-launch mid-flow resumes at SCR-01 (safest default).
    object OnboardingWelcome   : Destination("onboarding/welcome")   // SCR-01
    object OnboardingHistory   : Destination("onboarding/history")   // SCR-02
    object OnboardingTaste     : Destination("onboarding/taste")     // SCR-03
    object OnboardingGoal      : Destination("onboarding/goal")      // SCR-04
    object OnboardingCompanion : Destination("onboarding/companion") // SCR-05

    // ── Tab root screens ──────────────────────────────────────────────────────
    // These are the start destinations of their respective nested graphs.
    // Bottom nav bar + top bar visible only on these four routes.
    object Reading  : Destination("reading")  // SCR-06 — :feature:reading
    object Library  : Destination("library")  // SCR-07 — :feature:library
    object WantToRead    : Destination("library/want_to_read")  // SCR-07a
    object FinishedBooks : Destination("library/finished")      // SCR-07b
    object SeriesTracker : Destination("library/series_tracker") // SCR-07c — TD-111
    object ManageShelves : Destination("library/shelves")         // SCR-07d — Custom Shelves

    // SCR-07e — Shelf Detail. Tapped from ManageShelves row.
    // Shows books on a single shelf; + FAB opens BookPicker modal sheet to add more.
    // Route literal "library/shelves/{shelfId}" — ARG_SHELF_ID = "shelfId" must match.
    object ShelfDetail : Destination("library/shelves/{shelfId}") {
        const val ARG_SHELF_ID = "shelfId"
        fun createRoute(shelfId: Long): String = "library/shelves/$shelfId"
        fun navArguments() = listOf(
            navArgument(ARG_SHELF_ID) { type = NavType.LongType }
        )
    }
    object Discover : Destination("discover") // SCR-15 — :feature:discovery
    object Journey  : Destination("journey")  // SCR-08 — :feature:journey

    // ── Journey sub-screen ────────────────────────────────────────────────────
    // SCR-08a — Reading Fingerprint. No bottom nav shown. Back → SCR-08.
    object ReadingFingerprint : Destination("journey/fingerprint") // SCR-08a

    // SCR-16 — Annual Wrap-Up. Accessible from Journey tab from Nov 1.
    object AnnualWrapUp : Destination("journey/wrapup") // SCR-16

    // SCR-08b — Reading Spine. Premium. Horizontal timeline of finished books.
    // No bottom nav shown. Back → SCR-08 (Journey tab).
    object ReadingSpine : Destination("journey/reading_spine") // SCR-08b

    // ── Book Detail — :feature:book-detail ───────────────────────────────────
    // SCR-09 — pushed from Reading hero card, Library card, Discovery card.
    // Bottom nav hidden. Back pops to origin screen.
    // Route literal "book_detail/{bookId}" — ARG_BOOK_ID = "bookId" must match.
    object BookDetail : Destination("book_detail/{bookId}") {
        const val ARG_BOOK_ID = "bookId"
        fun createRoute(bookId: Long): String = "book_detail/$bookId"
        fun navArguments() = listOf(
            navArgument(ARG_BOOK_ID) { type = NavType.LongType }
        )
        fun deepLinks() = listOf(
            navDeepLink { uriPattern = "bookshelf://book/{$ARG_BOOK_ID}" }
        )
    }

    // ── Add Book flow — :feature:add-book ────────────────────────────────────
    // Modal full-screen destinations. No bottom nav.
    object AddBookSearch  : Destination("add_book/search")   // SCR-10
    object AddBookScanner : Destination("add_book/scanner")  // SCR-11
    // SCR-12 — optional isbn query param pre-fills the ISBN field when coming from scanner
    object AddBookManual  : Destination("add_book/manual?isbn={isbn}") {
        const val ARG_ISBN = "isbn"
        fun createRoute(isbn: String? = null): String =
            if (isbn != null) "add_book/manual?isbn=$isbn" else "add_book/manual"
    }

    // ── Import / Migration Wizard — :feature:migration ────────────────────────
    // SCR-13 — 5-step wizard. Modal full-screen. Back at Step 5 disabled during
    // active DB write. Cancel at Steps 1–4 returns to originating screen.
    object Migration : Destination("migration") // SCR-13

    // ── Companion Picker — Settings mode (Decision #59/#60) ─────────────────
    // Accessible from BS-08 Settings → "Your companion" row.
    // Uses OnboardingCompanionScreen with CompanionPickerMode.SETTINGS.
    object CompanionPicker : Destination("companion_picker")

    // ── Paywall — :feature:billing ────────────────────────────────────────────
    // SCR-14 — pushed when user taps a Premium-gated feature.
    // featureName string used for copy and tier-card highlighting.
    object Paywall : Destination("paywall/{featureName}") {
        const val ARG_FEATURE_NAME = "featureName"
        fun createRoute(featureName: String): String = "paywall/$featureName"
        fun navArguments() = listOf(
            navArgument(ARG_FEATURE_NAME) { type = NavType.StringType }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    companion object {

        /**
         * Deep link scheme for all Lorevyn deep links.
         * AndroidManifest intent-filter is registered in Phase 3 Task 10.
         * Routes are defined here so the NavGraph can recognise them
         * even before the manifest entries exist.
         * Source: D-03 Section 7.
         */
        const val DEEP_LINK_BASE = "bookshelf:/"

        // Registered deep link URI patterns (used in navDeepLink blocks below)
        const val DL_READING        = "bookshelf://reading"
        const val DL_LIBRARY        = "bookshelf://vault"
        const val DL_LIBRARY_SHELF  = "bookshelf://vault?shelf={shelfName}"
        const val DL_JOURNEY        = "bookshelf://insights"
        const val DL_JOURNEY_YEAR   = "bookshelf://insights?year={year}"
        const val DL_LOG_BOOK       = "bookshelf://log/{bookId}"
        const val DL_MIGRATION      = "bookshelf://migration"
        // bookshelf://book/{bookId} is defined in BookDetail.deepLinks()

        /**
         * The four routes where shell chrome (top bar + bottom nav + FAB)
         * is visible. All other routes are full-screen and hide the chrome.
         */
        val TAB_ROOT_ROUTES: Set<String> = setOf(
            Reading.route,
            Library.route,
            Discover.route,
            Journey.route
        )

        /**
         * Routes where the Log FAB is visible.
         * Decision #6: FAB hidden on Journey tab. Hidden on all full-screen
         * destinations. Visible on Reading, Library, and Discover roots.
         */
        val LOG_FAB_VISIBLE_ROUTES: Set<String> = setOf(
            Reading.route,
            Library.route,
            Discover.route
        )
    }
}
