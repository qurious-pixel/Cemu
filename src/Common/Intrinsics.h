#pragma once
#include <cstdint>

#if defined(_MSC_VER)
    #include <intrin.h>
#endif

static inline uint64_t Multiply64to128(uint64_t a, uint64_t b, uint64_t* high) {
#if defined(__SIZEOF_INT128__) // GCC/Clang (Linux, macOS, Android)
    unsigned __int128 res = (unsigned __int128)a * b;
    *high = (uint64_t)(res >> 64);
    return (uint64_t)res;
#elif defined(_MSC_VER) && defined(_M_X64) // MSVC x64
    return _umul128(a, b, high);
#elif defined(_MSC_VER) && defined(_M_ARM64) // MSVC ARM64
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
    // MSVC ARM64 does not have _udiv128. You must use a software fallback 
    // or the specific helper provided by the Windows SDK:
    extern "C" uint64_t UnsignedDivision128(uint64_t high, uint64_t low, uint64_t divisor, uint64_t * remainder);
    return UnsignedDivision128(high, low, divisor, remainder);
#else
    #error "128-bit division not supported on this platform"
#endif
}
