#pragma once

#include "Cafe/TitleList/TitleInfo.h"
#include "Cafe/TitleList/TitleList.h"
#include "JNIUtils.h"
#include "CompressTitleCallbacks.h"

#include <boost/iostreams/device/file_descriptor.hpp>
#include <boost/iostreams/stream_buffer.hpp>
#include <zarchive/zarchivewriter.h>
#include <zarchive/zarchivereader.h>

class WuaConverter
{
	TitleInfo m_titleInfo_base;
	TitleInfo m_titleInfo_update;
	TitleInfo m_titleInfo_aoc;

	struct ZArchiveWriterContext
	{
		static void NewOutputFile(sint32 partIndex, void* _ctx);

		static void WriteOutputData(const void* data, size_t length, void* _ctx);

		bool RecursivelyAddFiles(std::string archivePath, std::string fscPath);

		bool StoreTitle(TitleInfo* titleInfo);

		bool AddTitles(TitleInfo** titles, size_t count);

		int fd;
		bool isValid{false};
		std::unique_ptr<boost::iostreams::file_descriptor_sink> sink{};
		std::unique_ptr<ZArchiveWriter> zaWriter{};
		std::vector<uint8> transferBuffer;
		std::atomic_bool cancelled{false};
		// progress
		std::atomic_uint64_t transferredInputBytes{};
	} m_writerContext;

	std::thread m_workerThread;
	bool m_started{false};

  public:
	WuaConverter(
		const TitleInfo& titleInfo_base,
		const TitleInfo& titleInfo_update,
		const TitleInfo& titleInfo_aoc);

	~WuaConverter();

	std::string getCompressedFileName();

	uint64 getTransferredInputBytes() const;

	void startConversion(int fd, std::unique_ptr<CompressTitleCallbacks>&& callbacks);
};