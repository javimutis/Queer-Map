package com.cursoandroid.queermap.util

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.cursoandroid.queermap.HiltTestActivity
import com.google.common.base.Preconditions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    fragmentFactory: FragmentFactory? = null,
    crossinline action: T.() -> Unit = {}
) {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    )

    ActivityScenario.launch<HiltTestActivity>(startActivityIntent).onActivity { activity ->
        fragmentFactory?.let { activity.supportFragmentManager.fragmentFactory = it }

        val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
            Preconditions.checkNotNull(T::class.java.classLoader),
            T::class.java.name
        )
        fragment.arguments = fragmentArgs
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment, null)
            .commitNow()
        (fragment as T).action()
    }
}

// Ya tienes esta función en tu código, la mantengo aquí para referencia
suspend fun waitForNavigationTo(
    navController: NavController,
    destinationId: Int,
    timeoutMs: Long = 5000L
) {
    withContext(Dispatchers.Main.limitedParallelism(1)) { // Asegúrate de que esto se ejecute en el Main thread si afecta a la UI o Lifecycle
        withTimeout(timeoutMs) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val listener = object : NavController.OnDestinationChangedListener {
                    override fun onDestinationChanged(
                        controller: NavController,
                        destination: NavDestination,
                        arguments: Bundle?
                    ) {
                        if (destination.id == destinationId) {
                            navController.removeOnDestinationChangedListener(this)
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }
                }

                if (navController.currentDestination?.id == destinationId) {
                    continuation.resume(Unit)
                } else {
                    navController.addOnDestinationChangedListener(listener)
                    continuation.invokeOnCancellation {
                        navController.removeOnDestinationChangedListener(listener)
                    }
                }
            }
        }
    }
}