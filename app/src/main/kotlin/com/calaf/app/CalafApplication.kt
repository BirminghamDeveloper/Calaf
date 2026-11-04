package com.calaf.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Wiring lives in the flavour Hilt modules (`src/demo` and `src/live`), not here. */
@HiltAndroidApp
class CalafApplication : Application()
