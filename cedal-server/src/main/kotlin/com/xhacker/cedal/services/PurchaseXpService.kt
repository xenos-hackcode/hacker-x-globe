package com.xhacker.cedal.services

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToLong

// The real xp economy math - kept here as pure, testable functions even
// though nothing can actually charge real money yet (Shop's "Buy"/"Add
// payment method" are still explicit no-ops, see MemberShopScreen.kt and
// REMINDER.md - no licensed payment processor wired up). This is ready to be
// called the moment a real purchase/subscription webhook exists; until then
// it's deliberately not exposed via any route, since adding an endpoint that
// grants xp without a verified real payment behind it would just be a free
// xp exploit.
object PurchaseXpService {
    // The one real, named price point: £0.99 buys 10 xp. Everything else -
    // a user topping up any custom amount, subscriptions - scales off this
    // same per-pound rate (10 xp / 0.99 GBP), not a separate rate.
    const val BASE_PRICE_GBP = 0.99
    const val BASE_XP = 10L

    // Every 1st of the month: double xp for direct purchases/top-ups.
    fun isDoubleXpDay(date: LocalDate): Boolean = date.dayOfMonth == 1

    // Every Wednesday: half price for the same xp - mathematically this is
    // the exact same xp-per-pound rate as double xp, just framed as a
    // discount instead of a bonus. Stacks with the 1st-of-month bonus if
    // they ever coincide (a first-of-month Wednesday effectively becomes a
    // 4x day) - simplest rule, no priority ordering to reason about.
    fun isDiscountDay(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.WEDNESDAY

    fun xpMultiplier(date: LocalDate): Int =
        (if (isDoubleXpDay(date)) 2 else 1) * (if (isDiscountDay(date)) 2 else 1)

    // Any amount, not just exact £0.99 blocks - a real top-up, not a fixed
    // "buy 10 xp" button. Rounds to the nearest whole xp.
    fun xpForDirectPurchase(amountGBP: Double, date: LocalDate = LocalDate.now()): Long =
        (amountGBP / BASE_PRICE_GBP * BASE_XP * xpMultiplier(date)).roundToLong()

    data class SubscriptionTier(val name: String, val priceGBP: Double, val bonusPercent: Int)

    // Subscriptions give xp at half the direct-purchase rate (5 xp per £1,
    // not 10) - same as before - but each tier now also adds its own bonus
    // on top, scaled by tier: higher subscriptions give proportionally
    // better xp, matching the "highest tier acceleration" a subscription is
    // supposed to be for. Deliberately independent of the 1st-of-month/
    // Wednesday promos above - those are for one-off top-ups on a specific
    // day, not a fixed monthly charge.
    val SUBSCRIPTION_TIERS = listOf(
        SubscriptionTier("Cedal Overlord", 3.0, bonusPercent = 5),
        SubscriptionTier("Cedal Celestial", 8.0, bonusPercent = 10),
        SubscriptionTier("Cedal Void", 20.0, bonusPercent = 20),
    )

    private const val SUBSCRIPTION_XP_PER_GBP = 5.0

    fun xpForSubscription(tier: SubscriptionTier): Long =
        (tier.priceGBP * SUBSCRIPTION_XP_PER_GBP * (1 + tier.bonusPercent / 100.0)).roundToLong()

    // "Custom" recurring top-up (e.g. 0.10 GBP/day) - unlocked once an
    // account reaches E rank (Shop's second-lowest tier). Only the unlock
    // gate is real here; actually scheduling a recurring charge needs a
    // saved payment method and a real billing provider, neither of which
    // exist yet - see the class comment above.
    const val CUSTOM_TOPUP_REQUIRED_TIER = "E"
    const val CUSTOM_TOPUP_REQUIRED_LEVEL = 1

    fun canUseCustomTopup(xp: Long): Boolean =
        RankService.meetsShopRank(xp, CUSTOM_TOPUP_REQUIRED_TIER, CUSTOM_TOPUP_REQUIRED_LEVEL)
}
