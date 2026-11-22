// ICalculator.aidl — contract shared by server (:app) and client (:ipcclient).
// Keep both copies IDENTICAL (same package, same methods, same order).
package com.example.learning;

interface ICalculator {
    int add(int a, int b);
}
