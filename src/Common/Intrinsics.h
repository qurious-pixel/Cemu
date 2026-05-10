#pragma once
#include <cstdint>

#if defined(_MSC_VER)
    #include <intrin.h>
    #if defined(_M_ARM64)
        extern "C" uint64_t UnsignedDivision128(uint64_t high, uint64_t low, uint64_t divisor, uint64_t* remainder);
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
    // Now just call it here
    return UnsignedDivision128(high, low, divisor, remainder);
#else
    #error "128-bit division not supported on this platform"
#endif
}
