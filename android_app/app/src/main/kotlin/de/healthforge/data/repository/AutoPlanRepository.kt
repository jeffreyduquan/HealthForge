package de.healthforge.data.repository

import de.healthforge.data.network.AutoPlanApi
import de.healthforge.data.network.AutoPlanGenerateRequest
import de.healthforge.data.network.AutoPlanGenerateResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoPlanRepository @Inject constructor(
    private val api: AutoPlanApi,
) {
    suspend fun generate(req: AutoPlanGenerateRequest): Result<AutoPlanGenerateResponse> = runCatching {
        api.generate(req)
    }
}
