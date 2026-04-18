package com.lorevyn.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.lorevyn.core.domain.model.CompanionType
import com.lorevyn.core.navigation.AddBookMode
import com.lorevyn.core.navigation.CompanionPickerMode
import com.lorevyn.core.navigation.Destination
import com.lorevyn.feature.addbook.ui.AddBookManualScreen
import com.lorevyn.feature.addbook.ui.AddBookSearchScreen
import com.lorevyn.feature.addbook.ui.AddBookViewModel
import com.lorevyn.feature.addbook.ui.IsbnScannerScreen
import com.lorevyn.feature.billing.PaywallScreen
import com.lorevyn.feature.bookdetail.BookDetailScreen
import com.lorevyn.feature.discovery.DiscoverTabScreen
import com.lorevyn.feature.library.FinishedBooksScreen
import com.lorevyn.feature.library.FavoritesScreen
import com.lorevyn.feature.library.LibraryTabScreen
import com.lorevyn.feature.library.SeriesTrackerScreen
import com.lorevyn.feature.library.WantToReadScreen
import com.lorevyn.feature.migration.MigrationWizardScreen
import com.lorevyn.feature.journey.AnnualWrapUpScreen
import com.lorevyn.feature.journey.JourneyTabScreen
import com.lorevyn.feature.journey.ReadingFingerprintScreen
import com.lorevyn.feature.journey.ReadingSpineScreen
import com.lorevyn.feature.onboarding.OnboardingCompanionScreen
import com.lorevyn.feature.onboarding.OnboardingGoalScreen
import com.lorevyn.feature.onboarding.OnboardingHistoryScreen
import com.lorevyn.feature.onboarding.OnboardingTasteScreen
import com.lorevyn.feature.onboarding.OnboardingWelcomeScreen
import com.lorevyn.feature.reading.ReadingTabScreen
import com.lorevyn.feature.reading.ReadingTabViewModel
import kotlinx.coroutines.flow.filterNotNull

/**
 * Root NavHost for the Lorevyn app.
 *
 * Wired screens:
 *   SCR-01–05  Onboarding          ✅
 *   SCR-06     Reading Tab         ✅ Phase 4 — ReadingTabViewModel wired
 *   SCR-07     Library Tab Hub     ✅ Phase 5
 *   SCR-07a    Want to Read        ✅ Phase 5
 *   SCR-07b    Finished Books      ✅ Phase 5
 *   SCR-09     Book Detail         ✅ Phase 4
 *   SCR-10     Add Book Search     ✅ Phase 4
 *   SCR-11     ISBN Scanner        ✅ Phase 4
 *   SCR-14     Paywall             ✅ Phase 9
 *   SCR-15     Discover Tab        ✅ Phase 10
 *
 * isPremium is collected in LorevynApp from PremiumStatusViewModel and passed
 * down here. NavController never leaves :app (AGENTS rule 6).
 */
