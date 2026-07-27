package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.SportsRugby
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.West
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.ChatMessageDto
import com.xhacker.cedal.data.StickerDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// A real 1-on-1 conversation with an accepted friend (see ChatService
// server-side) - restyled to match cedal-mobile's chat screen: solid-cyan
// "mine" bubbles vs. translucent-navy "theirs" bubbles, both with one
// squared tail-corner instead of an actual speech-bubble tail, a "neural
// transmission" input bar, emoji reactions, reply/quote, and edit/delete
// (5-minute edit window) via long-press - the visual identity and the most-
// used real interactions, not the full media/sticker/poll/voice-note set.
// Polls every few seconds for new messages rather than a live-listener
// transport, same "pull instead of push" tradeoff used everywhere else in
// this app that doesn't have one yet.

private val EDIT_WINDOW_MS = 5 * 60 * 1000L
private val BUBBLE_MINE_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
private val BUBBLE_THEIRS_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)

// Icon pack - a third default set alongside Emoji/Stickers, real vector
// icons (Material Icons Extended - already a dependency; there's no Lucide
// binding for Compose/Kotlin, this is the native equivalent with a
// comparably huge catalog) rather than emoji, so it reads as visually
// distinct from both. Sent as "icon:<Name>" (see sendSticker/MessageBubble)
// so rendering can tell it apart from a URL sticker or an emoji glyph.
// Shared with the AI chat screens (Corneal/ARC/Code AI) - see
// StickerPickerOverlay's EMOJI/ICON tabs, reused there via same-package
// visibility rather than a duplicate copy.
val ALL_ICONS: List<Pair<String, ImageVector>> = listOf(
    // Reactions
    "Heart" to Icons.Filled.Favorite,
    "Heart Outline" to Icons.Filled.FavoriteBorder,
    "Star" to Icons.Filled.Star,
    "Star Outline" to Icons.Filled.StarBorder,
    "Star Half" to Icons.Filled.StarHalf,
    "Mood" to Icons.Filled.Mood,
    "Mood Bad" to Icons.Filled.MoodBad,
    "Satisfied" to Icons.Filled.SentimentSatisfied,
    "Very Satisfied" to Icons.Filled.SentimentVerySatisfied,
    "Dissatisfied" to Icons.Filled.SentimentDissatisfied,
    "Very Dissatisfied" to Icons.Filled.SentimentVeryDissatisfied,
    "Neutral" to Icons.Filled.SentimentNeutral,
    "Thumbs Up" to Icons.Filled.ThumbUp,
    "Thumbs Down" to Icons.Filled.ThumbDown,
    // Nature / weather
    "Sun" to Icons.Filled.WbSunny,
    "Cloud" to Icons.Filled.Cloud,
    "Umbrella" to Icons.Filled.Umbrella,
    "Snowflake" to Icons.Filled.AcUnit,
    "Water Drop" to Icons.Filled.WaterDrop,
    "Lightning" to Icons.Filled.Bolt,
    "Night" to Icons.Filled.NightsStay,
    "Brightness Low" to Icons.Filled.Brightness4,
    "Brightness High" to Icons.Filled.Brightness7,
    "Fire" to Icons.Filled.LocalFireDepartment,
    "Grass" to Icons.Filled.Grass,
    "Spa" to Icons.Filled.Spa,
    "Paw" to Icons.Filled.Pets,
    // Food / drink
    "Coffee" to Icons.Filled.LocalCafe,
    "Pizza" to Icons.Filled.LocalPizza,
    "Drinks" to Icons.Filled.LocalBar,
    "Cake" to Icons.Filled.Cake,
    "Restaurant" to Icons.Filled.Restaurant,
    "Fast Food" to Icons.Filled.Fastfood,
    "Ice Cream" to Icons.Filled.Icecream,
    "Dining" to Icons.Filled.LocalDining,
    "Beverage" to Icons.Filled.EmojiFoodBeverage,
    "Kitchen" to Icons.Filled.Kitchen,
    // Activities / sports
    "Soccer" to Icons.Filled.SportsSoccer,
    "Basketball" to Icons.Filled.SportsBasketball,
    "Tennis" to Icons.Filled.SportsTennis,
    "Gaming" to Icons.Filled.SportsEsports,
    "Football" to Icons.Filled.SportsFootball,
    "Baseball" to Icons.Filled.SportsBaseball,
    "Golf" to Icons.Filled.SportsGolf,
    "Handball" to Icons.Filled.SportsHandball,
    "Hockey" to Icons.Filled.SportsHockey,
    "Rugby" to Icons.Filled.SportsRugby,
    "Volleyball" to Icons.Filled.SportsVolleyball,
    "Cricket" to Icons.Filled.SportsCricket,
    "Martial Arts" to Icons.Filled.SportsMartialArts,
    "Motorsports" to Icons.Filled.SportsMotorsports,
    "Gym" to Icons.Filled.FitnessCenter,
    "Running" to Icons.Filled.DirectionsRun,
    "Cycling" to Icons.Filled.DirectionsBike,
    "Walking" to Icons.Filled.DirectionsWalk,
    "Swimming" to Icons.Filled.Pool,
    "Hiking" to Icons.Filled.Hiking,
    "Meditation" to Icons.Filled.SelfImprovement,
    // Travel / places
    "Flight" to Icons.Filled.Flight,
    "Takeoff" to Icons.Filled.FlightTakeoff,
    "Car" to Icons.Filled.DirectionsCar,
    "Bus" to Icons.Filled.DirectionsBus,
    "Boat" to Icons.Filled.DirectionsBoat,
    "Train" to Icons.Filled.Train,
    "Sailing" to Icons.Filled.Sailing,
    "Rocket" to Icons.Filled.RocketLaunch,
    "Globe" to Icons.Filled.Public,
    "Map" to Icons.Filled.Map,
    "Explore" to Icons.Filled.Explore,
    "Home" to Icons.Filled.Home,
    "Apartment" to Icons.Filled.Apartment,
    "Location" to Icons.Filled.LocationOn,
    "Mountain" to Icons.Filled.Terrain,
    "Beach" to Icons.Filled.BeachAccess,
    "Castle" to Icons.Filled.Castle,
    "Cabin" to Icons.Filled.Cabin,
    "Gallery" to Icons.Filled.PhotoLibrary,
    // Objects
    "Idea" to Icons.Filled.Lightbulb,
    "Tools" to Icons.Filled.Build,
    "Settings" to Icons.Filled.Settings,
    "Key" to Icons.Filled.Key,
    "Lock" to Icons.Filled.Lock,
    "Unlock" to Icons.Filled.LockOpen,
    "Backpack" to Icons.Filled.Backpack,
    "Watch" to Icons.Filled.Watch,
    "Headphones" to Icons.Filled.Headphones,
    "Camera" to Icons.Filled.CameraAlt,
    "Photo" to Icons.Filled.PhotoCamera,
    "Video" to Icons.Filled.Videocam,
    "Mic" to Icons.Filled.Mic,
    "Music Note" to Icons.Filled.MusicNote,
    "Piano" to Icons.Filled.Piano,
    "Palette" to Icons.Filled.Palette,
    "Brush" to Icons.Filled.Brush,
    "Book" to Icons.Filled.Book,
    "Read" to Icons.Filled.MenuBook,
    "Article" to Icons.Filled.Article,
    "Gift" to Icons.Filled.CardGiftcard,
    "Wallet" to Icons.Filled.AccountBalanceWallet,
    "Money" to Icons.Filled.AttachMoney,
    "Card" to Icons.Filled.CreditCard,
    "Cart" to Icons.Filled.ShoppingCart,
    "Shopping Bag" to Icons.Filled.ShoppingBag,
    "Mall" to Icons.Filled.LocalMall,
    "Diamond" to Icons.Filled.Diamond,
    "Puzzle" to Icons.Filled.Extension,
    "Folder" to Icons.Filled.Folder,
    "Poll" to Icons.Filled.Poll,
    "Grid" to Icons.Filled.Widgets,
    // Tech
    "Computer" to Icons.Filled.Computer,
    "Phone" to Icons.Filled.Smartphone,
    "Wifi" to Icons.Filled.Wifi,
    "Bluetooth" to Icons.Filled.Bluetooth,
    "Battery" to Icons.Filled.BatteryFull,
    "Memory" to Icons.Filled.Memory,
    "Storage" to Icons.Filled.Storage,
    "Code" to Icons.Filled.Code,
    "Bug" to Icons.Filled.BugReport,
    "Terminal" to Icons.Filled.Terminal,
    "Router" to Icons.Filled.Router,
    // Symbols
    "Warning" to Icons.Filled.Warning,
    "Error" to Icons.Filled.Error,
    "Check Circle" to Icons.Filled.CheckCircle,
    "Cancel" to Icons.Filled.Cancel,
    "Info" to Icons.Filled.Info,
    "Help" to Icons.Filled.Help,
    "Block" to Icons.Filled.Block,
    "Flag" to Icons.Filled.Flag,
    "Bookmark" to Icons.Filled.Bookmark,
    "Label" to Icons.Filled.Label,
    "Priority" to Icons.Filled.PriorityHigh,
    "New" to Icons.Filled.NewReleases,
    "Hot" to Icons.Filled.Whatshot,
    "Celebration" to Icons.Filled.Celebration,
    "Trophy" to Icons.Filled.EmojiEvents,
    "Medal" to Icons.Filled.MilitaryTech,
    "Premium" to Icons.Filled.WorkspacePremium,
    "Verified" to Icons.Filled.Verified,
    // People
    "Person" to Icons.Filled.Person,
    "Group" to Icons.Filled.Group,
    "Face" to Icons.Filled.Face,
    "Waving" to Icons.Filled.EmojiPeople,
    "Accessibility" to Icons.Filled.AccessibilityNew,
    // Direction / trend
    "North" to Icons.Filled.North,
    "South" to Icons.Filled.South,
    "East" to Icons.Filled.East,
    "West" to Icons.Filled.West,
    "Trending Up" to Icons.Filled.TrendingUp,
    "Trending Down" to Icons.Filled.TrendingDown,
    // Basic
    "Add" to Icons.Filled.Add,
    "Remove" to Icons.Filled.Remove,
    "Close" to Icons.Filled.Close,
    "Check" to Icons.Filled.Check,
    "Search" to Icons.Filled.Search,
    "Notification" to Icons.Filled.Notifications,
    "Alarm" to Icons.Filled.Alarm,
    "Timer" to Icons.Filled.Timer,
    "Schedule" to Icons.Filled.Schedule,
    "Today" to Icons.Filled.Today,
    "Event" to Icons.Filled.Event,
)

// Shared with the AI chat screens (Corneal/ARC/Code AI) - an Icon-pack pick
// arrives as a mediaUrl of "icon:<Name>" rather than a real loadable URL
// (see ALL_ICONS/StickerPickerOverlay), so those screens need this lookup
// too to render the actual glyph instead of trying to load it as an image.
val ICON_BY_NAME: Map<String, ImageVector> = ALL_ICONS.toMap()

