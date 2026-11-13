package `in`.campusone.app.data.remote

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseConfig(context: Context): SupabaseConfig =
        SupabaseConfigLoader.load(context)

    @Provides
    @Singleton
    fun provideSupabaseClient(
        context: Context,
        config: SupabaseConfig
    ): SupabaseClient {
        return HttpSupabaseClient(context, config)
    }
}
