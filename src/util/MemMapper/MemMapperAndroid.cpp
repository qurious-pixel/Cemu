#include "util/MemMapper/MemMapper.h"

#include <unistd.h>
#include <sys/mman.h>
#include <android/sharedmem.h>
#include <fcntl.h>
#include <cstring>
#include <unordered_map>

namespace MemMapper
{
    std::unordered_map<uintptr_t, uintptr_t> g_JitMaps;

    const size_t sPageSize{ []()
        {
            return (size_t)sysconf(_SC_PAGESIZE);
        }()
    };

    size_t GetPageSize() { return sPageSize; }

    int GetProt(PAGE_PERMISSION permissionFlags)
    {
        int p = PROT_NONE;
        if (HAS_FLAG(permissionFlags, PAGE_PERMISSION::P_READ))    p |= PROT_READ;
        if (HAS_FLAG(permissionFlags, PAGE_PERMISSION::P_WRITE))   p |= PROT_WRITE;
        if (HAS_FLAG(permissionFlags, PAGE_PERMISSION::P_EXECUTE)) p |= PROT_EXEC;
        return p;
    }

    void* ReserveMemory(void* baseAddr, size_t size, PAGE_PERMISSION permissionFlags)
    {
        void* ptr = mmap(baseAddr, size, PROT_NONE, MAP_PRIVATE | MAP_ANONYMOUS | MAP_NORESERVE, -1, 0);
        return (ptr == MAP_FAILED) ? nullptr : ptr;
    }

    void FreeReservation(void* baseAddr, size_t size)
    {
        munmap(baseAddr, size);
    }

    void* AllocateMemory(void* baseAddr, size_t size, PAGE_PERMISSION permissionFlags, bool fromReservation)
    {
        bool needsJit = HAS_FLAG(permissionFlags, PAGE_PERMISSION::P_WRITE) && 
                        HAS_FLAG(permissionFlags, PAGE_PERMISSION::P_EXECUTE);

        if (fromReservation)
        {
            uintptr_t addr = reinterpret_cast<uintptr_t>(baseAddr);
            uintptr_t alignedAddr = addr & ~(static_cast<uintptr_t>(sPageSize) - 1);
            int prot = GetProt(permissionFlags);
            
            if (mprotect(reinterpret_cast<void*>(alignedAddr), size, prot) == 0)
                return baseAddr;
            return nullptr;
        }
        else
        {
            int fd = ASharedMemory_create("MemMapper_JIT", size);
            if (fd < 0) return nullptr;

            if (needsJit)
            {
                void* writePtr = mmap(NULL, size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
                void* execPtr = mmap(baseAddr, size, PROT_READ | PROT_EXEC, MAP_SHARED, fd, 0);

                if (writePtr != MAP_FAILED && execPtr != MAP_FAILED) {
                    g_JitMaps[(uintptr_t)execPtr] = (uintptr_t)writePtr;
                    close(fd);
                    return execPtr;
                }
                
                if (writePtr != MAP_FAILED) munmap(writePtr, size);
                if (execPtr != MAP_FAILED) munmap(execPtr, size);
            }
            else
            {
                void* r = mmap(baseAddr, size, GetProt(permissionFlags), MAP_SHARED, fd, 0);
                close(fd);
                return (r == MAP_FAILED) ? nullptr : r;
            }
            close(fd);
            return nullptr;
        }
    }

    void* GetWritePtr(void* execAddr)
    {
        uintptr_t addr = (uintptr_t)execAddr;
        for (auto const& [execBase, writeBase] : g_JitMaps) {
            if (addr >= execBase) { 
                uintptr_t offset = addr - execBase;
                return reinterpret_cast<void*>(writeBase + offset);
            }
        }
        return nullptr;
    }

    bool JitWrite(void* destExecAddr, const void* srcCode, size_t size)
    {
        void* writeAddr = GetWritePtr(destExecAddr);
        if (!writeAddr) return false;

        std::memcpy(writeAddr, srcCode, size);

        char* begin = static_cast<char*>(destExecAddr);
        char* end = begin + size;
        __builtin___clear_cache(begin, end);

        return true;
    }

    void FreeMemory(void* baseAddr, size_t size, bool fromReservation)
    {
        if (!fromReservation) {
            g_JitMaps.erase((uintptr_t)baseAddr);
        }
        munmap(baseAddr, size);
    }
};