@Composable
fun LorevynNavHost(
    navController: NavHostController,
    startDestination: String,
    isPremium: Boolean,
    currentCompanionType: CompanionType = CompanionType.OWL,
    onLogProgress: (bookId: Long?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier
    ) {

        // ── Onboarding graph ─────────────────────────────────────────────────
        navigation(
            startDestination = Destination.OnboardingWelcome.route,
            route            = Destination.OnboardingGraph.route
        ) {
            composable(route = Destination.OnboardingWelcome.route) {
                OnboardingWelcomeScreen()
            }
            composable(route = Destination.OnboardingHistory.route) {
                OnboardingHistoryScreen()
            }
            composable(route = Destination.OnboardingTaste.route) {
                OnboardingTasteScreen()
            }
            composable(route = Destination.OnboardingGoal.route) {
                OnboardingGoalScreen()
            }
            composable(route = Destination.OnboardingCompanion.route) {
                OnboardingCompanionScreen(
                    onNavigateToAddBook = { mode ->
                        navController.navigate("add_book?mode=${mode.name}") {
                            popUpTo(Destination.OnboardingGraph.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ── Reading tab graph ────────────────────────────────────────────────
        navigation(
            startDestination = Destination.Reading.route,
            route            = Destination.ReadingGraph.route
        ) {
            composable(
                route      = Destination.Reading.route,
                deepLinks  = listOf(navDeepLink { uriPattern = Destination.DL_READING })
            ) {
                val viewModel: ReadingTabViewModel = hiltViewModel()
                ReadingTabScreen(
                    viewModel            = viewModel,
                    onNavigateToBookDetail = { bookId ->
                        navController.navigate(Destination.BookDetail.createRoute(bookId))
                    },
                    onNavigateToAddBook = {
                        navController.navigate("add_book?mode=${AddBookMode.ADD_BOOK.name}")
                    },
                    onNavigateToPaywall = { featureName ->
                        navController.navigate(Destination.Paywall.createRoute(featureName))
                    }
                )
            }
        }

        // ── Library tab graph ────────────────────────────────────────────────
        navigation(
            startDestination = Destination.Library.route,
            route            = Destination.LibraryGraph.route
        ) {
            composable(
                route     = Destination.Library.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = Destination.DL_LIBRARY },
                    navDeepLink { uriPattern = Destination.DL_LIBRARY_SHELF }
                )
            ) {
                LibraryTabScreen(
                    onNavigateToBookDetail = { bookId ->
                        navController.navigate(Destination.BookDetail.createRoute(bookId))
                    },
                    onNavigateToWantToRead = {
                        navController.navigate(Destination.WantToRead.route)
                    },
                    onNavigateToFinished   = {
                        navController.navigate(Destination.FinishedBooks.route)
                    },
                    onNavigateToAddBook    = {
                        navController.navigate("add_book?mode=${AddBookMode.WANT_TO_READ.name}")
                    },
                    onNavigateToSeriesTracker = {
                        navController.navigate(Destination.SeriesTracker.route)
                    },
                    onNavigateToSeriesPaywall = {
                        navController.navigate(Destination.Paywall.createRoute("series_tracker"))
                    },
                    onNavigateToManageShelves = {
                        navController.navigate(Destination.ManageShelves.route)
                    },
                    isPremium = isPremium,
                )
            }

            composable(route = Destination.WantToRead.route) {
                WantToReadScreen(
                    onNavigateBack         = { navController.popBackStack() },
                    onNavigateToBookDetail = { bookId ->
                        navController.navigate(Destination.BookDetail.createRoute(bookId))
                    },
                    onNavigateToAddBook    = {
                        navController.navigate("add_book?mode=${AddBookMode.WANT_TO_READ.name}")
                    }
                )
            }

            composable(route = Destination.FinishedBooks.route) {
                FinishedBooksScreen(
                    onNavigateBack         = { navController.popBackStack() },
                    onNavigateToBookDetail = { bookId ->
                        navController.navigate(Destination.BookDetail.createRoute(bookId))
                    }
                )
            }

            // SCR-07c — Series Completion Tracker (TD-111)
            composable(route = Destination.SeriesTracker.route) {
                SeriesTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onBookClick    = { bookId ->
                        navController.navigate(Destination.BookDetail.createRoute(bookId))
                    },
                )
            }

            // SCR-07d — Manage Custom Shelves
            composable(route = Destination.ManageShelves.route) {
                com.lorevyn.feature.library.shelves.ManageShelvesScreen(
                    onBack = { navController.popBackStack() },
                    onShelfTapped = { shelfId ->
                        navController.navigate(Destination.ShelfDetail.createRoute(shelfId))
                    },
                )
            }

            // SCR-07e — Shelf Detail + BookPicker
            composable(
                route      = Destination.ShelfDetail.route,
                arguments  = Destination.ShelfDetail.navArguments(),
            ) {
                com.lorevyn.feature.library.shelves.ShelfDetailScreen(
                    onBack = { navController.popBackStack() },
                    onBookClick = { bookId ->
                        navController.navigate(Destination.BookDetail.createRoute(bookId))
                    },
                )
            }
        }

        // ── Discover tab graph ───────────────────────────────────────────────
        // isPremium from PremiumStatusViewModel via LorevynApp.
        // Decision #19: Best Sellers = free. Genre/Author/Mood+Pace = PREMIUM_DISCOVERY_LENSES.
        // Decision #65: Premium filter chips visible to all — tap triggers paywall for free users.
        // "Add to Want to Read" from bottom sheet navigates to AddBook flow.
        navigation(
            startDestination = Destination.Discover.route,
            route            = Destination.DiscoverGraph.route
        ) {
            composable(route = Destination.Discover.route) {
                DiscoverTabScreen(
                    isPremium           = isPremium,
                    onNavigateToPaywall = {
                        navController.navigate(
                            Destination.Paywall.createRoute("discovery")
                        )
                    },
                    onNavigateToAddBook = {
                        navController.navigate(
                            "add_book?mode=${AddBookMode.WANT_TO_READ.name}"
                        )
                    }
                )
            }
        }

        // ── Journey tab graph ────────────────────────────────────────────────
        navigation(
            startDestination = Destination.Journey.route,
            route            = Destination.JourneyGraph.route
        ) {
            composable(
                route     = Destination.Journey.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = Destination.DL_JOURNEY },
                    navDeepLink { uriPattern = Destination.DL_JOURNEY_YEAR }
                )
            ) {
                JourneyTabScreen(
                    isPremium                = isPremium,
                    onAnnualWrapUpTapped     = { navController.navigate(Destination.AnnualWrapUp.route) },
                    onNavigateToFavorites    = { navController.navigate("favorites") },
                    onNavigateToReadingSpine = { navController.navigate(Destination.ReadingSpine.route) },
                )
            }
            composable(route = Destination.ReadingFingerprint.route) {
                ReadingFingerprintScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(route = Destination.AnnualWrapUp.route) {
                AnnualWrapUpScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(route = Destination.ReadingSpine.route) {
                ReadingSpineScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // ── Book Detail — SCR-09 ─────────────────────────────────────────────
        composable(
            route     = Destination.BookDetail.route,
            arguments = listOf(
                navArgument(Destination.BookDetail.ARG_BOOK_ID) {
                    type = NavType.LongType
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "${Destination.DEEP_LINK_BASE}/book/{${Destination.BookDetail.ARG_BOOK_ID}}"
                }
            )
        ) {
            BookDetailScreen(
                    onNavigateBack    = { navController.popBackStack() },
                    isPremium         = isPremium,
                    onNavigateToReading = {
                    navController.navigate(Destination.LibraryGraph.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                onNavigateToLog = { bookId -> onLogProgress(bookId) }
            )
        }

        // ── Add Book Search — SCR-10 ─────────────────────────────────────────
        composable(
            route     = "add_book?mode={mode}",
            arguments = listOf(
                navArgument("mode") {
                    type         = NavType.StringType
                    defaultValue = AddBookMode.ADD_BOOK.name
                }
            )
        ) { backStackEntry ->
            val modeString = backStackEntry.arguments
                ?.getString("mode") ?: AddBookMode.ADD_BOOK.name
            val mode = runCatching {
                AddBookMode.valueOf(modeString)
            }.getOrDefault(AddBookMode.ADD_BOOK)

            val viewModel: AddBookViewModel = hiltViewModel()

            LaunchedEffect(Unit) {
                backStackEntry.savedStateHandle
                    .getStateFlow<String?>("scanned_isbn", null)
                    .filterNotNull()
                    .collect { isbn ->
                        backStackEntry.savedStateHandle.remove<String>("scanned_isbn")
                        viewModel.onIsbnScanned(isbn)
                    }
            }

            AddBookSearchScreen(
                mode                   = mode,
                onNavigateBack         = { navController.popBackStack() },
                onNavigateToScanner    = {
                    navController.navigate(Destination.AddBookScanner.route)
                },
                onNavigateToManualEntry = { isbn ->
                    navController.navigate(Destination.AddBookManual.createRoute(isbn))
                },
                onNavigateToBookDetail = { bookId ->
                    navController.navigate(Destination.BookDetail.createRoute(bookId)) {
                        popUpTo("add_book?mode=${mode.name}") { inclusive = true }
                    }
                },
                onNavigateToReading    = {
                    navController.navigate(Destination.ReadingGraph.route) {
                        popUpTo("add_book?mode=${mode.name}") { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }

        // ── ISBN Scanner — SCR-11 ────────────────────────────────────────────
        composable(route = Destination.AddBookScanner.route) {
            IsbnScannerScreen(
                onIsbnScanned  = { isbn ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("scanned_isbn", isbn)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() },
                onManualEntryTapped = {
                    navController.navigate(Destination.AddBookManual.route) {
                        popUpTo(Destination.AddBookScanner.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Add Book Manual — SCR-12 ─────────────────────────────────────────
        composable(
            route     = Destination.AddBookManual.route,
            arguments = listOf(
                navArgument(Destination.AddBookManual.ARG_ISBN) {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                }
            )
        ) {
            AddBookManualScreen(
                onNavigateBack         = { navController.popBackStack() },
                onNavigateToBookDetail = { bookId ->
                    navController.navigate(Destination.BookDetail.createRoute(bookId)) {
                        popUpTo(Destination.AddBookManual.route) { inclusive = true }
                    }
                },
                onNavigateToReading    = {
                    navController.navigate(Destination.ReadingGraph.route) {
                        popUpTo(Destination.AddBookManual.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Migration Wizard ─────────────────────────────────────────────────
        composable(
            route     = Destination.Migration.route,
            deepLinks = listOf(navDeepLink { uriPattern = Destination.DL_MIGRATION })
        ) {
            val source = it.arguments?.getString("source")
            MigrationWizardScreen(
                preSelectedSource   = source,
                onNavigateToAddBook = { _ ->
                    navController.navigate("add_book?mode=${AddBookMode.ADD_BOOK.name}")
                }
            )
        }

        // ── Companion Picker — Settings mode (Decision #59/#60) ────────────────
        // Top-level composable — accessible from Settings on any tab.
        // Uses same OnboardingCompanionScreen in SETTINGS mode (back arrow,
        // "Save companion" CTA, pops back to Settings on confirm).
        composable(route = Destination.CompanionPicker.route) {
            // W-21: pre-highlight the user's current companion (Decision #76)
            val companionIndex = when (currentCompanionType) {
                CompanionType.OWL    -> 0
                CompanionType.CAT    -> 1
                CompanionType.DOG    -> 2
                CompanionType.RABBIT -> 3
            }
            OnboardingCompanionScreen(
                mode                  = CompanionPickerMode.SETTINGS,
                initialCompanionIndex = companionIndex,
                onNavigateBack        = { navController.popBackStack() }
            )
        }

        // ── Paywall — SCR-14 ─────────────────────────────────────────────────
        composable(
            route     = Destination.Paywall.route,
            arguments = listOf(
                navArgument(Destination.Paywall.ARG_FEATURE_NAME) {
                    type = NavType.StringType
                }
            )
        ) {
            PaywallScreen(
                onNavigateBack   = { navController.popBackStack() },
                onPurchaseSuccess = { navController.popBackStack() }
            )
        }

        // ── Favorites — Books That Found Me (TD-107) ─────────────────────────
        composable(route = "favorites") {
            FavoritesScreen(
                onBack      = { navController.popBackStack() },
                onBookClick = { bookId ->
                    navController.navigate(Destination.BookDetail.createRoute(bookId))
                }
            )
        }
    }
}

@Composable
private fun NavPlaceholder(label: String) {
    Box(
        modifier          = Modifier.fillMaxSize(),
        contentAlignment  = Alignment.Center
    ) {
        Text(text = label)
    }
}
