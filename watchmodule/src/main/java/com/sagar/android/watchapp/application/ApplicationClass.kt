package com.sagar.android.watchapp.application

import android.app.Application
import com.sagar.android.logutilmaster.LogUtil
import com.sagar.android.watchapp.BuildConfig
import com.sagar.android.watchapp.core.KeyWordsAndConstants.LOG_TAG
import com.sagar.android.watchapp.repository.Repository
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

class ApplicationClass : Application(), KodeinAware {

    private val logUtil: LogUtil by instance()

    override fun onCreate() {
        super.onCreate()

        logUtil.logV("^^^ WatchApp App started. ^^^")
    }

    override val kodein = Kodein.lazy {

        import(androidXModule(this@ApplicationClass))

        bind() from singleton {
            LogUtil(
                LogUtil.Builder()
                    .setCustomLogTag(LOG_TAG)
                    .setShouldHideLogInReleaseMode(false, BuildConfig.DEBUG)
            )
        }

        bind() from singleton {
            Repository()
        }

//        bind() from provider { ViewModelProvider(instance()) }
    }
}