@Composable
fun MemberChatThreadBody(
    friendId: String,
    friendName: String,
    onBack: () -> Unit,
    highlightId: String? = null,
    onOpenProfile: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    // "Jump to source" from the pinned-messages screen - scrolls straight to
    // the pinned message once history has loaded (no flash-tint here, unlike
    // Corneal/ARC/Code AI - this screen's bubble rendering has too many
    // media/sticker/poll variants to safely thread a highlight tint through
    // all of them right now; landing on the right spot is still the win).
    var pendingScrollTo by remember { mutableStateOf(highlightId) }
    LaunchedEffect(pendingScrollTo, messages.size) {
        val id = pendingScrollTo ?: return@LaunchedEffect
        val index = messages.indexOfFirst { it.id == id }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            pendingScrollTo = null
        }
    }
    // Telegram-style: land on the FIRST unread message, not the newest one -
    // with 20 unread, you shouldn't be dropped past 17 of them. Only ever
    // happens once, the very first time this thread's messages load - later
    // poll refreshes that add a genuinely new message still auto-scroll to
    // the bottom (see the LaunchedEffect(messages.size) further down),
    // that part is unchanged.
    var hasScrolledInitially by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val myUserId = viewModel.storage.userId
    // "Composing…" under the friend's name - see TypingService server-side.
    // Only ever meaningful if BOTH sides have it on, same intuition as most
    // chat apps' typing indicators (a courtesy exchange, not one-sided).
    var friendIsTyping by remember { mutableStateOf(false) }
    var lastTypingPingAt by remember { mutableStateOf(0L) }

    // Only ever runs when Settings > Privacy > "Bot View" is on - see
    // CornealBubbleState's own doc comment. Off (default): this whole block
    // never touches CornealBubbleState.currentChatContext at all, so there's
    // no code path where this conversation's content leaves this screen.
    LaunchedEffect(messages, friendName) {
        if (viewModel.storage.botViewEnabled) {
            com.xhacker.cedal.ui.CornealBubbleState.currentChatContext = com.xhacker.cedal.ui.ChatContext(
                friendId = friendId,
                friendName = friendName,
                recentMessages = messages.filterNot { it.deleted }.takeLast(10).map { if (it.senderId == myUserId) "You: ${it.text}" else "${friendName}: ${it.text}" },
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { com.xhacker.cedal.ui.CornealBubbleState.currentChatContext = null }
    }

    // "Where you left off" - remembers the message nearest the top of the
    // viewport whenever this thread is left (back-press or backgrounding,
    // same ON_STOP+onDispose pairing as the view-once purge effect right
    // below, since a single-Activity app doesn't dispose this composable
    // just from being backgrounded) so reopening the thread later resumes
    // there instead of always jumping to the newest message - see
    // SecureStorage.getChatScrollAnchor/setChatScrollAnchor and the
    // restore logic in the initial-scroll effect further down.
    fun saveScrollAnchor() {
        val id = messages.getOrNull(listState.firstVisibleItemIndex)?.id
        if (id != null) viewModel.storage.setChatScrollAnchor(friendId, id)
    }
    DisposableEffect(friendId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) saveScrollAnchor()
        }
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose {
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
            saveScrollAnchor()
        }
    }

    // "It would be like it never existed" - a consumed view-once message
    // isn't purged the instant it's exhausted, only once the user actually
    // leaves it behind: either by navigating out of this thread (onDispose,
    // e.g. back-press) or by backgrounding the app entirely while still on
    // this screen (ON_STOP via ProcessLifecycleOwner - same pattern as
    // AppLockState's lock-on-background in NavGraph.kt; onDispose alone
    // wouldn't fire for backgrounding, since this whole app is one
    // Activity/NavHost and backgrounding doesn't dispose the composable).
    // See ChatService.purgeConsumedViewOnce.
    DisposableEffect(friendId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.purgeConsumedViewOnceFireAndForget(friendId)
            }
        }
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose {
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
            viewModel.purgeConsumedViewOnceFireAndForget(friendId)
        }
    }

    // "No screenshot or screen record" while view-once is in play - Android
    // has no way to secure just one composable, FLAG_SECURE is whole-window
    // only, so this covers the entire thread screen for as long as it has
    // ANY view-once message in it (locked or revealed) - a screenshot/
    // screen-recording attempt just gets a black rectangle there instead
    // (and the app shows as a blank thumbnail in the recent-apps switcher).
    // Cleared on leaving so the rest of the app isn't affected.
    val hasViewOnceContent = messages.any { it.viewOnce }
    val secureFlagContext = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(hasViewOnceContent) {
        val activity = secureFlagContext as? android.app.Activity
        if (hasViewOnceContent) {
            activity?.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Which message (if any) the reply-preview/reaction-and-actions overlay
    // is currently for - only one of these is ever non-null at a time.
    var replyTarget by remember { mutableStateOf<ChatMessageDto?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessageDto?>(null) }
    var actionsForMessage by remember { mutableStateOf<ChatMessageDto?>(null) }
    // Forward/Pin/Report - see ChatActionComponents.kt, shared with the AI
    // chats. forwardingMessage holds whatever's being forwarded while the
    // "forward to..." sheet is open.
    var forwardingMessage by remember { mutableStateOf<ChatMessageDto?>(null) }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var stickerPickerOpen by remember { mutableStateOf(false) }
    var stickerPickerInitialTab by remember { mutableStateOf(StickerPanelTab.EMOJI) }
    // Tapping the "›" glyph opens this - a flat Telegram-style row (Camera/
    // Emoji/Folder). Gallery lives inside Camera's own sub-choice below;
    // Sticker lives inside Emoji's own sub-choice.
    var attachSheetOpen by remember { mutableStateOf(false) }
    // Camera's own Photo/Video/Gallery sub-choice - see CameraChoiceOverlay.
    var cameraChoiceOpen by remember { mutableStateOf(false) }
    // Emoji's own Emoji/Sticker/Icon sub-choice - see EmojiChoiceOverlay.
    var emojiChoiceOpen by remember { mutableStateOf(false) }
    var attachMenuOpen by remember { mutableStateOf(false) }
    var pollComposerOpen by remember { mutableStateOf(false) }
    // The "✕" that replaces Send while the composer is empty opens this -
    // Bubble (circular video note, like WhatsApp)/Voice (voice note)/
    // Square (silent square video note). See RecordOptionsOverlay.
    var recordSheetOpen by remember { mutableStateOf(false) }
    // Voice specifically shows VoiceRecorderPanel (record -> review, not an
    // immediate send) instead of the normal composer while true. A finished
    // recording is held in pendingVoiceFile/pendingVoiceDurationMs rather
    // than sent right away, so you can keep typing and send it together
    // with whatever text you add - see send() below.
    var voiceRecorderActive by remember { mutableStateOf(false) }
    var pendingVoiceFile by remember { mutableStateOf<java.io.File?>(null) }
    var pendingVoiceDurationMs by remember { mutableStateOf(0L) }
    // null = a normal video attach; "vid_bubble"/"vid_square" tags the
    // NEXT camera-video capture as a note instead - read by
    // cameraVideoLauncher's callback below, set right before launching.
    var pendingVideoNoteType by remember { mutableStateOf<String?>(null) }
    var headerMenuOpen by remember { mutableStateOf(false) }
    var deleteChatConfirmOpen by remember { mutableStateOf(false) }
    // New ⋮ menu items: Pin, Search, Export Chat, Report, Block, Shortcut.
    var isPinned by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var reportConfirmOpen by remember { mutableStateOf(false) }
    var blockConfirmOpen by remember { mutableStateOf(false) }
    // Cached friend info (name/avatar) can outlive the actual account - see
    // FriendService.friendStatus. Checked once per thread open rather than
    // only discovered from a failed send.
    var friendDeleted by remember { mutableStateOf(false) }
    // "Cedal Team" (the system account that delivers developer keys) - its
    // thread is read-only from this side, replaced by two self-service
    // actions instead of a normal composer. See FriendStatusResult.
    // isCedalTeam and DeveloperAccessService.generateKeyForSelf/revokeSelf.
    var isCedalTeam by remember { mutableStateOf(false) }
    var hasDeveloperAccess by remember { mutableStateOf(false) }
    var devActionBusy by remember { mutableStateOf(false) }
    var devActionError by remember { mutableStateOf<String?>(null) }
    var revokeConfirmOpen by remember { mutableStateOf(false) }
    LaunchedEffect(friendId) {
        viewModel.getPinnedState(friendId).onSuccess { isPinned = it }
        viewModel.isBlocked(friendId).onSuccess { isBlocked = it }
        viewModel.getFriendStatus(friendId).onSuccess { friendDeleted = !it.exists; isCedalTeam = it.isCedalTeam }
    }
    LaunchedEffect(isCedalTeam) {
        if (isCedalTeam) {
            viewModel.getProfile().onSuccess { hasDeveloperAccess = it.developerAccess }
        }
    }
    // "View Once" (header ⋮ menu) - applies to the NEXT message sent,
    // whatever it is (text or an attachment); resets after each send, same
    // as WhatsApp's per-message toggle rather than a sticky mode. null =
    // off; "once" = classic single-reveal; "custom_time"/"custom_count" -
    // see ViewOnceModeOverlay/ChatService.revealMessage's doc comments.
    var viewOnceMode by remember { mutableStateOf<String?>(null) }
    var viewOnceDurationMs by remember { mutableStateOf<Long?>(null) }
    var viewOnceMaxViews by remember { mutableStateOf<Int?>(null) }
    var viewOnceOverlayOpen by remember { mutableStateOf(false) }
    var popularityOverlayOpen by remember { mutableStateOf(false) }
    var myStickers by remember { mutableStateOf<List<StickerDto>>(emptyList()) }
    var uploadingSticker by remember { mutableStateOf(false) }
    var uploadingAttachment by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // "Export Chat" (header ⋮ menu) - a plain-text transcript, shared via
    // Android's normal share sheet rather than silently saved somewhere the
    // user would have to go hunting for.
    fun exportChat() {
        val transcript = messages
            .filterNot { it.deleted }
            .joinToString("\n") { msg ->
                val who = if (msg.senderId == myUserId) "You" else friendName
                val body = when {
                    msg.mediaType != null -> "[${msg.mediaType}]" + if (msg.text.isNotBlank()) " ${msg.text}" else ""
                    else -> msg.text
                }
                "[${formatMessageTime(msg.sentAt)}] $who: $body"
            }
        val dir = java.io.File(context.cacheDir, "chat_exports").apply { mkdirs() }
        val file = java.io.File(dir, "chat_with_${friendName.replace(Regex("[^A-Za-z0-9]"), "_")}.txt")
        file.writeText(transcript)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Export chat"))
    }

    // "Shortcut" (header ⋮ menu) - pins a home-screen shortcut for this
    // chat, same idea as WhatsApp's per-chat shortcut. Gracefully a no-op
    // on launchers that don't support pinning - ShortcutManagerCompat
    // already handles that fallback internally. Known gap: MainActivity
    // doesn't parse these intent extras yet, so tapping the shortcut opens
    // the app at its normal entry point rather than jumping straight into
    // this thread - the shortcut itself (icon + label + pin) is real, deep-
    // linking into the exact chat is follow-up work.
    fun addShortcut() {
        val shortcutIntent = android.content.Intent(context, com.xhacker.cedal.MainActivity::class.java).apply {
            action = android.content.Intent.ACTION_VIEW
            putExtra("open_chat_friend_id", friendId)
            putExtra("open_chat_friend_name", friendName)
        }
        val shortcut = androidx.core.content.pm.ShortcutInfoCompat.Builder(context, "chat_$friendId")
            .setShortLabel(friendName)
            .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(context, android.R.drawable.sym_action_chat))
            .setIntent(shortcutIntent)
            .build()
        androidx.core.content.pm.ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    fun sendMedia(url: String, mediaType: String, fileName: String?) {
        val mode = viewOnceMode
        val durationMs = viewOnceDurationMs
        val maxViews = viewOnceMaxViews
        viewOnceMode = null; viewOnceDurationMs = null; viewOnceMaxViews = null
        sending = true
        error = null
        scope.launch {
            viewModel.sendMessage(
                friendId, "", mediaUrl = url, mediaType = mediaType, fileName = fileName,
                viewOnce = mode != null, viewOnceMode = mode, viewOnceDurationMs = durationMs, viewOnceMaxViews = maxViews,
            )
                .onSuccess { msg -> messages = messages + msg }
                .onFailure { error = it.message }
            sending = false
        }
    }

    fun uploadAndSend(uri: android.net.Uri, kind: String, mediaType: String, fallbackFileName: String? = null) {
        uploadingAttachment = true
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = fallbackFileName ?: queryDisplayName(context, uri)
            if (bytes != null) {
                viewModel.uploadImage(kind, bytes, mimeType)
                    .onSuccess { url -> sendMedia(url, mediaType, fileName) }
                    .onFailure { error = it.message }
            }
            uploadingAttachment = false
        }
    }

    val stickerImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadingSticker = true
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) {
                viewModel.uploadImage("sticker", bytes, mimeType)
                    .onSuccess { url -> viewModel.createSticker(url).onSuccess { sticker -> myStickers = myStickers + sticker } }
                    .onFailure { error = it.message }
            }
            uploadingSticker = false
        }
    }

    // Gallery - multi-select images/videos already on the phone (the same
    // "your camera roll" idea as WhatsApp/Telegram's attachment gallery).
    val galleryPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        attachMenuOpen = false
        uris.forEach { uri ->
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            uploadAndSend(uri, "chat_media", if (mimeType.startsWith("video/")) "video" else "image")
        }
    }

    // Camera - captures straight to a FileProvider URI (see file_paths.xml's
    // chat_camera cache-path), then uploads/sends exactly like a gallery pick.
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraPhotoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
    ) { success ->
        attachMenuOpen = false
        val uri = pendingCameraUri
        if (success && uri != null) uploadAndSend(uri, "chat_media", "image")
    }
    val cameraVideoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CaptureVideo(),
    ) { success ->
        attachMenuOpen = false
        val uri = pendingCameraUri
        val noteType = pendingVideoNoteType
        pendingVideoNoteType = null
        if (success && uri != null) uploadAndSend(uri, "chat_media", noteType ?: "video")
    }

    fun newCameraUri(extension: String): android.net.Uri {
        val dir = java.io.File(context.cacheDir, "chat_camera").apply { mkdirs() }
        val file = java.io.File(dir, "capture_${System.currentTimeMillis()}.$extension")
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // Files - Android's own system document picker (Storage Access
    // Framework), not a custom-built file browser - modern Android blocks
    // apps from freely browsing arbitrary folders, this is the real
    // equivalent: the user picks from anywhere they already have access
    // (Downloads, Drive, Files, etc.), any file type ("*/*").
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        attachMenuOpen = false
        if (uri != null) uploadAndSend(uri, "chat_file", "file")
    }

    // Voice notes - see the "Voice" option on RecordOptionsOverlay, which
    // shows VoiceRecorderPanel (recording lives entirely inside that shared
    // composable) once mic permission is confirmed here.
    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) voiceRecorderActive = true }

    // CAMERA is declared in the manifest but is a dangerous runtime
    // permission - without actually requesting it first, TakePicture()'s
    // underlying ACTION_IMAGE_CAPTURE intent crashes instead of just
    // failing gracefully on some OEM camera apps (e.g. Samsung's).
    fun launchCameraPhoto() {
        val uri = newCameraUri("jpg")
        pendingCameraUri = uri
        cameraPhotoLauncher.launch(uri)
    }
    fun launchCameraVideo() {
        val uri = newCameraUri("mp4")
        pendingCameraUri = uri
        cameraVideoLauncher.launch(uri)
    }
    // One shared permission prompt for both - remembers which capture mode
    // was actually requested so it can resume the right one after grant.
    var pendingCameraAction by remember { mutableStateOf<String?>(null) }
    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            when (pendingCameraAction) {
                "video" -> launchCameraVideo()
                else -> launchCameraPhoto()
            }
        }
    }
    fun withCameraPermission(action: String, launch: () -> Unit) {
        pendingCameraAction = action
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            launch()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    fun openCamera() = withCameraPermission("photo") { launchCameraPhoto() }
    fun openCameraVideo() = withCameraPermission("video") { launchCameraVideo() }

    LaunchedEffect(friendId) {
        while (true) {
            viewModel.getMessages(friendId).onSuccess { fetched ->
                // Keep a message this session already revealed (view-once)
                // visible instead of letting the next poll re-strip it back
                // to locked - see revealMessage's call site below.
                messages = fetched.map { incoming ->
                    val existing = messages.firstOrNull { it.id == incoming.id }
                    if (incoming.viewOnce && incoming.viewed && existing != null &&
                        (existing.text.isNotEmpty() || existing.mediaUrl != null) && !existing.deleted
                    ) {
                        existing
                    } else {
                        incoming
                    }
                }
            }.onFailure { error = it.message }
            if (viewModel.storage.typingIndicatorsEnabled) {
                viewModel.getTypingFriendIds().onSuccess { typingIds -> friendIsTyping = friendId in typingIds }
            } else {
                friendIsTyping = false
            }
            delay(3000)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.listMyStickers().onSuccess { myStickers = it }
    }

    LaunchedEffect(messages.size) {
        if (messages.isEmpty() || pendingScrollTo != null) return@LaunchedEffect
        if (!hasScrolledInitially) {
            hasScrolledInitially = true
            // "Where you left off" takes priority over first-unread - see
            // saveScrollAnchor above. Falls back to the pre-existing
            // first-unread-or-bottom behavior the first time this thread is
            // ever opened (no saved anchor yet) or if the saved message has
            // since been deleted/isn't in the loaded window anymore.
            val savedIndex = viewModel.storage.getChatScrollAnchor(friendId)?.let { id -> messages.indexOfFirst { it.id == id } } ?: -1
            if (savedIndex >= 0) {
                listState.scrollToItem(savedIndex)
            } else {
                val firstUnreadIndex = messages.indexOfFirst { it.senderId != myUserId && !it.read }
                listState.scrollToItem(if (firstUnreadIndex >= 0) firstUnreadIndex else messages.size - 1)
            }
        } else {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Marks messages read as they actually scroll into view, instead of the
    // old "opening the thread reads everything" behavior - see
    // ChatService.markRead. Scans every currently-visible item for the
    // bottom-most unread incoming one (not just the trailing list item) -
    // the plain "last visible item" used to skip marking anything read
    // whenever YOUR OWN reply was the newest message in the thread, since
    // that reply (not the friend's still-visible-but-earlier message) was
    // the last item, and it's never eligible (senderId == myUserId). Still
    // throttled the same way (snapshotFlow only re-emits on a real value
    // change, not every frame) since this reduces back to a single nullable
    // index either way.
    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull { info -> messages.getOrNull(info.index)?.let { it.senderId != myUserId && !it.read } == true }
                ?.index
        }
            .collect { targetIndex ->
                if (targetIndex == null) return@collect
                val msg = messages.getOrNull(targetIndex) ?: return@collect
                viewModel.markRead(friendId, msg.id)
            }
    }

    fun send() {
        val text = input.trim()
        val voiceFile = pendingVoiceFile
        if ((text.isBlank() && voiceFile == null) || sending) return
        val editing = editingMessage
        val replyingTo = replyTarget?.id
        val mode = if (editing == null) viewOnceMode else null
        val durationMs = if (editing == null) viewOnceDurationMs else null
        val maxViews = if (editing == null) viewOnceMaxViews else null
        input = ""
        replyTarget = null
        editingMessage = null
        viewOnceMode = null; viewOnceDurationMs = null; viewOnceMaxViews = null
        pendingVoiceFile = null
        pendingVoiceDurationMs = 0L
        sending = true
        error = null
        scope.launch {
            val result = if (editing != null) {
                viewModel.editMessage(friendId, editing.id, text).onSuccess { updated -> messages = messages.map { if (it.id == updated.id) updated else it } }
            } else if (voiceFile != null) {
                val bytes = voiceFile.readBytes()
                viewModel.uploadImage("chat_media", bytes, "audio/mp4a-latm")
                    .mapCatching { url ->
                        viewModel.sendMessage(
                            friendId, text, replyingTo, mediaUrl = url, mediaType = "audio", fileName = voiceFile.name,
                            viewOnce = mode != null, viewOnceMode = mode, viewOnceDurationMs = durationMs, viewOnceMaxViews = maxViews,
                        ).getOrThrow()
                    }
                    .onSuccess { msg -> messages = messages + msg }
            } else {
                viewModel.sendMessage(
                    friendId, text, replyingTo,
                    viewOnce = mode != null, viewOnceMode = mode, viewOnceDurationMs = durationMs, viewOnceMaxViews = maxViews,
                ).onSuccess { msg -> messages = messages + msg }
            }
            result.onFailure { error = it.message }
            sending = false
        }
    }

    fun revealAndShow(message: ChatMessageDto) {
        scope.launch {
            viewModel.revealMessage(friendId, message.id)
                .onSuccess { revealed -> messages = messages.map { if (it.id == revealed.id) revealed else it } }
                // Custom view-once messages re-check their limit on every
                // tap (see ChatService.revealMessage) - once exhausted/
                // expired this surfaces as a normal error, not a crash.
                .onFailure { actionNotice = it.message ?: "This message can no longer be viewed" }
        }
    }

    // Stickers send instantly on tap (no typing/confirm step), same as
    // WhatsApp/Telegram - never goes through the reply/edit path.
    fun sendSticker(emoji: String) {
        stickerPickerOpen = false
        if (sending) return
        sending = true
        error = null
        scope.launch {
            viewModel.sendMessage(friendId, emoji, isSticker = true)
                .onSuccess { msg -> messages = messages + msg }
                .onFailure { error = it.message }
            sending = false
        }
    }

    fun sendPoll(question: String, options: List<String>) {
        if (sending) return
        sending = true
        error = null
        scope.launch {
            viewModel.sendMessage(friendId, "", pollQuestion = question, pollOptions = options)
                .onSuccess { msg -> messages = messages + msg; pollComposerOpen = false }
                .onFailure { error = it.message }
            sending = false
        }
    }

    fun voteInPoll(message: ChatMessageDto, optionIndex: Int) {
        scope.launch {
            viewModel.voteInPoll(friendId, message.id, optionIndex).onSuccess { votes ->
                messages = messages.map { if (it.id == message.id) it.copy(pollVotes = votes) else it }
            }
        }
    }

    // imePadding(): MainActivity uses enableEdgeToEdge(), so without this the
    // keyboard just draws on top of the input bar (and pushes the header off
    // the top of the visible area) instead of shrinking the column above it.
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).imePadding()) {
        ChatHeader(
            friendName = friendName,
            isTyping = friendIsTyping,
            onBack = onBack,
            onOpenProfile = { onOpenProfile(friendId) },
            onOpenMenu = { headerMenuOpen = true },
        )

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(messages, key = { _, msg -> msg.id }) { _, msg ->
                    val isMine = msg.senderId == myUserId
                    val replyTo = msg.replyToId?.let { rid -> messages.firstOrNull { it.id == rid } }
                    MessageBubble(
                        message = msg,
                        isMine = isMine,
                        replyTo = replyTo,
                        myUserId = myUserId,
                        viewModel = viewModel,
                        onLongPress = { actionsForMessage = msg },
                        onRevealViewOnce = { revealAndShow(msg) },
                        onVote = { optionIndex -> voteInPoll(msg, optionIndex) },
                    )
                }
                if (messages.isEmpty()) {
                    item {
                        Text(
                            "No messages yet - say hi 👋",
                            color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
            }

            // Telegram-style "back to the bottom" - only shows once you've
            // scrolled meaningfully far from the newest message, so it's
            // not just permanently sitting there for a short conversation
            // that already fits on screen.
            val showJumpToBottom = messages.isNotEmpty() &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: messages.size - 1) < messages.size - 5
            if (showJumpToBottom) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderCyan, CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            scope.launch { listState.animateScrollToItem(messages.size - 1) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↓", color = CedalColors.AccentCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        error?.let { Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }

        (editingMessage ?: replyTarget)?.let { target ->
            ComposerContextBanner(
                label = if (editingMessage != null) "Editing message" else "Replying to ${if (target.senderId == myUserId) "yourself" else friendName}",
                snippet = target.text,
                onCancel = { editingMessage = null; replyTarget = null; input = "" },
            )
        }

        if (isCedalTeam) {
            CedalTeamActionPanel(
                hasDeveloperAccess = hasDeveloperAccess,
                busy = devActionBusy,
                error = devActionError,
                onGenerateKey = {
                    devActionBusy = true
                    devActionError = null
                    scope.launch {
                        viewModel.generateOwnDeveloperKey()
                            .onFailure { devActionError = it.message ?: "Couldn't generate a key" }
                        devActionBusy = false
                    }
                },
                onRequestRevoke = { revokeConfirmOpen = true },
            )
        } else if (friendDeleted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("This account has been deleted.", color = CedalColors.TextMuted, fontSize = 12.sp)
            }
        } else if (voiceRecorderActive) {
            VoiceRecorderPanel(
                onReady = { file, durationMs ->
                    voiceRecorderActive = false
                    pendingVoiceFile = file
                    pendingVoiceDurationMs = durationMs
                },
                onCancel = { voiceRecorderActive = false },
            )
        } else {
            pendingVoiceFile?.let { file ->
                PendingVoiceChip(pendingVoiceDurationMs, onRemove = { pendingVoiceFile = null; pendingVoiceDurationMs = 0L })
            }
            ChatInputBar(
                input = input,
                onInputChange = { value ->
                    input = value
                    // Throttled - a ping every couple seconds while actively
                    // typing is plenty (TypingService's 6s freshness window
                    // easily covers the gap), no need to fire on every
                    // keystroke.
                    if (viewModel.storage.typingIndicatorsEnabled && value.isNotBlank()) {
                        val now = System.currentTimeMillis()
                        if (now - lastTypingPingAt > 2000) {
                            lastTypingPingAt = now
                            scope.launch { viewModel.pingTyping(friendId) }
                        }
                    }
                },
                sending = sending,
                viewOnceMode = viewOnceMode != null,
                onSend = { send() },
                onOpenAttachSheet = { attachSheetOpen = true },
                onOpenRecordSheet = { recordSheetOpen = true },
                hasPendingAttachment = pendingVoiceFile != null,
            )
        }
    }

    if (recordSheetOpen) {
        RecordOptionsOverlay(
            onBubble = {
                recordSheetOpen = false
                pendingVideoNoteType = "vid_bubble"
                withCameraPermission("video") { launchCameraVideo() }
            },
            onVoice = {
                recordSheetOpen = false
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    voiceRecorderActive = true
                } else {
                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            },
            onSquare = {
                recordSheetOpen = false
                pendingVideoNoteType = "vid_square"
                withCameraPermission("video") { launchCameraVideo() }
            },
            onDismiss = { recordSheetOpen = false },
        )
    }

    if (attachSheetOpen) {
        AttachmentSheetOverlay(
            // Camera doesn't jump straight into taking a photo anymore - a
            // real camera app also lets you switch to video or bail out to
            // your gallery without leaving the capture flow, so this opens
            // that same choice instead of assuming "photo".
            onCamera = { attachSheetOpen = false; cameraChoiceOpen = true },
            // Emoji doesn't jump straight into the Emoji tab anymore either -
            // mirrors Camera's own sub-choice (see EmojiChoiceOverlay).
            onEmoji = { attachSheetOpen = false; emojiChoiceOpen = true },
            onFolder = {
                attachSheetOpen = false
                filePicker.launch(arrayOf("*/*"))
            },
            onDismiss = { attachSheetOpen = false },
        )
    }

    if (cameraChoiceOpen) {
        CameraChoiceOverlay(
            onPhoto = { cameraChoiceOpen = false; openCamera() },
            onVideo = { cameraChoiceOpen = false; openCameraVideo() },
            onGallery = {
                cameraChoiceOpen = false
                galleryPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            onDismiss = { cameraChoiceOpen = false },
        )
    }

    if (emojiChoiceOpen) {
        EmojiChoiceOverlay(
            onEmoji = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.EMOJI; stickerPickerOpen = true },
            onSticker = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.STICKERS; stickerPickerOpen = true },
            onIcon = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.ICON; stickerPickerOpen = true },
            onDismiss = { emojiChoiceOpen = false },
        )
    }

    if (stickerPickerOpen) {
        StickerPickerOverlay(
            myStickers = myStickers,
            uploading = uploadingSticker,
            initialTab = stickerPickerInitialTab,
            onPickEmoji = { emoji -> input += emoji },
            onPickSticker = ::sendSticker,
            onUploadSticker = { stickerImagePicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onDismiss = { stickerPickerOpen = false },
        )
    }

    if (attachMenuOpen) {
        AttachmentMenuOverlay(
            uploading = uploadingAttachment,
            onCameraPhoto = {
                val uri = newCameraUri("jpg")
                pendingCameraUri = uri
                cameraPhotoLauncher.launch(uri)
            },
            onCameraVideo = {
                val uri = newCameraUri("mp4")
                pendingCameraUri = uri
                cameraVideoLauncher.launch(uri)
            },
            onFiles = { filePicker.launch(arrayOf("*/*")) },
            onPoll = { attachMenuOpen = false; pollComposerOpen = true },
            onDismiss = { attachMenuOpen = false },
        )
    }

    if (pollComposerOpen) {
        PollComposerOverlay(
            sending = sending,
            onCreate = { question, options -> sendPoll(question, options) },
            onDismiss = { pollComposerOpen = false },
        )
    }

    if (headerMenuOpen) {
        ChatHeaderMenuOverlay(
            viewOnceMode = viewOnceMode,
            isPinned = isPinned,
            isBlocked = isBlocked,
            onToggleViewOnce = { headerMenuOpen = false; viewOnceOverlayOpen = true },
            onTogglePin = {
                headerMenuOpen = false
                val next = !isPinned
                isPinned = next
                scope.launch { viewModel.bulkChatAction(listOf(friendId), if (next) "pin" else "unpin") }
            },
            onSearch = { headerMenuOpen = false; searchOpen = true },
            onExport = { headerMenuOpen = false; exportChat() },
            onReport = { headerMenuOpen = false; reportConfirmOpen = true },
            onToggleBlock = { headerMenuOpen = false; blockConfirmOpen = true },
            onShortcut = { headerMenuOpen = false; addShortcut() },
            onPopularity = { headerMenuOpen = false; popularityOverlayOpen = true },
            onDeleteChat = { headerMenuOpen = false; deleteChatConfirmOpen = true },
            onDismiss = { headerMenuOpen = false },
        )
    }

    if (popularityOverlayOpen) {
        ChatPopularityOverlay(
            friendId = friendId,
            viewModel = viewModel,
            onDismiss = { popularityOverlayOpen = false },
        )
    }

    if (viewOnceOverlayOpen) {
        ViewOnceModeOverlay(
            onPickCustomTime = { durationMs ->
                viewOnceMode = "custom_time"; viewOnceDurationMs = durationMs; viewOnceMaxViews = null
                viewOnceOverlayOpen = false
            },
            onPickCustomCount = { maxViews ->
                viewOnceMode = "custom_count"; viewOnceMaxViews = maxViews; viewOnceDurationMs = null
                viewOnceOverlayOpen = false
            },
            onTurnOff = {
                viewOnceMode = null; viewOnceDurationMs = null; viewOnceMaxViews = null
                viewOnceOverlayOpen = false
            },
            onDismiss = { viewOnceOverlayOpen = false },
        )
    }

    if (searchOpen) {
        ChatSearchOverlay(
            messages = messages.filterNot { it.deleted },
            onResultTap = { msg -> searchOpen = false; pendingScrollTo = msg.id },
            onDismiss = { searchOpen = false },
        )
    }

    if (reportConfirmOpen) {
        ReportOverlay(
            friendName = friendName,
            viewModel = viewModel,
            onSubmit = { reason, mediaUrl, mediaType, fileName ->
                reportConfirmOpen = false
                scope.launch {
                    viewModel.reportFriend(friendId, reason, mediaUrl, mediaType, fileName)
                        .onSuccess { actionNotice = "Reported for admin review" }
                }
            },
            onDismiss = { reportConfirmOpen = false },
        )
    }

    if (blockConfirmOpen) {
        SimpleConfirmOverlay(
            title = if (isBlocked) "Unblock $friendName?" else "Block $friendName?",
            body = if (isBlocked) "$friendName will be able to message you again." else "$friendName won't be able to message you, and this chat will disappear from your list.",
            confirmLabel = if (isBlocked) "UNBLOCK" else "BLOCK",
            onConfirm = {
                blockConfirmOpen = false
                val next = !isBlocked
                isBlocked = next
                scope.launch {
                    if (next) viewModel.blockFriend(friendId) else viewModel.unblockFriend(friendId)
                    if (next) onBack()
                }
            },
            onDismiss = { blockConfirmOpen = false },
        )
    }

    if (revokeConfirmOpen) {
        SimpleConfirmOverlay(
            title = "Revoke your developer access?",
            body = "This cannot be undone. You'll lose developer access immediately, and getting it back requires the owner to grant it to you again.",
            confirmLabel = "REVOKE",
            onConfirm = {
                revokeConfirmOpen = false
                devActionBusy = true
                devActionError = null
                scope.launch {
                    viewModel.revokeOwnDeveloperAccess()
                        .onSuccess { hasDeveloperAccess = false }
                        .onFailure { devActionError = it.message ?: "Couldn't revoke access" }
                    devActionBusy = false
                }
            },
            onDismiss = { revokeConfirmOpen = false },
        )
    }

    if (deleteChatConfirmOpen) {
        DeleteChatConfirmOverlay(
            onConfirm = {
                deleteChatConfirmOpen = false
                scope.launch {
                    viewModel.deleteConversation(friendId).onSuccess { onBack() }.onFailure { error = it.message }
                }
            },
            onDismiss = { deleteChatConfirmOpen = false },
        )
    }

    actionsForMessage?.let { msg ->
        ChatBubbleActionsOverlay(
            showReactions = true,
            canEdit = msg.senderId == myUserId && !msg.deleted && System.currentTimeMillis() - msg.sentAt < EDIT_WINDOW_MS,
            canDelete = msg.senderId == myUserId && !msg.deleted,
            onReact = { emoji ->
                actionsForMessage = null
                scope.launch {
                    viewModel.reactToMessage(friendId, msg.id, emoji).onSuccess { reactions ->
                        messages = messages.map { if (it.id == msg.id) it.copy(reactions = reactions) else it }
                    }
                }
            },
            onReply = { replyTarget = msg; editingMessage = null; actionsForMessage = null },
            onCopy = {
                actionsForMessage = null
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(msg.text))
            },
            onForward = { forwardingMessage = msg; actionsForMessage = null },
            onPin = {
                actionsForMessage = null
                scope.launch {
                    viewModel.pinMessage(chatType = "friend", messageId = msg.id, messageText = msg.text, chatKey = friendId).onSuccess { actionNotice = "Pinned" }
                }
            },
            onReport = {
                actionsForMessage = null
                scope.launch {
                    viewModel.reportMessage("friend", msg.id, msg.text).onSuccess { actionNotice = "Reported for admin review" }
                }
            },
            onEdit = { editingMessage = msg; replyTarget = null; input = msg.text; actionsForMessage = null },
            onDelete = {
                actionsForMessage = null
                scope.launch {
                    viewModel.deleteMessage(friendId, msg.id).onSuccess {
                        messages = messages.map { if (it.id == msg.id) it.copy(deleted = true, reactions = emptyMap()) else it }
                    }
                }
            },
            onDismiss = { actionsForMessage = null },
        )
    }

    forwardingMessage?.let { msg ->
        ForwardMessageSheet(
            viewModel = viewModel,
            onDismiss = { forwardingMessage = null },
            onPick = { label, key ->
                forwardingMessage = null
                scope.launch {
                    when {
                        key == "corneal" -> viewModel.cornealChat(msg.text)
                        key == "arc" -> viewModel.arcChat(msg.text)
                        key == "code" -> viewModel.submitAiRequest(msg.text)
                        key.startsWith("friend:") -> viewModel.sendMessage(key.removePrefix("friend:"), msg.text)
                    }
                    actionNotice = "Forwarded to $label"
                }
            },
        )
    }

    actionNotice?.let { notice ->
        LaunchedEffect(notice) { delay(2000); actionNotice = null }
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(notice, color = CedalColors.TextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessageDto,
    isMine: Boolean,
    replyTo: ChatMessageDto?,
    myUserId: String?,
    viewModel: AuthViewModel,
    onLongPress: () -> Unit,
    onRevealViewOnce: () -> Unit,
    onVote: (Int) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 280.dp)) {
            // View-once, still locked - a Telegram-style textured card
            // (sparkle noise + a flame glyph + a "1 play left" badge)
            // instead of a padlock placeholder, same treatment for text and
            // media since neither renders real content pre-reveal (server
            // withholds it - see ChatService.toDto - so there's nothing to
            // show anyway; only the recipient can tap to reveal). Checking
            // for actually-withheld content (rather than just !viewed)
            // matters for the custom modes - toDto() keeps them hidden on
            // every regular fetch even after some reveals, since each view
            // needs its own fresh tap (see ChatService.revealMessage).
            val viewOnceLocked = message.viewOnce && !message.deleted && message.text.isEmpty() && message.mediaUrl == null
            if (viewOnceLocked) {
                ViewOnceLockedCard(
                    isMine = isMine,
                    remainingViews = message.viewOnceMaxViews?.let { it - message.viewOnceViewCount },
                    onClick = { if (!isMine) onRevealViewOnce() },
                    onLongClick = onLongPress,
                )
                MessageFooter(message, onOpenActions = onLongPress)
                return@Column
            }
            // Once revealed, a view-once message just falls through to the
            // normal rendering below (media/sticker/poll/text bubble,
            // whichever applies) - showing what was actually sent instead
            // of a permanent "Opened" placeholder that never displayed it.
            // A media attachment (camera/gallery/files) - image/video get an
            // inline preview, a generic file gets a tappable name+icon card
            // that hands off to whatever app the OS resolves for that type.
            if (message.mediaUrl != null && !message.deleted) {
                MediaAttachment(message.mediaUrl!!, message.mediaType, message.fileName, isMine, onLongPress)
                if (message.text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(if (isMine) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                            .background(if (isMine) CedalColors.AccentCyan else CedalColors.CardBackground)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        ChatMessageContent(if (isMine) message.text else (message.translatedText ?: message.text), if (isMine) CedalColors.Background else CedalColors.TextPrimary, viewModel, fontSize = 13.sp)
                    }
                }
                MessageFooter(message, onOpenActions = onLongPress)
                return@Column
            }
            // A poll - tap an option to vote (or change your vote); bars
            // show live counts once anyone (including you) has voted.
            val pollOptions = message.pollOptions
            if (pollOptions != null && !message.deleted) {
                val myVote = message.pollVotes[myUserId]
                val totalVotes = message.pollVotes.size
                Column(
                    modifier = Modifier
                        .widthIn(min = 220.dp)
                        .clip(if (isMine) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                        .background(if (isMine) CedalColors.AccentCyan else CedalColors.CardBackground)
                        .let { if (isMine) it else it.border(1.dp, CedalColors.BorderSlate, BUBBLE_THEIRS_SHAPE) }
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = onLongPress,
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        "📊 ${message.text.ifBlank { "Poll" }}",
                        color = if (isMine) CedalColors.Background else CedalColors.TextPrimary,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp),
                    )
                    pollOptions.forEachIndexed { index, option ->
                        val count = message.pollVotes.values.count { it == index }
                        val fraction = if (totalVotes > 0) count.toFloat() / totalVotes else 0f
                        val picked = myVote == index
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMine) CedalColors.Background.copy(alpha = 0.15f) else CedalColors.Background)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onVote(index) }
                                .padding(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    if (picked) "🔘" else "⚪",
                                    fontSize = 12.sp,
                                )
                                Text(
                                    option,
                                    color = if (isMine) CedalColors.Background else CedalColors.TextPrimary,
                                    fontSize = 12.sp, modifier = Modifier.weight(1f).padding(start = 6.dp),
                                )
                                Text(
                                    "$count",
                                    color = if (isMine) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted,
                                    fontSize = 11.sp,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isMine) CedalColors.Background.copy(alpha = 0.2f) else CedalColors.BorderSlate),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isMine) CedalColors.Background else CedalColors.Success),
                                )
                            }
                        }
                    }
                    Text(
                        "$totalVotes vote${if (totalVotes == 1) "" else "s"}",
                        color = if (isMine) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted,
                        fontSize = 10.sp,
                    )
                }
                MessageFooter(message, onOpenActions = onLongPress)
                return@Column
            }
            // Stickers render as a plain large emoji - no bubble background/
            // border/tail, matching WhatsApp/Telegram's sticker treatment -
            // still long-pressable for reactions like any other message.
            if (message.isSticker && !message.deleted) {
                val stickerModifier = Modifier
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onLongPress,
                    )
                    .padding(4.dp)
                // Custom (uploaded) stickers are stored as a URL, Icon-pack
                // picks are stored as "icon:<Name>" (see ALL_ICONS/
                // ICON_BY_NAME), everything else is a plain emoji character.
                if (message.text.startsWith("http")) {
                    coil.compose.AsyncImage(
                        model = message.text,
                        contentDescription = "Sticker",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = stickerModifier.size(120.dp),
                    )
                } else if (message.text.startsWith("icon:") && ICON_BY_NAME.containsKey(message.text.removePrefix("icon:"))) {
                    Icon(
                        ICON_BY_NAME.getValue(message.text.removePrefix("icon:")),
                        contentDescription = "Sticker",
                        tint = CedalColors.AccentCyan,
                        modifier = stickerModifier.size(64.dp),
                    )
                } else {
                    Text(message.text, fontSize = 64.sp, modifier = stickerModifier)
                }
                MessageFooter(message, onOpenActions = onLongPress)
                return@Column
            }
            Box(
                modifier = Modifier
                    .clip(if (isMine) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                    .background(if (isMine) CedalColors.AccentCyan else CedalColors.CardBackground)
                    .let { if (isMine) it else it.border(1.dp, CedalColors.BorderSlate, BUBBLE_THEIRS_SHAPE) }
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !message.deleted,
                        onClick = {},
                        onLongClick = onLongPress,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Column {
                    if (replyTo != null) {
                        Row(modifier = Modifier.padding(bottom = 6.dp)) {
                            Box(modifier = Modifier.size(width = 3.dp, height = 28.dp).background(CedalColors.Success))
                            Column(modifier = Modifier.padding(start = 6.dp)) {
                                Text(
                                    if (replyTo.senderId == myUserId) "You" else "Them",
                                    color = if (isMine) CedalColors.Background else CedalColors.Success,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    if (replyTo.deleted) "Message deleted" else replyTo.text,
                                    color = if (isMine) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted,
                                    fontSize = 11.sp, maxLines = 1,
                                )
                            }
                        }
                    }
                    if (message.deleted) {
                        Text(
                            "This message was deleted",
                            color = if (isMine) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted,
                            fontSize = 13.sp, fontStyle = FontStyle.Italic,
                        )
                    } else {
                        // Cedal Team's key-delivery message fences the key
                        // itself as a ```key ... ``` block (see
                        // DeveloperAccessService.sendKeyMessage), so
                        // ChatMessageContent already renders it in its own
                        // bordered box with a Copy button (see
                        // ChatActionComponents.ChatCodeBlock) - no bespoke
                        // handling needed here.
                        ChatMessageContent(if (isMine) message.text else (message.translatedText ?: message.text), if (isMine) CedalColors.Background else CedalColors.TextPrimary, viewModel, fontSize = 14.sp)
                    }
                }
            }
            MessageFooter(message, onOpenActions = onLongPress)
        }
    }
}

