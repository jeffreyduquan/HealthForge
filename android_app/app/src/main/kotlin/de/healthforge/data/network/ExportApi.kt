package de.healthforge.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ExportApi {

    /**
     * Lädt die Server-seitige Datenauswertung als JSON oder PDF herunter.
     * Header `Content-Disposition` enthält den vom Server gesetzten Dateinamen.
     */
    @Streaming
    @GET("v1/export/full")
    suspend fun downloadFullExport(
        @Query("format") format: String,
    ): Response<ResponseBody>
}
