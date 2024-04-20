package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import org.springframework.stereotype.Service;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException {
        verifyInputs(personalCode, loanAmount, loanPeriod);

        int outputLoanAmount;
        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }

        int highestAmount = highestValidLoanAmount(loanPeriod);

        while (highestAmount < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT && loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            loanPeriod++;
            highestAmount = highestValidLoanAmount(loanPeriod);
        }

        // If we did not find a valid loan amount, throw corresponding exception
        if(highestAmount < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT)
            throw new NoValidLoanException("No valid loan found!");

        outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(loanPeriod));

        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int loanPeriod) {
        int high = DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT;
        int low = DecisionEngineConstants.MINIMUM_LOAN_AMOUNT;
        double creditScore;

        // Binary search on sorted loan amounts
        int result = 0;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            creditScore = calculateCreditScore(mid, loanPeriod);

            if (creditScore >= 1) {
                result = mid;
                low = mid + 1; // Search higher
            } else high = mid - 1; // Search lower
        }
        return result;
    }

    /**
     * Calculates the credit score given the loanAmount, loanPeriod and creditModifier
     *
     * @return calculated credit score
     */
    private double calculateCreditScore(int loanAmount, int loanPeriod){
        return ((double)creditModifier/loanAmount)*loanPeriod;
    }


    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = extractLastFourDigits(personalCode);

        if (segment < 2500)
            return 0;
        if (segment < 5000)
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        if (segment < 7500)
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    private int extractLastFourDigits(String personalCode) {
        return Integer.parseInt(personalCode.substring(personalCode.length() - 4));
    }


    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException {

        if (!validator.isValid(personalCode))
            throw new InvalidPersonalCodeException("Invalid personal ID code!");

        if (!isLoanAmountValid(loanAmount))
            throw new InvalidLoanAmountException("Invalid loan amount!");

        if (!isLoanPeriodValid(loanPeriod))
            throw new InvalidLoanPeriodException("Invalid loan period!");
    }

    private boolean isLoanAmountValid(Long loanAmount) {
        return DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount
                && loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT;
    }

    private boolean isLoanPeriodValid(int loanPeriod) {
        return DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod
                && loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD;
    }
}
