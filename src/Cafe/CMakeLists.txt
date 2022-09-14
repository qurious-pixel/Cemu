project(CemuCafe)

file(GLOB_RECURSE CPP_FILES *.cpp)
file(GLOB_RECURSE H_FILES *.h)

if(APPLE)
	file(GLOB_RECURSE MM_FILES *.mm)
	add_library(CemuCafe ${CPP_FILES} ${MM_FILES} ${H_FILES})
else()
	add_library(CemuCafe ${CPP_FILES} ${H_FILES})
endif()

set_property(TARGET CemuCafe PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

if (WIN32)
	list(APPEND PNG PNG::PNG)
else()
	list(APPEND PNG libpng.a)
endif()

target_include_directories(CemuCafe PUBLIC "../")

target_link_libraries(CemuCafe PRIVATE
	CemuAsm
	CemuAudio
	CemuCommon
	CemuComponents
	CemuConfig
	CemuGui
	CemuInput
	CemuResource
	CemuUtil
	imguiImpl
	Boost::headers
	Boost::nowide
	CURL::libcurl
	fmt::fmt
	glslang::SPIRV
	ih264d
	imgui::imgui
	OpenSSL::Crypto
	OpenSSL::SSL
	${PNG}
	pugixml::pugixml
	ZArchive::zarchive
	ZLIB::ZLIB
	zstd::zstd
)

if (ENABLE_WXWIDGETS)
	target_link_libraries(CemuCafe PRIVATE wx::base wx::core)
endif()

if(WIN32)
	target_link_libraries(CemuCafe PRIVATE iphlpapi)
endif()
