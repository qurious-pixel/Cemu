#pragma once

#include <cstdint>

// Standard types (Only keep these if they aren't globally defined elsewhere)
using uint64 = uint64_t;
using sint64 = int64_t;
using uint8  = uint8_t;

// ============================================================================
// 1. Hardware Fence and Timestamp Counter Abstraction
// ============================================================================
#if defined(_M_X64) || defined(__x86_64__)
    #if defined(_MSC_VER)
        #include <immintrin.h>
        #pragma intrinsic(__rdtsc)
        #define BARRIER_FENCE() _mm_mfence()
        #define READ_TSC()      __rdtsc()
    #else // GCC / Clang
        #include <x86intrin.h>
        #define BARRIER_FENCE() __builtin_ia32_mfence()
        #define READ_TSC()      __rdtsc()
    #endif
#elif defined(_M_ARM64) || defined(__aarch64__)
    #if defined(_MSC_VER)
        #include <intrin.h>
        #define BARRIER_FENCE() __dmb(_ARM64_BARRIER_SY)
        #define READ_TSC()      _ReadStatusReg(ARM64_CNTVCT_EL0)
    #else // GCC / Clang on Linux/Mac ARM64
        #define BARRIER_FENCE() __asm__ __volatile__("dmb sy" : : : "memory")
        inline uint64 READ_TSC() {
            uint64 virtual_timer;
            __asm__ __volatile__("mrs %0, cntvct_el0" : "=r" (virtual_timer));
            return virtual_timer;
        }
    #endif
#endif

// ============================================================================
// 2. 128-bit Math Abstraction (Windows vs. Linux/Mac)
// ============================================================================
struct uint128_t {
    uint64 low;
    uint64 high;
};

// Portable 128-bit Multiply
inline uint64 portable_umul128(uint64 multiplier, uint64 multiplicand, uint64* high) {
#if defined(_MSC_VER)
    #if defined(_M_X64)
        return _umul128(multiplier, multiplicand, high);
    #elif defined(_M_ARM64)
        // MSVC ARM64 alternative: __umulh returns upper 64-bits
        *high = __umulh(multiplier, multiplicand);
        return multiplier * multiplicand;
    #endif
#else
    // Linux/Mac GCC/Clang native 128-bit integer extension
    unsigned __int128 res = (unsigned __int128)multiplier * multiplicand;
    *high = (uint64)(res >> 64);
    return (uint64)res;
#endif
}

// Portable 128-bit Division
inline uint64 portable_udiv128(uint64 high, uint64 low, uint64 denominator, uint64* remainder) {
#if defined(_MSC_VER)
    #if defined(_M_X64)
        return _udiv128(high, low, denominator, remainder);
    #else
        // Software bit-shift long division fallback for MSVC ARM64
        if (high == 0) {
            if (remainder) *remainder = low % denominator;
            return low / denominator;
        }
        
        uint64 rem = 0;
        uint64 quot = 0;
        for (int i = 63; i >= 0; i--) {
            rem = (rem << 1) | (high >> 63);
            high <<= 1;
            if (rem >= denominator) {
                rem -= denominator;
                quot |= (1ULL << i);
            }
        }
        for (int i = 63; i >= 0; i--) {
            rem = (rem << 1) | (low >> 63);
            low <<= 1;
            if (rem >= denominator) {
                rem -= denominator;
                quot |= (1ULL << i); // Simplified demonstration logic
            }
        }
        // Secure native software fallback block
        uint64_t q = 0;
        uint64_t r = 0;
        for (int i = 127; i >= 0; i--) {
            r = (r << 1) | ((i >= 64 ? high >> (i - 64) : low >> i) & 1);
            if (r >= denominator) {
                r -= denominator;
                q |= (1ULL << (i % 64)); // Note: Standard compiler intrinsic replacement approach
            }
        }
        if (remainder) *remainder = r;
        return q;
    #endif
#else
    // Linux/Mac GCC/Clang native 128-bit division
    unsigned __int128 dividend = ((unsigned __int128)high << 64) | low;
    if (remainder) *remainder = (uint64)(dividend % denominator);
    return (uint64)(dividend / denominator);
#endif
}
