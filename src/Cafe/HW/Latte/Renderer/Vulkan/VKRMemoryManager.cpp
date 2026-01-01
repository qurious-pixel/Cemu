#include "Cafe/HW/Latte/Renderer/Vulkan/VKRMemoryManager.h"
#include "Cafe/HW/Latte/Renderer/Vulkan/VulkanRenderer.h"
#define VMA_IMPLEMENTATION
#define VMA_STATIC_VULKAN_FUNCTIONS 0
#define VMA_DYNAMIC_VULKAN_FUNCTIONS 0 
#include "vk_mem_alloc.h"
#include "VulkanAPI.h"

#include <algorithm>

// --- VKRMemoryManager Implementation ---

VKRMemoryManager::VKRMemoryManager(VulkanRenderer* renderer) 
    : m_vkr(renderer),
      m_stagingBuffer(renderer, this, VKR_BUFFER_TYPE::STAGING, 1024 * 1024 * 4), 
      m_vertexStrideBuffer(renderer, this, VKR_BUFFER_TYPE::STRIDE, 1024 * 1024 * 2) 
{
    // Constructor is now "Passive"
    // We do NOT call InitializeVMA() or Init() here to avoid Signal 11
}

VKRMemoryManager::~VKRMemoryManager() {
    ShutdownVMA();
}

bool VKRMemoryManager::Start() {
    // 1. Initialize VMA with safety checks
    if (!InitializeVMA()) {
        return false;
    }

    // 2. Initialize the sub-allocators now that VMA handle is valid
    m_stagingBuffer.Init();
    m_vertexStrideBuffer.Init();
    
    return true;
}

