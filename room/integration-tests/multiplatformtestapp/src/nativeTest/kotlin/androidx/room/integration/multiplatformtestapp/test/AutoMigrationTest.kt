/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.integration.multiplatformtestapp.test

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import platform.posix.remove

class AutoMigrationTest : BaseAutoMigrationTest() {
    private val filename = "/tmp/test-${Random.nextInt()}.db"
    private val driver: SQLiteDriver = BundledSQLiteDriver()

    private val dbFactory = { AutoMigrationDatabase::class.instantiateImpl() }

    private val migrationTestHelper =
        MigrationTestHelper(
            schemaDirectoryPath = getSchemaDirectoryPath(),
            fileName = filename,
            driver = driver,
            databaseClass = AutoMigrationDatabase::class,
            databaseFactory = dbFactory,
            autoMigrationSpecs = listOf(ProvidedSpecFrom2To3())
        )

    override fun getTestHelper() = migrationTestHelper

    override fun getDatabaseBuilder(): RoomDatabase.Builder<AutoMigrationDatabase> {
        return Room.databaseBuilder<AutoMigrationDatabase>(filename, dbFactory).setDriver(driver)
    }

    @BeforeTest
    fun before() {
        deleteDatabaseFile()
    }

    @AfterTest
    fun after() {
        migrationTestHelper.finished()
        deleteDatabaseFile()
    }

    private fun deleteDatabaseFile() {
        remove(filename)
        remove("$filename-wal")
        remove("$filename-shm")
    }
}
