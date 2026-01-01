#pragma once

#include "Cafe/HW/Latte/Renderer/Renderer.h"
#include "Cafe/HW/Latte/Renderer/Vulkan/VulkanAPI.h"
#include "util/containers/flat_hash_map.hpp"
#include "util/helpers/MemoryPool.h"

// VMA Header
#include "vk_mem_alloc.h"

#include <queue>
#include <vector>

enum class VKR_BUFFER_TYPE
{
    STAGING,  // CPU to GPU transfers
    INDEX,    // Geometry indices
    STRIDE,   // Vertex data realignment
};

struct VkImageMemAllocation
{
    VmaAllocation vmaAllocation;
    uint32 allocationSize;

    VkImageMemAllocation(VmaAllocation _vmaAlloc, uint32 _size) 
        : vmaAllocation(_vmaAlloc), allocationSize(_size) 
    {}
};

class VKRSynchronizedRingAllocator
{
public:
    struct BufferSyncPoint_t
    {
        uint64 commandBufferId;
        uint32 offset;
        BufferSyncPoint_t(uint64 _id, uint32 _off) : commandBufferId(_id), offset(_off) {};
    };

    struct AllocatorBuffer_t
    {
        VkBuffer vk_buffer;
        VmaAllocation vmaAllocation;
        VmaAllocationInfo vmaInfo; 
        uint32 size;
        uint32 writeIndex;
        bool requiresFlush;        
        
        std::queue<BufferSyncPoint_t> queue_syncPoints;
        uint64 lastSyncpointCmdBufferId{ 0xFFFFFFFFFFFFFFFFull };
    };

    struct AllocatorReservation_t
    {
        VkBuffer vkBuffer;
        VmaAllocation vmaAllocation;
        uint8* memPtr;
        uint32 bufferOffset;
        uint32 size;
        uint32 bufferIndex;
    };

    VKRSynchronizedRingAllocator(class VulkanRenderer* vkRenderer, class VKRMemoryManager* vkMemoryManager, VKR_BUFFER_TYPE bufferType, uint32 minBufferSize);
    ~VKRSynchronizedRingAllocator();

    // NEW: Initialization method to be called after VMA is ready
    void Init();

    AllocatorReservation_t AllocateBufferMemory(uint32 size, uint32 alignment);
    void CleanupBuffer(uint64 latestFinishedCommandBufferId);
    void GetStats(uint32& numBuffers, size_t& totalBufferSize, size_t& freeBufferSize) const;
    void Cleanup(uint64 fenceValue);

private:
    void allocateAdditionalUploadBuffer(uint32 sizeRequiredForAlloc);

    const class VulkanRenderer* m_vkr;
    class VKRMemoryManager* m_vkrMemMgr;
    const VKR_BUFFER_TYPE m_bufferType;
    const uint32 m_minimumBufferAllocSize;
    std::vector<AllocatorBuffer_t> m_buffers;
};

class VKRMemoryManager
{
public:
    VKRMemoryManager(class VulkanRenderer* renderer);
    ~VKRMemoryManager();
    
    bool Start();
    bool InitializeVMA();
    void ShutdownVMA();
    VmaAllocator GetVmaAllocator() const { return m_vmaAllocator; }

    bool CreateBuffer(VkDeviceSize size, VkBufferUsageFlags usage, VmaMemoryUsage vmaUsage, VkBuffer& buffer, VmaAllocation& allocation) const;
    void DeleteBuffer(VkBuffer& buffer, VmaAllocation& allocation) const;

    VkImageMemAllocation* imageMemoryAllocate(VkImage image);
    void imageMemoryFree(VkImage image, VkImageMemAllocation* allocation);

    void* TextureUploadBufferAcquire(uint32 size);
    void TextureUploadBufferRelease(uint8* mem);

    VKRSynchronizedRingAllocator& getStagingAllocator() { return m_stagingBuffer; }
    VKRSynchronizedRingAllocator& getVertexStrideAllocator() { return m_vertexStrideBuffer; }

    void cleanupBuffers(uint64 latestFinishedCommandBufferId);

private:
    class VulkanRenderer* m_vkr;
    VmaAllocator m_vmaAllocator{ VK_NULL_HANDLE };

    VKRSynchronizedRingAllocator m_stagingBuffer;
    VKRSynchronizedRingAllocator m_vertexStrideBuffer;
};