// Shared with the AI chat screens (Corneal/ARC/Code AI) - takes plain
// (mediaUrl, mediaType, fileName) rather than a ChatMessageDto since each AI
// lane has its own differently-shaped message DTO; friend chat's call site
// just unpacks its ChatMessageDto's three media fields into this.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaAttachment(mediaUrl: String, mediaType: String?, fileName: String?, isMine: Boolean, onLongPress: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaModifier = Modifier
        .combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                if (mediaType == "file") {
                    // "Allow files to open inside the chat" - firing
                    // ACTION_VIEW straight at the remote https:// URL (the
                    // old behavior) just opens it in a browser tab, which
                    // often can't actually open non-web file types. Instead:
                    // download it into the cache, then hand THAT off with
                    // its real (extension-derived) mime type via
                    // FileProvider, same as a normal downloaded-file open.
                    scope.launch {
                        try {
                            val extension = fileName?.substringAfterLast('.', "")?.lowercase()
                            val mimeType = extension?.let { android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) } ?: "*/*"
                            val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                java.net.URL(mediaUrl).openStream().use { it.readBytes() }
                            }
                            val dir = java.io.File(context.cacheDir, "chat_opens").apply { mkdirs() }
                            val file = java.io.File(dir, fileName ?: "file")
                            file.writeBytes(bytes)
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mimeType)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                context.startActivity(android.content.Intent.createChooser(intent, "Open with"))
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Couldn't open this file", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Hands off to whatever the OS resolves for this URL/type -
                    // the system video player for a video, same as tapping a link.
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mediaUrl))
                    runCatching { context.startActivity(intent) }
                }
            },
            onLongClick = onLongPress,
        )
    when (mediaType) {
        // A real uploaded sticker (as opposed to the fake "icon:Name"
        // pseudo-URL ChatMediaOrIcon already intercepts before this ever
        // runs) - same rendering as a real photo since it IS a real
        // loadable image, just conceptually different (see mediaPlaceholder
        // server-side for how the AI is told the two apart).
        "image", "sticker" -> coil.compose.AsyncImage(
            model = mediaUrl,
            contentDescription = "Image",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = mediaModifier.widthIn(max = 240.dp).clip(RoundedCornerShape(14.dp)),
        )
        "video" -> Box(
            modifier = mediaModifier
                .size(width = 200.dp, height = 140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶", color = CedalColors.AccentCyan, fontSize = 32.sp)
        }
        // Bubble - a circular video note with sound, WhatsApp-style.
        "vid_bubble" -> VideoNoteAttachment(mediaUrl, circular = true, muted = false, onLongPress = onLongPress)
        // Square - same idea, but square and always muted.
        "vid_square" -> VideoNoteAttachment(mediaUrl, circular = false, muted = true, onLongPress = onLongPress)
        "audio" -> AudioAttachment(mediaUrl, isMine, onLongPress)
        else -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = mediaModifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isMine) CedalColors.AccentCyan else CedalColors.CardBackground)
                .let { if (isMine) it else it.border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(12.dp)) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text("📎", fontSize = 18.sp)
            Text(
                fileName ?: "File",
                color = if (isMine) CedalColors.Background else CedalColors.TextPrimary,
                fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// Bubble/Square video notes - real inline playback (unlike plain "video",
// which just links out to the system player), clipped to a circle (Bubble,
// with sound) or a square (Square, always muted) - the shape/mute is a
// PLAYBACK-time choice here, not something baked into the video file
// itself, so this is the only place that distinction actually lives.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoNoteAttachment(mediaUrl: String, circular: Boolean, muted: Boolean, onLongPress: () -> Unit) {
    var isPlaying by remember { mutableStateOf(false) }
    var videoView by remember { mutableStateOf<android.widget.VideoView?>(null) }
    val shape = if (circular) CircleShape else RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(shape)
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, shape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    val view = videoView ?: return@combinedClickable
                    if (isPlaying) {
                        view.pause()
                        isPlaying = false
                    } else {
                        view.start()
                        isPlaying = true
                    }
                },
                onLongClick = onLongPress,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                android.widget.VideoView(context).apply {
                    setVideoURI(android.net.Uri.parse(mediaUrl))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        if (muted) mp.setVolume(0f, 0f)
                    }
                    videoView = this
                }
            },
        )
        if (!isPlaying) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = CedalColors.AccentCyan, modifier = Modifier.size(36.dp))
        }
    }
}

