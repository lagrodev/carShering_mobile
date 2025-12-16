package com.example.carcatalogue.data.repository

import com.example.carcatalogue.data.api.ApiService
import com.example.carcatalogue.data.model.*
import retrofit2.Response

class ContractRepository(private val apiService: ApiService) {
    
    suspend fun getUserContracts(page: Int = 0, size: Int = 20): Response<PagedModel<ContractResponse>> {
        return apiService.getAllContracts(page, size)
    }
    
    suspend fun getContract(contractId: Long): Response<ContractResponse> {
        return apiService.getContract(contractId)
    }
    
    suspend fun createContract(
        carId: Long,
        startDate: String,
        endDate: String,
        dailyRate: Double
    ): Response<ContractResponse> {
        return apiService.createContract(CreateContractRequest(carId, startDate, endDate, dailyRate))
    }
    
    suspend fun updateContract(
        contractId: Long,
        startDate: String,
        endDate: String,
        dailyRate: Double? = null
    ): Response<ContractResponse> {
        return apiService.updateContract(contractId, UpdateContractRequest(startDate, endDate, dailyRate))
    }
    
    suspend fun cancelContract(contractId: Long): Response<Unit> {
        return apiService.cancelContract(contractId)
    }
}
