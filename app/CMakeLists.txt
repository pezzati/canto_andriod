cmake_minimum_required(VERSION 3.4.1)

# convert SDK path to forward slashes on Windows
file(TO_CMAKE_PATH ${PATH_TO_SUPERPOWERED} PATH_TO_SUPERPOWERED)

set(CMAKE_VERBOSE_MAKEFILE on)

include_directories(src/main/cpp)
include_directories(${PATH_TO_SUPERPOWERED})

add_library(
        Canto
        SHARED
        src/main/cpp/Dubsmash.cpp
        src/main/cpp/Karaoke.cpp
        src/main/cpp/Singing.cpp
        src/main/cpp/EditDubsmash.cpp
        #        src/main/cpp/Player.cpp
        ${PATH_TO_SUPERPOWERED}/AndroidIO/SuperpoweredAndroidAudioIO.cpp
)

# link the native library against the following libraries
target_link_libraries(
        Canto
        ${PATH_TO_SUPERPOWERED}/libSuperpoweredAndroid${ANDROID_ABI}.a
        OpenSLES
        log
        android
)