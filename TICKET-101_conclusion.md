# Ticket 101 conclusion

**Note:** I was not sure, if we had to correct the code the intern provided or not, so I decided to do so just in case. References to old code snippets along with the changed code examples can be found in this file.

## Table of Contents

- [Feedback for intern](#feedback-for-intern)
  - [What went well?](#what-went-well-)
  - [Places for improvement](#places-for-improvement-)
    - [1. Catching exceptions in `calculateApprovedLoan()`](#1-catching-exceptions-in-calculateapprovedloan-method)
    - [2. Unnecessary looping in `calculateApprovedLoan()`](#2-can-break-out-of-while-loop-sooner-in-calculateapprovedloan-method)
    - [3. Checking loanAmount and loanPeriod In the `verifyInputs()`](#3-checking-loanamount-and-loanperiod-in-the-verifyinputs-method)
    - [4. Implementation of `getCreditModifier()`](#4-implementation-of-getcreditmodifier-method)
- [Most important shortcoming of TICKET-101](#most-important-shortcoming-of-ticket-101)

# Feedback for intern

## What went well? 🙂

1. **Modular Design:** The class is well-structured and modular, with methods encapsulating specific functionalities. 

2. **Error Handling:** The class handles various potential errors gracefully by throwing custom exceptions (InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, NoValidLoanException) with meaningful error messages. 

3. **Use of Constants:** Constants such as DecisionEngineConstants.MINIMUM_LOAN_AMOUNT and DecisionEngineConstants.MAXIMUM_LOAN_PERIOD are used appropriately, allowing for easier maintenance.

4. **Documentation:** The class and its methods are well-documented with Javadoc comments, providing clear explanations of their purpose, parameters and return values.
## Places for improvement 🛠️


### 1. Catching exceptions in  `calculateApprovedLoan()` method.

In the current code we are catching the exceptions thrown by the `verifyInputs()` method and returning a new Decision instance with the error message. This is unnecessary, since the same exceptions are already being thrown by the `calculateApprovedLoan()` method and being caught and handled in the controller. We can simply remove the try-catch statement.
### Previous Code
```java
try {
    verifyInputs(personalCode, loanAmount, loanPeriod);
} catch (Exception e) {
    return new Decision(null, null, e.getMessage());
}
```

### Improved Code
```java
verifyInputs(personalCode, loanAmount, loanPeriod);
```

### Explanation

When we do not catch the exceptions in the `calculateApprovedLoan()` method, since they still get caught in the controller, that already handles them as needed. No need to handle these exceptions here as well.

<br>

### 2. Can break out of while loop sooner in `calculateApprovedLoan()` method.

In the previous code we stayed in the while loop until we found a loan amount that was higher than our MIN_LOAN_AMOUNT even if that meant incrementing the loanPeriod out of acceptable range. In the improved version, we stop the search if loanPeriod goes out of range. 

### Previous Code
```java
while (highestValidLoanAmount(loanPeriod) < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
    loanPeriod++;
}

if (loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
    outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(loanPeriod));
} else {
    throw new NoValidLoanException("No valid loan found!");
}

```

### Improved Code
```java
int highestAmount = highestValidLoanAmount(loanPeriod);

while (highestAmount < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT && loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
    loanPeriod++;
    highestAmount = highestValidLoanAmount(loanPeriod);
}

// If we did not find a valid loan amount, throw corresponding exception
if(highestAmount < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) 
        throw new NoValidLoanException("No valid loan found!"); 

outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(loanPeriod));
```

<br>

### 3. Checking loanAmount and loanPeriod In the ```verifyInputs()``` method:

The current code for checking if the `loanAmount` and `loanPeriod` are between the minimum and maximum values is unnecessarily complex and could be simplified for better readability and maintainability.

### Previous Code
```java
if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
    || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
    throw new InvalidLoanAmountException("Invalid loan amount!");
}
if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
    || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
    throw new InvalidLoanPeriodException("Invalid loan period!");
}
```

### Improved Code
```java
if (!isLoanAmountValid(loanAmount)) 
    throw new InvalidLoanAmountException("Invalid loan amount!");

if (!isLoanPeriodValid(loanPeriod)) 
    throw new InvalidLoanPeriodException("Invalid loan period!");
```

### Explanation
By extracting the validation logic into separate methods, we make the code more readable and maintainable. It also aligns better with the Single Responsibility Principle (SRP), as each method is responsible for a single task.

```java
private boolean isLoanAmountValid(Long loanAmount) {
    return DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount
            && loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT;
}

private boolean isLoanPeriodValid(int loanPeriod) {
    return DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod
            && loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD;
}
```

<br>

### 4. Implementation of `getCreditModifier()` Method

The current implementation of the method uses complex conditional logic that can be simplified for better readability. Also, the extraction of the last 4 digits can be extracted to a separate method. 

### Previous Code
```java
private int getCreditModifier(String personalCode) {
    int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

    if (segment < 2500) {
        return 0;
    } else if (segment < 5000) {
        return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
    } else if (segment < 7500) {
        return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
    }

    return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
}
```

### Improved Code
```java
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
```

### Explanation
By breaking down the logic into two methods, `getCreditModifier()` and `extractLastFourDigits()`, we improve readability and maintainability. The `getCreditModifier()` method now focuses solely on determining the credit modifier based on the extracted segment, while the `extractLastFourDigits()` method isolates the logic for obtaining the last four digits of the personal code. Removing `else if` statements simplifies the code flow and makes it easier to follow.

<br>

## Most important shortcoming of TICKET-101

The most important shortcoming of the TICKET-101 implementation is that the `highestValidLoanAmount()` calculates the highest valid load amount incorrectly. The method does not take into consideration the calculated credit score of the user. The highest valid loan amount should be checked by the `credit score = (credit modifier / loan amount) * loan period` and only be approved when the result returned is `>= 1`.

### The fix
To fix this, we could calculate the credit score in a loop starting from MAXIMUM loan amount 10_000 and decrement it by 1 at each iteration. We should break out of the loop, when we find a loan amount that would be approved.

### The optimized fix -  O(log n)
To improve the time complexity of this fix, we could find the max valid load amount using binary search in range of MIN_LOAN_AMOUNT to MAX_LOAN_AMOUNT. This would give us O(log n) time complexity, compared to using a for loop that has O(n) time complexity. The binary search would return the highest valid loan amount that is approved for current loan period. If the search finds aa approved amount, it updates the result to be the current highest amount and continues the search towards higher values.

### Previous Code
```java
private int highestValidLoanAmount(int loanPeriod) {
  return creditModifier * loanPeriod;
}
```

### Improved Optimized Code
```java

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


private double calculateCreditScore(int loanAmount, int loanPeriod){
  return (double)(creditModifier/loanAmount)*loanPeriod;
}
```
