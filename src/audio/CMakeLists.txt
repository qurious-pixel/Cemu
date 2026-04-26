add_library(CemuAudio
	IAudioAPI.cpp
	IAudioAPI.h
	IAudioInputAPI.cpp
	IAudioInputAPI.h
)

set_property(TARGET CemuAudio PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

if(WIN32)
	target_sources(CemuAudio PRIVATE
		DirectSoundAPI.cpp
		DirectSoundAPI.h
		XAudio2API.cpp
		XAudio2API.h
	)
	if(NOT CMAKE_CXX_COMPILER_ID MATCHES "Clang")
        target_sources(CemuAudio PRIVATE
			XAudio27API.cpp
			XAudio27API.h
		)
	endif()
	if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
        # 3. FORCE include order: System headers MUST come before everything else
        # This prevents the compiler from picking up the 2010 xaudio2.h
        target_include_directories(CemuAudio BEFORE PRIVATE "D:/a/_temp/msys64/clang64/include")

        # 4. Use the WIDE version of LoadLibraryEx since Cemu defines XAUDIO2_DLL as a Wide String (L"...")
        target_compile_definitions(CemuAudio PRIVATE 
            UNICODE 
            _UNICODE
            XAUDIO2_DLL=L"xaudio2_9.dll"
            "__reserved=" # Fixes the 'unknown type name __reserved' error
        )
        
        target_compile_options(CemuAudio PRIVATE -fms-extensions)
    endif()
endif()

if(ENABLE_CUBEB)
	target_sources(CemuAudio PRIVATE
		CubebAPI.cpp
		CubebAPI.h
		CubebInputAPI.cpp
		CubebInputAPI.h
	)
	#add_compile_definitions(HAS_CUBEB)
endif()

target_include_directories(CemuAudio PUBLIC "../")

target_link_libraries(CemuAudio PRIVATE
	CemuCommon
	CemuGui
)

if(ENABLE_CUBEB)
	# PUBLIC because cubeb.h/cubeb.h is included in CubebAPI.h
	target_link_libraries(CemuAudio PUBLIC cubeb::cubeb)
endif()
