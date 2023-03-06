package gentle.hilt.playground.di


/*
// This will swap AlarmRepository with Fake one
@VisibleForTesting
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AlarmRepositoryModule::class])
object TestAlarmRepository{

    @Singleton
    @Provides
    fun repository() = AlarmRepositoryFake() as AlarmRepository

}
*/
