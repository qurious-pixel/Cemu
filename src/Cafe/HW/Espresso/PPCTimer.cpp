#include "Cafe/HW/Espresso/Const.h"
#include "config/ActiveSettings.h"
#include "util/helpers/fspinlock.h"
#include "util/highresolutiontimer/HighResolutionTimer.h"
#include "Common/cpu_features.h"

// Your platform-agnostic hardware/math wrappers
#include "Common/Intrinsics.h"

#include <chrono>
#include <thread>

// REMOVED: uint128_t struct definition (Moved to Intrinsics.h)
// REMOVED: static_assert(sizeof(uint128_t) == 16); (Moved to Intrinsics.h or handled via header)

uint64 _rdtscLastMeasure = 0;
uint64 _rdtscFrequency = 0;

uint128_t _rdtscAcc{};

uint64 muldiv64(uint64 a, uint64 b, uint64 d)
{
	uint64 diva = a / d;
	uint64 moda = a % d;
	uint64 divb = b / d;
	uint64 modb = b % d;
	return diva * b + moda * divb + moda * modb / d;
}

uint64 PPCTimer_estimateRDTSCFrequency()
{
    #if defined(ARCH_X86_64) || defined(__x86_64__) || defined(_M_X64)
	if (!g_CPUFeatures.x86.invariant_tsc)
		cemuLog_log(LogType::Force, "Invariant TSC not supported");
    #endif

	BARRIER_FENCE();
	uint64 tscStart = READ_TSC();
	
	// Cross-platform steady sleep instead of Windows-only GetTickCount()
	auto startTime = std::chrono::steady_clock::now();
	HRTick startTick = HighResolutionTimer::now().getTick();
	
	while (true)
	{
		auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(
			std::chrono::steady_clock::now() - startTime).count();
		if (elapsed >= 3000)
			break;
		std::this_thread::sleep_for(std::chrono::milliseconds(10));
	}
	
	BARRIER_FENCE();
	HRTick stopTick = HighResolutionTimer::now().getTick();
	uint64 tscEnd = READ_TSC();
	
	uint64 tsc_diff = tscEnd - tscStart;
	uint64 hrtFreq = 0;
	uint64 hrtDiff = HighResolutionTimer::getTimeDiffEx(startTick, stopTick, hrtFreq);
	uint64 tsc_freq = muldiv64(tsc_diff, hrtFreq, hrtDiff);

	return tsc_freq;
}

int PPCTimer_initThread()
{
	_rdtscFrequency = PPCTimer_estimateRDTSCFrequency();
	return 0;
}

void PPCTimer_init()
{
	std::thread t(PPCTimer_initThread);
	t.detach();
	_rdtscLastMeasure = READ_TSC();
}

uint64 _tickSummary = 0;

void PPCTimer_start()
{
	_rdtscLastMeasure = READ_TSC();
	_tickSummary = 0;
}

uint64 PPCTimer_getRawTsc()
{
	return READ_TSC();
}

uint64 PPCTimer_microsecondsToTsc(uint64 us)
{
	return (us * _rdtscFrequency) / 1000000ULL;
}

uint64 PPCTimer_tscToMicroseconds(uint64 us)
{
	uint128_t r{};
	// FIXED: Properly passing parameters to portable_umul128
	r.low = portable_umul128(us, 1000000ULL, &r.high);

	uint64 remainder;
	// FIXED: Properly passing parameters to portable_udiv128
	const uint64 microseconds = portable_udiv128(r.high, r.low, _rdtscFrequency, &remainder);

	return microseconds;
}

bool PPCTimer_isReady()
{
	return _rdtscFrequency != 0;
}

void PPCTimer_waitForInit()
{
	while (!PPCTimer_isReady()) std::this_thread::sleep_for(std::chrono::milliseconds(10));
}

FSpinlock sTimerSpinlock;

// thread safe
uint64 PPCTimer_getFromRDTSC()
{
	sTimerSpinlock.lock();
	BARRIER_FENCE();
	uint64 rdtscCurrentMeasure = READ_TSC();
	uint64 rdtscDif = rdtscCurrentMeasure - _rdtscLastMeasure;
	
	// optimized max(rdtscDif, 0) without conditionals
	rdtscDif = rdtscDif & ~(uint64)((sint64)rdtscDif >> 63);

	uint128_t diff{};
	// FIXED: Passing proper arguments to portable_umul128
	diff.low = portable_umul128(rdtscDif, Espresso::CORE_CLOCK, &diff.high);

	if(rdtscCurrentMeasure > _rdtscLastMeasure)
		_rdtscLastMeasure = rdtscCurrentMeasure; // only travel forward in time

	// Portable 128-bit addition with carry replacing Windows-only _addcarry_u64 inline
	uint64 old_low = _rdtscAcc.low;
	_rdtscAcc.low += diff.low;
	uint64 carry = (_rdtscAcc.low < old_low) ? 1 : 0;
	_rdtscAcc.high += diff.high + carry;

	uint64 remainder;
	// FIXED: Passing proper arguments to portable_udiv128
	uint64 elapsedTick = portable_udiv128(_rdtscAcc.high, _rdtscAcc.low, _rdtscFrequency, &remainder);

	_rdtscAcc.low = remainder;
	_rdtscAcc.high = 0;

	// timer scaling
	elapsedTick <<= 3ull; // *8
	uint8 timerShiftFactor = ActiveSettings::GetTimerShiftFactor();
	elapsedTick >>= timerShiftFactor;

	_tickSummary += elapsedTick;

	sTimerSpinlock.unlock();
	return _tickSummary;
}