// Decodes the actual audio (MediaExtractor + MediaCodec, not a fake/random
// pattern) into a fixed number of amplitude buckets for the waveform below -
// real per-file data, so louder/quieter parts of the actual recording show
// up as taller/shorter bars. Returns an empty list on any failure (corrupt
// file, unsupported codec, network hiccup mid-decode) - the UI below falls
// back to a flat duration+seek bar with no bars rather than breaking.
private suspend fun extractWaveform(url: String, bucketCount: Int = 40): List<Float> = withContext(Dispatchers.IO) {
    try {
        val extractor = android.media.MediaExtractor()
        extractor.setDataSource(url)
        var trackIndex = -1
        var format: android.media.MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(android.media.MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i
                format = f
                break
            }
        }
        if (trackIndex < 0 || format == null) return@withContext emptyList()
        extractor.selectTrack(trackIndex)
        val durationUs = if (format.containsKey(android.media.MediaFormat.KEY_DURATION)) format.getLong(android.media.MediaFormat.KEY_DURATION) else 1L
        val codec = android.media.MediaCodec.createDecoderByType(format.getString(android.media.MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = android.media.MediaCodec.BufferInfo()
        val sums = DoubleArray(bucketCount)
        val counts = LongArray(bucketCount)
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                if (bufferInfo.size > 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    val shorts = outputBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val bucket = if (durationUs > 0) ((bufferInfo.presentationTimeUs.toDouble() / durationUs) * bucketCount).toInt().coerceIn(0, bucketCount - 1) else 0
                    var sum = 0.0
                    var count = 0
                    while (shorts.hasRemaining()) {
                        sum += kotlin.math.abs(shorts.get().toDouble())
                        count++
                    }
                    if (count > 0) {
                        sums[bucket] += sum
                        counts[bucket] += count
                    }
                }
                codec.releaseOutputBuffer(outputIndex, false)
                if (bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            }
        }
        codec.stop()
        codec.release()
        extractor.release()

        val raw = (0 until bucketCount).map { i -> if (counts[i] > 0) (sums[i] / counts[i]).toFloat() else 0f }
        val max = raw.maxOrNull()?.takeIf { it > 0f } ?: 1f
        raw.map { (it / max).coerceIn(0.06f, 1f) }
    } catch (e: Exception) {
        emptyList()
    }
}

