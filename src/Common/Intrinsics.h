#pragma once
#include <cstdint>

#if defined(_MSC_VER)
    #include <intrin.h>
    #if defined(_M_ARM64)
        #include <arm64intr.h>
        extern "C" uint64_t UnsignedDivision128(uint64_t high, uint64_t low, uint64_t divisor, uint64_t* remainder);
        #define BARRIER_FENCE() __dmb(_ARM64_BARRIER_ISH)
        #define READ_TSC()      _ReadStatusReg(ARM64_CNTVCT_EL0)
    #else
        #include <immintrin.h>
        #define BARRIER_FENCE() _mm_mfence()
        #define READ_TSC()      __rdtsc()
    #endif
#else 
    #if defined(__aarch64__)
        #define BARRIER_FENCE() __asm__ __volatile__ ("dmb ish" : : : "memory")
        static inline uint64_t READ_TSC() {
            uint64_t val;
            __asm__ __volatile__("mrs %0, cntvct_el0" : "=r" (val));
            return val;
        }
    #else
        #include <x86intrin.h>
        #define BARRIER_FENCE() __asm__ __volatile__ ("mfence" : : : "memory")
        #define READ_TSC()      __rdtsc()
    #endif
#endif

static inline uint64_t Multiply64to128(uint64_t a, uint64_t b, uint64_t* high) {
#if defined(__SIZEOF_INT128__)
    unsigned __int128 res = (unsigned __int128)a * b;
    *high = (uint64_t)(res >> 64);
    return (uint64_t)res;
#elif defined(_MSC_VER) && defined(_M_X64)
    return _umul128(a, b, high);
#elif defined(_MSC_VER) && defined(_M_ARM64)
    *high = __umulh(a, b);
    return a * b;
#else
    #error "128-bit multiplication not supported on this platform"
#endif
}

static inline uint64_t Divide128by64(uint64_t high, uint64_t low, uint64_t divisor, uint64_t* remainder) {
#if defined(__SIZEOF_INT128__)
    unsigned __int128 dividend = ((unsigned __int128)high << 64) | low;
    *remainder = (uint64_t)(dividend % divisor);
    return (uint64_t)(dividend / divisor);
#elif defined(_MSC_VER) && defined(_M_X64)
    return _udiv128(high, low, divisor, remainder);
#elif defined(_MSC_VER) && defined(_M_ARM64)
    return UnsignedDivision128(high, low, divisor, remainder);
#else
    #error "128-bit division not supported on this platform"
#endif
}
