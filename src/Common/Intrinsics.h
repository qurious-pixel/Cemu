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
        #define PLATFORM_MFENCE() _mm_mfence()
        #define PLATFORM_RDTSC()  __rdtsc()
    #else // GCC / Clang
        #include <x86intrin.h>
        #define PLATFORM_MFENCE() __builtin_ia32_mfence()
        #define PLATFORM_RDTSC()  __rdtsc()
    #endif
#elif defined(_M_ARM64) || defined(__aarch64__)
    #if defined(_MSC_VER)
        // FIX: MSVC puts ARM64 system register intrinsics inside <intrin.h>
        #include <intrin.h>
        #define PLATFORM_MFENCE() __dmb(_ARM64_BARRIER_SY)
        #define PLATFORM_RDTSC()  _ReadStatusReg(ARM64_CNTVCT_EL0)
    #else // GCC / Clang on Linux/Mac ARM64
        #define PLATFORM_MFENCE() __asm__ __volatile__("dmb sy" : : : "memory")
        inline uint64 PLATFORM_RDTSC() {
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

// Portable 128-bit Multiply and Divide
inline uint64 portable_umul128(uint64 multiplier, uint64 multiplicand, uint64* high) {
#if defined(_MSC_VER)
    // MSVC provides this on x64 and ARM64 via <intrin.h>
    return _umul128(multiplier, multiplicand, high);
#else
    // Linux/Mac GCC/Clang native 128-bit integer extension
    unsigned __int128 res = (unsigned __int128)multiplier * multiplicand;
    *high = (uint64)(res >> 64);
    return (uint64)res;
#endif
}

inline uint64 portable_udiv128(uint64 high, uint64 low, uint64 denominator, uint64* remainder) {
#if defined(_MSC_VER) && (defined(_M_X64) || defined(__x86_64__))
    return _udiv128(high, low, denominator, remainder);
#elif defined(_MSC_VER) && defined(_M_ARM64)
    // Software fallback for Windows on ARM (MSVC lacks _udiv128)
    if (high == 0) {
        if (remainder) *remainder = low % denominator;
        return low / denominator;
    }
    unsigned __int128 dividend = ((unsigned __int128)high << 64) | low;
    if (remainder) *remainder = (uint64)(dividend % denominator);
    return (uint64)(dividend / denominator);
#else
    // Linux/Mac GCC/Clang native 128-bit division
    unsigned __int128 dividend = ((unsigned __int128)high << 64) | low;
    if (remainder) *remainder = (uint64)(dividend % denominator);
    return (uint64)(dividend / denominator);
#endif
}