bool VKRMemoryManager::InitializeVMA() {
    if (m_vmaAllocator != VK_NULL_HANDLE) return true;

    VkInstance instance = m_vkr->GetVkInstance();
    VkDevice device = m_vkr->GetLogicalDevice();
    VkPhysicalDevice physicalDevice = m_vkr->GetPhysicalDevice(); // Get the handle

    // --- ADD THE LOGGING CODE HERE ---
    VkPhysicalDeviceMemoryProperties memProperties;
    vkGetPhysicalDeviceMemoryProperties(physicalDevice, &memProperties);

    cemuLog_log(LogType::Force, "--- Shield TV Memory Diagnostic ---");
    for (uint32_t i = 0; i < memProperties.memoryTypeCount; i++) {
        cemuLog_log(LogType::Force, "Type [{}]: HeapIndex {}, Flags: {}", 
            i, 
            memProperties.memoryTypes[i].heapIndex,
            (int)memProperties.memoryTypes[i].propertyFlags);
    }

    VmaVulkanFunctions vulkanFunctions = {};
    // Instance functions
    vulkanFunctions.vkGetInstanceProcAddr = (PFN_vkGetInstanceProcAddr)vkGetInstanceProcAddr;
    vulkanFunctions.vkGetDeviceProcAddr = (PFN_vkGetDeviceProcAddr)vkGetDeviceProcAddr;
    
    // Physical Device functions (Fetched via Instance)
    vulkanFunctions.vkGetPhysicalDeviceProperties = (PFN_vkGetPhysicalDeviceProperties)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceProperties");
    vulkanFunctions.vkGetPhysicalDeviceMemoryProperties = (PFN_vkGetPhysicalDeviceMemoryProperties)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceMemoryProperties");
    
    // Device functions (Fetched via Device)
    auto GetDev = [&](const char* name) { return vkGetDeviceProcAddr(device, name); };

    vulkanFunctions.vkAllocateMemory = (PFN_vkAllocateMemory)GetDev("vkAllocateMemory");
    vulkanFunctions.vkFreeMemory = (PFN_vkFreeMemory)GetDev("vkFreeMemory");
    vulkanFunctions.vkMapMemory = (PFN_vkMapMemory)GetDev("vkMapMemory");
    vulkanFunctions.vkUnmapMemory = (PFN_vkUnmapMemory)GetDev("vkUnmapMemory");
    vulkanFunctions.vkFlushMappedMemoryRanges = (PFN_vkFlushMappedMemoryRanges)GetDev("vkFlushMappedMemoryRanges");
    vulkanFunctions.vkInvalidateMappedMemoryRanges = (PFN_vkInvalidateMappedMemoryRanges)GetDev("vkInvalidateMappedMemoryRanges");
    vulkanFunctions.vkBindBufferMemory = (PFN_vkBindBufferMemory)GetDev("vkBindBufferMemory");
    vulkanFunctions.vkBindImageMemory = (PFN_vkBindImageMemory)GetDev("vkBindImageMemory");
    vulkanFunctions.vkGetBufferMemoryRequirements = (PFN_vkGetBufferMemoryRequirements)GetDev("vkGetBufferMemoryRequirements");
    vulkanFunctions.vkGetImageMemoryRequirements = (PFN_vkGetImageMemoryRequirements)GetDev("vkGetImageMemoryRequirements");
    vulkanFunctions.vkCreateBuffer = (PFN_vkCreateBuffer)GetDev("vkCreateBuffer");
    vulkanFunctions.vkDestroyBuffer = (PFN_vkDestroyBuffer)GetDev("vkDestroyBuffer");
    vulkanFunctions.vkCreateImage = (PFN_vkCreateImage)GetDev("vkCreateImage");
    vulkanFunctions.vkDestroyImage = (PFN_vkDestroyImage)GetDev("vkDestroyImage");
    vulkanFunctions.vkCmdCopyBuffer = (PFN_vkCmdCopyBuffer)GetDev("vkCmdCopyBuffer");
    vulkanFunctions.vkGetPhysicalDeviceMemoryProperties2KHR = (PFN_vkGetPhysicalDeviceMemoryProperties2KHR)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceMemoryProperties2");
	vulkanFunctions.vkGetBufferMemoryRequirements2KHR = (PFN_vkGetBufferMemoryRequirements2KHR)GetDev("vkGetBufferMemoryRequirements2");
	vulkanFunctions.vkGetImageMemoryRequirements2KHR = (PFN_vkGetImageMemoryRequirements2KHR)GetDev("vkGetImageMemoryRequirements2");
	vulkanFunctions.vkBindBufferMemory2KHR = (PFN_vkBindBufferMemory2KHR)GetDev("vkBindBufferMemory2");
	vulkanFunctions.vkBindImageMemory2KHR = (PFN_vkBindImageMemory2KHR)GetDev("vkBindImageMemory2");
	
    VmaAllocatorCreateInfo allocatorInfo = {};
    allocatorInfo.vulkanApiVersion = VK_API_VERSION_1_1;
    allocatorInfo.physicalDevice = m_vkr->GetPhysicalDevice();
    allocatorInfo.device = device;
    allocatorInfo.instance = instance;
    allocatorInfo.pVulkanFunctions = &vulkanFunctions;

    // Safety check: if CreateBuffer is null, the loader is still not initialized correctly
    if (!vulkanFunctions.vkCreateBuffer) {
        cemuLog_log(LogType::Force, "VMA Init Error: Could not find vkCreateBuffer via GetDeviceProcAddr");
        return false;
    }

    VkResult res = vmaCreateAllocator(&allocatorInfo, &m_vmaAllocator);
    return (res == VK_SUCCESS);
}

void VKRMemoryManager::ShutdownVMA() {
    if (m_vmaAllocator) {
        vmaDestroyAllocator(m_vmaAllocator);
        m_vmaAllocator = nullptr;
    }
}

// --- Image & Buffer Management ---

