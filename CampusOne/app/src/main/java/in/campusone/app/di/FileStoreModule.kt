package `in`.campusone.app.di

import `in`.campusone.app.data.remote.FileStore
import `in`.campusone.app.data.remote.SupabaseFileStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FileStoreModule {

    @Binds
    @Singleton
    abstract fun bindFileStore(impl: SupabaseFileStore): FileStore
}
