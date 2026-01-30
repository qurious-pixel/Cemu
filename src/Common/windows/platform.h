#pragma once

#ifndef NOMINMAX
#define NOMINMAX
#endif

#include <winsock2.h>
#include <ws2tcpip.h>

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#ifdef __MINGW32__
extern "C++" {
    #include <Windows.h>
}
#else
    #include <Windows.h>
#endif

#define AF_BLUETOOTH AF_BTH
#define BTPROTO_RFCOMM BT_PORT_ANY

class SlimRWLock
{
public:
	SlimRWLock();

	void LockRead();
	void UnlockRead();
	void LockWrite();
	void UnlockWrite();

private:
	/*SRWLOCK*/ void* m_lock;
};

uint32_t GetExceptionError();
