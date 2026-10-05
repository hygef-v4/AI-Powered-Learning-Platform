package edu.aiplatform.billing.port;

public record CreditBalance(long freeBalance, String freePeriod, long purchasedBalance) {}
