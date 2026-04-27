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
        XAudio27API.cpp  # Moved outside the NOT Clang check
        XAudio27API.h    # Moved outside the NOT Clang check
    )
    
    if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
        # Your existing Clang-specific fixes...
        target_compile_definitions(CemuAudio PRIVATE 
            UNICODE 
            _UNICODE
            "__reserved="
        )
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
