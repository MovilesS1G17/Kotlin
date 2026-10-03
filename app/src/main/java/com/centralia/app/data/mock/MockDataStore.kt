package com.centralia.app.data.mock

import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json


class MockDataStore(rootDirectory: File) {


    sealed class StoreException(message: String) : Exception(message) {
        class UnableToCreateDirectory(cause: Throwable) : StoreException(
            "Centralia could not prepare local storage: ${cause.localizedMessage ?: cause}"
        )

        class UnableToRead(cause: Throwable) : StoreException(
            "Centralia could not read the local library: ${cause.localizedMessage ?: cause}"
        )

        class UnableToDecode(cause: Throwable) : StoreException(
            "Centralia could not understand the local library: ${cause.localizedMessage ?: cause}"
        )

        class UnableToEncode(cause: Throwable) : StoreException(
            "Centralia could not prepare the library for saving: ${cause.localizedMessage ?: cause}"
        )

        class UnableToWrite(cause: Throwable) : StoreException(
            "Centralia could not save the local library: ${cause.localizedMessage ?: cause}"
        )
    }

    private val directory: File = File(File(rootDirectory, "Centralia"), "MockData")
    private val mutex = Mutex()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }


    suspend fun <T> load(serializer: KSerializer<T>, filename: String, seed: () -> T): T =
        mutex.withLock { loadLocked(serializer, filename, seed) }


    suspend fun <T> save(serializer: KSerializer<T>, value: T, filename: String) {
        mutex.withLock { saveLocked(serializer, value, filename) }
    }


    suspend fun <T> mutate(
        serializer: KSerializer<T>,
        filename: String,
        seed: () -> T,
        transform: (T) -> T
    ): T = mutex.withLock {
        val current = loadLocked(serializer, filename, seed)
        val updated = transform(current)
        saveLocked(serializer, updated, filename)
        updated
    }

    private suspend fun <T> loadLocked(
        serializer: KSerializer<T>,
        filename: String,
        seed: () -> T
    ): T = withContext(Dispatchers.IO) {
        prepareDirectory()
        val file = File(directory, filename)

        if (!file.exists()) {
            val initialValue = seed()
            writeFile(serializer, initialValue, file)
            return@withContext initialValue
        }

        val text = try {
            file.readText()
        } catch (error: IOException) {
            throw StoreException.UnableToRead(error)
        }

        try {
            json.decodeFromString(serializer, text)
        } catch (error: Exception) {
            throw StoreException.UnableToDecode(error)
        }
    }

    private suspend fun <T> saveLocked(
        serializer: KSerializer<T>,
        value: T,
        filename: String
    ) = withContext(Dispatchers.IO) {
        prepareDirectory()
        writeFile(serializer, value, File(directory, filename))
    }

    private fun <T> writeFile(serializer: KSerializer<T>, value: T, file: File) {
        val text = try {
            json.encodeToString(serializer, value)
        } catch (error: Exception) {
            throw StoreException.UnableToEncode(error)
        }

        try {

            val temporaryFile = File(file.parentFile, "${file.name}.tmp")
            temporaryFile.writeText(text)
            if (file.exists() && !file.delete()) {
                throw IOException("Could not replace ${file.name}")
            }
            if (!temporaryFile.renameTo(file)) {
                throw IOException("Could not move ${temporaryFile.name} into place")
            }
        } catch (error: Exception) {
            throw StoreException.UnableToWrite(error)
        }
    }

    private fun prepareDirectory() {
        try {
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Could not create ${directory.absolutePath}")
            }
        } catch (error: Exception) {
            throw StoreException.UnableToCreateDirectory(error)
        }
    }
}