// A voice note - real playback via MediaPlayer, plus a real waveform (see
// extractWaveform above) with a duration display and a draggable seek
// control, instead of the old plain play/pause-only pill. Tap or drag
// anywhere along the waveform to jump to that point, same as any normal
// audio player's scrubber - not just play-from-the-start every time.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioAttachment(url: String, isMine: Boolean, onLongPress: () -> Unit) {
    var player by remember(url) { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember(url) { mutableStateOf(false) }
    var durationMs by remember(url) { mutableStateOf(0) }
    var positionMs by remember(url) { mutableStateOf(0) }
    var waveform by remember(url) { mutableStateOf<List<Float>>(emptyList()) }

    LaunchedEffect(url) { waveform = extractWaveform(url) }

    DisposableEffect(url) {
        onDispose { player?.release() }
    }

    fun ensurePlayer(onReady: (android.media.MediaPlayer) -> Unit) {
        val existing = player
        if (existing != null) {
            onReady(existing)
            return
        }
        runCatching {
            val newPlayer = android.media.MediaPlayer()
            newPlayer.setDataSource(url)
            newPlayer.setOnCompletionListener { isPlaying = false; positionMs = 0 }
            newPlayer.setOnPreparedListener {
                durationMs = it.duration
                player = it
                onReady(it)
            }
            newPlayer.prepareAsync()
        }
    }

    fun togglePlayback() {
        ensurePlayer { p ->
            if (isPlaying) {
                p.pause()
                isPlaying = false
            } else {
                p.start()
                isPlaying = true
            }
        }
    }

    fun seekToFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        ensurePlayer { p ->
            val target = (clamped * p.duration).toInt()
            p.seekTo(target)
            positionMs = target
        }
    }

    // Live-updates the scrubber position while actually playing - without
    // this the seek indicator would just sit frozen at wherever it last
    // was, even though audio is audibly progressing.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            player?.let { positionMs = it.currentPosition }
            delay(150)
        }
    }

    val barColor = if (isMine) CedalColors.Background else CedalColors.AccentCyan
    val trackColor = (if (isMine) CedalColors.Background else CedalColors.AccentCyan).copy(alpha = 0.35f)
    val progressFraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(min = 200.dp, max = 240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isMine) CedalColors.AccentCyan else CedalColors.CardBackground)
            .let { if (isMine) it else it.border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(20.dp)) }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onLongPress,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            if (isPlaying) "⏸" else "▶",
            color = if (isMine) CedalColors.Background else CedalColors.AccentCyan,
            fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { togglePlayback() }
                .padding(end = 4.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(url, durationMs) {
                        detectTapGestures { offset -> if (durationMs > 0) seekToFraction(offset.x / size.width) }
                    }
                    .pointerInput(url, durationMs) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            if (durationMs > 0) seekToFraction(change.position.x / size.width)
                        }
                    },
            ) {
                if (waveform.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(trackColor),
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        waveform.forEachIndexed { index, amplitude ->
                            val played = (index.toFloat() / waveform.size) < progressFraction
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(amplitude.coerceIn(0.12f, 1f))
                                    .clip(RoundedCornerShape(50))
                                    .background(if (played) barColor else trackColor),
                            )
                        }
                    }
                }
            }
            Text(
                "${formatVoiceDuration(positionMs.toLong())} / ${formatVoiceDuration(durationMs.toLong())}",
                color = if (isMine) CedalColors.Background.copy(alpha = 0.85f) else CedalColors.TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// Telegram's view-once card: a rounded card with a scattered sparkle/noise
// texture, a flame glyph centered, and a "1 play left" badge top-start -
// same look for text and media, since neither has real content on this
// client to differentiate on pre-reveal (see ChatService.toDto).
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ViewOnceLockedCard(isMine: Boolean, remainingViews: Int? = null, onClick: () -> Unit, onLongClick: () -> Unit) {
    val sparkles = remember { List(70) { Random.nextFloat() to Random.nextFloat() } }
    Box(
        modifier = Modifier
            .size(width = 150.dp, height = 170.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    if (isMine) listOf(CedalColors.AccentCyan, CedalColors.Background) else listOf(CedalColors.CardBackground, CedalColors.Background),
                ),
            )
            .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            sparkles.forEach { (fx, fy) ->
                drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 1.4.dp.toPx(), center = Offset(fx * size.width, fy * size.height))
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Icon(Icons.Filled.Replay, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Text((remainingViews ?: 1).coerceAtLeast(0).toString(), color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 3.dp))
        }
        Icon(
            Icons.Filled.LocalFireDepartment,
            contentDescription = if (isMine) "You sent a view once" else "Tap to view once",
            tint = Color.White,
            modifier = Modifier.align(Alignment.Center).size(36.dp),
        )
    }
}

@Composable
private fun MessageFooter(message: ChatMessageDto, onOpenActions: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Text(formatMessageTime(message.sentAt), color = CedalColors.TextMuted, fontSize = 9.sp)
        if (message.editedAt != null && !message.deleted) {
            Text(" · edited", color = CedalColors.TextMuted, fontSize = 9.sp)
        }
        if (!message.deleted) {
            Text(
                "⋮", color = CedalColors.TextMuted, fontSize = 12.sp,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenActions),
            )
        }
    }
    if (message.reactions.isNotEmpty()) {
        val counts = message.reactions.values.groupingBy { it }.eachCount()
        Row(modifier = Modifier.padding(top = 3.dp)) {
            counts.forEach { (emoji, count) ->
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("$emoji${if (count > 1) " $count" else ""}", fontSize = 11.sp, color = CedalColors.TextPrimary)
                }
            }
        }
    }
}

// Shown above the composer once a voice note has been recorded+reviewed
// (VoiceRecorderPanel's checkmark) but not sent yet - lets the user see
// what's about to go out and keep typing before actually sending it,
// instead of the old immediate-send-on-stop behavior.
@Composable
internal fun PendingVoiceChip(durationMs: Long, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.AccentCyan, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Filled.Mic, contentDescription = "Voice note", tint = CedalColors.AccentCyan, modifier = Modifier.size(18.dp))
        Text(
            "Voice note (${formatVoiceDuration(durationMs)}) ready to send",
            color = CedalColors.TextSecondary, fontSize = 12.sp,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        Text(
            "✕",
            color = CedalColors.TextMuted, fontSize = 14.sp,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRemove)
                .padding(4.dp),
        )
    }
}

private fun formatVoiceDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
internal fun ComposerContextBanner(label: String, snippet: String, onCancel: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(modifier = Modifier.size(width = 3.dp, height = 24.dp).background(CedalColors.Success))
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(label, color = CedalColors.Success, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(snippet, color = CedalColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
        Text(
            "✕",
            color = CedalColors.TextMuted, fontSize = 14.sp,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel)
                .padding(4.dp),
        )
    }
}