VkImageMemAllocation* VKRMemoryManager::imageMemoryAllocate(VkImage image) {
    if (m_vmaAllocator == VK_NULL_HANDLE) return nullptr;

    VmaAllocationCreateInfo allocInfo = {};
    // FORCE memory that is strictly for the GPU. 
    // This avoids the "Host Visible" heaps that usually cause Error -8 on Shield.
    allocInfo.usage = VMA_MEMORY_USAGE_GPU_ONLY; 
    allocInfo.requiredFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
    allocInfo.preferredFlags = 0; 
    
    VmaAllocation allocation;
    VmaAllocationInfo allocationInfo;
    
    VkResult res = vmaAllocateMemoryForImage(m_vmaAllocator, image, &allocInfo, &allocation, &allocationInfo);
    
    if (res != VK_SUCCESS) {
        // Log the actual error and the memory requirements of the image
        VkMemoryRequirements memReqs;
        vkGetImageMemoryRequirements(m_vkr->GetLogicalDevice(), image, &memReqs);
        cemuLog_log(LogType::Force, "VMA: imageMemoryAllocate failed (Error {}). Size: {}, Alignment: {}, TypeBits: {:x}", 
                    (int32_t)res, memReqs.size, memReqs.alignment, memReqs.memoryTypeBits);
        return nullptr;
    }

    vmaBindImageMemory(m_vmaAllocator, allocation, image);
    return new VkImageMemAllocation(allocation, (uint32)allocationInfo.size);
}

void VKRMemoryManager::imageMemoryFree(VkImage image, VkImageMemAllocation* allocation) {
    if (allocation && m_vmaAllocator) {
        vmaFreeMemory(m_vmaAllocator, allocation->vmaAllocation);
        delete allocation;
    }
}

void* VKRMemoryManager::TextureUploadBufferAcquire(uint32 size) {
    // If Start() hasn't been called, this will crash; 
    // we assume the Renderer logic calls Start() first.
    auto reservation = m_stagingBuffer.AllocateBufferMemory(size, 16);
    return reservation.memPtr; 
}

void VKRMemoryManager::TextureUploadBufferRelease(uint8* ptr) {
    // Standard release logic (managed by sync points)
}

bool VKRMemoryManager::CreateBuffer(VkDeviceSize size, VkBufferUsageFlags usage, VmaMemoryUsage vmaUsage, VkBuffer& buffer, VmaAllocation& allocation) const {
    if (m_vmaAllocator == VK_NULL_HANDLE) return false;

    VkBufferCreateInfo bufferInfo = { VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO };
    bufferInfo.size = size;
    bufferInfo.usage = usage;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    VmaAllocationCreateInfo allocInfo = {};
    allocInfo.usage = vmaUsage;

    // Shield/Tegra specific: If we want host access, we must set these flags
    if (vmaUsage == VMA_MEMORY_USAGE_AUTO_PREFER_HOST || vmaUsage == VMA_MEMORY_USAGE_CPU_TO_GPU) {
        allocInfo.flags = VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT | VMA_ALLOCATION_CREATE_MAPPED_BIT;
    }

    VkResult res = vmaCreateBuffer(m_vmaAllocator, &bufferInfo, &allocInfo, &buffer, &allocation, nullptr);
    return (res == VK_SUCCESS);
}

void VKRMemoryManager::DeleteBuffer(VkBuffer& buffer, VmaAllocation& allocation) const {
    if (buffer && m_vmaAllocator) {
        vmaDestroyBuffer(m_vmaAllocator, buffer, allocation);
        buffer = VK_NULL_HANDLE;
        allocation = VK_NULL_HANDLE;
    }
}

void VKRMemoryManager::cleanupBuffers(uint64 fenceValue) {
    m_stagingBuffer.CleanupBuffer(fenceValue);
    m_vertexStrideBuffer.CleanupBuffer(fenceValue);
}

// --- VKRSynchronizedRingAllocator Implementation ---

