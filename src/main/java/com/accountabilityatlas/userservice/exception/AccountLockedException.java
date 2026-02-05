package com.accountabilityatlas.userservice.exception;

public class AccountLockedException extends RuntimeException {
  public AccountLockedException() {
    super("Account is temporarily locked due to too many failed attempts");
  }
}
