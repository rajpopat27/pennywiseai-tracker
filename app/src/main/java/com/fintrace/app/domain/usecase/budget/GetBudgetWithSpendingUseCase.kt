package com.fintrace.app.domain.usecase.budget

import com.fintrace.app.data.repository.BudgetRepository
import com.fintrace.app.data.repository.BudgetWithSpending
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting the current budget with spending information.
 *
 * Returns a Flow that emits the current budget along with
 * how much has been spent this month and the remaining amount.
 */
class GetBudgetWithSpendingUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    /**
     * Get the active budget with current month's spending.
     *
     * @return Flow emitting BudgetWithSpending or null if no budget is set
     */
    operator fun invoke(): Flow<BudgetWithSpending?> {
        return budgetRepository.getBudgetWithSpending()
    }
}