VKRSynchronizedRingAllocator::VKRSynchronizedRingAllocator(VulkanRenderer* vkRenderer, VKRMemoryManager* vkMemoryManager, VKR_BUFFER_TYPE bufferType, uint32 minBufferSize)
    : m_vkr(vkRenderer), m_vkrMemMgr(vkMemoryManager), m_bufferType(bufferType), m_minimumBufferAllocSize(minBufferSize)
{
}

void VKRSynchronizedRingAllocator::Init() {
    // Only allocate if we haven't already
    if (m_buffers.empty()) {
        allocateAdditionalUploadBuffer(m_minimumBufferAllocSize);
    }
}

VKRSynchronizedRingAllocator::~VKRSynchronizedRingAllocator() {
    VmaAllocator allocator = m_vkrMemMgr->GetVmaAllocator();
    if (allocator != VK_NULL_HANDLE) {
        for (auto& buf : m_buffers) {
            vmaDestroyBuffer(allocator, buf.vk_buffer, buf.vmaAllocation);
        }
    }
    m_buffers.clear();
}

VKRSynchronizedRingAllocator::AllocatorReservation_t 
VKRSynchronizedRingAllocator::AllocateBufferMemory(uint32 size, uint32 alignment) {
    for (uint32 i = 0; i < m_buffers.size(); ++i) {
        auto& buf = m_buffers[i];
        uint32 alignedOffset = (buf.writeIndex + alignment - 1) & ~(alignment - 1);
        
        if (alignedOffset + size <= buf.vmaInfo.size) {
            AllocatorReservation_t res;
            res.vkBuffer = buf.vk_buffer;
            res.vmaAllocation = buf.vmaAllocation;
            res.memPtr = (uint8*)buf.vmaInfo.pMappedData + alignedOffset;
            res.bufferOffset = alignedOffset;
            res.size = size;
            res.bufferIndex = i;

            buf.writeIndex = alignedOffset + size;
            return res;
        }
    }

    allocateAdditionalUploadBuffer(size);
    return AllocateBufferMemory(size, alignment);
}

void VKRSynchronizedRingAllocator::CleanupBuffer(uint64 latestFinishedCommandBufferId) {
    for (auto& buf : m_buffers) {
        buf.writeIndex = 0; 
    }
}

void VKRSynchronizedRingAllocator::allocateAdditionalUploadBuffer(uint32 sizeRequiredForAlloc) {
    uint32 bufferAllocSize = std::max(m_minimumBufferAllocSize, sizeRequiredForAlloc);
    
    VkBufferCreateInfo bufferInfo = { VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO };
    bufferInfo.size = bufferAllocSize;
    bufferInfo.usage = (m_bufferType == VKR_BUFFER_TYPE::STAGING) ? VK_BUFFER_USAGE_TRANSFER_SRC_BIT : VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

    VmaAllocationCreateInfo allocCreateInfo = {};
    allocCreateInfo.usage = VMA_MEMORY_USAGE_AUTO;
    allocCreateInfo.flags = VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT | VMA_ALLOCATION_CREATE_MAPPED_BIT;

    AllocatorBuffer_t newBuffer{};
    // This will now only be called once m_vmaAllocator is valid
    vmaCreateBuffer(m_vkrMemMgr->GetVmaAllocator(), &bufferInfo, &allocCreateInfo, 
                    &newBuffer.vk_buffer, &newBuffer.vmaAllocation, &newBuffer.vmaInfo);

    newBuffer.writeIndex = 0;
    m_buffers.push_back(newBuffer);
}

void VKRSynchronizedRingAllocator::GetStats(uint32& numBuffers, size_t& totalSize, size_t& freeSize) const {
    numBuffers = (uint32)m_buffers.size();
    totalSize = 0;
    freeSize = 0;
    for (const auto& buf : m_buffers) {
        totalSize += (size_t)buf.vmaInfo.size;
        freeSize += (size_t)(buf.vmaInfo.size - buf.writeIndex);
    }
}
