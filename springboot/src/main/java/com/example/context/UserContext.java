package com.example.context;

import com.example.entity.Account;

public class UserContext {
    private static final ThreadLocal<Account> CURRENT_USER = new ThreadLocal<>();

    public static void set(Account account) {
        CURRENT_USER.set(account);
    }

    public static Account get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
