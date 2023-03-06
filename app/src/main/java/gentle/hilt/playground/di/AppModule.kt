package gentle.hilt.playground.di

import android.content.Context
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gentle.hilt.playground.R
import gentle.hilt.playground.data.room.AlarmDataBase
import gentle.hilt.playground.data.room.SleepTrackingDataBase
import gentle.hilt.playground.data.room.dao.DaoAlarm
import gentle.hilt.playground.data.room.dao.SleepTrackingDao
import gentle.hilt.playground.data.room.repository.AlarmEntityRepository
import gentle.hilt.playground.data.room.repository.SleepTrackingRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    // .allowMainThreadQueries() needs for instrumented tests in
    // SleepStatisticFragment,MapScreenFragment,AlarmFragmentTest
    @Singleton
    @Provides
    fun provideAlarmDataBase(@ApplicationContext context: Context): AlarmDataBase {
        return Room.databaseBuilder(context, AlarmDataBase::class.java, "alarm_db")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }

    @Singleton
    @Provides
    fun provideDaoAlarm(dataBase: AlarmDataBase) = dataBase.alarmDao()

    @Singleton
    @Provides
    fun provideSleepTrackingDataBase(
        @ApplicationContext context: Context
    ): SleepTrackingDataBase {
        return Room.databaseBuilder(context, SleepTrackingDataBase::class.java, "sleep_db")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }

    @Singleton
    @Provides
    fun provideDaoSleepTracking(dataBase: SleepTrackingDataBase) = dataBase.sleepTrackingDao()

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context
    ): RequestManager {
        return Glide.with(context).setDefaultRequestOptions(
            RequestOptions().placeholder(R.drawable.ic_setup_alarm)
        )
    }
}

// When you will have api with data that you need to make fakeDouble out of it,
// Just add domain layer then do AlarmEntityRepository(dao) as AlarmRepository
// and easily swap it in instrumented tests with hilt
//          Hilt Stuff..
// repository() = Fake() as AlarmRepository
// Right now my application don't have any internet data so i removed all useless interfaces
@Module
@InstallIn(SingletonComponent::class)
object AlarmRepositoryModule {
    @Singleton
    @Provides
    fun provideAlarmRepository(dao: DaoAlarm) = AlarmEntityRepository(dao)
}

@Module
@InstallIn(SingletonComponent::class)
object TrackingSleepRepository {

    @Singleton
    @Provides
    fun provideTrackingSleepRepository(dao: SleepTrackingDao) = SleepTrackingRepository(dao)
}