// Corneal's input row (CornealChatScreen.kt) as the shared baseline, plus
// one addition: the leading "›" glyph is clickable here, opening a
// Telegram-style attachment sheet (Camera/Gallery/Emoji/Sticker/Folder) -
// see AttachmentSheetOverlay.
@Composable
private fun ChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    sending: Boolean,
    viewOnceMode: Boolean,
    onSend: () -> Unit,
    onOpenAttachSheet: () -> Unit,
    onOpenRecordSheet: () -> Unit,
    hasPendingAttachment: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Text(
            "›",
            color = CedalColors.AccentCyan, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenAttachSheet)
                .padding(end = 8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (input.isEmpty()) {
                    Text(
                        if (viewOnceMode) "View once - type a message…" else "Type a neural transmission…",
                        color = CedalColors.TextMuted, fontSize = 15.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 15.sp),
                    cursorBrush = SolidColor(CedalColors.AccentCyan),
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Green while composing normally; red instead whenever View
            // Once (header ⋮ menu) is armed for this next message.
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            input.isBlank() -> CedalColors.TextMuted
                            viewOnceMode -> CedalColors.Error
                            else -> Color(0xFF00FF41)
                        },
                    ),
            )
        }
        val canSend = (input.isNotBlank() || hasPendingAttachment) && !sending
        // Empty input (and no pending voice note) swaps Send for a "✕" that
        // opens the Bubble/Voice/Square record sheet, instead of just
        // sitting there disabled.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(44.dp)
                .let {
                    if (canSend) {
                        it.shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0xFF00FF41), ambientColor = Color(0xFF00FF41))
                    } else {
                        it
                    }
                }
                .clip(RoundedCornerShape(14.dp))
                .background(if (canSend) Color(0xFF00FF41) else CedalColors.CardBackground)
                .border(1.dp, if (canSend) Color(0xFF00FF41) else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .clickable(
                    enabled = !sending,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = if (canSend) onSend else onOpenRecordSheet,
                ),
        ) {
            if (canSend) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = CedalColors.Background,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    "✕",
                    color = CedalColors.TextMuted, fontSize = 20.sp,
                )
            }
        }
    }
}

enum class StickerPanelTab { EMOJI, STICKERS, ICON }

// Every emoji here is hand-listed rather than pulled from a Unicode data
// file, so this is a large, category-spanning set (smileys, gestures,
// animals, food, activities, travel, objects, symbols, flags) rather than
// the full ~3,700-glyph Unicode registry - each one is named so the search
// bar has something to match against.
val ALL_EMOJI: List<Pair<String, String>> = listOf(
    // Smileys & emotion
    "grinning" to "😀", "grinning big eyes" to "😃", "grinning smiling eyes" to "😄", "beaming" to "😁",
    "laughing" to "😆", "sweat smile" to "😅", "rofl" to "🤣", "joy" to "😂",
    "slight smile" to "🙂", "upside down" to "🙃", "wink" to "😉", "smiling eyes" to "😊",
    "halo" to "😇", "heart eyes smile" to "🥰", "heart eyes" to "😍", "star struck" to "🤩",
    "kiss" to "😘", "kiss smile" to "😗", "kiss closed eyes" to "😚", "kiss wink" to "😙",
    "yum" to "😋", "tongue" to "😛", "tongue wink" to "😜", "zany" to "🤪",
    "tongue eyes" to "😝", "money mouth" to "🤑", "hug" to "🤗", "hand over mouth" to "🤭",
    "shush" to "🤫", "thinking" to "🤔", "zipper mouth" to "🤐", "raised eyebrow" to "🤨",
    "neutral" to "😐", "expressionless" to "😑", "no mouth" to "😶", "smirk" to "😏",
    "unamused" to "🙄", "roll eyes" to "🙄", "grimace" to "😬", "lying" to "🤥",
    "relieved" to "😌", "pensive" to "😔", "sleepy" to "😪", "drooling" to "🤤",
    "sleeping" to "😴", "mask" to "😷", "thermometer face" to "🤒", "bandage face" to "🤕",
    "nauseated" to "🤢", "vomit" to "🤮", "sneeze" to "🤧", "hot face" to "🥵",
    "cold face" to "🥶", "woozy" to "🥴", "dizzy" to "😵", "exploding head" to "🤯",
    "cowboy" to "🤠", "party" to "🥳", "sunglasses" to "😎", "nerd" to "🤓",
    "monocle" to "🧐", "confused" to "😕", "worried" to "😟", "slight frown" to "🙁",
    "frown" to "☹️", "open mouth" to "😮", "hushed" to "😯", "astonished" to "😲",
    "flushed" to "😳", "pleading" to "🥺", "frowning open" to "😦", "anguished" to "😧",
    "fearful" to "😨", "anxious sweat" to "😰", "sad relieved" to "😥", "crying" to "😢",
    "loud cry" to "😭", "scream" to "😱", "confounded" to "😖", "persevere" to "😣",
    "disappointed" to "😞", "sweat" to "😓", "weary" to "😩", "tired" to "😫",
    "yawn" to "🥱", "triumph" to "😤", "angry" to "😡", "rage" to "😠",
    "cursing" to "🤬", "devil smile" to "😈", "devil" to "👿", "skull" to "💀",
    "skull crossbones" to "☠️", "poop" to "💩", "clown" to "🤡", "ogre" to "👹",
    "goblin" to "👺", "ghost" to "👻", "alien" to "👽", "robot" to "🤖",
    "cat grin" to "😺", "cat joy" to "😸", "cat heart eyes" to "😻", "cat smirk" to "😼",
    "cat wry" to "😽", "cat weary" to "🙀", "cat crying" to "😿", "cat pouting" to "😾",
    // Gestures & body
    "wave" to "👋", "raised hand back" to "🤚", "hand splayed" to "🖐️", "hand" to "✋",
    "vulcan" to "🖖", "ok hand" to "👌", "pinched fingers" to "🤌", "pinch" to "🤏",
    "peace" to "✌️", "crossed fingers" to "🤞", "love you" to "🤟", "rock on" to "🤘",
    "call me" to "🤙", "point left" to "👈", "point right" to "👉", "point up" to "👆",
    "middle finger" to "🖕", "point down" to "👇", "point up two" to "☝️", "thumbs up" to "👍",
    "thumbs down" to "👎", "fist raised" to "✊", "fist" to "👊", "fist left" to "🤛",
    "fist right" to "🤜", "clap" to "👏", "raised hands" to "🙌", "open hands" to "👐",
    "palms together" to "🤲", "pray" to "🙏", "handshake" to "🤝", "writing hand" to "✍️",
    "nail polish" to "💅", "selfie" to "🤳", "muscle" to "💪", "leg" to "🦵",
    "foot" to "🦶", "ear" to "👂", "nose" to "👃", "brain" to "🧠",
    "eyes" to "👀", "eye" to "👁️", "tongue body" to "👅", "lips" to "👄",
    // Animals & nature
    "dog" to "🐶", "cat" to "🐱", "mouse" to "🐭", "hamster" to "🐹",
    "rabbit" to "🐰", "fox" to "🦊", "bear" to "🐻", "panda" to "🐼",
    "koala" to "🐨", "tiger" to "🐯", "lion" to "🦁", "cow" to "🐮",
    "pig" to "🐷", "frog" to "🐸", "monkey" to "🐵", "chicken" to "🐔",
    "penguin" to "🐧", "bird" to "🐦", "chick" to "🐣", "duck" to "🦆",
    "eagle" to "🦅", "owl" to "🦉", "bat" to "🦇", "wolf" to "🐺",
    "boar" to "🐗", "horse" to "🐴", "unicorn" to "🦄", "bee" to "🐝",
    "bug" to "🐛", "butterfly" to "🦋", "snail" to "🐌", "ladybug" to "🐞",
    "ant" to "🐜", "spider" to "🕷️", "scorpion" to "🦂", "turtle" to "🐢",
    "snake" to "🐍", "lizard" to "🦎", "t-rex" to "🦖", "octopus" to "🐙",
    "shrimp" to "🦐", "crab" to "🦀", "blowfish" to "🐡", "tropical fish" to "🐠",
    "fish" to "🐟", "dolphin" to "🐬", "whale" to "🐳", "shark" to "🦈",
    "crocodile" to "🐊", "zebra" to "🦓", "gorilla" to "🦍", "elephant" to "🐘",
    "rhino" to "🦏", "camel" to "🐪", "giraffe" to "🦒", "kangaroo" to "🦘",
    "sheep" to "🐑", "llama" to "🦙", "goat" to "🐐", "deer" to "🦌",
    "poodle" to "🐩", "turkey" to "🦃", "peacock" to "🦚", "parrot" to "🦜",
    "swan" to "🦢", "dove" to "🕊️", "raccoon" to "🦝", "skunk" to "🦨",
    "sloth" to "🦥", "hedgehog" to "🦔", "paw prints" to "🐾", "dragon" to "🐉",
    "cactus" to "🌵", "tree" to "🌳", "palm tree" to "🌴", "seedling" to "🌱",
    "herb" to "🌿", "four leaf clover" to "🍀", "leaves" to "🍃", "maple leaf" to "🍁",
    "flower" to "🌸", "rose" to "🌹", "sunflower" to "🌻", "tulip" to "🌷",
    "hibiscus" to "🌺", "bouquet" to "💐", "earth" to "🌍", "moon" to "🌙",
    "full moon" to "🌕", "sun face" to "🌞", "star glow" to "🌟", "sparkle" to "✨",
    "comet" to "☄️", "rainbow" to "🌈",
    // Food & drink
    "apple" to "🍎", "green apple" to "🍏", "pear" to "🍐", "orange" to "🍊",
    "lemon" to "🍋", "banana" to "🍌", "watermelon" to "🍉", "grapes" to "🍇",
    "strawberry" to "🍓", "cherries" to "🍒", "peach" to "🍑", "mango" to "🥭",
    "pineapple" to "🍍", "coconut" to "🥥", "kiwi" to "🥝", "tomato" to "🍅",
    "avocado" to "🥑", "eggplant" to "🍆", "carrot" to "🥕", "corn" to "🌽",
    "potato" to "🥔", "bread" to "🍞", "croissant" to "🥐", "cheese" to "🧀",
    "egg" to "🍳", "pancakes" to "🥞", "bacon" to "🥓", "burger" to "🍔",
    "fries" to "🍟", "pizza" to "🍕", "hotdog" to "🌭", "taco" to "🌮",
    "burrito" to "🌯", "salad" to "🥗", "spaghetti" to "🍝", "ramen" to "🍜",
    "sushi" to "🍣", "bento" to "🍱", "dumpling" to "🥟", "rice ball" to "🍙",
    "curry" to "🍛", "shaved ice" to "🍧", "ice cream" to "🍦", "donut" to "🍩",
    "cookie" to "🍪", "birthday cake" to "🎂", "shortcake" to "🍰", "chocolate" to "🍫",
    "candy" to "🍬", "lollipop" to "🍭", "honey" to "🍯", "popcorn" to "🍿",
    "milk" to "🥛", "coffee" to "☕", "tea" to "🍵", "beer" to "🍺",
    "clinking beer" to "🍻", "wine" to "🍷", "cocktail" to "🍸", "tropical drink" to "🍹",
    "champagne" to "🍾", "bottle" to "🍶",
    // Activities & travel
    "soccer ball" to "⚽", "basketball" to "🏀", "football" to "🏈", "baseball" to "⚾",
    "tennis" to "🎾", "volleyball" to "🏐", "rugby" to "🏉", "8 ball" to "🎱",
    "ping pong" to "🏓", "badminton" to "🏸", "hockey" to "🏒", "cricket" to "🏏",
    "golf" to "⛳", "bow arrow" to "🏹", "fishing" to "🎣", "boxing glove" to "🥊",
    "martial arts" to "🥋", "medal" to "🏅", "trophy" to "🏆", "flag checkered" to "🏁",
    "roller coaster" to "🎢", "circus tent" to "🎪", "artist" to "🎨", "clapper" to "🎬",
    "microphone" to "🎤", "headphones" to "🎧", "musical score" to "🎼", "drum" to "🥁",
    "saxophone" to "🎷", "guitar" to "🎸", "violin" to "🎻", "dice" to "🎲",
    "puzzle piece" to "🧩", "dart" to "🎯", "video game" to "🎮", "slot machine" to "🎰",
    "airplane" to "✈️", "rocket" to "🚀", "helicopter" to "🚁", "car" to "🚗",
    "taxi" to "🚕", "bus" to "🚌", "police car" to "🚓", "ambulance" to "🚑",
    "fire truck" to "🚒", "truck" to "🚚", "train" to "🚂", "tram" to "🚊",
    "boat" to "⛵", "speedboat" to "🚤", "ship" to "🚢", "anchor" to "⚓",
    "world map" to "🗺️", "mountain" to "⛰️", "volcano" to "🌋", "camping" to "🏕️",
    "beach umbrella" to "🏖️", "desert" to "🏜️", "castle" to "🏰", "stadium" to "🏟️",
    "ferris wheel" to "🎡", "house" to "🏠", "office" to "🏢", "hospital" to "🏥",
    "bank" to "🏦", "school" to "🏫", "church" to "⛪", "mosque" to "🕌",
    // Objects & symbols
    "watch" to "⌚", "phone" to "📱", "laptop" to "💻", "keyboard" to "⌨️",
    "camera" to "📷", "video camera" to "🎥", "battery" to "🔋", "bulb" to "💡",
    "flashlight" to "🔦", "candle" to "🕯️", "money bag" to "💰", "dollar" to "💵",
    "credit card" to "💳", "gem" to "💎", "wrench" to "🔧", "hammer" to "🔨",
    "gear" to "⚙️", "chain" to "⛓️", "lock" to "🔒", "unlock" to "🔓",
    "key" to "🔑", "pill" to "💊", "syringe" to "💉", "door" to "🚪",
    "bed" to "🛏️", "toilet" to "🚽", "shower" to "🚿", "bathtub" to "🛁",
    "shopping cart" to "🛒", "gift" to "🎁", "balloon" to "🎈", "party popper" to "🎉",
    "confetti" to "🎊", "envelope" to "✉️", "package" to "📦", "clipboard" to "📋",
    "calendar" to "📅", "chart up" to "📈", "chart down" to "📉", "book" to "📖",
    "books" to "📚", "pencil" to "✏️", "paperclip" to "📎", "scissors" to "✂️",
    "magnifying glass" to "🔍", "bell" to "🔔", "megaphone" to "📣", "speech bubble" to "💬",
    "red heart" to "❤️", "orange heart" to "🧡", "yellow heart" to "💛", "green heart" to "💚",
    "blue heart" to "💙", "purple heart" to "💜", "black heart" to "🖤", "white heart" to "🤍",
    "broken heart" to "💔", "heart exclaim" to "❣️", "two hearts" to "💕", "sparkling heart" to "💖",
    "heartbeat" to "💓", "revolving hearts" to "💞", "heart with arrow" to "💘", "heart box" to "💝",
    "peace symbol" to "☮️", "yin yang" to "☯️", "om" to "🕉️", "star of david" to "✡️",
    "wheel of dharma" to "☸️", "atom" to "⚛️", "warning" to "⚠️", "no entry" to "⛔",
    "prohibited" to "🚫", "recycle" to "♻️", "check mark" to "✅", "cross mark" to "❌",
    "question" to "❓", "exclamation" to "❗", "hundred" to "💯", "fire symbol" to "🔥",
    "sparkles" to "✨", "boom" to "💥", "collision" to "💢", "zzz" to "💤",
    "arrow right" to "➡️", "arrow left" to "⬅️", "arrow up" to "⬆️", "arrow down" to "⬇️",
    "infinity" to "♾️", "plus" to "➕", "minus" to "➖", "multiply" to "✖️",
    "divide" to "➗", "crown" to "👑", "gem stone" to "💎", "top hat" to "🎩",
    "graduation cap" to "🎓", "id card" to "🪪", "trophy cup" to "🏆",
    // Flags
    "checkered flag" to "🏁", "triangular flag" to "🚩", "crossed flags" to "🎌",
    "white flag" to "🏳️", "black flag" to "🏴", "rainbow flag" to "🏳️‍🌈",
    "usa flag" to "🇺🇸", "uk flag" to "🇬🇧", "canada flag" to "🇨🇦", "australia flag" to "🇦🇺",
    "germany flag" to "🇩🇪", "france flag" to "🇫🇷", "italy flag" to "🇮🇹", "spain flag" to "🇪🇸",
    "japan flag" to "🇯🇵", "korea flag" to "🇰🇷", "china flag" to "🇨🇳", "india flag" to "🇮🇳",
    "brazil flag" to "🇧🇷", "mexico flag" to "🇲🇽", "nigeria flag" to "🇳🇬", "south africa flag" to "🇿🇦",
    "kenya flag" to "🇰🇪", "ghana flag" to "🇬🇭", "netherlands flag" to "🇳🇱", "sweden flag" to "🇸🇪",
)

