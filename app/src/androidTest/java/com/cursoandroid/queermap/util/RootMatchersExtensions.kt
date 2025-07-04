package com.cursoandroid.queermap.util

import android.view.View
import androidx.test.espresso.Root
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun withDecorView(matcher: Matcher<View>): Matcher<Root> {
    return object : TypeSafeMatcher<Root>() {
        override fun describeTo(description: Description) {
            matcher.describeTo(description)
        }

        override fun matchesSafely(root: Root): Boolean {
            val decorView = root.decorView
            return matcher.matches(decorView)
        }
    }
}
