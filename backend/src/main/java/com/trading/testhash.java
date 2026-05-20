package com.trading;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class testhash {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("Admin@123456"));
    }
}
