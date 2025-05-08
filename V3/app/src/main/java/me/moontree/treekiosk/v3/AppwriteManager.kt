package me.moontree.treekiosk.v3

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases

object AppwriteManager {
    private lateinit var client: Client
    lateinit var account: Account
        private set
    lateinit var database: Databases
        private set

    fun initialize(context: Context) {
        if (!::client.isInitialized) {
            client = Client(context)
                .setEndpoint("https://cloud.appwrite.io/v1")
                .setProject("treekiosk")

            account = Account(client)
            database = Databases(client)
        }
    }
}
