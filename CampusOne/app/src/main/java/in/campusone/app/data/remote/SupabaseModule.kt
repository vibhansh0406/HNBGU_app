package `in`.campusone.app.data.remote

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseConfig(@ApplicationContext context: Context): SupabaseConfig =
        SupabaseConfigLoader.load(context)

    @Provides
    @Singleton
    fun provideSupabaseClient(
        config: SupabaseConfig,
        sessionStore: SessionStore
    ): SupabaseClient {
        return HttpSupabaseClient(config, sessionStore)
    }
}
