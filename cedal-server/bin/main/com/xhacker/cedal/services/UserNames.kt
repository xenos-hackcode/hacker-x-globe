package com.xhacker.cedal.services

import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.ResultRow

// Shared by every place that shows another user's name (friend search,
// friend requests/list, wallet transfers, trade posts) - nickname first,
// then email, and only falling all the way back to "Cedal <serial>" (never
// the generic "Unknown") for the rare account with neither set, using the
// account's own stable signup-order number so the same nameless account
// always shows the same fallback name.
fun displayNameFor(row: ResultRow): String =
    row[Users.nickname]?.takeIf { it.isNotBlank() }
        ?: row[Users.email]?.takeIf { it.isNotBlank() }
        ?: "Cedal ${row[Users.serial]}"