// One panel, three tabs - Emoji (inserts into the text field), Stickers
// (default pack + your own uploaded ones, sends instantly), and Icon
// (default monochrome-glyph pack, also sends instantly). Which tab opens
// first is picked by EmojiChoiceOverlay before this even shows.
@Composable
fun StickerPickerOverlay(
    myStickers: List<StickerDto>,
    uploading: Boolean,
    initialTab: StickerPanelTab = StickerPanelTab.EMOJI,
    onPickEmoji: (String) -> Unit,
    onPickSticker: (String) -> Unit,
    onUploadSticker: () -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableStateOf(initialTab) }
    // Resets whenever the tab switches - a leftover query from Emoji
    // shouldn't silently filter the Icon grid too.
    var query by remember { mutableStateOf("") }
    fun switchTab(next: StickerPanelTab) {
        tab = next
        query = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .imePadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}) // absorb taps
                .padding(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StickerPanelTabButton("😊", "Emoji", tab == StickerPanelTab.EMOJI) { switchTab(StickerPanelTab.EMOJI) }
                StickerPanelTabButton("🧩", "Stickers", tab == StickerPanelTab.STICKERS) { switchTab(StickerPanelTab.STICKERS) }
                StickerPanelTabButton("★", "Icon", tab == StickerPanelTab.ICON) { switchTab(StickerPanelTab.ICON) }
            }

            when (tab) {
                StickerPanelTab.EMOJI -> {
                    PickerSearchBar(query, { query = it }, "Search emoji…")
                    val filtered = if (query.isBlank()) ALL_EMOJI else ALL_EMOJI.filter { it.first.contains(query, ignoreCase = true) }
                    Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        filtered.chunked(8).forEach { rowEmoji ->
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                rowEmoji.forEach { (name, emoji) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPickEmoji(emoji) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(emoji, fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                        if (filtered.isEmpty()) {
                            Text("No emoji found", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
                StickerPanelTab.STICKERS -> {
                    // My Stickers - private to this account (see
                    // StickerService), a "+" tile at the front opens the
                    // photo picker to make a new one. No default pack
                    // anymore (those were emoji) - just what you've made.
                    Text("YOUR STICKERS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 10.dp))
                    val myRows = (listOf<StickerDto?>(null) + myStickers).chunked(6)
                    myRows.forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            rowItems.forEach { sticker ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            if (sticker != null) onPickSticker(sticker.imageUrl) else onUploadSticker()
                                        }
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (sticker != null) {
                                        coil.compose.AsyncImage(
                                            model = sticker.imageUrl,
                                            contentDescription = "Sticker",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                            modifier = Modifier.size(36.dp),
                                        )
                                    } else if (uploading) {
                                        androidx.compose.material3.CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("+", color = CedalColors.AccentCyan, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (myStickers.isEmpty() && !uploading) {
                        Text("No custom stickers yet - tap + to make one from a photo.", color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                StickerPanelTab.ICON -> {
                    PickerSearchBar(query, { query = it }, "Search icons…")
                    val filtered = if (query.isBlank()) ALL_ICONS else ALL_ICONS.filter { it.first.contains(query, ignoreCase = true) }
                    Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        filtered.chunked(6).forEach { rowIcons ->
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                rowIcons.forEach { (name, vector) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPickSticker("icon:$name") }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(vector, contentDescription = name, tint = CedalColors.AccentCyan, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                        if (filtered.isEmpty()) {
                            Text("No icons found", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerSearchBar(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(50))
            .background(CedalColors.Background)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = CedalColors.TextMuted, modifier = Modifier.size(16.dp))
        Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            if (query.isEmpty()) {
                Text(placeholder, color = CedalColors.TextMuted, fontSize = 13.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(CedalColors.AccentCyan),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StickerPanelTabButton(emoji: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (active) CedalColors.AccentCyan.copy(alpha = 0.15f) else CedalColors.BackgroundBlob)
                .border(1.dp, if (active) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Text(label, color = if (active) CedalColors.AccentCyan else CedalColors.TextSecondary, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

internal fun formatMessageTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

// Only the filename SAF's OpenDocument hands back is a content:// URI, not
// a human-readable name - this is the standard ContentResolver lookup for
// that (OpenableColumns.DISPLAY_NAME), same as any file-picking Android app.
fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String? =
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull()

// The chat thread's header - name is tappable (opens that friend's
// read-only profile) and ⋮ opens Delete Chat / View Once, matching
// standard "1-on-1 chat" header conventions instead of the plain
// title+back MemberBackBar every other screen in this app uses.
@Composable
private fun ChatHeader(friendName: String, isTyping: Boolean = false, onBack: () -> Unit, onOpenProfile: () -> Unit, onOpenMenu: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack)
                .padding(6.dp),
        ) {
            androidx.compose.material3.Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = CedalColors.TextPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenProfile),
        ) {
            Text(friendName, color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (isTyping) {
                Text("Composing…", color = CedalColors.AccentCyan, fontSize = 11.sp)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenMenu)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("⋮", color = CedalColors.TextPrimary, fontSize = 20.sp)
        }
    }
}

@Composable
private fun ChatHeaderMenuOverlay(
    viewOnceMode: String?,
    isPinned: Boolean,
    isBlocked: Boolean,
    onToggleViewOnce: () -> Unit,
    onTogglePin: () -> Unit,
    onSearch: () -> Unit,
    onExport: () -> Unit,
    onReport: () -> Unit,
    onToggleBlock: () -> Unit,
    onShortcut: () -> Unit,
    onPopularity: () -> Unit,
    onDeleteChat: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.TopEnd,
    ) {
        Column(
            modifier = Modifier
                .padding(top = 60.dp, end = 16.dp)
                .width(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(8.dp),
        ) {
            ActionMenuRow(
                label = when (viewOnceMode) {
                    null -> "View Once"
                    "custom_time" -> "View Once: Time Once ON"
                    else -> "View Once: View Once ON"
                },
                color = if (viewOnceMode != null) CedalColors.Error else CedalColors.TextPrimary,
                onClick = onToggleViewOnce,
            )
            ActionMenuRow(label = if (isPinned) "Unpin" else "Pin", color = CedalColors.TextPrimary, onClick = onTogglePin)
            ActionMenuRow(label = "Search", color = CedalColors.TextPrimary, onClick = onSearch)
            ActionMenuRow(label = "Export Chat", color = CedalColors.TextPrimary, onClick = onExport)
            ActionMenuRow(label = "Add Shortcut", color = CedalColors.TextPrimary, onClick = onShortcut)
            ActionMenuRow(label = "Popularity", color = CedalColors.TextPrimary, onClick = onPopularity)
            ActionMenuRow(label = "Report", color = CedalColors.Error, onClick = onReport)
            ActionMenuRow(label = if (isBlocked) "Unblock" else "Block", color = CedalColors.Error, onClick = onToggleBlock)
            ActionMenuRow(label = "Delete Chat", color = CedalColors.Error, onClick = onDeleteChat)
        }
    }
}

// "Popularity" (header ⋮ menu) - per-chat override of Settings > Security >
// Popularity, scoped to just this one friend (see PopularityService's
// ChatPopularityOverrides doc comment server-side). Each field is tri-state:
// "Default" (null, defer to the global setting), "Show", or "Hide".
@Composable
private fun ChatPopularityOverlay(friendId: String, viewModel: AuthViewModel, onDismiss: () -> Unit) {
    var override by remember { mutableStateOf(com.xhacker.cedal.data.ChatPopularityOverrideDto()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(friendId) {
        viewModel.getChatPopularityOverride(friendId).onSuccess { override = it }
    }

    fun update(next: com.xhacker.cedal.data.ChatPopularityOverrideDto) {
        override = next
        scope.launch { viewModel.setChatPopularityOverride(friendId, next) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .width(300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(18.dp),
        ) {
            Text("Popularity for this chat", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                "Overrides your global Popularity setting just for this person.",
                color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp),
            )
            PopularityOverrideRow("Name", override.showName) { update(override.copy(showName = it)) }
            PopularityOverrideRow("Profile Picture", override.showPfp) { update(override.copy(showPfp = it)) }
            PopularityOverrideRow("Age", override.showAge) { update(override.copy(showAge = it)) }
            PopularityOverrideRow("Rank", override.showRank) { update(override.copy(showRank = it)) }
            PopularityOverrideRow("Occupation", override.showOccupation) { update(override.copy(showOccupation = it)) }
            PopularityOverrideRow("Hobby", override.showHobby) { update(override.copy(showHobby = it)) }
            PopularityOverrideRow("Bio", override.showBio) { update(override.copy(showBio = it)) }
            PopularityOverrideRow("Gender", override.showGender) { update(override.copy(showGender = it)) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text("DONE", color = CedalColors.TextPrimary, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun PopularityOverrideRow(label: String, value: Boolean?, onChange: (Boolean?) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(label, color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        Row {
            listOf<Pair<String, Boolean?>>("Default" to null, "Show" to true, "Hide" to false).forEach { (chipLabel, chipValue) ->
                val selected = value == chipValue
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) CedalColors.AccentCyan else Color.Transparent)
                        .border(1.dp, if (selected) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(50))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(chipValue) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(chipLabel, color = if (selected) CedalColors.Background else CedalColors.TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// "View Once" (header ⋮ menu) - tapping it pops this panel instead of
// toggling directly: plain "View Once" (the classic single-reveal) or
// "Custom", which itself offers two mutually-exclusive limits - a time
// window after first reveal, or a fixed number of reveals - see
// ChatService.revealMessage's doc comment server-side for how each behaves.
// Both real options now ask for a number up front - "View Once" for how
// many times it can be opened (default 1, same as the old single-reveal
// behavior when left unchanged), "Time Once" for how long after first
// opening it stays viewable (secs/mins/hrs/days, clamped to 7 days - see
// ChatService.sendMessage's server-side clamp, mirrored here client-side).
@Composable
private fun ViewOnceModeOverlay(
    onPickCustomTime: (durationMs: Long) -> Unit,
    onPickCustomCount: (maxViews: Int) -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit,
) {
    var screen by remember { mutableStateOf("menu") } // "menu" | "count" | "time"
    var countText by remember { mutableStateOf("1") }
    var amountText by remember { mutableStateOf("30") }
    var unit by remember { mutableStateOf("Seconds") } // Seconds | Minutes | Hours | Days
    val maxDurationMs = 7L * 24 * 60 * 60 * 1000

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .width(280.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(18.dp),
        ) {
            Text("View Once", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))

            when (screen) {
                "menu" -> {
                    Text(
                        "Choose how this message disappears after it's sent.",
                        color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp),
                    )
                    ViewOnceChoiceRow(label = "View Once", description = "Set how many times it can be viewed.", onClick = { screen = "count" })
                    ViewOnceChoiceRow(label = "Time Once", description = "Set how long it stays viewable, up to 7 days.", onClick = { screen = "time" })
                    ViewOnceChoiceRow(label = "Turn Off", description = "Send normally, no view-once.", onClick = onTurnOff)
                }
                "count" -> {
                    Text("How many times can it be viewed?", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = countText,
                            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) countText = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(CedalColors.AccentCyan),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        )
                    }
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { screen = "menu" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("BACK", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CedalColors.AccentCyan)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    val amount = (countText.toIntOrNull() ?: 1).coerceIn(1, 1000)
                                    onPickCustomCount(amount)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("SET", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                else -> {
                    Text("Disappears this long after it's first opened (max 7 days).", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = amountText,
                                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) amountText = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(CedalColors.AccentCyan),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            )
                        }
                        listOf("Seconds", "Minutes", "Hours", "Days").forEach { u ->
                            Text(
                                u.take(3), color = if (unit == u) CedalColors.Background else CedalColors.TextPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (unit == u) CedalColors.AccentCyan else Color.Transparent)
                                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { unit = u }
                                    .padding(horizontal = 7.dp, vertical = 6.dp),
                            )
                        }
                    }
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { screen = "menu" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("BACK", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CedalColors.AccentCyan)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    val amount = amountText.toIntOrNull() ?: return@clickable
                                    if (amount < 1) return@clickable
                                    val multiplier = when (unit) { "Minutes" -> 60_000L; "Hours" -> 3_600_000L; "Days" -> 86_400_000L; else -> 1_000L }
                                    // "if they have 7 days and even 1 minute it just defaults
                                    // back to 7 days" - clamp, don't reject.
                                    onPickCustomTime((amount * multiplier).coerceAtMost(maxDurationMs))
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("SET", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewOnceChoiceRow(label: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(label, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(description, color = CedalColors.TextSecondary, fontSize = 11.sp)
    }
}

// "Search" (header ⋮ menu) - a simple contains-match over this thread's own
// messages (server-side full-text search would be overkill for a single
// conversation's history), tapping a hit reuses the existing pendingScrollTo
// jump-to-message mechanism (see MemberChatThreadBody).
@Composable
private fun ChatSearchOverlay(messages: List<ChatMessageDto>, onResultTap: (ChatMessageDto) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, messages) {
        if (query.isBlank()) emptyList() else messages.filter { it.text.contains(query, ignoreCase = true) }.reversed()
    }
    Box(modifier = Modifier.fillMaxSize().background(CedalColors.Background).imePadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(CedalColors.AccentCyan),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Search this chat", color = CedalColors.TextSecondary, fontSize = 14.sp)
                            inner()
                        },
                    )
                }
                Text(
                    "CANCEL", color = CedalColors.AccentCyan, fontSize = 12.sp,
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(12.dp),
                )
            }
            if (query.isNotBlank() && results.isEmpty()) {
                Text("No matches.", color = CedalColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { msg ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onResultTap(msg) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(formatMessageTime(msg.sentAt), color = CedalColors.TextMuted, fontSize = 11.sp)
                        Text(msg.text, color = CedalColors.TextPrimary, fontSize = 13.sp, maxLines = 2)
                    }
                    RowDivider()
                }
            }
        }
    }
}

// Replaces the normal composer for the "Cedal Team" thread - same idea as
// SystemFeedBody's read-only-for-non-admins state, but with two real
// actions instead of just a note. Revoke opens a confirm dialog first (see
// revokeConfirmOpen); Generate Key fires immediately since it's non-
// destructive (the resulting key message spells out its own session-only
// expiry - see DeveloperAccessService.sendKeyMessage).
@Composable
private fun CedalTeamActionPanel(
    hasDeveloperAccess: Boolean,
    busy: Boolean,
    error: String?,
    onGenerateKey: () -> Unit,
    onRequestRevoke: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(
            "Cedal Team messages are automated - you can't reply here.",
            color = CedalColors.TextMuted, fontSize = 12.sp,
        )
        error?.let { Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
        if (hasDeveloperAccess) {
            Row(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    "GENERATE KEY",
                    color = if (busy) CedalColors.TextMuted else CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(enabled = !busy, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onGenerateKey)
                        .padding(end = 20.dp, top = 4.dp, bottom = 4.dp),
                )
                Text(
                    "REVOKE MY DEVELOPER ACCOUNT",
                    color = if (busy) CedalColors.TextMuted else CedalColors.Error, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(enabled = !busy, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRequestRevoke)
                        .padding(top = 4.dp, bottom = 4.dp),
                )
            }
        } else {
            Text(
                "You don't currently have developer access.",
                color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

// Shared small "are you sure" prompt for Block - lighter-weight than
// DeleteChatConfirmOverlay's styling (no destructive-red border) since it
// isn't irreversible the way deleting a chat is. (Report has its own
// richer ReportOverlay below, with a reason field and evidence attachment.)
@Composable
private fun SimpleConfirmOverlay(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(20.dp),
        ) {
            Text(title, color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(body, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
            Row {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("CANCEL", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CedalColors.Error)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onConfirm)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(confirmLabel, color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// "Report" (chat thread ⋮ menu) - a reason (optional, free text) plus a
// single optional evidence attachment (photo/video/or any file), uploaded
// through the same /uploads flow chat attachments already use. A plain
// system content picker rather than the full camera/gallery/file sheet -
// reporting evidence is always an EXISTING photo/video/file already on the
// device, never something you'd want to capture live in the moment.
@Composable
private fun ReportOverlay(
    friendName: String,
    viewModel: AuthViewModel,
    onSubmit: (reason: String?, mediaUrl: String?, mediaType: String?, fileName: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var pickedFileName by remember { mutableStateOf<String?>(null) }
    var pickedMediaType by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadedUrl by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val mediaType = when {
            mime.startsWith("image/") -> "image"
            mime.startsWith("video/") -> "video"
            else -> "file"
        }
        pickedMediaType = mediaType
        pickedFileName = queryDisplayName(context, uri)
        uploadedUrl = null
        uploading = true
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                viewModel.uploadImage(if (mediaType == "file") "chat_file" else "chat_media", bytes, mime)
                    .onSuccess { url -> uploadedUrl = url }
            }
            uploading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(20.dp),
        ) {
            Text("Report $friendName?", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "This flags the conversation for admin review. It won't notify $friendName.",
                color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 2000) reason = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(CedalColors.AccentCyan),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    decorationBox = { inner ->
                        if (reason.isEmpty()) Text("Reason (optional)", color = CedalColors.TextSecondary, fontSize = 13.sp)
                        inner()
                    },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    if (pickedFileName != null) "📎 $pickedFileName" else "Attach photo, video, or file",
                    color = if (pickedFileName != null) CedalColors.TextPrimary else CedalColors.AccentCyan,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { picker.launch("*/*") },
                )
                if (uploading) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CedalColors.AccentCyan)
                } else if (pickedFileName != null) {
                    Text(
                        "✕", color = CedalColors.Error, fontSize = 14.sp,
                        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            pickedFileName = null; pickedMediaType = null; uploadedUrl = null
                        },
                    )
                }
            }

            Row(modifier = Modifier.padding(top = 18.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("CANCEL", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (uploading) CedalColors.BorderSlate else CedalColors.Error)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !uploading) {
                            onSubmit(reason.trim().ifBlank { null }, uploadedUrl, pickedMediaType, pickedFileName)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("REPORT", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun DeleteChatConfirmOverlay(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.Error, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(20.dp),
        ) {
            Text("Delete this chat?", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "This deletes every message in this conversation for both of you. This can't be undone.",
                color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            Row {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("CANCEL", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CedalColors.Error)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onConfirm)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("DELETE", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// Telegram-style attachment sheet - tapping the "›" glyph opens this, a
// flat row of real icons (Camera/Emoji/Folder), not text or emoji. Gallery
// lives inside Camera's own sub-choice (CameraChoiceOverlay) and Sticker
// lives inside Emoji's own sub-choice (EmojiChoiceOverlay) rather than each
// getting a top-level slot. Folder reuses the same system document picker
// comment as AttachmentMenuOverlay below (no custom file browser - modern
// Android blocks that without special store approval).
@Composable
fun AttachmentSheetOverlay(
    onCamera: () -> Unit,
    onEmoji: () -> Unit,
    onFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(20.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AttachSheetIcon(Icons.Filled.CameraAlt, "Camera", onCamera)
            AttachSheetIcon(Icons.Filled.EmojiEmotions, "Emoji", onEmoji)
            AttachSheetIcon(Icons.Filled.Folder, "Folder", onFolder)
        }
    }
}

@Composable
fun AttachSheetIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CedalColors.BackgroundBlob)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = CedalColors.AccentCyan, modifier = Modifier.size(24.dp))
        }
        Text(label, color = CedalColors.TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

// Opened by the "+" that replaces Send while the composer is empty (see
// ChatInputBar) - Bubble (a circular video note, WhatsApp-style - real
// video, played back clipped to a circle with sound), Voice (a plain voice
// note - see startRecording/stopRecordingAndSend), Square (a video note
// played back clipped to a square with NO sound, for when you want to show
// something without anyone hearing audio). Bubble/Square both record via
// the exact same camera-video capture as a normal video attach - only the
// mediaType tag differs (vid_bubble/vid_square vs plain video), see
// pendingVideoNoteType/cameraVideoLauncher above; the circle/square clip
// and mute happen at PLAYBACK time (MediaAttachment), not at capture time.
@Composable
fun RecordOptionsOverlay(onBubble: () -> Unit, onVoice: () -> Unit, onSquare: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(20.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AttachSheetIcon(Icons.Filled.Circle, "Bubble", onBubble)
            AttachSheetIcon(Icons.Filled.Mic, "Voice", onVoice)
            AttachSheetIcon(Icons.Filled.CropSquare, "Square", onSquare)
        }
    }
}

// What tapping "Camera" actually opens - a real camera app also lets you
// switch to video or jump to your gallery without leaving the capture
// flow; ACTION_IMAGE_CAPTURE alone (what TakePicture() sends) doesn't
// expose either, so this replaces that missing chrome.
@Composable
fun CameraChoiceOverlay(onPhoto: () -> Unit, onVideo: () -> Unit, onGallery: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(20.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AttachSheetIcon(Icons.Filled.CameraAlt, "Photo", onPhoto)
            AttachSheetIcon(Icons.Filled.Videocam, "Video", onVideo)
            AttachSheetIcon(Icons.Filled.PhotoLibrary, "Gallery", onGallery)
        }
    }
}

// What tapping "Emoji" actually opens - same idea as CameraChoiceOverlay:
// a real emoji button also lets you jump straight to Stickers or the Icon
// pack without leaving the flow, so this replaces jumping straight to the
// Emoji tab of StickerPickerOverlay.
@Composable
fun EmojiChoiceOverlay(onEmoji: () -> Unit, onSticker: () -> Unit, onIcon: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(20.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AttachSheetIcon(Icons.Filled.EmojiEmotions, "Emoji", onEmoji)
            AttachSheetIcon(Icons.Filled.Extension, "Sticker", onSticker)
            AttachSheetIcon(Icons.Filled.Widgets, "Icon", onIcon)
        }
    }
}

// The "+" menu - Camera, Files (system document picker - see
// MemberChatThreadBody's filePicker comment for why this isn't a custom-
// built file browser), and Poll. Emoji/Stickers/Gallery live in the other
// button (StickerPickerOverlay) instead, WhatsApp-style.
@Composable
private fun AttachmentMenuOverlay(
    uploading: Boolean,
    onCameraPhoto: () -> Unit,
    onCameraVideo: () -> Unit,
    onFiles: () -> Unit,
    onPoll: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Telegram-style: tapping Camera doesn't shoot a photo immediately - it
    // opens a small photo/video choice first (a real in-app camera preview
    // with a swipe-to-video gesture is a much bigger build; this is the
    // realistic version of "camera lets you take video too").
    var cameraChoiceOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(20.dp),
        ) {
            if (uploading) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                    androidx.compose.material3.CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Text("Uploading…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (cameraChoiceOpen) {
                Text("CAMERA", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AttachOption("📷", "Photo", onCameraPhoto)
                    AttachOption("🎥", "Video", onCameraVideo)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AttachOption("📷", "Camera") { cameraChoiceOpen = true }
                    AttachOption("📁", "Files", onFiles)
                    AttachOption("📊", "Poll", onPoll)
                }
            }
        }
    }
}

@Composable
private fun AttachOption(emoji: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CedalColors.BackgroundBlob)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Text(label, color = CedalColors.TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

// Poll composer - a question plus 2-4 options (server enforces the same
// 2-4 bound, see ChatService.sendMessage) - reached via the "+" menu.
@Composable
private fun PollComposerOverlay(sending: Boolean, onCreate: (String, List<String>) -> Unit, onDismiss: () -> Unit) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "")) }
    val canCreate = question.isNotBlank() && options.count { it.isNotBlank() } >= 2 && !sending

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(20.dp),
        ) {
            Text("NEW POLL", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 12.dp))
            PollTextField(value = question, onValueChange = { question = it }, placeholder = "Ask a question…")
            Text("OPTIONS", color = CedalColors.TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            options.forEachIndexed { index, opt ->
                PollTextField(
                    value = opt,
                    onValueChange = { new -> options = options.toMutableList().also { it[index] = new } },
                    placeholder = "Option ${index + 1}",
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            if (options.size < 4) {
                Text(
                    "+ Add option",
                    color = CedalColors.AccentCyan, fontSize = 12.sp,
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { options = options + "" },
                )
            } else {
                Box(modifier = Modifier.padding(bottom = 14.dp))
            }
            Row {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("CANCEL", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canCreate) CedalColors.Success else CedalColors.Success.copy(alpha = 0.35f))
                        .clickable(enabled = canCreate, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onCreate(question, options.filter { it.isNotBlank() })
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("CREATE", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun PollTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CedalColors.Background)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = CedalColors.TextMuted, fontSize = 13.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
            cursorBrush = SolidColor(CedalColors.AccentCyan),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
